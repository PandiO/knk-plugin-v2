# User Account Management - Implementation Roadmap

**Status**: Planning  
**Created**: January 7, 2026

This document provides a step-by-step implementation plan organized by component and priority.

---

## Phase 1: Foundation (Data Model & Repositories)

### Priority: CRITICAL - Blocks all other work

#### 1.1 Update User Entity
- [ ] Add `PasswordHash: string?` property
- [ ] Add `EmailVerified: bool` property (default: false)
- [ ] Add `AccountCreatedVia: AccountCreationMethod` enum property
- [ ] Add `LastPasswordChangeAt: DateTime?` property (audit trail)
- [ ] Add `LastEmailChangeAt: DateTime?` property (audit trail)
- [ ] Add `IsActive: bool` property (default: true; soft deletion flag)
- [ ] Add `DeletedAt: DateTime?` property (soft delete timestamp)
- [ ] Add `DeletedReason: string?` property (soft delete reason)
- [ ] Add `ArchiveUntil: DateTime?` property (TTL for soft-deleted records; 90-day default)
- [ ] Create `AccountCreationMethod` enum (WebApp = 0, MinecraftServer = 1)
- [ ] Add unique constraint annotations on Username, Email, Uuid
- [ ] Keep existing balances (Coins, Gems, ExperiencePoints) and document invariants: non-negative, service-only updates, audited mutations
- [ ] Add XML documentation

**File**: `Models/User.cs`

**Effort**: 30 minutes

---

#### 1.2 Create LinkCode Entity
- [ ] Create `Models/LinkCode.cs`
- [ ] Add properties: Id, UserId (FK), Code, CreatedAt, ExpiresAt, Status, UsedAt, User (nav)
- [ ] Create `LinkCodeStatus` enum (Active = 0, Used = 1, Expired = 2)
- [ ] Code format: **8 alphanumeric characters** (not 6)
- [ ] Add unique index on Code
- [ ] Add XML documentation

**File**: `Models/LinkCode.cs`

**Effort**: 20 minutes

---

#### 1.3 Create EF Core Migration
- [ ] Run: `dotnet ef migrations add AddLinkCodeAndUserAuthFields`
- [ ] Verify migration includes:
  - LinkCode table creation with 8-char Code column
  - PasswordHash, EmailVerified, AccountCreatedVia columns on User
  - LastPasswordChangeAt, LastEmailChangeAt, IsActive, DeletedAt, DeletedReason, ArchiveUntil columns on User
  - Unique indexes on Username, Email, Uuid (non-null only)
  - Foreign key User → LinkCode (no cascade; soft-delete handles cleanup)
- [ ] Review generated SQL
- [ ] Apply: `dotnet ef database update`
- [ ] Test rollback on dev environment

**Files**: `Migrations/[timestamp]_AddLinkCodeAndUserAuthFields.cs`

**Effort**: 45 minutes

---

#### 1.4 Extend IUserRepository Interface
Add method signatures (no implementation yet):

```csharp
// Unique constraint checks
Task<bool> IsUsernameTakenAsync(string username, int? excludeUserId = null);
Task<bool> IsEmailTakenAsync(string email, int? excludeUserId = null);
Task<bool> IsUuidTakenAsync(string uuid, int? excludeUserId = null);

// Find by criteria
Task<User?> GetByEmailAsync(string email);
Task<User?> GetByUuidAndUsernameAsync(string uuid, string username);

// Credentials & Email updates
Task UpdatePasswordHashAsync(int id, string passwordHash);
Task UpdateEmailAsync(int id, string email);

// Merge & conflict
Task<User?> FindDuplicateAsync(string uuid, string username);
Task MergeUsersAsync(int primaryUserId, int secondaryUserId);

// Link codes
Task<LinkCode> CreateLinkCodeAsync(LinkCode linkCode);
Task<LinkCode?> GetLinkCodeByCodeAsync(string code);
Task UpdateLinkCodeStatusAsync(int linkCodeId, LinkCodeStatus status);
Task<IEnumerable<LinkCode>> GetExpiredLinkCodesAsync();
```

