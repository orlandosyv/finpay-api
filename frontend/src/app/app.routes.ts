import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';
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
      import('./layout/app-shell/app-shell').then((component) => component.AppShell),
    canActivate: [authGuard],
    children: [
      {
        path: 'overview',
        loadComponent: () =>
          import('./features/session/session-page/session-page').then(
            (component) => component.SessionPage,
          ),
        title: 'Overview | FinPay Console',
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./shared/feature-placeholder/feature-placeholder').then(
            (component) => component.FeaturePlaceholder,
          ),
        data: {
          heading: 'Payments',
          description: 'Create, inspect and transition tenant-scoped payments from this section.',
          nextCapability: 'Payment management is the next implementation block.',
        },
        title: 'Payments | FinPay Console',
      },
      {
        path: 'team',
        loadComponent: () =>
          import('./shared/feature-placeholder/feature-placeholder').then(
            (component) => component.FeaturePlaceholder,
          ),
        canActivate: [adminGuard],
        data: {
          heading: 'Team',
          description: 'Manage users and roles that belong to the authenticated merchant.',
          nextCapability: 'This section is restricted to MERCHANT_ADMIN.',
        },
        title: 'Team | FinPay Console',
      },
      {
        path: 'webhooks',
        loadComponent: () =>
          import('./shared/feature-placeholder/feature-placeholder').then(
            (component) => component.FeaturePlaceholder,
          ),
        canActivate: [adminGuard],
        data: {
          heading: 'Webhook endpoints',
          description: 'Register callback URLs and manage endpoint activation for this merchant.',
          nextCapability: 'This section is restricted to MERCHANT_ADMIN.',
        },
        title: 'Webhooks | FinPay Console',
      },
      {
        path: 'webhook-events',
        loadComponent: () =>
          import('./shared/feature-placeholder/feature-placeholder').then(
            (component) => component.FeaturePlaceholder,
          ),
        canActivate: [adminGuard],
        data: {
          heading: 'Webhook deliveries',
          description: 'Inspect event processing, attempts, HTTP responses and retry outcomes.',
          nextCapability: 'This section is restricted to MERCHANT_ADMIN.',
        },
        title: 'Webhook deliveries | FinPay Console',
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'overview',
      },
      {
        path: '**',
        redirectTo: 'overview',
      },
    ],
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
