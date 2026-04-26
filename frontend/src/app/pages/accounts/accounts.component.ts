import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe, NgIf, NgFor } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';

@Component({
  selector: 'app-accounts',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgIf, NgFor, CurrencyPipe,
    MatCardModule, MatTableModule, MatButtonModule, MatIconModule,
    MatDialogModule, MatSelectModule, MatFormFieldModule,
    MatChipsModule, MatSnackBarModule
  ],
  template: `
    <div class="page-card">
      <div class="header">
        <h2>Hesaplarım</h2>
        <button mat-raised-button color="primary" (click)="showCreateForm.update(v => !v)">
          <mat-icon>add</mat-icon> Yeni Hesap
        </button>
      </div>

      <!-- Yeni hesap formu -->
      <div class="create-form" *ngIf="showCreateForm()">
        <form [formGroup]="createForm" (ngSubmit)="createAccount()">
          <mat-form-field appearance="outline">
            <mat-label>Hesap Türü</mat-label>
            <mat-select formControlName="accountType">
              <mat-option value="VADESIZ">Vadesiz Hesap</mat-option>
              <mat-option value="VADELI">Vadeli Hesap</mat-option>
              <mat-option value="TASARRUF">Tasarruf Hesabı</mat-option>
              <mat-option value="YATIRIM">Yatırım Hesabı</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Para Birimi</mat-label>
            <mat-select formControlName="currency">
              <mat-option value="TRY">TRY (Türk Lirası)</mat-option>
              <mat-option value="USD">USD (Dolar)</mat-option>
              <mat-option value="EUR">EUR (Euro)</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="form-actions">
            <button mat-raised-button color="primary" type="submit" [disabled]="createForm.invalid">
              Hesap Aç
            </button>
            <button mat-button type="button" (click)="showCreateForm.set(false)">İptal</button>
          </div>
        </form>
      </div>

      <!-- Hesap tablosu -->
      <div class="table-container">
        <table mat-table [dataSource]="accounts()" class="mat-elevation-z1">
          <ng-container matColumnDef="iban">
            <th mat-header-cell *matHeaderCellDef>IBAN</th>
            <td mat-cell *matCellDef="let acc">
              <code>{{ acc.iban }}</code>
            </td>
          </ng-container>

          <ng-container matColumnDef="accountType">
            <th mat-header-cell *matHeaderCellDef>Tür</th>
            <td mat-cell *matCellDef="let acc">{{ acc.accountType }}</td>
          </ng-container>

          <ng-container matColumnDef="balance">
            <th mat-header-cell *matHeaderCellDef>Bakiye</th>
            <td mat-cell *matCellDef="let acc" [class.amount-positive]="acc.balance > 0">
              {{ acc.balance | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}
            </td>
          </ng-container>

          <ng-container matColumnDef="currency">
            <th mat-header-cell *matHeaderCellDef>Döviz</th>
            <td mat-cell *matCellDef="let acc">{{ acc.currency }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Durum</th>
            <td mat-cell *matCellDef="let acc">
              <mat-chip [class]="'status-chip-' + acc.status">{{ acc.status }}</mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>İşlem</th>
            <td mat-cell *matCellDef="let acc">
              <button mat-icon-button color="warn"
                      *ngIf="acc.status === 'ACTIVE'"
                      (click)="freezeAccount(acc)"
                      title="Hesabı dondur">
                <mat-icon>lock</mat-icon>
              </button>
              <button mat-icon-button color="primary"
                      *ngIf="acc.status === 'FROZEN'"
                      (click)="activateAccount(acc)"
                      title="Hesabı aktifleştir">
                <mat-icon>lock_open</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>

      <p *ngIf="accounts().length === 0 && !loading()" class="empty-msg">
        Henüz hesabınız bulunmuyor.
      </p>
    </div>
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .create-form { background: #f9f9f9; padding: 16px; border-radius: 8px; margin-bottom: 20px;
                   display: flex; gap: 16px; align-items: flex-start; flex-wrap: wrap; }
    .form-actions { display: flex; gap: 8px; align-items: center; padding-top: 4px; }
    table { width: 100%; }
    code { font-size: 12px; background: #f5f5f5; padding: 2px 6px; border-radius: 3px; }
    .empty-msg { text-align: center; color: #666; padding: 32px; }
  `]
})
export class AccountsComponent implements OnInit {
  private accountService = inject(AccountService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  accounts = signal<Account[]>([]);
  loading = signal(true);
  showCreateForm = signal(false);

  displayedColumns = ['iban', 'accountType', 'balance', 'currency', 'status', 'actions'];

  createForm = this.fb.group({
    accountType: ['VADESIZ', Validators.required],
    currency: ['TRY', Validators.required]
  });

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.accountService.getMyAccounts().subscribe({
      next: (data) => { this.accounts.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  createAccount(): void {
    if (this.createForm.invalid) return;
    this.accountService.createAccount(this.createForm.value as any).subscribe({
      next: () => {
        this.snackBar.open('Hesap başarıyla açıldı!', 'Kapat', { duration: 3000 });
        this.showCreateForm.set(false);
        this.loadAccounts();
      },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Hata oluştu', 'Kapat', { duration: 3000 })
    });
  }

  freezeAccount(acc: Account): void {
    this.accountService.freezeAccount(acc.iban).subscribe({
      next: () => { this.snackBar.open('Hesap donduruldu', 'Kapat', { duration: 3000 }); this.loadAccounts(); },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Hata', 'Kapat', { duration: 3000 })
    });
  }

  activateAccount(acc: Account): void {
    this.accountService.activateAccount(acc.iban).subscribe({
      next: () => { this.snackBar.open('Hesap aktifleştirildi', 'Kapat', { duration: 3000 }); this.loadAccounts(); },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Hata', 'Kapat', { duration: 3000 })
    });
  }
}
