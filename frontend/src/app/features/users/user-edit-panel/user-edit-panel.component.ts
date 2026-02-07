import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import {
  UserResponse,
  UpdateUserRequest,
  CreateUserRequest,
} from '../../../core/models/user.model';

@Component({
  selector: 'app-user-edit-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-edit-panel.component.html',
})
export class UserEditPanelComponent implements OnInit {
  @Input() user: UserResponse | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() saved = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private userService = inject(UserService);

  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);
  isCreateMode = computed(() => this.user === null);

  editForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email]],
    password: [''],
    enabled: [true],
  });

  ngOnInit(): void {
    if (this.user) {
      this.editForm.patchValue({
        username: this.user.username,
        email: this.user.email,
        enabled: this.user.enabled,
      });
    } else {
      const passwordCtrl = this.editForm.get('password')!;
      passwordCtrl.setValidators([Validators.required, Validators.minLength(8)]);
      passwordCtrl.updateValueAndValidity();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.close.emit();
  }

  onSubmit(): void {
    if (this.editForm.invalid) return;

    const formValue = this.editForm.getRawValue();

    this.loading.set(true);
    this.error.set(null);

    if (this.isCreateMode()) {
      const request: CreateUserRequest = {
        username: formValue.username,
        email: formValue.email,
        password: formValue.password,
      };

      this.userService.createUser(request).subscribe({
        next: () => {
          this.loading.set(false);
          this.saved.emit();
        },
        error: (err) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to create user'));
          this.loading.set(false);
        },
      });
    } else {
      const request: UpdateUserRequest = {};

      if (formValue.username !== this.user!.username) request.username = formValue.username;
      if (formValue.email !== this.user!.email) request.email = formValue.email;
      if (formValue.password) request.password = formValue.password;
      if (formValue.enabled !== this.user!.enabled) request.enabled = formValue.enabled;

      if (Object.keys(request).length === 0) {
        this.close.emit();
        return;
      }

      this.userService.updateUser(this.user!.id, request).subscribe({
        next: () => {
          this.loading.set(false);
          this.saved.emit();
        },
        error: (err) => {
          this.error.set(this.extractErrorMessage(err, 'Failed to update user'));
          this.loading.set(false);
        },
      });
    }
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    const httpErr = err as {
      error?: { message?: string; validationErrors?: Record<string, string> };
    };
    const body = httpErr.error;
    if (body?.validationErrors) {
      return Object.values(body.validationErrors).join('. ');
    }
    return body?.message || fallback;
  }

  onOverlayClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('panel-overlay')) {
      this.close.emit();
    }
  }
}
