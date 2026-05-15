package com.harness.automation.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.dto.ProductDto;

/**
 * TC-SHP-001
 * Consumer contract test verifying shipping-service's ProductDto deserialization
 * handles the new discountPercent field added to product-service responses.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("shipping-service Consumer Contract Tests — ProductDto with discountPercent")
public class OrderItemServiceImplConsumerContractTest {

    private WireMockServer wireMockServer;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
        restTemplate = new RestTemplate();
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    private String productJsonWithDiscount(double discountPercent) {
        return String.format(
            "{\"productId\":1,\"productTitle\":\"Laptop Pro\",\"imageUrl\":\"http://example.com/img.jpg\","
            + "\"sku\":\"LAP-001\",\"priceUnit\":999.99,\"quantity\":5,\"discountPercent\":%.1f}",
            discountPercent);
    }

    @Test
    @Order(1)
    @DisplayName("TC-SHP-001: shipping-service deserializes ProductDto including discountPercent without exception")
    void shippingService_productDtoDeserialization_withDiscountPercent_shouldSucceedWithoutException() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(15.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";

        assertThatNoException().isThrownBy(() -> {
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            assertThat(productDto).isNotNull();
            assertThat(productDto.getProductId()).isEqualTo(1);
            assertThat(productDto.getProductTitle()).isEqualTo("Laptop Pro");
            assertThat(productDto.getSku()).isEqualTo("LAP-001");
            assertThat(productDto.getPriceUnit()).isEqualTo(999.99);
            assertThat(productDto.getQuantity()).isEqualTo(5);
        });
    }

    @Test
    @Order(2)
    @DisplayName("TC-SHP-001: shipping-service ProductDto preserves all pre-existing fields after enrichment")
    void shippingService_productDtoDeserialization_shouldPreserveAllExistingFields() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(15.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";
        ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);

        assertThat(productDto).isNotNull();
        assertThat(productDto.getProductId()).isNotNull();
        assertThat(productDto.getProductTitle()).isNotBlank();
        assertThat(productDto.getSku()).isNotBlank();
        assertThat(productDto.getPriceUnit()).isNotNull();
        assertThat(productDto.getQuantity()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("TC-SHP-001: shipping-service ProductDto deserialization succeeds when discountPercent is zero")
    void shippingService_productDtoDeserialization_withZeroDiscount_shouldSucceed() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/2"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(0.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/2";

        assertThatNoException().isThrownBy(() -> {
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            assertThat(productDto).isNotNull();
        });
    }
}
