import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="min-h-screen w-full flex items-center justify-center bg-[#0A0E17] p-6 font-sans relative overflow-hidden select-none">
      <!-- Glow sutil de fundo (estilo Raycast / Linear) -->
      <div class="absolute w-[500px] h-[500px] bg-gov-cobalt-600/10 rounded-full blur-[120px] pointer-events-none -top-40 -left-40"></div>
      <div class="absolute w-[400px] h-[400px] bg-emerald-500/5 rounded-full blur-[100px] pointer-events-none -bottom-20 -right-20"></div>

      <!-- Card Principal -->
      <div class="relative w-full max-w-md bg-[#111827] border border-white/10 rounded-2xl shadow-2xl p-8 z-10 space-y-6">
        <!-- Logo e Cabeçalho -->
        <div class="text-center space-y-2">
          <div class="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-gov-cobalt-600 text-white font-bold text-xl shadow-md mb-2">
            <svg class="w-6 h-6 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2"/>
              <polyline points="2 17 12 22 22 17"/>
              <polyline points="2 12 12 17 22 12"/>
            </svg>
          </div>
          <h1 class="text-2xl font-bold text-white tracking-tight flex items-center justify-center gap-2">
            <span>GovFlow</span>
            <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-gov-cobalt-500/20 text-gov-cobalt-400 font-semibold border border-gov-cobalt-500/30">PRO</span>
          </h1>
          <p class="text-xs text-gov-slate-400">
            Copiloto Operacional de Gestão do Transferegov para Consultorias
          </p>
        </div>

        <!-- Formulário de Login -->
        <form (ngSubmit)="onSubmit()" class="space-y-4">
          @if (erro()) {
            <div class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2">
              <svg class="w-4 h-4 text-rose-400 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <line x1="12" y1="8" x2="12" y2="12"/>
                <line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span>{{ erro() }}</span>
            </div>
          }

          <div>
            <label for="txt-email" class="block text-xs font-medium text-gov-slate-300 mb-1.5">
              E-mail do Analista
            </label>
            <input
              id="txt-email"
              type="email"
              [(ngModel)]="email"
              name="email"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3.5 py-2.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              placeholder="analista@govflow.com.br"
            />
          </div>

          <div>
            <label for="txt-senha" class="block text-xs font-medium text-gov-slate-300 mb-1.5">
              Senha de Acesso
            </label>
            <input
              id="txt-senha"
              type="password"
              [(ngModel)]="senha"
              name="senha"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3.5 py-2.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
              placeholder="••••••••"
            />
          </div>

          <button
            type="submit"
            [disabled]="carregando()"
            class="w-full py-2.5 px-4 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs tracking-tight transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer"
          >
            @if (carregando()) {
              <span class="animate-spin text-sm">⟳</span>
              <span>Autenticando...</span>
            } @else {
              <span>Acessar Painel</span>
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            }
          </button>
        </form>

        <!-- Botão de Acesso Imediato Modo Demonstração -->
        <div class="pt-4 border-t border-white/10 text-center space-y-2">
          <button
            type="button"
            (click)="entrarDemo()"
            class="w-full py-2 px-3 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-emerald-400 hover:text-emerald-300 transition-colors flex items-center justify-center gap-2 cursor-pointer"
          >
            <svg class="w-3.5 h-3.5 text-emerald-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
            </svg>
            <span>Acessar Imediatamente (Modo Preview Cockpit)</span>
          </button>
          <p class="text-[11px] text-gov-slate-500">
            Ambiente com prefeituras e convênios demonstrativos pré-carregados
          </p>
        </div>
      </div>
    </div>
  `
})
export class LoginPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  public email = 'analista@govflow.com.br';
  public senha = 'govflow123';
  public readonly carregando = signal<boolean>(false);
  public readonly erro = signal<string | null>(null);

  public entrarDemo(): void {
    this.authService.entrarModoDemo();
    this.toast.sucesso('Bem-vindo ao GovFlow!', 'Sessão demonstrativa ativada.');
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
        this.carregando.set(false);
        this.toast.sucesso('Autenticado com sucesso!', 'Carregando cockpit...');
        this.router.navigate(['/convenios']);
      },
      error: () => {
        this.carregando.set(false);
        // Fallback gracioso para modo demo se backend não estiver respondendo na porta 8080
        this.entrarDemo();
      }
    });
  }
}
