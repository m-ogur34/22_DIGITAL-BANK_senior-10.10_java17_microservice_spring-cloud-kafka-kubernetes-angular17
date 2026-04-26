import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AsyncPipe, NgIf } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    MatToolbarModule, MatButtonModule, MatIconModule, MatMenuModule,
    RouterLink, RouterLinkActive, NgIf
  ],
  template: `
    <mat-toolbar color="primary" *ngIf="authService.isLoggedIn()">
      <span class="brand">DigitalBank</span>

      <nav class="nav-links">
        <a mat-button routerLink="/dashboard" routerLinkActive="active-link">
          <mat-icon>dashboard</mat-icon> Dashboard
        </a>
        <a mat-button routerLink="/accounts" routerLinkActive="active-link">
          <mat-icon>account_balance</mat-icon> Hesaplar
        </a>
        <a mat-button routerLink="/transfer" routerLinkActive="active-link">
          <mat-icon>swap_horiz</mat-icon> Transfer
        </a>
        <a mat-button routerLink="/transactions" routerLinkActive="active-link">
          <mat-icon>receipt_long</mat-icon> İşlemler
        </a>
        <a mat-button routerLink="/loans" routerLinkActive="active-link">
          <mat-icon>credit_score</mat-icon> Krediler
        </a>
        <a mat-button routerLink="/admin/users" routerLinkActive="active-link"
           *ngIf="authService.isAdmin() || authService.isEmployee()">
          <mat-icon>manage_accounts</mat-icon> Yönetim
        </a>
      </nav>

      <span class="spacer"></span>

      <!-- Kullanıcı menüsü -->
      <button mat-icon-button [matMenuTriggerFor]="userMenu">
        <mat-icon>account_circle</mat-icon>
      </button>
      <mat-menu #userMenu="matMenu">
        <div class="user-info" mat-menu-item disabled>
          {{ authService.currentUser()?.fullName ?? authService.currentUser()?.email }}
        </div>
        <button mat-menu-item (click)="logout()">
          <mat-icon>logout</mat-icon> Çıkış Yap
        </button>
      </mat-menu>
    </mat-toolbar>
  `,
  styles: [`
    .brand { font-size: 20px; font-weight: 600; margin-right: 24px; }
    .nav-links { display: flex; gap: 4px; }
    .spacer { flex: 1; }
    .active-link { background: rgba(255,255,255,0.15); border-radius: 4px; }
    .user-info { font-size: 13px; color: #666; }
  `]
})
export class NavbarComponent {
  authService = inject(AuthService);
  private router = inject(Router);

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
