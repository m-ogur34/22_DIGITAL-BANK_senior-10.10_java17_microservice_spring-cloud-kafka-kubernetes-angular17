import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

// Angular 17 standalone bootstrap — NgModule yok
bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));
