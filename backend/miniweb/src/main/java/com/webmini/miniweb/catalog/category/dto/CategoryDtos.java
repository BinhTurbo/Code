package com.webmini.miniweb.catalog.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoryDtos {
    
    public record CategoryCreateRequest(
            @NotBlank(message = "Tên danh mục không được để trống")
            @Size(min = 2, max = 150, message = "Tên danh mục phải từ 2-150 ký tự")
            String name,
            
            @NotBlank(message = "Trạng thái không được để trống")
            @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
            String status
    ) {}

    public record CategoryUpdateRequest(
            @NotBlank(message = "Tên danh mục không được để trống")
            @Size(min = 2, max = 150, message = "Tên danh mục phải từ 2-150 ký tự")
            String name,
            
            @NotBlank(message = "Trạng thái không được để trống")
            @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
            String status
    ) {}

    public record CategoryResponse(
            Long id,
            String name,
            String status,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}