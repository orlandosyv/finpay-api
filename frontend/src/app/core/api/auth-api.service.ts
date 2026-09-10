import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  LoginRequest,
  LoginResponse,
  LogoutRequest,
  RefreshTokenRequest,
} from '../models/auth.models';

@Injectable({
  providedIn: 'root',
})
export class AuthApiService {
  private readonly http = inject(HttpClient);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', request);
  }

  refresh(request: RefreshTokenRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/refresh', request);
  }

  logout(request: LogoutRequest): Observable<void> {
    return this.http.post<void>('/api/auth/logout', request);
  }
}
