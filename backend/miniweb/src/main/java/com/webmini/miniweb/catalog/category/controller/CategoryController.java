package com.webmini.miniweb.catalog.category.controller;

import com.webmini.miniweb.catalog.category.dto.*;
import com.webmini.miniweb.catalog.category.dto.CategoryDtos;
import com.webmini.miniweb.catalog.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService serviceCategory;

    @PostMapping
    public CategoryDtos.CategoryResponse create(@Valid @RequestBody CategoryDtos.CategoryCreateRequest req) {
        return serviceCategory.create(req);
    }

    @GetMapping("/{id}")
    public CategoryDtos.CategoryResponse get(@PathVariable Long id) {
        return serviceCategory.get(id);
    }

    @PutMapping("/{id}")
    public CategoryDtos.CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryDtos.CategoryUpdateRequest req) {
        return serviceCategory.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        serviceCategory.delete(id);
    }

    @GetMapping
    public Page<CategoryDtos.CategoryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = toPageable(page, size, sort, "createdAt,desc");
        return serviceCategory.search(q, status, pageable);
    }

    private Pageable toPageable(int page, int size, String sort, String fallback) {
        String s = (sort == null || sort.isBlank()) ? fallback : sort;
        String[] parts = s.split(",");
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(new Sort.Order(dir, parts[0])));
    }
}