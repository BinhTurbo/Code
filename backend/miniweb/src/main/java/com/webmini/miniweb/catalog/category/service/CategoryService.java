package com.webmini.miniweb.catalog.category.service;

import com.webmini.miniweb.catalog.category.dto.*;
import com.webmini.miniweb.catalog.category.entity.Category;
import com.webmini.miniweb.catalog.category.mapper.CategoryMapper;
import com.webmini.miniweb.catalog.category.repo.CategoryRepository;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.common.*;
import com.webmini.miniweb.messaging.service.CategoryEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final CategoryMapper mapper;
    private final ProductRepository productRepo;
    private final CategoryEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryCreateRequest req) {
        String trimmedName = validateAndTrimName(req.name());
        validateStatus(req.status());
        
        if (repo.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Tên danh mục '" + trimmedName + "' đã tồn tại");
        }
        
        Category e = mapper.toEntity(req);
        e.setName(trimmedName);
        e.setUpdatedAt(null);
        
        Category saved = repo.save(e);
        eventPublisher.publishCategoryCreated(saved.getId(), saved.getName(), saved.getStatus().name());
        
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public CategoryDtos.CategoryResponse get(Long id) {

        Cache cache = cacheManager.getCache("categories");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(id);
            
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                try {
                    if (cachedValue instanceof CategoryDtos.CategoryResponse) {
                        return (CategoryDtos.CategoryResponse) cachedValue;
                    }
                    
                    if (cachedValue instanceof LinkedHashMap) {
                        CategoryDtos.CategoryResponse converted = objectMapper.convertValue(
                            cachedValue, 
                            CategoryDtos.CategoryResponse.class
                        );
                        cache.put(id, converted);
                        return converted;
                    }
                    cache.evict(id);
                } catch (Exception ex) {
                    cache.evict(id);
                }
            }
        }
        
        Category entity = repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        
        CategoryDtos.CategoryResponse response = mapper.toDto(entity);
        
        if (cache != null) {
            cache.put(id, response);
        }
        
        return response;
    }


    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryUpdateRequest req) {
        Category e = repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        
        String trimmedName = validateAndTrimName(req.name());
        validateStatus(req.status());
        
        if (!e.getName().equalsIgnoreCase(trimmedName) && repo.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Tên danh mục '" + trimmedName + "' đã tồn tại");
        }

        Category.CategoryStatus oldStatus = e.getStatus();
        Category.CategoryStatus newStatus = Category.CategoryStatus.valueOf(req.status());

        mapper.update(e, req);
        e.setName(trimmedName);
        e.setUpdatedAt(java.time.LocalDateTime.now());
        
        Category saved = repo.save(e);

        if (oldStatus == Category.CategoryStatus.ACTIVE &&
            newStatus == Category.CategoryStatus.INACTIVE) {
            cascadeInactiveProducts(saved);
        }

        // Publish event
        if (oldStatus != saved.getStatus()) {
            eventPublisher.publishCategoryStatusChanged(
                saved.getId(),
                saved.getName(),
                oldStatus.name(),
                saved.getStatus().name()
            );
        }

        return mapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy danh mục với ID: " + id);
        }
        
        if (productRepo.existsByCategoryId(id)) {
            throw new ConflictException(
                "Không thể xóa danh mục vì có sản phẩm đang tham chiếu. " +
                "Vui lòng di chuyển hoặc xóa các sản phẩm đó trước."
            );
        }
        
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<CategoryDtos.CategoryResponse> search(String q, String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
        
        return repo.search(q, status, pageable).map(mapper::toDto);
    }


    private void cascadeInactiveProducts(Category category) {
        List<Product> activeProducts = productRepo.findByCategoryIdAndStatus(
            category.getId(), 
            Product.ProductStatus.ACTIVE
        );
        
        if (activeProducts.isEmpty()) {
            return;
        }
        
        int count = 0;
        Cache productCache = cacheManager.getCache("products");
        
        for (Product product : activeProducts) {
            product.setStatus(Product.ProductStatus.INACTIVE);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            productRepo.save(product);
            
            if (productCache != null) {
                productCache.evict(product.getId());
            }
            
            count++;
        }
    }

    private String validateAndTrimName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Tên danh mục không được để trống");
        }
        
        String trimmed = name.trim();
        
        if (trimmed.length() < 2) {
            throw new ValidationException("Tên danh mục phải có ít nhất 2 ký tự");
        }
        
        if (trimmed.length() > 150) {
            throw new ValidationException("Tên danh mục không được vượt quá 150 ký tự");
        }
        
        return trimmed;
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("Trạng thái không được để trống");
        }
        
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ValidationException("Trạng thái phải là ACTIVE hoặc INACTIVE");
        }
    }
}