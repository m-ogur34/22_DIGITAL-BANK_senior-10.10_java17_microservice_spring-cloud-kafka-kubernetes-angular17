import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgIf } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

// TC Kimlik No doğrulama — client-side ön kontrol (sunucu da doğrular)
function tcValidation(control: AbstractControl): ValidationErrors | null {
  const tc = control.value?.toString() ?? '';
  if (tc.length !== 11 || tc[0] === '0') return { tcInvalid: true };
  const digits = tc.split('').map(Number);
  const oddSum = digits[0] + digits[2] + digits[4] + digits[6] + digits[8];
  const evenSum = digits[1] + digits[3] + digits[5] + digits[7];
  const d10 = (oddSum * 7 - evenSum) % 10;
  const d11 = digits.slice(0, 10).reduce((a, b) => a + b, 0) % 10;
  return (d10 !== digits[9] || d11 !== digits[10]) ? { tcInvalid: true } : null;
}

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, NgIf,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatSelectModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="register-wrapper">
      <mat-card class="register-card">
        <mat-card-header>
          <mat-card-title>Hesap Oluştur</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <div class="row-2">
              <mat-form-field appearance="outline">
                <mat-label>Ad</mat-label>
                <input matInput formControlName="firstName">
                <mat-error>Ad zorunlu</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Soyad</mat-label>
                <input matInput formControlName="lastName">
                <mat-error>Soyad zorunlu</mat-error>
              </mat-form-field>
            </div>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>TC Kimlik No</mat-label>
              <input matInput formControlName="tcKimlikNo" maxlength="11">
              <mat-error *ngIf="form.get('tcKimlikNo')?.hasError('required')">TC Kimlik No zorunlu</mat-error>
              <mat-error *ngIf="form.get('tcKimlikNo')?.hasError('tcInvalid')">Geçersiz TC Kimlik No</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>E-posta</mat-label>
              <input matInput type="email" formControlName="email">
              <mat-error>Geçerli e-posta girin</mat-error>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Telefon</mat-label>
              <input matInput formControlName="phone" placeholder="05xx xxx xx xx">
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Şifre</mat-label>
              <input matInput [type]="showPwd() ? 'text' : 'password'" formControlName="password">
              <button mat-icon-button matSuffix type="button" (click)="showPwd.update(v => !v)">
                <mat-icon>{{ showPwd() ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
              <mat-hint>En az 8 karakter, büyük/küçük harf ve rakam içermeli</mat-hint>
              <mat-error>Geçerli şifre girin (en az 8 karakter)</mat-error>
            </mat-form-field>

            <div class="error-msg" *ngIf="errorMsg()">{{ errorMsg() }}</div>
            <div class="success-msg" *ngIf="successMsg()">{{ successMsg() }}</div>

            <button mat-raised-button color="primary" type="submit"
                    class="full-width submit-btn" [disabled]="form.invalid || loading()">
              <mat-spinner diameter="20" *ngIf="loading()"></mat-spinner>
              <span *ngIf="!loading()">Kayıt Ol</span>
            </button>
          </form>
        </mat-card-content>

        <mat-card-actions>
          <p class="login-link">Hesabınız var mı? <a routerLink="/login">Giriş yapın</a></p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .register-wrapper {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #3f51b5 0%, #283593 100%);
      padding: 24px;
    }
    .register-card { width: 500px; padding: 16px; }
    .full-width { width: 100%; margin-bottom: 8px; }
    .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 8px; }
    .submit-btn { height: 44px; margin-top: 12px; }
    .error-msg { color: #f44336; font-size: 13px; margin-bottom: 8px; }
    .success-msg { color: #4caf50; font-size: 13px; margin-bottom: 8px; }
    .login-link { text-align: center; font-size: 13px; }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  loading = signal(false);
  errorMsg = signal('');
  successMsg = signal('');
  showPwd = signal(false);

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    tcKimlikNo: ['', [Validators.required, Validators.minLength(11), Validators.maxLength(11), tcValidation]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMsg.set('');

    this.authService.register(this.form.value as any).subscribe({
      next: () => {
        this.loading.set(false);
        this.successMsg.set('Kayıt başarılı! Giriş yapılıyor...');
        setTimeout(() => this.router.navigate(['/dashboard']), 1500);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message ?? 'Kayıt başarısız.');
      }
    });
  }
}
