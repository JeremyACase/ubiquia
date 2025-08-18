import { Component, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormControl, Validators } from '@angular/forms';
import { NgIf } from '@angular/common';
import { WorkbenchService } from './workbench.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  constructor() {
    document.addEventListener('keydown', (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') this.submit();
    });
  }
  private svc = inject(WorkbenchService);

  title = 'Ubiquia Workbench';
  prompt = new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(2000)] });
  sending = signal(false);
  sent = signal(false);
  error = signal<string | null>(null);

  readonly chars = computed(() => this.prompt.value?.length ?? 0);
  readonly max = 2000;

  async submit() {
    this.error.set(null);
    this.sent.set(false);

    if (this.prompt.invalid || this.sending()) return;
    this.sending.set(true);
    try {
      await this.svc.postPrompt(this.prompt.value).toPromise();
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
