package com.ecommerce.productservice.service;

import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TelemetryClient telemetryClient;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setDescription("Test Description");
        testProduct.setPrice(new BigDecimal("29.99"));
        testProduct.setStockQuantity(10);
        testProduct.setCategory("Electronics");
        testProduct.setSku("TEST-001");
        testProduct.setImageUrl("http://example.com/image.jpg");
    }

    @Test
    void createProduct_ShouldSaveAndReturnProduct() {
        // Given
        Product newProduct = new Product();
        newProduct.setName("New Product");
        newProduct.setDescription("New Description");
        newProduct.setPrice(new BigDecimal("19.99"));
        newProduct.setStockQuantity(5);

        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        Product result = productService.createProduct(newProduct);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("29.99"));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getAllProducts_ShouldReturnAllProducts() {
        // Given
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setPrice(new BigDecimal("39.99"));

        List<Product> products = Arrays.asList(testProduct, product2);
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<Product> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Test Product");
        assertThat(result.get(1).getName()).isEqualTo("Product 2");
        verify(productRepository).findAll();
    }

    @Test
    void getProductById_WithExistingProduct_ShouldReturnProduct() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        Product result = productService.getProductById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        verify(productRepository).findById(1L);
    }

    @Test
    void getProductById_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> productService.getProductById(999L));
        
        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findById(999L);
    }

    @Test
    void getProductsByCategory_ShouldReturnProductsInCategory() {
        // Given
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Another Electronics Product");
        product2.setCategory("Electronics");

        List<Product> products = Arrays.asList(testProduct, product2);
        when(productRepository.findByCategory("Electronics")).thenReturn(products);

        // When
        List<Product> result = productService.getProductsByCategory("Electronics");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCategory()).isEqualTo("Electronics");
        assertThat(result.get(1).getCategory()).isEqualTo("Electronics");
        verify(productRepository).findByCategory("Electronics");
    }

    @Test
    void updateProduct_WithExistingProduct_ShouldReturnUpdatedProduct() {
        // Given
        Product updateData = new Product();
        updateData.setName("Updated Product");
        updateData.setDescription("Updated Description");
        updateData.setPrice(new BigDecimal("39.99"));
        updateData.setStockQuantity(15);
        updateData.setCategory("Updated Category");
        updateData.setImageUrl("http://example.com/new-image.jpg");
        updateData.setSku("UPDATED-001");

        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Updated Product");
        updatedProduct.setDescription("Updated Description");
        updatedProduct.setPrice(new BigDecimal("39.99"));
        updatedProduct.setStockQuantity(15);
        updatedProduct.setCategory("Updated Category");
        updatedProduct.setImageUrl("http://example.com/new-image.jpg");
        updatedProduct.setSku("UPDATED-001");

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // When
        Product result = productService.updateProduct(1L, updateData);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Updated Product");
        assertThat(result.getDescription()).isEqualTo("Updated Description");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("39.99"));
        assertThat(result.getStockQuantity()).isEqualTo(15);
        assertThat(result.getCategory()).isEqualTo("Updated Category");
        assertThat(result.getImageUrl()).isEqualTo("http://example.com/new-image.jpg");
        assertThat(result.getSku()).isEqualTo("UPDATED-001");
        
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_WithNonExistentProduct_ShouldThrowException() {
        // Given
        Product updateData = new Product();
        updateData.setName("Updated Product");

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> productService.updateProduct(999L, updateData));
        
        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateStock_WithExistingProduct_ShouldReturnUpdatedProduct() {
        // Given
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Test Product");
        updatedProduct.setStockQuantity(20);

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(updatedProduct);

        // When
        Product result = productService.updateStock(1L, 20);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStockQuantity()).isEqualTo(20);
        verify(productRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateStock_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> productService.updateStock(999L, 15));
        
        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteProduct_WithExistingProduct_ShouldDeleteProduct() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(testProduct);

        // When
        productService.deleteProduct(1L);

        // Then
        verify(productRepository).findById(1L);
        verify(productRepository).delete(testProduct);
    }

    @Test
    void deleteProduct_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> productService.deleteProduct(999L));
        
        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findById(999L);
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void isProductAvailable_WithSufficientStock_ShouldReturnTrue() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(1L, 5);

        // Then
        assertThat(result).isTrue();
        verify(productRepository).findById(1L);
    }

    @Test
    void isProductAvailable_WithInsufficientStock_ShouldReturnFalse() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(1L, 15);

        // Then
        assertThat(result).isFalse();
        verify(productRepository).findById(1L);
    }

    @Test
    void isProductAvailable_WithExactStock_ShouldReturnTrue() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(1L, 10);

        // Then
        assertThat(result).isTrue();
        verify(productRepository).findById(1L);
    }

    @Test
    void isProductAvailable_WithNonExistentProduct_ShouldThrowException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> productService.isProductAvailable(999L, 5));
        
        assertThat(exception.getMessage()).isEqualTo("Product not found");
        verify(productRepository).findById(999L);
    }
}