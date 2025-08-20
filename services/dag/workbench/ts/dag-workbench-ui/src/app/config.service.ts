// src/app/config.service.ts
import { Injectable, signal, computed } from '@angular/core';

type WorkbenchConfig = { apiBase?: string };

async function fetchJson(url: string) {
  const res = await fetch(url, { cache: 'no-store' });
  if (!res.ok) throw new Error(String(res.status));
  return res.json();
}

@Injectable({ providedIn: 'root' })
export class ConfigService {
  private _cfg = signal<WorkbenchConfig | null>(null);
  readonly apiBase = computed(() => (this._cfg()?.apiBase || '/api').replace(/\/$/, ''));

  async load(): Promise<void> {
    if (this._cfg() !== null) return;
    const base = document.baseURI;
    const candidates = [
      new URL('workbench.config.json', base).toString(),          // served from public/
      new URL('assets/workbench.config.json', base).toString()    // fallback to classic assets/
    ];
    for (const url of candidates) {
      try {
        const json = (await fetchJson(url)) as WorkbenchConfig;
        this._cfg.set(json ?? {});
        return;
      } catch { /* try next */ }
    }
    this._cfg.set({});
  }
}
