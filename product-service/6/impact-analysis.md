# Impact Analysis Report
**PR:** #6 | **Branch:** feat/product-search-and-discount | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/abhisheksingh-0710/ecommerce-microservice-backend-app/pull/6
**Generated:** 2026-05-15T00:00:00Z

## Executive Summary

PR #6 adds a `discountPercent` field to the `Product` entity and `ProductDto`, and exposes two new GET endpoints (`/api/products/search` and `/api/products/discounted`) in `product-service`. All changes are purely additive — no existing endpoints were removed or renamed, and no existing DTO fields were removed. Four services are affected: `product-service` (directly changed), and `shipping-service`, `favourite-service`, and `proxy-client` as downstream dependents that call product-service endpoints and must tolerate the enriched response schema. No breaking changes were detected. A total of 15 test cases are recommended across unit, integration, provider-contract, consumer-contract, and smoke categories.

---

## Changed Files

### `product-service/src/main/java/com/selimhorri/app/domain/Product.java`
- **Service:** product-service
- **Layer:** other (JPA entity)
- **Summary:** Added `discountPercent` column (type `Double`, DB column `discount_percent`, default `0.0`) to the `Product` entity.
- **Field changes:**
  | Field | Type | Change Type | Risk |
  |-------|------|-------------|------|
  | discountPercent | Double | Added | Requires DB schema migration (new column); defaults to 0.0 so existing rows unaffected |

---

### `product-service/src/main/java/com/selimhorri/app/dto/ProductDto.java`
- **Service:** product-service
- **Layer:** dto
- **Summary:** Added `discountPercent` (Double) field to `ProductDto`, which is serialized in all product API responses.
- **Field changes:**
  | Field | Type | Change Type | Risk |
  |-------|------|-------------|------|
  | discountPercent | Double | Added | Downstream consumers receive a new field in JSON responses — additive, non-breaking, but consumers using strict deserialization (e.g. `FAIL_ON_UNKNOWN_PROPERTIES=true`) may throw |

---

### `product-service/src/main/java/com/selimhorri/app/helper/ProductMappingHelper.java`
- **Service:** product-service
- **Layer:** other (mapper)
- **Summary:** Wired `discountPercent` into both `map(Product → ProductDto)` and `map(ProductDto → Product)` builder chains.

---

### `product-service/src/main/java/com/selimhorri/app/repository/ProductRepository.java`
- **Service:** product-service
- **Layer:** repository
- **Summary:** Added two new query methods: `findByProductTitleContainingIgnoreCase` (JPQL case-insensitive search) and `findByDiscountPercentGreaterThan` (derived query for filtering discounted products).

---

### `product-service/src/main/java/com/selimhorri/app/resource/ProductResource.java`
- **Service:** product-service
- **Layer:** controller
- **Summary:** Added two new GET endpoints: `/search` (query param `title`) and `/discounted` (no params), both returning `DtoCollectionResponse<ProductDto>`.
- **API changes:**
  | Method | Path | Change Type | Notes |
  |--------|------|-------------|-------|
  | GET | /api/products/search?title= | Added | Case-insensitive title search; `title` is required query param |
  | GET | /api/products/discounted | Added | Returns all products with discountPercent > 0 |

---

### `product-service/src/main/java/com/selimhorri/app/service/ProductService.java`
- **Service:** product-service
- **Layer:** service
- **Summary:** Added `searchByTitle(String title)` and `findDiscounted()` method signatures to the `ProductService` interface.

---

### `product-service/src/main/java/com/selimhorri/app/service/impl/ProductServiceImpl.java`
- **Service:** product-service
- **Layer:** service
- **Summary:** Implemented `searchByTitle` and `findDiscounted` in `ProductServiceImpl`, both delegating to the new repository methods and mapping results via `ProductMappingHelper`.

---

## Dependency Graph

