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
    <div class="min-h-screen w-full bg-gov-slate-950 text-gov-slate-100 flex flex-col font-sans">
      <!-- Topo -->
      <header class="h-16 bg-gov-slate-900 border-b border-gov-slate-800 px-8 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-gov-cobalt-600 flex items-center justify-center font-bold text-white">G</div>
          <h1 class="text-lg font-bold text-white">GovFlow <span class="text-xs text-gov-slate-400 font-normal">| Documentos Fiscais</span></h1>
        </div>

        <div class="flex items-center gap-4 text-xs">
          <span class="text-gov-slate-300">{{ auth.usuario()?.nome || 'Analista' }}</span>
          <button (click)="auth.logout()" class="text-rose-400 hover:underline">Sair</button>
        </div>
      </header>

      <!-- Conteúdo -->
      <main class="flex-1 p-8 max-w-7xl mx-auto w-full space-y-6">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="text-xl font-bold text-white">Esteira de Documentos</h2>
            <p class="text-xs text-gov-slate-400">Documentos recebidos via WhatsApp e processados pelo motor de IA</p>
          </div>

          <button
            type="button"
            (click)="carregar()"
            class="px-4 py-2 rounded-lg bg-gov-slate-800 hover:bg-gov-slate-700 text-xs font-semibold text-white transition-colors"
          >
            Atualizar Lista
          </button>
        </div>

        <!-- Tabela -->
        <div class="bg-gov-slate-900 border border-gov-slate-800 rounded-xl overflow-hidden shadow-xl">
          <table class="w-full text-left text-xs text-gov-slate-300">
            <thead class="bg-gov-slate-950 text-gov-slate-400 uppercase tracking-wider font-semibold border-b border-gov-slate-800">
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
      </main>
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
