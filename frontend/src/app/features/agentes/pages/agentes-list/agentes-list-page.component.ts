import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AgenteService, Agente } from '../../../../core/services/agente.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { ToastService } from '../../../../core/ui/toast.service';
import { CadastrarAgenteModalComponent } from '../../components/cadastrar-agente-modal/cadastrar-agente-modal.component';
import { VincularPrefeiturasModalComponent } from '../../components/vincular-prefeituras-modal/vincular-prefeituras-modal.component';

@Component({
  selector: 'app-agentes-list-page',
  standalone: true,
  imports: [CommonModule, CadastrarAgenteModalComponent, VincularPrefeiturasModalComponent],
  template: `
    <div class="p-6 max-w-7xl mx-auto space-y-6 select-none font-sans">
      <!-- Cabeçalho -->
      <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 pb-4 border-b border-white/10">
        <div>
          <div class="flex items-center gap-2.5">
            <h1 class="text-xl font-bold text-white tracking-tight">Equipe de Agentes & Analistas</h1>
            <span class="px-2 py-0.5 rounded-full text-[10px] font-mono font-bold bg-gov-cobalt-500/20 text-gov-cobalt-400 border border-gov-cobalt-500/30">
              IAM & Governança
            </span>
          </div>
          <p class="text-xs text-gov-slate-400 mt-1">
            Gerencie os operadores da consultoria, números de telefone para WhatsApp e o escopo de prefeituras atendidas.
          </p>
        </div>

        @if (auth.isAdmin()) {
          <button
            type="button"
            (click)="modalAberto.set(true)"
            class="px-4 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-xs font-semibold text-white transition-all shadow-sm flex items-center gap-2 cursor-pointer shrink-0"
          >
            <span class="text-sm">➕</span>
            <span>Novo Agente</span>
          </button>
        }
      </div>

      <!-- Barra de Resumo -->
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div class="p-4 rounded-xl bg-[#111827] border border-white/10 flex items-center justify-between">
          <div>
            <div class="text-[11px] font-medium text-gov-slate-400">Total de Integrantes</div>
            <div class="text-2xl font-bold text-white mt-1">{{ agentes().length }}</div>
          </div>
          <div class="w-9 h-9 rounded-lg bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center font-bold">
            👥
          </div>
        </div>

        <div class="p-4 rounded-xl bg-[#111827] border border-white/10 flex items-center justify-between">
          <div>
            <div class="text-[11px] font-medium text-gov-slate-400">Agentes Operacionais</div>
            <div class="text-2xl font-bold text-emerald-400 mt-1">{{ totalAgentes() }}</div>
          </div>
          <div class="w-9 h-9 rounded-lg bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center font-bold">
            ⚡
          </div>
        </div>

        <div class="p-4 rounded-xl bg-[#111827] border border-white/10 flex items-center justify-between">
          <div>
            <div class="text-[11px] font-medium text-gov-slate-400">Administradores</div>
            <div class="text-2xl font-bold text-gov-cobalt-300 mt-1">{{ totalAdmins() }}</div>
          </div>
          <div class="w-9 h-9 rounded-lg bg-gov-cobalt-500/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center font-bold">
            🛡️
          </div>
        </div>
      </div>

      <!-- Grid de Cards de Agentes -->
      @if (carregando()) {
        <div class="py-16 flex flex-col items-center justify-center text-gov-slate-400 gap-3">
          <div class="w-7 h-7 border-2 border-gov-cobalt-500 border-t-transparent rounded-full animate-spin"></div>
          <span class="text-xs">Carregando equipe de agentes...</span>
        </div>
      } @else if (agentes().length === 0) {
        <div class="py-16 text-center rounded-2xl bg-[#111827] border border-white/10 p-8 space-y-3">
          <div class="text-3xl">👥</div>
          <h3 class="text-sm font-semibold text-white">Nenhum agente cadastrado</h3>
          <p class="text-xs text-gov-slate-400 max-w-sm mx-auto">
            Cadastre os analistas e operadores da consultoria para delegar o monitoramento de municípios.
          </p>
        </div>
      } @else {
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          @for (agente of agentes(); track agente.id) {
            <div class="p-5 rounded-xl bg-[#111827] border border-white/10 shadow-sm hover:border-white/20 transition-all flex flex-col justify-between space-y-4">
              <!-- Topo do Card -->
              <div>
                <div class="flex items-start justify-between gap-3">
                  <div class="flex items-center gap-3 min-w-0">
                    <div class="w-10 h-10 rounded-full bg-gov-cobalt-600/30 border border-gov-cobalt-500/40 flex items-center justify-center text-sm font-bold text-gov-cobalt-300 shrink-0">
                      {{ agente.nome.charAt(0).toUpperCase() }}
                    </div>
                    <div class="min-w-0">
                      <h4 class="text-sm font-semibold text-white truncate">{{ agente.nome }}</h4>
                      <p class="text-xs text-gov-slate-400 truncate">{{ agente.email }}</p>
                    </div>
                  </div>

                  <div class="flex items-center gap-1.5 shrink-0">
                    <span
                      class="text-[10px] font-mono px-2 py-0.5 rounded font-bold"
                      [ngClass]="{
                        'bg-gov-cobalt-500/20 text-gov-cobalt-300 border border-gov-cobalt-500/30': agente.role === 'ADMIN',
                        'bg-slate-700/50 text-slate-300 border border-slate-600/40': agente.role === 'AGENTE'
                      }"
                    >
                      {{ agente.role }}
                    </span>
                    <span
                      class="w-2 h-2 rounded-full"
                      [ngClass]="agente.ativo ? 'bg-emerald-400' : 'bg-rose-500'"
                      [title]="agente.ativo ? 'Agente Ativo' : 'Agente Inativo'"
                    ></span>
                  </div>
                </div>

                <!-- Celular WhatsApp -->
                <div class="mt-4 pt-3 border-t border-white/5 flex items-center justify-between text-xs">
                  <div class="flex items-center gap-2 text-gov-slate-300">
                    <svg class="w-3.5 h-3.5 text-emerald-400 shrink-0" viewBox="0 0 24 24" fill="currentColor">
                      <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19.01L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 14.99 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67Z"/>
                    </svg>
                    <span class="font-mono text-[11px]">{{ agente.telefoneCelular || 'Sem celular' }}</span>
                  </div>
                  <span class="text-[10px] text-gov-slate-500 font-mono">WhatsApp Canal</span>
                </div>

                <!-- Prefeituras Vinculadas -->
                <div class="mt-3">
                  <div class="text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider mb-1.5 flex items-center justify-between">
                    <span>Municípios Atribuídos</span>
                    <span class="font-mono">{{ agente.prefeiturasAtribuidasIds.length || 0 }}</span>
                  </div>

                  <div class="flex flex-wrap gap-1.5 min-h-[30px]">
                    @if (agente.role === 'ADMIN') {
                      <span class="text-[10px] px-2 py-0.5 rounded bg-gov-cobalt-500/10 border border-gov-cobalt-500/20 text-gov-cobalt-300 font-medium">
                        Visão Global (Todas as Prefeituras)
                      </span>
                    } @else if (agente.prefeiturasAtribuidasIds && agente.prefeiturasAtribuidasIds.length > 0) {
                      @for (prefId of agente.prefeiturasAtribuidasIds; track prefId) {
                        <span class="text-[10px] px-2 py-0.5 rounded bg-white/5 border border-white/10 text-gov-slate-200">
                          {{ obterNomePrefeitura(prefId) }}
                        </span>
                      }
                    } @else {
                      <span class="text-[10px] text-amber-400/80 italic">
                        Nenhuma prefeitura vinculada
                      </span>
                    }
                  </div>
                </div>
              </div>

              <!-- Ações -->
              @if (auth.isAdmin() && agente.role !== 'ADMIN') {
                <div class="pt-3 border-t border-white/5 flex items-center justify-between">
                  <button
                    type="button"
                    (click)="agenteParaVincular.set(agente)"
                    class="text-xs text-gov-cobalt-400 hover:text-gov-cobalt-300 flex items-center gap-1.5 cursor-pointer font-medium hover:bg-gov-cobalt-500/10 px-2 py-1 rounded transition-colors"
                  >
                    <span>🏛️</span>
                    <span>Vincular Prefeituras</span>
                  </button>

                  @if (agente.ativo) {
                    <button
                      type="button"
                      (click)="inativarAgente(agente)"
                      class="text-xs text-rose-400 hover:text-rose-300 hover:underline cursor-pointer"
                    >
                      Inativar Acesso
                    </button>
                  } @else {
                    <span class="text-xs text-gov-slate-500 italic">Acesso Desativado</span>
                  }
                </div>
              }
            </div>
          }
        </div>
      }
    </div>

    @if (modalAberto()) {
      <app-cadastrar-agente-modal
        (fechar)="modalAberto.set(false)"
        (cadastrado)="onAgenteCadastrado()"
      ></app-cadastrar-agente-modal>
    }

    @if (agenteParaVincular(); as agenteAlvo) {
      <app-vincular-prefeituras-modal
        [agente]="agenteAlvo"
        (fechar)="agenteParaVincular.set(null)"
        (salvo)="onVinculosAtualizados()"
      ></app-vincular-prefeituras-modal>
    }
  `
})
export class AgentesListPageComponent implements OnInit {
  private readonly agenteService = inject(AgenteService);
  private readonly municipioCtx = inject(MunicipioContextService);
  readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);

  readonly agentes = signal<Agente[]>([]);
  readonly carregando = signal(true);
  readonly modalAberto = signal(false);
  readonly agenteParaVincular = signal<Agente | null>(null);

  ngOnInit(): void {
    this.carregarAgentes();
  }

  carregarAgentes(): void {
    this.carregando.set(true);
    this.agenteService.listarAgentes().subscribe({
      next: (dados) => {
        this.agentes.set(dados || []);
        this.carregando.set(false);
      },
      error: () => {
        this.carregando.set(false);
      }
    });
  }

  onAgenteCadastrado(): void {
    this.modalAberto.set(false);
    this.carregarAgentes();
  }

  onVinculosAtualizados(): void {
    this.agenteParaVincular.set(null);
    this.carregarAgentes();
  }

  inativarAgente(agente: Agente): void {
    if (!confirm(`Deseja realmente inativar o acesso de ${agente.nome}?`)) {
      return;
    }
    this.agenteService.inativarAgente(agente.id).subscribe({
      next: () => {
        this.toast.sucesso(`Acesso de ${agente.nome} inativado.`);
        this.carregarAgentes();
      },
      error: () => {
        this.toast.erro('Falha ao inativar agente.');
      }
    });
  }

  obterNomePrefeitura(prefId: string): string {
    const mun = this.municipioCtx.municipios().find(m => m.id === prefId || m.codigoIbge === prefId);
    return mun ? `${mun.nome} - ${mun.uf}` : `Prefeitura (${prefId.substring(0, 8)})`;
  }

  totalAgentes(): number {
    return this.agentes().filter(a => a.role === 'AGENTE').length;
  }

  totalAdmins(): number {
    return this.agentes().filter(a => a.role === 'ADMIN').length;
  }
}
