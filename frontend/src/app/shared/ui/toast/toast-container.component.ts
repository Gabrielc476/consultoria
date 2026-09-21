import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from '../../../core/ui/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="fixed bottom-5 right-5 z-[9999] flex flex-col gap-2 max-w-md w-full pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex items-start gap-3 p-4 rounded-lg shadow-2xl border transition-all duration-300 transform translate-y-0"
          [ngClass]="getCardClasses(toast.tipo)"
        >
          <span class="text-xl leading-none mt-0.5">{{ getIcon(toast.tipo) }}</span>
          <div class="flex-1">
            <h4 class="text-sm font-semibold">{{ toast.titulo }}</h4>
            @if (toast.mensagem) {
              <p class="text-xs mt-0.5 opacity-90">{{ toast.mensagem }}</p>
            }
          </div>
          <button
            type="button"
            (click)="toastService.remover(toast.id)"
            class="text-xs opacity-60 hover:opacity-100 transition-opacity p-1"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  public readonly toastService = inject(ToastService);

  getCardClasses(tipo: ToastMessage['tipo']): string {
    switch (tipo) {
      case 'sucesso':
        return 'bg-emerald-900/90 text-emerald-100 border-emerald-500 backdrop-blur-md';
      case 'erro':
        return 'bg-rose-900/90 text-rose-100 border-rose-500 backdrop-blur-md';
      case 'aviso':
        return 'bg-amber-900/90 text-amber-100 border-amber-500 backdrop-blur-md';
      default:
        return 'bg-slate-900/90 text-slate-100 border-slate-700 backdrop-blur-md';
    }
  }

  getIcon(tipo: ToastMessage['tipo']): string {
    switch (tipo) {
      case 'sucesso': return '✓';
      case 'erro': return '✕';
      case 'aviso': return '⚠';
      default: return 'ℹ';
    }
  }
}
