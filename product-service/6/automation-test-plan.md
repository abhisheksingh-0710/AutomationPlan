# Automation Test Plan
**PR:** #6 | **Branch:** feat/product-search-and-discount | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/abhisheksingh-0710/ecommerce-microservice-backend-app/pull/6
**Generated:** 2026-05-15T00:00:00Z
**Total Test Cases:** 15

---

## Test Plan Summary

| Service | Role | Test Types | Test Case Count | Priority |
|---------|------|------------|-----------------|----------|
| product-service | Directly Changed | unit, integration, provider-contract | 9 | P1 |
| shipping-service | Downstream Dependent | consumer-contract | 1 | P1 |
| favourite-service | Downstream Dependent | consumer-contract | 1 | P1 |
| proxy-client | Downstream Dependent | consumer-contract, smoke | 3 | P1–P2 |

---

## Test Cases by Service

---

### product-service

#### TC-PRD-001: searchByTitle returns matching products case-insensitively

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-001 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Product search by title |
| **Trigger** | New `searchByTitle(String title)` method added to `ProductServiceImpl` |
| **Preconditions** | `ProductRepository` mock configured with stubbed `findByProductTitleContainingIgnoreCase` results |
| **Test Steps** | 1. Mock `productRepository.findByProductTitleContainingIgnoreCase("laptop")` to return `[Product("Laptop Pro"), Product("Gaming Laptop")]`<br>2. Call `productServiceImpl.searchByTitle("laptop")`<br>3. Assert returned list size is 2<br>4. Assert both product titles contain "laptop" (case-insensitive)<br>5. Mock returns empty list; call `searchByTitle("nonexistent")` — assert empty list returned |
| **Expected Result** | Correct filtered list returned; empty list handled gracefully without exception |
| **Automation Notes** | Class: `ProductServiceImplTest.java` in `product-service/src/test/java/.../service/impl/`; use `@ExtendWith(MockitoExtension.class)`; mock `ProductRepository` with `@Mock` |

---

#### TC-PRD-002: findDiscounted returns only products with discountPercent > 0

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-002 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Discounted product listing |
| **Trigger** | New `findDiscounted()` method added to `ProductServiceImpl` |
| **Preconditions** | `ProductRepository` mock configured with stubbed `findByDiscountPercentGreaterThan(0.0)` |
| **Test Steps** | 1. Mock `productRepository.findByDiscountPercentGreaterThan(0.0)` to return `[Product(discountPercent=15.0), Product(discountPercent=5.0)]`<br>2. Call `productServiceImpl.findDiscounted()`<br>3. Assert returned list size is 2<br>4. Assert all items have `discountPercent > 0.0`<br>5. Mock returns empty list — assert empty list returned without exception |
| **Expected Result** | Only products with `discountPercent > 0` returned |
| **Automation Notes** | Class: `ProductServiceImplTest.java`; add alongside TC-PRD-001 in same test class |

---

#### TC-PRD-003: ProductDto serializes discountPercent field correctly

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-003 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Product DTO serialization |
| **Trigger** | `discountPercent` (Double) field added to `ProductDto` |
| **Preconditions** | Jackson `ObjectMapper` available |
| **Test Steps** | 1. Build `ProductDto` with `discountPercent = 15.5`; serialize to JSON string<br>2. Assert serialized JSON contains key `"discountPercent"` with value `15.5`<br>3. Build `ProductDto` with `discountPercent = null`; serialize to JSON<br>4. Assert `"discountPercent"` key is present with `null` value (not omitted, since `@JsonInclude` is not on this field)<br>5. Deserialize JSON `{"discountPercent": 20.0, ...}` into `ProductDto` — assert `getDiscountPercent()` returns `20.0` |
| **Expected Result** | `discountPercent` round-trips through serialization/deserialization correctly |
| **Automation Notes** | Class: `ProductDtoTest.java` in `product-service/src/test/java/.../dto/`; use `new ObjectMapper()` |

---

#### TC-PRD-004: ProductMappingHelper maps discountPercent bidirectionally

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-004 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Entity-DTO mapping |
| **Trigger** | `discountPercent` wired into `ProductMappingHelper.map()` in both directions |
| **Preconditions** | Valid `Product` entity and `ProductDto` instances with associated `Category` |
| **Test Steps** | 1. Build `Product` with `discountPercent = 20.0`; call `ProductMappingHelper.map(product)` — assert `productDto.getDiscountPercent() == 20.0`<br>2. Build `ProductDto` with `discountPercent = 20.0`; call `ProductMappingHelper.map(productDto)` — assert `product.getDiscountPercent() == 20.0`<br>3. Test with `discountPercent = null` in both directions — assert null handled without NullPointerException |
| **Expected Result** | `discountPercent` mapped correctly in both `Product → ProductDto` and `ProductDto → Product` paths; null-safe |
| **Automation Notes** | Class: `ProductMappingHelperTest.java` in `product-service/src/test/java/.../helper/` |

