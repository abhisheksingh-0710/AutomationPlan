package com.harness.automation.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.ProductDto;

/**
 * TC-PRD-003
 * Unit tests verifying ProductDto serialization/deserialization of the new discountPercent field.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductDto Serialization Tests")
public class ProductDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @Order(1)
    @DisplayName("TC-PRD-003: ProductDto serializes discountPercent field with non-null value")
    void productDto_withDiscountPercent_shouldSerializeDiscountPercentField() throws Exception {
        ProductDto dto = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(5)
                .discountPercent(15.5)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"discountPercent\"");
        assertThat(json).contains("15.5");
    }

    @Test
    @Order(2)
    @DisplayName("TC-PRD-003: ProductDto serializes discountPercent as null when not set")
    void productDto_withNullDiscountPercent_shouldSerializeNullDiscountPercentField() throws Exception {
        ProductDto dto = ProductDto.builder()
                .productId(2)
                .productTitle("Monitor")
                .sku("MON-001")
                .priceUnit(299.99)
                .quantity(3)
                .discountPercent(null)
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"discountPercent\"");
    }

    @Test
    @Order(3)
    @DisplayName("TC-PRD-003: ProductDto deserializes discountPercent field from JSON correctly")
    void productDto_withDiscountPercentJson_shouldDeserializeCorrectly() throws Exception {
        String json = "{\"productId\":3,\"productTitle\":\"Keyboard\",\"imageUrl\":null,"
                + "\"sku\":\"KEY-001\",\"priceUnit\":49.99,\"quantity\":20,"
                + "\"discountPercent\":20.0,\"category\":null}";

        ProductDto dto = objectMapper.readValue(json, ProductDto.class);

        assertThat(dto.getDiscountPercent()).isEqualTo(20.0);
        assertThat(dto.getProductId()).isEqualTo(3);
        assertThat(dto.getProductTitle()).isEqualTo("Keyboard");
    }

    @Test
    @Order(4)
    @DisplayName("TC-PRD-003: ProductDto round-trips discountPercent through serialization and deserialization")
    void productDto_roundTrip_shouldPreserveDiscountPercent() throws Exception {
        ProductDto original = ProductDto.builder()
                .productId(4)
                .productTitle("Headphones")
                .sku("HEAD-001")
                .priceUnit(150.0)
                .quantity(8)
                .discountPercent(10.0)
                .build();

        String json = objectMapper.writeValueAsString(original);
        ProductDto deserialized = objectMapper.readValue(json, ProductDto.class);

        assertThat(deserialized.getDiscountPercent()).isEqualTo(original.getDiscountPercent());
    }
}
