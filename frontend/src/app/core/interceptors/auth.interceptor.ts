import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/**
 * HTTP Interceptor — JWT token yönetimi.
 *
 * Angular 17 Functional Interceptor (HttpInterceptorFn):
 * Sınıf tabanlı yerine fonksiyon tabanlı — daha temiz, inject() ile DI.
 *
 * Akış:
 * 1. Her isteğe Authorization: Bearer <token> header ekle
 * 2. 401 hatası gelirse refresh token ile yenile
 * 3. Yenileme başarılıysa orijinal isteği tekrarla
 * 4. Yenileme başarısızsa logout yap
 *
 * Neden interceptor? Her servise manuel header eklememek için DRY prensibi.
 */
export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // /auth/login ve /auth/register gibi public endpoint'lere token ekleme
  const isPublicEndpoint = req.url.includes('/auth/login')
    || req.url.includes('/auth/register')
    || req.url.includes('/auth/refresh');

  const token = authService.getAccessToken();

  // Token varsa ve public endpoint değilse Authorization header ekle
  const authReq = token && !isPublicEndpoint
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      // 401: Token geçersiz veya süresi dolmuş
      if (error.status === 401 && !isPublicEndpoint) {
        const refreshToken = authService.getRefreshToken();

        if (refreshToken) {
          // Refresh token ile yeni access token al
          return authService.refreshToken().pipe(
            switchMap(() => {
              // Yeni token ile orijinal isteği tekrarla
              const newToken = authService.getAccessToken();
              const retryReq = req.clone({
                setHeaders: { Authorization: `Bearer ${newToken}` }
              });
              return next(retryReq);
            }),
            catchError(refreshError => {
              // Refresh da başarısız → logout
              authService.logout();
              router.navigate(['/login']);
              return throwError(() => refreshError);
            })
          );
        } else {
          // Refresh token yok → logout
          authService.logout();
          router.navigate(['/login']);
        }
      }
      return throwError(() => error);
    })
  );
};
