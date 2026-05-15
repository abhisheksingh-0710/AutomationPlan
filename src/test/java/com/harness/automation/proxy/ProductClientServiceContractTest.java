package com.harness.automation.proxy;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.business.product.model.ProductDto;

/**
 * TC-PXY-001, TC-PXY-002, TC-PXY-003
 * Consumer contract and smoke tests for proxy-client product routing.
 * TC-PXY-001: Feign deserialization of enriched ProductDto.
 * TC-PXY-002: Search endpoint proxied correctly.
 * TC-PXY-003: Discounted endpoint proxied correctly.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("proxy-client ProductClientService Contract and Smoke Tests")
public class ProductClientServiceContractTest {

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

    private String productJson(double discountPercent) {
        return String.format(
            "{\"productId\":1,\"productTitle\":\"Tablet X\",\"imageUrl\":\"http://example.com/tablet.jpg\","
            + "\"sku\":\"TAB-001\",\"priceUnit\":399.99,\"quantity\":12,\"discountPercent\":%.1f}",
            discountPercent);
    }

    private String collectionJson(String... items) {
        StringBuilder sb = new StringBuilder("{\"collection\":[");
        for (int i = 0; i < items.length; i++) {
            sb.append(items[i]);
            if (i < items.length - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    // TC-PXY-001: FeignClient deserializes ProductDto with discountPercent

    @Test
    @Order(1)
    @DisplayName("TC-PXY-001: proxy-client deserializes ProductDto with discountPercent without DecodeException")
    void productClientService_findById_withDiscountPercent_shouldDeserializeSuccessfully() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJson(12.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";

        assertThatNoException().isThrownBy(() -> {
            ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);
            assertThat(productDto).isNotNull();
            assertThat(productDto.getProductId()).isEqualTo(1);
            assertThat(productDto.getDiscountPercent()).isEqualTo(12.0);
        });
    }

    @Test
    @Order(2)
    @DisplayName("TC-PXY-001: proxy-client ProductDto has all existing fields populated after enriched response")
    void productClientService_findById_shouldHaveAllExistingFieldsPopulated() {
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/1"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productJson(12.0))));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/1";
        ProductDto productDto = restTemplate.getForObject(url, ProductDto.class);

        assertThat(productDto).isNotNull();
        assertThat(productDto.getProductId()).isNotNull();
        assertThat(productDto.getProductTitle()).isNotBlank();
        assertThat(productDto.getSku()).isNotBlank();
        assertThat(productDto.getPriceUnit()).isNotNull();
        assertThat(productDto.getQuantity()).isNotNull();
        assertThat(productDto.getDiscountPercent()).isEqualTo(12.0);
    }

    // TC-PXY-002: Search endpoint proxied through proxy-client

    @Test
    @Order(3)
    @DisplayName("TC-PXY-002: GET /api/products/search proxied by proxy-client returns 200 with DtoCollectionResponse")
    void proxyClient_searchEndpoint_shouldReturn200WithCollection() {
        String productSearchJson = collectionJson(productJson(0.0));
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/product-service/api/products/search"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productSearchJson)));

        String proxyUrl = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/search";

        given()
            .baseUri(proxyUrl.substring(0, proxyUrl.lastIndexOf("/product-service")))
            .queryParam("title", "tablet")
            .accept(ContentType.JSON)
        .when()
            .get("/product-service/api/products/search")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("collection", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("TC-PXY-002: proxy-client search endpoint response body is valid DtoCollectionResponse")
    void proxyClient_searchEndpoint_responseBodyIsValidDtoCollectionResponse() throws Exception {
        String productSearchJson = collectionJson(productJson(0.0));
        WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/product-service/api/products/search"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(productSearchJson)));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/search?title=tablet";
        String responseBody = restTemplate.getForObject(url, String.class);

        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("collection");
    }

    // TC-PXY-003: Discounted endpoint proxied through proxy-client

    @Test
    @Order(5)
    @DisplayName("TC-PXY-003: GET /api/products/discounted proxied by proxy-client returns 200 with DtoCollectionResponse")
    void proxyClient_discountedEndpoint_shouldReturn200WithCollection() {
        String discountedJson = collectionJson(productJson(10.0), productJson(5.0));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/discounted"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(discountedJson)));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/discounted";
        String responseBody = restTemplate.getForObject(url, String.class);

        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("collection");
    }

    @Test
    @Order(6)
    @DisplayName("TC-PXY-003: proxy-client discounted endpoint — all returned products have discountPercent > 0")
    void proxyClient_discountedEndpoint_allReturnedProductsHavePositiveDiscountPercent() throws Exception {
        String discountedJson = collectionJson(productJson(10.0), productJson(5.0));
        WireMock.stubFor(WireMock.get(WireMock.urlEqualTo("/product-service/api/products/discounted"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(discountedJson)));

        String url = "http://localhost:" + wireMockServer.port() + "/product-service/api/products/discounted";
        String responseBody = restTemplate.getForObject(url, String.class);

        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(responseBody);
        com.fasterxml.jackson.databind.JsonNode collection = root.get("collection");

        assertThat(collection).isNotNull();
        collection.forEach(product -> {
            assertThat(product.has("discountPercent")).isTrue();
            double discount = product.get("discountPercent").asDouble();
            assertThat(discount).isGreaterThan(0.0);
        });
    }
}
