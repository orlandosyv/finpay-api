import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class HealthApiService {
  private readonly http = inject(HttpClient);

  check(): Observable<string> {
    return this.http.get('/api/health', { responseType: 'text' });
  }
}
