import { Injectable, signal } from '@angular/core';

export interface ToastMessage {
  id: string;
  tipo: 'sucesso' | 'erro' | 'aviso' | 'info';
  titulo: string;
  mensagem?: string;
  duracaoMs?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly _toasts = signal<ToastMessage[]>([]);
  public readonly toasts = this._toasts.asReadonly();

  public sucesso(titulo: string, mensagem?: string, duracaoMs = 4000): void {
    this.adicionar({ id: this.gerarId(), tipo: 'sucesso', titulo, mensagem, duracaoMs });
  }

  public erro(titulo: string, mensagem?: string, duracaoMs = 6000): void {
    this.adicionar({ id: this.gerarId(), tipo: 'erro', titulo, mensagem, duracaoMs });
  }

  public aviso(titulo: string, mensagem?: string, duracaoMs = 5000): void {
    this.adicionar({ id: this.gerarId(), tipo: 'aviso', titulo, mensagem, duracaoMs });
  }

  public remover(id: string): void {
    this._toasts.update(lista => lista.filter(t => t.id !== id));
  }

  private adicionar(toast: ToastMessage): void {
    this._toasts.update(lista => [...lista, toast]);

    if (toast.duracaoMs && toast.duracaoMs > 0) {
      setTimeout(() => {
        this.remover(toast.id);
      }, toast.duracaoMs);
    }
  }

  private gerarId(): string {
    return Math.random().toString(36).substring(2, 9);
  }
}
