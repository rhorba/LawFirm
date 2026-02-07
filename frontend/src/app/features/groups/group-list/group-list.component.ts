import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { GroupService } from '../../../core/services/group.service';
import { GroupResponse } from '../../../core/models/group.model';

@Component({
  selector: 'app-group-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './group-list.component.html',
  styleUrl: './group-list.component.css',
})
export class GroupListComponent implements OnInit {
  private groupService = inject(GroupService);
  private router = inject(Router);

  groups: GroupResponse[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    this.loadGroups();
  }

  loadGroups(): void {
    this.loading = true;
    this.error = null;
    this.groupService.getAllGroups().subscribe({
      next: (data) => {
        this.groups = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load groups';
        this.loading = false;
        console.error(err);
      },
    });
  }

  createGroup(): void {
    this.router.navigate(['/groups/create']);
  }

  editGroup(id: number): void {
    this.router.navigate(['/groups/edit', id]);
  }

  deleteGroup(id: number, name: string): void {
    if (confirm(`Are you sure you want to delete group "${name}"?`)) {
      this.groupService.deleteGroup(id).subscribe({
        next: () => {
          this.loadGroups();
        },
        error: (err) => {
          alert(err.error?.message || 'Failed to delete group');
        },
      });
    }
  }

  manageUsers(id: number): void {
    this.router.navigate(['/groups', id, 'users']);
  }
}
