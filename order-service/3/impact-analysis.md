# Impact Analysis Report
**PR:** #3 | **Branch:** test/service-discovery-agent-demo | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/abhisheksingh-0710/ecommerce-microservice-backend-app/pull/3
**Generated:** 2026-05-14T00:00:00Z

## Executive Summary

PR #3 (`test/service-discovery-agent-demo`) modifies **order-service** across two layers: the `OrderDto` DTO gains a new nullable `orderStatus` (String) field, and `OrderResource` gains a new `GET /api/orders/status/{status}` endpoint for filtering orders by lifecycle status. Three downstream services — **payment-service**, **shipping-service**, and **proxy-client** — consume order-service endpoints via RestTemplate or FeignClient and will receive the new `orderStatus` field in responses. No existing fields were removed and no existing endpoint paths were changed, so **there are no breaking changes** in this PR. A total of **14 test cases** are recommended across unit, integration, provider-contract, and consumer-contract categories.

---

## Changed Files

### `order-service/src/main/java/com/selimhorri/app/dto/OrderDto.java`
- **Service:** order-service
- **Layer:** dto
- **Summary:** Added a new nullable `orderStatus` String field annotated with `@JsonInclude(Include.NON_NULL)` to represent the order lifecycle state.
- **Field changes:**

  | Field | Type | Change Type | Risk |
  |-------|------|-------------|------|
  | orderStatus | String | Added | Downstream consumers receive this field in all order responses; if their local OrderDto model lacks the field, deserialization will silently ignore it (Jackson default) — low risk, but consumer-contract tests needed to confirm |

### `order-service/src/main/java/com/selimhorri/app/resource/OrderResource.java`
- **Service:** order-service
- **Layer:** controller
- **Summary:** Added new `GET /api/orders/status/{status}` endpoint that filters the full order list by lifecycle status using a case-insensitive match on `orderStatus`.
- **API changes:**

  | Method | Path | Change Type | Notes |
  |--------|------|-------------|-------|
  | GET | /api/orders/status/{status} | Added | New endpoint for status-based filtering; implementation streams findAll() in-memory — no DB-level filtering; valid statuses: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED |

---

## Dependency Graph

```
order-service  [DIRECTLY CHANGED]
  |-- orderStatus field added to OrderDto
  |-- GET /api/orders/status/{status} endpoint added
  |
  +---> payment-service  [CONSUMER-CONTRACT RISK]
  |         PaymentServiceImpl calls GET /order-service/api/orders/{orderId}
  |         via RestTemplate (@LoadBalanced)
  |         Will now receive orderStatus in OrderDto responses
  |
  +---> shipping-service  [CONSUMER-CONTRACT RISK]
  |         OrderItemServiceImpl calls GET /order-service/api/orders/{orderId}
  |         via RestTemplate (@LoadBalanced)
  |         Will now receive orderStatus in OrderDto responses
  |
  +---> proxy-client  [CONSUMER-CONTRACT RISK]
  |         OrderClientService FeignClient calls /order-service/api/orders/**
  |         Will now receive orderStatus in all OrderDto responses
  |         New endpoint GET /api/orders/status/{status} may need a new FeignClient method
  |
  +---> api-gateway  [TRANSPARENT PROXY - LOW RISK]
            Routes /order-service/** transparently via Spring Cloud Gateway
            No deserialization; no contract risk from this change
```

---

## Service-by-Service Impact

### order-service — [DIRECTLY CHANGED]
- **Role:** Directly changed
- **Reason impacted:** Source of all modifications in this PR
- **Risk level:** MEDIUM
- **Recommended actions:**
  - Verify `orderStatus` is persisted in the database and populated correctly for existing orders (migration may be needed for historical records)
  - Confirm `findByStatus()` performance — current implementation calls `findAll()` and filters in-memory; may need a DB-level query for large datasets
  - Ensure the new endpoint is documented in OpenAPI/Swagger if used
  - Add validation for allowed status values (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED) or handle unknown status gracefully (currently returns empty list)

### payment-service — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** `PaymentServiceImpl` calls `GET /order-service/api/orders/{orderId}` via `RestTemplate (@LoadBalanced)` and deserializes the response into an `OrderDto`. The response will now include the `orderStatus` field.
- **Risk level:** LOW
- **Affected client methods:**
  - `PaymentServiceImpl` — `restTemplate.getForObject("/order-service/api/orders/{orderId}", OrderDto.class, orderId)`
- **Recommended actions:**
  - Verify payment-service has its own `OrderDto` model class or uses a shared library; if it has a local copy, add `orderStatus` field to it
  - If using Jackson with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES=true`, the new field will cause a failure — check configuration
  - Run consumer-contract test to confirm deserialization is safe with and without `orderStatus` present

### shipping-service — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** `OrderItemServiceImpl` calls `GET /order-service/api/orders/{orderId}` via `RestTemplate (@LoadBalanced)` and deserializes the response into an `OrderDto`. The response will now include `orderStatus`.
- **Risk level:** LOW
- **Affected client methods:**
  - `OrderItemServiceImpl` — `restTemplate.getForObject("/order-service/api/orders/{orderId}", OrderDto.class, orderId)`
- **Recommended actions:**
  - Same as payment-service: verify local `OrderDto` model and Jackson configuration
  - Shipping logic may want to use `orderStatus` to conditionally trigger shipping actions — review business logic for new opportunity

### proxy-client — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** `OrderClientService` FeignClient calls `/order-service/api/orders/**` endpoints. It will receive `orderStatus` in all `OrderDto` responses. Additionally, the new `GET /api/orders/status/{status}` endpoint is not yet exposed via `OrderClientService`, meaning clients of proxy-client cannot filter orders by status through the proxy.
- **Risk level:** MEDIUM
- **Affected client methods:**
  - `OrderClientService.findById()` — FeignClient `GET /order-service/api/orders/{orderId}`
  - `OrderClientService.findAll()` — FeignClient `GET /order-service/api/orders`
  - `OrderController.findById()` — proxy-client controller that delegates to `OrderClientService`
- **Recommended actions:**
  - Add `orderStatus` field to the `OrderDto` model used in proxy-client (if it has a local copy)
  - Consider adding `OrderClientService.findByStatus(String status)` FeignClient method to expose the new endpoint through the proxy
  - Consider adding `GET /app/api/orders/status/{status}` to `OrderController` in proxy-client to expose this capability to UI/external consumers

### api-gateway — [TRANSPARENT PROXY - NOT IMPACTED]
- **Role:** Transparent proxy
- **Reason impacted:** Routes all `/order-service/**` traffic via Spring Cloud Gateway without deserializing payloads
- **Risk level:** NONE
- **Recommended actions:** No action needed. The new endpoint is automatically routed.

---

## Breaking Changes

No breaking changes detected in this PR.

- No existing fields were removed from `OrderDto`
- No existing endpoint paths were changed or removed
- The new `orderStatus` field uses `@JsonInclude(NON_NULL)` so it is absent for orders without a status, maintaining backward compatibility
- The new `GET /api/orders/status/{status}` endpoint is purely additive

---

## Deployment Recommendation

1. **order-service** — Deploy first; it is the provider. Backward-compatible change: existing consumers continue to work without code changes (Jackson ignores unknown fields by default).
2. **payment-service** — Deploy after order-service is confirmed healthy. Update local `OrderDto` model if applicable.
3. **shipping-service** — Deploy after order-service is confirmed healthy. Update local `OrderDto` model if applicable.
4. **proxy-client** — Deploy last. Optionally add `findByStatus` FeignClient method and corresponding controller endpoint to expose new capability to external consumers.
5. **api-gateway** — No deployment needed.
