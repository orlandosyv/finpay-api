import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthSessionService } from './auth-session.service';
import { authTokenInterceptor } from './auth-token.interceptor';
import { refreshTokenInterceptor } from './refresh-token.interceptor';
import { TokenStorageService } from './token-storage.service';
import { LoginResponse } from '../models/auth.models';
import { HttpClient } from '@angular/common/http';

describe('authentication interceptors', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let tokenStorage: TokenStorageService;

  const loginResponse = (accessToken: string, refreshToken: string): LoginResponse => ({
    accessToken,
    refreshToken,
    tokenType: 'Bearer',
    expiresIn: 900,
    refreshExpiresIn: 604800,
    userId: 10,
    email: 'admin@finpay.test',
    merchantId: 20,
    merchantName: 'FinPay Store',
    role: 'MERCHANT_ADMIN',
  });

  beforeEach(() => {
    sessionStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authTokenInterceptor, refreshTokenInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    tokenStorage = TestBed.inject(TokenStorageService);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('should add Bearer only to protected API requests', () => {
    tokenStorage.save(loginResponse('stored-access', 'stored-refresh'));

    http.get('/api/merchant/me').subscribe();
    const protectedRequest = httpTesting.expectOne('/api/merchant/me');
    expect(protectedRequest.request.headers.get('Authorization')).toBe('Bearer stored-access');
    protectedRequest.flush({});

    http
      .post('/api/auth/login', {
        email: 'admin@finpay.test',
        password: 'StrongPassword123!',
      })
      .subscribe();
    const loginRequest = httpTesting.expectOne('/api/auth/login');
    expect(loginRequest.request.headers.has('Authorization')).toBe(false);
    loginRequest.flush(loginResponse('new-access', 'new-refresh'));
  });

  it('should refresh once and retry the failed protected request', () => {
    tokenStorage.save(loginResponse('expired-access', 'current-refresh'));
    TestBed.inject(AuthSessionService);

    http.get('/api/merchant/me').subscribe();

    const firstRequest = httpTesting.expectOne('/api/merchant/me');
    expect(firstRequest.request.headers.get('Authorization')).toBe('Bearer expired-access');
    firstRequest.flush(
      {
        status: 401,
        error: 'Unauthorized',
        message: 'Access token expired',
        path: '/api/merchant/me',
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    expect(refreshRequest.request.headers.has('Authorization')).toBe(false);
    expect(refreshRequest.request.body).toEqual({
      refreshToken: 'current-refresh',
    });
    refreshRequest.flush(loginResponse('rotated-access', 'rotated-refresh'));

    const retriedRequest = httpTesting.expectOne('/api/merchant/me');
    expect(retriedRequest.request.headers.get('Authorization')).toBe('Bearer rotated-access');
    retriedRequest.flush({
      merchantId: 20,
      merchantName: 'FinPay Store',
      merchantStatus: 'ACTIVE',
      userId: 10,
      email: 'admin@finpay.test',
      role: 'MERCHANT_ADMIN',
    });

    expect(tokenStorage.getRefreshToken()).toBe('rotated-refresh');
  });

  it('preserves the idempotency key and session when the retried payment returns 409', () => {
    tokenStorage.save(loginResponse('expired', 'refresh'));
    let status = 0;
    http
      .post(
        '/api/payments',
        { amount: '150.00', currency: 'PEN' },
        {
          headers: { 'Idempotency-Key': 'same-operation' },
        },
      )
      .subscribe({ error: (e) => (status = e.status) });
    httpTesting.expectOne('/api/payments').flush({}, { status: 401, statusText: 'Unauthorized' });
    httpTesting.expectOne('/api/auth/refresh').flush(loginResponse('rotated', 'rotated-refresh'));
    const retry = httpTesting.expectOne('/api/payments');
    expect(retry.request.headers.get('Idempotency-Key')).toBe('same-operation');
    retry.flush({}, { status: 409, statusText: 'Conflict' });
    expect(status).toBe(409);
    expect(tokenStorage.getAccessToken()).toBe('rotated');
  });

  it('should retry logout with both rotated tokens', () => {
    tokenStorage.save(loginResponse('expired-access', 'current-refresh'));
    const authSession = TestBed.inject(AuthSessionService);

    authSession.logout().subscribe();

    const firstLogout = httpTesting.expectOne('/api/auth/logout');
    expect(firstLogout.request.headers.get('Authorization')).toBe('Bearer expired-access');
    expect(firstLogout.request.body).toEqual({
      refreshToken: 'current-refresh',
    });
    firstLogout.flush(
      {
        status: 401,
        error: 'Unauthorized',
        message: 'Access token expired',
        path: '/api/auth/logout',
      },
      { status: 401, statusText: 'Unauthorized' },
    );

    const refreshRequest = httpTesting.expectOne('/api/auth/refresh');
    refreshRequest.flush(loginResponse('rotated-access', 'rotated-refresh'));

    const retriedLogout = httpTesting.expectOne('/api/auth/logout');
    expect(retriedLogout.request.headers.get('Authorization')).toBe('Bearer rotated-access');
    expect(retriedLogout.request.body).toEqual({
      refreshToken: 'rotated-refresh',
    });
    retriedLogout.flush(null);

    expect(tokenStorage.read()).toBeNull();
  });
});
