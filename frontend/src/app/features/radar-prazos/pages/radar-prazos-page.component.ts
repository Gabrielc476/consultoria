import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { RadarPrazosApiService } from '../services/radar-prazos-api.service';
import { AlertaConvenio, MunicipioRadar, NivelRisco, RadarPrazosResponse, ResumoRadar, TipoPrazo } from '../model/radar-prazos.model';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import { CurrencyBrlPipe } from '../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../shared/pipes/cnpj.pipe';

@Component({
  selector: 'app-radar-prazos-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CurrencyBrlPipe,
    CnpjPipe
  ],
  template: `
    <div class="min-h-screen w-full bg-gov-slate-950 text-gov-slate-100 flex flex-col font-sans">
      <!-- Topo / Header da Plataforma -->
      <header class="h-16 bg-gov-slate-900 border-b border-gov-slate-800 px-8 flex items-center justify-between">
        <div class="flex items-center gap-6">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-lg bg-gov-cobalt-600 flex items-center justify-center font-bold text-white shadow-md">G</div>
            <h1 class="text-lg font-bold text-white tracking-tight">GovFlow <span class="text-xs text-gov-slate-400 font-normal">| Radar Transferegov</span></h1>
          </div>

          <!-- Navegação entre Módulos -->
          <nav class="flex items-center gap-2 text-xs">
            <a
              routerLink="/documentos"
              class="px-3 py-1.5 rounded-md text-gov-slate-400 hover:text-white hover:bg-gov-slate-800 transition-colors"
            >
              Esteira de Documentos
            </a>
            <a
              routerLink="/radar-prazos"
              class="px-3 py-1.5 rounded-md bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 font-semibold"
            >
              Radar de Prazos Críticos
            </a>
          </nav>
        </div>

        <div class="flex items-center gap-4 text-xs">
          <span class="text-gov-slate-300">{{ auth.usuario()?.nome || 'Consultor Especialista' }}</span>
          <button (click)="auth.logout()" class="text-rose-400 hover:underline">Sair</button>
        </div>
      </header>

      <!-- Conteúdo Principal -->
      <main class="flex-1 p-8 max-w-7xl mx-auto w-full space-y-6">
        <!-- Título e Ações de Topo -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h2 class="text-2xl font-bold text-white tracking-tight flex items-center gap-3">
              <span>Radar Proativo de Prazos Críticos</span>
              <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                SICONV Live Sync
              </span>
            </h2>
            <p class="text-xs text-gov-slate-400 mt-1">
              Semáforo de monitoramento diário de vigência, cláusulas suspensivas e prestação de contas dos municípios monitorados
            </p>
          </div>

          <div class="flex items-center gap-3">
            <button
              type="button"
              (click)="carregar()"
              [disabled]="carregando()"
              class="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-gov-slate-800 hover:bg-gov-slate-700 text-xs font-semibold text-white transition-colors border border-gov-slate-700 disabled:opacity-50"
            >
              <svg *ngIf="carregando()" class="animate-spin h-3.5 w-3.5 text-white" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
              </svg>
              <span>{{ carregando() ? 'Atualizando...' : 'Atualizar Dados' }}</span>
            </button>

            <button
              type="button"
              (click)="dispararAvaliacaoEAlertas()"
              [disabled]="avaliando()"
              class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-xs font-semibold text-white transition-colors shadow-md disabled:opacity-50"
            >
              <svg *ngIf="avaliando()" class="animate-spin h-3.5 w-3.5 text-white" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
              </svg>
              <span>{{ avaliando() ? 'Avaliando Prazos...' : 'Notificar Alertas RabbitMQ' }}</span>
            </button>
          </div>
        </div>

        <!-- SEMÁFORO DE RISCO (KPIs / CARDS CLICÁVEIS) -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <!-- Card Crítico -->
          <div
            (click)="setFiltroRisco('CRITICO')"
            class="cursor-pointer bg-gov-slate-900 border rounded-xl p-5 transition-all hover:scale-[1.02] shadow-lg relative overflow-hidden"
            [ngClass]="filtroRisco() === 'CRITICO' ? 'border-rose-500 ring-2 ring-rose-500/20 bg-rose-950/20' : 'border-rose-500/40 hover:border-rose-500'"
          >
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold uppercase tracking-wider text-rose-400">Prazos Críticos</span>
              <span class="relative flex h-3 w-3">
                <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-rose-400 opacity-75"></span>
                <span class="relative inline-flex rounded-full h-3 w-3 bg-rose-500"></span>
              </span>
            </div>
            <div class="mt-3 flex items-baseline gap-2">
              <span class="text-3xl font-extrabold text-white font-mono">{{ resumo().totalCriticos }}</span>
              <span class="text-xs text-gov-slate-400">convênios</span>
            </div>
            <p class="text-xs text-rose-400/80 mt-1">≤ 15 dias restantes ou já vencidos</p>
          </div>

          <!-- Card Atenção -->
          <div
            (click)="setFiltroRisco('ATENCAO')"
            class="cursor-pointer bg-gov-slate-900 border rounded-xl p-5 transition-all hover:scale-[1.02] shadow-lg"
            [ngClass]="filtroRisco() === 'ATENCAO' ? 'border-amber-500 ring-2 ring-amber-500/20 bg-amber-950/20' : 'border-amber-500/40 hover:border-amber-500'"
          >
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold uppercase tracking-wider text-amber-400">Em Atenção</span>
              <span class="h-2.5 w-2.5 rounded-full bg-amber-400"></span>
            </div>
            <div class="mt-3 flex items-baseline gap-2">
              <span class="text-3xl font-extrabold text-white font-mono">{{ resumo().totalAtencao }}</span>
              <span class="text-xs text-gov-slate-400">convênios</span>
            </div>
            <p class="text-xs text-amber-400/80 mt-1">16 a 60 dias para expiração</p>
          </div>

          <!-- Card Regular -->
          <div
            (click)="setFiltroRisco('REGULAR')"
            class="cursor-pointer bg-gov-slate-900 border rounded-xl p-5 transition-all hover:scale-[1.02] shadow-lg"
            [ngClass]="filtroRisco() === 'REGULAR' ? 'border-emerald-500 ring-2 ring-emerald-500/20 bg-emerald-950/20' : 'border-emerald-500/40 hover:border-emerald-500'"
          >
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold uppercase tracking-wider text-emerald-400">Situação Regular</span>
              <span class="h-2.5 w-2.5 rounded-full bg-emerald-400"></span>
            </div>
            <div class="mt-3 flex items-baseline gap-2">
              <span class="text-3xl font-extrabold text-white font-mono">{{ resumo().totalRegulares }}</span>
              <span class="text-xs text-gov-slate-400">convênios</span>
            </div>
            <p class="text-xs text-emerald-400/80 mt-1">&gt; 60 dias de prazo ativo</p>
          </div>

          <!-- Card Total Geral -->
          <div
            (click)="setFiltroRisco('TODOS')"
            class="cursor-pointer bg-gov-slate-900 border rounded-xl p-5 transition-all hover:scale-[1.02] shadow-lg"
            [ngClass]="filtroRisco() === 'TODOS' ? 'border-gov-cobalt-500 ring-2 ring-gov-cobalt-500/20 bg-gov-slate-800' : 'border-gov-slate-800 hover:border-gov-slate-700'"
          >
            <div class="flex items-center justify-between">
              <span class="text-xs font-semibold uppercase tracking-wider text-gov-slate-300">Total Monitorado</span>
              <span class="h-2.5 w-2.5 rounded-full bg-gov-cobalt-400"></span>
            </div>
            <div class="mt-3 flex items-baseline gap-2">
              <span class="text-3xl font-extrabold text-white font-mono">{{ resumo().totalMonitorados }}</span>
              <span class="text-xs text-gov-slate-400">instrumentos</span>
            </div>
            <p class="text-xs text-gov-slate-400 mt-1">Data base: {{ resumo().dataReferencia || 'Hoje' }}</p>
          </div>
        </div>

        <!-- BARRA DE CONTROLE & FILTROS -->
        <div class="bg-gov-slate-900 border border-gov-slate-800 rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <!-- Campo de Busca -->
          <div class="relative flex-1 max-w-md">
            <span class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gov-slate-500 text-xs">
              🔍
            </span>
            <input
              type="text"
              [(ngModel)]="termoBusca"
              placeholder="Buscar por convênio, município, CNPJ ou objeto..."
              class="w-full pl-9 pr-4 py-2 bg-gov-slate-950 border border-gov-slate-800 rounded-lg text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:border-gov-cobalt-500 transition-colors"
            />
          </div>

          <!-- Filtros de Risco Pills -->
          <div class="flex items-center gap-2 overflow-x-auto">
            <button
              type="button"
              (click)="setFiltroRisco('TODOS')"
              class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              [ngClass]="filtroRisco() === 'TODOS' ? 'bg-gov-cobalt-600 text-white font-semibold' : 'bg-gov-slate-800 text-gov-slate-400 hover:text-white'"
            >
              Todos ({{ resumo().totalMonitorados }})
            </button>
            <button
              type="button"
              (click)="setFiltroRisco('CRITICO')"
              class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              [ngClass]="filtroRisco() === 'CRITICO' ? 'bg-rose-600 text-white font-semibold' : 'bg-gov-slate-800 text-rose-400 hover:text-rose-300'"
            >
              Crítico ({{ resumo().totalCriticos }})
            </button>
            <button
              type="button"
              (click)="setFiltroRisco('ATENCAO')"
              class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              [ngClass]="filtroRisco() === 'ATENCAO' ? 'bg-amber-600 text-white font-semibold' : 'bg-gov-slate-800 text-amber-400 hover:text-amber-300'"
            >
              Atenção ({{ resumo().totalAtencao }})
            </button>
            <button
              type="button"
              (click)="setFiltroRisco('REGULAR')"
              class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
              [ngClass]="filtroRisco() === 'REGULAR' ? 'bg-emerald-600 text-white font-semibold' : 'bg-gov-slate-800 text-emerald-400 hover:text-emerald-300'"
            >
              Regular ({{ resumo().totalRegulares }})
            </button>
          </div>

          <!-- Alternador de Visão -->
          <div class="flex items-center bg-gov-slate-950 p-1 rounded-lg border border-gov-slate-800 text-xs">
            <button
              type="button"
              (click)="alternarVisao('CONVENIOS')"
              class="px-3 py-1.5 rounded-md font-medium transition-colors"
              [ngClass]="visaoAtiva() === 'CONVENIOS' ? 'bg-gov-slate-800 text-white shadow-sm' : 'text-gov-slate-400 hover:text-white'"
            >
              Convênios Detalhados
            </button>
            <button
              type="button"
              (click)="alternarVisao('MUNICIPIOS')"
              class="px-3 py-1.5 rounded-md font-medium transition-colors"
              [ngClass]="visaoAtiva() === 'MUNICIPIOS' ? 'bg-gov-slate-800 text-white shadow-sm' : 'text-gov-slate-400 hover:text-white'"
            >
              Por Município ({{ agrupamentoMunicipios().length }})
            </button>
          </div>
        </div>

        <!-- VISÃO 1: TABELA DETALHADA DE CONVÊNIOS -->
        <div *ngIf="visaoAtiva() === 'CONVENIOS'" class="bg-gov-slate-900 border border-gov-slate-800 rounded-xl overflow-hidden shadow-xl">
          <table class="w-full text-left text-xs text-gov-slate-300">
            <thead class="bg-gov-slate-950 text-gov-slate-400 uppercase tracking-wider font-semibold border-b border-gov-slate-800">
              <tr>
                <th class="py-3 px-4">Convênio / Município</th>
                <th class="py-3 px-4">Marco Crítico</th>
                <th class="py-3 px-4">Dias Restantes</th>
                <th class="py-3 px-4">Datas dos Prazos</th>
                <th class="py-3 px-4">Repasse Federal</th>
                <th class="py-3 px-4">Risco Semáforo</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gov-slate-800/60">
              @for (alerta of alertasFiltrados(); track alerta.convenioId || alerta.nrConvenio) {
                <tr class="hover:bg-gov-slate-800/40 transition-colors">
                  <!-- Convênio e Município -->
                  <td class="py-3.5 px-4">
                    <div class="font-mono font-bold text-white text-sm">
                      Nº {{ alerta.nrConvenio }}
                    </div>
                    <div class="text-xs text-gov-slate-400 flex items-center gap-1 mt-0.5">
                      <span class="text-white font-medium">{{ alerta.municipio }}-{{ alerta.uf }}</span>
                      <span>•</span>
                      <span class="font-mono">{{ alerta.cnpjProponente | cnpj }}</span>
                    </div>
                    <div *ngIf="alerta.objeto" class="text-[11px] text-gov-slate-500 mt-1 line-clamp-1 max-w-sm" [title]="alerta.objeto">
                      {{ alerta.objeto }}
                    </div>
                  </td>

                  <!-- Marco Crítico -->
                  <td class="py-3.5 px-4">
                    <span
                      class="inline-flex items-center px-2 py-0.5 rounded text-[11px] font-semibold"
                      [ngClass]="getTipoPrazoBadgeClass(alerta.tipoPrazoMaisProximo)"
                    >
                      {{ getTipoPrazoLabel(alerta.tipoPrazoMaisProximo) }}
                    </span>
                    <div class="text-[11px] text-gov-slate-400 mt-1 font-mono">
                      Limite: {{ alerta.prazoMaisProximo || 'Não informado' }}
                    </div>
                  </td>

                  <!-- Dias Restantes -->
                  <td class="py-3.5 px-4">
                    <div class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-mono font-bold" [ngClass]="getDiasRestantesBadgeClass(alerta)">
                      <span>{{ formatarDiasRestantes(alerta.diasRestantes) }}</span>
                    </div>
                  </td>

                  <!-- Detalhe dos Três Prazos -->
                  <td class="py-3.5 px-4 font-mono text-[11px] space-y-0.5">
                    <div *ngIf="alerta.dataSuspensiva" class="flex justify-between gap-2">
                      <span class="text-gov-slate-500">Suspensiva:</span>
                      <span [ngClass]="alerta.diasSuspensiva != null && alerta.diasSuspensiva <= 15 ? 'text-rose-400 font-bold' : 'text-gov-slate-300'">
                        {{ alerta.dataSuspensiva }} ({{ alerta.diasSuspensiva }}d)
                      </span>
                    </div>
                    <div *ngIf="alerta.dataFimVigencia" class="flex justify-between gap-2">
                      <span class="text-gov-slate-500">Vigência:</span>
                      <span [ngClass]="alerta.diasFimVigencia != null && alerta.diasFimVigencia <= 15 ? 'text-rose-400 font-bold' : 'text-gov-slate-300'">
                        {{ alerta.dataFimVigencia }} ({{ alerta.diasFimVigencia }}d)
                      </span>
                    </div>
                    <div *ngIf="alerta.dataLimitePrestacaoContas" class="flex justify-between gap-2">
                      <span class="text-gov-slate-500">P. Contas:</span>
                      <span [ngClass]="alerta.diasPrestacaoContas != null && alerta.diasPrestacaoContas <= 15 ? 'text-rose-400 font-bold' : 'text-gov-slate-300'">
                        {{ alerta.dataLimitePrestacaoContas }} ({{ alerta.diasPrestacaoContas }}d)
                      </span>
                    </div>
                  </td>

                  <!-- Repasse Federal -->
                  <td class="py-3.5 px-4 font-mono">
                    <div class="text-emerald-400 font-semibold">{{ alerta.valorRepasse | currencyBrl }}</div>
                    <div class="text-[11px] text-gov-slate-500">Global: {{ alerta.valorGlobal | currencyBrl }}</div>
                  </td>

                  <!-- Risco Semáforo -->
                  <td class="py-3.5 px-4">
                    <span
                      class="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-semibold uppercase tracking-wider"
                      [ngClass]="getRiscoBadgeClass(alerta.nivelRisco)"
                    >
                      <span class="h-2 w-2 rounded-full" [ngClass]="getRiscoDotClass(alerta.nivelRisco)"></span>
                      <span>{{ alerta.nivelRisco }}</span>
                    </span>
                  </td>
                </tr>
              }

              @if (alertasFiltrados().length === 0 && !carregando()) {
                <tr>
                  <td colspan="6" class="py-12 text-center text-gov-slate-500">
                    Nenhum convênio encontrado com os filtros aplicados.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- VISÃO 2: CONSOLIDAÇÃO POR MUNICÍPIO -->
        <div *ngIf="visaoAtiva() === 'MUNICIPIOS'" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (mun of municipiosFiltrados(); track mun.municipio + mun.uf + mun.cnpjProponente) {
            <div class="bg-gov-slate-900 border border-gov-slate-800 rounded-xl p-5 shadow-lg space-y-4 hover:border-gov-slate-700 transition-colors">
              <div class="flex items-start justify-between">
                <div>
                  <h3 class="text-base font-bold text-white">{{ mun.municipio }}-{{ mun.uf }}</h3>
                  <p class="text-xs text-gov-slate-400 font-mono mt-0.5">{{ mun.cnpjProponente | cnpj }}</p>
                  <p class="text-[11px] text-gov-slate-500 line-clamp-1 mt-0.5">{{ mun.nomeProponente }}</p>
                </div>
                <span
                  class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-bold uppercase"
                  [ngClass]="getRiscoBadgeClass(mun.maiorRisco)"
                >
                  <span class="h-1.5 w-1.5 rounded-full" [ngClass]="getRiscoDotClass(mun.maiorRisco)"></span>
                  {{ mun.maiorRisco }}
                </span>
              </div>

              <!-- Barras de Proporção de Prazos -->
              <div class="space-y-1.5">
                <div class="flex justify-between text-xs text-gov-slate-300">
                  <span>Convênios Monitorados:</span>
                  <span class="font-mono font-bold text-white">{{ mun.totalConvenios }}</span>
                </div>
                <div class="grid grid-cols-3 gap-2 pt-2 border-t border-gov-slate-800 text-center font-mono">
                  <div class="bg-rose-950/30 border border-rose-500/20 rounded p-1.5">
                    <div class="text-xs text-rose-400 font-bold">{{ mun.totalCriticos }}</div>
                    <div class="text-[10px] text-gov-slate-400">Críticos</div>
                  </div>
                  <div class="bg-amber-950/30 border border-amber-500/20 rounded p-1.5">
                    <div class="text-xs text-amber-400 font-bold">{{ mun.totalAtencao }}</div>
                    <div class="text-[10px] text-gov-slate-400">Atenção</div>
                  </div>
                  <div class="bg-emerald-950/30 border border-emerald-500/20 rounded p-1.5">
                    <div class="text-xs text-emerald-400 font-bold">{{ mun.totalRegulares }}</div>
                    <div class="text-[10px] text-gov-slate-400">Regulares</div>
                  </div>
                </div>
              </div>

              <button
                type="button"
                (click)="filtrarPorMunicipio(mun.municipio)"
                class="w-full py-1.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-xs font-semibold text-white transition-colors text-center"
              >
                Ver Convênios deste Município →
              </button>
            </div>
          }

          @if (municipiosFiltrados().length === 0 && !carregando()) {
            <div class="col-span-full py-12 text-center text-gov-slate-500 bg-gov-slate-900 border border-gov-slate-800 rounded-xl">
              Nenhum município localizado com o termo pesquisado.
            </div>
          }
        </div>
      </main>
    </div>
  `
})
export class RadarPrazosPageComponent implements OnInit {
  public readonly auth = inject(AuthService);
  private readonly radarApi = inject(RadarPrazosApiService);
  private readonly toast = inject(ToastService);

