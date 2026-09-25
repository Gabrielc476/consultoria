import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { ConvenioContextService } from '../../services/convenio-context.service';
import { ConvenioCockpit, FaseConvenio } from '../../model/convenio-fase.model';
import { PhaseStepperComponent } from '../../components/phase-stepper/phase-stepper.component';
import { ConvenioKpisComponent } from '../../components/convenio-kpis/convenio-kpis.component';
import { ConvenioDocumentsListComponent, DocumentoConvenioResumo } from '../../components/convenio-documents-list/convenio-documents-list.component';
import { ConvenioTimelineComponent, PrazoConvenioItem } from '../../components/convenio-timeline/convenio-timeline.component';

const MOCK_DOCS: DocumentoConvenioResumo[] = [
  {
    id: 'doc-nf-004821',
    numeroDocumento: 'NF-e 004821',
    tipo: 'Nota Fiscal de Serviços',
    credorRazaoSocial: 'Construtora Alvorada Ltda',
    valorBruto: 84500.0,
    dataRecebimento: 'Hoje, 09:14',
    origem: 'WHATSAPP',
    confiancaScore: 0.985,
    status: 'EM_CONFERENCIA'
  },
  {
    id: 'doc-bm-003',
    numeroDocumento: 'BM-03/2026',
    tipo: 'Boletim de Medição (Eng.)',
    credorRazaoSocial: 'Construtora Alvorada Ltda',
    valorBruto: 84500.0,
    dataRecebimento: 'Hoje, 09:15',
    origem: 'WHATSAPP',
    confiancaScore: 0.942,
    status: 'EM_CONFERENCIA'
  },
  {
    id: 'doc-darf-01',
    numeroDocumento: 'GPS / DARF INSS',
    tipo: 'Guia de Recolhimento',
    credorRazaoSocial: 'Receita Federal do Brasil',
    valorBruto: 9295.0,
    dataRecebimento: 'Ontem, 16:40',
    origem: 'WHATSAPP',
    confiancaScore: 0.99,
    status: 'PRONTO_PARA_TRANSFEREGOV'
  }
];

const MOCK_PRAZOS: PrazoConvenioItem[] = [
  {
    id: 'pz-01',
    fase: 'Fase 05 (OBTV)',
    descricao: 'Liquidação da 3ª Medição e Emissão da OBTV vinculada',
    dataLimite: '18/10/2026',
    diasRestantes: 18,
    criticidade: 'CRITICO'
  },
  {
    id: 'pz-02',
    fase: 'Fase 06 (Aditivo)',
    descricao: 'Protocolo de Solicitação de Prorrogação de Vigência no SICONV',
    dataLimite: '30/10/2026',
    diasRestantes: 30,
    criticidade: 'ATENCAO'
  },
  {
    id: 'pz-03',
    fase: 'Fase 08 (RCO)',
    descricao: 'Prazo Fatal para Prestação de Contas Final (60 dias após vigência)',
    dataLimite: '15/12/2026',
    diasRestantes: 76,
    criticidade: 'REGULAR'
  }
];

