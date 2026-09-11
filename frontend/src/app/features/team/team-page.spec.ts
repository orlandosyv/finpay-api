import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { TeamPage } from './team-page';

describe('TeamPage', () => {
  let http: HttpTestingController;
  const admin = {
    membershipId: 1,
    userId: 10,
    email: 'admin@store.com',
    status: 'ACTIVE',
    role: 'MERCHANT_ADMIN',
    createdAt: '2026-09-10T12:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TeamPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function createPage() {
    const fixture = TestBed.createComponent(TeamPage);
    fixture.detectChanges();
    return fixture;
  }

  it('lists the users returned for the authenticated merchant', () => {
    const fixture = createPage();
    const request = http.expectOne('/api/merchant/users');
    expect(request.request.method).toBe('GET');
    request.flush([admin]);
    fixture.detectChanges();

    expect(fixture.componentInstance.users().length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('admin@store.com');
    expect(fixture.nativeElement.textContent).toContain('MERCHANT_ADMIN');
  });

  it('does not send invalid user data', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/users').flush([]);
    const page = fixture.componentInstance;
    page.form.setValue({ email: 'invalid', password: 'short', role: 'MERCHANT_USER' });
    page.createUser();

    http.expectNone((request) => request.method === 'POST');
    expect(page.form.invalid).toBe(true);
  });

  it('creates a user with its selected role and clears sensitive data', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/users').flush([admin]);
    const page = fixture.componentInstance;
    page.form.setValue({
      email: ' Operator@Store.com ',
      password: 'OperatorPassword123!',
      role: 'MERCHANT_USER',
    });
    page.createUser();

    const request = http.expectOne('/api/merchant/users');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      email: 'operator@store.com',
      password: 'OperatorPassword123!',
      role: 'MERCHANT_USER',
    });
    request.flush({
      membershipId: 2,
      userId: 11,
      email: 'operator@store.com',
      status: 'ACTIVE',
      role: 'MERCHANT_USER',
      createdAt: '2026-09-10T12:05:00Z',
    });

    expect(page.users().length).toBe(2);
    expect(page.form.controls.email.value).toBe('');
    expect(page.form.controls.password.value).toBe('');
    expect(page.form.controls.role.value).toBe('MERCHANT_USER');
    expect(page.success()).toContain('operator@store.com');
  });

  it('shows the backend error when the email is already registered', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/users').flush([]);
    const page = fixture.componentInstance;
    page.form.setValue({
      email: 'existing@store.com',
      password: 'OperatorPassword123!',
      role: 'MERCHANT_ADMIN',
    });
    page.createUser();
    http
      .expectOne('/api/merchant/users')
      .flush({ message: 'Email is already registered' }, { status: 409, statusText: 'Conflict' });

    expect(page.error()).toContain('Email is already registered');
    expect(page.form.controls.password.value).toBe('OperatorPassword123!');
  });
});
