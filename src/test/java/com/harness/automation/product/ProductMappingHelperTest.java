package com.harness.automation.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.helper.ProductMappingHelper;

/**
 * TC-PRD-004
 * Unit tests verifying ProductMappingHelper maps discountPercent bidirectionally.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductMappingHelper Tests")
public class ProductMappingHelperTest {

    private Category buildCategory() {
        return Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/cat.jpg")
                .build();
    }

    private CategoryDto buildCategoryDto() {
        return CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/cat.jpg")
                .build();
    }

    @Test
    @Order(1)
    @DisplayName("TC-PRD-004: map(Product) correctly maps discountPercent to ProductDto")
    void mapProductToDto_withDiscountPercent_shouldMapDiscountPercentCorrectly() {
        Product product = Product.builder()
                .productId(1)
                .productTitle("Laptop Pro")
                .imageUrl("http://example.com/img.jpg")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(5)
                .discountPercent(20.0)
                .category(buildCategory())
                .build();

        ProductDto dto = ProductMappingHelper.map(product);

        assertThat(dto.getDiscountPercent()).isEqualTo(20.0);
        assertThat(dto.getProductId()).isEqualTo(1);
        assertThat(dto.getProductTitle()).isEqualTo("Laptop Pro");
        assertThat(dto.getPriceUnit()).isEqualTo(999.99);
    }

    @Test
    @Order(2)
    @DisplayName("TC-PRD-004: map(ProductDto) correctly maps discountPercent to Product entity")
    void mapDtoToProduct_withDiscountPercent_shouldMapDiscountPercentCorrectly() {
        ProductDto dto = ProductDto.builder()
                .productId(2)
                .productTitle("Gaming Laptop")
                .imageUrl("http://example.com/gl.jpg")
                .sku("GL-001")
                .priceUnit(1499.99)
                .quantity(3)
                .discountPercent(20.0)
                .categoryDto(buildCategoryDto())
                .build();

        Product product = ProductMappingHelper.map(dto);

        assertThat(product.getDiscountPercent()).isEqualTo(20.0);
        assertThat(product.getProductId()).isEqualTo(2);
        assertThat(product.getProductTitle()).isEqualTo("Gaming Laptop");
    }

    @Test
    @Order(3)
    @DisplayName("TC-PRD-004: map(Product) handles null discountPercent without NullPointerException")
    void mapProductToDto_withNullDiscountPercent_shouldHandleNullSafely() {
        Product product = Product.builder()
                .productId(3)
                .productTitle("Budget Laptop")
                .imageUrl("http://example.com/bl.jpg")
                .sku("BL-001")
                .priceUnit(499.99)
                .quantity(10)
                .discountPercent(null)
                .category(buildCategory())
                .build();

        assertThatNoException().isThrownBy(() -> {
            ProductDto dto = ProductMappingHelper.map(product);
            assertThat(dto.getDiscountPercent()).isNull();
        });
    }

    @Test
    @Order(4)
    @DisplayName("TC-PRD-004: map(ProductDto) handles null discountPercent without NullPointerException")
    void mapDtoToProduct_withNullDiscountPercent_shouldHandleNullSafely() {
        ProductDto dto = ProductDto.builder()
                .productId(4)
                .productTitle("Budget Phone")
                .imageUrl("http://example.com/bp.jpg")
                .sku("BP-001")
                .priceUnit(199.99)
                .quantity(15)
                .discountPercent(null)
                .categoryDto(buildCategoryDto())
                .build();

        assertThatNoException().isThrownBy(() -> {
            Product product = ProductMappingHelper.map(dto);
            assertThat(product.getDiscountPercent()).isNull();
        });
    }
}
