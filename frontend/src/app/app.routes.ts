import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

// Lazy loading: her modül ayrı chunk — ilk yükleme hızı artar
export const routes: Routes = [
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    // Giriş gerektiren sayfalar — authGuard tüm children'a uygulanır
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'accounts',
        loadComponent: () =>
          import('./pages/accounts/accounts.component').then(m => m.AccountsComponent)
      },
      {
        path: 'transfer',
        loadComponent: () =>
          import('./pages/transfer/transfer.component').then(m => m.TransferComponent)
      },
      {
        path: 'transactions',
        loadComponent: () =>
          import('./pages/transactions/transactions.component').then(m => m.TransactionsComponent)
      },
      {
        path: 'loans',
        loadComponent: () =>
          import('./pages/loans/loans.component').then(m => m.LoansComponent)
      },
      {
        // Sadece ADMIN ve EMPLOYEE erişebilir
        path: 'admin/users',
        canActivate: [roleGuard],
        data: { roles: ['ROLE_ADMIN', 'ROLE_EMPLOYEE'] },
        loadComponent: () =>
          import('./pages/admin/users/users.component').then(m => m.UsersComponent)
      }
    ]
  },
  {
    path: '**',
    redirectTo: '/dashboard'
  }
];
