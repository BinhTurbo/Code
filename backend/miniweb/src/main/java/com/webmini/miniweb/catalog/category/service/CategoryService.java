package com.webmini.miniweb.catalog.category.service;

import com.webmini.miniweb.catalog.category.dto.*;
import com.webmini.miniweb.catalog.category.entity.Category;
import com.webmini.miniweb.catalog.category.mapper.CategoryMapper;
import com.webmini.miniweb.catalog.category.repo.CategoryRepository;
import com.webmini.miniweb.catalog.category.specs.CategorySpecs;
import com.webmini.miniweb.catalog.product.entity.Product;
import com.webmini.miniweb.catalog.product.repo.ProductRepository;
import com.webmini.miniweb.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final CategoryMapper mapper;
    private final ProductRepository productRepo;

    @Transactional
    public CategoryDtos.CategoryResponse create(CategoryDtos.CategoryCreateRequest req) {
        // Validate and trim name
        String trimmedName = validateAndTrimName(req.name());
        
        // Validate status
        validateStatus(req.status());
        
        // Check duplicate name
        if (repo.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Tên danh mục '" + trimmedName + "' đã tồn tại");
        }
        
        // Create new category
        Category e = mapper.toEntity(req);
        e.setName(trimmedName);
        e.setUpdatedAt(null);
        
        return mapper.toDto(repo.save(e));
    }

    @Transactional(readOnly = true)
    public CategoryDtos.CategoryResponse get(Long id) {
        Category e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        return mapper.toDto(e);
    }

    @Transactional
    public CategoryDtos.CategoryResponse update(Long id, CategoryDtos.CategoryUpdateRequest req) {
        // Find existing category
        Category e = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy danh mục với ID: " + id));
        
        // Validate and trim name
        String trimmedName = validateAndTrimName(req.name());
        
        // Validate status
        validateStatus(req.status());
        
        // Check duplicate name (excluding current category)
        if (!e.getName().equalsIgnoreCase(trimmedName) && repo.existsByNameIgnoreCase(trimmedName)) {
            throw new ConflictException("Tên danh mục '" + trimmedName + "' đã tồn tại");
        }

        Category.CategoryStatus oldStatus = e.getStatus();

        // Update entity
        mapper.update(e, req);
        e.setName(trimmedName);
        e.setUpdatedAt(java.time.LocalDateTime.now());
        
        Category saved = repo.save(e);

        // Cascade INACTIVE status to products if category became INACTIVE
        if (oldStatus == Category.CategoryStatus.ACTIVE &&
                saved.getStatus() == Category.CategoryStatus.INACTIVE) {
            cascadeInactiveToProducts(id);
        }

        return mapper.toDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Không tìm thấy danh mục với ID: " + id);
        }
        
        // Check if any products reference this category
        if (productRepo.existsByCategory_Id(id)) {
            throw new ConflictException(
                "Không thể xóa danh mục vì có sản phẩm đang tham chiếu. " +
                "Vui lòng di chuyển hoặc xóa các sản phẩm đó trước."
            );
        }
        
        repo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Page<CategoryDtos.CategoryResponse> search(String q, String status, Pageable pageable) {
        // Validate status if provided
        if (status != null && !status.isBlank()) {
            validateStatus(status);
        }
        
        Specification<Category> spec = Specification.where(CategorySpecs.nameContains(q))
                .and(CategorySpecs.statusEquals(status));
        return repo.findAll(spec, pageable).map(mapper::toDto);
    }

    /**
     * Validate and trim category name
     */
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
     * Set all products of a category to INACTIVE when category becomes INACTIVE
     */
    private void cascadeInactiveToProducts(Long categoryId) {
        List<Product> products = productRepo.findAllByCategory_Id(categoryId);
        products.forEach(product -> {
            if (product.getStatus() == Product.ProductStatus.ACTIVE) {
                product.setStatus(Product.ProductStatus.INACTIVE);
                product.setUpdatedAt(java.time.LocalDateTime.now());
            }
        });
        if (!products.isEmpty()) {
            productRepo.saveAll(products);
        }
    }
}