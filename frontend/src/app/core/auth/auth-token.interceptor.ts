import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { isApiRequest, isPublicAuthRequest } from './auth-request';
import { TokenStorageService } from './token-storage.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url) || isPublicAuthRequest(request.url)) {
    return next(request);
  }

  const session = inject(TokenStorageService).read();

  if (!session) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: `${session.tokenType} ${session.accessToken}`,
      },
    }),
  );
};
