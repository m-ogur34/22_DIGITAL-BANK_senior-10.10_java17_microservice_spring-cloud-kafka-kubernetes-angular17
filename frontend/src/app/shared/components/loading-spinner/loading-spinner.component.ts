import { Component, inject } from '@angular/core';
import { NgIf } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { LoadingService } from '../../../core/services/loading.service';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [NgIf, MatProgressSpinnerModule],
  template: `
    <div class="overlay" *ngIf="loadingService.isLoading()">
      <mat-spinner diameter="48" />
    </div>
  `,
  styles: [`
    .overlay {
      position: fixed;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.3);
      z-index: 9999;
    }
  `]
})
export class LoadingSpinnerComponent {
  loadingService = inject(LoadingService);
}
