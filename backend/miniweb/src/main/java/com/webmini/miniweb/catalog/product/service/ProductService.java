package com.webmini.miniweb.catalog.product.service;

import com.webmini.miniweb.catalog.category.entity.Category;
import com.webmini.miniweb.catalog.category.repo.CategoryRepository;
import com.webmini.miniweb.catalog.product.dto.*;
import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.mapper.ProductMapper;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import com.webmini.miniweb.common.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final CategoryRepository categories;
    private final ProductMapper mapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Transactional
    public ProductDtos.ProductResponse create(ProductDtos.ProductCreateRequest req) {
        String trimmedSku = validateAndTrimSku(req.sku());
        String trimmedName = validateAndTrimName(req.name());
        validatePrice(req.price());
        validateStock(req.stock());
        validateStatus(req.status());

        if (repo.existsBySkuIgnoreCase(trimmedSku)) {
            throw new ConflictException("Mã SKU '" + trimmedSku + "' đã tồn tại");
        }
        Category cat = categories.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + req.categoryId()));

        Product e = mapper.toEntity(req);
        e.setSku(trimmedSku);
        e.setName(trimmedName);
        e.setCategory(cat);
        e.setUpdatedAt(null);

        Product saved = repo.save(e);

        if (saved.getStatus() == Product.ProductStatus.ACTIVE &&
                cat.getStatus() == Category.CategoryStatus.INACTIVE) {
            activateCategory(cat);
        }

        return mapper.toDto(saved);
    }


    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse get(Long id) {
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(id);
            
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                try {
                    if (cachedValue instanceof ProductDtos.ProductResponse) {
                        return (ProductDtos.ProductResponse) cachedValue;
                    }
                    if (cachedValue instanceof LinkedHashMap) {
                        ProductDtos.ProductResponse converted = objectMapper.convertValue(cachedValue, ProductDtos.ProductResponse.class);
                        cache.put(id, converted);
                        return converted;
                    }
                    cache.evict(id);
                } catch (Exception ex) {
                    cache.evict(id);
                }
            }
        }
        
        Product entity = repo.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        ProductDtos.ProductResponse response = mapper.toDto(entity);

        if (cache != null) {
            cache.put(id, response);
        }
        return response;
    }

    @Transactional
    public ProductDtos.ProductResponse update(Long id, ProductDtos.ProductUpdateRequest req) {
        Product e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        String trimmedName = validateAndTrimName(req.name());
        validatePrice(req.price());
        validateStock(req.stock());
        validateStatus(req.status());
        Category cat = categories.findById(req.categoryId()).orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + req.categoryId()));

        Product.ProductStatus oldStatus = e.getStatus();

        mapper.update(e, req);
        e.setName(trimmedName);
        e.setCategory(cat);
        e.setUpdatedAt(java.time.LocalDateTime.now());

        Product saved = repo.save(e);

        if (oldStatus == Product.ProductStatus.INACTIVE &&
                saved.getStatus() == Product.ProductStatus.ACTIVE &&
                cat.getStatus() == Category.CategoryStatus.INACTIVE) {
            activateCategory(cat);
        }

        return mapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.ProductResponse> search(String q, String sku, Long categoryId, String status, Integer minStockLt, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
        if (minStockLt != null && minStockLt < 0) {
            throw new ValidationException("Giá trị tồn kho tối thiểu phải >= 0");
        }
        return repo.search(q, sku, categoryId, status, minStockLt, pageable).map(mapper::toDto);
    }

    private String validateAndTrimSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("Mã SKU không được để trống");
        }
        String trimmed = sku.trim();
        if (trimmed.length() < 3) {
            throw new ValidationException("Mã SKU phải có ít nhất 3 ký tự");
        }
        if (trimmed.length() > 100) {
            throw new ValidationException("Mã SKU không được vượt quá 100 ký tự");
        }
        return trimmed;
    }

    private String validateAndTrimName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Tên sản phẩm không được để trống");
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            throw new ValidationException("Tên sản phẩm phải có ít nhất 2 ký tự");
        }
        if (trimmed.length() > 200) {
            throw new ValidationException("Tên sản phẩm không được vượt quá 200 ký tự");
        }
        return trimmed;
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("Giá bán không được để trống");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá bán phải >= 0");
        }
        if (price.precision() - price.scale() > 16) {
            throw new ValidationException("Giá bán không được vượt quá 9,999,999,999,999,999.99");
        }
        if (price.scale() > 2) {
            throw new ValidationException("Giá bán chỉ được có tối đa 2 chữ số thập phân");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null) {
            throw new ValidationException("Tồn kho không được để trống");
        }
        if (stock < 0) {
            throw new ValidationException("Tồn kho phải >= 0");
        }
        if (stock > Integer.MAX_VALUE) {
            throw new ValidationException("Tồn kho không được vượt quá 2,147,483,647");
        }
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("Trạng thái không được để trống");
        }
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ValidationException("Trạng thái phải là ACTIVE hoặc INACTIVE");
        }
    }

    private void activateCategory(Category category) {
        category.setStatus(Category.CategoryStatus.ACTIVE);
        category.setUpdatedAt(java.time.LocalDateTime.now());
        categories.save(category);
    }
}