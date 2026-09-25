import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FaseConvenio } from '../../model/convenio-fase.model';

@Component({
  selector: 'app-phase-stepper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="w-full bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm overflow-hidden select-none">
      <!-- Cabeçalho do Stepper -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 mb-6">
        <div>
          <h3 class="text-xs font-semibold uppercase tracking-wider text-gov-slate-300 flex items-center gap-2">
            <span>Ciclo de Vida do Convênio (Fases 0 a 9)</span>
          </h3>
          <p class="text-[11px] text-gov-slate-400 mt-0.5">
            Clique na fase desejada para abrir os detalhes das condicionantes e peças técnicas
          </p>
        </div>

        <!-- Legenda -->
        <div class="flex items-center gap-3 text-[11px] font-mono">
          <span class="inline-flex items-center gap-1.5 text-emerald-400">
            <span class="w-2 h-2 rounded-full bg-emerald-500"></span> Concluída
          </span>
          <span class="inline-flex items-center gap-1.5 text-gov-cobalt-400">
            <span class="w-2 h-2 rounded-full bg-gov-cobalt-500 animate-pulse"></span> Em Execução
          </span>
          <span class="inline-flex items-center gap-1.5 text-gov-slate-500">
            <span class="w-2 h-2 rounded-full bg-gov-slate-700"></span> Pendente
          </span>
        </div>
      </div>

      <!-- Container Rolável das 10 Fases -->
      <div class="overflow-x-auto pb-2 -mx-1 px-1">
        <div class="flex items-start min-w-[960px] justify-between">
          @for (fase of fases; track fase.numero; let last = $last; let i = $index) {
            <div class="flex flex-col items-center" [class.flex-1]="!last">
              <!-- Linha Superior: Círculo + Linha Conectora Alinhada -->
              <div class="flex items-center w-full">
                <!-- Círculo da Fase -->
                <button
                  type="button"
                  (click)="selecionar(fase)"
                  class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold shrink-0 transition-all cursor-pointer focus:outline-none focus:ring-2 focus:ring-gov-cobalt-500/50"
                  [ngClass]="{
                    'bg-emerald-500 text-white shadow-sm ring-2 ring-emerald-500/30': fase.status === 'CONCLUIDA',
                    'bg-gov-cobalt-600 text-white shadow-md ring-4 ring-gov-cobalt-500/30 scale-110': fase.status === 'EM_ANDAMENTO',
                    'bg-white/5 text-gov-slate-400 border border-white/10 hover:bg-white/10 hover:text-white': fase.status === 'PENDENTE' || fase.status === 'BLOQUEADA'
                  }"
                  [title]="fase.nome + ' - ' + fase.descricao"
                >
                  @if (fase.status === 'CONCLUIDA') {
                    <svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  } @else {
                    <span>{{ fase.numero }}</span>
                  }
                </button>

                <!-- Linha Conectora Horizontal Exata -->
                @if (!last) {
                  <div
                    class="flex-1 h-0.5 mx-2 transition-colors"
                    [ngClass]="{
                      'bg-emerald-500': fase.status === 'CONCLUIDA' && fases[i + 1].status === 'CONCLUIDA',
                      'bg-gradient-to-r from-emerald-500 to-gov-cobalt-500': fase.status === 'CONCLUIDA' && fases[i + 1].status === 'EM_ANDAMENTO',
                      'bg-white/10': fase.status !== 'CONCLUIDA' || fases[i + 1].status === 'PENDENTE'
                    }"
                  ></div>
                }
              </div>

              <!-- Rótulos da Fase (Abaixo do Círculo) -->
              <div
                role="button"
                tabindex="0"
                (click)="selecionar(fase)"
                (keyup.enter)="selecionar(fase)"
                class="mt-2.5 text-center w-24 -ml-0.5 focus:outline-none group text-left sm:text-center cursor-pointer"
              >
                <div
                  class="text-[11px] font-semibold truncate transition-colors leading-tight"
                  [ngClass]="{
                    'text-emerald-400': fase.status === 'CONCLUIDA',
                    'text-gov-cobalt-300 font-bold': fase.status === 'EM_ANDAMENTO',
                    'text-gov-slate-400 group-hover:text-gov-slate-200': fase.status === 'PENDENTE'
                  }"
                >
                  {{ fase.codigo }}
                </div>
                <div class="text-[10px] text-gov-slate-500 truncate leading-tight mt-0.5 group-hover:text-gov-slate-400">
                  {{ fase.subtitulo }}
                </div>
              </div>
            </div>
          }
        </div>
      </div>
    </div>
  `
})
export class PhaseStepperComponent {
  @Input() fases: FaseConvenio[] = [];
  @Output() faseSelecionada = new EventEmitter<FaseConvenio>();

  selecionar(fase: FaseConvenio): void {
    this.faseSelecionada.emit(fase);
  }
}
