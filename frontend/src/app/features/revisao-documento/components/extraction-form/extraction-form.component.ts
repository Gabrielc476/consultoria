import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { AuditChecklistComponent } from '../audit-checklist/audit-checklist.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../../shared/pipes/cnpj.pipe';
import { validarCnpj, sugerirCnpjCorreto } from '../../../../shared/utils/cnpj-validator';

@Component({
  selector: 'app-extraction-form',
  standalone: true,
  host: {
    'class': 'flex flex-col flex-1 h-full min-h-0 w-full overflow-hidden'
  },
  imports: [
    CommonModule,
    FormsModule,
    AuditChecklistComponent,
    StatusPillComponent,
    CurrencyBrlPipe,
    CnpjPipe
  ],
  template: `
    <div class="flex-1 h-full min-h-0 w-full flex flex-col bg-gov-slate-900 border-l border-gov-slate-800 text-gov-slate-100 overflow-hidden">
      <!-- Cabeçalho do formulário com Segmented Control de visualização -->
      <div class="flex-shrink-0 px-6 py-3 border-b border-gov-slate-800 flex items-center justify-between bg-gov-slate-900/95 backdrop-blur z-20">
        <div>
          <span class="text-[11px] uppercase tracking-wider text-gov-slate-400 font-semibold">Conferência Fiscal Human-in-the-Loop</span>
          <h2 class="text-base font-bold text-white flex items-center gap-2">
            Documento Hábil
            <app-status-pill [status]="state.documentoAtual()?.status"></app-status-pill>
          </h2>
        </div>

        <!-- Segmented Control de visualização: Alternância intuitiva sem ambiguidade -->
        <div class="bg-gov-slate-950 p-1 rounded-xl border border-gov-slate-800 flex items-center gap-1 shadow-inner">
          <button
            type="button"
            (click)="setModoEdicao(false)"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer"
            [ngClass]="!modoEdicaoGeral() 
              ? 'bg-gov-cobalt-600 text-white shadow-sm font-semibold' 
              : 'text-gov-slate-400 hover:text-gov-slate-200 hover:bg-gov-slate-800/60'"
            title="Modo Leitura Limpa: Focado em validação visual rápida dos dados extraídos pela IA"
          >
            <span>👁️</span>
            <span>Auditoria Rápida</span>
          </button>

          <button
            type="button"
            (click)="setModoEdicao(true)"
            class="px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 cursor-pointer"
            [ngClass]="modoEdicaoGeral() 
              ? 'bg-amber-600 text-white shadow-sm font-semibold' 
              : 'text-gov-slate-400 hover:text-gov-slate-200 hover:bg-gov-slate-800/60'"
            title="Modo Edição Completa: Abre todos os campos fiscais para correção manual direta"
          >
            <span>✏️</span>
            <span>Editar Campos</span>
          </button>
        </div>
      </div>

      <!-- Corpo rolável do painel de auditoria -->
      <div class="flex-1 min-h-0 overflow-y-auto p-5 space-y-4">

        <!-- 1. CHECKLIST EXECUTIVO DE 3 SEGUNDOS NO TOPO -->
        <app-audit-checklist></app-audit-checklist>

        <!-- 2. IDENTIFICAÇÃO DO DOCUMENTO (DISPLAY-FIRST OU FORMULÁRIO) -->
        <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 transition-all">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
              <span>📄</span> Identificação Fiscal
            </h3>

            @if (modoEdicaoGeral()) {
              <span class="text-[11px] text-amber-400/90 font-medium flex items-center gap-1">
                <span>●</span> Edição geral
              </span>
            } @else {
              <button
                type="button"
                (click)="editarIdentificacao.set(!editarIdentificacao())"
                class="text-xs text-gov-slate-400 hover:text-gov-cobalt-300 transition-colors flex items-center gap-1 cursor-pointer"
              >
                <span>{{ editarIdentificacao() ? '✕ Fechar Ajuste' : '✏️ Ajustar' }}</span>
              </button>
            }
          </div>

          <!-- VISÃO DISPLAY-FIRST: LEITURA ELEGANTE E SEM RUÍDO -->
          @if (!editarIdentificacao() && !modoEdicaoGeral()) {
            <div class="grid grid-cols-2 gap-4 text-xs">
              <div class="space-y-1">
                <span class="text-gov-slate-400 block text-[11px]">Tipo e Número:</span>
                <div class="font-semibold text-white text-sm flex items-center gap-2">
                  <span>{{ rotuloTipoDocumento(state.formulario().tipoDocumento) }}</span>
                  <span class="font-mono bg-gov-slate-800 px-2 py-0.5 rounded border border-gov-slate-700 text-gov-cobalt-300">
                    Nº {{ state.formulario().numeroDocumento || 'Sem número' }}
                  </span>
                </div>
              </div>

              <div class="space-y-1">
                <span class="text-gov-slate-400 block text-[11px]">Data de Emissão:</span>
                <div class="font-mono font-medium text-gov-slate-200 text-sm">
                  {{ state.formulario().dataEmissao || 'Não informada' }}
                </div>
              </div>

              <div class="col-span-2 pt-2 border-t border-gov-slate-800/80 grid grid-cols-3 gap-4">
                <div class="col-span-2 space-y-1">
                  <span class="text-gov-slate-400 block text-[11px]">Credor / Prestador de Serviço:</span>
                  <div class="font-semibold text-white text-sm truncate" [title]="state.formulario().razaoSocialCredor">
                    {{ state.formulario().razaoSocialCredor || 'Razão social não informada' }}
                  </div>
                  <div class="font-mono text-xs mt-0.5 flex items-center gap-2">
                    <span class="text-gov-slate-400">CNPJ:</span>
                    <span [ngClass]="cnpjInvalido() ? 'text-rose-400 font-bold bg-rose-950/60 px-1.5 py-0.5 rounded border border-rose-800' : 'text-white'">
                      {{ state.formulario().cnpjCredor ? (state.formulario().cnpjCredor | cnpj) : 'Pendente' }}
                    </span>
                    @if (cnpjInvalido() && sugestaoCnpj()) {
                      <button
                        type="button"
                        (click)="state.atualizarCampo('cnpjCredor', sugestaoCnpj()!)"
                        class="text-[10px] px-2 py-0.5 rounded bg-amber-600 hover:bg-amber-500 text-white font-semibold flex items-center gap-1 cursor-pointer transition-colors"
                        title="Corrigir Dígitos Verificadores do CNPJ"
                      >
                        <span>⚡</span> Corrigir
                      </button>
                    }
                  </div>
                </div>

                <div class="space-y-1">
                  <span class="text-gov-slate-400 block text-[11px]">Nota de Empenho:</span>
                  <div class="font-mono font-semibold text-gov-cobalt-300 text-xs">
                    {{ state.formulario().numeroEmpenho || '2026NE00018' }}
                  </div>
                  <span class="inline-block text-[10px] px-1.5 py-0.2 rounded bg-gov-cobalt-950 text-gov-cobalt-300 border border-gov-cobalt-800">
                    Sugerido pelo Convênio
                  </span>
                </div>
              </div>
            </div>
          } @else {
            <!-- MODO FORMULÁRIO EDITÁVEL -->
            <div class="grid grid-cols-2 gap-3 pt-1">
              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Tipo de Documento</label>
                <select
                  id="input-tipoDocumento"
                  [ngModel]="state.formulario().tipoDocumento"
                  (ngModelChange)="state.atualizarCampo('tipoDocumento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                >
                  <option value="NOTA_FISCAL_SERVICOS">Nota Fiscal de Serviços (NFS-e)</option>
                  <option value="NOTA_FISCAL_MERCADORIA">Nota Fiscal Eletrônica (NF-e)</option>
                  <option value="RECIBO_PAGAMENTO">Recibo Legal de Pagamento</option>
                  <option value="MEDICAO_OBRAS">Boletim de Medição de Obras</option>
                </select>
              </div>

              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Número da NF / Recibo</label>
                <input
                  id="input-numero"
                  type="text"
                  [ngModel]="state.formulario().numeroDocumento"
                  (ngModelChange)="state.atualizarCampo('numeroDocumento', $event)"
                  class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                />
              </div>

              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Data de Emissão</label>
                <input
                  id="input-data"
                  type="date"
                  [ngModel]="state.formulario().dataEmissao"
                  (ngModelChange)="state.atualizarCampo('dataEmissao', $event)"
                  class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                />
              </div>

              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Nota de Empenho</label>
                <input
                  id="input-empenho"
                  type="text"
                  [ngModel]="state.formulario().numeroEmpenho"
                  (ngModelChange)="state.atualizarCampo('numeroEmpenho', $event)"
                  class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                />
              </div>

              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">CNPJ do Credor</label>
                <input
                  id="input-cnpj"
                  type="text"
                  [ngModel]="state.formulario().cnpjCredor"
                  (ngModelChange)="state.atualizarCampo('cnpjCredor', $event)"
                  class="w-full font-mono bg-gov-slate-900 border rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                  [ngClass]="cnpjInvalido() ? 'border-rose-500 text-rose-200 ring-1 ring-rose-500/40' : 'border-gov-slate-700'"
                />
                @if (cnpjInvalido()) {
                  <div class="flex items-center justify-between text-[10px] text-rose-400 mt-1 font-mono">
                    <span>DV inválido</span>
                    @if (sugestaoCnpj()) {
                      <button
                        type="button"
                        (click)="state.atualizarCampo('cnpjCredor', sugestaoCnpj()!)"
                        class="text-amber-400 hover:text-amber-300 font-semibold underline cursor-pointer"
                      >
                        ⚡ Ajustar p/ {{ sugestaoCnpj() }}
                      </button>
                    }
                  </div>
                }
              </div>

              <div>
                <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Razão Social</label>
                <input
                  id="input-razao"
                  type="text"
                  [ngModel]="state.formulario().razaoSocialCredor"
                  (ngModelChange)="state.atualizarCampo('razaoSocialCredor', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-gov-slate-100 focus:border-gov-cobalt-500"
                />
              </div>
            </div>
          }
        </div>

        <!-- 3. CONCILIAÇÃO FINANCEIRA (VALORES & RETENÇÕES) -->
        <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 transition-all">
          <div class="flex items-center justify-between mb-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
              <span>💰</span> Valores & Conciliação Tributária
            </h3>

            <div class="flex items-center gap-2">
              @if (!state.possuiDivergencia()) {
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-medium bg-emerald-950/60 text-emerald-300 border border-emerald-600/40">
                  Consistência OK
                </span>
              }

              @if (modoEdicaoGeral()) {
                <span class="text-[11px] text-amber-400/90 font-medium flex items-center gap-1">
                  <span>●</span> Edição geral
                </span>
              } @else {
                <button
                  type="button"
                  (click)="editarValores.set(!editarValores())"
                  class="text-xs text-gov-slate-400 hover:text-gov-cobalt-300 transition-colors flex items-center gap-1 cursor-pointer"
                >
                  <span>{{ editarValores() ? '✕ Fechar Ajuste' : '✏️ Ajustar Valores' }}</span>
                </button>
              }
            </div>
          </div>

          <!-- CARDS DE VALORES COM TIPOGRAFIA LIMPA -->
          <div class="grid grid-cols-3 gap-3 mb-3">
            <!-- Valor Bruto -->
            <div class="bg-gov-slate-900/90 p-3 rounded-lg border border-gov-slate-700/80">
              <span class="text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider block mb-1">Valor Bruto</span>
              <div class="font-mono font-bold text-base text-white">
                {{ state.formulario().valorBruto | currencyBrl }}
              </div>
            </div>

            <!-- Deduções -->
            <div class="bg-gov-slate-900/90 p-3 rounded-lg border border-gov-slate-700/80">
              <span class="text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider block mb-1">Total Retenções (-)</span>
              <div class="font-mono font-bold text-base text-amber-400">
                {{ state.valorTotalDeducoes() | currencyBrl }}
              </div>
            </div>

            <!-- Líquido -->
            <div
              class="bg-gov-slate-900/90 p-3 rounded-lg border transition-all"
              [ngClass]="state.possuiDivergencia() ? 'border-rose-500/80 bg-rose-950/20' : 'border-emerald-500/40 bg-emerald-950/10'"
            >
              <span class="text-[10px] font-semibold text-gov-slate-300 uppercase tracking-wider block mb-1">Valor Líquido</span>
              <div class="font-mono font-bold text-base" [ngClass]="state.possuiDivergencia() ? 'text-rose-400' : 'text-emerald-400'">
                {{ state.formulario().valorLiquido | currencyBrl }}
              </div>
            </div>
          </div>

          <!-- DETALHAMENTO DE TRIBUTOS EM MODO LEITURA (CHIPS ELEGANTES) -->
          @if (!editarValores() && !modoEdicaoGeral()) {
            <div class="pt-2 border-t border-gov-slate-800/80">
              <span class="text-[11px] font-medium text-gov-slate-400 uppercase tracking-wider block mb-2">Tributos Retidos na Fonte</span>
              <div class="flex flex-wrap gap-2">
                @for (ret of state.formulario().retencoes; track $index) {
                  <div class="inline-flex items-center gap-2 px-3 py-1.5 rounded-lg bg-gov-slate-900 border border-gov-slate-700/80 text-xs">
                    <span class="font-medium text-gov-slate-300">{{ ret.tipoTributo }}</span>
                    <span class="font-mono font-bold text-amber-400">{{ ret.valorRetido | currencyBrl }}</span>
                  </div>
                }
                @if (state.formulario().retencoes.length === 0) {
                  <span class="text-xs text-gov-slate-500 italic">Sem deduções fiscais aplicáveis.</span>
                }
              </div>
            </div>
          } @else {
            <!-- MODO EDIÇÃO DE VALORES E ADIÇÃO/REMOÇÃO DE TRIBUTOS -->
            <div class="space-y-3 pt-2 border-t border-gov-slate-800">
              <div class="grid grid-cols-2 gap-3">
                <div>
                  <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Editar Valor Bruto (R$)</label>
                  <input
                    id="input-valorBruto"
                    type="number"
                    step="0.01"
                    [ngModel]="state.formulario().valorBruto"
                    (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                    class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white"
                  />
                </div>
                <div>
                  <label class="block text-[11px] font-medium text-gov-slate-300 mb-1">Editar Valor Líquido (R$)</label>
                  <input
                    id="input-valorLiquido"
                    type="number"
                    step="0.01"
                    [ngModel]="state.formulario().valorLiquido"
                    (ngModelChange)="state.atualizarCampo('valorLiquido', $event)"
                    class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-2.5 py-1.5 text-xs text-white"
                  />
                </div>
              </div>

              <div class="pt-2">
                <div class="flex items-center justify-between mb-2">
                  <span class="text-[11px] font-medium text-gov-slate-300 uppercase">Detalhamento dos Tributos</span>
                  <button
                    type="button"
                    (click)="state.adicionarRetencao()"
                    class="text-xs text-gov-cobalt-400 hover:text-gov-cobalt-300 font-semibold flex items-center gap-1"
                  >
                    <span>+</span> Adicionar Tributo
                  </button>
                </div>

                <div class="space-y-2">
                  @for (ret of state.formulario().retencoes; track $index) {
                    <div class="flex items-center gap-2 p-2 rounded-lg bg-gov-slate-900 border border-gov-slate-800 text-xs">
                      <select
                        [ngModel]="ret.tipoTributo"
                        (ngModelChange)="onTipoRetencaoChange($index, $event)"
                        class="w-1/3 bg-gov-slate-800 border border-gov-slate-700 rounded px-2 py-1 text-gov-slate-200"
                      >
                        <option value="INSS">INSS (11%)</option>
                        <option value="ISS">ISS Municipal</option>
                        <option value="IRRF">IRRF</option>
                        <option value="PIS">PIS</option>
                        <option value="COFINS">COFINS</option>
                        <option value="CSLL">CSLL</option>
                        <option value="OUTROS">Outros</option>
                      </select>

                      <div class="flex-1 relative flex items-center">
                        <span class="absolute left-2 font-mono text-gov-slate-400">R$</span>
                        <input
                          type="number"
                          step="0.01"
                          [ngModel]="ret.valorRetido"
                          (ngModelChange)="onValorRetencaoChange($index, $event)"
                          class="w-full bg-gov-slate-800 border border-gov-slate-700 rounded pl-7 pr-2 py-1 font-mono text-white"
                        />
                      </div>

                      <button
                        type="button"
                        (click)="state.removerRetencao($index)"
                        class="text-gov-slate-400 hover:text-rose-400 p-1"
                      >
                        ✕
                      </button>
                    </div>
                  }
                </div>
              </div>
            </div>
          }
        </div>

        <!-- 4. AUTOMAÇÃO E MEMORIZAÇÃO DE REGRAS -->
        <div class="flex items-center gap-2 p-3 rounded-lg bg-gov-slate-800/30 border border-gov-slate-800 text-xs text-gov-slate-300">
          <input
            id="chk-memorizar"
            type="checkbox"
            [ngModel]="state.formulario().memorizarRegraFornecedor"
            (ngModelChange)="state.atualizarCampo('memorizarRegraFornecedor', $event)"
            class="rounded bg-gov-slate-900 border-gov-slate-700 text-gov-cobalt-600 focus:ring-0 w-4 h-4 cursor-pointer"
          />
          <label for="chk-memorizar" class="cursor-pointer select-none">
            Memorizar retenções tributárias deste fornecedor para próximas notas fiscais
          </label>
        </div>
      </div>

      <!-- BARRA DE AÇÕES FIXA NO RODAPÉ (GARANTE VISIBILIDADE 100% SEM SCROLL) -->
      <div class="flex-shrink-0 px-6 py-4 bg-gov-slate-900 border-t border-gov-slate-800 flex items-center justify-between gap-4 z-20 shadow-2xl">
        <button
          type="button"
          (click)="state.abrirModalRejeicao()"
          [disabled]="state.salvando()"
          class="px-4 py-2.5 rounded-lg border border-rose-700/60 bg-rose-950/40 hover:bg-rose-900/60 text-rose-200 font-semibold text-xs tracking-wide transition-colors flex items-center gap-2"
          title="Atalho: Alt + R"
        >
          <span>✕</span> Rejeitar Documento
          <kbd class="ml-1 px-1.5 py-0.5 rounded bg-rose-900/80 text-[10px] text-rose-300 font-mono">Alt+R</kbd>
        </button>

        <button
          type="button"
          (click)="state.aprovar()"
          [disabled]="state.salvando()"
          class="flex-1 px-6 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm tracking-wide transition-all shadow-lg shadow-emerald-950 flex items-center justify-center gap-2 disabled:opacity-50 group cursor-pointer"
          title="Atalho: Espaço ou Ctrl + Enter"
        >
          @if (state.salvando()) {
            <span class="animate-spin text-base">⟳</span>
            <span>Salvando e avançando...</span>
          } @else {
            <span class="group-hover:scale-110 transition-transform">⚡</span>
            <span>Aprovar & Próximo</span>
            <span class="px-2 py-0.5 rounded bg-emerald-700/80 text-emerald-100 text-xs font-mono font-normal">
              {{ state.totalPendentes() > 0 ? (state.totalPendentes() + ' na fila') : 'Último' }}
            </span>
            <kbd class="ml-1 px-1.5 py-0.5 rounded bg-emerald-800/90 text-emerald-200 text-[11px] font-mono">Espaço</kbd>
          }
        </button>
      </div>
    </div>
  `
})
export class ExtractionFormComponent {
  public readonly state = inject(RevisaoStateService);

