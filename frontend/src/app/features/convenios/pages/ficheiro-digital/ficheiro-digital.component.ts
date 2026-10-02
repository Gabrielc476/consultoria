import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import {
  CATEGORIAS_POR_FASE,
  DocumentoFicheiro,
  FASES_CICLO_VIDA_CONFIG,
  FaseMeta,
  FicheiroDigital,
  PastaFase
} from '../../model/ficheiro-digital.model';
import { FicheiroDigitalService } from '../../services/ficheiro-digital.service';
import { ToastService } from '../../../../core/ui/toast.service';
import { DocumentoPreviewModalComponent } from '../../components/documento-preview-modal/documento-preview-modal.component';

@Component({
  selector: 'app-ficheiro-digital',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, DocumentoPreviewModalComponent],
  template: `
    <div class="p-4 md:p-6 max-w-7xl mx-auto space-y-6 select-none font-sans text-slate-200">
      <!-- Breadcrumb e Topo Contextual -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div class="space-y-1">
          <div class="flex items-center gap-2 text-xs text-slate-400">
            <a routerLink="/convenios" class="hover:text-white transition-colors">Convênios</a>
            <span>/</span>
            <a [routerLink]="['/convenios', convenioId()]" class="hover:text-white transition-colors">
              Cockpit #{{ ficheiro()?.numeroSiconv || 'Convênio' }}
            </a>
            <span>/</span>
            <span class="text-blue-400 font-medium">Ficheiro Digital (GED)</span>
          </div>

          <div class="flex items-center gap-3">
            <h1 class="text-xl md:text-2xl font-bold text-white tracking-tight flex items-center gap-2.5">
              <span>Ficheiro Digital do Convênio</span>
              <span class="text-xs px-2.5 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 font-mono">
                SICONV {{ ficheiro()?.numeroSiconv || '---' }}
              </span>
            </h1>
          </div>
          <p class="text-xs text-slate-400 max-w-2xl truncate">
            {{ ficheiro()?.objeto || 'Estrutura oficial organizada pelas 10 Fases do Ciclo de Vida da Transferência Voluntária' }}
          </p>
        </div>

        <!-- Ações Globais de Exportação -->
        <div class="flex items-center gap-2.5 shrink-0">
          <button
            type="button"
            (click)="exportarDossieIntegral()"
            [disabled]="exportandoZip() || !ficheiro()"
            class="px-3.5 py-2 rounded-xl bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-semibold text-slate-200 hover:text-white transition-all flex items-center gap-2 cursor-pointer shadow-sm disabled:opacity-50"
            title="Download de todos os arquivos organizados em pastas via streaming"
          >
            <svg *ngIf="!exportandoZip()" class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            <svg *ngIf="exportandoZip()" class="animate-spin h-4 w-4 text-emerald-400" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
            </svg>
            <span>{{ exportandoZip() ? 'Gerando ZIP Streaming...' : 'Exportar Dossiê (ZIP)' }}</span>
          </button>

          <button
            type="button"
            (click)="abrirModalUpload()"
            class="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition-all flex items-center gap-2 cursor-pointer shadow-lg shadow-blue-500/20"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            <span>Novo Documento</span>
          </button>
        </div>
      </div>

      <!-- Resumo Numérico do Ficheiro -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="p-4 rounded-2xl bg-[#111827] border border-white/10 space-y-1">
          <span class="text-xs text-slate-400 font-medium">Total de Documentos</span>
          <p class="text-2xl font-bold text-white tracking-tight">{{ ficheiro()?.totalArquivos ?? 0 }}</p>
          <span class="text-[11px] text-slate-500">Distribuídos em 10 Fases</span>
        </div>

        <div class="p-4 rounded-2xl bg-[#111827] border border-white/10 space-y-1">
          <span class="text-xs text-slate-400 font-medium">Armazenamento Alocado</span>
          <p class="text-2xl font-bold text-emerald-400 tracking-tight">
            {{ ficheiro()?.tamanhoTotalFormatado || formatarBytes(ficheiro()?.tamanhoTotalBytes) }}
          </p>
          <span class="text-[11px] text-slate-500">Cluster MinIO S3</span>
        </div>

        <div class="p-4 rounded-2xl bg-[#111827] border border-white/10 space-y-1">
          <span class="text-xs text-slate-400 font-medium">Fase Selecionada</span>
          <p class="text-lg font-bold text-blue-400 tracking-tight truncate">
            {{ faseAtivaConfig()?.numero }} - {{ faseAtivaConfig()?.nome }}
          </p>
          <span class="text-[11px] text-slate-500">{{ documentosFaseAtiva().length }} arquivos arquivados</span>
        </div>

        <div class="p-4 rounded-2xl bg-[#111827] border border-white/10 space-y-1">
          <span class="text-xs text-slate-400 font-medium">Conformidade e Integridade</span>
          <p class="text-lg font-bold text-amber-400 tracking-tight flex items-center gap-1.5">
            <svg class="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
            SHA-256 Nativo
          </p>
          <span class="text-[11px] text-slate-500">Rastreabilidade ponta a ponta</span>
        </div>
      </div>

      <!-- Layout Explorer de Dois Painéis -->
      <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <!-- PAINEL ESQUERDO: Árvore das 10 Fases do Ciclo de Vida (col 4) -->
        <aside class="lg:col-span-4 bg-[#111827] border border-white/10 rounded-2xl overflow-hidden shadow-xl">
          <div class="p-3.5 border-b border-white/10 bg-white/[0.02] flex items-center justify-between">
            <span class="text-xs font-semibold text-slate-300 tracking-wider uppercase flex items-center gap-2">
              <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
              </svg>
              Pastas das 10 Fases
            </span>
            <span class="text-[11px] font-mono text-slate-400">10 / 10</span>
          </div>

          <div class="divide-y divide-white/5 max-h-[700px] overflow-y-auto">
            <button
              *ngFor="let f of fasesConfig"
              type="button"
              (click)="selecionarFase(f.codigo)"
              [ngClass]="codigoFaseSelecionada() === f.codigo ? 'bg-blue-600/15 border-l-4 border-blue-500' : ''"
              class="w-full text-left p-3.5 hover:bg-white/[0.04] transition-all flex items-center justify-between gap-3 cursor-pointer group"
            >
              <div class="flex items-center gap-3 min-w-0">
                <span
                  [class.bg-blue-600]="codigoFaseSelecionada() === f.codigo"
                  [class.text-white]="codigoFaseSelecionada() === f.codigo"
                  class="w-7 h-7 rounded-lg bg-white/5 border border-white/10 text-slate-400 font-mono text-xs font-bold flex items-center justify-center shrink-0 group-hover:border-blue-500/40 transition-colors"
                >
                  {{ f.numero }}
                </span>
                <div class="truncate">
                  <h4
                    [class.text-white]="codigoFaseSelecionada() === f.codigo"
                    class="text-xs font-semibold text-slate-300 truncate group-hover:text-white transition-colors"
                  >
                    {{ f.nome }}
                  </h4>
                  <p class="text-[11px] text-slate-500 truncate">{{ f.descricao }}</p>
                </div>
              </div>

              <div class="flex items-center gap-2 shrink-0">
                <span
                  *ngIf="obterQuantidadeFase(f.codigo) > 0"
                  class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-blue-500/20 text-blue-300 border border-blue-500/30"
                >
                  {{ obterQuantidadeFase(f.codigo) }}
                </span>
                <span
                  *ngIf="obterQuantidadeFase(f.codigo) === 0"
                  class="text-[11px] text-slate-600 font-mono"
                >
                  0
                </span>
              </div>
            </button>
          </div>
        </aside>

        <!-- PAINEL DIREITO: Explorer de Documentos da Fase (col 8) -->
        <main class="lg:col-span-8 space-y-4">
          <!-- Cabeçalho da Fase Selecionada -->
          <div class="p-4 rounded-2xl bg-[#111827] border border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3 shadow-xl">
            <div class="space-y-0.5">
              <div class="flex items-center gap-2">
                <span class="px-2 py-0.5 rounded bg-blue-500/10 border border-blue-500/20 text-blue-400 font-mono text-xs font-bold">
                  FASE {{ faseAtivaConfig()?.numero }}
                </span>
                <h3 class="text-base font-bold text-white">{{ faseAtivaConfig()?.nome }}</h3>
              </div>
              <p class="text-xs text-slate-400">{{ faseAtivaConfig()?.descricao }}</p>
            </div>

            <div class="flex items-center gap-2 shrink-0">
              <button
                type="button"
                (click)="exportarZipFase()"
                [disabled]="documentosFaseAtiva().length === 0 || exportandoZipFase()"
                class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 disabled:opacity-40 disabled:cursor-not-allowed border border-white/10 text-xs font-medium text-slate-300 hover:text-white transition-colors flex items-center gap-1.5 cursor-pointer"
                title="Download em ZIP apenas dos arquivos desta fase"
              >
                <svg *ngIf="!exportandoZipFase()" class="w-3.5 h-3.5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                </svg>
                <svg *ngIf="exportandoZipFase()" class="animate-spin h-3.5 w-3.5 text-blue-400" viewBox="0 0 24 24" fill="none">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                </svg>
                <span>Baixar Pasta ZIP</span>
              </button>

              <button
                type="button"
                (click)="abrirModalUpload()"
                class="px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
              >
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
                </svg>
                <span>Enviar para esta Pasta</span>
              </button>
            </div>
          </div>

          <!-- Filtro de Busca e Categoria -->
          <div class="flex items-center gap-3">
            <div class="relative flex-1">
              <svg class="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
              <input
                type="text"
                [(ngModel)]="filtroTexto"
                placeholder="Buscar por nome do arquivo, tags ou categoria..."
                class="w-full pl-9 pr-4 py-2 rounded-xl bg-[#111827] border border-white/10 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          <!-- Lista / Grade de Arquivos -->
          <div class="bg-[#111827] border border-white/10 rounded-2xl overflow-hidden shadow-xl">
            <!-- Loading -->
            <div *ngIf="carregandoFicheiro()" class="p-12 text-center text-slate-400">
              <svg class="animate-spin h-8 w-8 mx-auto text-blue-500 mb-3" viewBox="0 0 24 24" fill="none">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
              </svg>
              <p class="text-xs">Carregando acervo do Ficheiro Digital...</p>
            </div>

            <!-- Empty State -->
            <div
              *ngIf="!carregandoFicheiro() && documentosFiltrados().length === 0"
              class="p-12 text-center space-y-3"
            >
              <div class="w-14 h-14 rounded-2xl bg-white/5 border border-white/10 text-slate-400 mx-auto flex items-center justify-center">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
                </svg>
              </div>
              <h4 class="text-sm font-semibold text-white">Nenhum documento arquivado nesta fase</h4>
              <p class="text-xs text-slate-400 max-w-sm mx-auto leading-relaxed">
                Envie documentos manualmente via upload ou aguarde o recebimento e classificação automática via IA/WhatsApp.
              </p>
              <button
                type="button"
                (click)="abrirModalUpload()"
                class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold cursor-pointer shadow-lg shadow-blue-500/20"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
                </svg>
                <span>Fazer Upload Agora</span>
              </button>
            </div>

            <!-- Tabela de Documentos -->
            <div *ngIf="!carregandoFicheiro() && documentosFiltrados().length > 0" class="overflow-x-auto">
              <table class="w-full text-left text-xs divide-y divide-white/5">
                <thead class="bg-white/[0.02] text-slate-400 uppercase text-[10px] tracking-wider">
                  <tr>
                    <th class="py-3 px-4">Documento</th>
                    <th class="py-3 px-3">Categoria Oficial</th>
                    <th class="py-3 px-3">Subpasta</th>
                    <th class="py-3 px-3">Tamanho</th>
                    <th class="py-3 px-3">Canal</th>
                    <th class="py-3 px-3">Data</th>
                    <th class="py-3 px-4 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody class="divide-y divide-white/5">
                  <tr
                    *ngFor="let doc of documentosFiltrados()"
                    class="hover:bg-white/[0.02] transition-colors group cursor-pointer"
                    (click)="abrirPreview(doc)"
                  >
                    <!-- Nome do Arquivo -->
                    <td class="py-3 px-4">
                      <div class="flex items-center gap-2.5 min-w-0">
                        <div class="w-7 h-7 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/20 flex items-center justify-center shrink-0">
                          <svg *ngIf="isPdf(doc)" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
                          </svg>
                          <svg *ngIf="isImage(doc)" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                          </svg>
                          <svg *ngIf="!isPdf(doc) && !isImage(doc)" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                          </svg>
                        </div>
                        <div class="truncate max-w-xs md:max-w-sm">
                          <span class="font-semibold text-white group-hover:text-blue-400 transition-colors block truncate" [title]="doc.nomeArquivoOriginal">
                            {{ doc.nomeArquivoOriginal }}
                          </span>
                          <span *ngIf="doc.hashSha256" class="text-[10px] font-mono text-slate-500 truncate block">
                            SHA: {{ doc.hashSha256.substring(0, 16) }}...
                          </span>
                        </div>
                      </div>
                    </td>

                    <!-- Categoria -->
                    <td class="py-3 px-3">
                      <span class="inline-block px-2 py-0.5 rounded text-[11px] font-medium bg-white/5 border border-white/10 text-slate-200">
                        {{ doc.categoriaDescricao || doc.categoriaDocumento }}
                      </span>
                    </td>

                    <!-- Pasta Virtual -->
                    <td class="py-3 px-3 font-mono text-[11px] text-slate-400">
                      {{ doc.pastaVirtual }}
                    </td>

                    <!-- Tamanho -->
                    <td class="py-3 px-3 font-mono text-[11px] text-slate-300">
                      {{ doc.tamanhoFormatado || formatarBytes(doc.tamanhoBytes) }}
                    </td>

                    <!-- Canal -->
                    <td class="py-3 px-3">
                      <span
                        [ngClass]="doc.origemCanal === 'WHATSAPP' ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20' : 'bg-white/5 text-slate-300 border-white/10'"
                        class="px-2 py-0.5 rounded text-[10px] font-bold border"
                      >
                        {{ doc.origemCanal }}
                      </span>
                    </td>

                    <!-- Data -->
                    <td class="py-3 px-3 text-[11px] text-slate-400">
                      {{ formatarData(doc.createdAt) }}
                    </td>

                    <!-- Ações -->
                    <td class="py-3 px-4 text-right" (click)="$event.stopPropagation()">
                      <div class="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          (click)="abrirPreview(doc)"
                          class="p-1.5 text-slate-400 hover:text-white rounded hover:bg-white/10 transition-colors cursor-pointer"
                          title="Visualizar documento e metadados"
                        >
                          <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </main>
      </div>

      <!-- Modal de Upload Drag & Drop -->
      <div
        *ngIf="modalUploadAberto()"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in"
      >
        <div class="bg-[#111827] border border-white/10 rounded-2xl p-6 max-w-lg w-full space-y-4 shadow-2xl">
          <div class="flex items-center justify-between pb-3 border-b border-white/10">
            <h3 class="text-sm font-bold text-white flex items-center gap-2">
              <svg class="w-4 h-4 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
              </svg>
              Upload Manual de Documento
            </h3>
            <button
              type="button"
              (click)="modalUploadAberto.set(false)"
              class="text-slate-400 hover:text-white p-1 rounded-lg hover:bg-white/5 cursor-pointer"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <!-- Área Drag and Drop -->
          <div
            (dragover)="onDragOver($event)"
            (dragleave)="onDragLeave($event)"
            (drop)="onDrop($event)"
            [ngClass]="isDragging() ? 'border-blue-500 bg-blue-500/10' : 'border-white/15'"
            class="border-2 border-dashed rounded-xl p-6 text-center space-y-2 hover:border-blue-400/50 transition-colors cursor-pointer"
            (click)="fileInput.click()"
          >
            <input #fileInput type="file" (change)="onFileSelected($event)" class="hidden" />
            <div class="w-10 h-10 rounded-xl bg-blue-500/10 text-blue-400 mx-auto flex items-center justify-center">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
              </svg>
            </div>
            <div class="text-xs">
              <span *ngIf="!arquivoSelecionado()" class="text-slate-300">
                Arraste o arquivo aqui ou <span class="text-blue-400 font-semibold underline">clique para selecionar</span>
              </span>
              <span *ngIf="arquivoSelecionado()" class="text-emerald-400 font-semibold truncate block">
                {{ arquivoSelecionado()?.name }} ({{ formatarBytes(arquivoSelecionado()?.size) }})
              </span>
            </div>
            <p class="text-[10px] text-slate-500">Suporta PDFs, planilhas e imagens de até 100MB</p>
          </div>

          <!-- Formulário de Metadados -->
          <div class="space-y-3 text-xs">
            <div>
              <label class="block text-[11px] font-medium text-slate-300 mb-1">Fase de Destino:</label>
              <select
                [(ngModel)]="uploadFase"
                (change)="atualizarCategoriasPorFase()"
                class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option *ngFor="let f of fasesConfig" [value]="f.codigo">
                  {{ f.numero }} - {{ f.nome }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-[11px] font-medium text-slate-300 mb-1">Categoria Oficial:</label>
              <select
                [(ngModel)]="uploadCategoria"
                class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
              >
                <option *ngFor="let c of categoriasDisponiveis()" [value]="c.valor">
                  {{ c.rotulo }}
                </option>
              </select>
            </div>

            <div>
              <label class="block text-[11px] font-medium text-slate-300 mb-1">Subpasta Virtual (opcional):</label>
              <input
                type="text"
                [(ngModel)]="uploadPastaVirtual"
                placeholder="Ex: /Medicoes ou /Laudos"
                class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none font-mono"
              />
            </div>

            <div>
              <label class="block text-[11px] font-medium text-slate-300 mb-1">Tags (separadas por vírgula):</label>
              <input
                type="text"
                [(ngModel)]="uploadTags"
                placeholder="obra, asfalto, 2026"
                class="w-full px-3 py-2 rounded-lg bg-black/40 border border-white/15 text-slate-200 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <!-- Ações do Modal -->
          <div class="flex justify-end gap-2 pt-3 border-t border-white/10">
            <button
              type="button"
              (click)="modalUploadAberto.set(false)"
              class="px-4 py-2 rounded-xl text-slate-300 hover:bg-white/5 text-xs font-semibold cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="button"
              (click)="executarUpload()"
              [disabled]="!arquivoSelecionado() || enviandoArquivo()"
              class="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white text-xs font-semibold cursor-pointer shadow-lg shadow-blue-500/20 flex items-center gap-2"
            >
              <span *ngIf="enviandoArquivo()">Enviando para o S3...</span>
              <span *ngIf="!enviandoArquivo()">Salvar no Ficheiro</span>
            </button>
          </div>
        </div>
      </div>

      <!-- Modal de Preview e Detalhes Integrado -->
      <app-documento-preview-modal
        *ngIf="documentoEmPreview()"
        [documento]="documentoEmPreview()!"
        (fechar)="fecharPreview()"
        (documentoMovido)="onDocumentoMovido($event)"
        (documentoExcluido)="onDocumentoExcluido($event)"
      ></app-documento-preview-modal>
    </div>
  `
})
export class FicheiroDigitalComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly ficheiroService = inject(FicheiroDigitalService);
  private readonly toast = inject(ToastService);

  readonly fasesConfig = FASES_CICLO_VIDA_CONFIG;
  readonly convenioId = signal<string>('');
  readonly ficheiro = signal<FicheiroDigital | null>(null);
  readonly carregandoFicheiro = signal<boolean>(true);

  readonly codigoFaseSelecionada = signal<string>('FASE_01_CELEBRACAO');
  readonly filtroTexto = signal<string>('');

  readonly exportandoZip = signal<boolean>(false);
  readonly exportandoZipFase = signal<boolean>(false);

  // Preview Modal
  readonly documentoEmPreview = signal<DocumentoFicheiro | null>(null);

  // Upload Modal & Drag-and-drop
  readonly modalUploadAberto = signal<boolean>(false);
  readonly isDragging = signal<boolean>(false);
  readonly arquivoSelecionado = signal<File | null>(null);
  readonly enviandoArquivo = signal<boolean>(false);

  uploadFase = 'FASE_01_CELEBRACAO';
  uploadCategoria = 'TERMO_CONVENIO';
  uploadPastaVirtual = '';
  uploadTags = '';

  readonly categoriasDisponiveis = computed(() => {
    return CATEGORIAS_POR_FASE[this.uploadFase] || [];
  });

  readonly faseAtivaConfig = computed<FaseMeta | undefined>(() => {
    return this.fasesConfig.find(f => f.codigo === this.codigoFaseSelecionada());
  });

  readonly pastaFaseAtiva = computed<PastaFase | undefined>(() => {
    const f = this.ficheiro();
    if (!f || !f.fases) return undefined;
    return f.fases.find(p => p.fase === this.codigoFaseSelecionada());
  });

  readonly documentosFaseAtiva = computed<DocumentoFicheiro[]>(() => {
    const pasta = this.pastaFaseAtiva();
    return pasta?.documentos || [];
  });

  readonly documentosFiltrados = computed<DocumentoFicheiro[]>(() => {
    const docs = this.documentosFaseAtiva();
    const query = this.filtroTexto().trim().toLowerCase();
    if (!query) return docs;

    return docs.filter(d => {
      const nome = d.nomeArquivoOriginal?.toLowerCase() || '';
      const cat = d.categoriaDescricao?.toLowerCase() || d.categoriaDocumento?.toLowerCase() || '';
      const pasta = d.pastaVirtual?.toLowerCase() || '';
      const tags = d.tags?.join(' ').toLowerCase() || '';
      return nome.includes(query) || cat.includes(query) || pasta.includes(query) || tags.includes(query);
    });
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.convenioId.set(id);
      this.carregarFicheiro();
    }
  }

  carregarFicheiro(): void {
    this.carregandoFicheiro.set(true);
    this.ficheiroService.obterFicheiro(this.convenioId()).subscribe({
      next: res => {
        this.ficheiro.set(res);
        this.carregandoFicheiro.set(false);
      },
      error: err => {
        console.error('Erro ao carregar ficheiro digital:', err);
        this.toast.erro('Não foi possível carregar a árvore do Ficheiro Digital.');
        this.carregandoFicheiro.set(false);
      }
    });
  }

  selecionarFase(codigo: string): void {
    this.codigoFaseSelecionada.set(codigo);
    this.uploadFase = codigo;
    this.atualizarCategoriasPorFase();
  }

  obterQuantidadeFase(codigo: string): number {
    const f = this.ficheiro();
    if (!f || !f.fases) return 0;
    const p = f.fases.find(item => item.fase === codigo);
    return p ? p.quantidadeArquivos : 0;
  }

  atualizarCategoriasPorFase(): void {
    const cats = CATEGORIAS_POR_FASE[this.uploadFase] || [];
    if (cats.length > 0) {
      this.uploadCategoria = cats[0].valor;
    }
  }

  abrirPreview(doc: DocumentoFicheiro): void {
    this.documentoEmPreview.set(doc);
  }

  fecharPreview(): void {
    this.documentoEmPreview.set(null);
  }

  onDocumentoMovido(docAtualizado: DocumentoFicheiro): void {
    this.carregarFicheiro();
  }

  onDocumentoExcluido(docId: string): void {
    this.carregarFicheiro();
  }

  abrirModalUpload(): void {
    this.uploadFase = this.codigoFaseSelecionada();
    this.atualizarCategoriasPorFase();
    this.arquivoSelecionado.set(null);
    this.uploadPastaVirtual = '';
    this.uploadTags = '';
    this.modalUploadAberto.set(true);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.arquivoSelecionado.set(event.dataTransfer.files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const target = event.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      this.arquivoSelecionado.set(target.files[0]);
    }
  }

  executarUpload(): void {
    const arq = this.arquivoSelecionado();
    if (!arq) return;

    this.enviandoArquivo.set(true);
    const tagsArray = this.uploadTags
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    this.ficheiroService.uploadDocumento(
      this.convenioId(),
      arq,
      this.uploadFase,
      this.uploadCategoria,
      this.uploadPastaVirtual.trim() ? this.uploadPastaVirtual.trim() : undefined,
      tagsArray.length > 0 ? tagsArray : undefined
    ).subscribe({
      next: () => {
        this.enviandoArquivo.set(false);
        this.modalUploadAberto.set(false);
        this.toast.sucesso('Documento enviado com sucesso para o Ficheiro Digital!');
        this.carregarFicheiro();
      },
      error: err => {
        this.enviandoArquivo.set(false);
        this.toast.erro('Falha no upload do documento: ' + (err.error?.detail || err.message));
      }
    });
  }

  exportarDossieIntegral(): void {
    this.exportandoZip.set(true);
    this.ficheiroService.downloadZipConvenio(this.convenioId()).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const siconv = this.ficheiro()?.numeroSiconv?.replace(/[^a-zA-Z0-9_-]/g, '_') || 'CONVENIO';
        a.download = `Dossie_Convenio_${siconv}.zip`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportandoZip.set(false);
        this.toast.sucesso('Dossiê integral compactado baixado com sucesso!');
      },
      error: err => {
        console.error('Erro ao baixar dossiê ZIP:', err);
        this.toast.erro('Falha ao gerar o arquivo ZIP do convênio.');
        this.exportandoZip.set(false);
      }
    });
  }

  exportarZipFase(): void {
    this.exportandoZipFase.set(true);
    const faseMeta = this.faseAtivaConfig();
    const nomePasta = faseMeta?.nomePasta || this.codigoFaseSelecionada();

    this.ficheiroService.downloadZipFase(this.convenioId(), this.codigoFaseSelecionada()).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Dossie_${nomePasta}.zip`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportandoZipFase.set(false);
        this.toast.sucesso(`Pasta ${faseMeta?.numero} baixada em ZIP com sucesso!`);
      },
      error: err => {
        console.error('Erro ao baixar ZIP da fase:', err);
        this.toast.erro('Falha ao gerar o arquivo ZIP da fase.');
        this.exportandoZipFase.set(false);
      }
    });
  }

  isPdf(doc: DocumentoFicheiro): boolean {
    return doc.contentType === 'application/pdf' ||
      (doc.nomeArquivoOriginal?.toLowerCase().endsWith('.pdf') ?? false);
  }

  isImage(doc: DocumentoFicheiro): boolean {
    const ct = doc.contentType || '';
    const nome = doc.nomeArquivoOriginal?.toLowerCase() || '';
    return ct.startsWith('image/') || nome.endsWith('.png') || nome.endsWith('.jpg') || nome.endsWith('.jpeg') || nome.endsWith('.webp');
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
