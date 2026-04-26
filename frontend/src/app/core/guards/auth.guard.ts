import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Route'a sadece giriş yapmış kullanıcılar erişebilir
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    return true;
  }

  // Giriş yapmamış → login sayfasına yönlendir, hedef URL'yi queryParam olarak sakla
  router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
  return false;
};
