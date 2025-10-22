package com.webmini.miniweb.catalog.product.controller;
import com.webmini.miniweb.catalog.product.dto.*;
import com.webmini.miniweb.catalog.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    @PostMapping
    public ProductDtos.ProductResponse create(@Valid @RequestBody ProductDtos.ProductCreateRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public ProductDtos.ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public ProductDtos.ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductDtos.ProductUpdateRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public Page<ProductDtos.ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer minStockLt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = toPageable(page, size, sort, "createdAt,desc");
        return service.search(q, sku, categoryId, status, minStockLt, pageable);
    }

    private Pageable toPageable(int page, int size, String sort, String fallback) {
        String s = (sort == null || sort.isBlank()) ? fallback : sort;
        String[] parts = s.split(",");
        Sort.Direction dir = (parts.length > 1 && parts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(new Sort.Order(dir, parts[0])));
    }
}