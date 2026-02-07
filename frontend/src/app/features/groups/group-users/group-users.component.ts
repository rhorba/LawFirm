import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { GroupService } from '../../../core/services/group.service';
import { UserService } from '../../../services/user.service';
import { GroupResponse } from '../../../core/models/group.model';
import { UserResponse } from '../../../core/models/user.model';

@Component({
  selector: 'app-group-users',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './group-users.component.html',
  styleUrl: './group-users.component.css',
})
export class GroupUsersComponent implements OnInit {
  private groupService = inject(GroupService);
  private userService = inject(UserService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  group: GroupResponse | null = null;
  allUsers: UserResponse[] = [];
  availableUsers: UserResponse[] = [];
  groupId: number | null = null;
  loading = false;
  error: string | null = null;
  showAddUsersModal = false;
  selectedUserIds: number[] = [];

  ngOnInit(): void {
    this.route.params.subscribe((params) => {
      this.groupId = +params['id'];
      this.loadGroup();
      this.loadAllUsers();
    });
  }

  loadGroup(): void {
    if (!this.groupId) return;

    this.loading = true;
    this.groupService.getGroupById(this.groupId).subscribe({
      next: (data) => {
        this.group = data;
        this.loading = false;
        this.updateAvailableUsers();
      },
      error: (err) => {
        this.error = 'Failed to load group';
        this.loading = false;
        console.error(err);
      },
    });
  }

  loadAllUsers(): void {
    this.userService.searchUsers({ page: 0, size: 1000 }).subscribe({
      next: (response) => {
        this.allUsers = response.content;
        this.updateAvailableUsers();
      },
      error: (err) => {
        console.error('Failed to load users', err);
      },
    });
  }

  updateAvailableUsers(): void {
    if (!this.group) return;

    const groupUserIds = this.group.users?.map((u) => u.id) || [];
    this.availableUsers = this.allUsers.filter((user) => !groupUserIds.includes(user.id));
  }

  openAddUsersModal(): void {
    this.selectedUserIds = [];
    this.showAddUsersModal = true;
  }

  closeAddUsersModal(): void {
    this.showAddUsersModal = false;
    this.selectedUserIds = [];
  }

  toggleUserSelection(userId: number): void {
    const index = this.selectedUserIds.indexOf(userId);
    if (index > -1) {
      this.selectedUserIds.splice(index, 1);
    } else {
      this.selectedUserIds.push(userId);
    }
  }

  isUserSelected(userId: number): boolean {
    return this.selectedUserIds.includes(userId);
  }

  addUsers(): void {
    if (!this.groupId || this.selectedUserIds.length === 0) return;

    this.loading = true;
    this.groupService.assignUsers(this.groupId, { userIds: this.selectedUserIds }).subscribe({
      next: () => {
        this.closeAddUsersModal();
        this.loadGroup();
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to add users';
        this.loading = false;
      },
    });
  }

  removeUser(userId: number, username: string): void {
    if (!this.groupId) return;

    if (confirm(`Remove user "${username}" from this group?`)) {
      this.groupService.removeUser(this.groupId, userId).subscribe({
        next: () => {
          this.loadGroup();
        },
        error: (err) => {
          alert(err.error?.message || 'Failed to remove user');
        },
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/groups']);
  }
}
