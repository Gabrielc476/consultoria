import { Component, Input, Output, EventEmitter, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { BoundingBoxOverlayComponent } from '../bounding-box-overlay/bounding-box-overlay.component';
import { BoundingBox } from '../../model/documento.model';

export type FiltroVisual = 'normal' | 'conforto' | 'alto-contraste' | 'grayscale' | 'invertido';

@Component({
  selector: 'app-media-workspace',
  standalone: true,
  host: {
    'class': 'flex flex-col flex-1 h-full min-h-0 w-full overflow-hidden'
  },
  imports: [CommonModule, NgxExtendedPdfViewerModule, BoundingBoxOverlayComponent],
  template: `
    <div class="relative w-full h-full flex flex-col bg-gov-slate-950 overflow-hidden select-none">
      <!-- Toolbar superior do visualizador de documento -->
      <div class="flex items-center justify-between px-3 py-1.5 bg-gov-slate-900 border-b border-gov-slate-800 text-xs text-gov-slate-300 z-20 shrink-0">
        <!-- Identificação do Arquivo -->
        <div class="flex items-center gap-2 min-w-0">
          <span class="font-mono text-gov-slate-200 font-semibold truncate max-w-[200px]" [title]="nomeArquivo()">
            {{ nomeArquivo() }}
          </span>
          <span
            class="px-1.5 py-0.5 rounded font-mono text-[10px] uppercase font-bold border"
            [ngClass]="isImage() ? 'bg-purple-950/60 text-purple-300 border-purple-500/40' : (isPdf() ? 'bg-blue-950/60 text-blue-300 border-blue-500/40' : 'bg-gov-slate-800 text-gov-slate-300 border-gov-slate-700')"
          >
            {{ isImage() ? 'IMAGEM' : (isPdf() ? 'PDF' : 'HTML') }}
          </span>
        </div>

        <!-- Controles da Toolbar -->
        <div class="flex items-center gap-1.5 flex-wrap justify-end">

          <!-- Controles Específicos para Imagens (Rotação & Filtros de Leitura) -->
          @if (isImage()) {
            <!-- Grupo de Rotação 90° -->
            <div class="flex items-center gap-0.5 bg-gov-slate-800/80 p-0.5 rounded border border-gov-slate-700/80">
              <button
                type="button"
                (click)="rotacionar(-90)"
                class="px-1.5 py-0.5 rounded hover:bg-gov-slate-700 text-gov-slate-200 text-xs font-bold transition-colors cursor-pointer"
                title="Girar 90° Anti-horário"
              >
                ↺
              </button>
              <button
                type="button"
                (click)="rotacionar(90)"
                class="px-1.5 py-0.5 rounded hover:bg-gov-slate-700 text-gov-slate-200 text-xs font-bold transition-colors cursor-pointer"
                title="Girar 90° Horário"
              >
                ↻
              </button>
              @if (rotacao() !== 0) {
                <button
                  type="button"
                  (click)="resetarRotacao()"
                  class="px-1 py-0.5 text-[10px] font-mono text-amber-400 hover:text-amber-300 transition-colors cursor-pointer"
                  title="Clique para voltar para 0°"
                >
                  {{ rotacao() }}°
                </button>
              }
            </div>

            <div class="h-3.5 w-px bg-gov-slate-700 mx-0.5"></div>

            <!-- Seletor de Filtro de Legibilidade / Contraste -->
            <div class="flex items-center gap-1">
              <button
                type="button"
                (click)="alternarFiltroVisual()"
                class="px-2 py-0.5 rounded text-[11px] font-medium transition-all flex items-center gap-1 border cursor-pointer"
                [ngClass]="filtroAtivo() !== 'normal' ? 'bg-amber-950/60 text-amber-300 border-amber-500/40 shadow-sm' : 'bg-gov-slate-800 text-gov-slate-400 border-gov-slate-700 hover:text-white'"
                [title]="'Filtro atual: ' + rotuloFiltro() + '. Clique para alternar (Conforto, Alto Contraste, P&B, Invertido)'"
              >
                <span>{{ iconeFiltro() }}</span>
                <span>{{ rotuloFiltro() }}</span>
              </button>
            </div>

            <div class="h-3.5 w-px bg-gov-slate-700 mx-0.5"></div>
          }

          <!-- Modo Conforto para PDF/HTML -->
          @if (!isImage()) {
            <button
              type="button"
              (click)="alternarConfortoVisual()"
              class="px-2 py-0.5 rounded text-[11px] font-medium transition-all flex items-center gap-1 border cursor-pointer"
              [ngClass]="modoConfortoVisual() ? 'bg-amber-950/60 text-amber-300 border-amber-500/40 shadow-sm' : 'bg-gov-slate-800 text-gov-slate-400 border-gov-slate-700 hover:text-white'"
              title="Alternar filtro de papel suave para reduzir contraste com o tema escuro"
            >
              <span>☕</span>
              <span>{{ modoConfortoVisual() ? 'Conforto Ativo' : 'Conforto Off' }}</span>
            </button>
            <div class="h-3.5 w-px bg-gov-slate-700 mx-0.5"></div>
          }

          <!-- Controles Universais de Zoom -->
          <div class="flex items-center gap-1">
            <button
              type="button"
              (click)="ajustarZoom(-0.15)"
              class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 font-bold transition-colors cursor-pointer"
              title="Reduzir Zoom"
            >
              -
            </button>
            <span class="font-mono text-xs px-1 text-gov-slate-300 min-w-[42px] text-center">
              {{ Math.round(zoomLevel() * 100) }}%
            </span>
            <button
              type="button"
              (click)="ajustarZoom(0.15)"
              class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 font-bold transition-colors cursor-pointer"
              title="Aumentar Zoom"
            >
              +
            </button>
            <button
              type="button"
              (click)="resetarVisao()"
              class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 text-[11px] transition-colors cursor-pointer"
              title="Ajustar ao tamanho ideal e centralizar"
            >
              Ajustar
            </button>
            <button
              type="button"
              (click)="zoom100()"
              class="px-1.5 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-400 hover:text-white text-[10px] font-mono transition-colors cursor-pointer"
              title="Zoom 100% (Tamanho Real)"
            >
              1:1
            </button>
          </div>

          <!-- Botão Modo PDF Alternativo -->
          @if (isPdf()) {
            <div class="h-3.5 w-px bg-gov-slate-700 mx-0.5"></div>
            <button
              type="button"
              (click)="alternarModoVisualizador()"
              class="px-2 py-0.5 rounded border border-gov-slate-700 bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 text-[11px] cursor-pointer"
              title="Alternar entre visualizador interativo com caixas e nativo"
            >
              {{ modoVisualizador() === 'ngx' ? 'Interativo' : 'Nativo' }}
            </button>
          }

          <!-- Botão Baixar Arquivo Original -->
          @if (arquivoUrl()) {
            <div class="h-3.5 w-px bg-gov-slate-700 mx-0.5"></div>
            <a
              [href]="arquivoUrl()"
              [download]="nomeArquivo()"
              target="_blank"
              class="px-2 py-0.5 rounded border border-gov-slate-700 bg-gov-slate-800 hover:bg-white/10 text-slate-300 hover:text-white text-[11px] flex items-center gap-1 transition-colors"
              title="Baixar arquivo original"
            >
              <span>⬇</span>
              <span class="hidden sm:inline">Download</span>
            </a>
          }
        </div>
      </div>

      <!-- Banner Informativo: Em Análise por IA -->
      @if (emAnaliseIA()) {
        <div class="px-4 py-2 bg-blue-950/70 border-b border-blue-500/30 flex items-center justify-between text-xs text-blue-200 z-10 animate-in fade-in duration-200 shrink-0">
          <div class="flex items-center gap-2 min-w-0">
            <span class="relative flex h-2.5 w-2.5 shrink-0">
              <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-blue-500"></span>
            </span>
            <span class="font-bold text-white shrink-0">Em análise por IA:</span>
            <span class="text-blue-300 truncate">
              O motor multimodal está identificando os campos de <strong class="text-white">{{ nomeArquivo() }}</strong>. O arquivo original está disponível abaixo.
            </span>
          </div>
          <span class="hidden md:inline-flex px-2 py-0.5 rounded bg-blue-500/20 text-blue-300 text-[10px] font-mono border border-blue-500/30 shrink-0 font-semibold uppercase">
            Processando
          </span>
        </div>
      }

      <!-- Área de Visualização Principal do Documento -->
      <div
        class="relative flex-1 overflow-hidden flex items-center justify-center p-2 bg-[#080d1a]"
        (wheel)="aoRolarMouse($event)"
      >
        <!-- CASO 1: IMAGEM (JPEG, PNG, WEBP) COM DRAG, ZOOM E ROTAÇÃO -->
        @if (isImage()) {
          @if (arquivoUrl()) {
            <div
              class="relative w-full h-full flex items-center justify-center overflow-hidden"
              [class.cursor-grab]="!isDragging()"
              [class.cursor-grabbing]="isDragging()"
              (mousedown)="iniciarArrasto($event)"
              (mousemove)="arrastar($event)"
              (mouseup)="finalizarArrasto()"
              (mouseleave)="finalizarArrasto()"
              (dblclick)="alternarZoomRapido($event)"
            >
              <!-- Container Transformado da Imagem (Escala, Rotação e Pan) -->
              <div
                class="relative transition-transform duration-75 select-none inline-block shadow-2xl rounded-lg border border-gov-slate-800"
                [style.transform]="transformStyle()"
                [style.transform-origin]="'center center'"
              >
                <img
                  [src]="arquivoUrl()"
                  [alt]="nomeArquivo()"
                  class="max-w-none block select-none pointer-events-none rounded max-h-[85vh]"
                  [style.filter]="filtroCss()"
                  (load)="onImageLoaded()"
                  (error)="onImageError()"
                />

                <!-- Camada de Caixas Delimitadoras da IA na Imagem -->
                <app-bounding-box-overlay
                  [boundingBoxes]="boundingBoxes()"
                  [scores]="scores()"
                  [campoAtivo]="campoAtivo()"
                  (caixaClicada)="onCaixaClicada($event)"
                >
                </app-bounding-box-overlay>
              </div>

              <!-- Dica Operacional Flutuante no Rodapé -->
              <div class="absolute bottom-2 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/10 text-[10px] text-gov-slate-400 font-medium pointer-events-none flex items-center gap-2 shadow-lg z-20">
                <span>🖱️ Arraste para mover</span>
                <span>•</span>
                <span>🔍 Roda do mouse amplia/reduz</span>
                <span>•</span>
                <span>⚡ 2x clique alterna zoom</span>
                @if (rotacao() !== 0) {
                  <span>•</span>
                  <span class="text-amber-400 font-mono font-bold">{{ rotacao() }}°</span>
                }
              </div>
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center p-12 text-gov-slate-400">
              <div class="w-8 h-8 border-2 border-purple-500 border-t-transparent rounded-full animate-spin mb-3"></div>
              <p class="text-xs text-gov-slate-400 font-medium">Carregando imagem original...</p>
            </div>
          }
        }

        <!-- CASO 2: PDF COM NGX OU IFRAME -->
        @else if (isPdf()) {
          @if (arquivoUrl()) {
            <div
              class="relative w-full h-full shadow-2xl rounded-lg border border-gov-slate-800 bg-white flex flex-col overflow-hidden transition-all"
              [ngClass]="modoConfortoVisual() ? 'comfort-paper-filter' : ''"
            >
              @if (modoVisualizador() === 'ngx') {
                <div class="relative w-full h-full">
                  <ngx-extended-pdf-viewer
                    [src]="arquivoUrl()"
                    [height]="'100%'"
                    [zoom]="zoomPercent()"
                    [showToolbar]="false"
                    [showSidebarButton]="false"
                    [showFindButton]="false"
                    [showPagingButtons]="true"
                    [showZoomButtons]="false"
                    [showPresentationModeButton]="false"
                    [showOpenFileButton]="false"
                    [showPrintButton]="false"
                    [showDownloadButton]="false"
                    [showSecondaryToolbarButton]="false"
                    [showRotateButton]="false"
                    (pageRendered)="onPdfPageRendered()"
                    class="block w-full h-[calc(100vh-120px)] min-w-[320px]"
                  >
                  </ngx-extended-pdf-viewer>

                  <!-- Camada de Caixas Delimitadoras da IA (Active Spotlight) -->
                  <app-bounding-box-overlay
                    [boundingBoxes]="boundingBoxes()"
                    [scores]="scores()"
                    [campoAtivo]="campoAtivo()"
                    (caixaClicada)="onCaixaClicada($event)"
                  >
                  </app-bounding-box-overlay>
                </div>
              } @else {
                <div class="relative w-full h-full">
                  <iframe
                    [src]="safeBlobUrl()"
                    class="w-full h-full min-h-[calc(100vh-120px)] border-0"
                    title="Visualizador PDF"
                  ></iframe>
                </div>
              }
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center p-12 text-gov-slate-400">
              <div class="w-8 h-8 border-2 border-gov-cobalt-500 border-t-transparent rounded-full animate-spin mb-3"></div>
              <p class="text-xs text-gov-slate-400 font-medium">Carregando documento fiscal original...</p>
            </div>
          }
        }

        <!-- CASO 3: HTML INLINE -->
        @else if (isHtml()) {
          @if (arquivoUrl()) {
            <div
              class="relative w-full h-full shadow-2xl rounded-lg border border-gov-slate-800 bg-white flex flex-col overflow-hidden transition-all"
              [ngClass]="modoConfortoVisual() ? 'comfort-paper-filter' : ''"
            >
              <iframe
                [src]="safeHtmlBlobUrl()"
                class="w-full h-full min-h-[calc(100vh-140px)] border-0 bg-white"
                title="Visualizador de Documento HTML GovFlow"
                sandbox="allow-same-origin allow-scripts"
              ></iframe>
            </div>
          } @else {
            <div class="flex flex-col items-center justify-center p-12 text-gov-slate-400">
              <div class="w-8 h-8 border-2 border-gov-cobalt-500 border-t-transparent rounded-full animate-spin mb-3"></div>
              <p class="text-xs text-gov-slate-400 font-medium">Carregando documento HTML...</p>
            </div>
          }
        }

        <!-- CASO 4: FORMATO NÃO SUPORTADO DIRETAMENTE -->
        @else {
          <div class="flex flex-col items-center justify-center p-12 text-center text-gov-slate-400 space-y-4">
            <div class="w-14 h-14 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center text-2xl">
              📦
            </div>
            <div>
              <p class="text-sm font-semibold text-white">{{ nomeArquivo() }}</p>
              <p class="text-xs text-gov-slate-400 mt-1">Formato não renderizável inline ({{ contentType() }})</p>
            </div>
            @if (arquivoUrl()) {
              <a
                [href]="arquivoUrl()"
                [download]="nomeArquivo()"
                class="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold"
              >
                Fazer Download do Arquivo
              </a>
            }
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    :host ::ng-deep .comfort-paper-filter {
      filter: sepia(0.22) brightness(0.92) contrast(0.96) !important;
      transition: filter 0.3s ease;
    }
  `]
})
export class MediaWorkspaceComponent {
  public readonly Math = Math;
  private readonly sanitizer = inject(DomSanitizer);

