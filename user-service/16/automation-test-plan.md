# Automation Test Plan
**PR:** #16 | **Branch:** feat/user-search | **Repo:** ecommerce-microservice-backend-app
**Source PR URL:** https://github.com/schmosbymosby26/ecommerce-microservice-backend-app/pull/16
**Generated:** 2026-05-15T00:00:00Z
**Total Test Cases:** 8

---

## Test Plan Summary

| Service | Role | Test Types | Test Case Count | Priority |
|---------|------|------------|-----------------|----------|
| user-service | Directly Changed | unit, integration, provider-contract | 6 | P0/P1 |
| proxy-client | Downstream Dependent | consumer-contract | 2 | P1 |

---

## Test Cases by Service

---
### user-service

#### TC-USR-001: findByEmail delegates to userService correctly

| Field | Value |
|-------|-------|
| **ID** | TC-USR-001 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | User lookup by email |
| **Trigger** | New `findByEmail` method added to `UserResource` |
| **Preconditions** | `UserService` mock configured to return a known list of `UserDto` objects |
| **Test Steps** | 1. Instantiate `UserResource` with a mocked `UserService`<br>2. Call `findByEmail("test@example.com")`<br>3. Assert return value is `ResponseEntity.ok(new DtoCollectionResponse<>(userService.findAll()))`<br>4. Assert HTTP status is 200 |
| **Expected Result** | Returns HTTP 200 with `DtoCollectionResponse` wrapping the mocked user list |
| **Automation Notes** | Add to `UserResourceTest.java` in `user-service/src/test/`; use `@ExtendWith(MockitoExtension.class)` and `@Mock UserService`; assert with `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)` |

---

#### TC-USR-002: findByEmail rejects blank email input

| Field | Value |
|-------|-------|
| **ID** | TC-USR-002 |
| **Type** | unit |
| **Priority** | P1 |
| **Flow** | User lookup by email — input validation |
| **Trigger** | `@NotBlank` constraint on `email` path variable in new endpoint |
| **Preconditions** | Spring validation enabled; `UserResource` loaded in test context |
| **Test Steps** | 1. Send `GET /api/users/email/` with blank/empty path variable via `MockMvc`<br>2. Assert HTTP 400 response<br>3. Assert response body contains validation error message |
| **Expected Result** | HTTP 400 with validation error; `UserService` is never invoked |
| **Automation Notes** | Add to `UserResourceIntegrationTest.java`; use `@WebMvcTest(UserResource.class)` with `MockMvc`; `mockMvc.perform(get("/api/users/email/ ")).andExpect(status().isBadRequest())` |

---

#### TC-USR-003: GET /api/users/email/{email} returns 200 with user collection

| Field | Value |
|-------|-------|
| **ID** | TC-USR-003 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Full HTTP request/response cycle for email search |
| **Trigger** | New endpoint added to `UserResource` controller |
| **Preconditions** | `user-service` running with `@SpringBootTest`; H2 database seeded with at least one user |
| **Test Steps** | 1. Start application context with `@SpringBootTest(webEnvironment = RANDOM_PORT)`<br>2. Send `GET /api/users/email/john@example.com` via `TestRestTemplate`<br>3. Assert HTTP 200<br>4. Assert response body is valid JSON with `collection` array field<br>5. Assert each element in `collection` contains `userId`, `firstName`, `lastName`, `email` |
| **Expected Result** | HTTP 200; non-null `collection` array; all UserDto fields present |
| **Automation Notes** | Add to `UserResourceIntegrationTest.java`; use `@SpringBootTest` + `TestRestTemplate`; assert with `assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)`; parse body with `ObjectMapper` |

---

#### TC-USR-004: GET /api/users/email/{email} returns 400 on blank email segment

| Field | Value |
|-------|-------|
| **ID** | TC-USR-004 |
| **Type** | integration |
| **Priority** | P1 |
| **Flow** | Input validation for email search endpoint |
| **Trigger** | `@NotBlank` and `@Valid` constraints on path variable |
| **Preconditions** | `user-service` running; Spring Boot validation auto-configured |
| **Test Steps** | 1. Send `GET /api/users/email/%20` (URL-encoded space) via `MockMvc`<br>2. Assert HTTP 400<br>3. Assert response body contains constraint violation message `"Input must not blank"` |
| **Expected Result** | HTTP 400; validation error in response; service layer not invoked |
| **Automation Notes** | Add to `UserResourceIntegrationTest.java`; use `@SpringBootTest` + `MockMvc`; test both space and empty-string variants |

---

#### TC-USR-005: GET /api/users/email/{email} response schema matches DtoCollectionResponse<UserDto>

| Field | Value |
|-------|-------|
| **ID** | TC-USR-005 |
| **Type** | provider-contract |
| **Priority** | P1 |
| **Flow** | Contract verification for new email-search endpoint |
| **Trigger** | New endpoint exposed — downstream consumers (e.g. `proxy-client`) may bind to it |
| **Preconditions** | `user-service` running; H2 seeded with test users |
| **Test Steps** | 1. Call `GET /api/users/email/test@example.com`<br>2. Assert `Content-Type: application/json`<br>3. Assert top-level body has `collection` key<br>4. Assert `collection` is an array<br>5. For each element assert: `userId` (integer), `firstName` (string), `lastName` (string), `email` (string), `phone` (string), `imageUrl` (nullable string), `credential` (object or null) are present |
| **Expected Result** | Response schema is stable and matches `DtoCollectionResponse<UserDto>` contract; no missing fields |
| **Automation Notes** | Add `UserEmailEndpointProviderContractTest.java`; use Pact provider verification or JSON Schema assertion with `json-schema-validator`; register as `@Provider("user-service")` in Pact if using Pact framework |

