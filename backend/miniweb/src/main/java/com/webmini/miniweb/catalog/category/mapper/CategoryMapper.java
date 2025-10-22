package com.webmini.miniweb.catalog.category.mapper;

import com.webmini.miniweb.catalog.category.dto.CategoryDtos;
import com.webmini.miniweb.catalog.category.entity.Category;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    Category toEntity(CategoryDtos.CategoryCreateRequest req);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Category entity, CategoryDtos.CategoryUpdateRequest req);
    CategoryDtos.CategoryResponse toDto(Category e);
}