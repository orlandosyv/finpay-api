import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { PaymentsPage } from './payments-page';
import { AuthSessionService } from '../../core/auth/auth-session.service';

describe('Payments workflow', () => {
  let http: HttpTestingController;
  const isAdmin = signal(true);
  const payment = {
    id: 7,
    amount: 150,
    currency: 'PEN',
    status: 'PENDING',
    createdAt: '2026-09-10T12:00:00Z',
    updatedAt: '2026-09-10T12:00:00Z',
  };
  function setup(mode = 'new') {
    TestBed.configureTestingModule({
      imports: [PaymentsPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthSessionService, useValue: { isAdmin } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { mode }, paramMap: convertToParamMap({ id: '7' }) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.createComponent(PaymentsPage);
  }
  beforeEach(() => isAdmin.set(true));
  afterEach(() => http.verify());
  it('rejects invalid amounts without sending a request', () => {
    const c = setup().componentInstance;
    for (const amount of ['0', '-1', '1.234', 'abc']) {
      c.form.setValue({ amount, currency: 'PEN' });
      c.create();
      expect(c.form.invalid).toBe(true);
    }
    http.expectNone('/api/payments');
  });
  it('reuses the same key after network failure and replay; new operation gets a new key', () => {
    const c = setup().componentInstance;
    c.form.setValue({ amount: '150.00', currency: 'PEN' });
    c.create();
    const key = c.key();
    const first = http.expectOne('/api/payments');
    expect(first.request.body.amount).toBe('150.00');
    expect(first.request.headers.get('Idempotency-Key')).toBe(key);
    first.error(new ProgressEvent('error'));
    c.create();
    const retry = http.expectOne('/api/payments');
    expect(retry.request.headers.get('Idempotency-Key')).toBe(key);
    expect(retry.request.body).toEqual({ amount: '150.00', currency: 'PEN' });
    retry.flush(payment, {
      status: 201,
      statusText: 'Created',
      headers: { 'Idempotency-Replayed': 'true' },
    });
    expect(c.replayed()).toBe(true);
    expect(c.payment()?.id).toBe(7);
    c.newOperation();
    expect(c.key()).not.toBe(key);
    expect(c.form.enabled).toBe(true);
  });
  it('loads a tenant payment list and shows empty state', () => {
    const fixture = setup('list');
    fixture.detectChanges();
    http.expectOne('/api/payments').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No payments yet');
  });
  it('approves then refunds and replaces the displayed status', () => {
    const fixture = setup('detail'),
      c = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/payments/7').flush(payment);
    c.confirmation.set('approve');
    c.transition();
    const approve = http.expectOne('/api/payments/7/approve');
    expect(approve.request.method).toBe('PATCH');
    approve.flush({ ...payment, status: 'APPROVED' });
    expect(c.payment()?.status).toBe('APPROVED');
    c.confirmation.set('refund');
    c.transition();
    http.expectOne('/api/payments/7/refund').flush({ ...payment, status: 'REFUNDED' });
    expect(c.payment()?.status).toBe('REFUNDED');
  });
  it('declines and displays backend conflicts', () => {
    const fixture = setup('detail'),
      c = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/payments/7').flush(payment);
    c.confirmation.set('decline');
    c.transition();
    http
      .expectOne('/api/payments/7/decline')
      .flush({ message: 'Invalid status transition' }, { status: 409, statusText: 'Conflict' });
    expect(c.error()).toContain('Invalid status transition');
    c.confirmation.set('decline');
    c.transition();
    http.expectOne('/api/payments/7/decline').flush({ ...payment, status: 'DECLINED' });
    expect(c.payment()?.status).toBe('DECLINED');
  });
  it('does not allow a merchant user to perform transitions', () => {
    isAdmin.set(false);
    const fixture = setup('detail'),
      c = fixture.componentInstance;
    fixture.detectChanges();
    http.expectOne('/api/payments/7').flush(payment);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Only a merchant administrator');
    c.confirmation.set('approve');
    c.transition();
    http.expectNone('/api/payments/7/approve');
  });
});
