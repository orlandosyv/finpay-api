export interface WebhookEndpointResponse {
  id: number;
  url: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookEndpointCreatedResponse extends WebhookEndpointResponse {
  signingSecret: string;
}

export interface CreateWebhookEndpointRequest {
  url: string;
}

export type OutboxEventStatus = 'PENDING' | 'PROCESSED' | 'FAILED';
export type WebhookEventType =
  'PAYMENT_CREATED' | 'PAYMENT_APPROVED' | 'PAYMENT_DECLINED' | 'PAYMENT_REFUNDED';
export type WebhookDeliveryStatus = 'SUCCEEDED' | 'FAILED';

export interface WebhookDashboardFilters {
  status?: OutboxEventStatus;
  eventType?: WebhookEventType;
  paymentId?: number;
  from?: string;
  to?: string;
  page: number;
  size: number;
}

export interface WebhookEventSummaryResponse {
  eventId: string;
  paymentId: number;
  eventType: string;
  status: OutboxEventStatus;
  failedAttempts: number;
  createdAt: string;
  nextAttemptAt: string | null;
  processedAt: string | null;
}

export interface WebhookEventDetailResponse extends WebhookEventSummaryResponse {
  payload: unknown;
}

export interface PagedWebhookEventResponse {
  content: WebhookEventSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WebhookDashboardSummaryResponse {
  totalEvents: number;
  processedEvents: number;
  pendingEvents: number;
  failedEvents: number;
  processedRate: number;
  totalDeliveries: number;
  successfulDeliveries: number;
  failedDeliveries: number;
  deliverySuccessRate: number;
  from: string | null;
  to: string | null;
}

export interface WebhookDeliveryResponse {
  deliveryId: string;
  eventId: string;
  endpointId: number;
  endpointUrl: string;
  attemptNumber: number;
  status: WebhookDeliveryStatus;
  responseStatus: number | null;
  errorMessage: string | null;
  attemptedAt: string;
}
