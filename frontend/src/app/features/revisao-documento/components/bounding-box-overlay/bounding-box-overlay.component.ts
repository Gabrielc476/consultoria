import { Component, Input, Output, EventEmitter, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BoundingBox } from '../../model/documento.model';

export interface BoxRenderItem {
  key: string;
  label: string;
  top: string;
  left: string;
  width: string;
  height: string;
  score: number;
  isAtivo: boolean;
}

@Component({
  selector: 'app-bounding-box-overlay',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="absolute inset-0 pointer-events-none z-30 overflow-hidden">
      @for (item of boxItems(); track item.key) {
        <div
          class="absolute pointer-events-auto cursor-pointer rounded transition-all duration-200 group"
          [ngClass]="getBoxClasses(item)"
          [style.top]="item.top"
          [style.left]="item.left"
          [style.width]="item.width"
          [style.height]="item.height"
          (click)="onBoxClick(item.key)"
          [title]="item.label + ' (' + Math.round(item.score * 100) + '%)'"
        >
          <!-- Tag de identificação sobre a caixa -->
          <div
            class="absolute -top-5 left-0 px-1.5 py-0.5 rounded text-[10px] font-mono tracking-tight font-semibold flex items-center gap-1 shadow-md whitespace-nowrap transition-opacity duration-150"
            [ngClass]="getTagClasses(item)"
          >
            <span>{{ item.label }}</span>
            <span class="opacity-75">({{ Math.round(item.score * 100) }}%)</span>
          </div>
        </div>
      }
    </div>
  `
})
export class BoundingBoxOverlayComponent {
  public readonly Math = Math;

  private readonly _boundingBoxes = signal<Record<string, BoundingBox>>({});
  private readonly _scores = signal<Record<string, number>>({});
  private readonly _campoAtivo = signal<string | null>(null);

  @Input()
  set boundingBoxes(val: Record<string, BoundingBox> | null | undefined) {
    this._boundingBoxes.set(val || {});
  }

  @Input()
  set scores(val: Record<string, number> | null | undefined) {
    this._scores.set(val || {});
  }

  @Input()
  set campoAtivo(val: string | null | undefined) {
    this._campoAtivo.set(val || null);
  }

  @Output() caixaClicada = new EventEmitter<string>();

  public readonly boxItems = computed<BoxRenderItem[]>(() => {
    const boxes = this._boundingBoxes();
    const scores = this._scores();
    const ativo = this._campoAtivo();

    return Object.entries(boxes).map(([key, box]) => {
      const score = scores[key] != null ? scores[key] : 0.95;
      const isAtivo = ativo === key;

      return {
        key,
        label: this.obterRotuloLegivel(key),
        top: `${box.ymin * 100}%`,
        left: `${box.xmin * 100}%`,
        width: `${Math.max(2, (box.xmax - box.xmin) * 100)}%`,
        height: `${Math.max(2, (box.ymax - box.ymin) * 100)}%`,
        score,
        isAtivo
      };
    });
  });

  public onBoxClick(key: string): void {
    this.caixaClicada.emit(key);
  }

  public getBoxClasses(item: BoxRenderItem): string {
    if (item.isAtivo) {
      return 'border-2 border-blue-500 bg-blue-500/25 ring-4 ring-blue-400/40 z-50 animate-spotlight';
    }

    if (item.score >= 0.90) {
      return 'border border-emerald-500/80 bg-emerald-500/10 hover:bg-emerald-500/20 hover:border-emerald-400';
    } else if (item.score >= 0.70) {
      return 'border border-amber-500/80 bg-amber-500/15 hover:bg-amber-500/25 hover:border-amber-400';
    } else {
      return 'border border-rose-500/80 bg-rose-500/20 hover:bg-rose-500/30 hover:border-rose-400';
    }
  }

  public getTagClasses(item: BoxRenderItem): string {
    if (item.isAtivo) {
      return 'bg-blue-600 text-white ring-1 ring-blue-300 opacity-100';
    }

    if (item.score >= 0.90) {
      return 'bg-emerald-800 text-emerald-100 opacity-70 group-hover:opacity-100';
    } else if (item.score >= 0.70) {
      return 'bg-amber-800 text-amber-100 opacity-80 group-hover:opacity-100';
    } else {
      return 'bg-rose-800 text-rose-100 opacity-90 group-hover:opacity-100';
    }
  }

  private obterRotuloLegivel(key: string): string {
    const labels: Record<string, string> = {
      numeroDocumento: 'NF',
      dataEmissao: 'Data',
      cnpjCredor: 'CNPJ',
      razaoSocialCredor: 'Credor',
      valorBruto: 'Vl. Bruto',
      valorLiquido: 'Vl. Líquido',
      valorTotalDeducoes: 'Deduções',
      numeroEmpenho: 'Empenho'
    };
    return labels[key] || key;
  }
}