**File**: `Repositories/Interfaces/IUserRepository.cs`

**Effort**: 20 minutes

---

#### 1.5 Implement IUserRepository Extensions
Implement all methods from 1.4 in UserRepository:

- [ ] `IsUsernameTakenAsync`: Case-insensitive query, exclude current user
- [ ] `IsEmailTakenAsync`: Case-insensitive, nullable handling
- [ ] `IsUuidTakenAsync`: Exact match, nullable handling
- [ ] `GetByEmailAsync`: Case-insensitive
- [ ] `GetByUuidAndUsernameAsync`: Both conditions required
- [ ] `UpdatePasswordHashAsync`: Direct update, save changes
- [ ] `UpdateEmailAsync`: Direct update, save changes
- [ ] `FindDuplicateAsync`: Search by UUID and Username
- [ ] `MergeUsersAsync`: 
  - Copy/update necessary fields from secondary to primary
  - Handle foreign keys (research other entities using User as FK)
  - Delete secondary user
  - Transaction support
- [ ] `CreateLinkCodeAsync`: Add to DB, save
- [ ] `GetLinkCodeByCodeAsync`: Query by code
- [ ] `UpdateLinkCodeStatusAsync`: Update status, save
- [ ] `GetExpiredLinkCodesAsync`: WHERE ExpiresAt < UtcNow

**File**: `Repositories/UserRepository.cs`

**Effort**: 2-3 hours (depends on foreign key complexity in merge)

**Blockers**: Need to identify all entities that have User as foreign key (Towns, Plots, etc.)

---

### Phase 1 Summary
- **Total Effort**: ~4 hours
- **Risk**: Medium (cascade delete/merge logic needs careful testing)
- **Blockers**: Entity relationship analysis for merge logic

---

## Phase 2: DTOs & Mapping (API Contract Layer)

### Priority: HIGH - Needed for service layer
### Status: ✅ COMPLETE (January 11, 2026)

#### 2.1 Create/Update User DTOs
Update `Dtos/UserDtos.cs`:

- [x] Update `UserCreateDto`:
  - Add `string? Uuid` (optional)
  - Add `string? Password` (optional)
  - Add `string? PasswordConfirmation` (optional)
  - Add `string? LinkCode` (optional for linking existing account)
  - Make Email optional
  
- [x] Create `UserUpdateDto`:
  - `string? Email`
  - `string? NewPassword`
  - `string NewPasswordConfirmation`
  - `string? CurrentPassword`
  
- [x] Create `ChangePasswordDto`:
  - `string CurrentPassword`
  - `string NewPassword`
  - `string PasswordConfirmation`
  
- [x] Create `UpdateEmailDto`:
  - `string NewEmail`
  - `string? CurrentPassword` (for security)
  
- [x] Update `UserDto`: Add `Gems`, `ExperiencePoints`, `EmailVerified`, `AccountCreatedVia`
- [x] Update `UserSummaryDto`: Add `Gems`, `ExperiencePoints`
- [x] Create `AccountMergeResultDto`: User with merge metadata
- [x] Ensure no PasswordHash in any DTO response

**File**: `Dtos/UserDtos.cs`

**Effort**: 1 hour

---

#### 2.2 Create LinkCode & Auth DTOs
Create `Dtos/LinkCodeDtos.cs`:

- [x] `LinkCodeResponseDto`: { code, expiresAt }
- [x] `LinkCodeRequestDto`: { userId }
- [x] `ValidateLinkCodeResponseDto`: { isValid, userId, username, email, error }
- [x] `DuplicateCheckDto`: { uuid, username }
- [x] `DuplicateCheckResponseDto`: { hasDuplicate, conflictingUser?, primaryUser?, message }
- [x] `AccountMergeDto`: { primaryUserId, secondaryUserId }
- [x] `LinkAccountDto`: { linkCode, email, password, passwordConfirmation }

**File**: `Dtos/LinkCodeDtos.cs` (new)

**Effort**: 1 hour

---

#### 2.3 Extend UserMappingProfile

Update `Mapping/UserMappingProfile.cs`:

