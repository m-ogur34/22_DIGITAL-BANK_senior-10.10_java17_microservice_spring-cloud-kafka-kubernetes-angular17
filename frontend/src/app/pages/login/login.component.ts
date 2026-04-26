import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgIf } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="login-wrapper">
      <mat-card class="login-card">
        <mat-card-header>
          <mat-card-title>DigitalBank</mat-card-title>
          <mat-card-subtitle>Hesabınıza giriş yapın</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>E-posta</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email">
              <mat-icon matSuffix>email</mat-icon>
              <mat-error *ngIf="loginForm.get('email')?.hasError('required')">E-posta zorunlu</mat-error>
              <mat-error *ngIf="loginForm.get('email')?.hasError('email')">Geçerli e-posta girin</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Şifre</mat-label>
              <input matInput [type]="showPassword() ? 'text' : 'password'"
                     formControlName="password" autocomplete="current-password">
              <button mat-icon-button matSuffix type="button"
                      (click)="showPassword.update(v => !v)">
                <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-error *ngIf="loginForm.get('password')?.hasError('required')">Şifre zorunlu</mat-error>
            </mat-form-field>

            <div class="error-msg" *ngIf="errorMsg()">{{ errorMsg() }}</div>

            <button mat-raised-button color="primary" type="submit"
                    class="full-width submit-btn"
                    [disabled]="loginForm.invalid || loading()">
              <mat-spinner diameter="20" *ngIf="loading()"></mat-spinner>
              <span *ngIf="!loading()">Giriş Yap</span>
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p class="register-link">
            Hesabınız yok mu? <a routerLink="/register">Kayıt olun</a>
          </p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #3f51b5 0%, #283593 100%);
    }
    .login-card { width: 400px; padding: 16px; }
    .full-width { width: 100%; margin-bottom: 12px; }
    .submit-btn { height: 44px; margin-top: 8px; }
    .error-msg { color: #f44336; font-size: 13px; margin-bottom: 8px; }
    .register-link { text-align: center; font-size: 13px; }
    mat-card-title { font-size: 24px; color: #3f51b5; }
  `]
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  errorMsg = signal('');
  showPassword = signal(false);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required]
  });

  onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.loading.set(true);
    this.errorMsg.set('');

    const { email, password } = this.loginForm.value;

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message ?? 'Giriş başarısız. E-posta veya şifre hatalı.');
      }
    });
  }
}
