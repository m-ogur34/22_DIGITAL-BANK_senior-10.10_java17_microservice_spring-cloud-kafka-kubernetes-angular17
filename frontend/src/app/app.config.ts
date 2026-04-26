import { ApplicationConfig } from '@angular/core';
import {
  provideRouter,
  withComponentInputBinding,
  withViewTransitions
} from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';

// Angular 17 standalone API: NgModule yerine fonksiyon tabanlı provider'lar
export const appConfig: ApplicationConfig = {
  providers: [
    // Router — view transitions (CSS animasyon), input binding
    provideRouter(routes, withComponentInputBinding(), withViewTransitions()),

    // HTTP — functional interceptor ile token enjeksiyonu
    provideHttpClient(withInterceptors([authInterceptor])),

    // Angular Material animasyonları async chunk olarak yükle
    provideAnimationsAsync()
  ]
};
