import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';
import { RevisaoApiService } from '../../../revisao-documento/services/revisao-api.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen w-full flex items-center justify-center bg-gov-slate-950 p-4 font-sans relative overflow-hidden">
      <!-- Fundo decorativo sutil -->
      <div class="absolute inset-0 bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-gov-cobalt-950/40 via-gov-slate-950 to-gov-slate-950"></div>

      <!-- Card Central de Login -->
      <div class="relative w-full max-w-md bg-gov-slate-900 border border-gov-slate-800 rounded-2xl shadow-2xl p-8 z-10 space-y-6">
        <!-- Logo e Cabeçalho -->
        <div class="text-center space-y-2">
          <div class="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-gov-cobalt-600 text-white font-black text-2xl shadow-lg shadow-gov-cobalt-900/50 mb-2">
            G
          </div>
          <h1 class="text-2xl font-black text-white tracking-tight">
            Gov<span class="text-gov-cobalt-400">Flow</span>
          </h1>
          <p class="text-xs text-gov-slate-400">
            Copiloto de Gestão do Transferegov para Consultorias Municipais
          </p>
        </div>

        <!-- Formulário de Login -->
        <form (ngSubmit)="onSubmit()" class="space-y-4">
          @if (erro()) {
            <div class="p-3 rounded-lg bg-rose-950/60 border border-rose-600/80 text-rose-200 text-xs flex items-center gap-2">
              <span>⚠️</span>
              <span>{{ erro() }}</span>
            </div>
          }

          <div>
            <label for="txt-email" class="block text-xs font-semibold text-gov-slate-300 mb-1">
              E-mail do Analista
            </label>
            <input
              id="txt-email"
              type="email"
              [(ngModel)]="email"
              name="email"
              required
              class="w-full bg-gov-slate-950 border border-gov-slate-700 rounded-lg px-3 py-2.5 text-sm text-white placeholder-gov-slate-500 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              placeholder="analista@govflow.com.br"
            />
          </div>

          <div>
            <label for="txt-senha" class="block text-xs font-semibold text-gov-slate-300 mb-1">
              Senha de Acesso
            </label>
            <input
              id="txt-senha"
              type="password"
              [(ngModel)]="senha"
              name="senha"
              required
              class="w-full bg-gov-slate-950 border border-gov-slate-700 rounded-lg px-3 py-2.5 text-sm text-white placeholder-gov-slate-500 focus:outline-none focus:border-gov-cobalt-500 focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            [disabled]="carregando()"
            class="w-full py-3 px-4 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-bold text-sm tracking-wide transition-colors shadow-lg shadow-gov-cobalt-950 disabled:opacity-50 flex items-center justify-center gap-2"
          >
            @if (carregando()) {
              <span class="animate-spin">⟳</span>
              <span>Autenticando...</span>
            } @else {
              <span>Acessar Painel de Conferência</span>
              <span>→</span>
            }
          </button>
        </form>

        <!-- Botão de Preenchimento Rápido (Dev / MVP Demo) -->
        <div class="pt-4 border-t border-gov-slate-800 text-center">
          <button
            type="button"
            (click)="preencherDemo()"
            class="text-xs text-gov-cobalt-400 hover:text-gov-cobalt-300 font-medium transition-colors"
          >
            ⚡ Preencher com Credenciais de Demonstração
          </button>
        </div>
      </div>
    </div>
  `
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly revisaoApi = inject(RevisaoApiService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  public email = 'analista@govflow.com.br';
  public senha = 'govflow123';
  public readonly carregando = signal<boolean>(false);
  public readonly erro = signal<string | null>(null);

  public preencherDemo(): void {
    this.email = 'analista@govflow.com.br';
    this.senha = 'govflow123';
  }

  public onSubmit(): void {
    if (!this.email || !this.senha) {
      this.erro.set('Preencha o e-mail e a senha.');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    this.authService.login(this.email, this.senha).subscribe({
      next: () => {
        this.toast.sucesso('Autenticado com sucesso!', 'Carregando fila de conferência...');

        // Busca o primeiro documento pendente para direcionamento imediato
        this.revisaoApi.listarDocumentos('EM_CONFERENCIA', 0, 1).subscribe({
          next: (page) => {
            this.carregando.set(false);
            if (page.items && page.items.length > 0) {
              const primeiro = page.items[0];
              this.router.navigate(['/documentos', primeiro.id, 'revisar']);
            } else {
              this.router.navigate(['/documentos']);
            }
          },
          error: () => {
            this.carregando.set(false);
            this.router.navigate(['/documentos']);
          }
        });
      },
      error: (err) => {
        this.carregando.set(false);
        this.erro.set(err?.error?.detail || 'Credenciais inválidas ou erro no servidor de autenticação.');
      }
    });
  }
}
