import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AgenteService, Agente } from '../../../../core/services/agente.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-vincular-prefeituras-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4 select-none font-sans overflow-y-auto">
      <div class="bg-[#111827] border border-white/10 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-5 my-8 animate-in fade-in duration-150">
        <!-- Cabeçalho -->
        <div class="flex items-center justify-between pb-3 border-b border-white/10">
          <div class="flex items-center gap-2.5">
            <div class="w-9 h-9 rounded-lg bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center text-base">
              🏛️
            </div>
            <div>
              <h3 class="text-sm font-bold text-white tracking-tight">Vincular Prefeituras ao Agente</h3>
              <p class="text-[11px] text-gov-slate-400">
                Defina o escopo de municípios que <strong class="text-white">{{ agente.nome }}</strong> poderá visualizar e operar
              </p>
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

        <!-- Dados do Agente -->
        <div class="p-3 rounded-lg bg-[#0A0E17] border border-white/5 flex items-center justify-between text-xs">
          <div class="space-y-0.5">
            <span class="text-white font-medium block">{{ agente.nome }}</span>
            <span class="text-gov-slate-400 text-[11px] block">{{ agente.email }}</span>
          </div>
          <div class="text-right space-y-0.5">
            <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-700/50 text-slate-300 border border-slate-600/40 font-bold block">
              {{ agente.role }}
            </span>
            <span class="text-[10px] text-gov-slate-400 font-mono block">
              {{ agente.telefoneCelular || 'Sem celular' }}
            </span>
          </div>
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

        <!-- Lista de Prefeituras -->
        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <label class="text-xs font-semibold text-gov-slate-300">
              Municípios Cadastrados na Consultoria
            </label>
            <span class="text-[11px] text-gov-slate-400 font-mono">
              {{ prefeiturasSelecionadas().size }} de {{ municipioCtx.municipios().length }} selecionado(s)
            </span>
          </div>

          <div class="max-h-60 overflow-y-auto bg-[#0A0E17] border border-white/10 rounded-lg p-2.5 space-y-2 divide-y divide-white/5">
            @for (mun of municipioCtx.municipios(); track mun.id) {
              <label class="flex items-center gap-3 pt-2 first:pt-0 cursor-pointer text-xs text-gov-slate-200 hover:text-white group">
                <input
                  type="checkbox"
                  [checked]="prefeiturasSelecionadas().has(mun.id)"
                  (change)="togglePrefeitura(mun.id)"
                  class="rounded border-white/20 bg-white/5 text-gov-cobalt-600 focus:ring-gov-cobalt-500 w-4 h-4 cursor-pointer"
                />
                <div class="flex items-center justify-between w-full">
                  <div>
                    <div class="font-medium text-white group-hover:text-gov-cobalt-300 transition-colors">
                      {{ mun.nome }} - {{ mun.uf }}
                    </div>
                    <div class="text-[10px] text-gov-slate-400 font-mono">
                      CNPJ: {{ mun.cnpj }}
                    </div>
                  </div>
                  <span class="text-[10px] text-gov-slate-400 font-mono bg-white/5 px-1.5 py-0.5 rounded">
                    IBGE {{ mun.codigoIbge }}
                  </span>
                </div>
              </label>
            }

            @if (municipioCtx.municipios().length === 0) {
              <div class="text-xs text-gov-slate-400 text-center py-6 space-y-1">
                <div class="text-2xl">🏛️</div>
                <p class="font-medium text-gov-slate-300">Nenhuma prefeitura cadastrada na consultoria ainda.</p>
                <p class="text-[11px] text-gov-slate-500">Cadastre uma prefeitura pelo seletor no topo da tela antes de vincular.</p>
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
            type="button"
            (click)="salvar()"
            [disabled]="carregando()"
            class="px-4 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 disabled:opacity-50 text-xs font-semibold text-white transition-all shadow-sm flex items-center gap-2 cursor-pointer"
          >
            @if (carregando()) {
              <span class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              <span>Salvando...</span>
            } @else {
              <span>Salvar Vínculos</span>
            }
          </button>
        </div>
      </div>
    </div>
  `
})
export class VincularPrefeiturasModalComponent implements OnInit {
  @Input({ required: true }) agente!: Agente;
  @Output() fechar = new EventEmitter<void>();
  @Output() salvo = new EventEmitter<void>();

  private readonly agenteService = inject(AgenteService);
  readonly municipioCtx = inject(MunicipioContextService);
  private readonly toast = inject(ToastService);

  prefeiturasSelecionadas = signal<Set<string>>(new Set());
  carregando = signal(false);
  erro = signal<string | null>(null);

  ngOnInit(): void {
    if (this.agente && this.agente.prefeiturasAtribuidasIds) {
      this.prefeiturasSelecionadas.set(new Set(this.agente.prefeiturasAtribuidasIds));
    }
  }

  togglePrefeitura(id: string): void {
    const atual = new Set(this.prefeiturasSelecionadas());
    if (atual.has(id)) {
      atual.delete(id);
    } else {
      atual.add(id);
    }
    this.prefeiturasSelecionadas.set(atual);
  }

  salvar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const ids = Array.from(this.prefeiturasSelecionadas());

    this.agenteService.atualizarAgente(this.agente.id, {
      prefeiturasIds: ids
    }).subscribe({
      next: () => {
        this.carregando.set(false);
        this.toast.sucesso(
          'Vínculos Atualizados!',
          `${ids.length} prefeitura(s) atribuída(s) a ${this.agente.nome}.`
        );
        this.salvo.emit();
        this.fechar.emit();
      },
      error: (err) => {
        this.carregando.set(false);
        const msg = err?.error?.detail || err?.error?.message || 'Falha ao atualizar prefeituras do agente.';
        this.erro.set(msg);
        this.toast.erro(msg);
      }
    });
  }

  fecharModal(): void {
    this.fechar.emit();
  }
}
