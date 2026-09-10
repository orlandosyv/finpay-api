import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { LoginResponse, MerchantRole } from '../models/auth.models';
import { adminGuard } from './admin.guard';
import { TokenStorageService } from './token-storage.service';

describe('adminGuard', () => {
  let tokenStorage: TokenStorageService;
  let router: Router;

  const loginResponse = (role: MerchantRole): LoginResponse => ({
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    expiresIn: 900,
    refreshExpiresIn: 604800,
    userId: 10,
    email: 'user@finpay.test',
    merchantId: 20,
    merchantName: 'FinPay Store',
    role,
  });

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });

    tokenStorage = TestBed.inject(TokenStorageService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should allow a merchant administrator', () => {
    tokenStorage.save(loginResponse('MERCHANT_ADMIN'));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, { url: '/app/team' } as RouterStateSnapshot),
    );

    expect(result).toBe(true);
  });

  it('should redirect a merchant user to overview', () => {
    tokenStorage.save(loginResponse('MERCHANT_USER'));

    const result = TestBed.runInInjectionContext(() =>
      adminGuard({} as ActivatedRouteSnapshot, { url: '/app/team' } as RouterStateSnapshot),
    ) as UrlTree;

    expect(router.serializeUrl(result)).toBe('/app/overview');
  });
});
