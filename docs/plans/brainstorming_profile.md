# Brainstorming: Extended User Profile

## Context
The current `User` entity only contains authentication data (`username`, `email`, `password`). The "Profile" page is currently read-only and sparse. To make the application viable for real-world usage, we need to capture humanizing details.

## Proposed Features
1.  **Database**: Add `first_name`, `last_name`, `phone_number`, and `bio` columns to the `users` table via Flyway.
2.  **Backend**: Add `updateProfile()` endpoint to `UserController` allowing users to modify these new fields.
3.  **Frontend**: Transform the `ProfileComponent` into a view/edit interface or add a specific "Edit Profile" modal/page.

## Question
Should we proceed with adding **Personal Details** (First Name, Last Name, Phone, Bio) to the `User` entity?
