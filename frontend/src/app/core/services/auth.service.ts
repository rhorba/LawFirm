import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { TokenService } from './token.service';
import { UserResponse } from '../models/user.model';

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserResponse;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);
  private router = inject(Router);

  currentUser = signal<UserResponse | null>(null);
  isAuthenticated = signal<boolean>(false);

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap((response) => {
        this.tokenService.setAccessToken(response.accessToken);
        this.tokenService.setRefreshToken(response.refreshToken);
        this.currentUser.set(response.user);
        this.isAuthenticated.set(true);
      })
    );
  }

  register(data: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, data).pipe(
      tap((response) => {
        this.tokenService.setAccessToken(response.accessToken);
        this.tokenService.setRefreshToken(response.refreshToken);
        this.currentUser.set(response.user);
        this.isAuthenticated.set(true);
      })
    );
  }

  logout(): void {
    this.tokenService.clearTokens();
    this.currentUser.set(null);
    this.isAuthenticated.set(false);
    this.router.navigate(['/login']);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http
      .post<AuthResponse>(
        `${environment.apiUrl}/auth/refresh`,
        {},
        { headers: { Authorization: `Bearer ${refreshToken}` } }
      )
      .pipe(
        tap((response) => {
          this.tokenService.setAccessToken(response.accessToken);
          this.currentUser.set(response.user);
        })
      );
  }

  hasPermission(permission: string): boolean {
    const user = this.currentUser();
    if (!user || !user.roles) return false;
    return user.roles.some(
      (role) => role.permissions && role.permissions.some((p) => p.name === permission)
    );
  }

  hasRole(roleName: string): boolean {
    const user = this.currentUser();
    if (!user || !user.roles) return false;
    return user.roles.some((role) => role.name === roleName);
  }
}

export { UserResponse, RoleResponse, PermissionResponse } from '../models/user.model';
