package com.webmini.miniweb.catalog.product.mapper;


import com.webmini.miniweb.catalog.product.dto.*;
import com.webmini.miniweb.catalog.product.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(ProductDtos.ProductCreateRequest req);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void update(@MappingTarget Product entity, ProductDtos.ProductUpdateRequest req);

    @Mapping(source="category.id", target="categoryId")
    @Mapping(source="category.name", target="categoryName")
    ProductDtos.ProductResponse toDto(Product e);
}