import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PrefeituraService, CadastrarPrefeituraPayload, PorteMunicipio } from '../../services/prefeitura.service';
import { MunicipioContextService } from '../../context/municipio-context.service';
import { ToastService } from '../../ui/toast.service';

@Component({
  selector: 'app-cadastrar-prefeitura-modal',
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
                <rect width="16" height="20" x="4" y="2" rx="2" ry="2"/>
                <path d="M9 22v-4h6v4"/>
                <path d="M8 6h.01"/>
                <path d="M16 6h.01"/>
                <path d="M12 6h.01"/>
                <path d="M12 10h.01"/>
                <path d="M12 14h.01"/>
              </svg>
            </div>
            <div>
              <h3 class="text-sm font-bold text-white tracking-tight">Cadastrar Nova Prefeitura Convenente</h3>
              <p class="text-[11px] text-gov-slate-400">Insira os dados do município contratante para monitoramento de convênios</p>
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
            <!-- CNPJ -->
            <div>
              <label for="cnpj" class="block text-xs font-medium text-gov-slate-300 mb-1">
                CNPJ da Prefeitura *
              </label>
              <input
                id="cnpj"
                type="text"
                [(ngModel)]="cnpj"
                name="cnpj"
                (input)="aplicarMascaraCnpj($event)"
                placeholder="00.000.000/0001-00"
                maxlength="18"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>

            <!-- Código IBGE -->
            <div>
              <label for="codigoIbge" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Código IBGE (7 dígitos) *
              </label>
              <input
                id="codigoIbge"
                type="text"
                [(ngModel)]="codigoIbge"
                name="codigoIbge"
                placeholder="2509701"
                maxlength="7"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>
          </div>

          <!-- Razão Social -->
          <div>
            <label for="razaoSocial" class="block text-xs font-medium text-gov-slate-300 mb-1">
              Razão Social Oficial *
            </label>
            <input
              id="razaoSocial"
              type="text"
              [(ngModel)]="razaoSocial"
              name="razaoSocial"
              placeholder="Prefeitura Municipal de Monteiro"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
            />
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <!-- Nome do Município -->
            <div class="sm:col-span-2">
              <label for="nomeMunicipio" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Nome do Município *
              </label>
              <input
                id="nomeMunicipio"
                type="text"
                [(ngModel)]="nomeMunicipio"
                name="nomeMunicipio"
                placeholder="Monteiro"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
              />
            </div>

            <!-- UF -->
            <div>
              <label for="uf" class="block text-xs font-medium text-gov-slate-300 mb-1">
                UF *
              </label>
              <input
                id="uf"
                type="text"
                [(ngModel)]="uf"
                name="uf"
                placeholder="PB"
                maxlength="2"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white uppercase placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 text-center font-bold"
              />
            </div>
          </div>

          <!-- Porte do Município -->
          <div>
            <label for="porteMunicipio" class="block text-xs font-medium text-gov-slate-300 mb-1">
              Porte do Município (Critério TCU / STN) *
            </label>
            <select
              id="porteMunicipio"
              [(ngModel)]="porteMunicipio"
              name="porteMunicipio"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 cursor-pointer"
            >
              <option value="PEQUENO_PORTE_1">Pequeno Porte I (Até 20.000 hab.)</option>
              <option value="PEQUENO_PORTE_2">Pequeno Porte II (20.001 a 50.000 hab.)</option>
              <option value="MEDIO_PORTE">Médio Porte (50.001 a 100.000 hab.)</option>
              <option value="GRANDE_PORTE">Grande Porte (Mais de 100.000 hab.)</option>
            </select>
          </div>

          <!-- Dados do Gestor (Opcionais) -->
          <div class="pt-2 border-t border-white/5 space-y-3">
            <span class="text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider block">
              Dados do Mandato do Prefeito (Opcional)
            </span>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label for="nomePrefeito" class="block text-[11px] text-gov-slate-400 mb-1">Nome do Prefeito</label>
                <input
                  id="nomePrefeito"
                  type="text"
                  [(ngModel)]="nomePrefeito"
                  name="nomePrefeito"
                  placeholder="Nome Completo do Gestor"
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
                />
              </div>

              <div>
                <label for="cpfPrefeito" class="block text-[11px] text-gov-slate-400 mb-1">CPF do Prefeito</label>
                <input
                  id="cpfPrefeito"
                  type="text"
                  [(ngModel)]="cpfPrefeito"
                  name="cpfPrefeito"
                  placeholder="000.000.000-00"
                  maxlength="14"
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                />
              </div>
            </div>
          </div>

          <!-- Rodapé com Ações -->
          <div class="pt-4 border-t border-white/10 flex items-center justify-end gap-3">
            <button
              type="button"
              (click)="fecharModal()"
              class="px-4 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-xs font-semibold text-gov-slate-300 transition-colors cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              [disabled]="salvando()"
              class="px-5 py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-xs font-semibold text-white transition-colors shadow-sm disabled:opacity-50 flex items-center gap-2 cursor-pointer"
            >
              @if (salvando()) {
                <span class="animate-spin text-sm">⟳</span>
                <span>Salvando...</span>
              } @else {
                <span>Cadastrar Prefeitura</span>
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CadastrarPrefeituraModalComponent {
  private readonly prefeituraService = inject(PrefeituraService);
  private readonly municipioCtx = inject(MunicipioContextService);
  private readonly toast = inject(ToastService);

  @Output() fechar = new EventEmitter<void>();
  @Output() cadastrado = new EventEmitter<any>();

  public cnpj = '';
  public codigoIbge = '';
  public razaoSocial = '';
  public nomeMunicipio = '';
  public uf = 'PB';
  public porteMunicipio: PorteMunicipio = 'PEQUENO_PORTE_1';
  public nomePrefeito = '';
  public cpfPrefeito = '';

  public readonly salvando = signal(false);
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
    this.cnpj = valor;
  }

  fecharModal(): void {
    this.fechar.emit();
  }

  salvar(): void {
    if (!this.cnpj || !this.razaoSocial || !this.nomeMunicipio || !this.uf || !this.codigoIbge) {
      this.erro.set('Preencha todos os campos obrigatórios (*).');
      return;
    }

    if (this.codigoIbge.trim().length !== 7) {
      this.erro.set('O Código IBGE deve possuir exatamente 7 dígitos numéricos.');
      return;
    }

    this.salvando.set(true);
    this.erro.set(null);

    const payload: CadastrarPrefeituraPayload = {
      cnpj: this.cnpj.trim(),
      razaoSocial: this.razaoSocial.trim(),
      nomeMunicipio: this.nomeMunicipio.trim(),
      uf: this.uf.trim().toUpperCase(),
      codigoIbge: this.codigoIbge.trim(),
      porteMunicipio: this.porteMunicipio,
      nomePrefeito: this.nomePrefeito ? this.nomePrefeito.trim() : undefined,
      cpfPrefeito: this.cpfPrefeito ? this.cpfPrefeito.trim() : undefined
    };

    this.prefeituraService.cadastrarPrefeitura(payload).subscribe({
      next: (resp) => {
        this.salvando.set(false);
        this.municipioCtx.adicionarMunicipio({
          id: resp.id,
          nome: resp.nomeMunicipio,
          uf: resp.uf,
          codigoIbge: resp.codigoIbge,
          cnpj: resp.cnpj,
          conveniosAtivos: 0,
          prazosCriticos: 0,
          situacaoCauc: 'REGULAR'
        });
        this.toast.sucesso('Prefeitura Cadastrada!', `Prefeitura de ${resp.nomeMunicipio} - ${resp.uf} está ativa.`);
        this.cadastrado.emit(resp);
        this.fecharModal();
      },
      error: (err) => {
        this.salvando.set(false);
        const msg = err?.error?.detail || err?.error?.message || 'Falha ao cadastrar prefeitura. Verifique os dados informados.';
        this.erro.set(msg);
        this.toast.erro(msg);
      }
    });
  }
}
