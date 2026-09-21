import { Component, Input, Output, EventEmitter, computed, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { BoundingBoxOverlayComponent } from '../bounding-box-overlay/bounding-box-overlay.component';
import { BoundingBox } from '../../model/documento.model';

@Component({
  selector: 'app-media-workspace',
  standalone: true,
  imports: [CommonModule, NgxExtendedPdfViewerModule, BoundingBoxOverlayComponent],
  template: `
    <div class="relative w-full h-full flex flex-col bg-gov-slate-950 overflow-hidden select-none">
      <!-- Toolbar superior do visualizador -->
      <div class="flex items-center justify-between px-3 py-1.5 bg-gov-slate-900 border-b border-gov-slate-800 text-xs text-gov-slate-300">
        <div class="flex items-center gap-2">
          <span class="font-mono text-gov-slate-400 font-semibold">{{ nomeArquivo() }}</span>
          <span class="px-1.5 py-0.5 rounded bg-gov-slate-800 text-gov-slate-400 font-mono text-[10px] uppercase">
            {{ isPdf() ? 'PDF' : 'IMAGEM' }}
          </span>
        </div>

        <div class="flex items-center gap-1">
          <button
            type="button"
            (click)="ajustarZoom(-0.1)"
            class="px-2 py-1 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 transition-colors"
            title="Reduzir Zoom"
          >
            -
          </button>
          <span class="font-mono text-xs px-1 text-gov-slate-300">{{ Math.round(zoomLevel() * 100) }}%</span>
          <button
            type="button"
            (click)="ajustarZoom(0.1)"
            class="px-2 py-1 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-200 transition-colors"
            title="Aumentar Zoom"
          >
            +
          </button>
          <button
            type="button"
            (click)="resetarZoom()"
            class="px-2 py-1 rounded bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 transition-colors text-[11px]"
          >
            Ajustar
          </button>
        </div>
      </div>

      <!-- Área de Visualização Principal -->
      <div class="relative flex-1 overflow-auto flex items-center justify-center p-4 bg-gov-slate-950">
        @if (isPdf()) {
          <div class="relative max-w-full max-h-full shadow-2xl rounded border border-gov-slate-800 bg-white">
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
              class="block w-full h-[calc(100vh-140px)] min-w-[500px]"
            >
            </ngx-extended-pdf-viewer>

            <!-- Camada de Bounding Boxes sincronizada sobre o documento -->
            <app-bounding-box-overlay
              [boundingBoxes]="boundingBoxes()"
              [scores]="scores()"
              [campoAtivo]="campoAtivo()"
              (caixaClicada)="onCaixaClicada($event)"
            >
            </app-bounding-box-overlay>
          </div>
        } @else {
          <!-- Renderizador de Imagem (JPEG/PNG do WhatsApp) -->
          <div
            class="relative max-w-full max-h-full inline-block shadow-2xl rounded border border-gov-slate-800 overflow-hidden"
            [style.transform]="'scale(' + zoomLevel() + ')'"
            [style.transform-origin]="'center top'"
          >
            <img
              [src]="arquivoUrl()"
              [alt]="nomeArquivo()"
              class="max-w-none object-contain select-none max-h-[calc(100vh-160px)]"
              (load)="onImageLoaded()"
            />

            <!-- Camada de Bounding Boxes sobre a Imagem -->
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
  `
})
export class MediaWorkspaceComponent {
  public readonly Math = Math;

  public readonly arquivoUrl = signal<string>('');
  public readonly contentType = signal<string>('application/pdf');
  public readonly nomeArquivo = signal<string>('documento.pdf');
  public readonly boundingBoxes = signal<Record<string, BoundingBox>>({});
  public readonly scores = signal<Record<string, number>>({});
  public readonly campoAtivo = signal<string | null>(null);
  public readonly zoomLevel = signal<number>(1.0);

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