```
product-service  [CHANGED]
  |
  +-- shipping-service  [CONSUMER-CONTRACT RISK]
  |        OrderItemServiceImpl calls GET /product-service/api/products/{productId}
  |        via RestTemplate (@LoadBalanced)
  |        Risk: new discountPercent field in response; strict Jackson config could fail
  |
  +-- favourite-service  [CONSUMER-CONTRACT RISK]
  |        FavouriteServiceImpl calls GET /product-service/api/products/{productId}
  |        via RestTemplate (@LoadBalanced)
  |        Risk: new discountPercent field in response; strict Jackson config could fail
  |
  +-- proxy-client  [CONSUMER-CONTRACT RISK]
           ProductClientService (FeignClient) calls /product-service/api/products/**
           Routes /api/products/search and /api/products/discounted now reachable
           Risk: FeignClient DTO deserialization must tolerate new discountPercent field
```

---

## Service-by-Service Impact

### product-service — [DIRECTLY CHANGED]
- **Role:** Directly changed
- **Reason impacted:** Two new API endpoints added; `discountPercent` field added to the primary `ProductDto` used in all product responses; repository layer extended.
- **Risk level:** MEDIUM
- **Recommended actions:**
  - Verify DB migration script adds `discount_percent` column with `DEFAULT 0.0` before deployment
  - Ensure Jackson `ObjectMapper` configuration does not break existing consumers
  - Add integration tests for both new endpoints with TestContainers
  - Run Pact provider verification against existing consumer contracts

---

### shipping-service — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** Calls `GET /product-service/api/products/{productId}` via `OrderItemServiceImpl` using RestTemplate. Response now includes `discountPercent` — if `ObjectMapper` is configured with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = true`, deserialization will throw `UnrecognizedPropertyException`.
- **Risk level:** MEDIUM
- **Affected client methods:**
  - `OrderItemServiceImpl` — RestTemplate call to `/product-service/api/products/{productId}`
- **Recommended actions:**
  - Verify `ObjectMapper` in shipping-service is configured with `FAIL_ON_UNKNOWN_PROPERTIES = false` (Spring Boot default) or add `@JsonIgnoreProperties(ignoreUnknown = true)` to the local Product DTO/model
  - Add consumer-contract test mocking the enriched ProductDto response

---

### favourite-service — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** Calls `GET /product-service/api/products/{productId}` via `FavouriteServiceImpl` using RestTemplate. Same `discountPercent` field risk as shipping-service.
- **Risk level:** MEDIUM
- **Affected client methods:**
  - `FavouriteServiceImpl` — RestTemplate call to `/product-service/api/products/{productId}`
- **Recommended actions:**
  - Verify `ObjectMapper` configured with `FAIL_ON_UNKNOWN_PROPERTIES = false`
  - Add consumer-contract test mocking the enriched ProductDto response

---

### proxy-client — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** Calls all product-service endpoints via `ProductClientService` (FeignClient). The two new endpoints (`/search`, `/discounted`) are routable through the existing wildcard FeignClient mapping. The `discountPercent` field in responses needs compatible Feign decoder configuration.
- **Risk level:** MEDIUM
- **Affected client methods:**
  - `ProductClientService` (FeignClient) — wildcard mapping `/product-service/api/products/**`
- **Recommended actions:**
  - Verify Feign decoder (Jackson) uses `FAIL_ON_UNKNOWN_PROPERTIES = false`
  - Add smoke tests confirming new endpoints are routable through proxy-client
  - Add consumer-contract test for enriched ProductDto deserialization via Feign

---

## Breaking Changes

No breaking changes detected in this PR.

All modifications are purely additive:
- New endpoints added (no existing endpoints removed or path-renamed)
- New field added to DTO (no existing fields removed or renamed)
- Existing API contracts are backward-compatible

---

## Deployment Recommendation

1. **product-service** — Deploy first; provider of all changed contracts. Ensure DB migration (`ALTER TABLE product ADD COLUMN discount_percent DECIMAL DEFAULT 0.0`) runs before service restart.
2. **shipping-service** — Deploy after product-service is healthy; validate RestTemplate deserialization with enriched response.
3. **favourite-service** — Deploy after product-service is healthy; same deserialization validation.
4. **proxy-client** — Deploy last; depends on product-service being fully available with new endpoints for smoke test validation.
