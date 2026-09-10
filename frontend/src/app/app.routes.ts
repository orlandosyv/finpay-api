import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login-page/login-page').then((component) => component.LoginPage),
    title: 'Sign in | FinPay Console',
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
