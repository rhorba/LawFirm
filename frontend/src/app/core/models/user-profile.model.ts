export interface UserProfile {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  bio?: string;
}

export interface UpdateProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  phoneNumber?: string | null;
  bio?: string | null;
}
