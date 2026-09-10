import { Component, inject, OnInit, signal } from '@angular/core';
import { HealthApiService } from '../../../core/api/health-api.service';

type ApiStatus = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-health-page',
  templateUrl: './health-page.html',
  styleUrl: './health-page.scss',
})
export class HealthPage implements OnInit {
  private readonly healthApi = inject(HealthApiService);

  protected readonly status = signal<ApiStatus>('checking');
  protected readonly message = signal('Checking API connection...');

  ngOnInit(): void {
    this.checkApi();
  }

  protected checkApi(): void {
    this.status.set('checking');
    this.message.set('Checking API connection...');

    this.healthApi.check().subscribe({
      next: (message) => {
        this.status.set('online');
        this.message.set(message);
      },
      error: () => {
        this.status.set('offline');
        this.message.set('The API is unavailable. Start FinPay on port 8080 and try again.');
      },
    });
  }
}