  // Estados com Signals
  public readonly carregando = signal<boolean>(false);
  public readonly avaliando = signal<boolean>(false);
  public readonly filtroRisco = signal<'TODOS' | NivelRisco>('TODOS');
  public readonly termoBusca = signal<string>('');
  public readonly visaoAtiva = signal<'CONVENIOS' | 'MUNICIPIOS'>('CONVENIOS');
  public readonly dadosRadar = signal<RadarPrazosResponse | null>(null);

  // Computeds
  public readonly resumo = computed<ResumoRadar>(() => {
    return this.dadosRadar()?.resumo || {
      totalMonitorados: 0,
      totalCriticos: 0,
      totalAtencao: 0,
      totalRegulares: 0,
      dataReferencia: ''
    };
  });

  public readonly agrupamentoMunicipios = computed<MunicipioRadar[]>(() => {
    return this.dadosRadar()?.agrupamentoPorMunicipio || [];
  });

  public readonly alertasFiltrados = computed<AlertaConvenio[]>(() => {
    const dados = this.dadosRadar();
    if (!dados) return [];

    let lista = dados.alertas;
    const risco = this.filtroRisco();
    if (risco !== 'TODOS') {
      lista = lista.filter(a => a.nivelRisco === risco);
    }

    const termo = this.termoBusca().trim().toLowerCase();
    if (termo) {
      lista = lista.filter(a =>
        a.nrConvenio.toLowerCase().includes(termo) ||
        a.municipio.toLowerCase().includes(termo) ||
        a.cnpjProponente.includes(termo) ||
        (a.nomeProponente && a.nomeProponente.toLowerCase().includes(termo)) ||
        (a.objeto && a.objeto.toLowerCase().includes(termo))
      );
    }

    return lista;
  });

