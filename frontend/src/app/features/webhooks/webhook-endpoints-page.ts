import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { WebhookApiService } from '../../core/api/webhook-api.service';
import {
  WebhookEndpointCreatedResponse,
  WebhookEndpointResponse,
} from '../../core/models/webhook.models';
import { requestError } from '../../shared/request-error';

@Component({
  selector: 'app-webhook-endpoints-page',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './webhook-endpoints-page.html',
  styleUrls: ['../../shared/operation-page.scss', './webhooks.scss'],
})
export class WebhookEndpointsPage implements OnInit {
  private readonly api = inject(WebhookApiService);

  readonly endpoints = signal<WebhookEndpointResponse[]>([]);
  readonly createdEndpoint = signal<WebhookEndpointCreatedResponse | null>(null);
  readonly disablingId = signal<number | null>(null);
  readonly confirmationId = signal<number | null>(null);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal('');
  readonly copyStatus = signal('');

  readonly form = new FormGroup({
    url: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.maxLength(2048),
        Validators.pattern(/^https?:\/\/[^\s]+$/i),
      ],
    }),
  });

  ngOnInit(): void {
    this.loadEndpoints();
  }

  loadEndpoints(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.api
      .getEndpoints()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (endpoints) => this.endpoints.set(endpoints),
        error: (error) => this.error.set(requestError(error)),
      });
  }

  createEndpoint(): void {
    if (this.creating()) return;
    this.form.controls.url.setValue(this.form.controls.url.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.creating.set(true);
    this.error.set('');
    this.copyStatus.set('');
    this.createdEndpoint.set(null);
    this.api
      .createEndpoint(this.form.getRawValue())
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: (created) => {
          const { signingSecret: _secret, ...endpoint } = created;
          this.endpoints.update((endpoints) => [...endpoints, endpoint]);
          this.createdEndpoint.set(created);
          this.form.reset({ url: '' });
        },
        error: (error) => this.error.set(requestError(error)),
      });
  }

  copySecret(): void {
    const secret = this.createdEndpoint()?.signingSecret;
    if (!secret) return;
    navigator.clipboard.writeText(secret).then(
      () => this.copyStatus.set('Signing secret copied.'),
      () => this.copyStatus.set('Copy failed. Select and copy the secret manually.'),
    );
  }

  acknowledgeSecret(): void {
    this.createdEndpoint.set(null);
    this.copyStatus.set('');
  }

  requestDisable(id: number): void {
    this.confirmationId.set(id);
    this.error.set('');
  }

  cancelDisable(): void {
    this.confirmationId.set(null);
  }

  disableEndpoint(id: number): void {
    if (this.disablingId() !== null) return;
    this.disablingId.set(id);
    this.error.set('');
    this.api
      .disableEndpoint(id)
      .pipe(finalize(() => this.disablingId.set(null)))
      .subscribe({
        next: () => {
          this.endpoints.update((endpoints) => endpoints.filter((endpoint) => endpoint.id !== id));
          this.confirmationId.set(null);
        },
        error: (error) => {
          this.confirmationId.set(null);
          this.error.set(requestError(error));
        },
      });
  }
}
