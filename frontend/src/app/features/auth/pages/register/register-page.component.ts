import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegisterPayload } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="min-h-screen w-full flex items-center justify-center bg-[#0A0E17] p-6 font-sans relative overflow-hidden select-none">
      <!-- Glow sutil de fundo (estilo Raycast / Linear) -->
      <div class="absolute w-[500px] h-[500px] bg-gov-cobalt-600/10 rounded-full blur-[120px] pointer-events-none -top-40 -left-40"></div>
      <div class="absolute w-[400px] h-[400px] bg-emerald-500/5 rounded-full blur-[100px] pointer-events-none -bottom-20 -right-20"></div>

      <!-- Card Principal -->
      <div class="relative w-full max-w-xl bg-[#111827] border border-white/10 rounded-2xl shadow-2xl p-8 z-10 space-y-6">
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
            Cadastro Corporativo da Consultoria & Gestor da Equipe
          </p>
        </div>

        <!-- Seletor de Modo (Entrar vs Criar Conta) -->
        <div class="flex items-center p-1 rounded-xl bg-white/5 border border-white/10">
          <a
            routerLink="/login"
            class="flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all text-center text-gov-slate-400 hover:text-white cursor-pointer"
          >
            Entrar
          </a>
          <span
            class="flex-1 py-1.5 text-xs font-semibold rounded-lg transition-all text-center bg-gov-cobalt-600 text-white shadow-sm"
          >
            Criar Conta da Consultoria
          </span>
        </div>

        @if (erro()) {
          <div class="p-3 rounded-lg bg-rose-500/10 border border-rose-500/20 text-rose-300 text-xs flex items-center gap-2 animate-in fade-in duration-200">
            <svg class="w-4 h-4 text-rose-400 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ erro() }}</span>
          </div>
        }

        <!-- FORMULÁRIO DE CADASTRO -->
        <form (ngSubmit)="onSubmitRegister()" class="space-y-4">
          <!-- 1. DADOS DA CONSULTORIA (EMPRESA / TENANT) -->
          <div class="p-4 rounded-xl bg-white/[0.02] border border-white/5 space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-[11px] font-bold text-gov-cobalt-400 uppercase tracking-wider flex items-center gap-1.5">
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect width="16" height="20" x="4" y="2" rx="2" ry="2"/>
                  <path d="M9 22v-4h6v4"/>
                  <path d="M8 6h.01"/>
                  <path d="M16 6h.01"/>
                </svg>
                <span>1. Dados da Consultoria (Empresa)</span>
              </span>
              <span class="text-[10px] text-gov-slate-500 font-mono">Tenant Multi-Agente</span>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
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
                  placeholder="Planeja Brasil Consultoria Municipal Ltda"
                />
              </div>

              <div>
                <label for="reg-fantasia" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  Nome Fantasia *
                </label>
                <input
                  id="reg-fantasia"
                  type="text"
                  [(ngModel)]="regNomeFantasia"
                  name="regNomeFantasia"
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                  placeholder="Planeja Brasil"
                />
              </div>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label for="reg-cnpj" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  CNPJ da Consultoria *
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
                  Telefone da Empresa
                </label>
                <input
                  id="reg-telefone"
                  type="text"
                  [(ngModel)]="regTelefone"
                  name="regTelefone"
                  maxlength="15"
                  (input)="aplicarMascaraTelefone($event, 'EMPRESA')"
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                  placeholder="(83) 3421-2000"
                />
              </div>
            </div>

            <!-- Seleção de Plano -->
            <div>
              <label class="block text-xs font-medium text-gov-slate-300 mb-1.5">
                Plano Operacional da Consultoria
              </label>
              <div class="grid grid-cols-3 gap-2">
                <button
                  type="button"
                  (click)="regPlano = 'STARTER'"
                  class="p-2 rounded-lg border text-left transition-all cursor-pointer"
                  [ngClass]="regPlano === 'STARTER' ? 'bg-gov-cobalt-500/15 border-gov-cobalt-500 text-white ring-1 ring-gov-cobalt-500/50' : 'bg-white/[0.02] border-white/10 text-gov-slate-400 hover:text-white'"
                >
                  <div class="text-[11px] font-bold">STARTER</div>
                  <div class="text-[10px] text-gov-slate-500">Até 5 prefeituras</div>
                </button>
                <button
                  type="button"
                  (click)="regPlano = 'PRO'"
                  class="p-2 rounded-lg border text-left transition-all cursor-pointer"
                  [ngClass]="regPlano === 'PRO' ? 'bg-gov-cobalt-500/15 border-gov-cobalt-500 text-white ring-1 ring-gov-cobalt-500' : 'bg-white/[0.02] border-white/10 text-gov-slate-400 hover:text-white'"
                >
                  <div class="text-[11px] font-bold text-gov-cobalt-300 flex items-center justify-between">
                    <span>PRO</span>
                    <span class="text-[8px] bg-gov-cobalt-500/30 text-gov-cobalt-300 px-1 py-0.5 rounded font-mono font-bold">PADRÃO</span>
                  </div>
                  <div class="text-[10px] text-gov-slate-500">Até 15 prefeituras</div>
                </button>
                <button
                  type="button"
                  (click)="regPlano = 'ENTERPRISE'"
                  class="p-2 rounded-lg border text-left transition-all cursor-pointer"
                  [ngClass]="regPlano === 'ENTERPRISE' ? 'bg-gov-cobalt-500/15 border-gov-cobalt-500 text-white ring-1 ring-gov-cobalt-500/50' : 'bg-white/[0.02] border-white/10 text-gov-slate-400 hover:text-white'"
                >
                  <div class="text-[11px] font-bold">ENTERPRISE</div>
                  <div class="text-[10px] text-gov-slate-500">Ilimitado</div>
                </button>
              </div>
            </div>
          </div>

          <!-- 2. ADMINISTRADOR PRINCIPAL DA CONTA (GESTOR DA EQUIPE DE AGENTES) -->
          <div class="p-4 rounded-xl bg-gov-cobalt-600/5 border border-gov-cobalt-500/20 space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-[11px] font-bold text-gov-cobalt-300 uppercase tracking-wider flex items-center gap-1.5">
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                <span>2. Administrador da Conta (Gestor)</span>
              </span>
              <span class="text-[10px] text-emerald-400 font-semibold px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
                Acesso Master
              </span>
            </div>

            <p class="text-[11px] text-gov-slate-400 leading-relaxed">
              O Administrador terá autonomia para convidar e gerenciar a <strong>equipe de múltiplos agentes</strong>, prefeituras convenentes e convênios da consultoria.
            </p>

            <div>
              <label for="reg-administrador" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Nome do Administrador / Gestor *
              </label>
              <input
                id="reg-administrador"
                type="text"
                [(ngModel)]="regNomeAdministrador"
                name="regNomeAdministrador"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                placeholder="Carlos Eduardo Lima"
              />
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label for="reg-email" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  E-mail Corporativo de Acesso *
                </label>
                <input
                  id="reg-email"
                  type="email"
                  [(ngModel)]="regEmailAdministrador"
                  name="regEmailAdministrador"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                  placeholder="gestor@planejabrasil.com.br"
                />
              </div>

              <div>
                <label for="reg-celular" class="block text-xs font-medium text-gov-slate-300 mb-1">
                  Celular do Gestor (WhatsApp) *
                </label>
                <input
                  id="reg-celular"
                  type="text"
                  [(ngModel)]="regCelularAdministrador"
                  name="regCelularAdministrador"
                  maxlength="15"
                  (input)="aplicarMascaraTelefone($event, 'ADMIN')"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                  placeholder="(83) 98888-7777"
                />
              </div>
            </div>

            <div>
              <label for="reg-senha" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Senha de Acesso (mínimo 6 caracteres) *
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
          </div>

          <button
            type="submit"
            [disabled]="carregando()"
            class="w-full py-2.5 px-4 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs tracking-tight transition-colors shadow-sm disabled:opacity-50 flex items-center justify-center gap-2 cursor-pointer mt-2"
          >
            @if (carregando()) {
              <span class="animate-spin text-sm">⟳</span>
              <span>Cadastrando Consultoria & Tenant...</span>
            } @else {
              <span>Cadastrar Consultoria e Iniciar</span>
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
            }
          </button>
        </form>

        <!-- Link Direto de Retorno ao Login -->
        <div class="text-center pt-3 border-t border-white/5 space-y-1">
          <p class="text-xs text-gov-slate-400">
            Sua consultoria já possui cadastro no GovFlow?
          </p>
          <a
            routerLink="/login"
            class="inline-flex items-center gap-1.5 text-xs font-semibold text-gov-cobalt-400 hover:text-gov-cobalt-300 transition-colors cursor-pointer group"
          >
            <span>Fazer login com conta de agente existente</span>
            <svg class="w-3.5 h-3.5 transition-transform group-hover:translate-x-0.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"/>
            </svg>
          </a>
        </div>

        <!-- Botão de Acesso Imediato Modo Demonstração -->
        <div class="pt-2 text-center space-y-2">
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
            Ambiente com prefeituras, múltiplos agentes e convênios pré-carregados
          </p>
        </div>
      </div>
    </div>
  `
})
export class RegisterPageComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  public regRazaoSocial = '';
  public regNomeFantasia = '';
  public regCnpj = '';
  public regTelefone = '';
  public regPlano = 'PRO';
  public regNomeAdministrador = '';
  public regEmailAdministrador = '';
  public regCelularAdministrador = '';
  public regSenha = '';

  // Getters/setters de compatibilidade para suítes de teste
  get regNomeAnalista(): string {
    return this.regNomeAdministrador;
  }
  set regNomeAnalista(val: string) {
    this.regNomeAdministrador = val;
  }

  get regEmailAnalista(): string {
    return this.regEmailAdministrador;
  }
  set regEmailAnalista(val: string) {
    this.regEmailAdministrador = val;
  }

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

  aplicarMascaraTelefone(event: Event, tipo: 'EMPRESA' | 'ADMIN'): void {
    const input = event.target as HTMLInputElement;
    let valor = input.value.replace(/\D/g, '');
    if (valor.length > 11) valor = valor.slice(0, 11);

    if (valor.length > 10) {
      valor = valor.replace(/^(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (valor.length > 6) {
      valor = valor.replace(/^(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    } else if (valor.length > 2) {
      valor = valor.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
    }

    if (tipo === 'EMPRESA') {
      this.regTelefone = valor;
    } else {
      this.regCelularAdministrador = valor;
    }
  }

  public entrarDemo(): void {
    this.authService.entrarModoDemo();
    this.toast.sucesso('Bem-vindo ao GovFlow!', 'Sessão demonstrativa ativada.');
  }

  public onSubmitRegister(): void {
    if (!this.regRazaoSocial || !this.regCnpj || !this.regNomeAdministrador || !this.regEmailAdministrador || !this.regSenha) {
      this.erro.set('Preencha todos os campos obrigatórios (*).');
      return;
    }

    if (this.regSenha.length < 6) {
      this.erro.set('A senha deve ter no mínimo 6 caracteres.');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    const nomeFantasiaFinal = (this.regNomeFantasia || this.regRazaoSocial).trim();
    const celularFinal = this.regCelularAdministrador?.trim() || this.regTelefone?.trim() || undefined;

    const payload: RegisterPayload = {
      razaoSocial: this.regRazaoSocial.trim(),
      nomeFantasia: nomeFantasiaFinal,
      cnpj: this.regCnpj.trim(),
      nomeAdministrador: this.regNomeAdministrador.trim(),
      emailAdministrador: this.regEmailAdministrador.trim(),
      senha: this.regSenha,
      telefone: this.regTelefone ? this.regTelefone.trim() : undefined,
      celularAdmin: celularFinal,
      plano: this.regPlano || 'PRO'
    };

    this.authService.cadastrarConsultoria(payload).subscribe({
      next: (resp) => {
        this.carregando.set(false);
        this.toast.sucesso('Consultoria Cadastrada!', `Bem-vindo, Administrador ${resp.nome}!`);
        this.router.navigate(['/convenios']);
      },
      error: (err) => {
        this.carregando.set(false);
        const msg = err.error?.detail || err.error?.message || (err.status === 0 ? 'Não foi possível conectar ao servidor GovFlow.' : 'Falha ao cadastrar consultoria. Verifique os dados informados.');
        this.erro.set(msg);
        this.toast.erro('Erro no Cadastro', msg);
      }
    });
  }
}
