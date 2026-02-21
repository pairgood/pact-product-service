package com.ecommerce.productservice.service;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.telemetry.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private TelemetryClient telemetryClient;
    
    public Product createProduct(Product product) {
        telemetryClient.logEvent("Creating new product: " + product.getName(), "INFO");
        Product savedProduct = productRepository.save(product);
        telemetryClient.logEvent("Product created successfully with ID: " + savedProduct.getId(), "INFO");
        return savedProduct;
    }
    
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    public Product getProductById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Product not found"));
    }
    
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    public Product updateProduct(Long id, Product productDetails) {
        telemetryClient.logEvent("Updating product with ID: " + id, "INFO");
        
        Product product = getProductById(id);
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setImageUrl(productDetails.getImageUrl());
        product.setSku(productDetails.getSku());
        
        Product updatedProduct = productRepository.save(product);
        telemetryClient.logEvent("Product updated successfully: " + id, "INFO");
        return updatedProduct;
    }
    
    public Product updateStock(Long id, Integer newQuantity) {
        telemetryClient.logEvent("Updating stock for product " + id + " to quantity: " + newQuantity, "INFO");
        
        Product product = getProductById(id);
        Integer oldQuantity = product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        
        Product updatedProduct = productRepository.save(product);
        telemetryClient.logEvent("Stock updated for product " + id + " from " + oldQuantity + " to " + newQuantity, "INFO");
        return updatedProduct;
    }
    
    public void deleteProduct(Long id) {
        telemetryClient.logEvent("Deleting product with ID: " + id, "INFO");
        
        Product product = getProductById(id);
        productRepository.delete(product);
        
        telemetryClient.logEvent("Product deleted successfully: " + id, "INFO");
    }
    
    public boolean isProductAvailable(Long id, Integer quantity) {
        Product product = getProductById(id);
        return product.getStockQuantity() >= quantity;
    }
}