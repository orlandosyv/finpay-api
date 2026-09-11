import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { WebhookEndpointsPage } from './webhook-endpoints-page';

describe('WebhookEndpointsPage', () => {
  let http: HttpTestingController;
  const endpoint = {
    id: 4,
    url: 'https://merchant.example.com/finpay',
    active: true,
    createdAt: '2026-09-10T12:00:00Z',
    updatedAt: '2026-09-10T12:00:00Z',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [WebhookEndpointsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function createPage() {
    const fixture = TestBed.createComponent(WebhookEndpointsPage);
    fixture.detectChanges();
    return fixture;
  }

  it('lists active webhook endpoints without exposing a secret', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/webhooks').flush([endpoint]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain(endpoint.url);
    expect(fixture.nativeElement.textContent).not.toContain('signingSecret');
  });

  it('rejects an invalid destination before calling the API', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/webhooks').flush([]);
    const page = fixture.componentInstance;
    page.form.setValue({ url: 'ftp://merchant.example.com/hook' });
    page.createEndpoint();

    http.expectNone((request) => request.method === 'POST');
    expect(page.form.invalid).toBe(true);
  });

  it('registers an endpoint and keeps its one-time secret visible', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/webhooks').flush([]);
    const page = fixture.componentInstance;
    page.form.setValue({ url: ' https://merchant.example.com/finpay ' });
    page.createEndpoint();

    const request = http.expectOne('/api/merchant/webhooks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ url: endpoint.url });
    request.flush({ ...endpoint, signingSecret: 'whsec_one_time_secret' });
    fixture.detectChanges();

    expect(page.endpoints()).toEqual([endpoint]);
    expect(page.createdEndpoint()?.signingSecret).toBe('whsec_one_time_secret');
    expect(fixture.nativeElement.textContent).toContain('whsec_one_time_secret');
  });

  it('disables an endpoint after explicit confirmation', () => {
    const fixture = createPage();
    http.expectOne('/api/merchant/webhooks').flush([endpoint]);
    const page = fixture.componentInstance;
    page.requestDisable(endpoint.id);
    http.expectNone(`/api/merchant/webhooks/${endpoint.id}`);
    page.disableEndpoint(endpoint.id);

    const request = http.expectOne(`/api/merchant/webhooks/${endpoint.id}`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    expect(page.endpoints()).toEqual([]);
  });
});
