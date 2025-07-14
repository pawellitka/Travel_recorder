import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { RouterModule } from '@angular/router';
import { routes } from './app/app.routes';
import { importProvidersFrom } from '@angular/core';

bootstrapApplication(AppComponent, {providers: [
  provideHttpClient(), importProvidersFrom(RouterModule.forRoot(routes))
]})
  .catch((err) => console.error(err));
