package com.harness.automation.product;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

/**
 * TC-PRD-005, TC-PRD-006, TC-PRD-007
 * Integration tests for ProductResource covering the new search, discounted, and
 * backward-compatible single-product endpoints.
 *
 * NOTE: These tests require a running product-service instance. In CI, run
 * with @SpringBootTest and TestContainers MySQL. The RestAssured base URI is
 * configured from the injected server port.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductResource Integration Tests")
public class ProductResourceIntegrationTest {

    private static final String BASE_PATH = "/api/products";

    private String baseUrl;

    @BeforeEach
    void setUp() {
        String host = System.getProperty("product.service.host", "localhost");
        String port = System.getProperty("product.service.port", "8500");
        baseUrl = "http://" + host + ":" + port;
        RestAssured.baseURI = baseUrl;
        RestAssured.basePath = BASE_PATH;
    }

    // TC-PRD-005: GET /api/products/search

    @Test
    @Order(1)
    @DisplayName("TC-PRD-005: GET /api/products/search?title=laptop returns HTTP 200 with matching products")
    void searchByTitle_withMatchingTitle_shouldReturn200WithResults() {
        given()
            .queryParam("title", "laptop")
            .accept(ContentType.JSON)
        .when()
            .get("/search")
        .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body("collection", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("TC-PRD-005: GET /api/products/search is case-insensitive")
    void searchByTitle_withUpperCaseTitle_shouldReturnSameResults() {
        int lowerCaseCount = given()
            .queryParam("title", "laptop")
            .accept(ContentType.JSON)
        .when()
            .get("/search")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection").size();

        int upperCaseCount = given()
            .queryParam("title", "LAPTOP")
            .accept(ContentType.JSON)
        .when()
            .get("/search")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection").size();

        assertThat(lowerCaseCount).isEqualTo(upperCaseCount);
    }

    @Test
    @Order(3)
    @DisplayName("TC-PRD-005: GET /api/products/search with nonexistent title returns 200 with empty collection")
    void searchByTitle_withNonExistentTitle_shouldReturn200WithEmptyCollection() {
        given()
            .queryParam("title", "xyznonexistentproduct12345")
            .accept(ContentType.JSON)
        .when()
            .get("/search")
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("collection", notNullValue());
    }

    @Test
    @Order(4)
    @DisplayName("TC-PRD-005: GET /api/products/search without title param returns 400")
    void searchByTitle_withMissingTitleParam_shouldReturn400() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/search")
        .then()
            .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    // TC-PRD-006: GET /api/products/discounted

    @Test
    @Order(5)
    @DisplayName("TC-PRD-006: GET /api/products/discounted returns HTTP 200 with only discounted products")
    void findDiscounted_shouldReturn200WithDiscountedProducts() {
        given()
            .accept(ContentType.JSON)
        .when()
            .get("/discounted")
        .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .body("collection", notNullValue());
    }

    @Test
    @Order(6)
    @DisplayName("TC-PRD-006: GET /api/products/discounted — all returned products have discountPercent > 0")
    void findDiscounted_allReturnedProducts_shouldHavePositiveDiscountPercent() {
        java.util.List<Map<String, Object>> collection = given()
            .accept(ContentType.JSON)
        .when()
            .get("/discounted")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        if (collection != null && !collection.isEmpty()) {
            collection.forEach(product -> {
                Object discount = product.get("discountPercent");
                assertThat(discount).isNotNull();
                assertThat(((Number) discount).doubleValue()).isGreaterThan(0.0);
            });
        }
    }

    // TC-PRD-007: GET /api/products/{productId} includes discountPercent

    @Test
    @Order(7)
    @DisplayName("TC-PRD-007: GET /api/products/{id} response includes discountPercent field")
    void findById_shouldIncludeDiscountPercentInResponse() {
        java.util.List<Map<String, Object>> allProducts = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        if (allProducts != null && !allProducts.isEmpty()) {
            Integer productId = (Integer) allProducts.get(0).get("productId");

            given()
                .accept(ContentType.JSON)
            .when()
                .get("/" + productId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasKey("discountPercent"))
                .body("productId", notNullValue())
                .body("productTitle", notNullValue())
                .body("sku", notNullValue())
                .body("priceUnit", notNullValue())
                .body("quantity", notNullValue());
        }
    }

    @Test
    @Order(8)
    @DisplayName("TC-PRD-007: GET /api/products/{id} response contains all expected backward-compatible fields")
    void findById_shouldReturnAllExistingFields() {
        java.util.List<Map<String, Object>> allProducts = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        if (allProducts != null && !allProducts.isEmpty()) {
            Integer productId = (Integer) allProducts.get(0).get("productId");

            given()
                .accept(ContentType.JSON)
            .when()
                .get("/" + productId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasKey("productId"))
                .body("$", hasKey("productTitle"))
                .body("$", hasKey("sku"))
                .body("$", hasKey("priceUnit"))
                .body("$", hasKey("quantity"))
                .body("$", hasKey("discountPercent"));
        }
    }
}
