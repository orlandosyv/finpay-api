import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { PaymentApiService, Payment, CreatePayment } from '../../core/api/payment-api.service';
import { AuthSessionService } from '../../core/auth/auth-session.service';
import { requestError } from '../../shared/request-error';
@Component({
  selector: 'app-payments-page',
  imports: [RouterLink, ReactiveFormsModule, DatePipe, DecimalPipe],
  templateUrl: './payments-page.html',
  styleUrls: ['../../shared/operation-page.scss'],
})
export class PaymentsPage implements OnInit {
  private readonly api = inject(PaymentApiService);
  private readonly route = inject(ActivatedRoute);
  readonly admin = inject(AuthSessionService).isAdmin;
  readonly mode = this.route.snapshot.data['mode'] as 'list' | 'new' | 'detail';
  readonly payments = signal<Payment[]>([]);
  readonly payment = signal<Payment | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly replayed = signal(false);
  readonly attempted = signal(false);
  readonly key = signal(crypto.randomUUID());
  readonly confirmation = signal<'approve' | 'decline' | 'refund' | null>(null);
  private submittedBody: CreatePayment | null = null;
  readonly form = new FormGroup({
    amount: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.pattern(/^(?:0|[1-9]\d{0,16})(?:\.\d{1,2})?$/),
        (c) => (Number(c.value) > 0 ? null : { positive: true }),
      ],
    }),
    currency: new FormControl('PEN', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/^[A-Z]{3}$/)],
    }),
  });
  ngOnInit() {
    if (this.mode !== 'new') this.load();
  }
  load() {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    if (this.mode === 'list') {
      this.api
        .list()
        .pipe(finalize(() => this.busy.set(false)))
        .subscribe({
          next: (result) => this.payments.set(result),
          error: (e) => this.error.set(requestError(e)),
        });
    } else {
      this.api
        .get(this.route.snapshot.paramMap.get('id') ?? '')
        .pipe(finalize(() => this.busy.set(false)))
        .subscribe({
          next: (result) => this.payment.set(result),
          error: (e) => this.error.set(requestError(e)),
        });
    }
  }
  create() {
    if (this.busy()) return;
    if (!this.submittedBody) {
      this.form.markAllAsTouched();
      if (this.form.invalid) return;
      this.submittedBody = this.form.getRawValue();
      this.form.disable();
      this.attempted.set(true);
    }
    this.busy.set(true);
    this.error.set('');
    this.api
      .create(this.submittedBody, this.key())
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (response) => {
          this.payment.set(response.body);
          this.replayed.set(response.headers.get('Idempotency-Replayed') === 'true');
        },
        error: (e) => this.error.set(requestError(e)),
      });
  }
  newOperation() {
    if (this.busy()) return;
    if (
      this.attempted() &&
      !this.payment() &&
      !globalThis.confirm(
        'The previous request may have reached FinPay. Check the payment list before starting a new operation. Continue with a new key?',
      )
    )
      return;
    this.submittedBody = null;
    this.attempted.set(false);
    this.key.set(crypto.randomUUID());
    this.payment.set(null);
    this.error.set('');
    this.replayed.set(false);
    this.form.enable();
    this.form.reset({ amount: '', currency: 'PEN' });
  }
  transition() {
    const payment = this.payment(),
      action = this.confirmation();
    if (!payment || !action || this.busy() || !this.admin()) return;
    this.busy.set(true);
    this.error.set('');
    this.api
      .transition(payment.id, action)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (p) => {
          this.payment.set(p);
          this.confirmation.set(null);
        },
        error: (e) => {
          this.confirmation.set(null);
          this.error.set(requestError(e));
        },
      });
  }
}
