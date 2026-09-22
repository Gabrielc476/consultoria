import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confidence-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (deveExibir()) {
      <span
        class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium border transition-colors select-none"
        [ngClass]="classes()"
        [title]="tooltipText()"
      >
        <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass()"></span>
        <span>{{ textoExibicao() }}</span>
      </span>
    }
  `
})
export class ConfidenceBadgeComponent {
  private readonly _score = signal<number>(1.0);
  private readonly _hideIfConfident = signal<boolean>(false);
  private readonly _source = signal<'ia' | 'convenio' | 'manual' | null>(null);
  private readonly _customLabel = signal<string | null>(null);

  @Input()
  set score(value: number | null | undefined) {
    this._score.set(value != null ? value : 0);
  }

  @Input()
  set hideIfConfident(value: boolean | string) {
    this._hideIfConfident.set(value === true || value === 'true' || value === '');
  }

  @Input()
  set source(value: 'ia' | 'convenio' | 'manual' | null | undefined) {
    this._source.set(value || null);
  }

  @Input()
  set customLabel(value: string | null | undefined) {
    this._customLabel.set(value || null);
  }

  public readonly deveExibir = computed(() => {
    if (this._source() === 'convenio') return true;
    if (this._hideIfConfident() && this._score() >= 0.90) return false;
    return true;
  });

  public readonly percentualFormatado = computed(() => {
    const s = this._score();
    return `${Math.round(s * 100)}%`;
  });

  public readonly textoExibicao = computed(() => {
    if (this._customLabel()) return this._customLabel()!;
    if (this._source() === 'convenio') return 'Convênio';
    const s = this._score();
    if (s >= 0.90) return 'Alta Certeza';
    if (s >= 0.70) return `Verificar (${Math.round(s * 100)}%)`;
    return `Inseguro (${Math.round(s * 100)}%)`;
  });

  public readonly classes = computed(() => {
    if (this._source() === 'convenio') {
      return 'bg-gov-cobalt-950/60 text-gov-cobalt-300 border-gov-cobalt-500/30';
    }
    const s = this._score();
    if (s >= 0.90) {
      return 'bg-emerald-950/40 text-emerald-300/80 border-emerald-500/20';
    } else if (s >= 0.70) {
      return 'bg-amber-950/70 text-amber-300 border-amber-500/40 shadow-sm';
    } else {
      return 'bg-rose-950/70 text-rose-300 border-rose-500/40 shadow-sm';
    }
  });

  public readonly dotClass = computed(() => {
    if (this._source() === 'convenio') return 'bg-gov-cobalt-400';
    const s = this._score();
    if (s >= 0.90) return 'bg-emerald-400';
    if (s >= 0.70) return 'bg-amber-400 animate-pulse';
    return 'bg-rose-400 animate-pulse';
  });

  public readonly tooltipText = computed(() => {
    if (this._source() === 'convenio') return 'Dado associado automaticamente ao convênio cadastrado';
    const s = this._score();
    if (s >= 0.90) return `Alta Confiança da IA (${Math.round(s * 100)}%) - Pronto para validação`;
    if (s >= 0.70) return `Atenção: Conferência visual recomendada (${Math.round(s * 100)}%)`;
    return `Alerta: Baixa confiança ou não encontrado (${Math.round(s * 100)}%)`;
  });
}
