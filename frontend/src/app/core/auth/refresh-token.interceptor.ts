import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthSessionService } from './auth-session.service';
import { isApiRequest, isPublicAuthRequest } from './auth-request';
import { AuthSession } from '../models/auth.models';

function retryWithSession(request: Parameters<HttpInterceptorFn>[0], session: AuthSession) {
  const isLogoutRequest = request.url.split('?')[0].endsWith('/api/auth/logout');

  return request.clone({
    body: isLogoutRequest
      ? {
          ...(request.body as Record<string, unknown>),
          refreshToken: session.refreshToken,
        }
      : request.body,
    setHeaders: {
      Authorization: `${session.tokenType} ${session.accessToken}`,
    },
  });
}

export const refreshTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const authSession = inject(AuthSessionService);
  const router = inject(Router);

  return next(request).pipe(
    catchError((error: unknown) => {
      const shouldRefresh =
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        isApiRequest(request.url) &&
        !isPublicAuthRequest(request.url);

      if (!shouldRefresh) {
        return throwError(() => error);
      }

      return authSession.refreshTokens().pipe(
        switchMap((session) => next(retryWithSession(request, session))),
        catchError(() => {
          authSession.clear();
          void router.navigateByUrl('/login');
          return throwError(() => error);
        }),
      );
    }),
  );
};
