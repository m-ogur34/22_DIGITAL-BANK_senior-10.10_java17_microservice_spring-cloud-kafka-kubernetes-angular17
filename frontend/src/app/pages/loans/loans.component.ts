import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CurrencyPipe, DatePipe, NgFor, NgIf, PercentPipe } from '@angular/common';
import { LoanService } from '../../core/services/loan.service';
import { LoanApplication, LoanApplicationRequest } from '../../core/models/loan.model';

@Component({
  selector: 'app-loans',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf, CurrencyPipe, DatePipe, PercentPipe,
    MatCardModule, MatTableModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule, MatChipsModule,
    MatExpansionModule, MatSnackBarModule
  ],
  template: `
    <div class="loans-page">
      <!-- Kredi başvuru formu -->
      <mat-card class="page-card">
        <mat-card-header>
          <mat-card-title>Kredi Başvurusu</mat-card-title>
          <mat-card-subtitle>Anında değerlendirme — kredi skoru hesaplama</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form [formGroup]="loanForm" (ngSubmit)="applyLoan()">
            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Kredi Türü</mat-label>
                <mat-select formControlName="loanType">
                  <mat-option value="IHTIYAC">İhtiyaç Kredisi (maks ₺500.000, faiz %2.50)</mat-option>
                  <mat-option value="KONUT">Konut Kredisi (maks ₺10.000.000, faiz %1.20)</mat-option>
                  <mat-option value="TASIT">Taşıt Kredisi (maks ₺2.000.000, faiz %1.80)</mat-option>
                </mat-select>
                <mat-error>Kredi türü seçin</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Talep Edilen Tutar (TRY)</mat-label>
                <input matInput type="number" formControlName="requestedAmount" min="1000" step="1000">
                <span matPrefix>₺&nbsp;</span>
                <mat-error>Geçerli tutar girin</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Vade (Ay)</mat-label>
                <mat-select formControlName="termMonths">
                  <mat-option *ngFor="let t of termOptions" [value]="t">{{ t }} Ay</mat-option>
                </mat-select>
              </mat-form-field>
            </div>

            <div class="form-row">
              <mat-form-field appearance="outline">
                <mat-label>Aylık Net Gelir (TRY)</mat-label>
                <input matInput type="number" formControlName="monthlyIncome" min="0">
                <span matPrefix>₺&nbsp;</span>
                <mat-error>Gelir bilgisi zorunlu</mat-error>
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Aylık Toplam Borç (TRY)</mat-label>
                <input matInput type="number" formControlName="monthlyDebt" min="0">
                <span matPrefix>₺&nbsp;</span>
                <mat-hint>Mevcut kredi/kira ödemeleri</mat-hint>
              </mat-form-field>
            </div>

            <div class="error-msg" *ngIf="errorMsg()">{{ errorMsg() }}</div>

            <button mat-raised-button color="primary" type="submit"
                    [disabled]="loanForm.invalid || loading()">
              <mat-icon>send</mat-icon> Başvur
            </button>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Aktif krediler -->
      <mat-card class="page-card">
        <mat-card-header>
          <mat-card-title>Kredilerim</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <mat-accordion *ngIf="loans().length > 0">
            <mat-expansion-panel *ngFor="let loan of loans()">
              <mat-expansion-panel-header>
                <mat-panel-title>{{ loan.loanTypeName }} — {{ loan.approvedAmount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</mat-panel-title>
                <mat-panel-description>
                  <mat-chip [class]="'status-chip-' + loan.status">{{ loan.statusName }}</mat-chip>
                </mat-panel-description>
              </mat-expansion-panel-header>

              <!-- Kredi detayları -->
              <div class="loan-details">
                <div class="detail-row">
                  <span>Onaylanan Tutar:</span>
                  <strong>{{ loan.approvedAmount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</strong>
                </div>
                <div class="detail-row">
                  <span>Aylık Taksit:</span>
                  <strong>{{ loan.monthlyInstallment | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</strong>
                </div>
                <div class="detail-row">
                  <span>Faiz Oranı:</span>
                  <strong>%{{ loan.annualInterestRate }}</strong>
                </div>
                <div class="detail-row">
                  <span>Vade:</span>
                  <strong>{{ loan.termMonths }} Ay</strong>
                </div>
                <div class="detail-row">
                  <span>Toplam Ödeme:</span>
                  <strong>{{ loan.totalPayment | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</strong>
                </div>
                <div class="detail-row">
                  <span>Kredi Skoru:</span>
                  <strong [class.amount-positive]="loan.creditScore >= 600"
                          [class.amount-negative]="loan.creditScore < 400">
                    {{ loan.creditScore }} / 1000
                  </strong>
                </div>
              </div>

              <!-- Taksit planı tablosu -->
              <div class="table-container" *ngIf="false">
                <h4>Taksit Planı</h4>
                <table mat-table [dataSource]="loan.installmentPlan!" class="mat-elevation-z1 compact-table">
                  <ng-container matColumnDef="month">
                    <th mat-header-cell *matHeaderCellDef>Ay</th>
                    <td mat-cell *matCellDef="let i">{{ i.month }}</td>
                  </ng-container>
                  <ng-container matColumnDef="dueDate">
                    <th mat-header-cell *matHeaderCellDef>Vade Tarihi</th>
                    <td mat-cell *matCellDef="let i">{{ i.dueDate | date:'dd.MM.yyyy' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="installmentAmount">
                    <th mat-header-cell *matHeaderCellDef>Taksit</th>
                    <td mat-cell *matCellDef="let i">{{ i.installmentAmount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="principalAmount">
                    <th mat-header-cell *matHeaderCellDef>Anapara</th>
                    <td mat-cell *matCellDef="let i">{{ i.principalAmount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="interestAmount">
                    <th mat-header-cell *matHeaderCellDef>Faiz</th>
                    <td mat-cell *matCellDef="let i">{{ i.interestAmount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}</td>
                  </ng-container>
                  <ng-container matColumnDef="status">
                    <th mat-header-cell *matHeaderCellDef>Durum</th>
                    <td mat-cell *matCellDef="let i">
                      <mat-chip [class]="'status-chip-' + i.status">{{ i.status }}</mat-chip>
                    </td>
                  </ng-container>
                  <tr mat-header-row *matHeaderRowDef="installmentColumns"></tr>
                  <tr mat-row *matRowDef="let row; columns: installmentColumns;"></tr>
                </table>
              </div>
            </mat-expansion-panel>
          </mat-accordion>

          <p *ngIf="loans().length === 0 && !loading()" class="empty-msg">
            Aktif krediniz bulunmuyor.
          </p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .loans-page { display: flex; flex-direction: column; gap: 24px; }
    .form-row { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 8px; }
    .form-row mat-form-field { flex: 1; min-width: 200px; }
    .loan-details { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 16px; }
    .detail-row { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #eee; }
    .compact-table { width: 100%; font-size: 13px; }
    .error-msg { color: #f44336; font-size: 13px; margin-bottom: 12px; }
    .empty-msg { text-align: center; color: #666; padding: 32px; }
    h4 { margin: 16px 0 8px; }
  `]
})
export class LoansComponent implements OnInit {
  private loanService = inject(LoanService);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  loans = signal<LoanApplication[]>([]);
  loading = signal(false);
  errorMsg = signal('');

