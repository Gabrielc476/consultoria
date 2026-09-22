import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { pdfDefaultOptions } from 'ngx-extended-pdf-viewer';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { problemDetailsInterceptor } from './core/interceptors/problem-details.interceptor';

// Configura pasta estática dos workers do pdf.js para evitar tela em branco
pdfDefaultOptions.assetsFolder = 'assets';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor, problemDetailsInterceptor])
    )
  ]
};
