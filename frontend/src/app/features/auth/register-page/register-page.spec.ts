import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { RegisterPage } from './register-page';
describe('Merchant registration', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('blocks weak passwords before sending', () => {
    const c = TestBed.createComponent(RegisterPage).componentInstance;
    c.form.setValue({ merchantName: 'Shop', email: 'owner@example.test', password: 'weak' });
    c.submit();
    expect(c.form.invalid).toBe(true);
    http.expectNone('/api/auth/register');
  });
  it('displays duplicate email then handles successful registration and clears password', () => {
    const c = TestBed.createComponent(RegisterPage).componentInstance;
    c.form.setValue({
      merchantName: 'Shop',
      email: 'owner@example.test',
      password: 'StrongPassword123!',
    });
    c.submit();
    http
      .expectOne('/api/auth/register')
      .flush({ message: 'Email already registered' }, { status: 409, statusText: 'Conflict' });
    expect(c.error()).toContain('Email already registered');
    c.form.controls.email.setValue('owner2@example.test');
    c.submit();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.method).toBe('POST');
    request.flush({ merchantId: 1 }, { status: 201, statusText: 'Created' });
    expect(c.created()).toBe(true);
    expect(c.form.controls.password.value).toBe('');
  });
});
