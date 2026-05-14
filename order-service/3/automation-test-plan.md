# Automation Test Plan
**PR:** #3 | **Branch:** test/service-discovery-agent-demo | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/abhisheksingh-0710/ecommerce-microservice-backend-app/pull/3
**Generated:** 2026-05-14T00:00:00Z
**Total Test Cases:** 14

---

## Test Plan Summary

| Service | Role | Test Types | Test Case Count | Priority |
|---------|------|------------|-----------------|----------|
| order-service | Directly Changed | unit, integration, provider-contract | 8 | P1 |
| payment-service | Downstream Dependent | consumer-contract | 2 | P1 |
| shipping-service | Downstream Dependent | consumer-contract | 2 | P1 |
| proxy-client | Downstream Dependent | consumer-contract | 2 | P1-P2 |

---

## Test Cases by Service

---

### order-service

#### TC-ORD-001: OrderDto includes orderStatus field with NON_NULL serialization

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-001 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Order DTO construction and serialization |
| **Trigger** | `orderStatus` field added to `OrderDto` with `@JsonInclude(NON_NULL)` |
| **Preconditions** | Jackson ObjectMapper configured as per order-service application context |
| **Test Steps** | 1. Build `OrderDto` with `orderStatus = "PENDING"` using builder<br>2. Serialize to JSON using ObjectMapper<br>3. Assert JSON string contains `"orderStatus":"PENDING"`<br>4. Build `OrderDto` without setting `orderStatus` (null)<br>5. Serialize to JSON<br>6. Assert JSON string does NOT contain `"orderStatus"` key |
| **Expected Result** | Field present when non-null; field absent when null (NON_NULL behaviour) |
| **Automation Notes** | Add to `OrderDtoSerializationTest.java` in `order-service/src/test/`; use `@JsonTest` slice or plain `ObjectMapper` unit test |

---

#### TC-ORD-002: OrderDto builder sets and gets orderStatus correctly

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-002 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Order DTO builder pattern validation |
| **Trigger** | `orderStatus` field added to `OrderDto` |
| **Preconditions** | None |
| **Test Steps** | 1. `OrderDto dto = OrderDto.builder().orderStatus("CONFIRMED").build();`<br>2. Assert `dto.getOrderStatus().equals("CONFIRMED")`<br>3. `OrderDto dto2 = OrderDto.builder().build();`<br>4. Assert `dto2.getOrderStatus()` is null (no NPE) |
| **Expected Result** | Builder sets and gets `orderStatus`; null default does not throw |
| **Automation Notes** | Add to `OrderDtoTest.java` in `order-service/src/test/java/com/selimhorri/app/dto/`; plain JUnit 5 test, no Spring context needed |

---

#### TC-ORD-003: OrderResource.findByStatus filters orders by status (unit)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-003 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Order status filtering in controller layer |
| **Trigger** | `GET /api/orders/status/{status}` endpoint added to `OrderResource` |
| **Preconditions** | `OrderService` mock available |
| **Test Steps** | 1. Mock `orderService.findAll()` to return [OrderDto(status=SHIPPED), OrderDto(status=PENDING), OrderDto(status=SHIPPED)]<br>2. Call `orderResource.findByStatus("SHIPPED")`<br>3. Assert HTTP 200<br>4. Assert response body collection contains exactly 2 items<br>5. Assert all items have `orderStatus = "SHIPPED"`<br>6. Call `orderResource.findByStatus("shipped")` (lowercase)<br>7. Assert same 2 items returned (case-insensitive match) |
| **Expected Result** | Only orders matching the status (case-insensitive) are returned |
| **Automation Notes** | Add to `OrderResourceTest.java`; use `@WebMvcTest(OrderResource.class)` with `@MockBean OrderService`; use `MockMvc.perform(get("/api/orders/status/SHIPPED"))` |

---