  installmentColumns = ['month', 'dueDate', 'installmentAmount', 'principalAmount', 'interestAmount', 'status'];
  termOptions = [6, 12, 18, 24, 36, 48, 60, 84, 120];

  loanForm = this.fb.group({
    loanType: ['IHTIYAC', Validators.required],
    requestedAmount: [null as number | null, [Validators.required, Validators.min(1000)]],
    termMonths: [12, Validators.required],
    monthlyIncome: [null as number | null, [Validators.required, Validators.min(0)]],
    monthlyDebt: [0, [Validators.required, Validators.min(0)]]
  });

  ngOnInit(): void {
    this.loadLoans();
  }

  loadLoans(): void {
    this.loanService.getMyLoans().subscribe({
      next: (data) => this.loans.set(data)
    });
  }

  applyLoan(): void {
    if (this.loanForm.invalid) return;

    this.loading.set(true);
    this.errorMsg.set('');

    const req: LoanApplicationRequest = {
      loanType: this.loanForm.value.loanType as any,
      amount: this.loanForm.value.requestedAmount!,
      termMonths: this.loanForm.value.termMonths!,
      disbursementIban: '',
      sigortaIsteniyor: false
    };
    const income = this.loanForm.value.monthlyIncome!;

    this.loanService.apply(req, income).subscribe({
      next: () => {
        this.loading.set(false);
        this.snackBar.open('Kredi başvurunuz alındı ve onaylandı!', 'Kapat', { duration: 5000 });
        this.loanForm.reset({ loanType: 'IHTIYAC', termMonths: 12, monthlyDebt: 0 });
        this.loadLoans();
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMsg.set(err.error?.message ?? 'Başvuru işlemi başarısız.');
      }
    });
  }
}
