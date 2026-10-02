import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { AuditChecklistComponent } from '../audit-checklist/audit-checklist.component';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../../shared/pipes/cnpj.pipe';
import { validarCnpj, sugerirCnpjCorreto } from '../../../../shared/utils/cnpj-validator';
import { parseNumberBr } from '../../../../shared/utils/number-utils';
import { CurrencyMaskDirective } from '../../../../shared/directives/currency-mask.directive';
import { DecimalMaskDirective } from '../../../../shared/directives/decimal-mask.directive';
import { ArquetipoAuditoria } from '../../model/documento.model';

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
    CurrencyMaskDirective,
    DecimalMaskDirective
  ],
  template: `
    <div class="flex-1 h-full min-h-0 w-full flex flex-col bg-gov-slate-900 border-l border-gov-slate-800 text-gov-slate-100 overflow-hidden">
      
      <!-- Topo: Seletor Inteligente IA & Tipo de Documento -->
      <div class="flex-shrink-0 px-5 py-3 border-b border-gov-slate-800 bg-gov-slate-900/95 backdrop-blur z-20">
        <div class="flex flex-col lg:flex-row lg:items-center justify-between gap-3">
          
          <!-- Lado Esquerdo: Metadados do Arquivo e Status -->
          <div class="min-w-0">
            <div class="flex items-center gap-2 mb-1 flex-wrap">
              <span class="text-xs font-mono font-semibold text-gov-slate-300 truncate max-w-xs" [title]="state.documentoAtual()?.nomeArquivoOriginal || 'Arquivo'">
                📄 {{ state.documentoAtual()?.nomeArquivoOriginal || 'Novo Documento' }}
              </span>
              <span class="text-[10px] font-mono px-2 py-0.5 rounded-full bg-gov-cobalt-950/80 text-gov-cobalt-300 border border-gov-cobalt-500/30">
                {{ state.faseCicloVidaRotulo() }}
              </span>
              <app-status-pill [status]="state.documentoAtual()?.status"></app-status-pill>
            </div>
            
            <div class="flex items-center gap-2">
              <h2 class="text-sm font-bold text-white flex items-center gap-1.5">
                <span>{{ state.iconeArquetipo() }}</span>
                <span>{{ state.rotuloCategoria() }}</span>
              </h2>

              <!-- Badge Inteligente IA vs Manual -->
              @if (!state.foiModificadoManualmente()) {
                <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-950/80 text-emerald-300 border border-emerald-500/40 text-[10px] font-medium" title="A tela já foi preparada e configurada pela IA automaticamente com base no documento">
                  <span class="text-emerald-400">🤖</span> Sugerido pela IA
                </span>
              } @else {
                <span class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-md bg-amber-950/80 text-amber-300 border border-amber-500/40 text-[10px] font-medium">
                  <span>✏️</span> Ajustado Manualmente
                  <button
                    type="button"
                    (click)="state.restaurarClassificacaoIA()"
                    class="ml-1 text-gov-cobalt-300 hover:text-white underline text-[10px] cursor-pointer"
                    title="Restaurar a classificação original detectada pela IA"
                  >
                    ↺ Restaurar IA
                  </button>
                </span>
              }
            </div>
          </div>

          <!-- Lado Direito: Dropdown Unificado + Alternância de Modo -->
          <div class="flex items-center gap-2.5 flex-wrap sm:flex-nowrap">
            
            <!-- Dropdown Único e Completo: Classificação Oficial e Cenários de Demonstração -->
            <div class="relative flex items-center">
              <label for="select-arquetipo" class="text-[11px] text-gov-slate-400 font-medium mr-2 whitespace-nowrap">
                Tipo:
              </label>
              <select
                id="select-arquetipo"
                [ngModel]="state.valorSelecaoDropdown()"
                (ngModelChange)="state.alterarSelecaoTipo($event)"
                class="px-3 py-1.5 text-xs rounded-lg bg-gov-slate-800 border border-gov-slate-700 text-gov-slate-100 font-semibold focus:ring-2 focus:ring-gov-cobalt-500 focus:border-gov-cobalt-500 cursor-pointer shadow-sm min-w-[240px] max-w-full"
                title="Classificação do documento. Já vem pré-selecionado pela IA; altere somente se a IA errou o tipo."
              >
                <optgroup label="📋 Tipos Oficiais de Convênio (IA / Arquétipos)">
                  <option value="ENGENHARIA">📐 Medição de Obras & Diário (Fase 04)</option>
                  <option value="FISCAL">📄 Nota Fiscal / Hábil (Fase 05)</option>
                  <option value="PROJETO_AMBIENTAL">🌿 Licença Ambiental & Projetos (Fase 02)</option>
                  <option value="JURIDICO_LICITATORIO">⚖️ Contrato & Licitação (Fase 03)</option>
                  <option value="REGULARIDADE_PROPOSTA">🏛️ Regularidade & CAUC (Fase 00)</option>
                  <option value="ALTERACOES_ADITIVOS">🔄 Termos Aditivos & Prazos (Fase 06)</option>
                  <option value="PRESTACAO_ENCERRAMENTO">📋 Prestação de Contas Final & RCO (Fase 07)</option>
                  <option value="PASSIVO_TCE">⚠️ Notificação SELIC & TCE (Fase 09)</option>
                  <option value="AGNOSTICO_UNIVERSAL">🌐 Documento Agnóstico / Livre</option>
                </optgroup>
                <optgroup label="⚡ Simulação com Dados Preenchidos (Testes)">
                  <option value="CENARIO:boletim-medicao">Simular: Boletim de Medição (BM)</option>
                  <option value="CENARIO:licenca-ambiental">Simular: Licença Ambiental (SUDEMA)</option>
                  <option value="CENARIO:contrato-licitacao">Simular: Contrato de Obras</option>
                  <option value="CENARIO:certidao-cauc">Simular: Certidão CAUC / SRF</option>
                  <option value="CENARIO:termo-aditivo">Simular: Termo Aditivo Contratual</option>
                  <option value="CENARIO:rco-prestacao">Simular: RCO & Prestação de Contas</option>
                  <option value="CENARIO:passivo-tce">Simular: Notificação SELIC / TCE</option>
                  <option value="CENARIO:agnostico-livre">Simular: Documento Livre Universal</option>
                </optgroup>
              </select>
            </div>

            <!-- Segmented Control de visualização: Leitura / Editar -->
            <div class="bg-gov-slate-950 p-0.5 rounded-lg border border-gov-slate-800 flex items-center shadow-inner">
              <button
                type="button"
                (click)="setModoEdicao(false)"
                class="px-2.5 py-1 rounded text-xs transition-all flex items-center gap-1 cursor-pointer"
                [ngClass]="!modoEdicaoGeral() 
                  ? 'bg-gov-cobalt-600 text-white font-semibold' 
                  : 'text-gov-slate-400 hover:text-gov-slate-200'"
                title="Modo Leitura / Auditoria Rápida"
              >
                <span>👁️</span> Leitura
              </button>
              <button
                type="button"
                (click)="setModoEdicao(true)"
                class="px-2.5 py-1 rounded text-xs transition-all flex items-center gap-1 cursor-pointer"
                [ngClass]="modoEdicaoGeral() 
                  ? 'bg-amber-600 text-white font-semibold' 
                  : 'text-gov-slate-400 hover:text-gov-slate-200'"
                title="Modo Edição Completa dos Campos"
              >
                <span>✏️</span> Editar
              </button>
            </div>

          </div>
        </div>
      </div>

      <!-- Corpo rolável do formulário de auditoria -->
      <div class="flex-1 min-h-0 overflow-y-auto p-5 space-y-4">

        <!-- 1. CHECKLIST EXECUTIVO DE 3 SEGUNDOS NO TOPO -->
        <app-audit-checklist></app-audit-checklist>

        <!-- ============================================================ -->
        <!-- 1. FORMULÁRIO DE ENGENHARIA / BOLETIM DE MEDIÇÃO (FASE 04)   -->
        <!-- ============================================================ -->
        @if (state.ehBoletimMedicao()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-amber-400 flex items-center gap-1.5">
              <span>📐</span> Identificação da Medição & Contrato Municipal
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº do Boletim (BM)</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroDocumento"
                  (ngModelChange)="state.atualizarCampo('numeroDocumento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Contrato Municipal</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroContrato"
                  (ngModelChange)="state.atualizarCampo('numeroContrato', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Período de Medição</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().periodoMedicao"
                  (ngModelChange)="state.atualizarCampo('periodoMedicao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data da Medição</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataEmissao"
                  (ngModelChange)="state.atualizarCampo('dataEmissao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Objeto da Medição / Serviços Executados</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().descricaoServico"
                (ngModelChange)="state.atualizarCampo('descricaoServico', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>

          <!-- Apuração Físico-Financeira -->
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
              <span>📊</span> Apuração Físico-Financeira da Obra
            </h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
              <div class="p-3 bg-gov-slate-900/90 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Desta Medição (Bruto)</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorBruto"
                  (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                  class="w-full bg-transparent text-emerald-300 font-mono font-bold text-base focus:outline-none"
                />
              </div>
              <div class="p-3 bg-gov-slate-900/90 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Acumulado Anterior</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorAcumuladoAnterior"
                  (ngModelChange)="state.atualizarCampo('valorAcumuladoAnterior', $event)"
                  class="w-full bg-transparent text-white font-mono font-semibold text-base focus:outline-none"
                />
              </div>
              <div class="p-3 bg-gov-slate-900/90 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Acumulado Atual</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorAcumuladoAtual"
                  (ngModelChange)="state.atualizarCampo('valorAcumuladoAtual', $event)"
                  class="w-full bg-transparent text-white font-mono font-semibold text-base focus:outline-none"
                />
              </div>
              <div class="p-3 bg-gov-slate-900/90 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Saldo Contratual</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().saldoContratual"
                  (ngModelChange)="state.atualizarCampo('saldoContratual', $event)"
                  class="w-full bg-transparent text-amber-300 font-mono font-semibold text-base focus:outline-none"
                />
              </div>
            </div>

            <!-- Barra de Progresso Físico -->
            <div class="pt-2">
              <div class="flex items-center justify-between text-xs mb-1">
                <span class="text-gov-slate-300 font-semibold">Evolução Física Global da Obra:</span>
                <span class="font-mono font-bold text-emerald-400">{{ state.formulario().percentualExecutado }}%</span>
              </div>
              <div class="w-full h-3 bg-gov-slate-950 rounded-full overflow-hidden border border-gov-slate-700">
                <div class="h-full bg-emerald-500 rounded-full transition-all duration-500"
                     [style.width.%]="state.formulario().percentualExecutado || 0"></div>
              </div>
            </div>
          </div>

          <!-- Contratada, ART e Fiscalização -->
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
              <span>👷</span> Contratada, ART do Fiscal & Atesto Técnico
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Razão Social da Construtora</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().razaoSocialCredor"
                  (ngModelChange)="state.atualizarCampo('razaoSocialCredor', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">CNPJ da Construtora</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().cnpjCredor"
                  (ngModelChange)="state.atualizarCampo('cnpjCredor', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">ART / RRT do Fiscal de Obras</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().artFiscal"
                  (ngModelChange)="state.atualizarCampo('artFiscal', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Engenheiro Fiscal Responsável</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().engenheiroFiscal"
                  (ngModelChange)="state.atualizarCampo('engenheiroFiscal', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Parecer Circunstanciado da Fiscalização Técnica</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().parecerFiscal"
                (ngModelChange)="state.atualizarCampo('parecerFiscal', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 2. FORMULÁRIO FISCAL (NOTA FISCAL / NFS-E - FASE 05)         -->
        <!-- ============================================================ -->
        @else if (state.ehFiscal()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-gov-cobalt-400 flex items-center gap-1.5">
              <span>📄</span> Identificação Fiscal da Nota (NFS-e / NF-e)
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Número da Nota</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroDocumento"
                  (ngModelChange)="state.atualizarCampo('numeroDocumento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Série</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().serieDocumento"
                  (ngModelChange)="state.atualizarCampo('serieDocumento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data de Emissão</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataEmissao"
                  (ngModelChange)="state.atualizarCampo('dataEmissao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nota de Empenho</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroEmpenho"
                  (ngModelChange)="state.atualizarCampo('numeroEmpenho', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Chave de Acesso NF-e (44 dígitos)</label>
              <input
                type="text"
                maxlength="44"
                [ngModel]="state.formulario().chaveAcessoNfe"
                (ngModelChange)="state.atualizarCampo('chaveAcessoNfe', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono text-xs"
              />
            </div>
          </div>

          <!-- Valores e Retenções Tributárias -->
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <div class="flex items-center justify-between">
              <h3 class="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
                <span>💰</span> Apuração de Valores & Retenções Tributárias
              </h3>
              <button
                type="button"
                (click)="state.adicionarRetencao()"
                class="text-xs px-2.5 py-1 rounded bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold flex items-center gap-1 cursor-pointer"
              >
                <span>+</span> Adicionar Tributo
              </button>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div class="p-3 bg-gov-slate-900 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Valor Bruto Total</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorBruto"
                  (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                  class="w-full bg-transparent text-white font-mono font-bold text-lg focus:outline-none"
                />
              </div>
              <div class="p-3 bg-gov-slate-900 rounded-lg border border-gov-slate-700">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Total Retenções / Deduções</span>
                <div class="text-rose-400 font-mono font-bold text-lg mt-1">
                  {{ state.valorTotalDeducoes() | currencyBrl }}
                </div>
              </div>
              <div class="p-3 bg-gov-slate-900 rounded-lg border" [ngClass]="state.possuiDivergencia() ? 'border-rose-500' : 'border-emerald-500'">
                <span class="text-gov-slate-400 block text-[10px] uppercase font-bold">Valor Líquido a Pagar</span>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorLiquido"
                  (ngModelChange)="state.atualizarCampo('valorLiquido', $event)"
                  class="w-full bg-transparent font-mono font-bold text-lg focus:outline-none"
                  [ngClass]="state.possuiDivergencia() ? 'text-rose-400' : 'text-emerald-400'"
                />
              </div>
            </div>

            <!-- Tabela Dinâmica de Retenções -->
            @if (state.formulario().retencoes.length > 0) {
              <div class="mt-3 overflow-x-auto">
                <table class="w-full text-xs text-left">
                  <thead class="text-gov-slate-400 bg-gov-slate-900/60 uppercase text-[10px]">
                    <tr>
                      <th class="p-2">Tributo</th>
                      <th class="p-2">Alíquota (%)</th>
                      <th class="p-2">Valor Retido (R$)</th>
                      <th class="p-2 text-right">Ação</th>
                    </tr>
                  </thead>
                  <tbody class="divide-y divide-gov-slate-800">
                    @for (r of state.formulario().retencoes; track $index) {
                      <tr>
                        <td class="p-2">
                          <select
                            [ngModel]="r.tipoTributo || r.tipo"
                            (ngModelChange)="onTipoRetencaoChange($index, $event)"
                            class="bg-gov-slate-900 border border-gov-slate-700 rounded px-2 py-1 text-white font-mono"
                          >
                            <option value="INSS">INSS</option>
                            <option value="ISS">ISS</option>
                            <option value="IRRF">IRRF</option>
                            <option value="PIS">PIS</option>
                            <option value="COFINS">COFINS</option>
                            <option value="CSLL">CSLL</option>
                            <option value="OUTROS">OUTROS</option>
                          </select>
                        </td>
                        <td class="p-2">
                          <input
                            type="text"
                            appDecimalMask
                            [maxDecimals]="2"
                            [ngModel]="r.aliquotaPercentual"
                            (ngModelChange)="onAliquotaRetencaoChange($index, $event)"
                            class="w-20 bg-gov-slate-900 border border-gov-slate-700 rounded px-2 py-1 text-white font-mono text-center"
                            placeholder="0,0"
                          />
                        </td>
                        <td class="p-2">
                          <input
                            type="text"
                            appCurrencyMask
                            [ngModel]="r.valorRetido"
                            (ngModelChange)="onValorRetencaoChange($index, $event)"
                            class="w-36 bg-gov-slate-900 border border-gov-slate-700 rounded px-2 py-1 text-white font-mono font-semibold"
                            placeholder="R$ 0,00"
                          />
                        </td>
                        <td class="p-2 text-right">
                          <button
                            type="button"
                            (click)="state.removerRetencao($index)"
                            class="text-rose-400 hover:text-rose-300 font-bold px-2 py-1 cursor-pointer"
                          >
                            ✕
                          </button>
                        </td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </div>
        }

        <!-- ============================================================ -->
        <!-- 3. FORMULÁRIO DE PROJETOS & AMBIENTAL (FASE 02)              -->
        <!-- ============================================================ -->
        @else if (state.ehProjetoAmbiental()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-emerald-400 flex items-center gap-1.5">
              <span>🌿</span> Licenciamento Ambiental & Cláusula Suspensiva
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Tipo de Licença</label>
                <select
                  [ngModel]="state.formulario().tipoLicenca"
                  (ngModelChange)="state.atualizarCampo('tipoLicenca', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                >
                  <option value="Licença Prévia (LP)">Licença Prévia (LP)</option>
                  <option value="Licença de Instalação (LI)">Licença de Instalação (LI)</option>
                  <option value="Licença de Operação (LO)">Licença de Operação (LO)</option>
                  <option value="Dispensa de Licenciamento">Dispensa de Licenciamento</option>
                </select>
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Órgão Ambiental Emissor</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().orgaoAmbiental"
                  (ngModelChange)="state.atualizarCampo('orgaoAmbiental', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data de Validade da Licença</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataValidadeLicenca"
                  (ngModelChange)="state.atualizarCampo('dataValidadeLicenca', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº do Processo / Licença</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroProcessoLicenca"
                  (ngModelChange)="state.atualizarCampo('numeroProcessoLicenca', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">BDI Adotado (%)</label>
                <input
                  type="text"
                  appDecimalMask
                  [maxDecimals]="2"
                  [ngModel]="state.formulario().bdiPercentual"
                  (ngModelChange)="state.atualizarCampo('bdiPercentual', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                  placeholder="0,0"
                />
              </div>
              <div class="flex items-center pt-5">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    [ngModel]="state.formulario().condicionantesAtendidas"
                    (ngModelChange)="state.atualizarCampo('condicionantesAtendidas', $event)"
                    class="rounded bg-gov-slate-900 border-gov-slate-700 text-emerald-500 focus:ring-emerald-500 h-4 w-4"
                  />
                  <span class="text-xs text-gov-slate-200 font-semibold">Todas as condicionantes cumpridas</span>
                </label>
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Parecer Técnico de Engenharia / Condicionantes</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().parecerFiscal"
                (ngModelChange)="state.atualizarCampo('parecerFiscal', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 4. FORMULÁRIO JURÍDICO & LICITAÇÃO (FASE 03)                 -->
        <!-- ============================================================ -->
        @else if (state.ehJuridicoLicitatorio()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-purple-400 flex items-center gap-1.5">
              <span>⚖️</span> Processo Licitatório, Edital & Homologação
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Modalidade da Licitação</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().modalidadeLicitacao"
                  (ngModelChange)="state.atualizarCampo('modalidadeLicitacao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº do Edital / Processo</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroProcessoLicitatorio"
                  (ngModelChange)="state.atualizarCampo('numeroProcessoLicitatorio', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data de Homologação</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataHomologacao"
                  (ngModelChange)="state.atualizarCampo('dataHomologacao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Publicação no DOU</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().publicacaoDouSecao"
                  (ngModelChange)="state.atualizarCampo('publicacaoDouSecao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data da Publicação DOU</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().publicacaoDouData"
                  (ngModelChange)="state.atualizarCampo('publicacaoDouData', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº Autorização de Início (AIO)</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroAio"
                  (ngModelChange)="state.atualizarCampo('numeroAio', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Empresa Vencedora & Contratada</label>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                <input
                  type="text"
                  [ngModel]="state.formulario().razaoSocialCredor"
                  (ngModelChange)="state.atualizarCampo('razaoSocialCredor', $event)"
                  placeholder="Razão Social"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
                <input
                  type="text"
                  [ngModel]="state.formulario().cnpjCredor"
                  (ngModelChange)="state.atualizarCampo('cnpjCredor', $event)"
                  placeholder="CNPJ"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 5. FORMULÁRIO DE REGULARIDADE & CAUC (FASE 00)               -->
        <!-- ============================================================ -->
        @else if (state.ehRegularidadeProposta()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-teal-400 flex items-center gap-1.5">
              <span>🏛️</span> Regularidade Fiscal Municipal (CAUC / SIAFI)
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Tipo de Certidão</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().tipoCertidao"
                  (ngModelChange)="state.atualizarCampo('tipoCertidao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-semibold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Situação da Certidão</label>
                <select
                  [ngModel]="state.formulario().situacaoRegularidade"
                  (ngModelChange)="state.atualizarCampo('situacaoRegularidade', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-bold"
                >
                  <option value="REGULAR">REGULAR (Adimplente)</option>
                  <option value="POSITIVA_COM_EFEITO_NEGATIVA">POSITIVA COM EFEITO DE NEGATIVA</option>
                  <option value="IRREGULAR">IRREGULAR (Inadimplente)</option>
                </select>
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data Limite de Validade</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataValidadeCertidao"
                  (ngModelChange)="state.atualizarCampo('dataValidadeCertidao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Emenda Parlamentar Vinculada</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroEmendaParlamentar"
                  (ngModelChange)="state.atualizarCampo('numeroEmendaParlamentar', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div class="md:col-span-2">
                <label class="block text-gov-slate-400 font-medium mb-1">Conta Vinculada (Op 006)</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().dadosContaVinculada"
                  (ngModelChange)="state.atualizarCampo('dadosContaVinculada', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 6. FORMULÁRIO DE ADITIVOS & ALTERAÇÕES (FASE 06)             -->
        <!-- ============================================================ -->
        @else if (state.ehAlteracoesAditivos()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-orange-400 flex items-center gap-1.5">
              <span>🔄</span> Aditamento Contratual & Reajuste de Preços
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Tipo de Aditivo</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().tipoAditivo"
                  (ngModelChange)="state.atualizarCampo('tipoAditivo', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº do Termo Aditivo</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroAditivo"
                  (ngModelChange)="state.atualizarCampo('numeroAditivo', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nova Vigência Fatal</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().novaDataVigencia"
                  (ngModelChange)="state.atualizarCampo('novaDataVigencia', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Percentual de Acréscimo (%)</label>
                <input
                  type="text"
                  appDecimalMask
                  [maxDecimals]="2"
                  [ngModel]="state.formulario().percentualAditamento"
                  (ngModelChange)="state.atualizarCampo('percentualAditamento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-semibold"
                  placeholder="0,0"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Valor do Acréscimo (R$)</label>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorBruto"
                  (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-emerald-300 font-mono font-bold"
                  placeholder="R$ 0,00"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Índice de Reajuste Adotado</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().indiceReajuste"
                  (ngModelChange)="state.atualizarCampo('indiceReajuste', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Justificativa Técnica do Aditamento</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().justificativaAditivo"
                (ngModelChange)="state.atualizarCampo('justificativaAditivo', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 7. FORMULÁRIO DE PRESTAÇÃO DE CONTAS & RCO (FASE 07/08)      -->
        <!-- ============================================================ -->
        @else if (state.ehPrestacaoEncerramento()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-sky-400 flex items-center gap-1.5">
              <span>📋</span> Prestação de Contas Final & Cumprimento do Objeto
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Termo de Recebimento</label>
                <select
                  [ngModel]="state.formulario().tipoRecebimentoObra"
                  (ngModelChange)="state.atualizarCampo('tipoRecebimentoObra', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                >
                  <option value="Termo de Recebimento Definitivo">Termo de Recebimento Definitivo</option>
                  <option value="Termo de Recebimento Provisório">Termo de Recebimento Provisório</option>
                  <option value="Relatório de Cumprimento do Objeto">Relatório de Cumprimento do Objeto (RCO)</option>
                </select>
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Comissão Municipal de Recebimento</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().comissaoRecebimento"
                  (ngModelChange)="state.atualizarCampo('comissaoRecebimento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Valor Devolvido em GRU (R$)</label>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorDevolvidoGru"
                  (ngModelChange)="state.atualizarCampo('valorDevolvidoGru', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-emerald-300 font-mono font-bold"
                  placeholder="R$ 0,00"
                />
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 pt-2">
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  [ngModel]="state.formulario().funcionalidadeAtestada"
                  (ngModelChange)="state.atualizarCampo('funcionalidadeAtestada', $event)"
                  class="rounded bg-gov-slate-900 border-gov-slate-700 text-sky-500 h-4 w-4"
                />
                <span class="text-xs text-gov-slate-200 font-semibold">Funcionalidade pública comprovada</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  [ngModel]="state.formulario().placaInauguracaoInstalada"
                  (ngModelChange)="state.atualizarCampo('placaInauguracaoInstalada', $event)"
                  class="rounded bg-gov-slate-900 border-gov-slate-700 text-sky-500 h-4 w-4"
                />
                <span class="text-xs text-gov-slate-200 font-semibold">Placa de Inauguração instalada</span>
              </label>
              <label class="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  [ngModel]="state.formulario().saldoContaZeroConfirmado"
                  (ngModelChange)="state.atualizarCampo('saldoContaZeroConfirmado', $event)"
                  class="rounded bg-gov-slate-900 border-gov-slate-700 text-sky-500 h-4 w-4"
                />
                <span class="text-xs text-gov-slate-200 font-semibold">Saldo Conta Op 006 R$ 0,00</span>
              </label>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 8. FORMULÁRIO DE PASSIVO JURÍDICO & TCE (FASE 09)            -->
        <!-- ============================================================ -->
        @else if (state.ehPassivoTce()) {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-rose-900/60 space-y-3">
            <h3 class="text-xs font-bold uppercase tracking-wider text-rose-400 flex items-center gap-1.5">
              <span>⚠️</span> Notificação de Glosa, SELIC 45 Dias & Tomada de Contas Especial
            </h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Tipo de Notificação / Apontamento</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().tipoNotificacaoPassivo"
                  (ngModelChange)="state.atualizarCampo('tipoNotificacaoPassivo', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-semibold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Nº do Processo no TCU / Concedente</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().numeroProcessoTce"
                  (ngModelChange)="state.atualizarCampo('numeroProcessoTce', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Prazo Fatal (Dias Restantes)</label>
                <input
                  type="number"
                  [ngModel]="state.formulario().prazoFatalDias"
                  (ngModelChange)="state.atualizarCampo('prazoFatalDias', $event)"
                  class="w-full bg-gov-slate-900 border border-rose-700 rounded px-2.5 py-1.5 text-rose-300 font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Órgão Notificante</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().orgaoNotificante"
                  (ngModelChange)="state.atualizarCampo('orgaoNotificante', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Valor Glosado com Taxa SELIC (R$)</label>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorGlosaSelic"
                  (ngModelChange)="state.atualizarCampo('valorGlosaSelic', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-rose-400 font-mono font-bold"
                  placeholder="R$ 0,00"
                />
              </div>
              <div class="flex items-center pt-5">
                <label class="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    [ngModel]="state.formulario().sumula230Ajuizada"
                    (ngModelChange)="state.atualizarCampo('sumula230Ajuizada', $event)"
                    class="rounded bg-gov-slate-900 border-gov-slate-700 text-rose-500 h-4 w-4"
                  />
                  <span class="text-xs text-rose-200 font-bold">Ação Ordinária Súmula 230 TCU Ajuizada</span>
                </label>
              </div>
            </div>
            <div>
              <label class="block text-gov-slate-400 font-medium mb-1">Parecer Jurídico da Defesa / Medida de Desbloqueio</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().parecerFiscal"
                (ngModelChange)="state.atualizarCampo('parecerFiscal', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>
        }

        <!-- ============================================================ -->
        <!-- 9. FORMULÁRIO AGNÓSTICO UNIVERSAL (NOVO TIPO / OUTROS)       -->
        <!-- ============================================================ -->
        @else {
          <div class="bg-gov-slate-800/40 rounded-xl p-4 border border-gov-slate-800/80 space-y-3">
            <div class="flex items-center justify-between">
              <h3 class="text-xs font-bold uppercase tracking-wider text-cyan-400 flex items-center gap-1.5">
                <span>🌐</span> Identificação Universal do Documento
              </h3>
              <span class="text-[11px] px-2 py-0.5 rounded bg-cyan-950/80 text-cyan-300 border border-cyan-800 font-mono">
                Modo Extensível
              </span>
            </div>
            
            <div class="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div class="md:col-span-2">
                <label class="block text-gov-slate-400 font-medium mb-1">Título / Identificação do Documento</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().tituloDocumento"
                  (ngModelChange)="state.atualizarCampo('tituloDocumento', $event)"
                  placeholder="Ex: Portaria de Nomeação de Comissão de Obras"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-semibold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Número / Protocolo</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().identificadorDocumento || state.formulario().numeroDocumento"
                  (ngModelChange)="state.atualizarCampo('numeroDocumento', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono font-bold"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Data do Documento</label>
                <input
                  type="date"
                  [ngModel]="state.formulario().dataEmissao"
                  (ngModelChange)="state.atualizarCampo('dataEmissao', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Órgão / Emissor</label>
                <input
                  type="text"
                  [ngModel]="state.formulario().orgaoEmissor || state.formulario().razaoSocialCredor"
                  (ngModelChange)="state.atualizarCampo('orgaoEmissor', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white"
                />
              </div>
              <div>
                <label class="block text-gov-slate-400 font-medium mb-1">Valor Envolvido (Opcional - R$)</label>
                <input
                  type="text"
                  appCurrencyMask
                  [ngModel]="state.formulario().valorBruto"
                  (ngModelChange)="state.atualizarCampo('valorBruto', $event)"
                  class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white font-mono"
                  placeholder="R$ 0,00"
                />
              </div>
            </div>

            <!-- Tabela Dinâmica de Metadados Chave-Valor -->
            <div class="pt-3 border-t border-gov-slate-800">
              <div class="flex items-center justify-between mb-2">
                <span class="text-xs font-bold text-gov-slate-300 flex items-center gap-1.5">
                  <span>🏷️</span> Metadados Customizados (Chave-Valor Dinâmicos)
                </span>
                <button
                  type="button"
                  (click)="state.adicionarMetadadoCustomizado()"
                  class="text-xs px-2.5 py-1 rounded bg-cyan-700 hover:bg-cyan-600 text-white font-semibold flex items-center gap-1 cursor-pointer"
                >
                  <span>+</span> Adicionar Campo Customizado
                </button>
              </div>

              @if ((state.formulario().metadadosCustomizados || []).length > 0) {
                <div class="space-y-2">
                  @for (m of state.formulario().metadadosCustomizados; track $index) {
                    <div class="flex items-center gap-2 text-xs">
                      <input
                        type="text"
                        [ngModel]="m.chave"
                        (ngModelChange)="state.atualizarMetadadoCustomizado($index, $event, m.valor)"
                        placeholder="Nome do Campo (ex: Portaria)"
                        class="w-1/3 bg-gov-slate-900 border border-gov-slate-700 rounded px-2 py-1 text-cyan-300 font-medium"
                      />
                      <input
                        type="text"
                        [ngModel]="m.valor"
                        (ngModelChange)="state.atualizarMetadadoCustomizado($index, m.chave, $event)"
                        placeholder="Valor do Campo (ex: Nº 084/2026)"
                        class="flex-1 bg-gov-slate-900 border border-gov-slate-700 rounded px-2 py-1 text-white"
                      />
                      <button
                        type="button"
                        (click)="state.removerMetadadoCustomizado($index)"
                        class="text-rose-400 hover:text-rose-300 px-2 py-1 font-bold cursor-pointer"
                        title="Remover este campo"
                      >
                        ✕
                      </button>
                    </div>
                  }
                </div>
              } @else {
                <div class="text-[11px] text-gov-slate-500 italic p-2 bg-gov-slate-950/60 rounded border border-dashed border-gov-slate-800">
                  Nenhum metadado customizado adicionado. Clique no botão acima para adicionar pares chave-valor específicos deste documento.
                </div>
              }
            </div>

            <div class="pt-2">
              <label class="block text-gov-slate-400 font-medium mb-1">Parecer Conclusivo / Justificativa de Custódia</label>
              <textarea
                rows="2"
                [ngModel]="state.formulario().parecerFiscal"
                (ngModelChange)="state.atualizarCampo('parecerFiscal', $event)"
                class="w-full bg-gov-slate-900 border border-gov-slate-700 rounded px-2.5 py-1.5 text-white text-xs"
              ></textarea>
            </div>
          </div>
        }

      </div>

      <!-- Rodapé com Ações de Aprovação Dinâmica e Rejeição -->
      <div class="flex-shrink-0 px-6 py-3 border-t border-gov-slate-800 bg-gov-slate-900/95 flex items-center justify-between gap-3 z-20">
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
            <span>{{ rotuloBotaoAprovar() }}</span>
            <span class="px-2 py-0.5 rounded bg-emerald-700/80 text-emerald-100 text-xs font-mono font-normal">
              {{ state.totalPendentes() > 0 ? (state.totalPendentes() + ' na fila') : 'Conferido' }}
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

  public setModoEdicao(ativo: boolean): void {
    this.modoEdicaoGeral.set(ativo);
  }

  public rotuloBotaoAprovar = computed(() => {
    switch (this.state.arquetipoAtivo()) {
      case 'ENGENHARIA': return 'Aprovar Medição & Próximo';
      case 'PROJETO_AMBIENTAL': return 'Aprovar Licenciamento & Próximo';
      case 'JURIDICO_LICITATORIO': return 'Aprovar Processo Licitatório & Próximo';
      case 'REGULARIDADE_PROPOSTA': return 'Aprovar Certidão CAUC & Próximo';
      case 'ALTERACOES_ADITIVOS': return 'Aprovar Termo Aditivo & Próximo';
      case 'PRESTACAO_ENCERRAMENTO': return 'Aprovar Prestação de Contas & Próximo';
      case 'PASSIVO_TCE': return 'Aprovar Peça / Notificação & Próximo';
      case 'AGNOSTICO_UNIVERSAL': return 'Aprovar e Arquivar no GED';
      case 'FISCAL':
      default: return 'Aprovar Documento & Próximo';
    }
  });

  public onTipoRetencaoChange(index: number, tipo: string): void {
    const ret = this.state.formulario().retencoes[index];
    if (ret) {
      this.state.atualizarRetencao(index, { ...ret, tipoTributo: tipo, tipo });
    }
  }

  public onAliquotaRetencaoChange(index: number, aliquota: any): void {
    const ret = this.state.formulario().retencoes[index];
    if (ret) {
      const numAliq = parseNumberBr(aliquota);
      let valorRetido = parseNumberBr(ret.valorRetido ?? ret.valor);
      if (numAliq > 0 && valorRetido === 0) {
        const bruto = parseNumberBr(this.state.formulario().valorBruto);
        if (bruto > 0) {
          valorRetido = Math.round((bruto * (numAliq / 100)) * 100) / 100;
        }
      }
      this.state.atualizarRetencao(index, {
        ...ret,
        aliquotaPercentual: numAliq,
        aliquota: numAliq,
        valorRetido,
        valor: valorRetido
      });
    }
  }

  public onValorRetencaoChange(index: number, valor: any): void {
    const ret = this.state.formulario().retencoes[index];
    if (ret) {
      const numVal = parseNumberBr(valor);
      this.state.atualizarRetencao(index, {
        ...ret,
        valorRetido: numVal,
        valor: numVal
      });
    }
  }
}
