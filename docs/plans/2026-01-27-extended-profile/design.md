# Design: Extended User Profile (Lazy Creation)

## 1. Overview
We will implement a separate `UserProfile` entity to store extended user details. This keeps the core authentication table lightweight. The profile will be created lazily (on-demand).

## 2. Database Schema
**Table**: `user_profiles`
- `id` (BIGINT PK): Auto-increment.
- `user_id` (BIGINT FK): Unique foreign key to `users.id`.
- `first_name` (VARCHAR 100): Optional.
- `last_name` (VARCHAR 100): Optional.
- `phone_number` (VARCHAR 20): Optional.
- `bio` (TEXT): Optional.
- `created_at`, `updated_at`: Standard auditing.

**Migration**: `V11__create_user_profiles_table.sql`

## 3. Backend Architecture
- **Entity**: `UserProfile` (1:1 relationship with `User`).
- **Repository**: `UserProfileRepository`.
- **DTOs**: `UserProfileResponse`, `UpdateProfileRequest`.
- **Service**: `UserProfileService`
    - `getProfile(userId)`: Returns Optional/throws NotFound.
    - `upsertProfile(userId, request)`: Creates or updates.
- **Controller**: `UserProfileController` (`/api/profile`)
    - `GET /me`: Returns profile or 404.
    - `PUT /me`: Upsert profile.

## 4. Frontend Architecture
- **Model**: `UserProfile` interface.
- **Service**: `ProfileService` (fetches/updates).
- **Component**: `ProfileComponent`
    - **State**: `profile: Signal<UserProfile | null>`, `isLoading`, `error`.
    - **Logic**:
        - On init, fetch profile.
        - If 404, set `profile = null` (shows "Incomplete" UI).
        - "Create Profile" button opens Edit Mode.
        - Save calls `PUT /me` and refreshes state.

## 5. Security
- Only the authenticated user can view/edit their own profile (verified via `SecurityContext`).
- Input validation on backend (lengths, sanitization).
