# Automation Test Plan
**PR:** #18 | **Branch:** feat/shipping-tracking | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/schmosbymosby26/ecommerce-microservice-backend-app/pull/18
**Generated:** 2026-05-16T00:00:00Z
**Total Test Cases:** 9

---

## Test Plan Summary

| Service | Role | Test Types | Test Case Count | Priority |
|---------|------|------------|-----------------|----------|
| shipping-service | Directly Changed | unit, integration, provider-contract | 6 | P0 |
| proxy-client | Downstream Dependent | consumer-contract | 3 | P1 |

---

## Test Cases by Service

---

### shipping-service

#### TC-SHP-001: trackByOrderId returns HTTP 200 with DtoCollectionResponse

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-001 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Shipping tracking by order ID |
| **Trigger** | New `trackByOrderId()` method added to `OrderItemResource` |
| **Preconditions** | `OrderItemService` mock configured to return a list of `OrderItemDto` objects |
| **Test Steps** | 1. Mock `OrderItemService.findAll()` to return a list with one `OrderItemDto`<br>2. Call `OrderItemResource.trackByOrderId("1")` directly<br>3. Assert return value is `ResponseEntity` with status 200<br>4. Assert body is non-null `DtoCollectionResponse<OrderItemDto>` |
| **Expected Result** | HTTP 200 with non-null `DtoCollectionResponse` body |
| **Automation Notes** | Add to `OrderItemResourceTest.java` in `shipping-service/src/test/`; use `@ExtendWith(MockitoExtension.class)` and `@InjectMocks OrderItemResource`; mock `OrderItemService` with `@Mock` |

---

#### TC-SHP-002: trackByOrderId delegates to orderItemService.findAll()

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-002 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | Shipping tracking — service delegation verification |
| **Trigger** | Implementation calls `orderItemService.findAll()` — verify delegation is correct and no additional method is called |
| **Preconditions** | `OrderItemService` mock ready |
| **Test Steps** | 1. Mock `OrderItemService.findAll()` to return empty list<br>2. Call `trackByOrderId("42")`<br>3. Verify `orderItemService.findAll()` was called exactly once via `Mockito.verify(..., times(1))`<br>4. Verify no other `orderItemService` methods were called |
| **Expected Result** | `findAll()` invoked exactly once; no other service methods invoked |
| **Automation Notes** | Add to `OrderItemResourceTest.java`; use `Mockito.verifyNoMoreInteractions(orderItemService)` after `verify` |

---

#### TC-SHP-003: trackByOrderId with non-numeric orderId does not throw

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-003 |
| **Type** | unit |
| **Priority** | P2 |
| **Flow** | Shipping tracking — invalid input guard |
| **Trigger** | `orderId` is `String` @PathVariable — ensure no implicit integer parsing causes exceptions |
| **Preconditions** | `OrderItemService` mock returns empty list |
| **Test Steps** | 1. Call `trackByOrderId("not-a-number")`<br>2. Assert no exception is thrown<br>3. Assert HTTP 200 is returned |
| **Expected Result** | No exception; HTTP 200 returned (orderId is not parsed as int in this method) |
| **Automation Notes** | Add to `OrderItemResourceTest.java`; use `assertDoesNotThrow(() -> resource.trackByOrderId("abc"))` |

---

#### TC-SHP-004: GET /api/shippings/track/{orderId} returns 200 — full integration

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-004 |
| **Type** | integration |
| **Priority** | P0 |
| **Flow** | Shipping tracking end-to-end via REST |
| **Trigger** | New endpoint added — full stack integration must be verified before merging |
| **Preconditions** | shipping-service running with `@SpringBootTest(webEnvironment = RANDOM_PORT)`; H2 in-memory DB seeded with at least one `OrderItem` record |
| **Test Steps** | 1. Seed DB with one `OrderItem` (orderId=1, productId=1)<br>2. Send `GET /api/shippings/track/1` using `TestRestTemplate`<br>3. Assert HTTP status 200<br>4. Assert `Content-Type: application/json`<br>5. Assert response body is non-null and deserializes to `DtoCollectionResponse<OrderItemDto>`<br>6. Assert the `collection` array is non-null |
| **Expected Result** | HTTP 200; valid JSON body with `collection` array |
| **Automation Notes** | Create or add to `OrderItemResourceIntegrationTest.java` in `shipping-service/src/test/`; annotate with `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`; use `@Autowired TestRestTemplate`; use H2 profile (application-test.yml) |

