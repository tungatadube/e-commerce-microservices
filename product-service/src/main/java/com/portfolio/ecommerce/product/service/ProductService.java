package com.portfolio.ecommerce.product.service;

import com.portfolio.ecommerce.product.dto.ProductRequest;
import com.portfolio.ecommerce.product.dto.ProductResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ProductService {
    @Cacheable(value = "products", key = "#id")
    ProductResponse getProductById(Long id);

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> searchProducts(String search, Pageable pageable);

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    ProductResponse createProduct(ProductRequest request);

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    ProductResponse updateProduct(Long id, ProductRequest request);

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    void deleteProduct(Long id);

    ProductResponse updateStock(Long id, Integer quantity);

    List<String> getAllCategories();

    List<ProductResponse> getProductsByCategory(String category);
}
