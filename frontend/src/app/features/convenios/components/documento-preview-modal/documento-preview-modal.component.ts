import { Component, EventEmitter, Input, OnInit, OnDestroy, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  DocumentoFicheiro,
  FASES_CICLO_VIDA_CONFIG,
  CATEGORIAS_POR_FASE,
  MoverDocumentoPayload,
  ExcluirDocumentoPayload
} from '../../model/ficheiro-digital.model';
import { FicheiroDigitalService } from '../../services/ficheiro-digital.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-documento-preview-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center p-3 md:p-6 bg-black/85 backdrop-blur-md select-none font-sans overflow-hidden animate-in fade-in duration-200"
      (click)="onBackdropClick($event)"
    >
      <div
        class="bg-[#111827] border border-white/10 rounded-2xl w-full max-w-7xl h-[92vh] flex flex-col shadow-2xl overflow-hidden"
        (click)="$event.stopPropagation()"
      >
        <!-- Topo / Barra de Ações -->
        <header class="px-5 py-3.5 border-b border-white/10 flex items-center justify-between bg-white/[0.02]">
          <div class="flex items-center gap-3 min-w-0">
            <div class="w-9 h-9 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 flex items-center justify-center shrink-0">
              <svg *ngIf="isPdf()" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
              </svg>
              <svg *ngIf="isImage()" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
              </svg>
              <svg *ngIf="!isPdf() && !isImage()" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>

            <div class="truncate">
              <div class="flex items-center gap-2">
                <h2 class="text-sm font-semibold text-white truncate max-w-lg" [title]="documento.nomeArquivoOriginal">
                  {{ documento.nomeArquivoOriginal }}
                </h2>
                <span *ngIf="emAnaliseIA()" class="px-2 py-0.5 rounded-full bg-blue-500/20 text-blue-300 border border-blue-500/30 text-[10px] font-mono font-bold flex items-center gap-1">
                  <span class="w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse"></span>
                  <span>Em Análise por IA</span>
                </span>
              </div>
              <div class="flex items-center gap-2 mt-0.5 text-xs text-slate-400">
                <span class="px-2 py-0.5 rounded bg-white/5 border border-white/10 text-slate-300 font-mono text-[11px]">
                  {{ documento.tamanhoFormatado || formatarBytes(documento.tamanhoBytes) }}
                </span>
                <span>•</span>
                <span class="text-slate-300">{{ obterNomeFase(documento.faseCicloVida) }}</span>
                <span>•</span>
                <span class="text-blue-300 font-medium">{{ documento.categoriaDescricao || documento.categoriaDocumento }}</span>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 shrink-0">
            <!-- Controles Específicos para Imagem -->
            <div *ngIf="isImage() && rawUrl()" class="flex items-center gap-1 bg-white/5 p-1 rounded-lg border border-white/10">
              <button
                type="button"
                (click)="rotacionar(-90)"
                class="px-2 py-1 rounded hover:bg-white/10 text-slate-300 hover:text-white text-xs font-bold transition-colors cursor-pointer"
                title="Girar 90° Anti-horário"
              >
                ↺
              </button>
              <button
                type="button"
                (click)="rotacionar(90)"
                class="px-2 py-1 rounded hover:bg-white/10 text-slate-300 hover:text-white text-xs font-bold transition-colors cursor-pointer"
                title="Girar 90° Horário"
              >
                ↻
              </button>
              <div class="h-3 w-px bg-white/10 mx-0.5"></div>
              <button
                type="button"
                (click)="ajustarZoom(-0.15)"
                class="px-2 py-1 rounded hover:bg-white/10 text-slate-300 hover:text-white text-xs font-bold transition-colors cursor-pointer"
                title="Reduzir Zoom"
              >
                -
              </button>
              <span class="font-mono text-xs text-slate-300 min-w-[36px] text-center">
                {{ Math.round(zoomLevel() * 100) }}%
              </span>
              <button
                type="button"
                (click)="ajustarZoom(0.15)"
                class="px-2 py-1 rounded hover:bg-white/10 text-slate-300 hover:text-white text-xs font-bold transition-colors cursor-pointer"
                title="Aumentar Zoom"
              >
                +
              </button>
              <button
                type="button"
                (click)="resetarVisao()"
                class="px-2 py-1 rounded hover:bg-white/10 text-slate-400 hover:text-white text-[11px] transition-colors cursor-pointer"
                title="Ajustar e Centralizar"
              >
                Ajustar
              </button>
            </div>

            <!-- Botão Nova Aba -->
            <a
              *ngIf="rawUrl()"
              [href]="rawUrl()"
              target="_blank"
              class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white border border-white/10 text-xs font-medium transition-colors flex items-center gap-1.5"
              title="Abrir em aba externa"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
              </svg>
              <span>Aba Externa</span>
            </a>

            <!-- Alternar Drawer de Detalhes -->
            <button
              type="button"
              (click)="toggleDrawer()"
              [class.bg-blue-600]="drawerAberto()"
              [class.text-white]="drawerAberto()"
              class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-slate-300 hover:text-white border border-white/10 text-xs font-medium transition-colors flex items-center gap-1.5 cursor-pointer"
            >
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
              </svg>
              <span>Metadados & Auditoria</span>
            </button>

            <!-- Fechar Modal -->
            <button
              type="button"
              (click)="fechar.emit()"
              class="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-white/10 transition-colors cursor-pointer"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>
        </header>

        <!-- Corpo: Preview + Drawer Lateral Retrátil -->
        <div class="flex-1 flex overflow-hidden relative">
          <!-- Área de Preview Inline -->
          <main class="flex-1 bg-[#0b0f19] flex items-center justify-center p-3 relative overflow-hidden">
            <!-- Banner Informativo: Em Análise por IA -->
            <div *ngIf="emAnaliseIA()" class="absolute top-3 left-3 right-3 z-10 px-3.5 py-1.5 rounded-lg bg-blue-950/80 border border-blue-500/30 flex items-center justify-between text-xs text-blue-200 backdrop-blur shadow-lg animate-in fade-in">
              <div class="flex items-center gap-2">
                <span class="relative flex h-2 w-2">
                  <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                  <span class="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                </span>
                <span class="font-bold text-white">Status: Em Análise por IA</span>
                <span class="text-blue-300 hidden sm:inline">• Visualização do arquivo original disponível.</span>
              </div>
              <span class="text-[10px] font-mono text-blue-300/80">OCR & Catalogação</span>
            </div>

            <!-- Spinner Carregamento -->
            <div *ngIf="carregandoPreview()" class="flex flex-col items-center gap-3 text-slate-400">
              <svg class="animate-spin h-8 w-8 text-blue-500" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
              </svg>
              <span class="text-xs">Carregando visualização do documento...</span>
            </div>

            <!-- Erro de Preview -->
            <div *ngIf="!carregandoPreview() && erroPreview()" class="text-center p-6 max-w-md">
              <div class="w-12 h-12 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20 mx-auto flex items-center justify-center mb-3">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
                </svg>
              </div>
              <h3 class="text-sm font-semibold text-white mb-1">Visualização Indisponível</h3>
              <p class="text-xs text-slate-400 mb-4">{{ erroPreview() }}</p>
              <a
                *ngIf="rawUrl()"
                [href]="rawUrl()"
                target="_blank"
                class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
              >
                Tentar Download Direto
              </a>
            </div>

            <!-- Renderizador PDF ou HTML Inline -->
            <iframe
              *ngIf="!carregandoPreview() && !erroPreview() && (isPdf() || isHtml()) && safeUrl()"
              [src]="safeUrl()"
              class="w-full h-full rounded-xl border border-white/10 bg-white"
              title="Visualizador de Documento GovFlow"
            ></iframe>

            <!-- Renderizador de Imagem Inline com Rotação, Zoom e Pan -->
            <div
              *ngIf="!carregandoPreview() && !erroPreview() && isImage() && rawUrl()"
              class="relative w-full h-full flex items-center justify-center overflow-hidden p-2"
              [class.cursor-grab]="!isDragging()"
              [class.cursor-grabbing]="isDragging()"
              (mousedown)="iniciarArrasto($event)"
              (mousemove)="arrastar($event)"
              (mouseup)="finalizarArrasto()"
              (mouseleave)="finalizarArrasto()"
              (wheel)="aoRolarMouse($event)"
              (dblclick)="alternarZoomDuplo($event)"
            >
              <div
                class="relative transition-transform duration-75 select-none inline-block shadow-2xl rounded-lg border border-white/10"
                [style.transform]="transformImageStyle()"
                [style.transform-origin]="'center center'"
              >
                <img
                  [src]="rawUrl()"
                  [alt]="documento.nomeArquivoOriginal"
                  class="max-w-none block select-none pointer-events-none rounded max-h-[80vh]"
                />
              </div>

              <!-- Dica Operacional Flutuante -->
              <div class="absolute bottom-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/10 text-[10px] text-slate-400 font-medium pointer-events-none flex items-center gap-2 shadow-lg z-20">
                <span>🖱️ Arraste para mover</span>
                <span>•</span>
                <span>🔍 Roda amplia/reduz</span>
                <span>•</span>
                <span>⚡ 2x clique zoom</span>
                <span *ngIf="rotacao() !== 0" class="text-amber-400 font-mono font-bold">• {{ rotacao() }}°</span>
              </div>
            </div>

            <!-- Arquivo não visualizável inline -->
            <div *ngIf="!carregandoPreview() && !erroPreview() && !isPdf() && !isHtml() && !isImage()" class="text-center p-6">
              <div class="w-16 h-16 rounded-2xl bg-white/5 border border-white/10 text-slate-300 mx-auto flex items-center justify-center mb-4">
                <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                </svg>
              </div>
              <h3 class="text-sm font-semibold text-white mb-1">Pré-visualização não suportada para este formato</h3>
              <p class="text-xs text-slate-400 mb-4 font-mono">{{ documento.contentType }}</p>
              <a
                *ngIf="rawUrl()"
                [href]="rawUrl()"
                target="_blank"
                class="inline-flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
              >
                Fazer Download do Arquivo
              </a>
            </div>
          </main>

          <!-- Drawer Retrátil de Metadados, Mover e Trilha de Auditoria -->
          <aside
            *ngIf="drawerAberto()"
            class="w-96 border-l border-white/10 bg-[#111827] flex flex-col h-full shrink-0 shadow-2xl animate-in slide-in-from-right duration-200"
          >
            <!-- Abas do Drawer -->
            <nav class="flex border-b border-white/10 bg-white/[0.01]">
              <button
                type="button"
                (click)="abaAtiva.set('metadados')"
                [class.text-blue-400]="abaAtiva() === 'metadados'"
                [class.border-blue-500]="abaAtiva() === 'metadados'"
                class="flex-1 py-2.5 text-xs font-medium border-b-2 border-transparent transition-colors text-slate-400 hover:text-slate-200 cursor-pointer"
              >
                Metadados
              </button>
              <button
                type="button"
                (click)="abaAtiva.set('mover')"
                [class.text-blue-400]="abaAtiva() === 'mover'"
                [class.border-blue-500]="abaAtiva() === 'mover'"
                class="flex-1 py-2.5 text-xs font-medium border-b-2 border-transparent transition-colors text-slate-400 hover:text-slate-200 cursor-pointer"
              >
                Mover / Ajustar
              </button>
              <button
                type="button"
                (click)="carregarAuditoria()"
                [class.text-blue-400]="abaAtiva() === 'auditoria'"
                [class.border-blue-500]="abaAtiva() === 'auditoria'"
                class="flex-1 py-2.5 text-xs font-medium border-b-2 border-transparent transition-colors text-slate-400 hover:text-slate-200 cursor-pointer"
              >
                Auditoria
              </button>
            </nav>

            <!-- Conteúdo da Aba -->
            <div class="flex-1 overflow-y-auto p-4 space-y-4 text-xs">
              <!-- Aba 1: Metadados & Integridade -->
              <div *ngIf="abaAtiva() === 'metadados'" class="space-y-4">
                <!-- Hash SHA-256 (Imutabilidade) -->
                <div class="p-3 rounded-xl bg-white/[0.03] border border-white/10 space-y-1.5">
                  <div class="flex items-center justify-between text-[11px] text-slate-400">
                    <span class="font-medium flex items-center gap-1">
                      <svg class="w-3.5 h-3.5 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                      </svg>
                      Hash SHA-256 (Integridade)
                    </span>
                    <button
                      type="button"
                      (click)="copiarHash()"
                      class="text-blue-400 hover:text-blue-300 font-sans cursor-pointer text-[10px]"
                    >
                      Copiar
                    </button>
                  </div>
                  <p class="font-mono text-[10px] text-slate-200 break-all select-all bg-black/30 p-2 rounded border border-white/5">
                    {{ documento.hashSha256 || 'Calculando no servidor...' }}
                  </p>
                </div>

                <!-- Detalhes Arquivísticos -->
                <div class="space-y-2.5 text-slate-300">
                  <div class="flex justify-between py-1 border-b border-white/5">
                    <span class="text-slate-400">Canal de Origem:</span>
                    <span class="font-medium text-white px-2 py-0.5 rounded bg-white/5 border border-white/10">
                      {{ documento.origemCanal || 'UPLOAD_MANUAL' }}
                    </span>
                  </div>
                  <div class="flex justify-between py-1 border-b border-white/5">
                    <span class="text-slate-400">Pasta Virtual:</span>
                    <span class="font-mono text-slate-200 truncate max-w-[200px]" [title]="documento.pastaVirtual">
                      {{ documento.pastaVirtual }}
                    </span>
                  </div>
                  <div class="flex justify-between py-1 border-b border-white/5">
                    <span class="text-slate-400">Tipo MIME:</span>
                    <span class="font-mono text-slate-200">{{ documento.contentType }}</span>
                  </div>
                  <div class="flex justify-between py-1 border-b border-white/5">
                    <span class="text-slate-400">Enviado em:</span>
                    <span class="text-slate-200">{{ formatarData(documento.createdAt) }}</span>
                  </div>
                </div>

                <!-- Tags -->
                <div *ngIf="documento.tags && documento.tags.length > 0" class="space-y-1.5">
                  <span class="text-[11px] font-medium text-slate-400">Tags Classificatórias:</span>
                  <div class="flex flex-wrap gap-1.5">
                    <span
                      *ngFor="let tag of documento.tags"
                      class="px-2 py-0.5 rounded bg-blue-500/10 text-blue-300 border border-blue-500/20 text-[10px] font-medium"
                    >
                      #{{ tag }}
                    </span>
                  </div>
                </div>

                <!-- Zona de Perigo / Excluir -->
                <div class="pt-4 border-t border-white/10">
                  <button
                    type="button"
                    (click)="confirmarExclusao.set(true)"
                    class="w-full py-2 px-3 rounded-lg border border-rose-500/30 text-rose-400 hover:bg-rose-500/10 text-xs font-semibold transition-colors flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                    <span>Excluir Documento</span>
                  </button>
                </div>
              </div>

              <!-- Aba 2: Mover / Reclassificar Pasta -->
              <div *ngIf="abaAtiva() === 'mover'" class="space-y-4">
                <p class="text-[11px] text-slate-400 leading-relaxed">
                  Mova este documento para outra fase do ciclo de vida ou organize-o dentro de uma subpasta virtual.
                </p>

                <div class="space-y-3">
                  <div>
                    <label class="block text-[11px] font-medium text-slate-300 mb-1">Nova Fase de Destino:</label>
                    <select
                      [(ngModel)]="moverFase"
                      class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    >
                      <option *ngFor="let f of fasesConfig" [value]="f.codigo">
                        {{ f.numero }} - {{ f.nome }}
                      </option>
                    </select>
                  </div>

                  <div>
                    <label class="block text-[11px] font-medium text-slate-300 mb-1">Subpasta Virtual (opcional):</label>
                    <input
                      type="text"
                      [(ngModel)]="moverPastaVirtual"
                      placeholder="/Medicoes/2026"
                      class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none font-mono"
                    />
                  </div>

                  <div>
                    <label class="block text-[11px] font-medium text-slate-300 mb-1">
                      Justificativa da Movimentação <span class="text-rose-400">*</span>:
                    </label>
                    <textarea
                      [(ngModel)]="moverJustificativa"
                      rows="3"
                      placeholder="Descreva o motivo do reordenamento ou reclassificação deste arquivo..."
                      class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    ></textarea>
                  </div>

                  <button
                    type="button"
                    (click)="salvarMovimentacao()"
                    [disabled]="salvandoMovimentacao() || !moverJustificativa.trim()"
                    class="w-full py-2 px-4 rounded-lg bg-blue-600 hover:bg-blue-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold transition-colors flex items-center justify-center gap-2 cursor-pointer shadow-lg shadow-blue-500/20"
                  >
                    <span *ngIf="salvandoMovimentacao()">Salvando...</span>
                    <span *ngIf="!salvandoMovimentacao()">Confirmar Movimentação</span>
                  </button>
                </div>
              </div>

              <!-- Aba 3: Trilha de Auditoria Imutável -->
              <div *ngIf="abaAtiva() === 'auditoria'" class="space-y-3">
                <div *ngIf="carregandoAuditoria()" class="text-center py-6 text-slate-400">
                  <svg class="animate-spin h-5 w-5 mx-auto text-blue-500 mb-2" viewBox="0 0 24 24" fill="none">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                  </svg>
                  <span>Carregando trilha imutável...</span>
                </div>

                <div *ngIf="!carregandoAuditoria() && trilhaAuditoria().length === 0" class="text-center py-6 text-slate-400 text-xs">
                  Nenhum registro de auditoria posterior encontrado para este documento.
                </div>

                <div *ngIf="!carregandoAuditoria() && trilhaAuditoria().length > 0" class="space-y-3">
                  <div
                    *ngFor="let item of trilhaAuditoria()"
                    class="p-2.5 rounded-lg bg-white/[0.02] border border-white/5 space-y-1"
                  >
                    <div class="flex items-center justify-between text-[10px]">
                      <span class="font-bold text-blue-400 tracking-wide">{{ item.acao }}</span>
                      <span class="text-slate-500 font-mono">{{ formatarData(item.createdAt) }}</span>
                    </div>
                    <p class="text-slate-300 text-[11px] leading-relaxed">{{ item.justificativa }}</p>
                    <div *ngIf="item.estadoNovo" class="text-[10px] text-slate-500 font-mono break-all mt-1 bg-black/20 p-1 rounded">
                      {{ item.estadoNovo }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </aside>
        </div>

        <!-- Modal de Confirmação de Exclusão -->
        <div
          *ngIf="confirmarExclusao()"
          class="fixed inset-0 z-60 bg-black/80 flex items-center justify-center p-4 animate-in fade-in"
        >
          <div class="bg-[#1f2937] border border-white/10 rounded-xl p-5 max-w-sm w-full space-y-3">
            <h4 class="text-sm font-bold text-white">Confirmar Exclusão</h4>
            <p class="text-xs text-slate-300">
              O documento será marcado como <strong>EXCLUÍDO</strong> logicamente e preservado na trilha de auditoria.
            </p>
            <div>
              <label class="block text-[11px] text-slate-400 mb-1">Motivo da Exclusão <span class="text-rose-400">*</span>:</label>
              <textarea
                [(ngModel)]="exclusaoJustificativa"
                rows="2"
                placeholder="Ex: Documento duplicado ou emitido incorretamente..."
                class="w-full px-3 py-1.5 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-rose-500 focus:outline-none"
              ></textarea>
            </div>
            <div class="flex justify-end gap-2 pt-2">
              <button
                type="button"
                (click)="confirmarExclusao.set(false)"
                class="px-3 py-1.5 rounded-lg text-slate-300 hover:bg-white/5 text-xs font-medium cursor-pointer"
              >
                Cancelar
              </button>
              <button
                type="button"
                (click)="executarExclusao()"
                [disabled]="!exclusaoJustificativa.trim() || excluindo()"
                class="px-3 py-1.5 rounded-lg bg-rose-600 hover:bg-rose-500 disabled:opacity-50 text-white text-xs font-semibold cursor-pointer"
              >
                <span *ngIf="excluindo()">Excluindo...</span>
                <span *ngIf="!excluindo()">Excluir Documento</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DocumentoPreviewModalComponent implements OnInit, OnDestroy {
  @Input({ required: true }) documento!: DocumentoFicheiro;
  @Output() fechar = new EventEmitter<void>();
  @Output() documentoMovido = new EventEmitter<DocumentoFicheiro>();
  @Output() documentoExcluido = new EventEmitter<string>();

  private readonly ficheiroService = inject(FicheiroDigitalService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly toast = inject(ToastService);

  readonly fasesConfig = FASES_CICLO_VIDA_CONFIG;
  readonly drawerAberto = signal<boolean>(true);
  readonly abaAtiva = signal<'metadados' | 'mover' | 'auditoria'>('metadados');

  readonly carregandoPreview = signal<boolean>(true);
  readonly erroPreview = signal<string | null>(null);
  readonly rawUrl = signal<string | null>(null);
  readonly safeUrl = signal<SafeResourceUrl | null>(null);

  readonly carregandoAuditoria = signal<boolean>(false);
  readonly trilhaAuditoria = signal<any[]>([]);

  // Estados de Movimentação
  moverFase = '';
  moverPastaVirtual = '';
  moverJustificativa = '';
  readonly salvandoMovimentacao = signal<boolean>(false);

  // Estados de Exclusão
  readonly confirmarExclusao = signal<boolean>(false);
  exclusaoJustificativa = '';
  readonly excluindo = signal<boolean>(false);

  ngOnInit(): void {
    if (this.documento) {
      this.moverFase = this.documento.faseCicloVida || 'FASE_01_CELEBRACAO';
      this.moverPastaVirtual = this.documento.pastaVirtual || '';
      this.obterPreviewUrl();
    }
  }

  readonly Math = Math;
  readonly zoomLevel = signal<number>(1.0);
  readonly rotacao = signal<number>(0);
  readonly panX = signal<number>(0);
  readonly panY = signal<number>(0);
  readonly isDragging = signal<boolean>(false);
  private dragStartX = 0;
  private dragStartY = 0;
  private initialPanX = 0;
  private initialPanY = 0;

  isImage(): boolean {
    const ct = this.documento?.contentType?.toLowerCase() || '';
    const nome = this.documento?.nomeArquivoOriginal?.toLowerCase() || '';
    return ct.startsWith('image/') ||
      nome.endsWith('.png') ||
      nome.endsWith('.jpg') ||
      nome.endsWith('.jpeg') ||
      nome.endsWith('.webp') ||
      nome.endsWith('.bmp') ||
      nome.endsWith('.svg');
  }

  isHtml(): boolean {
    if (this.isImage()) return false;
    const ct = this.documento?.contentType?.toLowerCase() || '';
    const nome = this.documento?.nomeArquivoOriginal?.toLowerCase() || '';
    return ct.includes('html') || nome.endsWith('.html') || nome.endsWith('.htm');
  }

  isPdf(): boolean {
    if (this.isImage() || this.isHtml()) return false;
    const ct = this.documento?.contentType?.toLowerCase() || '';
    const nome = this.documento?.nomeArquivoOriginal?.toLowerCase() || '';
    return ct.includes('pdf') || nome.endsWith('.pdf');
  }

  rotacionar(delta: number): void {
    this.rotacao.update(r => (r + delta + 360) % 360);
  }

  resetarRotacao(): void {
    this.rotacao.set(0);
  }

  ajustarZoom(delta: number): void {
    this.zoomLevel.update(z => Math.max(0.3, Math.min(4.0, Math.round((z + delta) * 100) / 100)));
  }

  resetarVisao(): void {
    this.zoomLevel.set(1.0);
    this.rotacao.set(0);
    this.panX.set(0);
    this.panY.set(0);
  }

  iniciarArrasto(e: MouseEvent): void {
    if (e.button !== 0) return;
    this.isDragging.set(true);
    this.dragStartX = e.clientX;
    this.dragStartY = e.clientY;
    this.initialPanX = this.panX();
    this.initialPanY = this.panY();
    e.preventDefault();
  }

  arrastar(e: MouseEvent): void {
    if (!this.isDragging()) return;
    this.panX.set(this.initialPanX + (e.clientX - this.dragStartX));
    this.panY.set(this.initialPanY + (e.clientY - this.dragStartY));
  }

  finalizarArrasto(): void {
    this.isDragging.set(false);
  }

  aoRolarMouse(e: WheelEvent): void {
    if (!this.isImage()) return;
    e.preventDefault();
    const delta = e.deltaY < 0 ? 0.15 : -0.15;
    this.ajustarZoom(delta);
  }

  alternarZoomDuplo(e: MouseEvent): void {
    if (!this.isImage()) return;
    e.preventDefault();
    if (this.zoomLevel() > 1.2) {
      this.resetarVisao();
    } else {
      this.zoomLevel.set(2.0);
    }
  }

  transformImageStyle(): string {
    return `translate(${this.panX()}px, ${this.panY()}px) scale(${this.zoomLevel()}) rotate(${this.rotacao()}deg)`;
  }

  emAnaliseIA(): boolean {
    const s = this.documento?.status || '';
    return s === 'EM_ANALISE_IA' || s === 'RECEBIDO';
  }

  toggleDrawer(): void {
    this.drawerAberto.update(v => !v);
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.fechar.emit();
    }
  }

  obterPreviewUrl(): void {
    if (this.rawUrl() && this.rawUrl()!.startsWith('blob:')) {
      URL.revokeObjectURL(this.rawUrl()!);
      this.rawUrl.set(null);
      this.safeUrl.set(null);
    }

    this.carregandoPreview.set(true);
    this.erroPreview.set(null);

    this.ficheiroService.baixarArquivo(this.documento.id).subscribe({
      next: (blob) => {
        let mime = blob.type;
        const nome = (this.documento?.nomeArquivoOriginal || '').toLowerCase();
        if (nome.endsWith('.pdf')) mime = 'application/pdf';
        else if (nome.endsWith('.png')) mime = 'image/png';
        else if (nome.endsWith('.jpg') || nome.endsWith('.jpeg')) mime = 'image/jpeg';
        else if (nome.endsWith('.webp')) mime = 'image/webp';
        else if (nome.endsWith('.svg')) mime = 'image/svg+xml';
        else if (nome.endsWith('.html') || nome.endsWith('.htm')) mime = 'text/html';

        const finalBlob = mime && mime !== blob.type ? new Blob([blob], { type: mime }) : blob;
        const objectUrl = URL.createObjectURL(finalBlob);
        this.rawUrl.set(objectUrl);
        this.safeUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(objectUrl));
        this.carregandoPreview.set(false);
      },
      error: (err) => {
        console.error('Erro ao obter preview do documento:', err);
        this.erroPreview.set('Não foi possível carregar a visualização do arquivo.');
        this.carregandoPreview.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.rawUrl() && this.rawUrl()!.startsWith('blob:')) {
      URL.revokeObjectURL(this.rawUrl()!);
      this.rawUrl.set(null);
      this.safeUrl.set(null);
    }
  }

  carregarAuditoria(): void {
    this.abaAtiva.set('auditoria');
    this.carregandoAuditoria.set(true);

    this.ficheiroService.listarAuditoria(this.documento.id).subscribe({
      next: lista => {
        this.trilhaAuditoria.set(lista || []);
        this.carregandoAuditoria.set(false);
      },
      error: err => {
        console.error('Erro ao carregar auditoria:', err);
        this.carregandoAuditoria.set(false);
      }
    });
  }

  salvarMovimentacao(): void {
    if (!this.moverJustificativa.trim()) return;

    this.salvandoMovimentacao.set(true);
    const payload: MoverDocumentoPayload = {
      novaFase: this.moverFase,
      novaPastaVirtual: this.moverPastaVirtual.trim() ? this.moverPastaVirtual.trim() : undefined,
      justificativa: this.moverJustificativa.trim()
    };

    this.ficheiroService.moverDocumento(this.documento.id, payload).subscribe({
      next: docAtualizado => {
        this.salvandoMovimentacao.set(false);
        this.toast.sucesso('Documento movido com sucesso no Ficheiro Digital!');
        this.documento = docAtualizado;
        this.documentoMovido.emit(docAtualizado);
        this.abaAtiva.set('metadados');
      },
      error: err => {
        this.salvandoMovimentacao.set(false);
        this.toast.erro('Falha ao mover documento: ' + (err.error?.detail || err.message));
      }
    });
  }

  executarExclusao(): void {
    if (!this.exclusaoJustificativa.trim()) return;

    this.excluindo.set(true);
    const payload: ExcluirDocumentoPayload = {
      justificativa: this.exclusaoJustificativa.trim()
    };

    this.ficheiroService.excluirDocumento(this.documento.id, payload).subscribe({
      next: () => {
        this.excluindo.set(false);
        this.confirmarExclusao.set(false);
        this.toast.sucesso('Documento excluído com sucesso!');
        this.documentoExcluido.emit(this.documento.id);
        this.fechar.emit();
      },
      error: err => {
        this.excluindo.set(false);
        this.toast.erro('Falha ao excluir documento: ' + (err.error?.detail || err.message));
      }
    });
  }

  copiarHash(): void {
    if (this.documento.hashSha256) {
      navigator.clipboard.writeText(this.documento.hashSha256);
      this.toast.sucesso('Hash SHA-256 copiado para a área de transferência.');
    }
  }

  obterNomeFase(codigoFase: string): string {
    const f = this.fasesConfig.find(item => item.codigo === codigoFase);
    return f ? `${f.numero} - ${f.nome}` : codigoFase;
  }

  formatarBytes(bytes?: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  formatarData(dataStr?: string): string {
    if (!dataStr) return '-';
    try {
      const d = new Date(dataStr);
      return d.toLocaleDateString('pt-BR') + ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
    } catch {
      return dataStr;
    }
  }
}
