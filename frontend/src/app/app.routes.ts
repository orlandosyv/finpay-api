import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page/login-page').then((component) => component.LoginPage),
    canActivate: [guestGuard],
    title: 'Sign in | FinPay Console',
  },
  {
    path: 'app',
    loadComponent: () =>
      import('./features/session/session-page/session-page').then(
        (component) => component.SessionPage,
      ),
    canActivate: [authGuard],
    title: 'Merchant session | FinPay Console',
  },
  {
    path: 'system/health',
    loadComponent: () =>
      import('./features/system/health-page/health-page').then((component) => component.HealthPage),
    title: 'API status | FinPay Console',
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'login',
  },
  {
    path: '**',
    redirectTo: 'login',
  },
];