- [x] `User → UserDto`: Add Gems, ExperiencePoints, EmailVerified mapping; exclude PasswordHash (Ignore)
- [x] `UserDto → User`: Exclude PasswordHash from reverse mapping
- [x] `User → UserSummaryDto`: Add Gems, ExperiencePoints
- [x] `LinkCode → LinkCodeResponseDto`: Map Code, ExpiresAt
- [x] Add mapping for merge/conflict scenarios
- [x] Add validation that PasswordHash is never exposed

**File**: `Mapping/UserMappingProfile.cs`

**Effort**: 45 minutes

---

### Phase 2 Summary
- **Total Effort**: ~3 hours | **Actual Effort**: ~1 hour
- **Risk**: Low (straightforward DTO creation)
- **Dependencies**: Phase 1 (entities must exist first)
- **Status**: ✅ COMPLETE
- **Build Status**: SUCCESS (0 new errors/warnings)

---

## Phase 3: Service Layer (Business Logic)

### Priority: HIGH - Core logic sits here
### Status: ✅ COMPLETE (January 13, 2026)

#### 3.1 Create Password Utility / Service
Create `Services/PasswordService.cs` or utility class:

- [x] `HashPasswordAsync(string password) → string` (bcrypt with 10-12 rounds)
- [x] `VerifyPasswordAsync(string plainPassword, string hash) → bool`
- [x] `ValidatePasswordAsync(string password) → (bool isValid, string? error)`
  - Min: 8 chars, Max: 128 chars
  - No forced complexity
  - Blacklist top 1000 compromised passwords + common patterns
- [x] Add NuGet: `BCrypt.Net-Next` (install if not present)
- [x] Externalize rounds to config (appsettings.json: `BcryptRounds: 10`)

**Files**: 
- `Services/PasswordService.cs` (new)
- `Services/Interfaces/IPasswordService.cs` (new)
- `appsettings.json` (add Security:BcryptRounds: 10)

**Effort**: 1-1.5 hours | **Actual Effort**: ~45 minutes

---

#### 3.2 Create LinkCode Utility / Service
Create `Services/LinkCodeService.cs`:

- [x] `GenerateCodeAsync() → string` (8 alphanumeric random; format: ABC12XYZ)
- [x] `GenerateLinkCodeAsync(int? userId) → LinkCodeResponseDto`
- [x] `ValidateLinkCodeAsync(string code) → (bool isValid, LinkCode? linkCode, string? error)`
- [x] `ConsumeLinkCodeAsync(string code) → (bool success, LinkCode? linkCode, string? error)`
- [x] `GetExpiredCodesAsync() → IEnumerable<LinkCode>`
- [x] `CleanupExpiredCodesAsync() → int` (count deleted)
- [x] Use cryptographically secure random (not `Random()`; use `RandomNumberGenerator`)

**File**: `Services/LinkCodeService.cs` (new)
**File**: `Services/Interfaces/ILinkCodeService.cs` (new)

**Effort**: 1.5 hours | **Actual Effort**: ~45 minutes

---

#### 3.3 Extend IUserService Interface
Add method signatures:

```csharp
// Validation
Task<(bool IsValid, string? ErrorMessage)> ValidateUserCreationAsync(UserCreateDto dto);
Task<(bool IsValid, string? ErrorMessage)> ValidatePasswordAsync(string password);

// Unique constraint checks
Task<(bool IsTaken, int? ConflictingUserId)> CheckUsernameTakenAsync(string username, int? excludeUserId = null);
Task<(bool IsTaken, int? ConflictingUserId)> CheckEmailTakenAsync(string email, int? excludeUserId = null);
Task<(bool IsTaken, int? ConflictingUserId)> CheckUuidTakenAsync(string uuid, int? excludeUserId = null);

// Credentials
Task ChangePasswordAsync(int userId, string currentPassword, string newPassword, string passwordConfirmation);
Task<bool> VerifyPasswordAsync(string plainPassword, string? passwordHash);
Task UpdateEmailAsync(int userId, string newEmail, string? currentPassword = null);

// Balances (Coins, Gems, ExperiencePoints)
// All mutations must be atomic, reject underflows, and record reason/context for audit
Task AdjustBalancesAsync(int userId, int coinsDelta, int gemsDelta, int experienceDelta, string reason, string? metadata = null);

// Link codes
Task<LinkCodeResponseDto> GenerateLinkCodeAsync(int? userId);
Task<(bool IsValid, UserDto? User)> ConsumeLinkCodeAsync(string code);
Task<IEnumerable<LinkCode>> GetExpiredLinkCodesAsync();
Task CleanupExpiredLinksAsync();

// Merging & Linking
Task<(bool HasConflict, int? SecondaryUserId)> CheckForDuplicateAsync(string uuid, string username);
Task<UserDto> MergeAccountsAsync(int primaryUserId, int secondaryUserId);
```

