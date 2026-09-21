import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RevisaoStateService } from '../../services/revisao-state.service';

@Component({
  selector: 'app-rejection-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (state.modalRejeicaoAberto()) {
      <div class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
        <div class="w-full max-w-lg bg-gov-slate-900 border border-gov-slate-800 rounded-xl shadow-2xl overflow-hidden animate-spotlight">
          <!-- Header -->
          <div class="px-6 py-4 border-b border-gov-slate-800 flex items-center justify-between bg-rose-950/20">
            <h3 class="text-sm font-bold text-rose-300 flex items-center gap-2">
              <span>⚠️</span> Justificativa de Rejeição
            </h3>
            <button
              type="button"
              (click)="state.fecharModalRejeicao()"
              class="text-gov-slate-400 hover:text-white p-1"
            >
              ✕
            </button>
          </div>

          <!-- Body -->
          <div class="p-6 space-y-4 text-xs text-gov-slate-300">
            <p>
              Ao rejeitar este documento, ele será marcado como inconsistente e removido da esteira para o Transferegov.
              O motivo ficará registrado na trilha de auditoria para o gestor municipal.
            </p>

            <div>
              <label for="txt-motivo" class="block font-semibold text-gov-slate-200 mb-1">
                Motivo da Rejeição (obrigatório):
              </label>
              <textarea
                id="txt-motivo"
                rows="4"
                [(ngModel)]="motivo"
                class="w-full bg-gov-slate-950 border border-gov-slate-700 rounded-lg p-3 text-sm text-white focus:outline-none focus:border-rose-500 focus:ring-1 focus:ring-rose-500 transition-colors"
                placeholder="Ex: Documento ilegível, divergência de CNPJ com o contrato, falta de ateste do fiscal de obras..."
              ></textarea>
              <span class="text-[10px] text-gov-slate-500 mt-1 block">Mínimo de 5 caracteres.</span>
            </div>
          </div>

          <!-- Footer -->
          <div class="px-6 py-4 border-t border-gov-slate-800 bg-gov-slate-950/60 flex items-center justify-end gap-3">
            <button
              type="button"
              (click)="state.fecharModalRejeicao()"
              class="px-4 py-2 rounded-lg bg-gov-slate-800 hover:bg-gov-slate-700 text-gov-slate-300 font-semibold text-xs transition-colors"
            >
              Cancelar
            </button>

            <button
              type="button"
              (click)="confirmarRejeicao()"
              [disabled]="state.salvando() || motivo().trim().length < 5"
              class="px-5 py-2 rounded-lg bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs transition-colors shadow-lg shadow-rose-950 disabled:opacity-40"
            >
              @if (state.salvando()) {
                <span>Rejeitando...</span>
              } @else {
                <span>Confirmar Rejeição</span>
              }
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class RejectionDialogComponent {
  public readonly state = inject(RevisaoStateService);
  public readonly motivo = signal<string>('');

  public confirmarRejeicao(): void {
    if (this.motivo().trim().length >= 5) {
      this.state.rejeitar(this.motivo().trim());
      this.motivo.set('');
    }
  }
}
