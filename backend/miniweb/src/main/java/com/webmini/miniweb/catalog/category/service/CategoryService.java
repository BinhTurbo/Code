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
        log.debug("🔍 [CACHE] Đang tìm category ID={} trong cache...", id);
        
        Cache cache = cacheManager.getCache("categories");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(id);
            
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                log.debug("✅ [CACHE HIT] Tìm thấy cache cho ID={}, kiểu: {}", id, cachedValue != null ? cachedValue.getClass().getSimpleName() : "null");

                try {
                    if (cachedValue instanceof CategoryDtos.CategoryResponse) {
                        log.info("✅ [CACHE] Trả về cache đúng kiểu cho ID={}", id);
                        return (CategoryDtos.CategoryResponse) cachedValue;
                    }
                    
                    if (cachedValue instanceof LinkedHashMap) {
                        log.warn("⚠️ [CACHE] Cache là LinkedHashMap, đang convert cho ID={}...", id);
                        CategoryDtos.CategoryResponse converted = objectMapper.convertValue(
                            cachedValue, 
                            CategoryDtos.CategoryResponse.class
                        );
                        log.info("✅ [CACHE] Convert thành công LinkedHashMap → CategoryResponse cho ID={}", id);
                        
                        cache.put(id, converted);
                        log.debug("💾 [CACHE] Đã cache lại đúng kiểu cho ID={}", id);
                        
                        return converted;
                    }
                    
                    log.error("❌ [CACHE] Cache có kiểu lạ: {} cho ID={}, xóa cache...", 
                             cachedValue.getClass().getName(), id);
                    cache.evict(id);
                    
                } catch (Exception ex) {
                    log.error("❌ [CACHE] Lỗi khi xử lý cache cho ID={}: {}", id, ex.getMessage());
                    cache.evict(id);
                }
            } else {
                log.debug("❌ [CACHE MISS] Không tìm thấy cache cho ID={}", id);
            }
        }
        
        log.info("🔄 [DB QUERY] Query database cho category ID={}", id);
        Category entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        
        CategoryDtos.CategoryResponse response = mapper.toDto(entity);
        
        if (cache != null) {
            cache.put(id, response);
            log.debug("💾 [CACHE] Đã cache response cho ID={}", id);
        }
        
        return response;
    }

    /**
     * ✅ CẢI TIẾN: Cascade inactive products khi category bị inactive
     */
    @Transactional
    @CacheEvict(value = "categories", key = "#id")
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryUpdateRequest req) {
        Category e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        
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

        // 🔥 XỬ LÝ CASCADE: ACTIVE → INACTIVE
        if (oldStatus == Category.CategoryStatus.ACTIVE && 
            newStatus == Category.CategoryStatus.INACTIVE) {
            
            log.info("⚠️ [CASCADE] Category ID={} chuyển ACTIVE → INACTIVE, đang inactive các products...", id);
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
    @CacheEvict(value = "categories", key = "#id")
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

    /**
     * 🔥 CASCADE: Tự động inactive tất cả products thuộc category
     *
     * ⚠️ Logic:
     * - Tìm tất cả products ACTIVE thuộc category
     * - Chuyển status sang INACTIVE
     * - Xóa cache của products bị ảnh hưởng
     * - Log số lượng products bị inactive
     */
    private void cascadeInactiveProducts(Category category) {
        // Tìm tất cả products ACTIVE thuộc category này
        List<Product> activeProducts = productRepo.findByCategoryIdAndStatus(
            category.getId(), 
            Product.ProductStatus.ACTIVE
        );
        
        if (activeProducts.isEmpty()) {
            log.info("✅ [CASCADE] Không có products ACTIVE nào thuộc category ID={}", category.getId());
            return;
        }
        
        log.warn("⚠️ [CASCADE] Tìm thấy {} products ACTIVE thuộc category ID={}, đang inactive...", 
                 activeProducts.size(), category.getId());
        
        // Inactive từng product
        int count = 0;
        Cache productCache = cacheManager.getCache("products");
        
        for (Product product : activeProducts) {
            product.setStatus(Product.ProductStatus.INACTIVE);
            product.setUpdatedAt(java.time.LocalDateTime.now());
            productRepo.save(product);
            
            // Xóa cache của product
            if (productCache != null) {
                productCache.evict(product.getId());
                log.debug("🗑️ [CACHE] Đã xóa cache cho product ID={}", product.getId());
            }
            
            count++;
            log.debug("✅ [CASCADE] Inactive product ID={} (SKU: {})", product.getId(), product.getSku());
        }
        
        log.warn("⚠️ [CASCADE] Đã inactive {} products thuộc category ID={} ({})", 
                 count, category.getId(), category.getName());
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