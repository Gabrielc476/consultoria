import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  CondicionanteItem,
  DossieClausulaSuspensiva,
  TipoCondicionante
} from '../../model/clausula-suspensiva.model';
import { ClausulaSuspensivaService } from '../../services/clausula-suspensiva.service';
import { ConvenioCockpit } from '../../model/convenio-fase.model';

@Component({
  selector: 'app-clausula-suspensiva-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
      <div class="bg-[#0e1726] border border-white/10 rounded-2xl max-w-5xl w-full shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
        
        <!-- Modal Top Bar -->
        <div class="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-gradient-to-r from-gov-cobalt-950/60 via-slate-900 to-[#0e1726] shrink-0">
          <div class="flex items-center gap-3">
            <span class="px-2.5 py-1 rounded bg-gov-cobalt-500/20 text-gov-cobalt-300 border border-gov-cobalt-500/30 text-xs font-mono font-bold">
              FASE 02
            </span>
            <div>
              <h2 class="text-base sm:text-lg font-bold text-white flex items-center gap-2">
                <span>Gestão de Cláusula Suspensiva (Caixa GIGOV)</span>
                @if (dossie()?.superada) {
                  <span class="text-xs px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 font-sans">
                    ✓ SUPERADA
                  </span>
                }
              </h2>
              <p class="text-xs text-gov-slate-400 mt-0.5">
                Convênio: <strong class="text-gov-slate-200">{{ convenio.numeroSiconv }}</strong> • {{ convenio.objeto }}
              </p>
            </div>
          </div>
          <button
            type="button"
            (click)="fechar.emit()"
            class="text-gov-slate-400 hover:text-white p-2 rounded-lg hover:bg-white/5 transition-colors cursor-pointer text-lg font-bold"
          >
            ✕
          </button>
        </div>

        <!-- Feedback Messages -->
        @if (mensagemSucesso()) {
          <div class="px-6 py-2.5 bg-emerald-950/80 border-b border-emerald-500/30 text-emerald-200 text-xs flex items-center justify-between">
            <span class="flex items-center gap-2">
              <svg class="w-4 h-4 text-emerald-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              {{ mensagemSucesso() }}
            </span>
            <button (click)="mensagemSucesso.set(null)" class="text-emerald-400 hover:text-white">✕</button>
          </div>
        }

        @if (mensagemErro()) {
          <div class="px-6 py-2.5 bg-rose-950/80 border-b border-rose-500/30 text-rose-200 text-xs flex items-center justify-between">
            <span class="flex items-center gap-2">
              <svg class="w-4 h-4 text-rose-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              {{ mensagemErro() }}
            </span>
            <button (click)="mensagemErro.set(null)" class="text-rose-400 hover:text-white">✕</button>
          </div>
        }

        <!-- Scrollable Modal Content -->
        <div class="p-6 overflow-y-auto space-y-6 flex-1 text-xs">
          
          <!-- 1. Cronômetro Fatal dos 180 Dias -->
          <div class="p-4 rounded-xl bg-white/[0.03] border border-white/10 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div class="flex items-center gap-4">
              <div
                class="w-12 h-12 rounded-xl flex items-center justify-center font-bold text-lg border"
                [ngClass]="{
                  'bg-emerald-500/10 border-emerald-500/30 text-emerald-400': dossie()?.criticidade === 'REGULAR',
                  'bg-amber-500/10 border-amber-500/30 text-amber-400': dossie()?.criticidade === 'ATENCAO',
                  'bg-rose-500/10 border-rose-500/30 text-rose-400': dossie()?.criticidade === 'CRITICO' || dossie()?.criticidade === 'EXPIRADO'
                }"
              >
                ⏱
              </div>
              <div>
                <div class="flex items-center gap-2">
                  <span class="text-gov-slate-400 font-medium">Prazo Fatal da Cláusula Suspensiva:</span>
                  <span
                    class="px-2 py-0.5 rounded text-[11px] font-bold"
                    [ngClass]="{
                      'bg-emerald-500/20 text-emerald-300': dossie()?.criticidade === 'REGULAR',
                      'bg-amber-500/20 text-amber-300': dossie()?.criticidade === 'ATENCAO',
                      'bg-rose-500/20 text-rose-300': dossie()?.criticidade === 'CRITICO' || dossie()?.criticidade === 'EXPIRADO'
                    }"
                  >
                    {{ dossie()?.criticidadeDescricao || 'Aguardando avaliação' }}
                  </span>
                </div>
                <div class="mt-1 flex items-baseline gap-2">
                  <strong class="text-lg text-white font-mono font-bold">{{ dossie()?.diasRestantes }} dias</strong>
                  <span class="text-gov-slate-400">
                    restantes até <strong class="text-gov-slate-200">{{ dossie()?.prazoFatalEfetivo || dossie()?.prazoOriginal }}</strong>
                  </span>
                  @if (dossie()?.prorrogacaoSolicitada) {
                    <span class="px-2 py-0.5 rounded bg-gov-cobalt-500/20 text-gov-cobalt-300 text-[10px] font-mono">
                      (Prorrogação Solicitada)
                    </span>
                  }
                </div>
              </div>
            </div>

            <!-- Botões de Prorrogação -->
            @if (!dossie()?.superada) {
              <div class="flex items-center gap-2">
                @if (dossie()?.prorrogacaoSolicitada) {
                  <button
                    type="button"
                    (click)="deferirProrrogacao()"
                    class="px-3.5 py-2 rounded-lg bg-emerald-600/30 hover:bg-emerald-600/50 text-emerald-200 border border-emerald-500/40 font-medium transition-colors cursor-pointer inline-flex items-center gap-2"
                  >
                    <span>✓ Deferir Prorrogação</span>
                  </button>
                }
                <button
                  type="button"
                  (click)="formProrrogacaoAberto.set(!formProrrogacaoAberto())"
                  class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-gov-slate-200 border border-white/10 font-medium transition-colors cursor-pointer inline-flex items-center gap-2"
                >
                  <svg class="w-3.5 h-3.5 text-gov-cobalt-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
                  </svg>
                  <span>Solicitar Prorrogação</span>
                </button>
              </div>
            }
          </div>

          <!-- Formulário Inline de Prorrogação -->
          @if (formProrrogacaoAberto()) {
            <div class="p-4 rounded-xl bg-gov-cobalt-950/40 border border-gov-cobalt-500/30 space-y-3">
              <h4 class="text-xs font-bold text-gov-cobalt-300 flex items-center gap-2">
                <span>Protocolo de Solicitação de Prorrogação de Cláusula Suspensiva</span>
              </h4>
              <p class="text-gov-slate-300 text-[11px] leading-relaxed">
                Conforme Portaria Conjunta MGI/MF/CGU nº 33/2023, o pedido de dilação deve ser formalizado perante a Mandatária (Caixa) antes do vencimento do prazo original.
              </p>
              <div class="flex flex-wrap items-center gap-3">
                <label class="text-gov-slate-400">Nova Data Proposta:</label>
                <input
                  type="date"
                  [(ngModel)]="novaDataProrrogacao"
                  class="px-3 py-1.5 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-gov-cobalt-400"
                />
                <button
                  type="button"
                  (click)="salvarProrrogacao()"
                  class="px-3.5 py-1.5 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold cursor-pointer"
                >
                  Confirmar Solicitação
                </button>
                <button
                  type="button"
                  (click)="formProrrogacaoAberto.set(false)"
                  class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gov-slate-400 cursor-pointer"
                >
                  Cancelar
                </button>
              </div>
            </div>
          }

          <!-- 2. Checklist dos Três Pilares da Caixa -->
          <div>
            <div class="flex items-center justify-between mb-3">
              <h3 class="text-sm font-bold text-white flex items-center gap-2">
                <span>Os Três Pilares Condicionantes da Caixa GIGOV</span>
                <span class="text-gov-slate-400 font-normal text-xs">(Manual MN AE099)</span>
              </h3>
              <span class="text-gov-slate-400 text-[11px]">
                {{ pilaresAprovadosCount() }} de 3 pilares aprovados
              </span>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
              
              <!-- Card Pilar 1: Engenharia & Custos -->
              <div class="p-4 rounded-xl bg-white/[0.02] border border-white/10 flex flex-col justify-between space-y-3 relative group hover:border-white/20 transition-all">
                <div>
                  <div class="flex items-start justify-between gap-2 mb-2">
                    <div class="flex items-center gap-2">
                      <span class="text-base">📐</span>
                      <strong class="text-gov-slate-200 text-xs font-semibold">1. Engenharia & Custos</strong>
                    </div>
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-bold"
                      [ngClass]="obterClasseStatus(pilarEngenharia()?.status)"
                    >
                      {{ pilarEngenharia()?.statusDescricao }}
                    </span>
                  </div>

                  <p class="text-gov-slate-400 text-[11px] leading-relaxed">
                    Projetos executivos, planilha orçamentária referenciada em SINAPI/SICRO, Curva ABC e BDI analítico balizado pelo Acórdão TCU 2622/2013 (máx 30%).
                  </p>

                  <div class="mt-3 pt-3 border-t border-white/5 space-y-1.5 text-[11px]">
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Documento:</span>
                      <span class="text-gov-slate-300 font-mono">{{ pilarEngenharia()?.numeroDocumentoComprobatorio || 'Não emitido' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Orçamento Caixa:</span>
                      <span class="text-gov-slate-200 font-mono">{{ (pilarEngenharia()?.valorOrcamentoAprovadoCaixa || 0) | currency:'BRL':'symbol':'1.2-2' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">BDI Aprovado:</span>
                      <span class="text-emerald-400 font-mono font-semibold">{{ pilarEngenharia()?.percentualBdiAprovado ? (pilarEngenharia()?.percentualBdiAprovado + '%') : 'Não aprovado' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">ART / RRT:</span>
                      <span class="text-gov-slate-300 font-mono">{{ pilarEngenharia()?.numeroArtRrt || 'Não informado' }}</span>
                    </div>
                  </div>

                  @if (pilarEngenharia()?.observacoesAnaliseCaixa) {
                    <div class="mt-2.5 p-2 rounded bg-white/5 border border-white/5 text-[11px] text-amber-200">
                      <strong>Nota Caixa:</strong> {{ pilarEngenharia()?.observacoesAnaliseCaixa }}
                    </div>
                  }
                </div>

                <!-- Ações do Pilar 1 -->
                <div class="pt-3 border-t border-white/5 flex flex-wrap gap-2 items-center">
                  @if (!dossie()?.superada && pilarEngenharia()?.status !== 'APROVADO') {
                    <button
                      type="button"
                      (click)="abrirModalAprovacao('ENGENHARIA_PROJETOS_SINAPI')"
                      class="px-2.5 py-1.5 rounded bg-emerald-600/30 hover:bg-emerald-600/50 border border-emerald-500/40 text-emerald-300 font-medium text-[11px] cursor-pointer"
                    >
                      ✓ Aprovar LAE/SPA
                    </button>
                    <button
                      type="button"
                      (click)="abrirModalDiligencia('ENGENHARIA_PROJETOS_SINAPI')"
                      class="px-2.5 py-1.5 rounded bg-amber-600/20 hover:bg-amber-600/40 border border-amber-500/30 text-amber-300 text-[11px] cursor-pointer"
                    >
                      ⚠️ Diligência
                    </button>
                  }
                  @if (!dossie()?.superada) {
                    <label class="px-2.5 py-1.5 rounded bg-white/5 hover:bg-white/10 border border-white/10 text-gov-slate-300 text-[11px] cursor-pointer">
                      <span>📁 Anexar LAE</span>
                      <input type="file" class="hidden" (change)="onUploadArquivo($event, 'ENGENHARIA_PROJETOS_SINAPI')" />
                    </label>
                  }
                  @if (pilarEngenharia()?.s3KeyDocumento) {
                    <button
                      type="button"
                      (click)="baixarDocumento('ENGENHARIA_PROJETOS_SINAPI')"
                      class="px-2.5 py-1.5 rounded bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/40 border border-gov-cobalt-500/30 text-gov-cobalt-300 text-[11px] cursor-pointer inline-flex items-center gap-1"
                    >
                      <span>📥 Baixar LAE</span>
                    </button>
                  }
                </div>
              </div>

              <!-- Card Pilar 2: Licenciamento Ambiental -->
              <div class="p-4 rounded-xl bg-white/[0.02] border border-white/10 flex flex-col justify-between space-y-3 relative group hover:border-white/20 transition-all">
                <div>
                  <div class="flex items-start justify-between gap-2 mb-2">
                    <div class="flex items-center gap-2">
                      <span class="text-base">🌿</span>
                      <strong class="text-gov-slate-200 text-xs font-semibold">2. Licenciamento Ambiental</strong>
                    </div>
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-bold"
                      [ngClass]="obterClasseStatus(pilarAmbiental()?.status)"
                    >
                      {{ pilarAmbiental()?.statusDescricao }}
                    </span>
                  </div>

                  <p class="text-gov-slate-400 text-[11px] leading-relaxed">
                    Licença Prévia (LP) e de Instalação (LI), Licença Ambiental Simplificada ou Declaração Oficial de Inexigibilidade emitida pelo órgão competente.
                  </p>

                  <div class="mt-3 pt-3 border-t border-white/5 space-y-1.5 text-[11px]">
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Licença / Dispensa:</span>
                      <span class="text-gov-slate-300 font-mono">{{ pilarAmbiental()?.numeroDocumentoComprobatorio || 'Pendente' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Órgão Emissor:</span>
                      <span class="text-gov-slate-300">{{ pilarAmbiental()?.orgaoEmissor || 'Não informado' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Validade da Licença:</span>
                      <span class="text-gov-slate-200 font-mono">{{ pilarAmbiental()?.dataValidade || 'Em análise' }}</span>
                    </div>
                    @if (pilarAmbiental()?.dataLimiteSaneamento) {
                      <div class="flex justify-between text-amber-300">
                        <span>Prazo Diligência:</span>
                        <strong class="font-mono">{{ pilarAmbiental()?.dataLimiteSaneamento }}</strong>
                      </div>
                    }
                  </div>

                  @if (pilarAmbiental()?.observacoesAnaliseCaixa) {
                    <div class="mt-2.5 p-2 rounded bg-white/5 border border-white/5 text-[11px] text-amber-200">
                      <strong>Apontamento Caixa:</strong> {{ pilarAmbiental()?.observacoesAnaliseCaixa }}
                    </div>
                  }
                </div>

                <!-- Ações do Pilar 2 -->
                <div class="pt-3 border-t border-white/5 flex flex-wrap gap-2 items-center">
                  @if (!dossie()?.superada && pilarAmbiental()?.status !== 'APROVADO') {
                    <button
                      type="button"
                      (click)="abrirModalAprovacao('LICENCIAMENTO_AMBIENTAL')"
                      class="px-2.5 py-1.5 rounded bg-emerald-600/30 hover:bg-emerald-600/50 border border-emerald-500/40 text-emerald-300 font-medium text-[11px] cursor-pointer"
                    >
                      ✓ Aprovar Licença
                    </button>
                    <button
                      type="button"
                      (click)="abrirModalDiligencia('LICENCIAMENTO_AMBIENTAL')"
                      class="px-2.5 py-1.5 rounded bg-amber-600/20 hover:bg-amber-600/40 border border-amber-500/30 text-amber-300 text-[11px] cursor-pointer"
                    >
                      ⚠️ Diligência
                    </button>
                  }
                  @if (!dossie()?.superada) {
                    <label class="px-2.5 py-1.5 rounded bg-white/5 hover:bg-white/10 border border-white/10 text-gov-slate-300 text-[11px] cursor-pointer">
                      <span>📁 Anexar Licença</span>
                      <input type="file" class="hidden" (change)="onUploadArquivo($event, 'LICENCIAMENTO_AMBIENTAL')" />
                    </label>
                  }
                  @if (pilarAmbiental()?.s3KeyDocumento) {
                    <button
                      type="button"
                      (click)="baixarDocumento('LICENCIAMENTO_AMBIENTAL')"
                      class="px-2.5 py-1.5 rounded bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/40 border border-gov-cobalt-500/30 text-gov-cobalt-300 text-[11px] cursor-pointer inline-flex items-center gap-1"
                    >
                      <span>📥 Baixar Licença</span>
                    </button>
                  }
                </div>
              </div>

              <!-- Card Pilar 3: Titularidade do Imóvel -->
              <div class="p-4 rounded-xl bg-white/[0.02] border border-white/10 flex flex-col justify-between space-y-3 relative group hover:border-white/20 transition-all">
                <div>
                  <div class="flex items-start justify-between gap-2 mb-2">
                    <div class="flex items-center gap-2">
                      <span class="text-base">🏛️</span>
                      <strong class="text-gov-slate-200 text-xs font-semibold">3. Titularidade do Imóvel</strong>
                    </div>
                    <span
                      class="px-2 py-0.5 rounded text-[10px] font-bold"
                      [ngClass]="obterClasseStatus(pilarTitularidade()?.status)"
                    >
                      {{ pilarTitularidade()?.statusDescricao }}
                    </span>
                  </div>

                  <p class="text-gov-slate-400 text-[11px] leading-relaxed">
                    Certidão de Inteiro Teor de Matrícula no Cartório de Imóveis (CRI menor de 30 a 90 dias) ou Auto de Imissão Provisória na Posse judicial.
                  </p>

                  <div class="mt-3 pt-3 border-t border-white/5 space-y-1.5 text-[11px]">
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Matrícula CRI:</span>
                      <span class="text-gov-slate-300 font-mono">{{ pilarTitularidade()?.numeroDocumentoComprobatorio || 'Pendente' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Cartório / Serventia:</span>
                      <span class="text-gov-slate-300">{{ pilarTitularidade()?.orgaoEmissor || 'Não informado' }}</span>
                    </div>
                    <div class="flex justify-between">
                      <span class="text-gov-slate-500">Data de Aceite:</span>
                      <span class="text-gov-slate-200 font-mono">{{ pilarTitularidade()?.dataAprovacao || 'Em conferência' }}</span>
                    </div>
                  </div>

                  @if (pilarTitularidade()?.observacoesAnaliseCaixa) {
                    <div class="mt-2.5 p-2 rounded bg-white/5 border border-white/5 text-[11px] text-amber-200">
                      <strong>Apontamento Caixa:</strong> {{ pilarTitularidade()?.observacoesAnaliseCaixa }}
                    </div>
                  }
                </div>

                <!-- Ações do Pilar 3 -->
                <div class="pt-3 border-t border-white/5 flex flex-wrap gap-2 items-center">
                  @if (!dossie()?.superada && pilarTitularidade()?.status !== 'APROVADO') {
                    <button
                      type="button"
                      (click)="abrirModalAprovacao('TITULARIDADE_IMOVEL')"
                      class="px-2.5 py-1.5 rounded bg-emerald-600/30 hover:bg-emerald-600/50 border border-emerald-500/40 text-emerald-300 font-medium text-[11px] cursor-pointer"
                    >
                      ✓ Aprovar CRI
                    </button>
                    <button
                      type="button"
                      (click)="abrirModalDiligencia('TITULARIDADE_IMOVEL')"
                      class="px-2.5 py-1.5 rounded bg-amber-600/20 hover:bg-amber-600/40 border border-amber-500/30 text-amber-300 text-[11px] cursor-pointer"
                    >
                      ⚠️ Diligência
                    </button>
                  }
                  @if (!dossie()?.superada) {
                    <label class="px-2.5 py-1.5 rounded bg-white/5 hover:bg-white/10 border border-white/10 text-gov-slate-300 text-[11px] cursor-pointer">
                      <span>📁 Anexar CRI</span>
                      <input type="file" class="hidden" (change)="onUploadArquivo($event, 'TITULARIDADE_IMOVEL')" />
                    </label>
                  }
                  @if (pilarTitularidade()?.s3KeyDocumento) {
                    <button
                      type="button"
                      (click)="baixarDocumento('TITULARIDADE_IMOVEL')"
                      class="px-2.5 py-1.5 rounded bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/40 border border-gov-cobalt-500/30 text-gov-cobalt-300 text-[11px] cursor-pointer inline-flex items-center gap-1"
                    >
                      <span>📥 Baixar CRI</span>
                    </button>
                  }
                </div>
              </div>

            </div>
          </div>

          <!-- 3. Painel de Superação da Cláusula Suspensiva -->
          <div class="pt-2">
            @if (dossie()?.superada) {
              <!-- Convênio 100% Eficaz -->
              <div class="p-5 rounded-2xl bg-gradient-to-r from-emerald-950/70 via-slate-900 to-emerald-950/70 border border-emerald-500/40 flex flex-col md:flex-row items-center justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 text-lg">
                    ✓
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-emerald-300">
                      Cláusula Suspensiva Superada — Convênio 100% Eficaz!
                    </h4>
                    <p class="text-xs text-gov-slate-300 mt-0.5">
                      Termo de Retirada registrado no Transferegov.br e custodiado no MinIO. O convênio está plenamente destravado para publicação de editais e emissão de AIO na <strong>Fase 3</strong>.
                    </p>
                  </div>
                </div>
                <div class="flex items-center gap-3">
                  <button
                    type="button"
                    (click)="baixarTermoRetirada()"
                    class="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-semibold text-xs transition-colors cursor-pointer inline-flex items-center gap-2"
                  >
                    <span>📥 Baixar Termo de Retirada</span>
                  </button>
                </div>
              </div>
            } @else if (pilaresAprovadosCount() === 3) {
              <!-- Todos os 3 pilares aprovados: Pronto para superação -->
              <div class="p-5 rounded-2xl bg-gradient-to-r from-gov-cobalt-950/70 via-slate-900 to-emerald-950/70 border border-emerald-500/40 flex flex-col md:flex-row items-center justify-between gap-4">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full bg-emerald-500/20 border border-emerald-500/40 flex items-center justify-center text-emerald-400 text-lg">
                    ★
                  </div>
                  <div>
                    <h4 class="text-sm font-bold text-white">
                      Todos os 3 Pilares Foram Aprovados pela Mandatária!
                    </h4>
                    <p class="text-xs text-gov-slate-300 mt-0.5">
                      O processo atende a todos os requisitos do Decreto nº 11.531/2023. Realize o upload do Termo de Retirada assinado para formalizar a superação imediata.
                    </p>
                  </div>
                </div>

                <div class="flex items-center gap-3 shrink-0">
                  <label class="px-4 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs shadow-lg shadow-emerald-900/30 transition-all cursor-pointer flex items-center gap-2">
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
                    </svg>
                    <span>Upload Termo Assinado & Superar</span>
                    <input type="file" class="hidden" (change)="onUploadTermoRetirada($event)" />
                  </label>
                </div>
              </div>
            } @else {
              <!-- Pilares Pendentes -->
              <div class="p-4 rounded-xl bg-white/[0.02] border border-white/5 flex items-center justify-between text-gov-slate-400">
                <span class="flex items-center gap-2 text-xs">
                  <span class="text-amber-400">ℹ</span>
                  A superação da cláusula suspensiva exige a aprovação cumulativa dos 3 pilares técnicos pela Caixa GIGOV.
                </span>
                <span class="text-xs font-mono">
                  Falta aprovar: {{ 3 - pilaresAprovadosCount() }} pilar(es)
                </span>
              </div>
            }
          </div>

        </div>

        <!-- Modal Footer -->
        <div class="px-6 py-3 border-t border-white/10 bg-black/30 flex justify-end shrink-0">
          <button
            type="button"
            (click)="fechar.emit()"
            class="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/15 text-white font-medium text-xs transition-colors cursor-pointer"
          >
            Fechar Dossiê
          </button>
        </div>

      </div>
    </div>

    <!-- Modal Auxiliar: Registro de Diligência da Caixa -->
    @if (modalDiligenciaAberto()) {
      <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-60 flex items-center justify-center p-4">
        <div class="bg-[#111c30] border border-amber-500/40 rounded-xl max-w-md w-full p-5 shadow-2xl space-y-4">
          <div class="flex items-center justify-between pb-2 border-b border-white/10">
            <h4 class="text-xs font-bold text-amber-300 flex items-center gap-2">
              <span>⚠️ Registro de Diligência Técnica da Caixa</span>
            </h4>
            <button (click)="modalDiligenciaAberto.set(false)" class="text-gov-slate-400 hover:text-white cursor-pointer">✕</button>
          </div>

          <div class="space-y-3 text-xs">
            <div>
              <label class="block text-gov-slate-300 mb-1">Apontamentos / Laudo de Pendências:</label>
              <textarea
                [(ngModel)]="diligenciaObservacoes"
                rows="3"
                placeholder="Ex: Adequar a Curva ABC e memorial descritivo conforme apontado pela GIGOV..."
                class="w-full p-2.5 rounded-lg bg-black/40 border border-white/20 text-white focus:outline-none focus:border-amber-400"
              ></textarea>
            </div>

            <div>
              <label class="block text-gov-slate-300 mb-1">Data-Limite para Saneamento:</label>
              <input
                type="date"
                [(ngModel)]="diligenciaDataLimite"
                class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-amber-400"
              />
            </div>
          </div>

          <div class="flex justify-end gap-2 pt-2 border-t border-white/10">
            <button
              type="button"
              (click)="modalDiligenciaAberto.set(false)"
              class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gov-slate-400 text-xs cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="button"
              (click)="salvarDiligencia()"
              class="px-3.5 py-1.5 rounded-lg bg-amber-600 hover:bg-amber-500 text-white font-semibold text-xs cursor-pointer"
            >
              Registrar Notificação
            </button>
          </div>
        </div>
      </div>
    }

    <!-- Modal Auxiliar: Aprovação Técnica Real do Pilar (Elimina Scope Creep / Hardcode) -->
    @if (modalAprovacaoAberto()) {
      <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-60 flex items-center justify-center p-4">
        <div class="bg-[#111c30] border border-emerald-500/40 rounded-xl max-w-lg w-full p-5 shadow-2xl space-y-4">
          <div class="flex items-center justify-between pb-2 border-b border-white/10">
            <h4 class="text-xs font-bold text-emerald-300 flex items-center gap-2">
              <span>✓ Aprovação Técnica de Condicionante</span>
            </h4>
            <button (click)="modalAprovacaoAberto.set(false)" class="text-gov-slate-400 hover:text-white cursor-pointer">✕</button>
          </div>

          <div class="space-y-3 text-xs">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-gov-slate-300 mb-1">Nº Documento / LAE / Matrícula:</label>
                <input
                  type="text"
                  [(ngModel)]="aprovacaoNumeroDocumento"
                  placeholder="Ex: SPA-914250/2026"
                  class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-emerald-400"
                />
              </div>
              <div>
                <label class="block text-gov-slate-300 mb-1">Órgão Emissor / Mandatária:</label>
                <input
                  type="text"
                  [(ngModel)]="aprovacaoOrgaoEmissor"
                  placeholder="Ex: Caixa GIGOV"
                  class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white focus:outline-none focus:border-emerald-400"
                />
              </div>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-gov-slate-300 mb-1">Data de Aprovação:</label>
                <input
                  type="date"
                  [(ngModel)]="aprovacaoData"
                  class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-emerald-400"
                />
              </div>
              <div>
                <label class="block text-gov-slate-300 mb-1">Data de Validade (se houver):</label>
                <input
                  type="date"
                  [(ngModel)]="aprovacaoDataValidade"
                  class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-emerald-400"
                />
              </div>
            </div>

            @if (pilarAprovacao() === 'ENGENHARIA_PROJETOS_SINAPI') {
              <div class="p-3 rounded-lg bg-white/[0.02] border border-white/10 space-y-3">
                <span class="text-[11px] font-bold text-gov-cobalt-300">Parâmetros SINAPI & BDI (Acórdão TCU nº 2.622/2013)</span>
                
                <div class="grid grid-cols-2 gap-3">
                  <div>
                    <label class="block text-gov-slate-300 mb-1">Orçamento Aprovado (R$):</label>
                    <input
                      type="number"
                      step="0.01"
                      [(ngModel)]="aprovacaoValorOrcamento"
                      class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-emerald-400"
                    />
                  </div>
                  <div>
                    <label class="block text-gov-slate-300 mb-1">Percentual de BDI (%):</label>
                    <input
                      type="number"
                      step="0.01"
                      max="30"
                      [(ngModel)]="aprovacaoPercentualBdi"
                      class="w-full p-2 rounded-lg bg-black/40 border text-white font-mono focus:outline-none"
                      [ngClass]="aprovacaoPercentualBdi > 30 ? 'border-rose-500 focus:border-rose-400 text-rose-300' : 'border-white/20 focus:border-emerald-400'"
                    />
                  </div>
                </div>

                @if (aprovacaoPercentualBdi > 30) {
                  <p class="text-rose-400 text-[10px] font-medium">
                    ⚠️ Limite Máximo TCU Ultrapassado: O teto referencial do BDI para obras de edificações e praças é de até 30.00%.
                  </p>
                }

                <div>
                  <label class="block text-gov-slate-300 mb-1">Nº ART / RRT:</label>
                  <input
                    type="text"
                    [(ngModel)]="aprovacaoArtRrt"
                    placeholder="Ex: ART-PB-123456"
                    class="w-full p-2 rounded-lg bg-black/40 border border-white/20 text-white font-mono focus:outline-none focus:border-emerald-400"
                  />
                </div>
              </div>
            }
          </div>

          <div class="flex justify-end gap-2 pt-2 border-t border-white/10">
            <button
              type="button"
              (click)="modalAprovacaoAberto.set(false)"
              class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-gov-slate-400 text-xs cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="button"
              (click)="salvarAprovacaoTecnica()"
              [disabled]="pilarAprovacao() === 'ENGENHARIA_PROJETOS_SINAPI' && aprovacaoPercentualBdi > 30"
              class="px-3.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold text-xs cursor-pointer"
            >
              Confirmar Aprovação
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ClausulaSuspensivaModalComponent implements OnInit {
  @Input({ required: true }) convenio!: ConvenioCockpit;
  @Output() fechar = new EventEmitter<void>();
  @Output() atualizado = new EventEmitter<DossieClausulaSuspensiva>();

  private readonly service = inject(ClausulaSuspensivaService);

  readonly dossie = signal<DossieClausulaSuspensiva | null>(null);
  readonly carregando = signal(false);
  readonly mensagemSucesso = signal<string | null>(null);
  readonly mensagemErro = signal<string | null>(null);

  readonly formProrrogacaoAberto = signal(false);
  public novaDataProrrogacao = '';

  readonly modalDiligenciaAberto = signal(false);
  readonly pilarDiligencia = signal<TipoCondicionante | null>(null);
  public diligenciaObservacoes = '';
  public diligenciaDataLimite = '';

  readonly modalAprovacaoAberto = signal(false);
  readonly pilarAprovacao = signal<TipoCondicionante | null>(null);
  public aprovacaoNumeroDocumento = '';
  public aprovacaoOrgaoEmissor = '';
  public aprovacaoData = '';
  public aprovacaoDataValidade = '';
  public aprovacaoValorOrcamento = 0;
  public aprovacaoPercentualBdi = 22.12;
  public aprovacaoArtRrt = '';

  ngOnInit(): void {
    this.carregarDossie();
  }

  carregarDossie(): void {
    this.carregando.set(true);
    this.service.obterDossiePorConvenioId(this.convenio.id, this.convenio.numeroSiconv).subscribe({
      next: data => {
        this.dossie.set(data);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
      }
    });
  }

  pilarEngenharia(): CondicionanteItem | undefined {
    return this.dossie()?.condicionantes.find(c => c.tipo === 'ENGENHARIA_PROJETOS_SINAPI');
  }

  pilarAmbiental(): CondicionanteItem | undefined {
    return this.dossie()?.condicionantes.find(c => c.tipo === 'LICENCIAMENTO_AMBIENTAL');
  }

  pilarTitularidade(): CondicionanteItem | undefined {
    return this.dossie()?.condicionantes.find(c => c.tipo === 'TITULARIDADE_IMOVEL');
  }

  pilaresAprovadosCount(): number {
    return this.dossie()?.condicionantes.filter(c => c.status === 'APROVADO').length || 0;
  }

  obterClasseStatus(status?: string): string {
    switch (status) {
      case 'APROVADO':
        return 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
      case 'EM_ANALISE_CAIXA':
        return 'bg-gov-cobalt-500/20 text-gov-cobalt-300 border border-gov-cobalt-500/40';
      case 'DILIGENCIA_EMITIDA':
        return 'bg-amber-500/20 text-amber-300 border border-amber-500/40';
      default:
        return 'bg-white/5 text-gov-slate-400 border border-white/10';
    }
  }

  abrirModalAprovacao(tipo: TipoCondicionante): void {
    this.pilarAprovacao.set(tipo);
    this.aprovacaoNumeroDocumento = '';
    this.aprovacaoOrgaoEmissor = tipo === 'ENGENHARIA_PROJETOS_SINAPI' ? 'Caixa GIGOV' : (tipo === 'LICENCIAMENTO_AMBIENTAL' ? 'Órgão Ambiental' : 'Cartório de Registro de Imóveis');
    this.aprovacaoData = new Date().toISOString().split('T')[0];
    this.aprovacaoDataValidade = '';
    this.aprovacaoValorOrcamento = this.convenio.valorTotal || 0;
    this.aprovacaoPercentualBdi = 22.12;
    this.aprovacaoArtRrt = '';
    this.modalAprovacaoAberto.set(true);
  }

  salvarAprovacaoTecnica(): void {
    const tipo = this.pilarAprovacao();
    if (!tipo) return;

    if (!this.aprovacaoNumeroDocumento) {
      this.mensagemErro.set('Informe o número do documento comprobatório ou laudo.');
      return;
    }

    if (tipo === 'ENGENHARIA_PROJETOS_SINAPI' && this.aprovacaoPercentualBdi > 30) {
      this.mensagemErro.set('Percentual de BDI não pode exceder o teto do TCU de 30%.');
      return;
    }

    this.service.aprovarCondicionante(this.convenio.id, tipo, {
      numeroDocumentoComprobatorio: this.aprovacaoNumeroDocumento,
      orgaoEmissor: this.aprovacaoOrgaoEmissor,
      dataAprovacao: this.aprovacaoData,
      dataValidade: this.aprovacaoDataValidade || undefined,
      valorOrcamentoAprovado: tipo === 'ENGENHARIA_PROJETOS_SINAPI' ? this.aprovacaoValorOrcamento : undefined,
      percentualBdiAprovado: tipo === 'ENGENHARIA_PROJETOS_SINAPI' ? this.aprovacaoPercentualBdi : undefined,
      numeroArtRrt: tipo === 'ENGENHARIA_PROJETOS_SINAPI' ? this.aprovacaoArtRrt : undefined
    }).subscribe({
      next: item => {
        this.modalAprovacaoAberto.set(false);
        this.mensagemSucesso.set(`Pilar '${item.descricaoTipo}' aprovado com sucesso!`);
        this.carregarDossie();
      },
      error: () => {
        this.mensagemErro.set('Erro ao registrar aprovação do pilar.');
      }
    });
  }

  abrirModalDiligencia(tipo: TipoCondicionante): void {
    this.pilarDiligencia.set(tipo);
    this.diligenciaObservacoes = '';
    const futuro = new Date();
    futuro.setDate(futuro.getDate() + 20);
    this.diligenciaDataLimite = futuro.toISOString().split('T')[0];
    this.modalDiligenciaAberto.set(true);
  }

  salvarDiligencia(): void {
    const tipo = this.pilarDiligencia();
    if (!tipo || !this.diligenciaObservacoes || !this.diligenciaDataLimite) {
      this.mensagemErro.set('Preencha os apontamentos e a data limite de saneamento.');
      return;
    }

    this.service.registrarDiligencia(this.convenio.id, tipo, {
      observacoes: this.diligenciaObservacoes,
      dataLimiteSaneamento: this.diligenciaDataLimite
    }).subscribe({
      next: item => {
        this.modalDiligenciaAberto.set(false);
        this.mensagemSucesso.set(`Diligência registrada para '${item.descricaoTipo}'. Prazo limite: ${this.diligenciaDataLimite}`);
        this.carregarDossie();
      },
      error: () => {
        this.mensagemErro.set('Falha ao registrar diligência.');
      }
    });
  }

  salvarProrrogacao(): void {
    if (!this.novaDataProrrogacao) {
      this.mensagemErro.set('Informe a nova data proposta para prorrogação.');
      return;
    }

    this.service.solicitarProrrogacao(this.convenio.id, {
      novoPrazoProrrogado: this.novaDataProrrogacao
    }).subscribe({
      next: atualizado => {
        this.formProrrogacaoAberto.set(false);
        this.dossie.set(atualizado);
        this.mensagemSucesso.set(`Solicitação de prorrogação protocolada com novo prazo fatal: ${this.novaDataProrrogacao}`);
      },
      error: () => {
        this.mensagemErro.set('Falha ao protocolar solicitação de prorrogação.');
      }
    });
  }

  deferirProrrogacao(): void {
    const prazo = this.dossie()?.novoPrazoProrrogado || this.novaDataProrrogacao;
    if (!prazo) {
      this.mensagemErro.set('Não há data proposta de prorrogação registrada.');
      return;
    }

    this.service.deferirProrrogacao(this.convenio.id, {
      novoPrazoProrrogado: prazo
    }).subscribe({
      next: atualizado => {
        this.dossie.set(atualizado);
        this.mensagemSucesso.set(`Prorrogação deferida com sucesso! Novo prazo fatal vigente: ${prazo}`);
      },
      error: () => {
        this.mensagemErro.set('Erro ao deferir prorrogação de prazo.');
      }
    });
  }

  baixarDocumento(tipo: TipoCondicionante): void {
    this.service.downloadDocumento(this.convenio.id, tipo).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${tipo.toLowerCase()}_comprovante.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.mensagemErro.set('Erro ao baixar documento do MinIO.');
      }
    });
  }

  baixarTermoRetirada(): void {
    this.service.downloadTermoRetirada(this.convenio.id).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'termo_retirada_clausula_suspensiva.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.mensagemErro.set('Erro ao baixar Termo de Retirada do MinIO.');
      }
    });
  }

  onUploadArquivo(event: Event, tipo: TipoCondicionante): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.service.uploadDocumento(this.convenio.id, tipo, file).subscribe({
        next: item => {
          this.mensagemSucesso.set(`Arquivo '${file.name}' anexado com sucesso para ${item.descricaoTipo}!`);
          this.carregarDossie();
        },
        error: () => {
          this.mensagemErro.set('Erro ao realizar upload do arquivo.');
        }
      });
    }
  }

  onUploadTermoRetirada(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      this.service.uploadTermoRetirada(this.convenio.id, file).subscribe({
        next: atualizado => {
          this.dossie.set(atualizado);
          this.mensagemSucesso.set(`Termo de Retirada '${file.name}' registrado no MinIO e Cláusula Suspensiva superada com sucesso!`);
          this.atualizado.emit(atualizado);
        },
        error: () => {
          this.mensagemErro.set('Erro ao registrar Termo de Retirada.');
        }
      });
    }
  }
}
