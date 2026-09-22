import { Component, Input, Output, EventEmitter, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { BoundingBoxOverlayComponent } from '../bounding-box-overlay/bounding-box-overlay.component';
import { BoundingBox } from '../../model/documento.model';

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
      <div class="flex items-center justify-between px-3 py-1.5 bg-gov-slate-900 border-b border-gov-slate-800 text-xs text-gov-slate-300 z-20">
        <div class="flex items-center gap-2">
          <span class="font-mono text-gov-slate-200 font-semibold truncate max-w-[220px]" [title]="nomeArquivo()">
            {{ nomeArquivo() }}
          </span>
          <span class="px-1.5 py-0.5 rounded bg-gov-slate-800 text-gov-cobalt-400 font-mono text-[10px] uppercase font-bold border border-gov-slate-700">
            {{ isPdf() ? 'PDF' : 'IMAGEM' }}
          </span>
        </div>

        <div class="flex items-center gap-1.5">
          <!-- Botão de Conforto Visual / Anti-Glare (Modo Leitura Suave) -->
          <button
            type="button"
            (click)="alternarConfortoVisual()"
            class="px-2 py-1 rounded text-[11px] font-medium transition-all flex items-center gap-1 border"
            [ngClass]="modoConfortoVisual() ? 'bg-amber-950/60 text-amber-300 border-amber-500/40 shadow-sm' : 'bg-gov-slate-800 text-gov-slate-400 border-gov-slate-700 hover:text-white'"
            title="Alternar filtro de papel suave para reduzir contraste com o tema escuro e evitar fadiga visual"
          >
            <span>☕</span>
            <span>{{ modoConfortoVisual() ? 'Conforto Ativo' : 'Conforto Off' }}</span>
          </button>

          <div class="h-3.5 w-px bg-gov-slate-700 mx-1"></div>

          <!-- Controles de Zoom -->
          <button
            type="button"
            (click)="ajustarZoom(-0.1)"
            class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 font-bold"
            title="Reduzir Zoom"
          >
            -
          </button>
          <span class="font-mono text-xs px-1 text-gov-slate-300 min-w-[40px] text-center">{{ Math.round(zoomLevel() * 100) }}%</span>
          <button
            type="button"
            (click)="ajustarZoom(0.1)"
            class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 font-bold"
            title="Aumentar Zoom"
          >
            +
          </button>
          <button
            type="button"
            (click)="resetarZoom()"
            class="px-2 py-0.5 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 text-[11px]"
            title="Ajustar ao tamanho ideal"
          >
            Ajustar
          </button>

          <div class="h-3.5 w-px bg-gov-slate-700 mx-1"></div>

          <!-- Alternar Modo Visualizador -->
          <button
            type="button"
            (click)="alternarModoVisualizador()"
            class="px-2 py-0.5 rounded border border-gov-slate-700 bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 text-[11px]"
            title="Alternar entre visualizador interativo com caixas e nativo"
          >
            {{ modoVisualizador() === 'ngx' ? 'Interativo' : 'Nativo' }}
          </button>
        </div>
      </div>

      <!-- Área de Visualização Principal do Documento -->
      <div class="relative flex-1 overflow-auto flex items-center justify-center p-3 bg-gov-slate-950/90">
        @if (isPdf()) {
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
        } @else {
          <!-- Renderizador de Imagem (JPEG/PNG do WhatsApp) -->
          <div
            class="relative max-w-full max-h-full inline-block shadow-2xl rounded border border-gov-slate-800 overflow-hidden transition-all"
            [ngClass]="modoConfortoVisual() ? 'comfort-paper-filter' : ''"
            [style.transform]="'scale(' + zoomLevel() + ')'"
            [style.transform-origin]="'center top'"
          >
            <img
              [src]="arquivoUrl()"
              [alt]="nomeArquivo()"
              class="max-w-none object-contain select-none max-h-[calc(100vh-130px)]"
              (load)="onImageLoaded()"
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

  @Input()
  set url(val: string) {
    this.arquivoUrl.set(val);
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

  @Output() fieldSelected = new EventEmitter<string>();

  public readonly isPdf = computed(() => {
    return this.contentType().includes('pdf') || this.nomeArquivo().toLowerCase().endsWith('.pdf');
  });

  public readonly zoomPercent = computed(() => {
    return `${Math.round(this.zoomLevel() * 100)}%`;
  });

  public ajustarZoom(delta: number): void {
    this.zoomLevel.update(z => Math.max(0.4, Math.min(2.5, Math.round((z + delta) * 10) / 10)));
  }

  public resetarZoom(): void {
    this.zoomLevel.set(1.0);
  }

  public onPdfPageRendered(): void {
    // Sincronização após renderização da página
  }

  public onImageLoaded(): void {
    // Imagem pronta
  }

  public onCaixaClicada(key: string): void {
    this.fieldSelected.emit(key);
  }
}