---

#### TC-USR-006: findByEmail implementation filters by email (bug documentation — expected to FAIL)

| Field | Value |
|-------|-------|
| **ID** | TC-USR-006 |
| **Type** | unit |
| **Priority** | **P0** |
| **Flow** | Correctness of email search logic |
| **Trigger** | Implementation calls `this.userService.findAll()` instead of filtering by email — functional defect in PR #16 |
| **Preconditions** | Two users seeded: `alice@example.com` and `bob@example.com` |
| **Test Steps** | 1. Call `GET /api/users/email/alice@example.com`<br>2. Assert HTTP 200<br>3. Assert `collection` array contains exactly one user<br>4. Assert that user's `email` equals `alice@example.com`<br>5. Assert `bob@example.com` is NOT present in the collection |
| **Expected Result** | Only Alice is returned — **this test will currently FAIL** because the implementation returns all users regardless of email input |
| **Automation Notes** | Add `UserEmailFilterTest.java`; this test documents the known defect and should block merge until fixed; add `@Disabled("Known defect — implementation calls findAll() instead of filtering")` comment temporarily, then remove when the bug is resolved |

---

### proxy-client

#### TC-PRX-001: UserClientService missing findByEmail binding

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-001 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | proxy-client consumer contract for user email search |
| **Trigger** | `user-service` added `GET /api/users/email/{email}` but `UserClientService` FeignClient does not yet expose it |
| **Preconditions** | `UserClientService` updated with `findByEmail()` method; Pact mock provider running |
| **Test Steps** | 1. Define Pact interaction: consumer `proxy-client` calls `GET /user-service/api/users/email/test@example.com`<br>2. Provider returns HTTP 200 with `{"collection": [{"userId": 1, "firstName": "John", "lastName": "Doe", "email": "test@example.com"}]}`<br>3. Execute `userClientService.findByEmail("test@example.com")`<br>4. Assert no `FeignException` is thrown<br>5. Assert deserialized response is `UserUserServiceCollectionDtoResponse` with one entry |
| **Expected Result** | FeignClient successfully calls and deserializes the new endpoint response |
| **Automation Notes** | Add `UserClientServicePactConsumerTest.java` in `proxy-client/src/test/`; use `@PactConsumerTest` and `@Pact(consumer = "proxy-client", provider = "user-service")`; will require adding `findByEmail` to `UserClientService` interface first |

---

#### TC-PRX-002: UserController exposes /api/users/email/{email} route through proxy

| Field | Value |
|-------|-------|
| **ID** | TC-PRX-002 |
| **Type** | consumer-contract |
| **Priority** | P1 |
| **Flow** | End-to-end email search via proxy gateway |
| **Trigger** | New upstream endpoint in `user-service` requires matching proxy route in `UserController` |
| **Preconditions** | `UserClientService.findByEmail()` added; `UserController` updated with `/email/{email}` route; `user-service` mock/stub running |
| **Test Steps** | 1. Start `proxy-client` with `@SpringBootTest`<br>2. Mock `UserClientService.findByEmail("search@test.com")` to return a `ResponseEntity` with one user<br>3. Send `GET /api/users/email/search@test.com` to proxy-client<br>4. Assert HTTP 200<br>5. Assert response body matches `UserUserServiceCollectionDtoResponse` with the mocked user |
| **Expected Result** | Proxy routes request to `user-service` and returns result correctly |
| **Automation Notes** | Add to `UserControllerTest.java` in `proxy-client/src/test/`; use `@WebMvcTest(UserController.class)` + `@MockBean UserClientService`; assert with `MockMvc.perform(get("/api/users/email/search@test.com")).andExpect(status().isOk())` |

---

## Test Execution Order

Run in the following order to surface failures early:

1. **P0 first — TC-USR-006** (implementation bug validation; will fail until the bug is fixed — gates the merge)
2. **Provider-contract tests — TC-USR-005** (verify `user-service` schema before consumer tests run)
3. **Unit tests in parallel — TC-USR-001, TC-USR-002** (fast feedback; no infrastructure required)
4. **Integration tests — TC-USR-003, TC-USR-004** (require Spring context + H2; run after unit tests pass)
5. **Consumer-contract tests — TC-PRX-001, TC-PRX-002** (require provider contract verified first; can run against Pact mock)

---

## Automation Framework Notes

### user-service
- **Unit tests:** JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`)
  - Suggested class: `user-service/src/test/java/com/selimhorri/app/resource/UserResourceTest.java`
- **Integration tests:** JUnit 5 + `@SpringBootTest` + `MockMvc` + H2 in-memory database
  - Suggested class: `user-service/src/test/java/com/selimhorri/app/resource/UserResourceIntegrationTest.java`
- **Provider-contract tests:** Pact JVM provider (`au.com.dius.pact.provider:junit5`)
  - Suggested class: `user-service/src/test/java/com/selimhorri/app/contract/UserEmailEndpointProviderContractTest.java`
  - Annotate with `@Provider("user-service")` and `@PactBroker` or `@PactFolder`

### proxy-client
- **Consumer-contract tests:** Pact JVM consumer (`au.com.dius.pact.consumer:junit5`)
  - Suggested class: `proxy-client/src/test/java/com/selimhorri/app/business/user/service/UserClientServicePactConsumerTest.java`
  - Annotate with `@PactConsumerTest`
  - Controller integration test: `proxy-client/src/test/java/com/selimhorri/app/business/user/controller/UserControllerTest.java`

### General
- Use `TestContainers` for any tests requiring a real MySQL instance in CI
- H2 in-memory is sufficient for all tests in this PR (no schema changes)
- Run `./mvnw test -pl user-service,proxy-client` from repo root to scope test execution
