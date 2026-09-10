import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CurrentMerchantResponse } from '../models/merchant.models';

@Injectable({
  providedIn: 'root',
})
export class MerchantApiService {
  private readonly http = inject(HttpClient);

  getCurrentMerchant(): Observable<CurrentMerchantResponse> {
    return this.http.get<CurrentMerchantResponse>('/api/merchant/me');
  }
}
