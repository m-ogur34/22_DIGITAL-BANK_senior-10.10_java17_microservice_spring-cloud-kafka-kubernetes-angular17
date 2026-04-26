import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Route data'sında tanımlanan role(s)'e sahip kullanıcılara izin ver
export const roleGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Route data: { roles: ['ADMIN'] } veya { roles: ['ADMIN', 'EMPLOYEE'] }
  const requiredRoles: string[] = route.data?.['roles'] ?? [];

  if (requiredRoles.length === 0) return true;

  const user = authService.currentUser();
  if (!user) {
    router.navigate(['/login']);
    return false;
  }

  const hasRole = requiredRoles.some(role => user.roles.includes(role));
  if (!hasRole) {
    // Yetkisiz → dashboard'a yönlendir (403 sayfası yerine)
    router.navigate(['/dashboard']);
    return false;
  }

  return true;
};
