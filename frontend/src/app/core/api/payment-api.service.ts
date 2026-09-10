import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
export interface Payment {
  id: number;
  amount: number;
  currency: string;
  status: 'PENDING' | 'APPROVED' | 'DECLINED' | 'REFUNDED';
  createdAt: string;
  updatedAt: string;
}
export interface CreatePayment {
  amount: string;
  currency: string;
}
@Injectable({ providedIn: 'root' })
export class PaymentApiService {
  private readonly http = inject(HttpClient);
  list() {
    return this.http.get<Payment[]>('/api/payments');
  }
  get(id: string) {
    return this.http.get<Payment>('/api/payments/' + encodeURIComponent(id));
  }
  create(body: CreatePayment, key: string) {
    return this.http.post<Payment>('/api/payments', body, {
      headers: { 'Idempotency-Key': key },
      observe: 'response',
    });
  }
  transition(id: number, action: 'approve' | 'decline' | 'refund') {
    return this.http.patch<Payment>('/api/payments/' + id + '/' + action, {});
  }
}