---

#### TC-SHP-005: GET /api/shippings/track/{orderId} returns all items (unfiltered behaviour)

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-005 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Shipping tracking correctness — documents current unfiltered behaviour |
| **Trigger** | Implementation calls `findAll()` not `findByOrderId()` — test documents whether this is intentional |
| **Preconditions** | shipping-service running; DB seeded with `OrderItem` records for orderId=1 AND orderId=2 |
| **Test Steps** | 1. Seed DB: OrderItem(orderId=1, productId=1) and OrderItem(orderId=2, productId=2)<br>2. Send `GET /api/shippings/track/1`<br>3. Assert HTTP 200<br>4. Assert response collection contains BOTH items (orderId=1 and orderId=2)<br>5. Add a `// KNOWN: findAll() used — filter by orderId not yet implemented` comment in test |
| **Expected Result** | Both items returned (current behaviour); if a future fix filters by orderId, this test should be updated |
| **Automation Notes** | Add to `OrderItemResourceIntegrationTest.java`; document in test that this behaviour is explicitly asserted and should be revisited when filtering is implemented |

---

#### TC-SHP-006: GET /api/shippings/track/{orderId} response schema matches provider contract

| Field | Value |
|-------|-------|
| **ID** | TC-SHP-006 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Provider contract verification for new tracking endpoint |
| **Trigger** | New endpoint added — downstream consumers (proxy-client) must be able to parse the response |
| **Preconditions** | Pact provider verification test wired with `@Provider("SHIPPING-SERVICE")`; Pact broker or local pact file available |
| **Test Steps** | 1. Define Pact interaction: consumer requests `GET /shipping-service/api/shippings/track/1`<br>2. Provider returns HTTP 200 with body: `{"collection": [{"orderId": 1, "productId": 1, "orderQuantity": 2}]}`<br>3. Run Pact provider verification against live shipping-service<br>4. Assert all interactions verified |
| **Expected Result** | Pact verification passes; response schema includes `collection` array with `OrderItemDto` fields |
| **Automation Notes** | Create `ShippingServicePactProviderTest.java` in `shipping-service/src/test/`; use `@ExtendWith(PactVerificationExtension.class)`; annotate with `@Provider("SHIPPING-SERVICE")` and `@PactBroker` or `@PactFolder`; use TestContainers for DB |

---

### proxy-client

#### TC-PRX-001: OrderItemClientService.findAll() still resolves after shipping-service update

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | proxy-client → shipping-service: fetch all order items |
| **Trigger** | shipping-service `OrderItemResource` was modified; existing FeignClient consumers must still be compatible |
| **Preconditions** | Pact consumer test environment; WireMock or Pact mock server standing in for SHIPPING-SERVICE |
| **Test Steps** | 1. Define Pact interaction: `GET /shipping-service/api/shippings` → HTTP 200, body `{"collection": [...]}`<br>2. Call `OrderItemClientService.findAll()` against Pact mock server<br>3. Assert HTTP 200 received<br>4. Assert response body deserializes to `OrderItemOrderItemServiceDtoCollectionResponse` without error<br>5. Publish pact to Pact broker |
| **Expected Result** | FeignClient successfully deserializes response; pact published |
| **Automation Notes** | Create `OrderItemClientServicePactConsumerTest.java` in `proxy-client/src/test/`; use `@ExtendWith(PactConsumerTestExt.class)` and `@Consumer("PROXY-CLIENT")` annotation; use `MockMvcRequestSpecification` or Feign builder with pact mock URL |

---

