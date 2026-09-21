import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { DocumentoResumo } from '../../model/documento.model';

@Component({
  selector: 'app-queue-drawer',
  standalone: true,
  imports: [CommonModule, CurrencyBrlPipe],
  template: `
    @if (state.drawerFilaAberto()) {
      <!-- Backdrop -->
      <div
        class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 transition-opacity"
        (click)="state.toggleDrawer()"
      ></div>

      <!-- Drawer Panel -->
      <div class="fixed top-0 right-0 h-full w-96 max-w-full bg-gov-slate-900 border-l border-gov-slate-800 z-50 flex flex-col shadow-2xl transform transition-transform duration-300">
        <!-- Header -->
        <div class="px-5 py-4 border-b border-gov-slate-800 flex items-center justify-between">
          <div>
            <h3 class="text-sm font-bold text-white flex items-center gap-2">
              <span>📋</span> Fila de Conferência
            </h3>
            <span class="text-xs text-gov-slate-400">
              {{ state.totalPendentes() }} documento(s) aguardando conferência
            </span>
          </div>

          <button
            type="button"
            (click)="state.toggleDrawer()"
            class="p-1 rounded text-gov-slate-400 hover:text-white hover:bg-gov-slate-800 transition-colors"
          >
            ✕
          </button>
        </div>

        <!-- Lista de Documentos -->
        <div class="flex-1 overflow-y-auto p-4 space-y-2">
          @for (doc of state.filaPendentes(); track doc.id) {
            <div
              (click)="selecionarDocumento(doc)"
              class="p-3 rounded-lg border transition-all cursor-pointer"
              [ngClass]="doc.id === state.documentoAtual()?.id
                ? 'bg-gov-cobalt-950/40 border-gov-cobalt-500/80 ring-1 ring-gov-cobalt-500'
                : 'bg-gov-slate-800/40 border-gov-slate-800 hover:bg-gov-slate-800/80 hover:border-gov-slate-700'"
            >
              <div class="flex items-center justify-between mb-1">
                <span class="font-mono text-xs font-bold text-white truncate max-w-[200px]">
                  {{ doc.numeroDocumento || doc.nomeArquivoOriginal }}
                </span>
                <span class="text-[10px] px-1.5 py-0.5 rounded font-semibold bg-amber-950 text-amber-300 border border-amber-500/30">
                  Pendente
                </span>
              </div>

              <div class="text-xs text-gov-slate-400 truncate mb-1">
                {{ doc.credor || 'Credor a identificar' }}
              </div>

              <div class="flex items-center justify-between text-xs pt-1 border-t border-gov-slate-800/60">
                <span class="font-mono font-bold text-emerald-400">
                  {{ (doc.valorBruto || 0) | currencyBrl }}
                </span>
                <span class="text-[10px] text-gov-slate-500">
                  {{ doc.createdAt | date:'dd/MM HH:mm' }}
                </span>
              </div>
            </div>
          }

          @if (state.filaPendentes().length === 0) {
            <div class="text-center py-12 text-gov-slate-500 text-xs">
              <span class="text-2xl block mb-2">🎉</span>
              Nenhum documento pendente na fila!
            </div>
          }
        </div>

        <!-- Rodapé do Drawer -->
        <div class="p-4 border-t border-gov-slate-800 bg-gov-slate-900/80 text-center">
          <button
            type="button"
            (click)="state.carregarFila()"
            class="w-full py-2 px-3 rounded-md bg-gov-slate-800 hover:bg-gov-slate-700 text-xs text-gov-slate-200 font-semibold transition-colors"
          >
            Atualizar Fila
          </button>
        </div>
      </div>
    }
  `
})
export class QueueDrawerComponent {
  public readonly state = inject(RevisaoStateService);
  private readonly router = inject(Router);

  public selecionarDocumento(doc: DocumentoResumo): void {
    this.router.navigate(['/documentos', doc.id, 'revisar']);
    this.state.carregarDocumento(doc.id);
    this.state.toggleDrawer();
  }
}
