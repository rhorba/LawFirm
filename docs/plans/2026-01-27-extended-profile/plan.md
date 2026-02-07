# Plan: Extended User Profile

## Phase 1: Backend Implementation

### Step 1: Database Migration
- [x] Create `backend/src/main/resources/db/migration/V11__create_user_profiles_table.sql`
    - Define table with `user_id` FK and columns.

### Step 2: Domain Entity
- [x] Create `backend/src/main/java/com/boilerplate/domain/model/UserProfile.java`
    - Annotate with `@Entity`, `@OneToOne`.
- [x] Create `backend/src/main/java/com/boilerplate/domain/repository/UserProfileRepository.java`

### Step 3: DTOs & Mapper
- [x] Create `backend/src/main/java/com/boilerplate/application/dto/request/UpdateProfileRequest.java`
- [x] Create `backend/src/main/java/com/boilerplate/application/dto/response/UserProfileResponse.java`
- [x] Create `backend/src/main/java/com/boilerplate/application/mapper/UserProfileMapper.java`

### Step 4: Service Logic
- [x] Create `backend/src/main/java/com/boilerplate/application/service/UserProfileService.java`
    - Implement `getProfile(userId)` (Throw custom `ResourceNotFoundException` if missing).
    - Implement `upsertProfile(userId, request)`.

### Step 5: Controller
- [x] Create `backend/src/main/java/com/boilerplate/presentation/controller/UserProfileController.java`
    - `GET /api/profile/me`
    - `PUT /api/profile/me`

## Phase 2: Frontend Implementation

### Step 6: Models & Service
- [x] Create `frontend/src/app/core/models/user-profile.model.ts`
- [x] Create `frontend/src/app/services/profile.service.ts`
    - `getProfile()`
    - `updateProfile(data)`

### Step 7: Profile Component Update
- [x] Modify `frontend/src/app/features/profile/profile.component.ts`
    - Add state handling for "Loading", "Found", and "Not Found".
    - Add "Edit" toggle logic.
- [x] Modify `frontend/src/app/features/profile/profile.component.html`
    - Add "Profile Incomplete" placeholder.
    - Add Form inputs for editing.
