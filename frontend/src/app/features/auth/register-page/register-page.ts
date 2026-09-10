import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { requestError } from '../../../shared/request-error';
@Component({
  selector: 'app-register-page',
  imports: [ReactiveFormsModule, RouterLink],
  styleUrls: ['../../../shared/operation-page.scss'],
  styles: [':host { max-width: 620px; margin: 40px auto; padding: 0 20px; }'],
  templateUrl: './register-page.html',
})
export class RegisterPage {
  private readonly http = inject(HttpClient);
  readonly busy = signal(false);
  readonly created = signal(false);
  readonly error = signal('');
  readonly form = new FormGroup({
    merchantName: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        Validators.minLength(12),
        Validators.maxLength(72),
        Validators.pattern(/^(?=\S+$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).*$/),
      ],
    }),
  });
  submit() {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.busy.set(true);
    this.error.set('');
    const body = this.form.getRawValue();
    this.http
      .post('/api/auth/register', {
        ...body,
        merchantName: body.merchantName.trim(),
        email: body.email.trim(),
      })
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.form.reset();
          this.created.set(true);
        },
        error: (e) => this.error.set(requestError(e)),
      });
  }
}
