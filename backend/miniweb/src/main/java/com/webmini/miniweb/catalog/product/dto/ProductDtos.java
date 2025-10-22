package com.webmini.miniweb.catalog.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductDtos {
    
    public record ProductCreateRequest(
            @NotBlank(message = "Mã SKU không được để trống")
            @Size(min = 3, max = 100, message = "Mã SKU phải từ 3-100 ký tự")
            String sku,
            
            @NotBlank(message = "Tên sản phẩm không được để trống")
            @Size(min = 2, max = 200, message = "Tên sản phẩm phải từ 2-200 ký tự")
            String name,
            
            @NotNull(message = "Danh mục không được để trống")
            Long categoryId,
            
            @NotNull(message = "Giá bán không được để trống")
            @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải >= 0")
            @DecimalMax(value = "9999999999999999.99", message = "Giá bán không được vượt quá 9,999,999,999,999,999.99")
            @Digits(integer = 16, fraction = 2, message = "Giá bán phải có tối đa 16 chữ số phần nguyên và 2 chữ số thập phân")
            BigDecimal price,
            
            @NotNull(message = "Tồn kho không được để trống")
            @Min(value = 0, message = "Tồn kho phải >= 0")
            @Max(value = 2147483647, message = "Tồn kho không được vượt quá 2,147,483,647")
            Integer stock,
            
            @NotBlank(message = "Trạng thái không được để trống")
            @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
            String status
    ) {}

    public record ProductUpdateRequest(
            @NotBlank(message = "Tên sản phẩm không được để trống")
            @Size(min = 2, max = 200, message = "Tên sản phẩm phải từ 2-200 ký tự")
            String name,
            
            @NotNull(message = "Danh mục không được để trống")
            Long categoryId,
            
            @NotNull(message = "Giá bán không được để trống")
            @DecimalMin(value = "0.0", inclusive = true, message = "Giá bán phải >= 0")
            @DecimalMax(value = "9999999999999999.99", message = "Giá bán không được vượt quá 9,999,999,999,999,999.99")
            @Digits(integer = 16, fraction = 2, message = "Giá bán phải có tối đa 16 chữ số phần nguyên và 2 chữ số thập phân")
            BigDecimal price,
            
            @NotNull(message = "Tồn kho không được để trống")
            @Min(value = 0, message = "Tồn kho phải >= 0")
            @Max(value = 2147483647, message = "Tồn kho không được vượt quá 2,147,483,647")
            Integer stock,
            
            @NotBlank(message = "Trạng thái không được để trống")
            @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
            String status
    ) {}

    public record ProductResponse(
            Long id,
            String sku,
            String name,
            Long categoryId,
            String categoryName,
            BigDecimal price,
            Integer stock,
            String status,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}