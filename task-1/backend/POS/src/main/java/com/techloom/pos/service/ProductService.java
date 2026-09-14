package com.techloom.pos.service;

import com.techloom.pos.dto.ProductRequest;
import com.techloom.pos.dto.ProductResponse;
import com.techloom.pos.entity.Product;
import com.techloom.pos.exception.ResourceNotFoundException;
import com.techloom.pos.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest request) {
        String category = request.getCategory() == null || request.getCategory().isBlank()
                ? "General"
                : request.getCategory().trim();

        Product product = Product.builder()
                .category(category)
                .name(request.getName())
                .price(request.getPrice())
                .stock(request.getStock())
                .build();

        return mapToResponse(productRepository.save(product));
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String category = request.getCategory() == null || request.getCategory().isBlank()
                ? "General"
                : request.getCategory().trim();

        product.setCategory(category);
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        return mapToResponse(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .category(product.getCategory())
                .name(product.getName())
                .price(product.getPrice())
                .stock(product.getStock())
                .build();
    }
}