  public readonly municipiosFiltrados = computed<MunicipioRadar[]>(() => {
    const termo = this.termoBusca().trim().toLowerCase();
    let lista = this.agrupamentoMunicipios();

    if (termo) {
      lista = lista.filter(m =>
        m.municipio.toLowerCase().includes(termo) ||
        m.cnpjProponente.includes(termo) ||
        m.nomeProponente.toLowerCase().includes(termo)
      );
    }

    return lista;
  });

  ngOnInit(): void {
    this.carregar();
  }

  public carregar(): void {
    this.carregando.set(true);
    this.radarApi.getRadar().subscribe({
      next: (resp) => {
        this.dadosRadar.set(resp);
        this.carregando.set(false);
      },
      error: (err) => {
        this.carregando.set(false);
        this.toast.erro('Erro ao carregar Radar de Prazos', err.message || 'Falha na comunicação com o serviço.');
      }
    });
  }

  public dispararAvaliacaoEAlertas(): void {
    this.avaliando.set(true);
    this.radarApi.dispararAvaliacao().subscribe({
      next: (res) => {
        this.avaliando.set(false);
        this.toast.sucesso(
          'Avaliação de Prazos Concluída',
          `${res.totalAlertasCriticosEmitidos} alertas de prazos críticos despachados para o RabbitMQ!`
        );
        this.carregar();
      },
      error: (err) => {
        this.avaliando.set(false);
        this.toast.erro('Falha na avaliação de prazos', err.message);
      }
    });
  }

