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
    @CacheEvict(value = "products", allEntries = true)
    public ProductDtos.ProductResponse create(ProductDtos.ProductCreateRequest req) {
        // Validate SKU
        String trimmedSku = validateAndTrimSku(req.sku());
        
        // Validate name
        String trimmedName = validateAndTrimName(req.name());
        
        // Validate price
        validatePrice(req.price());
        
        // Validate stock
        validateStock(req.stock());
        
        // Validate status
        validateStatus(req.status());
        
        // Check duplicate SKU
        if (repo.existsBySkuIgnoreCase(trimmedSku)) {
            throw new ConflictException("Mã SKU '" + trimmedSku + "' đã tồn tại");
        }
        
        // Validate category exists
        Category cat = categories.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + req.categoryId()));

        // Create new product
        Product e = mapper.toEntity(req);
        e.setSku(trimmedSku);
        e.setName(trimmedName);
        e.setCategory(cat);
        e.setUpdatedAt(null);

        Product saved = repo.save(e);

        // Auto-activate category if product is ACTIVE but category is INACTIVE
        if (saved.getStatus() == Product.ProductStatus.ACTIVE &&
                cat.getStatus() == Category.CategoryStatus.INACTIVE) {
            activateCategory(cat);
        }

        return mapper.toDto(saved);
    }

    /**
     * ✅ TỰ XỬ LÝ CACHE - Không dùng @Cacheable
     * 
     * 🔥 Flow:
     * 1. Kiểm tra cache thủ công
     * 2. Nếu có cache:
     *    - Try cast về ProductResponse
     *    - Nếu lỗi → convert từ LinkedHashMap
     * 3. Nếu không có cache → query DB → cache lại
     */
    @Transactional(readOnly = true)
    // ❌ BỎ @Cacheable (vì nó cast trước khi vào method)
    // @Cacheable(value = "products", key = "#id")
    public ProductDtos.ProductResponse get(Long id) {
        log.debug("🔍 [CACHE] Đang tìm product ID={} trong cache...", id);
        
        // 1️⃣ Kiểm tra cache thủ công
        Cache cache = cacheManager.getCache("products");
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(id);
            
            if (wrapper != null) {
                Object cachedValue = wrapper.get();
                log.debug("✅ [CACHE HIT] Tìm thấy cache cho product ID={}, kiểu: {}", id, 
                         cachedValue != null ? cachedValue.getClass().getSimpleName() : "null");
                
                // 2️⃣ Xử lý cache data
                try {
                    // Try cast trực tiếp (nếu cache đúng)
                    if (cachedValue instanceof ProductDtos.ProductResponse) {
                        log.info("✅ [CACHE] Trả về cache đúng kiểu cho product ID={}", id);
                        return (ProductDtos.ProductResponse) cachedValue;
                    }
                    
                    // Nếu là LinkedHashMap → convert
                    if (cachedValue instanceof LinkedHashMap) {
                        log.warn("⚠️ [CACHE] Cache là LinkedHashMap, đang convert cho product ID={}...", id);
                        ProductDtos.ProductResponse converted = objectMapper.convertValue(
                            cachedValue, 
                            ProductDtos.ProductResponse.class
                        );
                        log.info("✅ [CACHE] Convert thành công LinkedHashMap → ProductResponse cho ID={}", id);
                        
                        // 3️⃣ Cache lại đúng kiểu (để lần sau không cần convert)
                        cache.put(id, converted);
                        log.debug("💾 [CACHE] Đã cache lại đúng kiểu cho product ID={}", id);
                        
                        return converted;
                    }
                    
                    // Kiểu không mong đợi → xóa cache lỗi
                    log.error("❌ [CACHE] Cache có kiểu lạ: {} cho product ID={}, xóa cache...", 
                             cachedValue.getClass().getName(), id);
                    cache.evict(id);
                    
                } catch (Exception ex) {
                    log.error("❌ [CACHE] Lỗi khi xử lý cache cho product ID={}: {}", id, ex.getMessage());
                    // Xóa cache lỗi
                    cache.evict(id);
                }
            } else {
                log.debug("❌ [CACHE MISS] Không tìm thấy cache cho product ID={}", id);
            }
        }
        
        // 4️⃣ Cache miss hoặc lỗi → Query DB
        log.info("🔄 [DB QUERY] Query database cho product ID={}", id);
        Product entity = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        
        ProductDtos.ProductResponse response = mapper.toDto(entity);
        
        // 5️⃣ Cache lại kết quả
        if (cache != null) {
            cache.put(id, response);
            log.debug("💾 [CACHE] Đã cache response cho product ID={}", id);
        }
        
        return response;
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductDtos.ProductResponse update(Long id, ProductDtos.ProductUpdateRequest req) {
        // Find existing product
        Product e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        
        // Validate name
        String trimmedName = validateAndTrimName(req.name());
        
        // Validate price
        validatePrice(req.price());
        
        // Validate stock
        validateStock(req.stock());
        
        // Validate status
        validateStatus(req.status());
        
        // Validate category exists
        Category cat = categories.findById(req.categoryId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + req.categoryId()));

        Product.ProductStatus oldStatus = e.getStatus();

        // Update entity
        mapper.update(e, req);
        e.setName(trimmedName);
        e.setCategory(cat);
        e.setUpdatedAt(java.time.LocalDateTime.now());

        Product saved = repo.save(e);

        // Auto-activate category if product became ACTIVE but category is INACTIVE
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
    // ⚠️ Không cache Page object vì không serialize/deserialize tốt với Redis
    // Chỉ cache get by ID là đủ
    public Page<ProductDtos.ProductResponse> search(String q, String sku, Long categoryId,
                                                    String status, Integer minStockLt, Pageable pageable) {
        // Validate status if provided
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
        
        // Validate minStockLt if provided
        if (minStockLt != null && minStockLt < 0) {
            throw new ValidationException("Giá trị tồn kho tối thiểu phải >= 0");
        }
        
        return repo.search(q, sku, categoryId, status, minStockLt, pageable).map(mapper::toDto);
    }

    /**
     * Validate and trim SKU
     */
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

    /**
     * Validate and trim product name
     */
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

    /**
     * Validate price (DECIMAL(18,2) >= 0)
     */
    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new ValidationException("Giá bán không được để trống");
        }
        
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá bán phải >= 0");
        }
        
        // Check DECIMAL(18,2) constraint: max 16 digits before decimal, 2 after
        if (price.precision() - price.scale() > 16) {
            throw new ValidationException("Giá bán không được vượt quá 9,999,999,999,999,999.99");
        }
        
        if (price.scale() > 2) {
            throw new ValidationException("Giá bán chỉ được có tối đa 2 chữ số thập phân");
        }
    }

    /**
     * Validate stock (INT >= 0)
     */
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

    /**
     * Validate status enum value
     */
    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new ValidationException("Trạng thái không được để trống");
        }
        
        if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
            throw new ValidationException("Trạng thái phải là ACTIVE hoặc INACTIVE");
        }
    }

    /**
     * Auto-activate category when creating/updating an ACTIVE product
     */
    private void activateCategory(Category category) {
        category.setStatus(Category.CategoryStatus.ACTIVE);
        category.setUpdatedAt(java.time.LocalDateTime.now());
        categories.save(category);
    }
}