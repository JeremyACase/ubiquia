import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface ExecutionContext { userPrompt: string; }

@Injectable({ providedIn: 'root' })
export class WorkbenchService {
  private http = inject(HttpClient);
  private base = '/ubiquia/communication-service/node-reverse-proxy/workbench-user-prompt-node';

  postPrompt(prompt: string) {
    const payload: ExecutionContext = { userPrompt: prompt };
    return this.http.post(`${this.base}/push`, payload, { observe: 'response' as const });
  }
}
