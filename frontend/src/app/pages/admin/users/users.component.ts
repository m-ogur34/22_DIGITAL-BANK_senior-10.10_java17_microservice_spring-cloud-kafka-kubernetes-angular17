import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe, NgFor, NgIf } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { environment } from '../../../../environments/environment';

interface UserSummary {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  userType: string;
  active: boolean;
  createdAt: string;
  roles: string[];
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [
    ReactiveFormsModule, NgFor, NgIf, DatePipe,
    MatCardModule, MatTableModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatSnackBarModule, MatTooltipModule
  ],
  template: `
    <div class="page-card">
      <div class="header">
        <h2>Kullanıcı Yönetimi</h2>
        <span class="total-badge">Toplam: {{ users().length }} kullanıcı</span>
      </div>

      <!-- Arama -->
      <mat-form-field appearance="outline" class="search-field">
        <mat-label>Kullanıcı ara</mat-label>
        <input matInput [formControl]="searchControl" placeholder="Ad, soyad veya e-posta">
        <mat-icon matSuffix>search</mat-icon>
      </mat-form-field>

      <!-- Kullanıcı tablosu -->
      <div class="table-container">
        <table mat-table [dataSource]="filteredUsers()" class="mat-elevation-z1">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Ad Soyad</th>
            <td mat-cell *matCellDef="let u">{{ u.firstName }} {{ u.lastName }}</td>
          </ng-container>

          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>E-posta</th>
            <td mat-cell *matCellDef="let u">{{ u.email }}</td>
          </ng-container>

          <ng-container matColumnDef="userType">
            <th mat-header-cell *matHeaderCellDef>Tür</th>
            <td mat-cell *matCellDef="let u">
              <mat-chip [color]="u.userType === 'EMPLOYEE' ? 'accent' : 'primary'" highlighted>
                {{ u.userType }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="roles">
            <th mat-header-cell *matHeaderCellDef>Roller</th>
            <td mat-cell *matCellDef="let u">
              <mat-chip *ngFor="let role of u.roles" class="role-chip">
                {{ role.replace('ROLE_', '') }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Durum</th>
            <td mat-cell *matCellDef="let u">
              <mat-chip [class]="u.active ? 'status-chip-COMPLETED' : 'status-chip-FAILED'">
                {{ u.active ? 'Aktif' : 'Pasif' }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Kayıt Tarihi</th>
            <td mat-cell *matCellDef="let u">{{ u.createdAt | date:'dd.MM.yyyy' }}</td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>İşlem</th>
            <td mat-cell *matCellDef="let u">
              <button mat-icon-button
                      [color]="u.active ? 'warn' : 'primary'"
                      [matTooltip]="u.active ? 'Pasifleştir' : 'Aktifleştir'"
                      (click)="toggleStatus(u)">
                <mat-icon>{{ u.active ? 'person_off' : 'person' }}</mat-icon>
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </div>

      <p *ngIf="filteredUsers().length === 0 && !loading()" class="empty-msg">
        Kullanıcı bulunamadı.
      </p>
    </div>
  `,
  styles: [`
    .header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
    .total-badge { background: #e8eaf6; color: #3f51b5; padding: 4px 12px; border-radius: 12px; font-size: 13px; }
    .search-field { width: 100%; margin-bottom: 16px; }
    table { width: 100%; }
    .role-chip { font-size: 11px; height: 22px; margin: 2px; }
    .empty-msg { text-align: center; color: #666; padding: 32px; }
  `]
})
export class UsersComponent implements OnInit {
  private http = inject(HttpClient);
  private snackBar = inject(MatSnackBar);

  users = signal<UserSummary[]>([]);
  filteredUsers = signal<UserSummary[]>([]);
  loading = signal(false);

  searchControl = new FormControl('');
  displayedColumns = ['name', 'email', 'userType', 'roles', 'status', 'createdAt', 'actions'];

  ngOnInit(): void {
    this.loadUsers();

    // Arama — 300ms debounce ile gereksiz filtrelemeyi önle
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(term => this.filterUsers(term ?? ''));
  }

  loadUsers(): void {
    this.loading.set(true);
    this.http.get<UserSummary[]>(`${environment.apiUrls.auth}/api/admin/users`).subscribe({
      next: (data) => {
        this.users.set(data);
        this.filteredUsers.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  filterUsers(term: string): void {
    const lower = term.toLowerCase();
    const filtered = this.users().filter(u =>
      u.firstName.toLowerCase().includes(lower) ||
      u.lastName.toLowerCase().includes(lower) ||
      u.email.toLowerCase().includes(lower)
    );
    this.filteredUsers.set(filtered);
  }

  toggleStatus(user: UserSummary): void {
    const endpoint = user.active
      ? `${environment.apiUrls.auth}/api/admin/users/${user.id}/deactivate`
      : `${environment.apiUrls.auth}/api/admin/users/${user.id}/activate`;

    this.http.patch(endpoint, {}).subscribe({
      next: () => {
        this.snackBar.open(
          user.active ? 'Kullanıcı pasifleştirildi' : 'Kullanıcı aktifleştirildi',
          'Kapat', { duration: 3000 }
        );
        this.loadUsers();
      },
      error: (err) => this.snackBar.open(err.error?.message ?? 'Hata oluştu', 'Kapat', { duration: 3000 })
    });
  }
}
