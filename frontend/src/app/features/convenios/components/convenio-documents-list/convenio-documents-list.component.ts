import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { ConfidenceBadgeComponent } from '../../../../shared/ui/confidence-badge/confidence-badge.component';

export interface DocumentoConvenioResumo {
  id: string;
  numeroDocumento: string;
  tipo: string;
  credorRazaoSocial: string;
  valorBruto: number;
  dataRecebimento: string;
  origem: 'WHATSAPP' | 'MANUAL';
  confiancaScore: number;
  status: 'EM_CONFERENCIA' | 'PRONTO_PARA_TRANSFEREGOV' | 'REJEITADO';
}

@Component({
  selector: 'app-convenio-documents-list',
  standalone: true,
  imports: [CommonModule, CurrencyBrlPipe, ConfidenceBadgeComponent],
  template: `
    <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm h-full flex flex-col justify-between">
      <div>
        <div class="flex items-center justify-between pb-3 border-b border-white/10 mb-3">
          <div class="flex items-center gap-2">
            <div class="w-2 h-2 rounded-full bg-emerald-500"></div>
            <h4 class="text-xs font-semibold uppercase tracking-wider text-gov-slate-300">
              Documentos Hábeis Recebidos (WhatsApp)
            </h4>
          </div>
          <span class="text-[11px] font-mono text-gov-slate-400">
            {{ documentos.length }} pendentes
          </span>
        </div>

        <div class="divide-y divide-white/5 overflow-y-auto max-h-72">
          @for (doc of documentos; track doc.id) {
            <div class="py-3 flex items-center justify-between gap-3 hover:bg-white/[0.02] px-2 rounded-lg transition-colors group">
              <div class="flex items-center gap-3 min-w-0">
                <!-- Ícone WhatsApp / Documento -->
                <div class="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400 shrink-0">
                  <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                </div>
                <div class="min-w-0">
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-xs font-semibold text-white truncate">
                      {{ doc.numeroDocumento }}
                    </span>
                    <span class="text-[10px] text-gov-slate-400">{{ doc.tipo }}</span>
                  </div>
                  <div class="text-[11px] text-gov-slate-400 truncate mt-0.5">
                    {{ doc.credorRazaoSocial }}
                  </div>
                </div>
              </div>

              <div class="flex items-center gap-3 shrink-0">
                <div class="text-right">
                  <div class="font-mono text-xs font-semibold text-gov-slate-100">
                    {{ doc.valorBruto | currencyBrl }}
                  </div>
                  <div class="text-[10px] text-gov-slate-500">
                    {{ doc.dataRecebimento }}
                  </div>
                </div>

                <app-confidence-badge [score]="doc.confiancaScore"></app-confidence-badge>

                <button
                  type="button"
                  (click)="revisar(doc.id)"
                  class="px-3 py-1.5 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white text-xs font-semibold shadow-sm transition-all focus:outline-none cursor-pointer flex items-center gap-1.5"
                >
                  <span>Revisar</span>
                  <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="9 18 15 12 9 6"/>
                  </svg>
                </button>
              </div>
            </div>
          } @empty {
            <div class="py-8 text-center text-xs text-gov-slate-500">
              Nenhum documento pendente para este convênio.
            </div>
          }
        </div>
      </div>

      <div class="pt-3 border-t border-white/10 text-right">
        <a
          routerLink="/documentos"
          class="text-xs text-gov-cobalt-400 hover:text-gov-cobalt-300 transition-colors hover:underline inline-flex items-center gap-1"
        >
          <span>Ver todos os documentos da esteira</span>
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="9 18 15 12 9 6"/>
          </svg>
        </a>
      </div>
    </div>
  `
})
export class ConvenioDocumentsListComponent {
  private readonly router = inject(Router);

  @Input() documentos: DocumentoConvenioResumo[] = [];

  revisar(id: string): void {
    this.router.navigate(['/documentos', id, 'revisar']);
  }
}
