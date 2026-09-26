import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MunicipioRiscoCauc, CertidaoCaucItem } from '../model/cauc.model';
import { CaucHealthMatrixComponent } from '../components/cauc-health-matrix/cauc-health-matrix.component';
import { RadarCaucService } from '../services/radar-cauc.service';

/**
 * =========================================================================
 * 📌 MAPEAMENTO DE PLACEHOLDER - TASK-FE-08 (Radar CAUC & Matriz de Risco)
 * -------------------------------------------------------------------------
 * O QUE É MOCK:
 * - A lista de municípios e status das 16 certidões do CAUC e notificações
 *   SELIC de 45 dias (Súmula 230/TCU) geradas localmente.
 *
 * O QUE SUBSTITUI NO FUTURO:
 * - Task-12: Radar CAUC / tb_certidoes_cauc (Flyway V5) do Core Service.
 * - Task-19: Passivo, Notificações SELIC e Blindagem Súmula 230/TCU (Flyway V13).
 * - Task-10: Rotina matinal de avaliação de prazos de vigência.
 * =========================================================================
 */
const MOCK_MUNICIPIOS_RISCO: MunicipioRiscoCauc[] = [
  {
    id: 'mun-patos-01',
    nome: 'Patos',
    uf: 'PB',
    conveniosAtivos: 5,
    certidoesRegulares: 15,
    certidoesAlerta: 1,
    certidoesVencidas: 0,
    proximoPrazoFatal: {
      descricao: 'Cláusula Suspensiva (Caixa)',
      diasRestantes: 11,
      dataLimite: '05/10/2026',
      tipo: 'SUSPENSIVA'
    },
    certidoes: [
      { codigo: '1.1', grupo: 'TRIBUTOS_FGTS', nome: 'Certidão PGFN / Receita Federal', orgaoEmissor: 'RFB', status: 'REGULAR', dataValidade: '28/11/2026', diasParaVencer: 65 },
      { codigo: '1.2', grupo: 'TRIBUTOS_FGTS', nome: 'Regularidade do FGTS (CRF)', orgaoEmissor: 'Caixa', status: 'ALERTA', dataValidade: '04/10/2026', diasParaVencer: 10 },
      { codigo: '1.4', grupo: 'TRIBUTOS_FGTS', nome: 'Certidão Negativa Trabalhista (CNDT)', orgaoEmissor: 'TST', status: 'REGULAR', dataValidade: '15/12/2026', diasParaVencer: 82 },
      { codigo: '2.1', grupo: 'PRESTACAO_CONTAS', nome: 'Prestação de Contas Convênios', orgaoEmissor: 'SICONV', status: 'REGULAR', dataValidade: 'Vigente', diasParaVencer: 120 },
      { codigo: '3.1', grupo: 'SICONFI_FISCAL', nome: 'Homologação RREO / RGF (SICONFI)', orgaoEmissor: 'STN', status: 'REGULAR', dataValidade: '30/10/2026', diasParaVencer: 36 },
      { codigo: '4.1', grupo: 'LIMITES_CONSTITUCIONAIS', nome: 'Aplicação Mínima Saúde (15%)', orgaoEmissor: 'SIOPS', status: 'REGULAR', dataValidade: 'Vigente', diasParaVencer: 90 }
    ]
  },
  {
    id: 'mun-sousa-02',
    nome: 'Sousa',
    uf: 'PB',
    conveniosAtivos: 3,
    certidoesRegulares: 16,
    certidoesAlerta: 0,
    certidoesVencidas: 0,
    proximoPrazoFatal: {
      descricao: 'Prestação de Contas RCO',
      diasRestantes: 22,
      dataLimite: '16/10/2026',
      tipo: 'RCO'
    },
    certidoes: [
      { codigo: '1.1', grupo: 'TRIBUTOS_FGTS', nome: 'Certidão PGFN / Receita Federal', orgaoEmissor: 'RFB', status: 'REGULAR', dataValidade: '10/12/2026', diasParaVencer: 77 },
      { codigo: '1.2', grupo: 'TRIBUTOS_FGTS', nome: 'Regularidade do FGTS (CRF)', orgaoEmissor: 'Caixa', status: 'REGULAR', dataValidade: '20/11/2026', diasParaVencer: 57 }
    ]
  },
  {
    id: 'mun-pombal-03',
    nome: 'Pombal',
    uf: 'PB',
    conveniosAtivos: 3,
    certidoesRegulares: 14,
    certidoesAlerta: 1,
    certidoesVencidas: 1,
    proximoPrazoFatal: {
      descricao: 'Notificação SELIC (45 dias)',
      diasRestantes: 42,
      dataLimite: '05/11/2026',
      tipo: 'SELIC'
    },
    certidoes: [
      { codigo: '1.1', grupo: 'TRIBUTOS_FGTS', nome: 'Certidão PGFN / Receita Federal', orgaoEmissor: 'RFB', status: 'REGULAR', dataValidade: '20/11/2026', diasParaVencer: 57 },
      { codigo: '3.2', grupo: 'SICONFI_FISCAL', nome: 'RGF 2º Quadrimestre (SICONFI)', orgaoEmissor: 'STN', status: 'VENCIDA', dataValidade: '20/09/2026', diasParaVencer: -4 }
    ]
  },
  {
    id: 'mun-monteiro-04',
    nome: 'Monteiro',
    uf: 'PB',
    conveniosAtivos: 3,
    certidoesRegulares: 15,
    certidoesAlerta: 1,
    certidoesVencidas: 0,
    proximoPrazoFatal: {
      descricao: 'Termo Aditivo Prorrogação',
      diasRestantes: 55,
      dataLimite: '18/11/2026',
      tipo: 'ADITIVO'
    },
    certidoes: []
  },
  {
    id: 'mun-cajazeiras-05',
    nome: 'Cajazeiras',
    uf: 'PB',
    conveniosAtivos: 3,
    certidoesRegulares: 16,
    certidoesAlerta: 0,
    certidoesVencidas: 0,
    proximoPrazoFatal: {
      descricao: 'Relatório Final de Objeto',
      diasRestantes: 78,
      dataLimite: '11/12/2026',
      tipo: 'RCO'
    },
    certidoes: []
  }
];

