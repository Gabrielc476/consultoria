import { Component, Input, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-confidence-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span
      class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium border"
      [ngClass]="classes()"
      [title]="tooltipText()"
    >
      <span class="w-1.5 h-1.5 rounded-full" [ngClass]="dotClass()"></span>
      <span>{{ percentualFormatado() }}</span>
    </span>
  `
})
export class ConfidenceBadgeComponent {
  private readonly _score = signal<number>(1.0);

  @Input()
  set score(value: number | null | undefined) {
    this._score.set(value != null ? value : 0);
  }

  public readonly percentualFormatado = computed(() => {
    const s = this._score();
    return `${Math.round(s * 100)}%`;
  });

  public readonly classes = computed(() => {
    const s = this._score();
    if (s >= 0.90) {
      return 'bg-emerald-950/60 text-emerald-300 border-emerald-500/30';
    } else if (s >= 0.70) {
      return 'bg-amber-950/60 text-amber-300 border-amber-500/30';
    } else {
      return 'bg-rose-950/60 text-rose-300 border-rose-500/30';
    }
  });

  public readonly dotClass = computed(() => {
    const s = this._score();
    if (s >= 0.90) return 'bg-emerald-400';
    if (s >= 0.70) return 'bg-amber-400';
    return 'bg-rose-400';
  });

  public readonly tooltipText = computed(() => {
    const s = this._score();
    if (s >= 0.90) return 'Alta Confiança da IA (>90%)';
    if (s >= 0.70) return 'Atenção: Conferência recomendada (70-90%)';
    return 'Baixa Confiança: Requer verificação minuciosa (<70%)';
  });
}
