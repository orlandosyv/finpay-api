import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { WebhookDashboardPage } from './webhook-dashboard-page';

describe('WebhookDashboardPage', () => {
  let http: HttpTestingController;
  const eventId = 'd5eb98f7-0cec-47b5-91f0-adc9559e953a';
  const summary = {
    totalEvents: 2,
    processedEvents: 1,
    pendingEvents: 1,
    failedEvents: 0,
    processedRate: 50,
    totalDeliveries: 2,
    successfulDeliveries: 1,
    failedDeliveries: 1,
    deliverySuccessRate: 50,
    from: null,
    to: null,
  };
  const event = {
    eventId,
    paymentId: 101,
    eventType: 'payment.created',
    status: 'PENDING',
    failedAttempts: 1,
    createdAt: '2026-09-10T12:00:00Z',
    nextAttemptAt: '2026-09-10T12:05:00Z',
    processedAt: null,
  };

  afterEach(() => http.verify());

  function setup(mode: 'list' | 'detail') {
    TestBed.configureTestingModule({
      imports: [WebhookDashboardPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { data: { mode }, paramMap: convertToParamMap({ eventId }) },
          },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(WebhookDashboardPage);
    fixture.detectChanges();
    return fixture;
  }

  function flushList(content = [event], page = 0, totalPages = 1) {
    http.expectOne((request) => request.url.endsWith('/summary')).flush(summary);
    http
      .expectOne((request) => request.url === '/api/merchant/webhook-events')
      .flush({
        content,
        page,
        size: 20,
        totalElements: content.length,
        totalPages,
      });
  }

  it('loads summary metrics and the first event page', () => {
    const fixture = setup('list');
    flushList();
    fixture.detectChanges();

    expect(fixture.componentInstance.summary()?.deliverySuccessRate).toBe(50);
    expect(fixture.nativeElement.textContent).toContain('payment.created');
    expect(fixture.nativeElement.textContent).toContain('1 failed deliveries');
  });

  it('sends status, type, payment and date filters using backend enum values', () => {
    const fixture = setup('list');
    flushList();
    const page = fixture.componentInstance;
    page.filters.setValue({
      status: 'FAILED',
      eventType: 'PAYMENT_REFUNDED',
      paymentId: '102',
      from: '2026-09-10T10:00',
      to: '2026-09-10T12:00',
    });
    page.applyFilters();

    const summaryRequest = http.expectOne((request) => request.url.endsWith('/summary'));
    expect(summaryRequest.request.params.has('from')).toBe(true);
    expect(summaryRequest.request.params.has('to')).toBe(true);
    summaryRequest.flush(summary);
    const eventsRequest = http.expectOne(
      (request) =>
        request.url === '/api/merchant/webhook-events' &&
        request.params.get('status') === 'FAILED' &&
        request.params.get('eventType') === 'PAYMENT_REFUNDED' &&
        request.params.get('paymentId') === '102',
    );
    eventsRequest.flush({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
  });

  it('rejects an inverted date range without sending requests', () => {
    const fixture = setup('list');
    flushList();
    const page = fixture.componentInstance;
    page.filters.patchValue({ from: '2026-09-10T12:00', to: '2026-09-10T10:00' });
    page.applyFilters();

    http.expectNone((request) => request.url.includes('/api/merchant/webhook-events'));
    expect(page.filterError()).toContain('From must be before');
  });

  it('loads event payload and its complete delivery history', () => {
    const fixture = setup('detail');
    http.expectOne(`/api/merchant/webhook-events/${eventId}`).flush({
      ...event,
      payload: { type: 'payment.created', data: { payment: { id: 101 } } },
    });
    http.expectOne(`/api/merchant/webhook-events/${eventId}/deliveries`).flush([
      {
        deliveryId: '618c06ee-b3ae-445f-8b0f-63b67abf3a55',
        eventId,
        endpointId: 4,
        endpointUrl: 'https://merchant.example.com/finpay',
        attemptNumber: 1,
        status: 'FAILED',
        responseStatus: 500,
        errorMessage: 'Webhook returned HTTP 500',
        attemptedAt: '2026-09-10T12:01:00Z',
      },
    ]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Immutable payload');
    expect(fixture.nativeElement.textContent).toContain('HTTP 500');
    expect(fixture.nativeElement.textContent).toContain('Webhook returned HTTP 500');
    expect(fixture.nativeElement.textContent).toContain('Next automatic retry');
  });
});
