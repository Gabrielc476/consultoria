import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConvenioContextService } from '../../services/convenio-context.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { ConvenioCockpit } from '../../model/convenio-fase.model';

@Component({
  selector: 'app-convenios-list-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, CurrencyBrlPipe],
  template: `
    <div class="p-6 max-w-7xl mx-auto space-y-6 select-none font-sans">
      <!-- Topo: Título & Contexto do Município -->
      <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <span class="text-xs font-mono px-2 py-0.5 rounded bg-gov-cobalt-500/15 text-gov-cobalt-400 font-semibold border border-gov-cobalt-500/30">
              Módulo de Gestão SICONV
            </span>
            <span class="text-xs text-gov-slate-400">
              {{ municipioCtx.nomeMunicipioAtivoFormatado() }}
            </span>
          </div>
          <h2 class="text-2xl font-bold text-white tracking-tight mt-1">
            Convênios & Contratos de Repasse
          </h2>
          <p class="text-xs text-gov-slate-400 mt-1">
            Selecione um convênio para acessar o Cockpit operacional de 10 Fases, documentos hábeis e conciliação bancária.
          </p>
        </div>

        <!-- Ação Rápida -->
        <div class="flex items-center gap-3">
          <a
            routerLink="/radar-cauc"
            class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-gov-slate-200 border border-white/10 transition-colors inline-flex items-center gap-2"
          >
            <svg class="w-3.5 h-3.5 text-rose-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>Ver Radar CAUC & Prazos</span>
          </a>
        </div>
      </div>

      <!-- Resumo KPI Rápido dos Convênios deste Município -->
      <div class="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-semibold uppercase tracking-wider text-gov-slate-400">Convênios Ativos</div>
          <div class="text-2xl font-bold font-mono text-white mt-1">{{ conveniosDoMunicipio().length }}</div>
          <div class="text-[10px] text-gov-slate-400 mt-1">Monitorados pela consultoria</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-semibold uppercase tracking-wider text-gov-slate-400">Total em Repasse</div>
          <div class="text-2xl font-bold font-mono text-gov-cobalt-400 mt-1">{{ totalRepasse() | currencyBrl }}</div>
          <div class="text-[10px] text-gov-slate-400 mt-1">Recursos Federais</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-semibold uppercase tracking-wider text-gov-slate-400">Saldo Contas Op 006</div>
          <div class="text-2xl font-bold font-mono text-emerald-400 mt-1">{{ totalSaldoOp006() | currencyBrl }}</div>
          <div class="text-[10px] text-gov-slate-400 mt-1">Disponível na Caixa</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-semibold uppercase tracking-wider text-gov-slate-400">Alertas de Vencimento</div>
          <div class="text-2xl font-bold font-mono text-rose-400 mt-1">{{ prazosCriticosCount() }}</div>
          <div class="text-[10px] text-rose-400/80 mt-1">Menos de 30 dias</div>
        </div>
      </div>

      <!-- Barra de Filtro e Busca -->
      <div class="bg-[#111827] border border-white/10 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <!-- Campo de Busca -->
        <div class="relative flex-1 max-w-md">
          <input
            type="text"
            [ngModel]="termoBusca()"
            (ngModelChange)="termoBusca.set($event)"
            placeholder="Buscar por SICONV, órgão concedente ou objeto..."
            class="w-full pl-9 pr-4 py-2 bg-white/5 border border-white/10 rounded-lg text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
          />
          <svg class="w-4 h-4 text-gov-slate-500 absolute left-3 top-2.5 pointer-events-none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <path d="m21 21-4.3-4.3"/>
          </svg>
        </div>

        <!-- Filtros Rápidos por Grupo de Fases -->
        <div class="flex items-center gap-1.5 overflow-x-auto text-xs pb-1 md:pb-0">
          <button
            type="button"
            (click)="filtroFase.set('TODAS')"
            class="px-3 py-1.5 rounded-lg transition-colors shrink-0"
            [ngClass]="filtroFase() === 'TODAS' ? 'bg-gov-cobalt-600 text-white font-semibold' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
          >
            Todos
          </button>
          <button
            type="button"
            (click)="filtroFase.set('PLANEJAMENTO')"
            class="px-3 py-1.5 rounded-lg transition-colors shrink-0"
            [ngClass]="filtroFase() === 'PLANEJAMENTO' ? 'bg-gov-cobalt-600 text-white font-semibold' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
          >
            Fases 0 a 3 (Proposta/VRPL)
          </button>
          <button
            type="button"
            (click)="filtroFase.set('OBRAS_OBTV')"
            class="px-3 py-1.5 rounded-lg transition-colors shrink-0"
            [ngClass]="filtroFase() === 'OBRAS_OBTV' ? 'bg-gov-cobalt-600 text-white font-semibold' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
          >
            Fases 4 e 5 (Medições & OBTV)
          </button>
          <button
            type="button"
            (click)="filtroFase.set('PRESTACAO')"
            class="px-3 py-1.5 rounded-lg transition-colors shrink-0"
            [ngClass]="filtroFase() === 'PRESTACAO' ? 'bg-gov-cobalt-600 text-white font-semibold' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
          >
            Fases 6 a 9 (Prestação & SELIC)
          </button>
          <button
            type="button"
            (click)="filtroFase.set('CRITICOS')"
            class="px-3 py-1.5 rounded-lg transition-colors shrink-0"
            [ngClass]="filtroFase() === 'CRITICOS' ? 'bg-rose-600 text-white font-semibold' : 'text-rose-400 hover:bg-rose-500/10'"
          >
            Prazos Críticos
          </button>
        </div>
      </div>

      <!-- Grid de Cards dos Convênios -->
      <div class="grid grid-cols-1 md:grid-cols-2 gap-5">
        @for (conv of conveniosFiltrados(); track conv.id) {
          <div class="bg-[#111827] border border-white/10 hover:border-gov-cobalt-500/40 rounded-xl p-5 shadow-sm transition-all duration-200 flex flex-col justify-between group relative overflow-hidden">
            <!-- Linha Superior: Siconv #, Concedente e Badge de Status -->
            <div>
              <div class="flex items-center justify-between gap-2 pb-3 border-b border-white/5">
                <div class="flex items-center gap-2">
                  <span class="font-mono text-xs font-bold text-gov-cobalt-400 px-2 py-0.5 rounded bg-gov-cobalt-500/15 border border-gov-cobalt-500/25">
                    #{{ conv.numeroSiconv }}
                  </span>
                  <span class="text-[11px] text-gov-slate-400 truncate">
                    {{ conv.orgaoConcedente }}
                  </span>
                </div>

                @if (conv.diasParaVencimento <= 30) {
                  <span class="text-[10px] font-semibold px-2 py-0.5 rounded bg-rose-500/15 text-rose-400 border border-rose-500/25 flex items-center gap-1 shrink-0">
                    <span class="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></span>
                    {{ conv.diasParaVencimento }} dias restantes
                  </span>
                } @else {
                  <span class="text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-400 border border-emerald-500/25 shrink-0">
                    Em dia ({{ conv.diasParaVencimento }} dias)
                  </span>
                }
              </div>

              <!-- Objeto do Convênio -->
              <h3 class="text-sm font-bold text-white tracking-tight mt-3 line-clamp-2 group-hover:text-gov-cobalt-200 transition-colors">
                {{ conv.objeto }}
              </h3>

              <!-- Fase Atual do Convênio (Ciclo 0 a 9) -->
              <div class="mt-4 p-3 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between">
                <div>
                  <div class="text-[10px] font-semibold uppercase tracking-wider text-gov-slate-400">Fase Atual no Transferegov</div>
                  <div class="text-xs font-bold text-white mt-0.5 flex items-center gap-2">
                    <span class="w-2 h-2 rounded-full bg-gov-cobalt-400 animate-pulse"></span>
                    <span>Fase 0{{ conv.faseAtualNumero }} — {{ conv.fases[conv.faseAtualNumero].nome }}</span>
                  </div>
                </div>
                <div class="text-right">
                  <div class="text-[10px] text-gov-slate-400">Execução RAE</div>
                  <div class="text-xs font-mono font-bold text-emerald-400">{{ conv.percentualExecucao }}%</div>
                </div>
              </div>

              <!-- Dados Financeiros -->
              <div class="grid grid-cols-2 gap-3 mt-4 text-xs">
                <div>
                  <span class="text-[10px] text-gov-slate-400 uppercase tracking-wider block">Repasse Federal</span>
                  <span class="font-mono font-bold text-white">{{ conv.valorRepasse | currencyBrl }}</span>
                </div>
                <div>
                  <span class="text-[10px] text-gov-slate-400 uppercase tracking-wider block">Saldo Op 006 (Caixa)</span>
                  <span class="font-mono font-bold text-gov-cobalt-300">{{ conv.saldoContaOp006 | currencyBrl }}</span>
                </div>
              </div>
            </div>

            <!-- Botão de Ação: Abrir Cockpit -->
            <div class="pt-4 mt-4 border-t border-white/5 flex items-center justify-between gap-3">
              <span class="text-[10px] text-gov-slate-400">
                Vigência até {{ conv.dataFimVigencia }}
              </span>

              <button
                type="button"
                (click)="abrirCockpit(conv.id)"
                class="px-4 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs transition-colors flex items-center gap-1.5 shadow-sm cursor-pointer"
              >
                <span>Acessar Cockpit</span>
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </button>
            </div>
          </div>
        } @empty {
          <div class="col-span-full py-16 text-center text-gov-slate-400">
            <svg class="w-10 h-10 mx-auto text-gov-slate-600 mb-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.3-4.3"/>
            </svg>
            <p class="text-sm font-semibold text-gov-slate-300">Nenhum convênio encontrado com os filtros selecionados.</p>
            <p class="text-xs text-gov-slate-500 mt-1">Alterne o município ativo no topo ou limpe o termo de busca.</p>
          </div>
        }
      </div>
    </div>
  `
})
export class ConveniosListPageComponent {
  readonly convenioCtx = inject(ConvenioContextService);
  readonly municipioCtx = inject(MunicipioContextService);
  private readonly router = inject(Router);

