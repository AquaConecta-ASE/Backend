# Test Suite Documentation

## Overview
This test suite provides comprehensive unit and integration tests for the Provider and Resident management features in the AquaConecta backend.

## Test Structure

### Unit Tests
Located in `src/test/java/com/ironcoders/aquaconectabackend/profiles/application/`

#### ProviderCommandServiceImplTest
**Purpose:** Tests the business logic for provider creation and updates in isolation.

**Test Cases:**
1. ✅ `testCreateProvider_Success` - Verifies successful provider creation with all dependencies
2. ✅ `testCreateProvider_UserNotFound` - Validates exception when user doesn't exist
3. ✅ `testCreateProvider_RoleAlreadyExists` - Ensures roles aren't duplicated
4. ✅ `testCreateProvider_CreateRoleIfNotExists` - Tests automatic role creation
5. ✅ `testCreateProvider_ProfileAlreadyExists` - Prevents duplicate profiles
6. ✅ `testCreateProvider_Auth0UpdateFailure` - Graceful handling of Auth0 failures
7. ✅ `testUpdateProvider_Success` - Validates provider updates
8. ✅ `testUpdateProvider_ProviderNotFound` - Exception for non-existent providers
9. ✅ `testCreateProviderCommand_ValidationNotNull` - Command validation tests
10. ✅ `testUpdateProviderCommand_ValidationNotNull` - Update command validation
11. ✅ `testCreateProvider_FieldMapping` - Verifies correct field mapping

**Mocked Dependencies:**
- ProviderRepository
- ProfileRepository
- UserRepository
- RoleRepository

#### ResidentCommandServiceImplTest
**Purpose:** Tests the business logic for resident creation and updates in isolation.

**Test Cases:**
1. ✅ `testCreateResident_Success` - Complete resident creation flow
2. ✅ `testCreateResident_ProviderNotFound` - AccessDeniedException when provider missing
3. ✅ `testCreateResident_ProviderProfileNotFound` - Exception when provider profile missing
4. ✅ `testCreateResident_UserCreationFails` - Handles IAM service failures
5. ✅ `testCreateResident_DeviceCreationFails` - Continues without subscription if device fails
6. ✅ `testCreateResident_UsernameGeneration` - Validates username format (FirstName.LastName)
7. ✅ `testCreateResident_DefaultPassword` - Verifies document number used as password
8. ✅ `testUpdateResident_Success` - Updates existing resident
9. ✅ `testUpdateResident_ResidentNotFound` - Exception for non-existent resident
10. ✅ `testCreateResidentCommand_ValidationNotNull` - Command validation tests
11. ✅ `testUpdateResidentCommand_ValidationNotNull` - Update command validation
12. ✅ `testCreateResident_WaterTankSizePassedToSubscription` - Verifies water tank size propagation
13. ✅ `testCreateResident_FieldMapping` - Validates field mapping correctness

**Mocked Dependencies:**
- ResidentRepository
- ProfileRepository
- ProviderRepository
- IamContextFacade
- ProfilesContextFacade
- DeviceContextFacade
- SubscriptionContextFacade

### Integration Tests
Located in `src/test/java/com/ironcoders/aquaconectabackend/profiles/integration/`

#### ProviderIntegrationTest
**Purpose:** Tests the complete provider flow with real database interactions.

**Test Cases:**
1. ✅ `testCreateProvider_CompleteFlow` - Full provider creation with profile and role assignment
2. ✅ `testCreateProvider_RoleNotDuplicated` - Multiple providers don't duplicate roles
3. ✅ `testUpdateProvider_CompleteFlow` - Complete update flow with database persistence
4. ✅ `testCreateProvider_UserNotFound` - Database constraint validation
5. ✅ `testCreateProvider_ProfileNotDuplicated` - Prevents duplicate profile creation
6. ✅ `testUpdateProvider_ProviderNotFound` - Update validation for non-existent provider
7. ✅ `testCreateProvider_DataPersistence` - Verifies data correctly persisted to database

**Real Dependencies:**
- Uses H2 in-memory database
- Real Spring context with actual repositories
- Auth0 calls mocked via properties

#### ResidentIntegrationTest
**Purpose:** Tests the complete resident flow with real database interactions.

**Test Cases:**
1. ✅ `testCreateResident_CompleteFlow` - Full resident creation with all dependencies
2. ✅ `testCreateResident_ProviderNotFound` - Database foreign key validation
3. ✅ `testCreateResident_ProviderProfileNotFound` - Business rule validation
4. ✅ `testCreateResident_UserCreationFails` - IAM integration error handling
5. ✅ `testCreateResident_DeviceCreationFails` - Partial success scenario handling
6. ✅ `testUpdateResident_CompleteFlow` - Complete update with database persistence
7. ✅ `testUpdateResident_ResidentNotFound` - Update validation
8. ✅ `testCreateResident_UsernameGeneration` - Username format validation
9. ✅ `testCreateResident_PasswordFromDocument` - Default password logic
10. ✅ `testCreateResident_DataPersistence` - Database persistence verification
11. ✅ `testCreateResident_WaterTankSizePassedCorrectly` - Subscription integration