---

#### TC-PRD-005: GET /api/products/search returns filtered product list

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-005 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Product search API end-to-end |
| **Trigger** | New `GET /api/products/search` endpoint added to `ProductResource` |
| **Preconditions** | product-service running with TestContainers (H2/MySQL); DB seeded with products: `['Laptop Pro', 'Gaming Laptop', 'Desk Chair']` |
| **Test Steps** | 1. `GET /api/products/search?title=laptop` — assert HTTP 200<br>2. Assert response body is `DtoCollectionResponse` containing exactly 2 products<br>3. Assert both product titles contain "laptop" (case-insensitive)<br>4. `GET /api/products/search?title=LAPTOP` — assert same 2 results (case-insensitive)<br>5. `GET /api/products/search?title=nonexistent` — assert HTTP 200 with empty collection<br>6. `GET /api/products/search` (missing `title` param) — assert HTTP 400 |
| **Expected Result** | Correct filtered results; case-insensitive; empty result graceful; missing param returns 400 |
| **Automation Notes** | Class: `ProductResourceIntegrationTest.java` in `product-service/src/test/java/.../resource/`; use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`; use `@Testcontainers` with MySQL container |

---

#### TC-PRD-006: GET /api/products/discounted returns only discounted products

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-006 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Discounted product listing API end-to-end |
| **Trigger** | New `GET /api/products/discounted` endpoint added to `ProductResource` |
| **Preconditions** | product-service running with TestContainers; DB seeded with 2 products (`discountPercent=10.0`, `discountPercent=5.0`) and 1 product (`discountPercent=0.0`) |
| **Test Steps** | 1. `GET /api/products/discounted` — assert HTTP 200<br>2. Assert response collection contains exactly 2 products<br>3. Assert all returned products have `discountPercent > 0`<br>4. Assert the `discountPercent=0.0` product is NOT in the response |
| **Expected Result** | Only products with active discount returned |
| **Automation Notes** | Add to `ProductResourceIntegrationTest.java`; share TestContainers setup with TC-PRD-005 |

---

#### TC-PRD-007: GET /api/products/{productId} response includes discountPercent field

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-007 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Single product retrieval |
| **Trigger** | `discountPercent` field added to `ProductDto` changes response shape of pre-existing `GET /api/products/{productId}` endpoint |
| **Preconditions** | product-service running with TestContainers; DB seeded with product having `discountPercent=10.0` |
| **Test Steps** | 1. `GET /api/products/{id}` with valid product ID — assert HTTP 200<br>2. Assert JSON response contains key `"discountPercent"` with value `10.0`<br>3. Assert all previously-present fields (`productId`, `productTitle`, `sku`, `priceUnit`, `quantity`, `category`) still present with correct values |
| **Expected Result** | Response is backward-compatible: all existing fields present plus new `discountPercent` |
| **Automation Notes** | Add to `ProductResourceIntegrationTest.java`; use `JsonPath` assertions |

---

#### TC-PRD-008: Provider contract — GET /api/products/{productId} schema backward-compatible

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-008 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Product retrieval contract |
| **Trigger** | `discountPercent` field added to `ProductDto` changes the serialized response schema |
| **Preconditions** | Pact broker available (or local pact files); product-service running in test mode |
| **Test Steps** | 1. Load existing Pact contracts for `GET /api/products/{productId}` from all known consumers (shipping-service, favourite-service, proxy-client)<br>2. Run Pact provider verification (`@PactVerification`) against running product-service<br>3. Assert all previously-defined consumer interactions still satisfied<br>4. Assert new `discountPercent` field present in response body for all product responses |
| **Expected Result** | All existing consumer contracts still satisfied; `discountPercent` is present as an additive field |
| **Automation Notes** | Class: `ProductServiceProviderPactTest.java` in `product-service/src/test/java/.../pact/`; use `@Provider("product-service")` + `@PactBroker` or local pact files; `@SpringBootTest` with TestContainers |

---

#### TC-PRD-009: Provider contract — GET /api/products collection includes discountPercent per item

| Field | Value |
|-------|-------|
| **ID** | TC-PRD-009 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Product listing contract |
| **Trigger** | `discountPercent` field added to `ProductDto` affects collection response from `GET /api/products` |
| **Preconditions** | Pact broker/files available; product-service running with test data |
| **Test Steps** | 1. Execute Pact provider verification for `GET /api/products` interaction<br>2. Assert response array has at least one item<br>3. Assert each item in collection contains `"discountPercent"` field<br>4. Assert all pre-existing fields in each item unchanged |
| **Expected Result** | Collection endpoint response backward-compatible; every product object contains `discountPercent` |
| **Automation Notes** | Add as separate `@PactVerification` interaction in `ProductServiceProviderPactTest.java` |

---

### shipping-service

#### TC-SHP-001: OrderItemServiceImpl parses ProductDto response with new discountPercent field

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Order item creation — product lookup |
| **Trigger** | `discountPercent` field added to `ProductDto` response from product-service; `OrderItemServiceImpl` calls `GET /product-service/api/products/{productId}` |
| **Preconditions** | WireMock or Pact mock server stubbing product-service; shipping-service application context loaded |
| **Test Steps** | 1. Stub `GET /product-service/api/products/1` to return JSON including `"discountPercent": 15.0` alongside all existing fields<br>2. Invoke `OrderItemServiceImpl` method that fetches a product by ID<br>3. Assert no `UnrecognizedPropertyException` or `HttpMessageConversionException` thrown<br>4. Assert all existing product fields (productId, productTitle, sku, priceUnit, quantity) still correctly populated on the deserialized object<br>5. (If shipping-service maps discountPercent) assert `discountPercent` value accessible |
| **Expected Result** | RestTemplate deserialization succeeds; enriched response does not break shipping-service product fetch |
| **Automation Notes** | Class: `OrderItemServiceImplPactConsumerTest.java` in `shipping-service/src/test/java/.../service/impl/`; use `@ExtendWith(PactConsumerTestExt.class)` and `@PactTestFor(providerName = "product-service")`; alternatively use `WireMockExtension` |

---

### favourite-service

#### TC-FAV-001: FavouriteServiceImpl parses enriched ProductDto with discountPercent

| Field | Value |
|-------|-------|
| **ID** | TC-FAV-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Favourite retrieval — product lookup |
| **Trigger** | `discountPercent` field added to `ProductDto` response; `FavouriteServiceImpl` calls `GET /product-service/api/products/{productId}` |
| **Preconditions** | WireMock or Pact mock server stubbing product-service; favourite-service application context loaded |
| **Test Steps** | 1. Stub `GET /product-service/api/products/1` to return JSON including `"discountPercent": 5.0` alongside all existing fields<br>2. Invoke `FavouriteServiceImpl` method that fetches product by ID during favourite retrieval<br>3. Assert no deserialization exception thrown<br>4. Assert favourite entity constructed successfully<br>5. Assert existing product fields (productId, productTitle, priceUnit, quantity) still correctly populated |
| **Expected Result** | RestTemplate deserialization succeeds; favourite-service handles enriched ProductDto without error |
| **Automation Notes** | Class: `FavouriteServiceImplPactConsumerTest.java` in `favourite-service/src/test/java/.../service/impl/`; use Pact consumer test or WireMock |

---

### proxy-client

#### TC-PXY-001: ProductClientService FeignClient deserializes ProductDto with discountPercent

| Field | Value |
|-------|-------|
| **ID** | TC-PXY-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Proxy product fetch |
| **Trigger** | `discountPercent` field added to `ProductDto`; `ProductClientService` FeignClient calls `GET /product-service/api/products/{productId}` |
| **Preconditions** | Pact mock server or WireMock stubbing product-service responses; proxy-client Feign configuration loaded |
| **Test Steps** | 1. Stub product-service `GET /product-service/api/products/1` to return JSON with `"discountPercent": 12.0` and all existing product fields<br>2. Invoke `ProductClientService.findById(1)` (or equivalent FeignClient method)<br>3. Assert Feign decoding succeeds — no `DecodeException` or `FeignException` thrown<br>4. Assert returned `ProductDto` has `discountPercent == 12.0`<br>5. Assert all existing fields still populated correctly |
| **Expected Result** | FeignClient deserializes enriched ProductDto successfully; discountPercent accessible |
| **Automation Notes** | Class: `ProductClientServicePactConsumerTest.java` in `proxy-client/src/test/java/.../client/`; use `@ExtendWith(PactConsumerTestExt.class)` with Pact mock provider |

---

#### TC-PXY-002: GET /app/api/products/search proxied correctly through proxy-client

| Field | Value |
|-------|-------|
| **ID** | TC-PXY-002 |
| **Type** | smoke |
| **Priority** | P2 |
| **Flow** | End-to-end product search via proxy |
| **Trigger** | New `GET /api/products/search` endpoint added to product-service; must be reachable through proxy-client's wildcard FeignClient mapping |
| **Preconditions** | Both proxy-client and product-service running; Eureka service registry up; at least one product seeded in DB |
| **Test Steps** | 1. Call `GET /app/api/products/search?title=test` through proxy-client (port 8080)<br>2. Assert HTTP 200 returned<br>3. Assert response body is valid `DtoCollectionResponse<ProductDto>`<br>4. Assert no routing error (404, 500) from proxy-client layer |
| **Expected Result** | New endpoint fully routable through proxy-client; response proxied without error |
| **Automation Notes** | Class: `ProductProxySmokeTest.java` in `proxy-client/src/test/java/.../smoke/`; use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate`; requires full stack (use Docker Compose or testcontainers-compose) |

