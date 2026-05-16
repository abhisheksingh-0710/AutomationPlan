# Impact Analysis Report
**PR:** #18 | **Branch:** feat/shipping-tracking | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/schmosbymosby26/ecommerce-microservice-backend-app/pull/18
**Generated:** 2026-05-16T00:00:00Z

## Executive Summary

PR #18 adds a new `GET /api/shippings/track/{orderId}` endpoint to `shipping-service`'s `OrderItemResource` controller. One service is directly changed (`shipping-service`) and one downstream dependent is at risk (`proxy-client`), which holds a FeignClient bound to `SHIPPING-SERVICE` that does not yet declare the new tracking method. No existing endpoints or DTO fields were removed or renamed, so **there are no breaking changes**. A total of **9 test cases** are recommended across unit, integration, provider-contract, and consumer-contract test types. There is also a notable functional correctness concern: the new endpoint calls `orderItemService.findAll()` rather than a filtered query, meaning it returns all order items regardless of the supplied `orderId` — this should be validated or intentionally documented.

---

## Changed Files

### `shipping-service/src/main/java/com/selimhorri/app/resource/OrderItemResource.java`
- **Service:** shipping-service
- **Layer:** controller
- **Summary:** Added new `trackByOrderId()` handler method mapped to `GET /api/shippings/track/{orderId}`, returning `DtoCollectionResponse<OrderItemDto>`; three blank trailing lines removed.

**API changes:**

| Method | Path | Change Type | Notes |
|--------|------|-------------|-------|
| GET | /api/shippings/track/{orderId} | Added | New endpoint for order-ID-based shipping tracking; implementation currently returns `findAll()` unfiltered |

**Field changes:** None.

---

## Dependency Graph

```
shipping-service  [DIRECTLY CHANGED]
  New endpoint: GET /api/shippings/track/{orderId}
       |
       +---> proxy-client  [CONSUMER-CONTRACT RISK]
       |         OrderItemClientService (@FeignClient SHIPPING-SERVICE)
       |         Missing: no trackByOrderId() method in FeignClient interface
       |
       +---> api-gateway  [ROUTING — LOW RISK]
                 Routes /shipping-service/** → SHIPPING-SERVICE (auto-routed, no change needed)

shipping-service  [CALLS]
  --> order-service  (GET /order-service/api/orders/{orderId} via RestTemplate in OrderItemServiceImpl)
  --> product-service (GET /product-service/api/products/{productId} via RestTemplate in OrderItemServiceImpl)
```

---

## Service-by-Service Impact

### shipping-service — [DIRECTLY CHANGED]
- **Role:** Directly changed
- **Reason impacted:** `OrderItemResource.java` was modified to add `GET /api/shippings/track/{orderId}`
- **Risk level:** MEDIUM
- **Recommended actions:**
  - Write unit tests for `trackByOrderId()` in `OrderItemResourceTest`
  - Write integration test calling `GET /api/shippings/track/{orderId}` via `@SpringBootTest` with TestContainers
  - Verify whether the current `findAll()` delegation is intentional or a bug (should it be `findByOrderId(orderId)`?)
  - Add provider-contract Pact test asserting the response schema for the new endpoint
  - Ensure the new endpoint is reflected in any API documentation (OpenAPI/Swagger)

### proxy-client — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** Calls `GET /api/shippings/**` via `OrderItemClientService` FeignClient bound to `SHIPPING-SERVICE`; the new endpoint is not yet declared in the interface
- **Risk level:** LOW (no existing methods broken; gap is an omission, not a breakage)
- **Affected client methods:**
  - `OrderItemClientService.findAll()` — still routable, no change
  - `OrderItemClientService.findById(orderId, productId)` — still routable, no change
  - `OrderItemClientService.findById(OrderItemId)` — still routable, no change
  - **MISSING:** no `trackByOrderId(orderId)` method — proxy-client cannot expose the new tracking capability until this is added
- **Recommended actions:**
  - Add `@GetMapping("/track/{orderId}") ResponseEntity<OrderItemOrderItemServiceDtoCollectionResponse> trackByOrderId(@PathVariable String orderId)` to `OrderItemClientService`
  - Add corresponding handler in `OrderItemController` to expose the endpoint through the proxy
  - Write consumer-contract Pact test verifying `OrderItemClientService.findAll()` still deserializes correctly after the shipping-service update
  - Write gap-detection test documenting that `trackByOrderId` is absent and needs to be added

### api-gateway — [ROUTING — INFORMATIONAL]
- **Role:** Infrastructure dependent
- **Reason impacted:** Routes all `/shipping-service/**` traffic to `SHIPPING-SERVICE` via Spring Cloud Gateway; the new path `/shipping-service/api/shippings/track/{orderId}` is automatically routed without any config change
- **Risk level:** LOW
- **Recommended actions:** Smoke-test the new path through the gateway after deployment to confirm routing works end-to-end

---

## Breaking Changes

No breaking changes detected in this PR.

No endpoints were removed or renamed. No DTO fields were removed or renamed. The new endpoint is purely additive.

---

## Deployment Recommendation

1. **shipping-service** — Deploy first; provider of the new endpoint. Backward-compatible addition, no consumer breakage on deploy.
2. **proxy-client** — Deploy after shipping-service is healthy; update `OrderItemClientService` to declare `trackByOrderId()` and expose it through `OrderItemController` before deploying.
3. **api-gateway** — No config change required; verify routing smoke test after shipping-service is deployed.