  public readonly termoBusca = signal<string>('');
  public readonly filtroFase = signal<'TODAS' | 'PLANEJAMENTO' | 'OBRAS_OBTV' | 'PRESTACAO' | 'CRITICOS'>('TODAS');

  readonly conveniosDoMunicipio = computed(() => {
    return this.convenioCtx.conveniosDoMunicipio();
  });

  readonly totalRepasse = computed(() => {
    return this.conveniosDoMunicipio().reduce((acc, c) => acc + c.valorRepasse, 0);
  });

  readonly totalSaldoOp006 = computed(() => {
    return this.conveniosDoMunicipio().reduce((acc, c) => acc + c.saldoContaOp006, 0);
  });

  readonly prazosCriticosCount = computed(() => {
    return this.conveniosDoMunicipio().filter(c => c.diasParaVencimento <= 30).length;
  });

  readonly conveniosFiltrados = computed(() => {
    let lista = this.conveniosDoMunicipio();
    const filtro = this.filtroFase();

    if (filtro === 'PLANEJAMENTO') {
      lista = lista.filter(c => c.faseAtualNumero <= 3);
    } else if (filtro === 'OBRAS_OBTV') {
      lista = lista.filter(c => c.faseAtualNumero === 4 || c.faseAtualNumero === 5);
    } else if (filtro === 'PRESTACAO') {
      lista = lista.filter(c => c.faseAtualNumero >= 6);
    } else if (filtro === 'CRITICOS') {
      lista = lista.filter(c => c.diasParaVencimento <= 30);
    }

    const busca = this.termoBusca().toLowerCase().trim();
    if (busca) {
      lista = lista.filter(c =>
        c.numeroSiconv.toLowerCase().includes(busca) ||
        c.objeto.toLowerCase().includes(busca) ||
        c.orgaoConcedente.toLowerCase().includes(busca)
      );
    }

    return lista;
  });

  abrirCockpit(convenioId: string): void {
    this.convenioCtx.selecionarConvenio(convenioId);
    this.router.navigate(['/convenios', convenioId]);
  }
}
