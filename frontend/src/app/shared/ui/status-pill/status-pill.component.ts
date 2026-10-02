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
      case 'EM_ANALISE_IA':
      case 'RECEBIDO': return 'Em Análise por IA';
      case 'EM_CONFERENCIA': return 'Em Conferência';
      case 'PRONTO_PARA_TRANSFEREGOV': return 'Pronto Transferegov';
      case 'APROVADO': return 'Aprovado';
      case 'REJEITADO': return 'Rejeitado';
      case 'EXCLUIDO': return 'Excluído';
      default: return this._status();
    }
  });

  public readonly classes = computed(() => {
    switch (this._status()) {
      case 'EM_ANALISE_IA':
      case 'RECEBIDO':
        return 'bg-blue-950/60 text-blue-300 border-blue-500/40 shadow-sm';
      case 'EM_CONFERENCIA':
        return 'bg-amber-950/50 text-amber-300 border-amber-500/40';
      case 'PRONTO_PARA_TRANSFEREGOV':
      case 'APROVADO':
        return 'bg-emerald-950/50 text-emerald-300 border-emerald-500/40';
      case 'REJEITADO':
        return 'bg-rose-950/50 text-rose-300 border-rose-500/40';
      case 'EXCLUIDO':
        return 'bg-slate-900/60 text-slate-400 border-slate-700';
      default:
        return 'bg-slate-800 text-slate-300 border-slate-700';
    }
  });

  public readonly dotClass = computed(() => {
    switch (this._status()) {
      case 'EM_ANALISE_IA':
      case 'RECEBIDO': return 'bg-blue-400 animate-pulse';
      case 'EM_CONFERENCIA': return 'bg-amber-400 animate-pulse';
      case 'PRONTO_PARA_TRANSFEREGOV':
      case 'APROVADO': return 'bg-emerald-400';
      case 'REJEITADO': return 'bg-rose-400';
      case 'EXCLUIDO': return 'bg-slate-500';
      default: return 'bg-slate-400';
    }
  });
}