  public readonly modoVisualizador = signal<'ngx' | 'nativo'>('ngx');
  public readonly modoConfortoVisual = signal<boolean>(true);

  // Estados de Imagem
  public readonly rotacao = signal<number>(0);
  public readonly filtroAtivo = signal<FiltroVisual>('normal');
  public readonly panX = signal<number>(0);
  public readonly panY = signal<number>(0);
  public readonly isDragging = signal<boolean>(false);
  private dragStartX = 0;
  private dragStartY = 0;
  private initialPanX = 0;
  private initialPanY = 0;

  public readonly arquivoUrl = signal<string>('');
  public readonly contentType = signal<string>('application/pdf');
  public readonly nomeArquivo = signal<string>('documento.pdf');
  public readonly boundingBoxes = signal<Record<string, BoundingBox>>({});
  public readonly scores = signal<Record<string, number>>({});
  public readonly campoAtivo = signal<string | null>(null);
  public readonly zoomLevel = signal<number>(1.0);

  public readonly safeBlobUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.arquivoUrl();
    if (!url) return null;
    const separator = url.includes('#') ? '&' : '#';
    const viewerParams = `${separator}toolbar=1&navpanes=0&view=FitH`;
    return this.sanitizer.bypassSecurityTrustResourceUrl(`${url}${viewerParams}`);
  });

  public alternarModoVisualizador(): void {
    this.modoVisualizador.update(m => m === 'ngx' ? 'nativo' : 'ngx');
  }

  public alternarConfortoVisual(): void {
    this.modoConfortoVisual.update(v => !v);
  }

  // Rotação da Imagem (em saltos de 90 graus)
  public rotacionar(deltaGraus: number): void {
    this.rotacao.update(r => (r + deltaGraus + 360) % 360);
  }

  public resetarRotacao(): void {
    this.rotacao.set(0);
  }

  // Filtros de Leitura e Contraste
  public alternarFiltroVisual(): void {
    const filtros: FiltroVisual[] = ['normal', 'conforto', 'alto-contraste', 'grayscale', 'invertido'];
    const idx = filtros.indexOf(this.filtroAtivo());
    const proximo = filtros[(idx + 1) % filtros.length];
    this.filtroAtivo.set(proximo);
  }

  public readonly filtroCss = computed<string>(() => {
    switch (this.filtroAtivo()) {
      case 'conforto':
        return 'sepia(0.24) brightness(0.92) contrast(0.96)';
      case 'alto-contraste':
        return 'contrast(1.85) brightness(1.08) saturate(1.2)';
      case 'grayscale':
        return 'grayscale(1) contrast(1.4) brightness(0.98)';
      case 'invertido':
        return 'invert(0.92) hue-rotate(180deg) contrast(1.25)';
      case 'normal':
      default:
        return 'none';
    }
  });

  public readonly rotuloFiltro = computed<string>(() => {
    switch (this.filtroAtivo()) {
      case 'conforto': return 'Conforto';
      case 'alto-contraste': return 'Alto Contraste';
      case 'grayscale': return 'P&B / Nitidez';
      case 'invertido': return 'Invertido';
      case 'normal': default: return 'Normal';
    }
  });

  public readonly iconeFiltro = computed<string>(() => {
    switch (this.filtroAtivo()) {
      case 'conforto': return '☕';
      case 'alto-contraste': return '⚡';
      case 'grayscale': return '🔲';
      case 'invertido': return '🌓';
      case 'normal': default: return '🎨';
    }
  });

  // Transform CSS composto para Imagem
  public readonly transformStyle = computed<string>(() => {
    return `translate(${this.panX()}px, ${this.panY()}px) scale(${this.zoomLevel()}) rotate(${this.rotacao()}deg)`;
  });

  // Arraste / Pan
  public iniciarArrasto(e: MouseEvent): void {
    if (e.button !== 0) return; // apenas botão esquerdo
    this.isDragging.set(true);
    this.dragStartX = e.clientX;
    this.dragStartY = e.clientY;
    this.initialPanX = this.panX();
    this.initialPanY = this.panY();
    e.preventDefault();
  }

  public arrastar(e: MouseEvent): void {
    if (!this.isDragging()) return;
    const deltaX = e.clientX - this.dragStartX;
    const deltaY = e.clientY - this.dragStartY;
    this.panX.set(this.initialPanX + deltaX);
    this.panY.set(this.initialPanY + deltaY);
  }

  public finalizarArrasto(): void {
    this.isDragging.set(false);
  }

  // Roda do mouse para Zoom
  public aoRolarMouse(e: WheelEvent): void {
    if (!this.isImage()) return;
    e.preventDefault();
    const delta = e.deltaY < 0 ? 0.12 : -0.12;
    this.ajustarZoom(delta);
  }

  // Duplo clique alterna entre 1.0 e 2.0
  public alternarZoomRapido(e: MouseEvent): void {
    if (!this.isImage()) return;
    e.preventDefault();
    if (this.zoomLevel() > 1.2) {
      this.zoomLevel.set(1.0);
      this.panX.set(0);
      this.panY.set(0);
    } else {
      this.zoomLevel.set(2.0);
    }
  }

  public zoom100(): void {
    this.zoomLevel.set(1.0);
  }

  public resetarVisao(): void {
    this.zoomLevel.set(1.0);
    this.rotacao.set(0);
    this.panX.set(0);
    this.panY.set(0);
  }

  @Input()
  set url(val: string) {
    this.arquivoUrl.set(val);
    this.resetarVisao();
  }

  @Input()
  set mimeType(val: string | null | undefined) {
    this.contentType.set(val || 'application/pdf');
  }

  @Input()
  set filename(val: string | null | undefined) {
    this.nomeArquivo.set(val || 'documento.pdf');
  }

  @Input()
  set boxes(val: Record<string, BoundingBox> | null | undefined) {
    this.boundingBoxes.set(val || {});
  }

  @Input()
  set fieldScores(val: Record<string, number> | null | undefined) {
    this.scores.set(val || {});
  }

  @Input()
  set activeField(val: string | null | undefined) {
    this.campoAtivo.set(val || null);
  }

  @Input()
  set status(val: string | null | undefined) {
    this.statusDoc.set(val || '');
  }
  public readonly statusDoc = signal<string>('');

  @Output() fieldSelected = new EventEmitter<string>();

  // Priorização Estrita de Imagem
  public readonly isImage = computed(() => {
    const ct = this.contentType().toLowerCase();
    const nome = this.nomeArquivo().toLowerCase();
    return ct.startsWith('image/') ||
      nome.endsWith('.png') ||
      nome.endsWith('.jpg') ||
      nome.endsWith('.jpeg') ||
      nome.endsWith('.webp') ||
      nome.endsWith('.bmp') ||
      nome.endsWith('.svg');
  });

  public readonly isHtml = computed(() => {
    if (this.isImage()) return false;
    const ct = this.contentType().toLowerCase();
    const nome = this.nomeArquivo().toLowerCase();
    return ct.includes('html') || nome.endsWith('.html') || nome.endsWith('.htm');
  });

  public readonly isPdf = computed(() => {
    if (this.isImage() || this.isHtml()) return false;
    const ct = this.contentType().toLowerCase();
    const nome = this.nomeArquivo().toLowerCase();
    return ct.includes('pdf') || nome.endsWith('.pdf');
  });

  public readonly emAnaliseIA = computed(() => {
    const s = this.statusDoc();
    return s === 'EM_ANALISE_IA' || s === 'RECEBIDO' || Object.keys(this.boundingBoxes()).length === 0;
  });

  public readonly safeHtmlBlobUrl = computed<SafeResourceUrl | null>(() => {
    const url = this.arquivoUrl();
    if (!url) return null;
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  });

  public readonly zoomPercent = computed(() => {
    return `${Math.round(this.zoomLevel() * 100)}%`;
  });

  public ajustarZoom(delta: number): void {
    this.zoomLevel.update(z => Math.max(0.2, Math.min(4.0, Math.round((z + delta) * 100) / 100)));
  }

  public resetarZoom(): void {
    this.zoomLevel.set(1.0);
  }

  public onPdfPageRendered(): void {
    // Sincronização após renderização da página
  }

  public onImageLoaded(): void {
    // Imagem carregada com sucesso
  }

  public onImageError(): void {
    console.warn('Falha ao renderizar imagem inline:', this.nomeArquivo());
  }

  public onCaixaClicada(key: string): void {
    this.fieldSelected.emit(key);
  }
}
