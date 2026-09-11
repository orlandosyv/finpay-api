import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateMerchantUserRequest,
  CurrentMerchantResponse,
  MerchantUserResponse,
} from '../models/merchant.models';

@Injectable({
  providedIn: 'root',
})
export class MerchantApiService {
  private readonly http = inject(HttpClient);

  getCurrentMerchant(): Observable<CurrentMerchantResponse> {
    return this.http.get<CurrentMerchantResponse>('/api/merchant/me');
  }

  getUsers(): Observable<MerchantUserResponse[]> {
    return this.http.get<MerchantUserResponse[]>('/api/merchant/users');
  }

  createUser(request: CreateMerchantUserRequest): Observable<MerchantUserResponse> {
    return this.http.post<MerchantUserResponse>('/api/merchant/users', request);
  }
}
