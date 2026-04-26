import { Injectable, signal } from '@angular/core';

// Global loading state — HTTP interceptor veya component'lar tetikler
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private _loading = signal(false);
  readonly isLoading = this._loading.asReadonly();

  show(): void { this._loading.set(true); }
  hide(): void { this._loading.set(false); }
}
