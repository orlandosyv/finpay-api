import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../../../core/api/auth-api.service';
import { ApiError } from '../../../core/models/api-error';

@Component({
  selector: 'app-login-page',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-page.html',
  styleUrl: './login-page.scss',
})
export class LoginPage {
  private readonly authApi = inject(AuthApiService);

  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(72)],
    }),
  });

  protected readonly submitting = signal(false);
  protected readonly errorMessage = signal<string | null>(null);
  protected readonly fieldErrors = signal<Record<string, string>>({});
  protected readonly authenticatedMerchant = signal<string | null>(null);

  protected submit(): void {
    this.errorMessage.set(null);
    this.fieldErrors.set({});
    this.authenticatedMerchant.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    this.authApi
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (response) => {
          this.authenticatedMerchant.set(
            `${response.merchantName} · ${response.email} · ${response.role}`,
          );
        },
        error: (error: HttpErrorResponse) => {
          const apiError = this.readApiError(error);
          this.fieldErrors.set(apiError?.fieldErrors ?? {});
          this.errorMessage.set(
            apiError?.message ?? 'FinPay could not complete the login request. Try again.',
          );
        },
      });
  }

  private readApiError(error: HttpErrorResponse): ApiError | null {
    if (
      typeof error.error === 'object' &&
      error.error !== null &&
      typeof error.error.message === 'string'
    ) {
      return error.error as ApiError;
    }

    return null;
  }
}
