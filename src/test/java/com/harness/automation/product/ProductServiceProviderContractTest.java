package com.harness.automation.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;

/**
 * TC-PRD-008, TC-PRD-009
 * Provider contract tests verifying that product-service responses remain
 * backward-compatible with downstream consumers after the discountPercent field was added.
 *
 * These tests act as lightweight schema-verification tests (no Pact broker required)
 * that verify the response shape consumers depend on is intact.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductService Provider Contract Tests")
public class ProductServiceProviderContractTest {

    private static final String BASE_PATH = "/api/products";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        String host = System.getProperty("product.service.host", "localhost");
        String port = System.getProperty("product.service.port", "8500");
        RestAssured.baseURI = "http://" + host + ":" + port;
        RestAssured.basePath = BASE_PATH;
    }

    // TC-PRD-008: Single product response schema

    @Test
    @Order(1)
    @DisplayName("TC-PRD-008: GET /api/products/{id} response schema is backward-compatible with consumers")
    void singleProduct_responseSchema_shouldContainAllConsumerRequiredFields() {
        List<Map<String, Object>> allProducts = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        assertThat(allProducts).isNotNull();
        if (!allProducts.isEmpty()) {
            Integer productId = (Integer) allProducts.get(0).get("productId");

            given()
                .accept(ContentType.JSON)
            .when()
                .get("/" + productId)
            .then()
                .statusCode(HttpStatus.OK.value())
                // Fields required by shipping-service, favourite-service, proxy-client consumers
                .body("$", hasKey("productId"))
                .body("$", hasKey("productTitle"))
                .body("$", hasKey("imageUrl"))
                .body("$", hasKey("sku"))
                .body("$", hasKey("priceUnit"))
                .body("$", hasKey("quantity"))
                // New additive field — must be present
                .body("$", hasKey("discountPercent"));
        }
    }

    @Test
    @Order(2)
    @DisplayName("TC-PRD-008: discountPercent is present in single product response as a numeric or null value")
    void singleProduct_discountPercentField_shouldBePresentAndNumericOrNull() throws Exception {
        List<Map<String, Object>> allProducts = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        if (allProducts != null && !allProducts.isEmpty()) {
            Integer productId = (Integer) allProducts.get(0).get("productId");

            String responseBody = given()
                .accept(ContentType.JSON)
            .when()
                .get("/" + productId)
            .then()
                .statusCode(HttpStatus.OK.value())
                .extract().body().asString();

            JsonNode root = objectMapper.readTree(responseBody);
            assertThat(root.has("discountPercent")).isTrue();
            JsonNode discountNode = root.get("discountPercent");
            assertThat(discountNode.isNull() || discountNode.isNumber())
                    .as("discountPercent should be null or a number").isTrue();
        }
    }

    // TC-PRD-009: Collection endpoint schema

    @Test
    @Order(3)
    @DisplayName("TC-PRD-009: GET /api/products collection response includes discountPercent per item")
    void productCollection_responseSchema_shouldContainDiscountPercentInEachItem() {
        List<Map<String, Object>> products = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        assertThat(products).isNotNull();
        if (!products.isEmpty()) {
            products.forEach(product -> {
                assertThat(product).containsKey("productId");
                assertThat(product).containsKey("productTitle");
                assertThat(product).containsKey("sku");
                assertThat(product).containsKey("priceUnit");
                assertThat(product).containsKey("quantity");
                assertThat(product).containsKey("discountPercent");
            });
        }
    }

    @Test
    @Order(4)
    @DisplayName("TC-PRD-009: GET /api/products all pre-existing fields are intact in collection response")
    void productCollection_preExistingFields_shouldRemainUnchanged() {
        List<Map<String, Object>> products = given()
            .accept(ContentType.JSON)
        .when()
            .get("")
        .then()
            .statusCode(HttpStatus.OK.value())
            .extract().jsonPath().getList("collection");

        assertThat(products).isNotNull();
        if (!products.isEmpty()) {
            Map<String, Object> firstProduct = products.get(0);
            assertThat(firstProduct.get("productId")).isNotNull();
            assertThat(firstProduct.get("productTitle")).isNotNull();
            assertThat(firstProduct.get("sku")).isNotNull();
            assertThat(firstProduct.get("priceUnit")).isNotNull();
            assertThat(firstProduct.get("quantity")).isNotNull();
        }
    }
}
