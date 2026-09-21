import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-pill',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold uppercase tracking-wider border"
      [ngClass]="classes()"
    >
      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass()"></span>
      <span>{{ label() }}</span>
    </span>
  `
})
export class StatusPillComponent {
  private readonly _status = signal<string>('RECEBIDO');

  @Input()
  set status(value: string | null | undefined) {
    this._status.set(value || 'RECEBIDO');
  }

  public readonly label = computed(() => {
    switch (this._status()) {
      case 'EM_CONFERENCIA': return 'Em Conferência';
      case 'PRONTO_PARA_TRANSFEREGOV': return 'Pronto Transferegov';
      case 'REJEITADO': return 'Rejeitado';
      case 'RECEBIDO': return 'Recebido';
      default: return this._status();
    }
  });

  public readonly classes = computed(() => {
    switch (this._status()) {
      case 'EM_CONFERENCIA':
        return 'bg-amber-950/50 text-amber-300 border-amber-500/40';
      case 'PRONTO_PARA_TRANSFEREGOV':
        return 'bg-emerald-950/50 text-emerald-300 border-emerald-500/40';
      case 'REJEITADO':
        return 'bg-rose-950/50 text-rose-300 border-rose-500/40';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  });

  public readonly dotClass = computed(() => {
    switch (this._status()) {
      case 'EM_CONFERENCIA': return 'bg-amber-400 animate-pulse';
      case 'PRONTO_PARA_TRANSFEREGOV': return 'bg-emerald-400';
      case 'REJEITADO': return 'bg-rose-400';
      default: return 'bg-slate-400';
    }
  });
}
