import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgenteService, CriarAgentePayload } from '../../../../core/services/agente.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-cadastrar-agente-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4 select-none font-sans overflow-y-auto">
      <div class="bg-[#111827] border border-white/10 rounded-2xl max-w-xl w-full p-6 shadow-2xl space-y-5 my-8 animate-in fade-in duration-150">
        <!-- Cabeçalho -->
        <div class="flex items-center justify-between pb-3 border-b border-white/10">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-lg bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
                <path d="M22 21v-2a4 4 0 0 0-3-3.87"/>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
              </svg>
            </div>
            <div>
              <h3 class="text-sm font-bold text-white tracking-tight">Cadastrar Novo Agente / Analista</h3>
              <p class="text-[11px] text-gov-slate-400">Adicione um operador à equipe, configure o celular para WhatsApp e atribua prefeituras</p>
            </div>
          </div>
          <button
            type="button"
            (click)="fecharModal()"
            class="text-gov-slate-400 hover:text-white p-1 rounded-lg hover:bg-white/5 transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        @if (erro()) {
          <div class="p-3 rounded-lg bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
            <svg class="w-4 h-4 text-rose-400 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            <span>{{ erro() }}</span>
          </div>
        }

        <!-- Formulário -->
        <form (ngSubmit)="salvar()" class="space-y-4">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- Nome -->
            <div>
              <label for="nome" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Nome Completo *
              </label>
              <input
                id="nome"
                type="text"
                [(ngModel)]="nome"
                name="nome"
                placeholder="Ex: João Analista"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
              />
            </div>

            <!-- E-mail -->
            <div>
              <label for="email" class="block text-xs font-medium text-gov-slate-300 mb-1">
                E-mail Institucional *
              </label>
              <input
                id="email"
                type="email"
                [(ngModel)]="email"
                name="email"
                placeholder="joao@planejabrasil.com.br"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
              />
            </div>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- Senha Temporária -->
            <div>
              <label for="senha" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Senha Provisória *
              </label>
              <input
                id="senha"
                type="password"
                [(ngModel)]="senha"
                name="senha"
                placeholder="Mínimo 6 caracteres"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
              />
            </div>

            <!-- Celular (WhatsApp) -->
            <div>
              <label for="celular" class="block text-xs font-medium text-gov-slate-300 mb-1 flex items-center justify-between">
                <span>Telefone Celular (WhatsApp) *</span>
                <span class="text-[10px] text-emerald-400 font-mono">E.164</span>
              </label>
              <input
                id="celular"
                type="text"
                [(ngModel)]="celular"
                name="celular"
                (input)="aplicarMascaraTelefone($event)"
                placeholder="(83) 98888-1111"
                maxlength="15"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>
          </div>

          <!-- Seleção N:N de Prefeituras -->
          <div>
            <label class="block text-xs font-medium text-gov-slate-300 mb-1">
              Prefeituras Atribuídas (Escopo de Visualização)
            </label>
            <p class="text-[11px] text-gov-slate-400 mb-2">
              O agente terá visão e permissões restritas exclusivamente aos municípios selecionados abaixo:
            </p>
            <div class="max-h-40 overflow-y-auto bg-[#0A0E17] border border-white/10 rounded-lg p-2.5 space-y-1.5 divide-y divide-white/5">
              @for (mun of municipioCtx.municipios(); track mun.id) {
                <label class="flex items-center gap-2.5 pt-1.5 first:pt-0 cursor-pointer text-xs text-gov-slate-200 hover:text-white">
                  <input
                    type="checkbox"
                    [checked]="prefeiturasSelecionadas().has(mun.id)"
                    (change)="togglePrefeitura(mun.id)"
                    class="rounded border-white/20 bg-white/5 text-gov-cobalt-600 focus:ring-gov-cobalt-500"
                  />
                  <div class="flex items-center justify-between w-full">
                    <span class="font-medium">{{ mun.nome }} - {{ mun.uf }}</span>
                    <span class="text-[10px] text-gov-slate-400 font-mono">{{ mun.codigoIbge }}</span>
                  </div>
                </label>
              }
              @if (municipioCtx.municipios().length === 0) {
                <div class="text-xs text-gov-slate-400 text-center py-2">
                  Nenhuma prefeitura cadastrada na consultoria ainda.
                </div>
              }
            </div>
          </div>

          <!-- Botões de Ação -->
          <div class="flex items-center justify-end gap-3 pt-3 border-t border-white/10">
            <button
              type="button"
              (click)="fecharModal()"
              class="px-4 py-2 rounded-lg text-xs font-medium text-gov-slate-400 hover:text-white hover:bg-white/5 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="carregando()"
              class="px-4 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 disabled:opacity-50 text-xs font-semibold text-white transition-all shadow-sm flex items-center gap-2 cursor-pointer"
            >
              @if (carregando()) {
                <span class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                <span>Salvando...</span>
              } @else {
                <span>Cadastrar Agente</span>
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CadastrarAgenteModalComponent {
  @Output() fechar = new EventEmitter<void>();
  @Output() cadastrado = new EventEmitter<void>();

  private readonly agenteService = inject(AgenteService);
  readonly municipioCtx = inject(MunicipioContextService);
  private readonly toast = inject(ToastService);

  nome = '';
  email = '';
  senha = '';
  celular = '';
  prefeiturasSelecionadas = signal<Set<string>>(new Set());

  carregando = signal(false);
  erro = signal<string | null>(null);

  togglePrefeitura(id: string): void {
    const atual = new Set(this.prefeiturasSelecionadas());
    if (atual.has(id)) {
      atual.delete(id);
    } else {
      atual.add(id);
    }
    this.prefeiturasSelecionadas.set(atual);
  }

  aplicarMascaraTelefone(event: Event): void {
    const input = event.target as HTMLInputElement;
    let v = input.value.replace(/\D/g, '');
    if (v.length > 11) v = v.slice(0, 11);

    if (v.length > 10) {
      v = v.replace(/^(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
    } else if (v.length > 6) {
      v = v.replace(/^(\d{2})(\d{4})(\d{0,4})/, '($1) $2-$3');
    } else if (v.length > 2) {
      v = v.replace(/^(\d{2})(\d{0,5})/, '($1) $2');
    }
    this.celular = v;
    input.value = v;
  }

  salvar(): void {
    if (!this.nome || !this.email || !this.senha || !this.celular) {
      this.erro.set('Preencha todos os campos obrigatórios.');
      return;
    }

    if (this.senha.length < 6) {
      this.erro.set('A senha provisória deve conter no mínimo 6 caracteres.');
      return;
    }

    this.carregando.set(true);
    this.erro.set(null);

    const payload: CriarAgentePayload = {
      nome: this.nome.trim(),
      email: this.email.trim(),
      senha: this.senha,
      telefoneCelular: this.celular.trim(),
      prefeiturasIds: Array.from(this.prefeiturasSelecionadas())
    };

    this.agenteService.cadastrarAgente(payload).subscribe({
      next: () => {
        this.carregando.set(false);
        this.toast.sucesso(`Agente ${this.nome} cadastrado com sucesso!`);
        this.cadastrado.emit();
      },
      error: (err) => {
        this.carregando.set(false);
        const msg = err?.error?.detail || err?.error?.message || 'Falha ao cadastrar agente. Verifique se o e-mail já existe.';
        this.erro.set(msg);
        this.toast.erro(msg);
      }
    });
  }

  fecharModal(): void {
    this.fechar.emit();
  }
}
