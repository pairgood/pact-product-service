package com.ecommerce.productservice.pact;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.PactBrokerAuth;
import com.ecommerce.productservice.model.Product;
import com.ecommerce.productservice.repository.ProductRepository;
import com.ecommerce.productservice.telemetry.TelemetryClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Provider("product-service")   // MUST match spring.application.name exactly
@PactBroker(
    url = "http://localhost:9292",
    authentication = @PactBrokerAuth(username = "admin", password = "admin")
)
@IgnoreNoPactsToVerify  // Allow test to pass when no consumer pacts exist yet
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceProviderPactTest {

    @LocalServerPort
    private int port;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private TelemetryClient telemetryClient;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.setTarget(new HttpTestTarget("localhost", port));
        }

        // Set up default mock behavior for telemetry client
        when(telemetryClient.startTrace(anyString(), anyString(), anyString(), anyString())).thenReturn("trace_123");
        doNothing().when(telemetryClient).finishTrace(anyString(), anyInt(), anyString());
        doNothing().when(telemetryClient).logEvent(anyString(), anyString());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        // Context will be null when @IgnoreNoPactsToVerify creates a placeholder test
        if (context != null) {
            context.verifyInteraction();
        }
    }

    // State string must be IDENTICAL to consumer's given() — character for character
    @State("a product with id 42 exists")
    void productWithId42Exists() {
        Product product = new Product("Widget", "A great widget", new BigDecimal("99.99"), 100);
        product.setId(42L);
        product.setCategory("Electronics");

        when(productRepository.findById(42L)).thenReturn(Optional.of(product));
    }

    @State("a product with id 999 does not exist")
    void productWithId999DoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());
    }

    @State("products exist in the system")
    void productsExist() {
        Product product1 = new Product("Widget", "A great widget", new BigDecimal("99.99"), 100);
        product1.setId(1L);
        product1.setCategory("Electronics");

        Product product2 = new Product("Gadget", "An amazing gadget", new BigDecimal("149.99"), 50);
        product2.setId(2L);
        product2.setCategory("Electronics");

        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        when(productRepository.findAll()).thenReturn(products);
    }

    @State("products exist in Electronics category")
    void electronicsProductsExist() {
        Product product1 = new Product("Widget", "A great widget", new BigDecimal("99.99"), 100);
        product1.setId(1L);
        product1.setCategory("Electronics");

        Product product2 = new Product("Gadget", "An amazing gadget", new BigDecimal("149.99"), 50);
        product2.setId(2L);
        product2.setCategory("Electronics");

        List<Product> products = new ArrayList<>();
        products.add(product1);
        products.add(product2);

        when(productRepository.findByCategory("Electronics")).thenReturn(products);
    }

    @State("a product with id 1 exists that can be updated")
    void productWithId1CanBeUpdated() {
        Product existingProduct = new Product("Old Widget", "Old description", new BigDecimal("99.99"), 100);
        existingProduct.setId(1L);
        existingProduct.setCategory("Electronics");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @State("a product with id 1 exists for stock update")
    void productWithId1ForStockUpdate() {
        Product product = new Product("Widget", "A great widget", new BigDecimal("99.99"), 100);
        product.setId(1L);
        product.setCategory("Electronics");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @State("a product with id 1 exists and can be deleted")
    void productWithId1CanBeDeleted() {
        Product product = new Product("Widget", "A great widget", new BigDecimal("99.99"), 100);
        product.setId(1L);
        product.setCategory("Electronics");

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).deleteById(1L);
    }
}
