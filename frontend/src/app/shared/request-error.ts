import { HttpErrorResponse } from '@angular/common/http';
export function requestError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) return 'Cannot reach FinPay. Check your connection and try again.';
    const body = error.error;
    if (body && typeof body === 'object' && typeof body.message === 'string') {
      const fields =
        body.fieldErrors && typeof body.fieldErrors === 'object'
          ? Object.values(body.fieldErrors)
              .filter((v) => typeof v === 'string')
              .join(' ')
          : '';
      return body.message + (fields ? ': ' + fields : '');
    }
    return 'Request failed (HTTP ' + error.status + '). Please try again.';
  }
  return 'Request failed. Please try again.';
}