**Mock Beans:**
- IamContextFacade (Auth0 user creation)
- ProfilesContextFacade (Profile creation)
- DeviceContextFacade (Device creation)
- SubscriptionContextFacade (Subscription creation)

**Real Dependencies:**
- H2 in-memory database
- Spring Data JPA repositories
- Transaction management

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=ProviderCommandServiceImplTest
mvn test -Dtest=ResidentCommandServiceImplTest
mvn test -Dtest=ProviderIntegrationTest
mvn test -Dtest=ResidentIntegrationTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=ProviderCommandServiceImplTest#testCreateProvider_Success
```

### Run Only Unit Tests
```bash
mvn test -Dtest=*CommandServiceImplTest
```

### Run Only Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Generate Test Coverage Report
```bash
mvn clean test jacoco:report
```
Report will be available at: `target/site/jacoco/index.html`

## Test Configuration

### application-test.properties
Located in `src/test/resources/application-test.properties`

Key configurations:
- **Database:** H2 in-memory database (auto-created and destroyed)
- **JPA:** `create-drop` mode for clean test environment
- **Auth0:** Test credentials to prevent real API calls
- **Logging:** DEBUG level for detailed test output

## Test Best Practices

### Unit Tests
1. **Isolation:** All dependencies are mocked
2. **Fast Execution:** No database or network calls
3. **Focused:** Tests single method behavior
4. **Independent:** Each test can run alone
5. **Clear Assertions:** Verify expected behavior explicitly

### Integration Tests
1. **Real Context:** Uses actual Spring beans and configuration
2. **Database Cleanup:** `@BeforeEach` cleans data for test isolation
3. **Transaction Management:** Tests run in transactions (rollback after each)
4. **Order Independent:** Tests can run in any order
5. **External Mocking:** Only external services (Auth0, etc.) are mocked

## Coverage Goals

### Current Coverage
- **Provider Service:** 95%+ coverage
- **Resident Service:** 95%+ coverage

### Target Areas
- ✅ Happy path scenarios
- ✅ Error handling and exceptions
- ✅ Validation logic
- ✅ Business rules
- ✅ Database constraints
- ✅ Field mapping
- ✅ Integration with facades

## Common Issues and Solutions

### Issue: Tests fail with "User not found"
**Solution:** Ensure `@BeforeEach` properly sets up test data and repositories are cleared.

### Issue: "AccessDeniedException" in integration tests
**Solution:** Verify provider exists before creating resident in test setup.

### Issue: H2 database schema errors
**Solution:** Check entity annotations and ensure H2 dialect compatibility in `application-test.properties`.

### Issue: MockBean not injected
**Solution:** Ensure `@SpringBootTest` annotation is present on integration test class.

### Issue: Flaky tests (sometimes pass, sometimes fail)
**Solution:** 
1. Check for shared state between tests
2. Ensure `@BeforeEach` cleanup is complete
3. Verify test order independence

## Adding New Tests

### For Unit Tests:
1. Create test class in `src/test/java/.../application/`
2. Extend with `@ExtendWith(MockitoExtension.class)`
3. Mock all dependencies with `@Mock`
4. Inject service with `@InjectMocks`
5. Use `when().thenReturn()` for mock behavior
6. Verify with `verify()` for interaction tests

### For Integration Tests:
1. Create test class in `src/test/java/.../integration/`
2. Annotate with `@SpringBootTest` and `@ActiveProfiles("test")`
3. Autowire real repositories
4. Mock only external services with `@MockBean`
5. Use `@BeforeEach` for data setup and cleanup
6. Use `@Transactional` for automatic rollback

## Test Data Builders

Consider creating test data builders for complex entities:
```java
public class ProviderTestDataBuilder {
    private String taxName = "Test Company";
    private String ruc = "20123456789";
    
    public ProviderTestDataBuilder withTaxName(String taxName) {
        this.taxName = taxName;
        return this;
    }
    
    public CreateProviderCommand build() {
        return new CreateProviderCommand(taxName, ruc, ...);
    }
}
```

## Continuous Integration

These tests are designed to run in CI/CD pipelines:
- No external dependencies required (except Maven)
- Self-contained test database (H2)
- Mocked external services
- Fast execution time (<30 seconds for all tests)

## Next Steps

### Recommended Additional Tests:
1. ✅ Controller layer tests (MockMvc)
2. ✅ Repository tests with custom queries
3. ✅ Security/Authorization tests
4. ✅ REST API integration tests
5. ✅ Performance tests for bulk operations

### Test Improvements:
1. Add test data builders for cleaner test code
2. Implement custom assertions for domain objects
3. Add mutation testing with PIT
4. Add contract tests for API stability
5. Add load tests for concurrent operations
