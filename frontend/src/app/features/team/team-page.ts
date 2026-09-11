import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { MerchantApiService } from '../../core/api/merchant-api.service';
import { MerchantRole } from '../../core/models/auth.models';
import { MerchantUserResponse } from '../../core/models/merchant.models';
import { requestError } from '../../shared/request-error';

@Component({
  selector: 'app-team-page',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './team-page.html',
  styleUrls: ['../../shared/operation-page.scss', './team-page.scss'],
})
export class TeamPage implements OnInit {
  private readonly api = inject(MerchantApiService);

  readonly users = signal<MerchantUserResponse[]>([]);
  readonly administratorCount = computed(
    () => this.users().filter((user) => user.role === 'MERCHANT_ADMIN').length,
  );
  readonly operatorCount = computed(
    () => this.users().filter((user) => user.role === 'MERCHANT_USER').length,
  );
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal('');
  readonly success = signal('');

  readonly form = new FormGroup({
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
    role: new FormControl<MerchantRole>('MERCHANT_USER', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.error.set('');
    this.api
      .getUsers()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (users) => this.users.set(users),
        error: (error) => this.error.set(requestError(error)),
      });
  }

  createUser(): void {
    if (this.creating()) return;
    this.form.controls.email.setValue(this.form.controls.email.value.trim().toLowerCase());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const value = this.form.getRawValue();
    this.creating.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .createUser({
        email: value.email,
        password: value.password,
        role: value.role,
      })
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: (created) => {
          this.users.update((users) => [...users, created]);
          this.form.reset({ email: '', password: '', role: 'MERCHANT_USER' });
          this.success.set(`${created.email} was added as ${created.role}.`);
        },
        error: (error) => this.error.set(requestError(error)),
      });
  }
}
