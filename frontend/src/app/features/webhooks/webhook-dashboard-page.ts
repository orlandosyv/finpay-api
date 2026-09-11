import { DatePipe, DecimalPipe, JsonPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { WebhookApiService } from '../../core/api/webhook-api.service';
import {
  OutboxEventStatus,
  PagedWebhookEventResponse,
  WebhookDashboardFilters,
  WebhookDashboardSummaryResponse,
  WebhookDeliveryResponse,
  WebhookEventDetailResponse,
  WebhookEventType,
} from '../../core/models/webhook.models';
import { requestError } from '../../shared/request-error';

@Component({
  selector: 'app-webhook-dashboard-page',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, DecimalPipe, JsonPipe],
  templateUrl: './webhook-dashboard-page.html',
  styleUrls: ['../../shared/operation-page.scss', './webhooks.scss'],
})
export class WebhookDashboardPage implements OnInit {
  private readonly api = inject(WebhookApiService);
  private readonly route = inject(ActivatedRoute);

  readonly mode = this.route.snapshot.data['mode'] as 'list' | 'detail';
  readonly summary = signal<WebhookDashboardSummaryResponse | null>(null);
  readonly page = signal<PagedWebhookEventResponse | null>(null);
  readonly event = signal<WebhookEventDetailResponse | null>(null);
  readonly deliveries = signal<WebhookDeliveryResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly filterError = signal('');
  readonly pageSize = 20;

  readonly filters = new FormGroup({
    status: new FormControl<OutboxEventStatus | ''>('', { nonNullable: true }),
    eventType: new FormControl<WebhookEventType | ''>('', { nonNullable: true }),
    paymentId: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^[1-9]\d*$/)],
    }),
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true }),
  });

  ngOnInit(): void {
    if (this.mode === 'detail') {
      this.loadDetail();
    } else {
      this.loadDashboard(0);
    }
  }

  applyFilters(): void {
    this.filters.markAllAsTouched();
    if (this.filters.invalid || !this.validateDateRange()) return;
    this.loadDashboard(0);
  }

  resetFilters(): void {
    this.filters.reset({ status: '', eventType: '', paymentId: '', from: '', to: '' });
    this.filterError.set('');
    this.loadDashboard(0);
  }

  previousPage(): void {
    const current = this.page();
    if (!current || current.page <= 0 || this.loading()) return;
    this.loadEvents(current.page - 1);
  }

  nextPage(): void {
    const current = this.page();
    if (!current || current.page + 1 >= current.totalPages || this.loading()) return;
    this.loadEvents(current.page + 1);
  }

  loadDashboard(pageNumber = this.page()?.page ?? 0): void {
    if (this.loading()) return;
    const filters = this.buildFilters(pageNumber);
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      summary: this.api.getSummary(filters.from, filters.to),
      page: this.api.getEvents(filters),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.summary.set(result.summary);
          this.page.set(result.page);
        },
        error: (error) => this.error.set(requestError(error)),
      });
  }

  loadDetail(): void {
    if (this.loading()) return;
    const eventId = this.route.snapshot.paramMap.get('eventId') ?? '';
    this.loading.set(true);
    this.error.set('');
    forkJoin({
      event: this.api.getEvent(eventId),
      deliveries: this.api.getDeliveries(eventId),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.event.set(result.event);
          this.deliveries.set(result.deliveries);
        },
        error: (error) => this.error.set(requestError(error)),
      });
  }

  responseLabel(delivery: WebhookDeliveryResponse): string {
    return delivery.responseStatus === null
      ? 'No HTTP response'
      : `HTTP ${delivery.responseStatus}`;
  }

  private loadEvents(pageNumber: number): void {
    this.loading.set(true);
    this.error.set('');
    this.api
      .getEvents(this.buildFilters(pageNumber))
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.page.set(page),
        error: (error) => this.error.set(requestError(error)),
      });
  }

  private buildFilters(page: number): WebhookDashboardFilters {
    const value = this.filters.getRawValue();
    return {
      status: value.status || undefined,
      eventType: value.eventType || undefined,
      paymentId: value.paymentId ? Number(value.paymentId) : undefined,
      from: this.toIso(value.from),
      to: this.toIso(value.to),
      page,
      size: this.pageSize,
    };
  }

  private validateDateRange(): boolean {
    const { from, to } = this.filters.getRawValue();
    if (from && to && new Date(from).getTime() > new Date(to).getTime()) {
      this.filterError.set('From must be before or equal to To.');
      return false;
    }
    this.filterError.set('');
    return true;
  }

  private toIso(value: string): string | undefined {
    return value ? new Date(value).toISOString() : undefined;
  }
}