@Component({
  selector: 'app-radar-cauc-page',
  standalone: true,
  imports: [CommonModule, CaucHealthMatrixComponent],
  template: `
    <div class="p-6 max-w-7xl mx-auto space-y-6">
      <!-- Cabeçalho da Página -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <h2 class="text-xl font-bold text-white tracking-tight">
              Radar de Prazos & Regularidade CAUC (LRF Art. 25)
            </h2>
            <span class="inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/25">
              Live Sync STN / SICONV
            </span>
          </div>
          <p class="text-xs text-gov-slate-400 mt-0.5">
            Monitoramento preventivo das 16 certidões fiscais municipais e contagem regressiva de prazos fatais
          </p>
        </div>

        <div class="flex items-center gap-3">
          <button
            type="button"
            (click)="atualizar()"
            class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-white border border-white/10 transition-colors inline-flex items-center gap-2"
          >
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
              <path d="M3 3v5h5"/>
              <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
              <path d="M16 21h5v-5"/>
            </svg>
            <span>Atualizar Certidões</span>
          </button>
        </div>
      </div>

      <!-- 4 Cards Superiores de Risco Consolidado -->
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-medium text-gov-slate-400 uppercase tracking-wider">
            Municípios Gerenciados
          </div>
          <div class="text-2xl font-bold font-mono text-white mt-1">14</div>
          <div class="text-[11px] text-gov-slate-400 mt-1">Sertão e Agreste Paraibano</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-medium text-gov-slate-400 uppercase tracking-wider">
            Prazos Críticos (< 15 dias)
          </div>
          <div class="text-2xl font-bold font-mono text-rose-400 mt-1">3</div>
          <div class="text-[11px] text-rose-400/80 mt-1">Risco iminente de perda de verba</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-medium text-gov-slate-400 uppercase tracking-wider">
            Certidões em Alerta
          </div>
          <div class="text-2xl font-bold font-mono text-amber-400 mt-1">2</div>
          <div class="text-[11px] text-amber-400/80 mt-1">Vencimento em menos de 10 dias</div>
        </div>

        <div class="bg-[#111827] border border-white/10 rounded-xl p-4 shadow-sm">
          <div class="text-[11px] font-medium text-gov-slate-400 uppercase tracking-wider">
            Verbas sob Gestão
          </div>
          <div class="text-2xl font-bold font-mono text-gov-slate-100 mt-1">R$ 14.8M</div>
          <div class="text-[11px] text-emerald-400 mt-1">42 convênios federais ativos</div>
        </div>
      </div>

      <!-- Tabela Matriz de Risco Municipal -->
      <div class="bg-[#111827] border border-white/10 rounded-xl overflow-hidden shadow-sm">
        <table class="w-full text-left text-xs text-gov-slate-300">
          <thead class="bg-[#0A0E17] text-gov-slate-400 uppercase tracking-wider font-semibold border-b border-white/10 text-[10px]">
            <tr>
              <th class="py-3 px-5">Município</th>
              <th class="py-3 px-5">Saúde Fiscal CAUC (16 Itens: CNDT, FGTS, RFB, SICONFI)</th>
              <th class="py-3 px-5">Convênios Ativos</th>
              <th class="py-3 px-5">Próximo Prazo Fatal</th>
              <th class="py-3 px-5 text-right">Ação</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-white/5">
            @for (item of municipios(); track item.id) {
              <tr class="hover:bg-white/[0.02] transition-colors">
                <!-- Município -->
                <td class="py-3.5 px-5">
                  <div class="font-semibold text-white text-xs">
                    Prefeitura de {{ item.nome }}
                  </div>
                  <div class="text-[10px] text-gov-slate-500 font-mono mt-0.5">
                    UF: {{ item.uf }}
                  </div>
                </td>

                <!-- Matriz de Saúde CAUC (16 Pontos) -->
                <td class="py-3.5 px-5">
                  <app-cauc-health-matrix [certidoes]="item.certidoes"></app-cauc-health-matrix>
                </td>

                <!-- Convênios Ativos -->
                <td class="py-3.5 px-5 font-mono text-gov-slate-200">
                  <span class="inline-flex items-center justify-center w-6 h-6 rounded-full bg-white/5 font-semibold text-xs">
                    {{ item.conveniosAtivos }}
                  </span>
                </td>

                <!-- Próximo Prazo Fatal -->
                <td class="py-3.5 px-5">
                  <div class="flex items-center gap-2">
                    @if (item.proximoPrazoFatal.diasRestantes <= 15) {
                      <span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-500/15 text-rose-400 border border-rose-500/30">
                        D-{{ item.proximoPrazoFatal.diasRestantes }} dias: {{ item.proximoPrazoFatal.descricao }}
                      </span>
                    } @else if (item.proximoPrazoFatal.diasRestantes <= 30) {
                      <span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-500/15 text-amber-400 border border-amber-500/30">
                        D-{{ item.proximoPrazoFatal.diasRestantes }} dias: {{ item.proximoPrazoFatal.descricao }}
                      </span>
                    } @else {
                      <span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-white/5 text-gov-slate-300 border border-white/10">
                        D-{{ item.proximoPrazoFatal.diasRestantes }} dias: {{ item.proximoPrazoFatal.descricao }}
                      </span>
                    }
                  </div>
                </td>

                <!-- Ação -->
                <td class="py-3.5 px-5 text-right">
                  <button
                    type="button"
                    (click)="abrirDossie(item)"
                    class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-gov-slate-200 transition-colors"
                  >
                    Ver Dossiê
                  </button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      </div>

      <!-- Modal de Dossiê CAUC & Blindagem Jurídica (Súmula 230/TCU) -->
      @if (dossieAberto()) {
        <div class="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div class="bg-[#111827] border border-white/10 rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-4">
            <div class="flex items-center justify-between pb-3 border-b border-white/10">
              <div>
                <h3 class="text-sm font-bold text-white">
                  Dossiê CAUC & Regularidade — Prefeitura de {{ municipioSelecionado()?.nome }}
                </h3>
                <p class="text-[11px] text-gov-slate-400 mt-0.5">
                  Extrato das 16 certidões oficiais da Secretaria do Tesouro Nacional (LRF Art. 25)
                </p>
              </div>
              <button (click)="dossieAberto.set(false)" class="text-gov-slate-400 hover:text-white">✕</button>
            </div>

            <!-- Lista de Certidões do Dossiê -->
            <div class="space-y-2 max-h-64 overflow-y-auto pr-1">
              @for (cert of municipioSelecionado()?.certidoes; track cert.codigo) {
                <div class="p-2.5 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-xs">
                  <div>
                    <div class="font-medium text-white flex items-center gap-1.5">
                      <span class="font-mono text-gov-slate-400">[{{ cert.codigo }}]</span>
                      <span>{{ cert.nome }}</span>
                    </div>
                    <div class="text-[11px] text-gov-slate-400">
                      Órgão: {{ cert.orgaoEmissor }} | Validade: {{ cert.dataValidade }}
                    </div>
                  </div>
                  <span
                    class="px-2 py-0.5 rounded text-[10px] font-semibold"
                    [ngClass]="{
                      'bg-emerald-500/15 text-emerald-400 border border-emerald-500/25': cert.status === 'REGULAR',
                      'bg-amber-500/15 text-amber-400 border border-amber-500/25': cert.status === 'ALERTA',
                      'bg-rose-500/15 text-rose-400 border border-rose-500/25': cert.status === 'VENCIDA'
                    }"
                  >
                    {{ cert.status }}
                  </span>
                </div>
              } @empty {
                <div class="py-6 text-center text-xs text-gov-slate-500">
                  Todas as 16 certidões do CAUC encontram-se em situação regular.
                </div>
              }
            </div>

            <!-- Card de Blindagem Jurídica (Súmula 230/TCU / Task 19) -->
            <div class="p-3.5 rounded-xl bg-gov-cobalt-500/10 border border-gov-cobalt-500/20 text-xs space-y-1.5">
              <div class="font-semibold text-gov-cobalt-300 flex items-center gap-1.5">
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                <span>Blindagem Jurídica Ativa (Súmula 230 / TCU & Art. 26-A Lei 10.522)</span>
              </div>
              <p class="text-[11px] text-gov-slate-300 leading-relaxed">
                Caso haja omissão de ex-gestor, o GovFlow gera a minuta de representação ao Ministério Público para suspensão cautelar de restrições no CAUC/SIAFI sem perda dos repasses federais.
              </p>
            </div>

            <div class="text-right pt-2">
              <button
                type="button"
                (click)="dossieAberto.set(false)"
                class="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/15 text-xs font-semibold text-white transition-colors"
              >
                Concluir Visualização
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `
})
export class RadarCaucPageComponent implements OnInit {
  private readonly caucService = inject(RadarCaucService);

  readonly municipios = signal<MunicipioRiscoCauc[]>(MOCK_MUNICIPIOS_RISCO);
  readonly carregando = signal(false);
  readonly sincronizando = signal(false);

  readonly dossieAberto = signal(false);
  readonly municipioSelecionado = signal<MunicipioRiscoCauc | null>(null);

  ngOnInit(): void {
    this.carregarDados();
  }

  carregarDados(): void {
    this.carregando.set(true);
    this.caucService.obterResumo().subscribe({
      next: (res) => {
        if (res && res.municipios && res.municipios.length > 0) {
          this.municipios.set(res.municipios);
        }
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
      }
    });
  }

  atualizar(): void {
    this.sincronizando.set(true);
    this.caucService.reavaliarConformidade().subscribe({
      next: () => {
        this.carregarDados();
        this.sincronizando.set(false);
      },
      error: () => {
        this.sincronizando.set(false);
      }
    });
  }

  abrirDossie(mun: MunicipioRiscoCauc): void {
    this.municipioSelecionado.set(mun);
    this.dossieAberto.set(true);
  }
}
