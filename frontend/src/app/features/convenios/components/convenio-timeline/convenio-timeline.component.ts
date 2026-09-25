import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface PrazoConvenioItem {
  id: string;
  fase: string;
  descricao: string;
  dataLimite: string;
  diasRestantes: number;
  criticidade: 'CRITICO' | 'ATENCAO' | 'REGULAR';
}

@Component({
  selector: 'app-convenio-timeline',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm h-full flex flex-col justify-between">
      <div>
        <div class="flex items-center justify-between pb-3 border-b border-white/10 mb-3">
          <div class="flex items-center gap-2">
            <svg class="w-4 h-4 text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect width="18" height="18" x="3" y="4" rx="2" ry="2"/>
              <line x1="16" x2="16" y1="2" y2="6"/>
              <line x1="8" x2="8" y1="2" y2="6"/>
              <line x1="3" x2="21" y1="10" y2="10"/>
            </svg>
            <h4 class="text-xs font-semibold uppercase tracking-wider text-gov-slate-300">
              Cronograma de Prazos Fatais
            </h4>
          </div>
          <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 text-gov-slate-400">
            Regra STF / TCU
          </span>
        </div>

        <div class="space-y-3 overflow-y-auto max-h-72">
          @for (item of prazos; track item.id) {
            <div class="p-3 rounded-lg border border-white/5 bg-white/[0.02] flex items-center justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-white/10 text-gov-slate-300 font-semibold">
                    {{ item.fase }}
                  </span>
                  <span class="text-xs font-medium text-white truncate">
                    {{ item.descricao }}
                  </span>
                </div>
                <div class="text-[11px] text-gov-slate-400 mt-1 flex items-center gap-2">
                  <span>Data Limite:</span>
                  <span class="font-mono text-gov-slate-300">{{ item.dataLimite }}</span>
                </div>
              </div>

              <!-- Badge de Criticidade & Dias Restantes -->
              <div class="text-right shrink-0">
                @if (item.criticidade === 'CRITICO') {
                  <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-rose-500/15 text-rose-400 border border-rose-500/30">
                    D-{{ item.diasRestantes }} dias (Crítico)
                  </span>
                } @else if (item.criticidade === 'ATENCAO') {
                  <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-amber-500/15 text-amber-400 border border-amber-500/30">
                    D-{{ item.diasRestantes }} dias (Atenção)
                  </span>
                } @else {
                  <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/30">
                    D-{{ item.diasRestantes }} dias (Regular)
                  </span>
                }
              </div>
            </div>
          }
        </div>
      </div>

      <div class="pt-3 border-t border-white/10 text-right">
        <a
          routerLink="/radar-cauc"
          class="text-xs text-gov-cobalt-400 hover:text-gov-cobalt-300 transition-colors hover:underline inline-flex items-center gap-1"
        >
          <span>Acessar Radar Completo da Carteira</span>
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </a>
      </div>
    </div>
  `
})
export class ConvenioTimelineComponent {
  @Input() prazos: PrazoConvenioItem[] = [];
}
