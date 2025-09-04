import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { firstValueFrom } from 'rxjs';
import { WorkbenchService } from './workbench.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private svc = inject(WorkbenchService);

  title = 'Ubiquia Workbench';

  prompt = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(2000)]
  });

  // 👇 turn valueChanges into a signal
  private promptValue = toSignal(this.prompt.valueChanges, {
    initialValue: this.prompt.value
  });

  // now computed depends on a signal and will update
  readonly chars = computed(() => this.promptValue()?.length ?? 0);
  readonly max = 2000;

  sending = signal(false);
  sent = signal(false);
  error = signal<string | null>(null);

  constructor() {
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') this.submit();
    });
  }

  async submit() {
    this.error.set(null);
    this.sent.set(false);
    if (this.prompt.invalid || this.sending()) return;

    this.sending.set(true);
    try {
      const res = await firstValueFrom(this.svc.postPrompt(this.prompt.value));
      if (res.status < 200 || res.status >= 300) {
        throw new Error(`Non-2xx status: ${res.status}`);
      }
      this.sent.set(true);
      this.prompt.reset('');
    } catch (e: any) {
      const msg = e?.error?.message || e?.message || 'Failed to send prompt';
      this.error.set(String(msg));
    } finally {
      this.sending.set(false);
    }
  }
}
