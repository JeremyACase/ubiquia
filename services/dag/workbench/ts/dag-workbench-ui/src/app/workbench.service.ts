import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ConfigService } from './config.service'; // loads runtime config

interface ExecutionContext { userPrompt: string; }

@Injectable({ providedIn: 'root' })
export class WorkbenchService {
  private http = inject(HttpClient);
  private cfg = inject(ConfigService);

  postPrompt(prompt: string) {
    const payload: ExecutionContext = { userPrompt: prompt };
    // e.g., http://ubiquia:8080/dag/workbench/generate (from workbench.config.json)
    const url = `${this.cfg.apiBase()}/generate`;
    return this.http.post(url, payload, { observe: 'response' as const });
  }
}
