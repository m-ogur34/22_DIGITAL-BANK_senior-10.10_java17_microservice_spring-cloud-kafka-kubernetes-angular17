import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { CurrencyPipe, DatePipe, NgIf, NgFor } from '@angular/common';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction, TransactionSearchRequest } from '../../core/models/transaction.model';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgIf, NgFor, CurrencyPipe, DatePipe,
    MatCardModule, MatTableModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule,
    MatDatepickerModule, MatNativeDateModule, MatPaginatorModule
  ],
  template: `
    <div class="page-card">
      <h2>İşlem Geçmişi</h2>

      <!-- Arama/Filtreleme formu -->
      <form [formGroup]="searchForm" (ngSubmit)="search()" class="search-form">
        <mat-form-field appearance="outline">
          <mat-label>Anahtar kelime</mat-label>
          <input matInput formControlName="keyword" placeholder="Açıklama ile ara">
          <mat-icon matSuffix>search</mat-icon>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>IBAN</mat-label>
          <input matInput formControlName="iban" placeholder="TR...">
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Başlangıç Tarihi</mat-label>
          <input matInput [matDatepicker]="startPicker" formControlName="startDate">
          <mat-datepicker-toggle matIconSuffix [for]="startPicker"></mat-datepicker-toggle>
          <mat-datepicker #startPicker></mat-datepicker>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Bitiş Tarihi</mat-label>
          <input matInput [matDatepicker]="endPicker" formControlName="endDate">
          <mat-datepicker-toggle matIconSuffix [for]="endPicker"></mat-datepicker-toggle>
          <mat-datepicker #endPicker></mat-datepicker>
        </mat-form-field>

        <div class="search-actions">
          <button mat-raised-button color="primary" type="submit">
            <mat-icon>search</mat-icon> Ara
          </button>
          <button mat-button type="button" (click)="resetSearch()">Temizle</button>
        </div>
      </form>

      <!-- Tablo -->
      <div class="table-container">
        <table mat-table [dataSource]="transactions()" class="mat-elevation-z1">
          <ng-container matColumnDef="date">
            <th mat-header-cell *matHeaderCellDef>Tarih</th>
            <td mat-cell *matCellDef="let t">{{ t.createdAt | date:'dd.MM.yyyy HH:mm' }}</td>
          </ng-container>

          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef>Tür</th>
            <td mat-cell *matCellDef="let t">
              <mat-icon [color]="t.type === 'CREDIT' ? 'primary' : 'warn'">
                {{ t.type === 'CREDIT' ? 'arrow_downward' : 'arrow_upward' }}
              </mat-icon>
              {{ t.type }}
            </td>
          </ng-container>

          <ng-container matColumnDef="amount">
            <th mat-header-cell *matHeaderCellDef>Tutar</th>
            <td mat-cell *matCellDef="let t"
                [class.amount-positive]="t.type === 'CREDIT'"
                [class.amount-negative]="t.type === 'DEBIT'">
              {{ t.type === 'CREDIT' ? '+' : '-' }}{{ t.amount | currency:'TRY':'symbol-narrow':'1.2-2':'tr' }}
            </td>
          </ng-container>

          <ng-container matColumnDef="counterparty">
            <th mat-header-cell *matHeaderCellDef>Karşı Taraf IBAN</th>
            <td mat-cell *matCellDef="let t">
              <span class="iban-small">{{ t.type === 'DEBIT' ? t.receiverIban : t.senderIban }}</span>
            </td>
          </ng-container>

          <ng-container matColumnDef="description">
            <th mat-header-cell *matHeaderCellDef>Açıklama</th>
            <td mat-cell *matCellDef="let t">{{ t.description }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Durum</th>
            <td mat-cell *matCellDef="let t">
              <mat-chip [class]="'status-chip-' + t.status">{{ t.status }}</mat-chip>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>

      <p *ngIf="transactions().length === 0 && !loading()" class="empty-msg">İşlem bulunamadı.</p>

      <mat-paginator [length]="totalElements()" [pageSize]="pageSize"
                     [pageSizeOptions]="[10, 25, 50]"
                     (page)="onPageChange($event)">
      </mat-paginator>
    </div>
  `,
  styles: [`
    h2 { margin-bottom: 20px; }
    .search-form { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-start; margin-bottom: 20px;
                   padding: 16px; background: #f9f9f9; border-radius: 8px; }
    .search-actions { display: flex; gap: 8px; align-items: center; padding-top: 4px; }
    table { width: 100%; }
    .iban-small { font-size: 11px; font-family: monospace; }
    .empty-msg { text-align: center; color: #666; padding: 32px; }
    td mat-icon { vertical-align: middle; }
  `]
})
export class TransactionsComponent implements OnInit {
  private transactionService = inject(TransactionService);
  private fb = inject(FormBuilder);

  transactions = signal<Transaction[]>([]);
  loading = signal(false);
  totalElements = signal(0);
  pageSize = 10;
  currentPage = 0;

  displayedColumns = ['date', 'type', 'amount', 'counterparty', 'description', 'status'];

  searchForm = this.fb.group({
    keyword: [''],
    iban: [''],
    startDate: [null as Date | null],
    endDate: [null as Date | null]
  });

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.currentPage = 0;
    this.loadData();
  }

  resetSearch(): void {
    this.searchForm.reset();
    this.search();
  }

  loadData(): void {
    this.loading.set(true);
    const val = this.searchForm.value;

    const req: TransactionSearchRequest = {
      keyword: val.keyword ?? undefined,
      iban: val.iban ?? undefined,
      startDate: val.startDate?.toISOString() ?? undefined,
      endDate: val.endDate?.toISOString() ?? undefined,
      page: this.currentPage,
      size: this.pageSize
    };

    this.transactionService.search(req).subscribe({
      next: (page: any) => {
        this.transactions.set(page.content ?? page);
        this.totalElements.set(page.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  onPageChange(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }
}
