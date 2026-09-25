import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RevisaoApiService } from '../../services/revisao-api.service';
import { Documento, PageResponse } from '../../model/documento.model';
import { StatusPillComponent } from '../../../../shared/ui/status-pill/status-pill.component';
import { ConfidenceBadgeComponent } from '../../../../shared/ui/confidence-badge/confidence-badge.component';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-documentos-list-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    StatusPillComponent,
    ConfidenceBadgeComponent,
    CurrencyBrlPipe
  ],
  template: `
    <div class="p-6 max-w-7xl mx-auto space-y-6">
      <!-- Cabeçalho da Esteira -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div class="flex items-center gap-2">
            <h2 class="text-xl font-bold text-white tracking-tight">Esteira de Documentos Fiscais</h2>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-500/15 text-emerald-400 border border-emerald-500/25">
              WhatsApp Inbound Ativo
            </span>
          </div>
          <p class="text-xs text-gov-slate-400 mt-0.5">Documentos recebidos via WhatsApp e processados pelo motor de IA multimodal</p>
        </div>

        <button
          type="button"
          (click)="carregar()"
          class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-white border border-white/10 transition-colors inline-flex items-center gap-2"
        >
          <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
            <path d="M3 3v5h5"/>
            <path d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"/>
            <path d="M16 21h5v-5"/>
          </svg>
          <span>Atualizar Lista</span>
        </button>
      </div>

      <!-- Tabela -->
      <div class="bg-[#111827] border border-white/10 rounded-xl overflow-hidden shadow-sm">
        <table class="w-full text-left text-xs text-gov-slate-300">
          <thead class="bg-[#0A0E17] text-gov-slate-400 uppercase tracking-wider font-semibold border-b border-white/10 text-[10px]">
              <tr>
                <th class="py-3 px-4">Documento / NF</th>
                <th class="py-3 px-4">Credor</th>
                <th class="py-3 px-4">Valor Bruto</th>
                <th class="py-3 px-4">Confiança IA</th>
                <th class="py-3 px-4">Status</th>
                <th class="py-3 px-4 text-right">Ação</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gov-slate-800/60">
              @for (doc of documentos(); track doc.id) {
                <tr class="hover:bg-gov-slate-800/30 transition-colors">
                  <td class="py-3.5 px-4 font-mono font-medium text-white">
                    {{ doc.extracaoSugerida?.numeroDocumento || doc.nomeArquivoOriginal }}
                  </td>
                  <td class="py-3.5 px-4">
                    {{ doc.extracaoSugerida?.razaoSocialCredor || 'Identificando...' }}
                  </td>
                  <td class="py-3.5 px-4 font-mono text-emerald-400 font-semibold">
                    {{ doc.extracaoSugerida?.valorBruto | currencyBrl }}
                  </td>
                  <td class="py-3.5 px-4">
                    <app-confidence-badge [score]="doc.extracaoSugerida?.confidenceScoreGeral"></app-confidence-badge>
                  </td>
                  <td class="py-3.5 px-4">
                    <app-status-pill [status]="doc.status"></app-status-pill>
                  </td>
                  <td class="py-3.5 px-4 text-right">
                    <a
                      [routerLink]="['/documentos', doc.id, 'revisar']"
                      class="inline-flex items-center gap-1 px-3 py-1.5 rounded-md bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold transition-colors shadow-sm"
                    >
                      <span>Auditar Lado a Lado</span>
                      <span>→</span>
                    </a>
                  </td>
                </tr>
              }

              @if (documentos().length === 0 && !carregando()) {
                <tr>
                  <td colspan="6" class="py-12 text-center text-gov-slate-500">
                    Nenhum documento encontrado no momento.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
    </div>
  `
})
export class DocumentosListPageComponent implements OnInit {
  private readonly api = inject(RevisaoApiService);
  public readonly auth = inject(AuthService);

  public readonly documentos = signal<Documento[]>([]);
  public readonly carregando = signal<boolean>(false);

  public ngOnInit(): void {
    this.carregar();
  }

  public carregar(): void {
    this.carregando.set(true);
    this.api.listarDocumentos(undefined, 0, 50).subscribe({
      next: (page) => {
        const lista = page.items || page.content || [];
        this.documentos.set(lista);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
      }
    });
  }
}
