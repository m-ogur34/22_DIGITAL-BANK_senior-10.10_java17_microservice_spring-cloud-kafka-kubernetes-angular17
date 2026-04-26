import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthState, LoginRequest, RegisterRequest, TokenResponse } from '../models/user.model';

/**
 * Kimlik doğrulama servisi.
 *
 * Angular Signals (Angular 17):
 * - signal(): Reaktif durum değişkeni — değer değişince bağlı view'lar otomatik güncellenir
 * - computed(): Signal'den türetilen hesaplanmış değer — memoize edilir
 * - effect(): Signal değişince yan etki çalıştırır
 *
 * Neden Signals? RxJS BehaviorSubject'e göre:
 * - Daha basit API (subscribe() gerekmez)
 * - Zone.js bağımsız değişim tespiti
 * - TypeScript tip güvenliği daha güçlü
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly AUTH_BASE = environment.apiUrls.auth;

  // Auth durumu — Signal ile reaktif state yönetimi
  private _authState = signal<AuthState>({
    isLoggedIn: false,
    userId: null,
    email: null,
    fullName: null,
    roles: [],
    accessToken: null,
    refreshToken: null
  });

  // Public readonly erişim — dışarıdan değiştirilemez
  readonly authState = this._authState.asReadonly();

  // Computed değerler — signal'den türetilir, bağımlı signal değişince otomatik güncellenir
  readonly isLoggedIn = computed(() => this._authState().isLoggedIn);
  readonly isAdmin = computed(() => this._authState().roles.includes('ROLE_ADMIN'));
  readonly isEmployee = computed(() => this._authState().roles.includes('ROLE_EMPLOYEE'));
  readonly currentUser = computed(() => ({
    email: this._authState().email,
    fullName: this._authState().fullName,
    roles: this._authState().roles
  }));

  constructor(private http: HttpClient, private router: Router) {
    // Sayfa yenilenmesinde localStorage'dan token'ı geri yükle
    this.restoreSession();
  }

  /**
   * Kullanıcı girişi.
   * tap(): Observable'ı etkilemeden yan etki — state güncelleme.
   */
  login(request: LoginRequest): Observable<any> {
    return this.http.post<any>(`${this.AUTH_BASE}/auth/login`, request).pipe(
      tap(response => {
        if (response.success) {
          this.setSession(response.data);
        }
      })
    );
  }

  /**
   * Yeni kullanıcı kaydı.
   */
  register(request: RegisterRequest): Observable<any> {
    return this.http.post<any>(`${this.AUTH_BASE}/auth/register`, request).pipe(
      tap(response => {
        if (response.success) {
          this.setSession(response.data);
        }
      })
    );
  }

  /**
   * Çıkış yapma — token'ları temizle, login sayfasına yönlendir.
   */
  logout(): void {
    const token = this._authState().accessToken;
    if (token) {
      // Backend'e logout isteği gönder (refresh token iptal)
      this.http.post(`${this.AUTH_BASE}/auth/logout`, {}).subscribe({
        error: () => {} // Hata olsa da local state temizlenir
      });
    }
    this.clearSession();
    this.router.navigate(['/login']);
  }

  /**
   * Access token'ı yenile.
   * HTTP interceptor tarafından otomatik çağrılır (401 hatasında).
   */
  refreshToken(): Observable<any> {
    const refreshToken = this._authState().refreshToken;
    return this.http.post<any>(`${this.AUTH_BASE}/auth/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (response.success) {
          this.setSession(response.data);
        }
      })
    );
  }

  getAccessToken(): string | null {
    return this._authState().accessToken;
  }

  getRefreshToken(): string | null {
    return this._authState().refreshToken;
  }

  /**
   * Auth state'ini günceller ve localStorage'a kaydeder.
   */
  private setSession(tokenData: TokenResponse): void {
    // JWT payload'ını parse et (base64 decode)
    const payload = this.parseJwtPayload(tokenData.accessToken);

    this._authState.set({
      isLoggedIn: true,
      userId: payload?.sub ?? null,
      email: tokenData.email,
      fullName: tokenData.fullName,
      roles: tokenData.roles,
      accessToken: tokenData.accessToken,
      refreshToken: tokenData.refreshToken
    });

    // Token'ları localStorage'a kaydet
    localStorage.setItem('access_token', tokenData.accessToken);
    localStorage.setItem('refresh_token', tokenData.refreshToken);
    localStorage.setItem('user_email', tokenData.email);
    localStorage.setItem('user_full_name', tokenData.fullName);
    localStorage.setItem('user_roles', JSON.stringify(tokenData.roles));
  }

  private clearSession(): void {
    this._authState.set({
      isLoggedIn: false, userId: null, email: null, fullName: null,
      roles: [], accessToken: null, refreshToken: null
    });
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('user_email');
    localStorage.removeItem('user_full_name');
    localStorage.removeItem('user_roles');
  }

  /**
   * Sayfa yenilemesinde localStorage'dan oturumu geri yükler.
   */
  private restoreSession(): void {
    const accessToken = localStorage.getItem('access_token');
    const refreshToken = localStorage.getItem('refresh_token');
    if (!accessToken) return;

    const payload = this.parseJwtPayload(accessToken);
    if (!payload || this.isTokenExpired(payload.exp)) {
      this.clearSession();
      return;
    }

    this._authState.set({
      isLoggedIn: true,
      userId: payload.sub,
      email: localStorage.getItem('user_email'),
      fullName: localStorage.getItem('user_full_name'),
      roles: JSON.parse(localStorage.getItem('user_roles') ?? '[]'),
      accessToken,
      refreshToken
    });
  }

  private parseJwtPayload(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch {
      return null;
    }
  }

  private isTokenExpired(exp: number): boolean {
    return Date.now() >= exp * 1000;
  }
}
