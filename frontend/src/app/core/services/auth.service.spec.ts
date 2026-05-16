import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService, LoginRequest } from './auth.service';
import { TokenService } from './token.service';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let tokenService: jasmine.SpyObj<TokenService>;

  const mockAuthResponse = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: { id: 1, username: 'admin', email: 'admin@test.com', enabled: true, groups: [] },
  };

  beforeEach(() => {
    const tokenSpy = jasmine.createSpyObj('TokenService', [
      'setAccessToken', 'setRefreshToken', 'clearTokens',
      'getAccessToken', 'getRefreshToken',
    ]);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [
        AuthService,
        { provide: TokenService, useValue: tokenSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    tokenService = TestBed.inject(TokenService) as jasmine.SpyObj<TokenService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('login should POST credentials and set tokens + signals', () => {
    const credentials: LoginRequest = { username: 'admin', password: 'admin123' };

    service.login(credentials).subscribe((res) => {
      expect(res.accessToken).toBe('access-token');
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(tokenService.setAccessToken).toHaveBeenCalledWith('access-token');
    expect(tokenService.setRefreshToken).toHaveBeenCalledWith('refresh-token');
    expect(service.currentUser()?.username).toBe('admin');
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('logout should clear tokens and reset signals', () => {
    service.logout();

    expect(tokenService.clearTokens).toHaveBeenCalled();
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('hasPermission should return false when user has no groups', () => {
    expect(service.hasPermission('CASE_READ')).toBeFalse();
  });

  it('hasPermission should return true when user group has the permission', () => {
    service.currentUser.set({
      id: 1, username: 'admin', email: 'admin@test.com', enabled: true,
      groups: [{ id: 1, name: 'Admin', permissions: [{ id: 1, name: 'CASE_READ', description: '' }], roles: [] }],
    } as any);

    expect(service.hasPermission('CASE_READ')).toBeTrue();
    expect(service.hasPermission('CASE_DELETE')).toBeFalse();
  });

  it('hasRole should return true when user group has the role', () => {
    service.currentUser.set({
      id: 1, username: 'admin', email: 'admin@test.com', enabled: true,
      groups: [{ id: 1, name: 'Admin', permissions: [], roles: [{ id: 1, name: 'ADMIN', description: '' }] }],
    } as any);

    expect(service.hasRole('ADMIN')).toBeTrue();
    expect(service.hasRole('MODERATOR')).toBeFalse();
  });

  it('refreshToken should POST and update access token signal', () => {
    tokenService.getRefreshToken.and.returnValue('old-refresh');

    service.refreshToken().subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(tokenService.setAccessToken).toHaveBeenCalledWith('access-token');
  });
});