@Component({
  selector: 'app-convenio-cockpit-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    PhaseStepperComponent,
    ConvenioKpisComponent,
    ConvenioDocumentsListComponent,
    ConvenioTimelineComponent
  ],
  template: `
    <div class="p-6 max-w-7xl mx-auto space-y-6 select-none font-sans">
      <!-- Topo: Identificação do Convênio e Seletor Rápido de Convênio -->
      <div class="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div>
          <!-- Barra de Contexto com Seletor Rápido de Convênio -->
          <div class="flex flex-wrap items-center gap-2 mb-2">
            <!-- Botão de Alternar Convênio (Dropdown / Switcher) -->
            <div class="relative">
              <button
                type="button"
                (click)="seletorConvenioAberto.set(!seletorConvenioAberto())"
                class="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/30 border border-gov-cobalt-500/40 text-xs font-mono font-bold text-gov-cobalt-300 transition-all focus:outline-none cursor-pointer"
                title="Clique para alternar entre os convênios da prefeitura"
              >
                <span>Convênio Transferegov #{{ convenio().numeroSiconv }}</span>
                <svg class="w-3.5 h-3.5 text-gov-cobalt-400 transition-transform" [class.rotate-180]="seletorConvenioAberto()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="m6 9 6 6 6-6"/>
                </svg>
              </button>

              <!-- Modal / Popover Dropdown de Seleção de Convênio -->
              @if (seletorConvenioAberto()) {
                <div class="absolute left-0 mt-2 w-80 sm:w-96 bg-[#111827] border border-white/10 rounded-xl shadow-2xl p-3 z-50 divide-y divide-white/5 space-y-2 animate-in fade-in duration-100">
                  <div class="flex items-center justify-between pb-2">
                    <span class="text-[10px] font-semibold uppercase tracking-wider text-gov-slate-400">
                      Convênios de {{ municipioCtx.nomeMunicipioAtivoFormatado() }}
                    </span>
                    <a
                      routerLink="/convenios/lista"
                      (click)="seletorConvenioAberto.set(false)"
                      class="text-[10px] text-gov-cobalt-400 hover:underline"
                    >
                      Ver todos
                    </a>
                  </div>

                  <!-- Busca Rápida de Convênios -->
                  <div class="pt-2">
                    <input
                      type="text"
                      [(ngModel)]="buscaConvenio"
                      placeholder="Filtrar por número ou objeto..."
                      class="w-full px-3 py-1.5 bg-white/5 border border-white/10 rounded-lg text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                    />
                  </div>

                  <!-- Lista de Convênios do Município -->
                  <div class="py-2 max-h-64 overflow-y-auto space-y-1">
                    @for (c of conveniosFiltrados(); track c.id) {
                      <button
                        type="button"
                        (click)="trocarConvenio(c.id)"
                        class="w-full text-left p-2.5 rounded-lg text-xs transition-colors block group"
                        [ngClass]="c.id === convenio().id ? 'bg-gov-cobalt-600/20 border border-gov-cobalt-500/30' : 'hover:bg-white/5 border border-transparent'"
                      >
                        <div class="flex items-center justify-between gap-1">
                          <span class="font-mono font-bold" [class.text-gov-cobalt-300]="c.id === convenio().id" [class.text-white]="c.id !== convenio().id">
                            #{{ c.numeroSiconv }}
                          </span>
                          <span class="text-[10px] font-medium px-1.5 py-0.5 rounded" [ngClass]="c.diasParaVencimento <= 30 ? 'bg-rose-500/20 text-rose-300' : 'bg-white/5 text-gov-slate-400'">
                            Fase 0{{ c.faseAtualNumero }} • {{ c.diasParaVencimento }}d
                          </span>
                        </div>
                        <div class="text-[11px] text-gov-slate-300 truncate mt-1">
                          {{ c.objeto }}
                        </div>
                      </button>
                    }
                  </div>
                </div>
              }
            </div>

            <span class="text-xs text-gov-slate-400">
              {{ convenio().orgaoConcedente }}
            </span>
          </div>

          <h2 class="text-xl sm:text-2xl font-bold text-white tracking-tight">
            {{ convenio().objeto }}
          </h2>

          <div class="text-xs text-gov-slate-400 mt-1 flex items-center gap-2">
            <span>Prefeitura Convenente:</span>
            <strong class="text-gov-slate-200">{{ municipioCtx.nomeMunicipioAtivoFormatado() }}</strong>
          </div>
        </div>

        <!-- Ações do Topo -->
        <div class="flex items-center gap-2 sm:gap-3 shrink-0">
          <a
            routerLink="/convenios/lista"
            class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-gov-slate-200 border border-white/10 transition-colors inline-flex items-center gap-2"
          >
            <svg class="w-3.5 h-3.5 text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="8" y1="6" x2="21" y2="6"/>
              <line x1="8" y1="12" x2="21" y2="12"/>
              <line x1="8" y1="18" x2="21" y2="18"/>
              <line x1="3" y1="6" x2="3.01" y2="6"/>
              <line x1="3" y1="12" x2="3.01" y2="12"/>
              <line x1="3" y1="18" x2="3.01" y2="18"/>
            </svg>
            <span>Lista de Convênios</span>
          </a>

          <button
            type="button"
            (click)="abrirModalFase(faseSelecionada())"
            class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-gov-slate-200 border border-white/10 transition-colors inline-flex items-center gap-2"
          >
            <svg class="w-3.5 h-3.5 text-gov-cobalt-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <span>Dossiê da Fase</span>
          </button>

          <a
            href="https://transferegov.sistema.gov.br"
            target="_blank"
            rel="noopener noreferrer"
            class="px-3.5 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-xs font-semibold text-white shadow-sm transition-colors inline-flex items-center gap-2"
          >
            <span>Transferegov</span>
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
              <polyline points="15 3 21 3 21 9"/>
              <line x1="10" y1="14" x2="21" y2="3"/>
            </svg>
          </a>
        </div>
      </div>

      <!-- 1. Stepper Linear das 10 Fases (Fases 0 a 9) -->
      <app-phase-stepper
        [fases]="convenio().fases"
        (faseSelecionada)="onFaseClick($event)"
      ></app-phase-stepper>

      <!-- 2. KPIs de Execução Física e Saldo Bancário Op 006 -->
      <app-convenio-kpis [convenio]="convenio()"></app-convenio-kpis>

      <!-- 3. Seção Dividida: Documentos do WhatsApp & Cronograma -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <app-convenio-documents-list [documentos]="documentos()"></app-convenio-documents-list>
        <app-convenio-timeline [prazos]="prazos()"></app-convenio-timeline>
      </div>

      <!-- Modal Contextual da Fase Selecionada -->
      @if (detalhesFaseAberta()) {
        <div class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div class="bg-[#111827] border border-white/10 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
              <div class="flex items-center gap-2">
                <span class="text-xs font-mono font-semibold px-2 py-0.5 rounded bg-gov-cobalt-500/20 text-gov-cobalt-400 border border-gov-cobalt-500/30">
                  {{ faseSelecionada().codigo }}
                </span>
                <h3 class="text-sm font-bold text-white">{{ faseSelecionada().nome }}</h3>
              </div>
              <button (click)="detalhesFaseAberta.set(false)" class="text-gov-slate-400 hover:text-white cursor-pointer">✕</button>
            </div>

            <div class="text-xs text-gov-slate-300 leading-relaxed">
              {{ faseSelecionada().descricao }}
            </div>

            <div class="p-3 rounded-lg bg-white/5 border border-white/5 space-y-2 text-xs">
              <div class="flex justify-between">
                <span class="text-gov-slate-400">Status Operacional:</span>
                <strong class="text-emerald-400">{{ faseSelecionada().status }}</strong>
              </div>
              <div class="flex justify-between">
                <span class="text-gov-slate-400">Responsável:</span>
                <span class="text-gov-slate-200">Consultoria / Fiscal Municipal</span>
              </div>
            </div>

            <div class="text-right pt-2">
              <button
                type="button"
                (click)="detalhesFaseAberta.set(false)"
                class="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/15 text-xs font-medium text-white transition-colors cursor-pointer"
              >
                Fechar
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class ConvenioCockpitPageComponent implements OnInit {
  readonly municipioCtx = inject(MunicipioContextService);
  readonly convenioCtx = inject(ConvenioContextService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly seletorConvenioAberto = signal(false);
  public buscaConvenio = '';

  readonly convenio = computed(() => this.convenioCtx.convenioAtivo());
  readonly documentos = signal<DocumentoConvenioResumo[]>(MOCK_DOCS);
  readonly prazos = signal<PrazoConvenioItem[]>(MOCK_PRAZOS);

  readonly faseSelecionada = signal<FaseConvenio>(this.convenio().fases[this.convenio().faseAtualNumero] || this.convenio().fases[0]);
  readonly detalhesFaseAberta = signal(false);

  readonly conveniosFiltrados = computed(() => {
    const lista = this.convenioCtx.conveniosDoMunicipio();
    const busca = this.buscaConvenio.toLowerCase().trim();
    if (!busca) return lista;
    return lista.filter(c =>
      c.numeroSiconv.toLowerCase().includes(busca) ||
      c.objeto.toLowerCase().includes(busca)
    );
  });

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.convenioCtx.selecionarConvenio(id);
        const atual = this.convenioCtx.convenioAtivo();
        if (atual && atual.fases) {
          this.faseSelecionada.set(atual.fases[atual.faseAtualNumero] || atual.fases[0]);
        }
      }
    });
  }

  trocarConvenio(id: string): void {
    this.convenioCtx.selecionarConvenio(id);
    this.seletorConvenioAberto.set(false);
    this.router.navigate(['/convenios', id]);
  }

  onFaseClick(fase: FaseConvenio): void {
    this.faseSelecionada.set(fase);
    this.detalhesFaseAberta.set(true);
  }

  abrirModalFase(fase: FaseConvenio): void {
    this.faseSelecionada.set(fase);
    this.detalhesFaseAberta.set(true);
  }
}