  public setFiltroRisco(risco: 'TODOS' | NivelRisco): void {
    this.filtroRisco.set(risco);
  }

  public alternarVisao(visao: 'CONVENIOS' | 'MUNICIPIOS'): void {
    this.visaoAtiva.set(visao);
  }

  public filtrarPorMunicipio(municipio: string): void {
    this.termoBusca.set(municipio);
    this.visaoAtiva.set('CONVENIOS');
  }

  public formatarDiasRestantes(dias?: number): string {
    if (dias == null) return 'Sem prazo ativo';
    if (dias < 0) return `Vencido há ${Math.abs(dias)}d`;
    if (dias === 0) return 'Vence Hoje!';
    return `${dias} dias restantes`;
  }

  public getDiasRestantesBadgeClass(alerta: AlertaConvenio): string {
    const dias = alerta.diasRestantes;
    if (dias == null) return 'bg-gov-slate-800 text-gov-slate-400 border border-gov-slate-700';
    if (dias <= 15) return 'bg-rose-500/20 text-rose-400 border border-rose-500/40';
    if (dias <= 60) return 'bg-amber-500/20 text-amber-400 border border-amber-500/40';
    return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40';
  }

  public getRiscoBadgeClass(risco?: NivelRisco): string {
    switch (risco) {
      case 'CRITICO':
        return 'bg-rose-500/20 text-rose-400 border border-rose-500/30';
      case 'ATENCAO':
        return 'bg-amber-500/20 text-amber-400 border border-amber-500/30';
      case 'REGULAR':
        return 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30';
      default:
        return 'bg-gov-slate-800 text-gov-slate-400';
    }
  }