**File**: `Services/Interfaces/IUserService.cs`

**Effort**: 30 minutes | **Actual Effort**: ~15 minutes

---

#### 3.4 Implement Extended UserService

Update `Services/UserService.cs`:
Task AdjustBalancesAsync(int userId, int coinsDelta, int gemsDelta, int experienceDelta, string reason, string? metadata = null);

// Link codes
Task<LinkCodeResponseDto> GenerateLinkCodeAsync(int? userId);
Task<(bool IsValid, UserDto? User)> ConsumeLinkCodeAsync(string code);
Task<IEnumerable<LinkCode>> GetExpiredLinkCodesAsync();
Task CleanupExpiredLinksAsync();

// Merging & Linking
Task<(bool HasConflict, int? SecondaryUserId)> CheckForDuplicateAsync(string uuid, string username);
Task<UserDto> MergeAccountsAsync(int primaryUserId, int secondaryUserId);
```

**File**: `Services/Interfaces/IUserService.cs`

**Effort**: 30 minutes

---

#### 3.4 Implement Extended UserService

Update `Services/UserService.cs`:

- [x] Inject `ILinkCodeService` and `IPasswordService` dependencies
- [x] Implement all validation methods
  - ValidateUserCreationAsync
  - ValidatePasswordAsync
  - CheckUsernameTakenAsync
  - CheckEmailTakenAsync
  - CheckUuidTakenAsync
  
- [x] Implement password methods
  - ChangePasswordAsync
  - VerifyPasswordAsync
  - UpdateEmailAsync
  
- [x] Implement balance adjustment method
  - AdjustBalancesAsync (with underflow protection, atomic operations)
  
- [x] Implement link code methods
  - GenerateLinkCodeAsync
  - ConsumeLinkCodeAsync
  - GetExpiredLinkCodesAsync
  - CleanupExpiredLinksAsync
  
- [x] Implement merge methods
  - CheckForDuplicateAsync
  - MergeAccountsAsync

**File**: `Services/UserService.cs`

**Effort**: 3-4 hours | **Actual Effort**: ~2 hours

---

### Phase 3 Summary
- **Total Effort**: ~6.5 hours | **Actual Effort**: ~3.75 hours
- **Risk**: Low (business logic; well-defined requirements)
- **Dependencies**: Phase 1 (repository methods), Phase 2 (DTOs)
- **Status**: ✅ COMPLETE
- **Build Status**: SUCCESS (13 warnings, 0 errors)

**Deliverables**:
- ✅ PasswordService with bcrypt hashing (10 rounds configurable)
- ✅ Password validation (8-128 chars, weak password blacklist, pattern detection)
- ✅ LinkCodeService with cryptographically secure code generation (8 chars)
- ✅ Link code validation and consumption (20-minute expiration)
- ✅ Extended UserService with all account management methods
- ✅ Dependency injection registration
- ✅ Configuration in appsettings.json

**New Files Created**:
- Services/PasswordService.cs
- Services/LinkCodeService.cs
- Services/Interfaces/IPasswordService.cs
- Services/Interfaces/ILinkCodeService.cs

**Files Modified**:
- Services/UserService.cs (extended with new methods)
- Services/Interfaces/IUserService.cs (new method signatures)
- DependencyInjection/ServiceCollectionExtensions.cs (service registration)
- appsettings.json (Security configuration)
- appsettings.Development.json (Security configuration)
- Dtos/UserDtos.cs (added PasswordConfirmation to UserCreateDto)

---
  - UpdateEmailAsync

- [ ] Implement balance mutation method
  - AdjustBalancesAsync (Coins/Gems/ExperiencePoints): atomic, serialized, reject underflows, log reason/metadata
  - Ensure audit trail entries are created for Coins; log Gems/XP with lighter metadata but still recoverable
  
- [ ] Implement link code delegation methods
  - GenerateLinkCodeAsync (delegate to LinkCodeService)
  - ConsumeLinkCodeAsync (delegate to LinkCodeService)
  - GetExpiredLinkCodesAsync
  - CleanupExpiredLinksAsync
  
- [ ] Implement merge orchestration
  - CheckForDuplicateAsync
  - MergeAccountsAsync (orchestrates repo merge, logging)
  
- [ ] Update CreateAsync:
  - Call ValidateUserCreationAsync
  - Handle LinkCode if provided (validate & consume it)
  - Hash password if provided
  - Set AccountCreatedVia based on provided data
  - Generate LinkCode for response if web app first
  
- [ ] Add logging at key points (merge, password change, etc.)

**File**: `Services/UserService.cs`

**Effort**: 3-4 hours

---

### Phase 3 Summary
- **Total Effort**: ~6 hours
- **Risk**: Medium (business logic complexity, especially validation and merge)
- **Dependencies**: Phase 1 & 2, password hashing library

---

## Phase 4: API Controllers & Endpoints

### Priority: HIGH - Exposes all functionality

#### 4.1 Update UsersController GET Endpoints

- [ ] `GET /api/users` - Ensure response includes Gems, ExperiencePoints
- [ ] `GET /api/users/{id}` - Ensure response includes Gems, ExperiencePoints, EmailVerified
- [ ] `GET /api/users/uuid/{uuid}` - Same DTO structure
- [ ] `GET /api/users/username/{username}` - Same DTO structure

**Effort**: 30 minutes

---

#### 4.2 Update UsersController POST Create Endpoint

Update `POST /api/users`:

- [ ] Call `ValidateUserCreationAsync` first
- [ ] Return 400 with specific error if validation fails
- [ ] Check for duplicates (username, email, uuid if provided)
- [ ] If password provided: hash it before saving
- [ ] If LinkCode provided: validate and consume it
- [ ] Set AccountCreatedVia (WebApp if email provided, MinecraftServer if uuid provided)
- [ ] Generate LinkCode for response
- [ ] Return 201 with created user + LinkCode metadata
- [ ] Use transaction to ensure atomic operation

**Effort**: 1.5 hours

---

#### 4.3 Add Authentication Endpoints

Add to UsersController:

- [ ] `POST /api/users/generate-link-code` 
  - Request: { userId }
  - Response: 200 { code, expiresAt }
  - Errors: 400, 404
  
- [ ] `POST /api/users/validate-link-code/{code}`
  - Response: 200 { isValid, userId, username, email, error }
  - Errors: 400
  
- [ ] `PUT /api/users/{id}/change-password`
  - Request: { currentPassword, newPassword, passwordConfirmation }
  - Response: 204 No Content
  - Errors: 400, 401, 404
  - Verify current password before allowing change
  
- [ ] `PUT /api/users/{id}/update-email`
  - Request: { newEmail, currentPassword? }
  - Response: 204 No Content
  - Errors: 400 (duplicate email), 404
  - Validate email uniqueness
  
- [ ] `POST /api/users/check-duplicate`
  - Request: { uuid, username }
  - Response: 200 { hasDuplicate, conflictingUser?, primaryUser? }
  - For Minecraft plugin use

**Effort**: 2 hours

---

#### 4.4 Add Account Merge Endpoints

Add to UsersController:

- [ ] `POST /api/users/merge`
  - Request: { primaryUserId, secondaryUserId }
  - Response: 200 { mergedUser }
  - Errors: 400, 404
  - Verify both users exist
  - Call MergeAccountsAsync
  - Return merged user state
  
- [ ] `POST /api/users/link-account`
  - Request: { linkCode, email, password, passwordConfirmation }
  - For completing account via link code from Minecraft
  - Response: 200 { user }
  - Errors: 400, 404
  - Validate link code
  - Find user by link code
  - Update with email/password

**Effort**: 1.5 hours

---

#### 4.5 Error Response Consistency

- [ ] Create standard error response format
  - Example: `{ error: "ValidationFailed", message: "...", field: "...", code: "..." }`
- [ ] Use consistent HTTP status codes:
  - 400 Bad Request (validation, conflicts)
  - 401 Unauthorized (password incorrect)
  - 404 Not Found
  - 409 Conflict (duplicate constraint)
- [ ] Add detailed messages (see spec part B.7)

**Effort**: 1 hour

---

### Phase 4 Summary
- **Total Effort**: ~6.5 hours
- **Risk**: Medium (endpoint integration, error handling)
- **Dependencies**: Phase 1-3

---

## Phase 5: Testing

### Priority: HIGH - Ensures reliability

#### 5.1 Unit Tests: UserService
Create/update `Tests/Services/UserServiceTests.cs`:

- [ ] Test ValidateUserCreationAsync:
  - Valid input → passes
  - Missing username → fails
  - Duplicate username → fails
  - Invalid password → fails
  - Password mismatch → fails
  
- [ ] Test ValidatePasswordAsync:
  - Valid password → passes
  - Too short → fails
  - (Add complexity checks as per decision)
  
- [ ] Test CheckUsernameTaken:
  - Existing username → returns true with userId
  - New username → returns false
  - Excludes current user correctly
  
- [ ] Test ChangePasswordAsync:
  - Correct current password → succeeds
  - Incorrect current password → fails
  - New passwords don't match → fails
  
- [ ] Test MergeAccountsAsync:
  - Merges correctly
  - Secondary deleted
  - Primary retains data

**Effort**: 3 hours

---

#### 5.2 Unit Tests: LinkCodeService
Create `Tests/Services/LinkCodeServiceTests.cs`:

- [ ] Test GenerateCodeAsync:
  - Generates valid 6-char alphanumeric
  - Uniqueness (generate 100, no duplicates)
  
- [ ] Test ValidateLinkCodeAsync:
  - Valid code → passes
  - Expired code → fails with error
  - Non-existent code → fails
  
- [ ] Test ConsumeLinkCodeAsync:
  - Valid code → marks as Used, returns code
  - Expired code → fails
  
- [ ] Test CleanupExpiredCodesAsync:
  - Deletes only expired codes
  - Returns count
  - Doesn't delete active codes

**Effort**: 2 hours

---

#### 5.3 Integration Tests: Account Creation Flows
Create `Tests/Integration/AccountManagementTests.cs`:

- [ ] Test web app first flow:
  - Create user with email/password → succeeds
  - Link code generated → valid
  - Verify password hashed → not plaintext
  
- [ ] Test server first flow:
  - Create user with uuid/username only → succeeds
  - No email/password → nullable
  - Can generate link code later
  
- [ ] Test linking:
  - Valid link code → consumes and links
  - Expired code → fails
  - Duplicate detected → returns conflict
  
- [ ] Test merge:
  - Two users for same player → merge succeeds
  - Secondary deleted → verify FK cascade
  - Primary retains Coins/Gems/XP from winning side

**Effort**: 3-4 hours

---

#### 5.4 Integration Tests: Unique Constraints
Create `Tests/Integration/UniqueConstraintTests.cs`:

- [ ] Duplicate username → fails with specific error
- [ ] Duplicate email → fails with specific error
- [ ] Duplicate uuid → fails (if not null) with specific error
- [ ] Exclude current user from check → allows no-op updates

**Effort**: 1.5 hours

---

#### 5.5 API Endpoint Tests
Create `Tests/Api/UsersControllerTests.cs`:

- [ ] Test POST /api/users (create):
  - Valid web app signup → 201
  - Valid server join → 201 (minimal data)
  - Duplicate username → 400
  - Link code invalid → 400
  
- [ ] Test POST /api/users/generate-link-code:
  - Valid user → 200 with code + expiration
  - User not found → 404
  
- [ ] Test POST /api/users/validate-link-code/{code}:
  - Valid code → 200 with validation details
  - Expired code → 400
  - Invalid code → 400
  
- [ ] Test PUT /api/users/{id}/change-password:
  - Current password correct → 204
  - Current password wrong → 401
  - New passwords don't match → 400
  
- [ ] Test POST /api/users/check-duplicate:
  - Duplicate found → 200 with details
  - No duplicate → 200 { hasDuplicate: false }

**Effort**: 2-3 hours

---

### Phase 5 Summary
- **Total Effort**: ~12-14 hours
- **Risk**: Low (testing is straightforward, no new concepts)
- **Critical for**: Ensuring merge logic works correctly, password validation, unique constraints

---

## Phase 6: Documentation & Cleanup

### Priority: MEDIUM

- [ ] Add XML documentation to all public methods
- [ ] Update API documentation/Swagger comments
- [ ] Create developer guide: "How to add new account validation rules"
- [ ] Document password hashing approach in comments
- [ ] Document foreign key handling in merge logic
- [ ] Update README with new endpoints

**Effort**: 2 hours

---

## Phase 7: Future Enhancements (Out of Scope for MVP)

- [ ] Email verification flow (optional emails)
- [ ] Password reset / "Forgot Password"
- [ ] Session invalidation on password change
- [ ] Audit logging of account changes
- [ ] Background job for link code cleanup (Hangfire)
- [ ] Account deactivation (IsActive = false)
- [ ] Rate limiting on password attempts
- [ ] 2FA / MFA support

---

## Implementation Priority Matrix

| Phase | Component | Duration | Risk | Blocker | Status |
|-------|-----------|----------|------|---------|--------|
| 1 | Data Model & Repos | 4h | Low | None (no FK) | Not Started |
| 2 | DTOs & Mapping | 3h | Low | Phase 1 | Not Started |
| 3 | Service Layer | 6.5h | Med | Phase 1-2 | Not Started |
| 4 | Controllers | 6.5h | Med | Phase 1-3 | Not Started |
| 5 | Testing | 12-14h | Low | Phase 1-4 | Not Started |
| 6 | Documentation | 2h | Low | Phase 1-5 | Not Started |

**Total Estimated Effort**: ~34-37 hours (development + review)

**Recommended Timeline**: 
- Week 1: Phases 1-2 (Entity + DTO foundation)
- Week 2: Phase 3 (Service logic + password/link code utilities)
- Week 3: Phase 4 (API endpoints)
- Week 4: Phase 5 (Testing + fixes)

---

## ✅ DESIGN DECISIONS: ALL CONFIRMED

All critical decisions have been made and documented. See **SPEC_USER_ACCOUNT_MANAGEMENT.md Part F** for full details.

### Quick Reference Summary

| Decision | Confirmed Choice |
|----------|-----------------|
| **Password Policy** | 8-128 chars, no forced complexity, blacklist weak passwords |
| **Link Code** | 8 alphanumeric chars (ABC12XYZ), formatted as ABC-12XYZ |
| **Account Merge** | Winner's values only (no consolidation) |
| **Delete Strategy** | Soft delete with 90-day TTL |
| **Audit Trail** | Minimal (track in User model: LastPasswordChangeAt, LastEmailChangeAt, DeletedAt, DeletedReason) |
| **Foreign Keys** | None currently; merge logic is straightforward |

---

## Getting Started Checklist

- [x] Review SPEC_USER_ACCOUNT_MANAGEMENT.md in full
- [x] Confirm all Part F design decisions ✅
- [x] Verify foreign key usage of User entity (confirmed: none)
- [x] Decide on merge strategy (soft delete with winner's values)
- [ ] Set up test database for safe migration testing
- [ ] Install BCrypt.Net-Next NuGet package
- [ ] Download/embed top 1000 weak passwords list
- [ ] Create feature branch: `feature/user-account-management`
- [ ] Begin Phase 1: Data model updates

---

**Document Version**: 1.0  
**Last Updated**: January 7, 2026  
**Ready to Start**: After clarifications in Part F are addressed
