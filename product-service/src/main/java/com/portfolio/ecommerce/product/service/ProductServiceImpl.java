package com.portfolio.ecommerce.product.service;

import com.portfolio.ecommerce.product.dto.ProductRequest;
import com.portfolio.ecommerce.product.dto.ProductResponse;
import com.portfolio.ecommerce.product.exception.InsufficientStockException;
import com.portfolio.ecommerce.product.exception.ProductNotFoundException;
import com.portfolio.ecommerce.product.model.Product;
import com.portfolio.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Cacheable(value = "products", key = "#id")
    @Override
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination");
        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<ProductResponse> searchProducts(String search, Pageable pageable) {
        log.info("Searching products with term: {}", search);
        return productRepository.searchProducts(search, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product: {}", request.getName());
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created with id: {}", savedProduct.getId());
        return mapToResponse(savedProduct);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {}", updatedProduct.getId());
        return mapToResponse(updatedProduct);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft deleted: {}", id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    // Add to ProductService class

    public List<ProductResponse> getProductsByCategory(String category) {
        log.info("Fetching products by category: {}", category);
        return productRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<String> getAllCategories() {
        log.info("Fetching all categories");
        return productRepository.findAllCategories();
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateStock(Long id, Integer quantity) {
        log.info("Updating stock for product {}: {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        if (quantity < 0 && Math.abs(quantity) > product.getStock()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + product.getStock() + ", Requested: " + Math.abs(quantity));
        }

        product.setStock(product.getStock() + quantity);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock updated for product {}: new stock = {}", id, updatedProduct.getStock());
        return mapToResponse(updatedProduct);
    }
}