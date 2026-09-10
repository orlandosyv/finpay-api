import { Injectable } from '@angular/core';
import { AuthSession, LoginResponse } from '../models/auth.models';

const SESSION_STORAGE_KEY = 'finpay.auth.session';

@Injectable({
  providedIn: 'root',
})
export class TokenStorageService {
  read(): AuthSession | null {
    const storage = this.getStorage();

    if (!storage) {
      return null;
    }

    const serializedSession = storage.getItem(SESSION_STORAGE_KEY);

    if (!serializedSession) {
      return null;
    }

    try {
      const session: unknown = JSON.parse(serializedSession);

      if (this.isAuthSession(session)) {
        return session;
      }
    } catch {
      // Invalid browser state is cleared below.
    }

    storage.removeItem(SESSION_STORAGE_KEY);
    return null;
  }

  save(response: LoginResponse): AuthSession {
    const now = Date.now();
    const session: AuthSession = {
      ...response,
      accessTokenExpiresAt: now + response.expiresIn * 1000,
      refreshTokenExpiresAt: now + response.refreshExpiresIn * 1000,
    };

    this.getStorage()?.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    return session;
  }

  clear(): void {
    this.getStorage()?.removeItem(SESSION_STORAGE_KEY);
  }

  getAccessToken(): string | null {
    return this.read()?.accessToken ?? null;
  }

  getRefreshToken(): string | null {
    return this.read()?.refreshToken ?? null;
  }

  private getStorage(): Storage | null {
    try {
      return globalThis.sessionStorage;
    } catch {
      return null;
    }
  }

  private isAuthSession(value: unknown): value is AuthSession {
    if (typeof value !== 'object' || value === null) {
      return false;
    }

    const candidate = value as Partial<AuthSession>;

    return (
      typeof candidate.accessToken === 'string' &&
      typeof candidate.refreshToken === 'string' &&
      typeof candidate.tokenType === 'string' &&
      typeof candidate.accessTokenExpiresAt === 'number' &&
      typeof candidate.refreshTokenExpiresAt === 'number' &&
      typeof candidate.email === 'string' &&
      typeof candidate.merchantName === 'string' &&
      typeof candidate.role === 'string'
    );
  }
}
