import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateWebhookEndpointRequest,
  PagedWebhookEventResponse,
  WebhookDashboardFilters,
  WebhookDashboardSummaryResponse,
  WebhookDeliveryResponse,
  WebhookEndpointCreatedResponse,
  WebhookEndpointResponse,
  WebhookEventDetailResponse,
} from '../models/webhook.models';

@Injectable({ providedIn: 'root' })
export class WebhookApiService {
  private readonly http = inject(HttpClient);

  getEndpoints(): Observable<WebhookEndpointResponse[]> {
    return this.http.get<WebhookEndpointResponse[]>('/api/merchant/webhooks');
  }

  createEndpoint(
    request: CreateWebhookEndpointRequest,
  ): Observable<WebhookEndpointCreatedResponse> {
    return this.http.post<WebhookEndpointCreatedResponse>('/api/merchant/webhooks', request);
  }

  disableEndpoint(id: number): Observable<void> {
    return this.http.delete<void>(`/api/merchant/webhooks/${id}`);
  }

  getSummary(from?: string, to?: string): Observable<WebhookDashboardSummaryResponse> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<WebhookDashboardSummaryResponse>('/api/merchant/webhook-events/summary', {
      params,
    });
  }

  getEvents(filters: WebhookDashboardFilters): Observable<PagedWebhookEventResponse> {
    let params = new HttpParams().set('page', filters.page).set('size', filters.size);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.eventType) params = params.set('eventType', filters.eventType);
    if (filters.paymentId !== undefined) {
      params = params.set('paymentId', filters.paymentId);
    }
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);
    return this.http.get<PagedWebhookEventResponse>('/api/merchant/webhook-events', { params });
  }

  getEvent(eventId: string): Observable<WebhookEventDetailResponse> {
    return this.http.get<WebhookEventDetailResponse>(`/api/merchant/webhook-events/${eventId}`);
  }

  getDeliveries(eventId: string): Observable<WebhookDeliveryResponse[]> {
    return this.http.get<WebhookDeliveryResponse[]>(
      `/api/merchant/webhook-events/${eventId}/deliveries`,
    );
  }
}
