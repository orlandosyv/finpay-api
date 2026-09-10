import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthApiService } from './auth-api.service';
import { LoginResponse } from '../models/auth.models';

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(AuthApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should send the credentials to the login endpoint', () => {
    const response: LoginResponse = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshExpiresIn: 604800,
      userId: 10,
      email: 'admin@finpay.test',
      merchantId: 20,
      merchantName: 'FinPay Test Store',
      role: 'MERCHANT_ADMIN',
    };

    service
      .login({
        email: 'admin@finpay.test',
        password: 'StrongPassword123!',
      })
      .subscribe((result) => {
        expect(result).toEqual(response);
      });

    const request = httpTesting.expectOne('/api/auth/login');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'admin@finpay.test',
      password: 'StrongPassword123!',
    });
    request.flush(response);
  });
});
