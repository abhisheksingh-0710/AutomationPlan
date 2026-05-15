# Impact Analysis Report
**PR:** #16 | **Branch:** feat/user-search | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/schmosbymosby26/ecommerce-microservice-backend-app/pull/16
**Generated:** 2026-05-15T00:00:00Z

## Executive Summary

PR #16 adds a new `GET /api/users/email/{email}` endpoint to `user-service`'s `UserResource` controller, enabling user lookup by email address. Two services are affected: `user-service` (directly changed) and `proxy-client` (downstream dependent whose `UserClientService` FeignClient does not yet expose the new route). There are **no breaking changes** — no existing endpoints were removed or modified. However, a **functional defect** is present: the implementation delegates to `userService.findAll()` instead of filtering by email, meaning the endpoint returns all users regardless of the supplied email. Eight test cases are recommended (6 for `user-service`, 2 for `proxy-client`), including one P0 case documenting the implementation bug.

---

## Changed Files

### `user-service/src/main/java/com/selimhorri/app/resource/UserResource.java`
- **Service:** user-service
- **Layer:** controller
- **Summary:** Added a new `findByEmail` handler method mapped to `GET /email/{email}` under the `/api/users` base path, returning a `DtoCollectionResponse<UserDto>`.
- **API changes:**

  | Method | Path | Change Type | Notes |
  |--------|------|-------------|-------|
  | GET | /api/users/email/{email} | Added | Returns `DtoCollectionResponse<UserDto>`; currently delegates to `userService.findAll()` — does not filter by email (functional bug) |

- **Field changes:** None

---

## Dependency Graph

```
user-service  [DIRECTLY CHANGED]
  NEW endpoint: GET /api/users/email/{email}
       |
       +---> proxy-client  [CONSUMER-CONTRACT GAP]
       |          UserClientService FeignClient does NOT declare findByEmail()
       |          UserController has no /email/{email} route
       |
       +---> order-service  [NO IMPACT]
                  CartServiceImpl calls GET /api/users/{userId} only
                  New endpoint does not affect existing call paths
```

---

## Service-by-Service Impact

### user-service — [DIRECTLY CHANGED]
- **Role:** Directly changed
- **Reason impacted:** `UserResource.java` was modified to add `GET /api/users/email/{email}` endpoint
- **Risk level:** MEDIUM
- **Recommended actions:**
  - Fix the implementation bug: `findByEmail` must call a service method that filters by email, not `findAll()`
  - Add `findByEmail(String email)` to `UserService` interface and `UserServiceImpl`
  - Add integration test covering the full request/response cycle for the new endpoint
  - Add provider-contract test asserting `DtoCollectionResponse<UserDto>` schema

### proxy-client — [DOWNSTREAM DEPENDENT]
- **Role:** Downstream dependent
- **Reason impacted:** `UserClientService` is a FeignClient bound to `USER-SERVICE /user-service/api/users`. It exposes `findAll()`, `findById()`, `findByUsername()`, `save()`, `update()`, `deleteById()` — but **no `findByEmail()` method**. Any proxy route to the new email-search endpoint is absent.
- **Risk level:** LOW (additive gap, not a breakage)
- **Affected client methods:**
  - `UserClientService` — missing `@GetMapping("/email/{email}") findByEmail(String email)` declaration
  - `UserController` — missing `@GetMapping("/email/{email}")` route delegating to `userClientService`
- **Recommended actions:**
  - Add `findByEmail(@PathVariable String email)` to `UserClientService` FeignClient interface
  - Add matching `findByEmail` route in `UserController` that delegates to `userClientService.findByEmail()`
  - Add consumer-contract test using Pact to assert the FeignClient can deserialize `DtoCollectionResponse<UserDto>`

### order-service — [NOT IMPACTED]
- **Reason:** `CartServiceImpl` calls `GET /api/users/{userId}` only. The new `/email/{email}` endpoint does not alter any existing contract.

### payment-service, shipping-service, favourite-service, product-service — [NOT IMPACTED]
- **Reason:** None of these services call `user-service` user-lookup endpoints per the available service graphs and source inspection.

---

## Breaking Changes

No breaking changes detected in this PR.

No existing endpoints were removed or modified. No DTO fields were removed or renamed. All existing consumers of `user-service` continue to function without modification.

> ⚠️ **Functional Defect (not a breaking change, but P0 quality issue):** `UserResource.findByEmail()` ignores the `email` path variable and calls `this.userService.findAll()`. This means the endpoint returns all users for any email query. This must be fixed before the PR is merged.

---

## Deployment Recommendation

1. **user-service** — fix the implementation bug first, then deploy. This is the provider; it must be healthy before consumers can use the new endpoint.
2. **proxy-client** — add `findByEmail` to `UserClientService` and `UserController`, then deploy after `user-service` is confirmed healthy. This is an additive change and is backwards-compatible with the current `user-service` state.
