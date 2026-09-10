import { computed, inject, Injectable, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, shareReplay, tap, throwError } from 'rxjs';
import { AuthApiService } from '../api/auth-api.service';
import { AuthSession, LoginRequest, LoginResponse } from '../models/auth.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({
  providedIn: 'root',
})
export class AuthSessionService {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly sessionState = signal<AuthSession | null>(this.tokenStorage.read());
  private refreshRequest: Observable<AuthSession> | null = null;

  readonly session = this.sessionState.asReadonly();
  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly isAdmin = computed(() => this.sessionState()?.role === 'MERCHANT_ADMIN');

  login(request: LoginRequest): Observable<AuthSession> {
    return this.authApi
      .login({
        email: request.email.trim(),
        password: request.password,
      })
      .pipe(map((response) => this.saveSession(response)));
  }

  refreshTokens(): Observable<AuthSession> {
    if (this.refreshRequest) {
      return this.refreshRequest;
    }

    const refreshToken = this.tokenStorage.getRefreshToken();

    if (!refreshToken) {
      this.clear();
      return throwError(() => new Error('No refresh token is available.'));
    }

    this.refreshRequest = this.authApi.refresh({ refreshToken }).pipe(
      map((response) => this.saveSession(response)),
      catchError((error: unknown) => {
        this.clear();
        return throwError(() => error);
      }),
      finalize(() => {
        this.refreshRequest = null;
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );

    return this.refreshRequest;
  }

  logout(): Observable<void> {
    const refreshToken = this.tokenStorage.getRefreshToken();

    if (!refreshToken) {
      this.clear();
      return of(undefined);
    }

    return this.authApi.logout({ refreshToken }).pipe(
      tap(() => this.clear()),
      catchError((error: unknown) => {
        this.clear();
        return throwError(() => error);
      }),
    );
  }

  hasSession(): boolean {
    const session = this.sessionState();

    if (!session || session.refreshTokenExpiresAt <= Date.now()) {
      this.clear();
      return false;
    }

    return true;
  }

  clear(): void {
    this.tokenStorage.clear();
    this.sessionState.set(null);
  }

  private saveSession(response: LoginResponse): AuthSession {
    const session = this.tokenStorage.save(response);
    this.sessionState.set(session);
    return session;
  }
}
