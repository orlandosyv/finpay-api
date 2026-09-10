import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { MerchantApiService } from '../../../core/api/merchant-api.service';
import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { ApiError } from '../../../core/models/api-error';
import { CurrentMerchantResponse } from '../../../core/models/merchant.models';

@Component({
  selector: 'app-session-page',
  imports: [DatePipe],
  templateUrl: './session-page.html',
  styleUrl: './session-page.scss',
})
export class SessionPage implements OnInit {
  private readonly merchantApi = inject(MerchantApiService);
  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  protected readonly session = this.authSession.session;
  protected readonly merchant = signal<CurrentMerchantResponse | null>(null);
  protected readonly loading = signal(true);
  protected readonly loggingOut = signal(false);
  protected readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.loadMerchant();
  }

  protected loadMerchant(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.merchantApi
      .getCurrentMerchant()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (merchant) => this.merchant.set(merchant),
        error: (error: HttpErrorResponse) => {
          const apiError = error.error as Partial<ApiError> | null;
          this.errorMessage.set(
            apiError?.message ?? 'The protected merchant context could not be loaded.',
          );
        },
      });
  }

  protected logout(): void {
    this.loggingOut.set(true);
    this.errorMessage.set(null);

    this.authSession
      .logout()
      .pipe(finalize(() => this.loggingOut.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl('/login'),
        error: () => {
          void this.router.navigateByUrl('/login');
        },
      });
  }
}