#### TC-PRX-002: proxy-client lacks trackByOrderId() — gap detection

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-002 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | proxy-client → shipping-service: track by order ID (missing binding) |
| **Trigger** | `GET /api/shippings/track/{orderId}` added in shipping-service but no corresponding method in `OrderItemClientService` |
| **Preconditions** | Access to `OrderItemClientService` interface source |
| **Test Steps** | 1. Use Java reflection: `OrderItemClientService.class.getDeclaredMethods()`<br>2. Assert that NO method is annotated with `@GetMapping` path containing `/track/{orderId}`<br>3. Log: "GAP: proxy-client does not expose trackByOrderId — add @GetMapping('/track/{orderId}') to OrderItemClientService"<br>4. (This test should be converted to a positive test once the method is added) |
| **Expected Result** | Test documents the gap; fails with a clear message once the gap is intentionally left open, driving a code change |
| **Automation Notes** | Add to `OrderItemClientServiceTest.java` in `proxy-client/src/test/`; use JUnit 5 `assertFalse` with descriptive message; mark with `@Tag("gap-detection")` so it can be run selectively in CI |

---

#### TC-PRX-003: OrderItemClientService FeignClient base path is still correct after shipping-service update

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-003 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | proxy-client Feign binding verification |
| **Trigger** | shipping-service resource class modified — base path must remain `/api/shippings` |
| **Preconditions** | `OrderItemClientService` interface accessible; shipping-service deployed or Pact mock available |
| **Test Steps** | 1. Inspect `@FeignClient` annotation on `OrderItemClientService`: assert `name = "SHIPPING-SERVICE"` and `path = "/shipping-service/api/shippings"`<br>2. For each existing method (`findAll`, `findById`, `save`, `update`, `deleteById`): verify the Pact mock server responds as expected<br>3. Assert all 5 existing Feign methods still return HTTP 200 from the mock |
| **Expected Result** | All existing methods routable; base path unchanged |
| **Automation Notes** | Extend `OrderItemClientServicePactConsumerTest.java`; add one Pact interaction per existing method; use `@Pact(consumer = "PROXY-CLIENT", provider = "SHIPPING-SERVICE")` per method |

---

## Test Execution Order

1. **P0 first** — TC-SHP-004 (integration): must pass before any contract tests run; confirms the new endpoint is live and reachable
2. **Unit tests in parallel** — TC-SHP-001, TC-SHP-002, TC-SHP-003 (fast, no DB required)
3. **Provider-contract** — TC-SHP-006 (after integration passes; publishes pact for consumers)
4. **Integration correctness** — TC-SHP-005 (after TC-SHP-004; documents unfiltered-behaviour expectation)
5. **Consumer-contract** — TC-PRX-001, TC-PRX-003 (after provider pact is published)
6. **Gap detection** — TC-PRX-002 (run last; informational — drives a follow-up code change in proxy-client)

---

## Automation Framework Notes

- **Unit tests:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
  - Suggested class: `shipping-service/src/test/java/com/selimhorri/app/resource/OrderItemResourceTest.java`
- **Integration tests:** JUnit 5 + `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`
  - Suggested class: `shipping-service/src/test/java/com/selimhorri/app/resource/OrderItemResourceIntegrationTest.java`
  - Use H2 in-memory DB (already configured in `application-dev.yml`)
- **Provider-contract tests:** Pact JVM (`au.com.dius.pact.provider:junit5`)
  - Suggested class: `shipping-service/src/test/java/com/selimhorri/app/contract/ShippingServicePactProviderTest.java`
- **Consumer-contract tests:** Pact JVM (`au.com.dius.pact.consumer:junit5`)
  - Suggested class: `proxy-client/src/test/java/com/selimhorri/app/business/orderItem/service/OrderItemClientServicePactConsumerTest.java`
- **TestContainers:** Recommended for DB-dependent integration tests to ensure reproducibility (MySQL for prod profile); H2 profile is sufficient for the current unit/integration scope
- **CI note:** Run TC-SHP-004 (P0 integration) as a gate; block merge if it fails
