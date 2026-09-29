import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterPayload } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
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

        <!-- Seletor de Modo (Entrar vs Criar Conta) -->
        <div class="flex items-center p-1 rounded-xl bg-white/5 border border-white/10">
          <button
            type="button"
            (click)="aba.set('LOGIN')"
            class="flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all cursor-pointer"
            [ngClass]="aba() === 'LOGIN' ? 'bg-gov-cobalt-600 text-white shadow-sm' : 'text-gov-slate-400 hover:text-white'"
          >
            Entrar
          </button>
          <a
            routerLink="/cadastro"
            (click)="aba.set('REGISTER')"
            class="flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all text-center cursor-pointer"
            [ngClass]="aba() === 'REGISTER' ? 'bg-gov-cobalt-600 text-white shadow-sm' : 'text-gov-slate-400 hover:text-white'"
          >
            Criar Conta
          </a>
        </div>

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

        <!-- 1. FORMULÁRIO DE LOGIN -->
        @if (aba() === 'LOGIN') {
          <form (ngSubmit)="onSubmitLogin()" class="space-y-4">
            <div>
              <label for="txt-email" class="block text-xs font-medium text-gov-slate-300 mb-1.5">
                E-mail de Acesso (Agente / Gestor)
              </label>
              <input
                id="txt-email"
                type="email"
                [(ngModel)]="email"
                name="email"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3.5 py-2.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
                placeholder="agente@govflow.com.br"
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

            <!-- Link Explícito para Página de Cadastro -->
            <div class="text-center pt-3 border-t border-white/5 space-y-1">
              <p class="text-xs text-gov-slate-400">
                Sua consultoria ainda não tem conta no GovFlow?
              </p>
              <a
                routerLink="/cadastro"
                class="inline-flex items-center gap-1.5 text-xs font-semibold text-gov-cobalt-400 hover:text-gov-cobalt-300 transition-colors cursor-pointer group"
              >
                <span>Criar nova conta de consultoria</span>
                <svg class="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </a>
            </div>
          </form>
        }

        <!-- 2. FORMULÁRIO DE ONBOARDING CONSULTORIA (CRIAR CONTA) -->
        @if (aba() === 'REGISTER') {
          <form (ngSubmit)="onSubmitRegister()" class="space-y-3.5">
            <div>
              <label for="reg-razao" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Razão Social da Consultoria *
              </label>
              <input
                id="reg-razao"
                type="text"
                [(ngModel)]="regRazaoSocial"
                name="regRazaoSocial"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                placeholder="Planeja Brasil Consultoria Municipal"
              />
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label for="reg-cnpj" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  CNPJ *
                </label>
                <input
                  id="reg-cnpj"
                  type="text"
                  [(ngModel)]="regCnpj"
                  name="regCnpj"
                  maxlength="18"
                  (input)="aplicarMascaraCnpj($event)"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                  placeholder="00.000.000/0001-00"
                />
              </div>

              <div>
                <label for="reg-telefone" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  Telefone
                </label>
                <input
                  id="reg-telefone"
                  type="text"
                  [(ngModel)]="regTelefone"
                  name="regTelefone"
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                  placeholder="(83) 98888-7777"
                />
              </div>
            </div>

            <div>
              <label for="reg-analista" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Nome do Administrador da Conta *
              </label>
              <input
                id="reg-analista"
                type="text"
                [(ngModel)]="regNomeAnalista"
                name="regNomeAnalista"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                placeholder="Carlos Eduardo Lima"
              />
            </div>

            <div>
              <label for="reg-email" class="block text-xs font-medium text-gov-slate-300 mb-1">
                E-mail Corporativo do Administrador *
              </label>
              <input
                id="reg-email"
                type="email"
                [(ngModel)]="regEmailAnalista"
                name="regEmailAnalista"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                placeholder="carlos@planejabrasil.com.br"
              />
            </div>

            <div>
              <label for="reg-senha" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Senha (mínimo 6 dígitos) *
              </label>
              <input
                id="reg-senha"
                type="password"
                [(ngModel)]="regSenha"
                name="regSenha"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                placeholder="••••••••"
              />
            </div>

            <button
              type="submit"
              [disabled]="carregando()"
              class="w-full py-2.5 px-4 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs tracking-tight transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer mt-2"
            >
              @if (carregando()) {
                <span class="animate-spin text-sm">⟳</span>
                <span>Criando Conta & Tenant...</span>
              } @else {
                <span>Criar Conta e Iniciar</span>
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              }
            </button>

            <!-- Link de Retorno ao Login -->
            <div class="text-center pt-3 border-t border-white/5 space-y-1">
              <p class="text-xs text-gov-slate-400">
                Já possui uma conta de analista?
              </p>
              <a
                (click)="aba.set('LOGIN')"
                class="inline-flex items-center gap-1.5 text-xs font-semibold text-gov-cobalt-400 hover:text-gov-cobalt-300 transition-colors cursor-pointer group"
              >
                <span>Fazer login com conta existente</span>
                <svg class="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </a>
            </div>
          </form>
        }

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

  public readonly aba = signal<'LOGIN' | 'REGISTER'>('LOGIN');

  // Login
  public email = 'analista@govflow.com.br';
  public senha = 'govflow123';

  // Register
  public regRazaoSocial = '';
  public regCnpj = '';
  public regTelefone = '';
  public regNomeAnalista = '';
  public regEmailAnalista = '';
  public regSenha = '';

  public readonly carregando = signal<boolean>(false);
  public readonly erro = signal<string | null>(null);

  aplicarMascaraCnpj(event: Event): void {
    const input = event.target as HTMLInputElement;
    let valor = input.value.replace(/\D/g, '');
    if (valor.length > 14) valor = valor.slice(0, 14);

    if (valor.length > 12) {
      valor = valor.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{1,2})/, '$1.$2.$3/$4-$5');
    } else if (valor.length > 8) {
      valor = valor.replace(/^(\d{2})(\d{3})(\d{3})(\d{1,4})/, '$1.$2.$3/$4');
    } else if (valor.length > 5) {
      valor = valor.replace(/^(\d{2})(\d{3})(\d{1,3})/, '$1.$2.$3');
    } else if (valor.length > 2) {
      valor = valor.replace(/^(\d{2})(\d{1,3})/, '$1.$2');
    }
    this.regCnpj = valor;
  }

  public entrarDemo(): void {
    this.authService.entrarModoDemo();
    this.toast.sucesso('Bem-vindo ao GovFlow!', 'Sessão demonstrativa ativada.');
  }

  public onSubmitLogin(): void {
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
      error: (err) => {
        this.carregando.set(false);
        const msg = err.status === 401 ? 'E-mail ou senha incorretos.' : (err.error?.detail || err.error?.message || (err.status === 0 ? 'Não foi possível conectar ao servidor GovFlow.' : 'Falha na autenticação.'));
        this.erro.set(msg);
        this.toast.erro('Falha no Login', msg);
      }
    });
  }

  public onSubmitRegister(): void {
    if (!this.regRazaoSocial || !this.regCnpj || !this.regNomeAnalista || !this.regEmailAnalista || !this.regSenha) {
      this.erro.set('Preencha todos os campos obrigatórios (*).');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    const payload: RegisterPayload = {
      razaoSocial: this.regRazaoSocial.trim(),
      cnpj: this.regCnpj.trim(),
      nomeAdministrador: this.regNomeAnalista.trim(),
      emailAdministrador: this.regEmailAnalista.trim(),
      nomeAnalista: this.regNomeAnalista.trim(),
      emailAnalista: this.regEmailAnalista.trim(),
      senha: this.regSenha,
      telefone: this.regTelefone ? this.regTelefone.trim() : undefined,
      plano: 'PRO'
    };

    this.authService.cadastrarConsultoria(payload).subscribe({
      next: (resp) => {
        this.carregando.set(false);
        this.toast.sucesso('Consultoria Cadastrada!', `Bem-vindo, ${resp.nome}!`);
        this.router.navigate(['/convenios']);
      },
      error: (err) => {
        this.carregando.set(false);
        const msg = err.error?.detail || err.error?.message || (err.status === 0 ? 'Não foi possível conectar ao servidor GovFlow.' : 'Falha ao cadastrar consultoria.');
        this.erro.set(msg);
        this.toast.erro('Erro no Cadastro', msg);
      }
    });
  }
}
