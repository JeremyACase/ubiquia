import { ApplicationConfig, APP_INITIALIZER, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { ConfigService } from './config.service';

function initConfig(cfg: ConfigService) {
  return () => cfg.load();
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(withFetch()),
    { provide: APP_INITIALIZER, useFactory: initConfig, deps: [ConfigService], multi: true }
  ]
};