  public readonly modoEdicaoGeral = signal<boolean>(false);
  public readonly editarIdentificacao = signal<boolean>(false);
  public readonly editarValores = signal<boolean>(false);

  public readonly cnpjInvalido = computed(() => {
    const cnpj = this.state.formulario().cnpjCredor;
    if (!cnpj) return false;
    return !validarCnpj(cnpj);
  });

  public readonly sugestaoCnpj = computed(() => {
    const cnpj = this.state.formulario().cnpjCredor;
    return sugerirCnpjCorreto(cnpj);
  });

  public setModoEdicao(ativo: boolean): void {
    this.modoEdicaoGeral.set(ativo);
  }

  public alternarModoEdicaoGeral(): void {
    this.modoEdicaoGeral.update(v => !v);
  }

  public rotuloTipoDocumento(tipo: string | undefined): string {
    const mapa: Record<string, string> = {
      NOTA_FISCAL_SERVICOS: 'Nota Fiscal de Serviços (NFS-e)',
      NOTA_FISCAL_MERCADORIA: 'Nota Fiscal de Mercadorias (NF-e)',
      RECIBO_PAGAMENTO: 'Recibo Legal',
      MEDICAO_OBRAS: 'Boletim de Medição'
    };
    return mapa[tipo || ''] || 'Documento Fiscal';
  }

  public onTipoRetencaoChange(index: number, tipo: string): void {
    const ret = this.state.formulario().retencoes[index];
    if (ret) {
      this.state.atualizarRetencao(index, { ...ret, tipoTributo: tipo });
    }
  }

  public onValorRetencaoChange(index: number, valor: any): void {
    const ret = this.state.formulario().retencoes[index];
    if (ret) {
      this.state.atualizarRetencao(index, { ...ret, valorRetido: Number(valor) || 0 });
    }
  }
}
