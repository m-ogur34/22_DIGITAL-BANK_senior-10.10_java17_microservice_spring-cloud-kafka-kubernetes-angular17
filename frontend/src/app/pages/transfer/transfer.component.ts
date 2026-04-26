import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe, NgFor, NgIf } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';
import { TransactionService } from '../../core/services/transaction.service';

@Component({
  selector: 'app-transfer',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf, CurrencyPipe,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatDividerModule,
    MatProgressSpinnerModule, MatSnackBarModule
  ],
  template: `
    <div class="page-card">
      <h2><mat-icon>swap_horiz</mat-icon> Para Transferi</h2>
      <p class="subtitle">EFT ve havale işlemleri — günlük limit: ₺50.000</p>

      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <!-- Gönderen hesap -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Gönderen Hesap (IBAN)</mat-label>
          <mat-select formControlName="fromIban">
            <mat-option *ngFor="let acc of myAccounts()" [value]="acc.iban">
              {{ acc.iban }} — {{ acc.balance | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}
            </mat-option>
          </mat-select>
          <mat-error>Gönderen hesap seçin</mat-error>
        </mat-form-field>

        <!-- Alıcı IBAN -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Alıcı IBAN</mat-label>
          <input matInput formControlName="toIban" placeholder="TR00 0000 0000 0000 0000 0000 00"
                 (input)="formatIban($event)">
          <mat-hint>Yurt içi (TR ile başlar) veya yurt dışı IBAN</mat-hint>
          <mat-error *ngIf="form.get('toIban')?.hasError('required')">Alıcı IBAN zorunlu</mat-error>
          <mat-error *ngIf="form.get('toIban')?.hasError('minlength')">Geçerli IBAN girin</mat-error>
        </mat-form-field>

        <!-- Tutar -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Tutar (TRY)</mat-label>
          <input matInput type="number" formControlName="amount" min="1" step="0.01">
          <span matPrefix>₺&nbsp;</span>
          <mat-error *ngIf="form.get('amount')?.hasError('required')">Tutar zorunlu</mat-error>
          <mat-error *ngIf="form.get('amount')?.hasError('min')">Minimum 1 TL</mat-error>
        </mat-form-field>

        <!-- Açıklama -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Açıklama</mat-label>
          <input matInput formControlName="description" placeholder="İsteğe bağlı">
        </mat-form-field>

        <mat-divider></mat-divider>

        <!-- Özet -->
        <div class="summary" *ngIf="form.get('amount')?.value">
          <p>Gönderilecek: <strong>{{ form.get('amount')?.value | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</strong></p>
        </div>

        <div class="error-msg" *ngIf="errorMsg()">{{ errorMsg() }}</div>

        <button mat-raised-button color="primary" type="submit"
                [disabled]="form.invalid || loading()" class="submit-btn">
          <mat-spinner diameter="20" *ngIf="loading()"></mat-spinner>
          <mat-icon *ngIf="!loading()">send</mat-icon>
          <span>{{ loading() ? 'İşleniyor...' : 'Transfer Gönder' }}</span>
        </button>
      </form>
    </div>
  `,
  styles: [`
    h2 { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .subtitle { color: #666; font-size: 13px; margin-bottom: 24px; }
    .full-width { width: 100%; margin-bottom: 12px; }
    mat-divider { margin: 16px 0; }
    .summary { padding: 12px; background: #f5f5f5; border-radius: 4px; margin: 12px 0; }
    .submit-btn { display: flex; align-items: center; gap: 8px; height: 44px; margin-top: 8px; }
    .error-msg { color: #f44336; font-size: 13px; margin: 8px 0; }
  `]
})
export class TransferComponent implements OnInit {
  private fb = inject(FormBuilder);
  private accountService = inject(AccountService);
  private transactionService = inject(TransactionService);
  private snackBar = inject(MatSnackBar);

  myAccounts = signal<Account[]>([]);
  loading = signal(false);
  errorMsg = signal('');

  form = this.fb.group({
    fromIban: ['', Validators.required],
    toIban: ['', [Validators.required, Validators.minLength(15)]],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    description: ['']
  });

  ngOnInit(): void {
    this.accountService.getMyAccounts().subscribe({
      next: (accounts) => this.myAccounts.set(accounts.filter(a => a.status === 'ACTIVE'))
    });
  }

  formatIban(event: Event): void {
    const input = event.target as HTMLInputElement;
    // Boşlukları kaldır, büyük harfe çevir
    const clean = input.value.replace(/\s/g, '').toUpperCase();
    this.form.patchValue({ toIban: clean }, { emitEvent: false });
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.loading.set(true);
    this.errorMsg.set('');

    const req = {
      senderIban: this.form.value.fromIban!,
      receiverIban: this.form.value.toIban!,
      amount: this.form.value.amount!,
      description: this.form.value.description ?? ''
    };

    this.transactionService.transfer(req).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Transfer başarıyla tamamlandı!', 'Kapat', {
          duration: 4000,
          panelClass: 'success-snack'
        });
        this.form.reset({ fromIban: req.senderIban });
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message ?? 'Transfer işlemi başarısız oldu.');
      }
    });
  }
}
