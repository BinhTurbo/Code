package com.webmini.miniweb.catalog.product.service;

import com.webmini.miniweb.catalog.category.entity.Category;
import com.webmini.miniweb.catalog.category.repo.CategoryRepository;
import com.webmini.miniweb.catalog.product.dto.*;
import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.mapper.ProductMapper;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import com.webmini.miniweb.catalog.product.specs.ProductSpecs;
import com.webmini.miniweb.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repo;
    private final CategoryRepository categories;
    private final ProductMapper mapper;

    @Transactional
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

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse get(Long id) {
        Product e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với ID: " + id));
        return mapper.toDto(e);
    }

    @Transactional
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
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
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
        
        Specification<Product> spec = Specification.where(ProductSpecs.qLike(q))
                .and(ProductSpecs.skuEquals(sku))
                .and(ProductSpecs.categoryIdEquals(categoryId))
                .and(ProductSpecs.statusEquals(status))
                .and(ProductSpecs.stockLt(minStockLt));
        return repo.findAll(spec, pageable).map(mapper::toDto);
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