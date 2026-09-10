import { TestBed } from '@angular/core/testing';
import { LoginResponse } from '../models/auth.models';
import { TokenStorageService } from './token-storage.service';

describe('TokenStorageService', () => {
  let service: TokenStorageService;

  beforeEach(() => {
    sessionStorage.clear();
    service = TestBed.inject(TokenStorageService);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should persist and clear an authenticated session', () => {
    const response: LoginResponse = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900,
      refreshExpiresIn: 604800,
      userId: 10,
      email: 'admin@finpay.test',
      merchantId: 20,
      merchantName: 'FinPay Store',
      role: 'MERCHANT_ADMIN',
    };

    const savedSession = service.save(response);

    expect(service.read()).toEqual(savedSession);
    expect(service.getAccessToken()).toBe('access-token');
    expect(service.getRefreshToken()).toBe('refresh-token');

    service.clear();

    expect(service.read()).toBeNull();
  });
});