---

#### TC-PXY-003: GET /app/api/products/discounted proxied correctly through proxy-client

| Field | Value |
|-------|-------|
| **ID** | TC-PXY-003 |
| **Type** | smoke |
| **Priority** | P2 |
| **Flow** | End-to-end discounted products listing via proxy |
| **Trigger** | New `GET /api/products/discounted` endpoint added to product-service; must be reachable through proxy-client's wildcard FeignClient mapping |
| **Preconditions** | Both proxy-client and product-service running; Eureka up; at least one product with `discountPercent > 0` seeded |
| **Test Steps** | 1. Call `GET /app/api/products/discounted` through proxy-client (port 8080)<br>2. Assert HTTP 200<br>3. Assert response body is valid `DtoCollectionResponse<ProductDto>`<br>4. Assert each product in response has `discountPercent > 0` |
| **Expected Result** | New discounted endpoint routable through proxy-client; response passes through correctly |
| **Automation Notes** | Add to `ProductProxySmokeTest.java`; share test infrastructure with TC-PXY-002 |

---

## Test Execution Order

1. **P0 tests first** — No P0 (breaking change) tests in this PR; skip to P1.
2. **Unit tests** (TC-PRD-001 through TC-PRD-004) — run in parallel; fastest feedback on logic correctness.
3. **Provider-contract tests** (TC-PRD-008, TC-PRD-009) — run before consumer-contract tests so provider behaviour is verified first.
4. **Integration tests** (TC-PRD-005, TC-PRD-006, TC-PRD-007) — run after unit tests; require DB via TestContainers.
5. **Consumer-contract tests** (TC-SHP-001, TC-FAV-001, TC-PXY-001) — run after provider-contract tests pass.
6. **Smoke tests** (TC-PXY-002, TC-PXY-003) — run last; require full service stack.

