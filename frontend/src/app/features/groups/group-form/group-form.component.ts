import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupService } from '../../../core/services/group.service';
import { UserService } from '../../../services/user.service';
import { GroupRequest } from '../../../core/models/group.model';
import { RoleResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-group-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './group-form.component.html',
  styleUrl: './group-form.component.css',
})
export class GroupFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  private groupService = inject(GroupService);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  groupForm: FormGroup;
  roles: RoleResponse[] = [];
  isEditMode = false;
  groupId: number | null = null;
  loading = false;
  error: string | null = null;

  constructor() {
    this.groupForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', Validators.maxLength(255)],
      roleIds: [[]],
    });
  }

  ngOnInit(): void {
    this.loadRoles();

    this.route.params.subscribe((params) => {
      if (params['id']) {
        this.isEditMode = true;
        this.groupId = +params['id'];
        this.loadGroup(this.groupId);
      }
    });
  }

  loadRoles(): void {
    this.userService.getRoles().subscribe({
      next: (data) => {
        this.roles = data;
      },
      error: (err) => {
        console.error('Failed to load roles', err);
      },
    });
  }

  loadGroup(id: number): void {
    this.groupService.getGroupById(id).subscribe({
      next: (group) => {
        this.groupForm.patchValue({
          name: group.name,
          description: group.description,
          roleIds: group.roles.map((r) => r.id),
        });
      },
      error: (err) => {
        this.error = 'Failed to load group';
        console.error(err);
      },
    });
  }

  toggleRole(roleId: number): void {
    const roleIds = this.groupForm.get('roleIds')?.value || [];
    const index = roleIds.indexOf(roleId);

    if (index > -1) {
      roleIds.splice(index, 1);
    } else {
      roleIds.push(roleId);
    }

    this.groupForm.patchValue({ roleIds });
  }

  isRoleSelected(roleId: number): boolean {
    const roleIds = this.groupForm.get('roleIds')?.value || [];
    return roleIds.includes(roleId);
  }

  onSubmit(): void {
    if (this.groupForm.invalid) {
      return;
    }

    this.loading = true;
    this.error = null;

    const request: GroupRequest = this.groupForm.value;

    const operation =
      this.isEditMode && this.groupId
        ? this.groupService.updateGroup(this.groupId, request)
        : this.groupService.createGroup(request);

    operation.subscribe({
      next: () => {
        this.router.navigate(['/groups']);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to save group';
        this.loading = false;
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/groups']);
  }
}
