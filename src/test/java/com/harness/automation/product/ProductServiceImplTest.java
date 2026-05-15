package com.harness.automation.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;

/**
 * TC-PRD-001, TC-PRD-002
 * Unit tests for ProductServiceImpl covering new searchByTitle and findDiscounted methods.
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("ProductServiceImpl Unit Tests")
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productServiceImpl;

    private Category dummyCategory() {
        return Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .imageUrl("http://example.com/cat.jpg")
                .build();
    }

    private Product buildProduct(Integer id, String title, Double discountPercent) {
        return Product.builder()
                .productId(id)
                .productTitle(title)
                .imageUrl("http://example.com/img.jpg")
                .sku("SKU-" + id)
                .priceUnit(100.0)
                .quantity(10)
                .discountPercent(discountPercent)
                .category(dummyCategory())
                .build();
    }

    // TC-PRD-001: searchByTitle returns matching products case-insensitively

    @Test
    @Order(1)
    @DisplayName("TC-PRD-001: searchByTitle returns matching products for a given title")
    void searchByTitle_withMatchingProducts_shouldReturnFilteredList() {
        Product laptop1 = buildProduct(1, "Laptop Pro", 0.0);
        Product laptop2 = buildProduct(2, "Gaming Laptop", 0.0);
        when(productRepository.findByProductTitleContainingIgnoreCase("laptop"))
                .thenReturn(Arrays.asList(laptop1, laptop2));

        List<?> result = productServiceImpl.searchByTitle("laptop");

        assertThat(result).hasSize(2);
        assertThat(result).extracting("productTitle")
                .allMatch(t -> ((String) t).toLowerCase().contains("laptop"));
    }

    @Test
    @Order(2)
    @DisplayName("TC-PRD-001: searchByTitle returns empty list when no products match")
    void searchByTitle_withNoMatchingProducts_shouldReturnEmptyList() {
        when(productRepository.findByProductTitleContainingIgnoreCase("nonexistent"))
                .thenReturn(Collections.emptyList());

        List<?> result = productServiceImpl.searchByTitle("nonexistent");

        assertThat(result).isEmpty();
    }

    // TC-PRD-002: findDiscounted returns only products with discountPercent > 0

    @Test
    @Order(3)
    @DisplayName("TC-PRD-002: findDiscounted returns only products with discountPercent greater than 0")
    void findDiscounted_withDiscountedProducts_shouldReturnDiscountedList() {
        Product p1 = buildProduct(1, "Sale Laptop", 15.0);
        Product p2 = buildProduct(2, "Discounted Phone", 5.0);
        when(productRepository.findByDiscountPercentGreaterThan(0.0))
                .thenReturn(Arrays.asList(p1, p2));

        List<?> result = productServiceImpl.findDiscounted();

        assertThat(result).hasSize(2);
        assertThat(result).extracting("discountPercent")
                .allMatch(d -> (Double) d > 0.0);
    }

    @Test
    @Order(4)
    @DisplayName("TC-PRD-002: findDiscounted returns empty list when no discounted products exist")
    void findDiscounted_withNoDiscountedProducts_shouldReturnEmptyList() {
        when(productRepository.findByDiscountPercentGreaterThan(0.0))
                .thenReturn(Collections.emptyList());

        List<?> result = productServiceImpl.findDiscounted();

        assertThat(result).isEmpty();
    }
}
