import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { ConfidenceBadgeComponent } from '../../../../shared/ui/confidence-badge/confidence-badge.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../../shared/pipes/cnpj.pipe';

@Component({
  selector: 'app-extraction-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ConfidenceBadgeComponent,
    StatusPillComponent,
    CurrencyBrlPipe
  ],
  template: `
    <div class="h-full flex flex-col bg-gov-slate-900 border-l border-gov-slate-800 text-gov-slate-100 overflow-hidden">
      <!-- Cabeçalho do formulário -->
      <div class="px-6 py-4 border-b border-gov-slate-800 flex items-center justify-between bg-gov-slate-900/90 backdrop-blur">
        <div>
          <span class="text-xs uppercase tracking-wider text-gov-slate-400 font-semibold">Conferência Fiscal</span>
          <h2 class="text-lg font-bold text-white flex items-center gap-2">
            Documento Hábil
            <app-status-pill [status]="state.documentoAtual()?.status"></app-status-pill>
          </h2>
        </div>

        <div class="flex items-center gap-2">
          <span class="text-xs text-gov-slate-400">Score IA:</span>
          <app-confidence-badge [score]="state.documentoAtual()?.extracaoSugerida?.confidenceScoreGeral"></app-confidence-badge>
        </div>
      </div>

      <!-- Corpo rolável do formulário -->
      <div class="flex-1 overflow-y-auto p-6 space-y-6">

        <!-- Seção 1: Identificação do Documento -->
        <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-4">
          <h3 class="text-xs font-semibold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
            <span>📄</span> Identificação Fiscal
          </h3>

          <div class="grid grid-cols-2 gap-4">
            <!-- Tipo de Documento -->
            <div>
              <label class="block text-xs font-medium text-gov-slate-300 mb-1">Tipo de Documento</label>
              <select
                [ngModel]="state.formulario().tipoDocumento"
                (ngModelChange)="state.atualizarCampo('tipoDocumento', $event)"
                (focus)="state.focarCampo('tipoDocumento')"
                (blur)="state.focarCampo(null)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              >
                <option value="NOTA_FISCAL_SERVICOS">Nota Fiscal de Serviços (NFS-e)</option>
                <option value="NOTA_FISCAL_MERCADORIA">Nota Fiscal Eletrônica (NF-e)</option>
                <option value="RECIBO_PAGAMENTO">Recibo Legal de Pagamento</option>
                <option value="MEDICAO_OBRAS">Boletim de Medição de Obras</option>
              </select>
            </div>

            <!-- Número do Documento -->
            <div>
              <div class="flex items-center justify-between mb-1">
                <label for="input-numero" class="block text-xs font-medium text-gov-slate-300">Número da NF / Recibo</label>
                <app-confidence-badge [score]="state.confidenceScores()['numeroDocumento']"></app-confidence-badge>
              </div>
              <input
                id="input-numero"
                type="text"
                [ngModel]="state.formulario().numeroDocumento"
                (ngModelChange)="state.atualizarCampo('numeroDocumento', $event)"
                (focus)="state.focarCampo('numeroDocumento')"
                (blur)="state.focarCampo(null)"
                class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
                placeholder="Ex: 0001542"
              />
            </div>

            <!-- Data de Emissão -->
            <div>
              <div class="flex items-center justify-between mb-1">
                <label for="input-data" class="block text-xs font-medium text-gov-slate-300">Data de Emissão</label>
                <app-confidence-badge [score]="state.confidenceScores()['dataEmissao']"></app-confidence-badge>
              </div>
              <input
                id="input-data"
                type="date"
                [ngModel]="state.formulario().dataEmissao"
                (ngModelChange)="state.atualizarCampo('dataEmissao', $event)"
                (focus)="state.focarCampo('dataEmissao')"
                (blur)="state.focarCampo(null)"
                class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              />
            </div>

            <!-- Número de Empenho -->
            <div>
              <div class="flex items-center justify-between mb-1">
                <label for="input-empenho" class="block text-xs font-medium text-gov-slate-300">Nota de Empenho</label>
                <app-confidence-badge [score]="state.confidenceScores()['numeroEmpenho']"></app-confidence-badge>
              </div>
              <input
                id="input-empenho"
                type="text"
                [ngModel]="state.formulario().numeroEmpenho"
                (ngModelChange)="state.atualizarCampo('numeroEmpenho', $event)"
                (focus)="state.focarCampo('numeroEmpenho')"
                (blur)="state.focarCampo(null)"
                class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
                placeholder="Ex: 2026NE00045"
              />
            </div>
          </div>

          <!-- Credor / Prestador -->
          <div class="grid grid-cols-3 gap-4 pt-2 border-t border-gov-slate-800">
            <div>
              <div class="flex items-center justify-between mb-1">
                <label for="input-cnpj" class="block text-xs font-medium text-gov-slate-300">CNPJ do Credor</label>
                <app-confidence-badge [score]="state.confidenceScores()['cnpjCredor']"></app-confidence-badge>
              </div>
              <input
                id="input-cnpj"
                type="text"
                [ngModel]="state.formulario().cnpjCredor"
                (ngModelChange)="state.atualizarCampo('cnpjCredor', $event)"
                (focus)="state.focarCampo('cnpjCredor')"
                (blur)="state.focarCampo(null)"
                class="w-full font-mono bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
                placeholder="00.000.000/0000-00"
              />
            </div>

            <div class="col-span-2">
              <div class="flex items-center justify-between mb-1">
                <label for="input-razao" class="block text-xs font-medium text-gov-slate-300">Razão Social / Nome Fantasia</label>
                <app-confidence-badge [score]="state.confidenceScores()['razaoSocialCredor']"></app-confidence-badge>
              </div>
              <input
                id="input-razao"
                type="text"
                [ngModel]="state.formulario().razaoSocialCredor"
                (ngModelChange)="state.atualizarCampo('razaoSocialCredor', $event)"
                (focus)="state.focarCampo('razaoSocialCredor')"
                (blur)="state.focarCampo(null)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded-lg px-3 py-2 text-sm text-gov-slate-100 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
                placeholder="Nome da empresa contratada"
              />
            </div>
          </div>
        </div>

        <!-- Seção 2: Conciliação Financeira (Cockpit Tributário) -->
        <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="text-xs font-semibold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
              <span>💰</span> Valores & Retenções Fiscais
            </h3>

            @if (state.possuiDivergencia()) {
              <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-semibold bg-rose-950 text-rose-300 border border-rose-600 animate-pulse">
                Divergência: {{ state.diferencaLiquido() | currencyBrl }}
              </span>
            } @else {
              <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded text-xs font-medium bg-emerald-950/60 text-emerald-300 border border-emerald-600/40">
                Consistência Fiscal OK
              </span>
            }
          </div>

          <!-- Cards de Valores -->
          <div class="grid grid-cols-3 gap-3">
            <!-- Valor Bruto -->
            <div
              class="bg-gov-slate-900 p-3 rounded-lg border border-gov-slate-700/80 transition-all"
              [class.ring-2]="state.campoEmFoco() === 'valorBruto'"
              [class.ring-blue-500]="state.campoEmFoco() === 'valorBruto'"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="text-[11px] font-medium text-gov-slate-400 uppercase">Valor Bruto</span>
                <app-confidence-badge [score]="state.confidenceScores()['valorBruto']"></app-confidence-badge>
              </div>
              <div class="relative flex items-center">
                <span class="absolute left-2 text-xs font-mono text-gov-slate-400">R$</span>
                <input
                  type="number"
                  step="0.01"
                  [ngModel]="state.formulario().valorBruto"
                  (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                  (focus)="state.focarCampo('valorBruto')"
                  (blur)="state.focarCampo(null)"
                  class="w-full bg-transparent font-mono font-bold text-base text-white pl-8 pr-2 py-1 focus:outline-none"
                />
              </div>
            </div>

            <!-- Total Deduções -->
            <div class="bg-gov-slate-900 p-3 rounded-lg border border-gov-slate-700/80">
              <div class="flex items-center justify-between mb-1">
                <span class="text-[11px] font-medium text-gov-slate-400 uppercase">Total Retenções (-)</span>
                <span class="text-[10px] font-mono text-gov-slate-500">Soma Automática</span>
              </div>
              <div class="font-mono font-bold text-base text-amber-400 py-1">
                {{ state.valorTotalDeducoes() | currencyBrl }}
              </div>
            </div>

            <!-- Valor Líquido -->
            <div
              class="bg-gov-slate-900 p-3 rounded-lg border transition-all"
              [ngClass]="state.possuiDivergencia() ? 'border-rose-500/80 bg-rose-950/20' : 'border-emerald-600/60 bg-emerald-950/20'"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="text-[11px] font-medium text-gov-slate-300 uppercase">Valor Líquido</span>
                <app-confidence-badge [score]="state.confidenceScores()['valorLiquido']"></app-confidence-badge>
              </div>
              <div class="relative flex items-center">
                <span class="absolute left-2 text-xs font-mono text-gov-slate-400">R$</span>
                <input
                  type="number"
                  step="0.01"
                  [ngModel]="state.formulario().valorLiquido"
                  (ngModelChange)="state.atualizarCampo('valorLiquido', $event)"
                  (focus)="state.focarCampo('valorLiquido')"
                  (blur)="state.focarCampo(null)"
                  class="w-full bg-transparent font-mono font-bold text-base text-white pl-8 pr-2 py-1 focus:outline-none"
                />
              </div>
            </div>
          </div>

          <!-- Banner de Alerta de Divergência com Botão de Balanceamento -->
          @if (state.possuiDivergencia()) {
            <div class="flex items-center justify-between p-3 rounded-lg bg-rose-950/60 border border-rose-600/80 text-xs">
              <div class="flex items-center gap-2 text-rose-200">
                <span class="text-base">⚠️</span>
                <span>
                  O valor líquido declarado diverge do cálculo (Bruto - Retenções = <strong>{{ state.valorLiquidoCalculado() | currencyBrl }}</strong>).
                </span>
              </div>

              <button
                type="button"
                (click)="state.sincronizarValorLiquido()"
                class="px-3 py-1.5 rounded-md bg-rose-600 hover:bg-rose-500 text-white font-semibold transition-colors shadow-sm flex items-center gap-1.5 whitespace-nowrap"
              >
                <span>⚡</span> Sincronizar Líquido
              </button>
            </div>
          }

          <!-- Tabela de Retenções Tributárias Individuais -->
          <div class="pt-2">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-medium text-gov-slate-300 uppercase tracking-wider">Detalhamento de Tributos Retidos</span>
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
                <div class="flex items-center gap-3 p-2 rounded-lg bg-gov-slate-900 border border-gov-slate-800 text-xs">
                  <!-- Tipo -->
                  <div class="w-1/3">
                    <select
                      [ngModel]="ret.tipoTributo"
                      (ngModelChange)="onTipoRetencaoChange($index, $event)"
                      class="w-full bg-gov-slate-800 border border-gov-slate-700 rounded px-2 py-1 text-gov-slate-200 focus:outline-none"
                    >
                      <option value="INSS">INSS (11%)</option>
                      <option value="ISS">ISS Municipal</option>
                      <option value="IRRF">IRRF</option>
                      <option value="PIS">PIS</option>
                      <option value="COFINS">COFINS</option>
                      <option value="CSLL">CSLL</option>
                      <option value="OUTROS">Outra Dedução</option>
                    </select>
                  </div>

                  <!-- Valor Retido -->
                  <div class="flex-1 relative flex items-center">
                    <span class="absolute left-2 font-mono text-gov-slate-400">R$</span>
                    <input
                      type="number"
                      step="0.01"
                      [ngModel]="ret.valorRetido"
                      (ngModelChange)="onValorRetencaoChange($index, $event)"
                      class="w-full bg-gov-slate-800 border border-gov-slate-700 rounded pl-7 pr-2 py-1 font-mono text-white focus:outline-none"
                      placeholder="0,00"
                    />
                  </div>

                  <!-- Botão Excluir -->
                  <button
                    type="button"
                    (click)="state.removerRetencao($index)"
                    class="text-gov-slate-400 hover:text-rose-400 p-1 transition-colors"
                    title="Remover retenção"
                  >
                    ✕
                  </button>
                </div>
              }

              @if (state.formulario().retencoes.length === 0) {
                <p class="text-xs text-gov-slate-500 italic py-1">Nenhuma retenção fiscal informada para este documento.</p>
              }
            </div>
          </div>
        </div>

        <!-- Seção 3: Preferências e Automações -->
        <div class="flex items-center gap-2 p-3 rounded-lg bg-gov-slate-800/30 border border-gov-slate-800 text-xs text-gov-slate-300">
          <input
            id="chk-memorizar"
            type="checkbox"
            [ngModel]="state.formulario().memorizarRegraFornecedor"
            (ngModelChange)="state.atualizarCampo('memorizarRegraFornecedor', $event)"
            class="rounded bg-gov-slate-900 border-gov-slate-700 text-gov-cobalt-600 focus:ring-0 w-4 h-4 cursor-pointer"
          />
          <label for="chk-memorizar" class="cursor-pointer select-none">
            Memorizar padrão de retenções deste fornecedor para próximas notas
          </label>
        </div>
      </div>

      <!-- Barra de Ações Fixa no Rodapé -->
      <div class="p-4 bg-gov-slate-900 border-t border-gov-slate-800 flex items-center justify-between gap-4">
        <button
          type="button"
          (click)="state.abrirModalRejeicao()"
          [disabled]="state.salvando()"
          class="px-4 py-2.5 rounded-lg border border-rose-700/60 bg-rose-950/40 hover:bg-rose-900/60 text-rose-200 font-semibold text-xs tracking-wide transition-colors flex items-center gap-2"
          title="Atalho: Esc ou Alt + R"
        >
          <span>✕</span> Rejeitar Documento
          <kbd class="ml-1 px-1.5 py-0.5 rounded bg-rose-900/80 text-[10px] text-rose-300 font-mono">Alt+R</kbd>
        </button>

        <button
          type="button"
          (click)="state.aprovar()"
          [disabled]="state.salvando()"
          class="flex-1 px-6 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-sm tracking-wide transition-colors shadow-lg shadow-emerald-950 flex items-center justify-center gap-2 disabled:opacity-50"
          title="Atalho: Ctrl + Enter"
        >
          @if (state.salvando()) {
            <span class="animate-spin text-base">⟳</span>
            <span>Salvando...</span>
          } @else {
            <span>⚡</span>
            <span>Aprovar Dados (OK)</span>
            <kbd class="ml-2 px-2 py-0.5 rounded bg-emerald-700 text-xs font-mono font-normal">Ctrl+Enter</kbd>
          }
        </button>
      </div>
    </div>
  `
})
export class ExtractionFormComponent {
  public readonly state = inject(RevisaoStateService);

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
