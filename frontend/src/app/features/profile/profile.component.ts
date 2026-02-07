import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../services/profile.service';
import { UserProfile } from '../../core/models/user-profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  authService = inject(AuthService);
  private profileService = inject(ProfileService);
  private fb = inject(FormBuilder);

  profile = signal<UserProfile | null>(null);
  isLoading = signal<boolean>(true);
  isEditing = signal<boolean>(false);
  error = signal<string | null>(null);

  profileForm = this.fb.group({
    firstName: ['', [Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.maxLength(20)]],
    bio: [''],
  });

  get userInitial(): string {
    const user = this.authService.currentUser();
    return user ? user.username.charAt(0).toUpperCase() : '?';
  }

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.profileService.getProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        this.isLoading.set(false);
        this.error.set(null);
      },
      error: (err) => {
        if (err.status === 404) {
          this.profile.set(null);
        } else {
          this.error.set('Failed to load profile');
        }
        this.isLoading.set(false);
      },
    });
  }

  toggleEdit(): void {
    const currentProfile = this.profile();
    if (currentProfile) {
      this.profileForm.patchValue(currentProfile);
    }
    this.isEditing.set(!this.isEditing());
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;

    this.isLoading.set(true);
    this.profileService.updateProfile(this.profileForm.value).subscribe({
      next: (updatedProfile) => {
        this.profile.set(updatedProfile);
        this.isEditing.set(false);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to update profile');
        this.isLoading.set(false);
      },
    });
  }
}