  public getRiscoDotClass(risco?: NivelRisco): string {
    switch (risco) {
      case 'CRITICO':
        return 'bg-rose-500 animate-pulse';
      case 'ATENCAO':
        return 'bg-amber-400';
      case 'REGULAR':
        return 'bg-emerald-400';
      default:
        return 'bg-gov-slate-500';
    }
  }

  public getTipoPrazoLabel(tipo?: TipoPrazo): string {
    switch (tipo) {
      case 'CLAUSULA_SUSPENSIVA':
        return 'Cláusula Suspensiva';
      case 'FIM_VIGENCIA':
        return 'Fim da Vigência';
      case 'PRESTACAO_CONTAS':
        return 'Prestação de Contas';
      default:
        return 'Não especificado';
    }
  }

  public getTipoPrazoBadgeClass(tipo?: TipoPrazo): string {
    switch (tipo) {
      case 'CLAUSULA_SUSPENSIVA':
        return 'bg-purple-500/20 text-purple-300 border border-purple-500/30';
      case 'FIM_VIGENCIA':
        return 'bg-blue-500/20 text-blue-300 border border-blue-500/30';
      case 'PRESTACAO_CONTAS':
        return 'bg-orange-500/20 text-orange-300 border border-orange-500/30';
      default:
        return 'bg-gov-slate-800 text-gov-slate-400';
    }
  }
}
