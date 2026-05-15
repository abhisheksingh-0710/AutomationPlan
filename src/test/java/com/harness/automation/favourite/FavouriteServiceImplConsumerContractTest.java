package com.harness.automation.favourite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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
 * TC-FAV-001
 * Consumer contract test verifying favourite-service's ProductDto deserialization
 * handles the new discountPercent field added to product-service responses.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("favourite-service Consumer Contract Tests — ProductDto with discountPercent")
public class FavouriteServiceImplConsumerContractTest {

    private WireMockServer wireMockServer;
    private RestTemplate restTemplate;

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
            "{\"productId\":1,\"productTitle\":\"Wireless Mouse\",\"imageUrl\":\"http://example.com/mouse.jpg\","
            + "\"sku\":\"WM-001\",\"priceUnit\":49.99,\"quantity\":20,\"discountPercent\":%.1f}",
            discountPercent);
    }

    @Test
    @Order(1)
    @DisplayName("TC-FAV-001: favourite-service deserializes ProductDto with discountPercent without exception")
    void favouriteService_productDtoDeserialization_withDiscountPercent_shouldSucceed() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(5.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";

        assertThatNoException().isThrownBy(() -> {
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            assertThat(productDto).isNotNull();
            assertThat(productDto.getProductId()).isEqualTo(1);
            assertThat(productDto.getProductTitle()).isEqualTo("Wireless Mouse");
        });
    }

    @Test
    @Order(2)
    @DisplayName("TC-FAV-001: favourite-service ProductDto retains all pre-existing fields after product-service enrichment")
    void favouriteService_productDtoDeserialization_shouldPreserveExistingFields() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(5.0))));

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
    @DisplayName("TC-FAV-001: favourite-service favourite entity is constructed successfully with enriched ProductDto")
    void favouriteService_withEnrichedProductDto_shouldConstructFavouriteEntitySuccessfully() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJsonWithDiscount(5.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";
        ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);

        assertThat(productDto).isNotNull();
        // Simulate favourite entity construction — verify all fields needed by FavouriteMappingHelper are present
        assertThat(productDto.getProductId()).isEqualTo(1);
        assertThat(productDto.getProductTitle()).isEqualTo("Wireless Mouse");
        assertThat(productDto.getPriceUnit()).isEqualTo(49.99);
        assertThat(productDto.getQuantity()).isEqualTo(20);
    }
}
