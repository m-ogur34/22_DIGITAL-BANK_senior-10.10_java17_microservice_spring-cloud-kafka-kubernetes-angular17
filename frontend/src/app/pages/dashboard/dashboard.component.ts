import { Component, inject, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    MatCardModule, MatIconModule, MatButtonModule,
    RouterLink, CurrencyPipe, DatePipe, NgFor, NgIf
  ],
  template: `
    <div class="dashboard">
      <h1 class="greeting">
        Hoş geldiniz, {{ authService.currentUser()?.fullName?.split(' ')[0] }} 👋
      </h1>

      <!-- Özet kartlar -->
      <div class="summary-grid">
        <mat-card class="summary-card">
          <mat-card-content>
            <mat-icon color="primary">account_balance_wallet</mat-icon>
            <div>
              <p class="label">Toplam Bakiye</p>
              <p class="amount">{{ totalBalance() | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</p>
            </div>
          </mat-card-content>
        </mat-card>

        <mat-card class="summary-card">
          <mat-card-content>
            <mat-icon color="accent">credit_card</mat-icon>
            <div>
              <p class="label">Hesap Sayısı</p>
              <p class="amount">{{ accounts().length }}</p>
            </div>
          </mat-card-content>
        </mat-card>
      </div>

      <!-- Hesaplar listesi -->
      <mat-card class="page-card">
        <mat-card-header>
          <mat-card-title>Hesaplarım</mat-card-title>
          <span class="spacer"></span>
          <a mat-stroked-button routerLink="/accounts">Tümünü Gör</a>
        </mat-card-header>

        <mat-card-content>
          <div *ngIf="loading()">Yükleniyor...</div>

          <div class="account-list" *ngIf="!loading()">
            <div class="account-row" *ngFor="let acc of accounts()">
              <div class="account-info">
                <mat-icon>{{ getAccountIcon(acc.accountType) }}</mat-icon>
                <div>
                  <p class="account-type">{{ acc.accountType }}</p>
                  <p class="account-iban">{{ acc.iban }}</p>
                </div>
              </div>
              <div class="account-balance" [class.amount-positive]="acc.balance > 0">
                {{ acc.balance | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}
              </div>
            </div>

            <p *ngIf="accounts().length === 0" class="empty-msg">
              Henüz hesabınız yok. <a routerLink="/accounts">Hesap açın</a>
            </p>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Hızlı işlemler -->
      <mat-card class="page-card">
        <mat-card-header>
          <mat-card-title>Hızlı İşlemler</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="quick-actions">
            <a mat-raised-button color="primary" routerLink="/transfer">
              <mat-icon>swap_horiz</mat-icon> Para Transferi
            </a>
            <a mat-raised-button routerLink="/loans">
              <mat-icon>credit_score</mat-icon> Kredi Başvurusu
            </a>
            <a mat-raised-button routerLink="/transactions">
              <mat-icon>receipt_long</mat-icon> İşlem Geçmişi
            </a>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .greeting { font-size: 24px; margin-bottom: 24px; }
    .summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .summary-card mat-card-content { display: flex; align-items: center; gap: 16px; padding: 16px; }
    .summary-card mat-icon { font-size: 40px; width: 40px; height: 40px; }
    .label { font-size: 13px; color: #666; margin: 0; }
    .amount { font-size: 22px; font-weight: 600; margin: 0; }
    mat-card-header { display: flex; align-items: center; margin-bottom: 16px; }
    .spacer { flex: 1; }
    .account-list { display: flex; flex-direction: column; gap: 12px; }
    .account-row { display: flex; justify-content: space-between; align-items: center; padding: 12px; border: 1px solid #eee; border-radius: 8px; }
    .account-info { display: flex; align-items: center; gap: 12px; }
    .account-type { font-weight: 500; margin: 0; }
    .account-iban { font-size: 12px; color: #666; margin: 0; }
    .account-balance { font-size: 18px; font-weight: 600; }
    .empty-msg { color: #666; text-align: center; padding: 24px; }
    .quick-actions { display: flex; gap: 16px; flex-wrap: wrap; }
  `]
})
export class DashboardComponent implements OnInit {
  private accountService = inject(AccountService);
  authService = inject(AuthService);

  accounts = signal<Account[]>([]);
  loading = signal(true);
  totalBalance = signal(0);

  ngOnInit(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.totalBalance.set(accounts.reduce((sum, a) => sum + a.balance, 0));
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  getAccountIcon(type: string): string {
    const icons: Record<string, string> = {
      VADESIZ: 'account_balance',
      VADELI: 'savings',
      TASARRUF: 'piggy_bank',
      YATIRIM: 'trending_up'
    };
    return icons[type] ?? 'account_balance';
  }
}