#### TC-ORD-004: GET /api/orders/status/{status} returns 200 with matching orders (integration)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-004 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Order retrieval by status — full request/response cycle |
| **Trigger** | `GET /api/orders/status/{status}` endpoint added |
| **Preconditions** | order-service running with H2 in-memory DB; DB seeded with 3 orders: 2 PENDING, 1 SHIPPED |
| **Test Steps** | 1. `GET /api/orders/status/PENDING`<br>2. Assert HTTP 200<br>3. Assert response `Content-Type: application/json`<br>4. Assert response body is a `DtoCollectionResponse` with 2 items<br>5. Assert each item has `orderStatus = "PENDING"` |
| **Expected Result** | Exactly 2 orders returned, both with `orderStatus=PENDING` |
| **Automation Notes** | Add to `OrderResourceIntegrationTest.java`; use `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `TestRestTemplate`; seed data via `@BeforeEach` with `OrderRepository.saveAll()`; use TestContainers for MySQL if running against MySQL profile |

---

#### TC-ORD-005: GET /api/orders/status/{status} is case-insensitive (integration)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-005 |
| **Type** | integration |
| **Priority** | P2 |
| **Flow** | Order status filtering — case insensitivity |
| **Trigger** | Implementation uses `equalsIgnoreCase` |
| **Preconditions** | DB seeded with an order with `orderStatus='pending'` (lowercase) |
| **Test Steps** | 1. `GET /api/orders/status/PENDING`<br>2. Assert HTTP 200<br>3. Assert response contains the order with `orderStatus='pending'` |
| **Expected Result** | Status match is case-insensitive |
| **Automation Notes** | Extend `OrderResourceIntegrationTest.java`; add a test method `testFindByStatusCaseInsensitive()` |

---

#### TC-ORD-006: GET /api/orders/{orderId} response includes orderStatus field (integration)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-006 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Order retrieval by ID — response schema verification |
| **Trigger** | `orderStatus` added to `OrderDto` |
| **Preconditions** | DB seeded with an order having `orderStatus='CONFIRMED'` |
| **Test Steps** | 1. `GET /api/orders/{orderId}` with a known valid ID<br>2. Assert HTTP 200<br>3. Assert response JSON has field `"orderStatus"` with value `"CONFIRMED"`<br>4. Assert existing fields (`orderId`, `orderDate`, `orderDesc`, `orderFee`) are all still present |
| **Expected Result** | Response includes `orderStatus`; no existing fields removed |
| **Automation Notes** | Add to `OrderResourceIntegrationTest.java`; use `JsonPath` assertions on response body |

---

#### TC-ORD-007: Provider contract — OrderDto response schema includes orderStatus (provider-contract)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-007 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Order retrieval flow — provider side contract verification |
| **Trigger** | `orderStatus` field added to `OrderDto`; downstream consumers must be able to handle it |
| **Preconditions** | order-service running; Pact provider state: "an order with id 1 exists with status PENDING" |
| **Test Steps** | 1. Set up Pact provider verification for `GET /api/orders/1`<br>2. Verify response contains `orderStatus` as String<br>3. Verify response contains all existing fields: `orderId`, `orderDate`, `orderDesc`, `orderFee`<br>4. Verify HTTP 200 |
| **Expected Result** | Provider response matches consumer expectations including new `orderStatus` field |
| **Automation Notes** | Create `OrderProviderPactTest.java` using `@Provider("order-service")` + `@PactBroker`; implement provider state handler for "order with status"; run via `mvn test -pl order-service` |

---

#### TC-ORD-008: Provider contract — GET /api/orders/status/{status} new endpoint contract (provider-contract)

| Field | Value |
|-------|-------|
| **ID** | TC-ORD-008 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Status-based order listing — provider side |
| **Trigger** | New `GET /api/orders/status/{status}` endpoint added |
| **Preconditions** | Pact provider state: "orders with status PENDING exist" |
| **Test Steps** | 1. `GET /api/orders/status/PENDING`<br>2. Verify HTTP 200<br>3. Verify response is `DtoCollectionResponse<OrderDto>`<br>4. Verify each item in collection has at minimum `orderId`, `orderDate`, `orderDesc`, `orderFee` fields |
| **Expected Result** | New endpoint returns correct schema matching what consumers expect |
| **Automation Notes** | Add provider state to `OrderProviderPactTest.java`; add interaction in consumer-side Pact tests for proxy-client |

---

### payment-service

#### TC-PAY-001: PaymentServiceImpl deserializes OrderDto with new orderStatus field

| Field | Value |
|-------|-------|
| **ID** | TC-PAY-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Payment creation — order lookup step |
| **Trigger** | `orderStatus` field added upstream in order-service `OrderDto` |
| **Preconditions** | Mock HTTP server returning order-service response JSON |
| **Test Steps** | 1. Set up WireMock stub: `GET /order-service/api/orders/1` returns `{"orderId":1,"orderDate":"...","orderDesc":"test","orderFee":99.99,"orderStatus":"SHIPPED"}`<br>2. Call `PaymentServiceImpl` method that fetches order by ID<br>3. Assert no deserialization exception thrown<br>4. Assert returned `OrderDto` has `orderStatus = "SHIPPED"` |
| **Expected Result** | `PaymentServiceImpl` handles new field without error |
| **Automation Notes** | Create `PaymentServiceImplOrderContractTest.java` in `payment-service/src/test/`; use `@SpringBootTest` + WireMock or Pact consumer test; ensure payment-service local `OrderDto` (or shared model) includes `orderStatus` field |

---

#### TC-PAY-002: PaymentServiceImpl tolerates null/absent orderStatus in order response

| Field | Value |
|-------|-------|
| **ID** | TC-PAY-002 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Payment creation — order lookup with absent status field |
| **Trigger** | `orderStatus` is `@JsonInclude(NON_NULL)` — field may be absent for legacy orders |
| **Preconditions** | Mock HTTP server returning order-service response without `orderStatus` field |
| **Test Steps** | 1. Stub `GET /order-service/api/orders/1` to return `{"orderId":1,"orderDate":"...","orderDesc":"test","orderFee":99.99}` (no `orderStatus`)<br>2. Call `PaymentServiceImpl` order fetch method<br>3. Assert no exception<br>4. Assert `orderStatus` is null in returned `OrderDto` |
| **Expected Result** | Absent `orderStatus` does not cause deserialization failure |
| **Automation Notes** | Extend `PaymentServiceImplOrderContractTest.java`; critical if payment-service uses `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` — check `ObjectMapper` config first |

---

### shipping-service

#### TC-SHP-001: OrderItemServiceImpl deserializes OrderDto with new orderStatus field

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Shipping order item creation — order lookup step |
| **Trigger** | `orderStatus` field added upstream in order-service `OrderDto` |
| **Preconditions** | Mock HTTP server returning order-service response JSON with `orderStatus` |
| **Test Steps** | 1. Stub `GET /order-service/api/orders/1` to return `{"orderId":1,"orderDate":"...","orderDesc":"test","orderFee":50.0,"orderStatus":"DELIVERED"}`<br>2. Invoke `OrderItemServiceImpl` method that calls order-service<br>3. Assert no exception<br>4. Assert returned `OrderDto` has `orderStatus = "DELIVERED"` |
| **Expected Result** | `OrderItemServiceImpl` handles new field without error |
| **Automation Notes** | Create `OrderItemServiceImplOrderContractTest.java` in `shipping-service/src/test/`; use WireMock or Pact consumer test |

---

#### TC-SHP-002: OrderItemServiceImpl tolerates absent orderStatus in order response

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-002 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Shipping order item creation — order lookup with absent status |
| **Trigger** | `orderStatus` is `@JsonInclude(NON_NULL)` — may be absent for legacy orders |
| **Preconditions** | Mock HTTP server returning order-service response without `orderStatus` |
| **Test Steps** | 1. Stub `GET /order-service/api/orders/1` to return JSON without `orderStatus`<br>2. Invoke `OrderItemServiceImpl` order fetch method<br>3. Assert no `NullPointerException` or deserialization exception<br>4. Assert result `OrderDto` has `orderStatus = null` |
| **Expected Result** | Field absence handled gracefully |
| **Automation Notes** | Extend `OrderItemServiceImplOrderContractTest.java` |

---

### proxy-client

#### TC-PRX-001: OrderClientService FeignClient deserializes OrderDto with orderStatus

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | Proxy client order fetch — response deserialization |
| **Trigger** | `orderStatus` field added to order-service `OrderDto` |
| **Preconditions** | Mock order-service running (WireMock); proxy-client `OrderDto` model exists locally |
| **Test Steps** | 1. Stub `GET /order-service/api/orders/1` to return JSON with `"orderStatus":"PENDING"`<br>2. Call `OrderController.findById("1")` in proxy-client<br>3. Assert HTTP 200<br>4. Assert response body includes `"orderStatus":"PENDING"` |
| **Expected Result** | FeignClient passes `orderStatus` through to proxy-client response |
| **Automation Notes** | Create `OrderClientServiceContractTest.java` in `proxy-client/src/test/`; use `@SpringBootTest` + WireMock server on order-service port |

---

#### TC-PRX-002: OrderClientService FeignClient — new status endpoint discoverability

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-002 |
| **Type** | consumer-contract |
| **Priority** | P2 |
| **Flow** | Proxy client status-based order filtering |
| **Trigger** | New `GET /api/orders/status/{status}` endpoint added in order-service; proxy-client FeignClient may need updating |
| **Preconditions** | order-service running with new endpoint deployed |
| **Test Steps** | 1. Check if `OrderClientService` has a `findByStatus(String status)` method<br>2. If yes: stub `GET /order-service/api/orders/status/PENDING` to return 200 with 2 orders; call the method; assert 2 orders returned<br>3. If no: call `GET /order-service/api/orders/status/PENDING` directly via order-service URL; assert 200; document the gap in proxy-client as a TODO |
| **Expected Result** | Either proxy-client exposes the new endpoint, or gap is documented as a follow-up task |
| **Automation Notes** | Add to `OrderClientServiceContractTest.java`; if `findByStatus` does not exist, create a GitHub issue or TODO comment in `OrderClientService.java` |

---

## Test Execution Order

1. **P0 tests first** — None in this PR (no breaking changes)
2. **P1 unit tests** — TC-ORD-001, TC-ORD-002, TC-ORD-003 (run in parallel, no DB required)
3. **P1 integration tests** — TC-ORD-004, TC-ORD-006 (require order-service + DB)
4. **P1 provider-contract tests** — TC-ORD-007, TC-ORD-008 (requires order-service running)
5. **P1 consumer-contract tests** — TC-PAY-001, TC-PAY-002, TC-SHP-001, TC-SHP-002, TC-PRX-001 (run after provider contracts pass)
6. **P2 tests** — TC-ORD-005, TC-PRX-002 (run last; non-blocking for deployment)

---

## Automation Framework Notes

### Unit Tests
- **Framework:** JUnit 5 + Mockito
- **Annotation:** `@ExtendWith(MockitoExtension.class)` for pure unit; `@WebMvcTest` for controller slice
- **Target classes:** `OrderDtoTest`, `OrderDtoSerializationTest`, `OrderResourceTest`
- **Location:** `order-service/src/test/java/com/selimhorri/app/`

### Integration Tests
- **Framework:** JUnit 5 + `@SpringBootTest(webEnvironment=RANDOM_PORT)` + `TestRestTemplate`
- **Database:** H2 in-memory for local; TestContainers (`mysql:8`) for CI/MySQL profile
- **Target classes:** `OrderResourceIntegrationTest`
- **Location:** `order-service/src/test/java/com/selimhorri/app/resource/`

### Provider-Contract Tests (order-service)
- **Framework:** Pact JVM — `au.com.dius.pact.provider:junit5spring`
- **Annotations:** `@Provider("order-service")`, `@PactBroker` or `@PactFolder`
- **Target class:** `OrderProviderPactTest`
- **Location:** `order-service/src/test/java/com/selimhorri/app/contract/`
- **Provider states:** implement `@State("an order with id 1 exists with status PENDING")` via `@BeforeEach` data seeding

### Consumer-Contract Tests (downstream services)
- **Framework:** Pact JVM consumer — `au.com.dius.pact.consumer:junit5` OR WireMock `com.github.tomakehurst:wiremock-jre8`
- **For payment-service:** `PaymentServiceImplOrderContractTest` in `payment-service/src/test/`
- **For shipping-service:** `OrderItemServiceImplOrderContractTest` in `shipping-service/src/test/`
- **For proxy-client:** `OrderClientServiceContractTest` in `proxy-client/src/test/`

### Suggested New Test File Paths
| Test Class | Path |
|-----------|------|
| `OrderDtoSerializationTest` | `order-service/src/test/java/com/selimhorri/app/dto/OrderDtoSerializationTest.java` |
| `OrderDtoTest` | `order-service/src/test/java/com/selimhorri/app/dto/OrderDtoTest.java` |
| `OrderResourceTest` | `order-service/src/test/java/com/selimhorri/app/resource/OrderResourceTest.java` |
| `OrderResourceIntegrationTest` | `order-service/src/test/java/com/selimhorri/app/resource/OrderResourceIntegrationTest.java` |
| `OrderProviderPactTest` | `order-service/src/test/java/com/selimhorri/app/contract/OrderProviderPactTest.java` |
| `PaymentServiceImplOrderContractTest` | `payment-service/src/test/java/com/selimhorri/app/service/PaymentServiceImplOrderContractTest.java` |
| `OrderItemServiceImplOrderContractTest` | `shipping-service/src/test/java/com/selimhorri/app/service/OrderItemServiceImplOrderContractTest.java` |
| `OrderClientServiceContractTest` | `proxy-client/src/test/java/com/selimhorri/app/client/OrderClientServiceContractTest.java` |