---

## Automation Framework Notes

### Unit & Integration Tests
- **Framework:** JUnit 5 (`junit-jupiter`) + Mockito (`mockito-core`, `mockito-junit-jupiter`) + Spring Boot Test (`@SpringBootTest`)
- **DB isolation:** Use `@Testcontainers` + `@Container` with `MySQLContainer` or H2 in-memory for integration tests
- **HTTP assertions:** Use `TestRestTemplate` or `MockMvc` + `JsonPath`

### Contract Tests
- **Framework:** Pact JVM (`au.com.dius.pact.provider:junit5` for provider, `au.com.dius.pact.consumer:junit5` for consumer)
- **Provider tests:** `@Provider("product-service")` + `@PactBroker` annotation or local pact files in `src/test/resources/pacts/`
- **Consumer tests:** `@Consumer("shipping-service")` / `@Consumer("favourite-service")` / `@Consumer("proxy-client")` + `@PactTestFor`

### Smoke Tests
- **Framework:** `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
- **Stack:** Docker Compose or Testcontainers Compose for full-stack smoke tests

### Suggested File Paths
| Test ID | File Path |
|---------|-----------|
| TC-PRD-001, TC-PRD-002 | `product-service/src/test/java/com/selimhorri/app/service/impl/ProductServiceImplTest.java` |
| TC-PRD-003 | `product-service/src/test/java/com/selimhorri/app/dto/ProductDtoTest.java` |
| TC-PRD-004 | `product-service/src/test/java/com/selimhorri/app/helper/ProductMappingHelperTest.java` |
| TC-PRD-005, TC-PRD-006, TC-PRD-007 | `product-service/src/test/java/com/selimhorri/app/resource/ProductResourceIntegrationTest.java` |
| TC-PRD-008, TC-PRD-009 | `product-service/src/test/java/com/selimhorri/app/pact/ProductServiceProviderPactTest.java` |
| TC-SHP-001 | `shipping-service/src/test/java/com/selimhorri/app/service/impl/OrderItemServiceImplPactConsumerTest.java` |
| TC-FAV-001 | `favourite-service/src/test/java/com/selimhorri/app/service/impl/FavouriteServiceImplPactConsumerTest.java` |
| TC-PXY-001 | `proxy-client/src/test/java/com/selimhorri/app/client/ProductClientServicePactConsumerTest.java` |
| TC-PXY-002, TC-PXY-003 | `proxy-client/src/test/java/com/selimhorri/app/smoke/ProductProxySmokeTest.java` |
