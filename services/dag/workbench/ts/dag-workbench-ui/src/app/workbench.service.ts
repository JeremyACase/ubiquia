import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface ExecutionContext {
  userPrompt: string;
}

@Injectable({ providedIn: 'root' })
export class WorkbenchService {
  private http = inject(HttpClient);

  private apiBase(): string {
    // Read from <meta name="ubiquia-api-base"> or default to /api
    const meta = document.querySelector('meta[name="ubiquia-api-base"]') as HTMLMetaElement | null;
    return (meta?.content || '/api').replace(/\/$/, '');
  }

  postPrompt(prompt: string) {
    const payload: ExecutionContext = { userPrompt: prompt };
    return this.http.post(`${this.apiBase()}/workbench/prompt`, payload);
  }
}
