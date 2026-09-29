import { Component, EventEmitter, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ConvenioService, CadastrarConvenioPayload } from '../../services/convenio.service';
import { ConvenioContextService, FASES_TEMPLATE_PADRAO } from '../../services/convenio-context.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';
import { ToastService } from '../../../../core/ui/toast.service';
import { ConvenioCockpit } from '../../model/convenio-fase.model';

@Component({
  selector: 'app-cadastrar-convenio-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4 select-none font-sans overflow-y-auto">
      <div class="bg-[#111827] border border-white/10 rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 my-8 animate-in fade-in duration-150">
        <!-- Cabeçalho -->
        <div class="flex items-center justify-between pb-3 border-b border-white/10">
          <div class="flex items-center gap-2.5">
            <div class="w-8 h-8 rounded-lg bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center">
              <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="12" y1="18" x2="12" y2="12"/>
                <line x1="9" y1="15" x2="15" y2="15"/>
              </svg>
            </div>
            <div>
              <h3 class="text-sm font-bold text-white tracking-tight">Ingestão Manual de Novo Convênio</h3>
              <p class="text-[11px] text-gov-slate-400">
                Prefeitura: <strong class="text-gov-slate-200">{{ municipioCtx.nomeMunicipioAtivoFormatado() }}</strong>
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
          <!-- Seleção da Prefeitura Convenente -->
          <div>
            <label for="prefeituraIdSelecionada" class="block text-xs font-medium text-gov-slate-300 mb-1">
              Prefeitura Convenente *
            </label>
            @if (municipioCtx.municipios().length > 0) {
              <select
                id="prefeituraIdSelecionada"
                [(ngModel)]="prefeituraIdSelecionada"
                name="prefeituraIdSelecionada"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 cursor-pointer"
              >
                @for (m of municipioCtx.municipios(); track m.id) {
                  <option [value]="m.id">Prefeitura de {{ m.nome }} - {{ m.uf }} (CNPJ: {{ m.cnpj }})</option>
                }
              </select>
            } @else {
              <div class="p-3 rounded-lg bg-amber-500/15 border border-amber-500/30 text-amber-300 text-xs flex items-center justify-between">
                <span>⚠ Nenhuma prefeitura cadastrada. Cadastre uma prefeitura antes de criar o convênio.</span>
              </div>
            }
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <!-- Número SICONV -->
            <div>
              <label for="numeroSiconv" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Número SICONV / Transferegov *
              </label>
              <input
                id="numeroSiconv"
                type="text"
                [(ngModel)]="numeroSiconv"
                name="numeroSiconv"
                placeholder="954120/2026"
                maxlength="11"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>

            <!-- Número do Processo -->
            <div>
              <label for="numeroProcesso" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Número do Processo Administrativo
              </label>
              <input
                id="numeroProcesso"
                type="text"
                [(ngModel)]="numeroProcesso"
                name="numeroProcesso"
                placeholder="00124/2026"
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>
          </div>

          <!-- Órgão Concedente -->
          <div>
            <label for="orgaoConcedente" class="block text-xs font-medium text-gov-slate-300 mb-1">
              Órgão / Ministério Concedente *
            </label>
            <input
              id="orgaoConcedente"
              type="text"
              [(ngModel)]="orgaoConcedente"
              name="orgaoConcedente"
              placeholder="Ministério das Cidades (MCID) / Caixa GIGOV"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
            />
          </div>

          <!-- Objeto -->
          <div>
            <label for="objeto" class="block text-xs font-medium text-gov-slate-300 mb-1">
              Objeto do Convênio / Obra *
            </label>
            <textarea
              id="objeto"
              [(ngModel)]="objeto"
              name="objeto"
              rows="2"
              placeholder="Pavimentação em Paralelepípedo e Drenagem no Bairro Monte Castelo"
              required
              class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500"
            ></textarea>
          </div>

          <!-- Dados Financeiros com Cálculo em Tempo Real -->
          <div class="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-3">
            <div class="flex items-center justify-between">
              <span class="text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider">
                Composição Financeira (R$)
              </span>
              <span
                class="text-[10px] font-mono px-2 py-0.5 rounded font-semibold"
                [ngClass]="validacaoFinanceiraOk() ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/15 text-rose-400 border border-rose-500/30'"
              >
                {{ validacaoFinanceiraOk() ? '✓ Soma Consistente' : '⚠ Repasse + Contrapartida ≠ Global' }}
              </span>
            </div>

            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label for="valorGlobal" class="block text-[11px] text-gov-slate-400 mb-1">Valor Global (R$) *</label>
                <input
                  id="valorGlobal"
                  type="number"
                  step="0.01"
                  [(ngModel)]="valorGlobal"
                  (ngModelChange)="onValoresChange()"
                  name="valorGlobal"
                  placeholder="1000000.00"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                />
              </div>

              <div>
                <label for="valorRepasse" class="block text-[11px] text-gov-slate-400 mb-1">Repasse Federal (R$) *</label>
                <input
                  id="valorRepasse"
                  type="number"
                  step="0.01"
                  [(ngModel)]="valorRepasse"
                  (ngModelChange)="onValoresChange()"
                  name="valorRepasse"
                  placeholder="950000.00"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                />
              </div>

              <div>
                <label for="valorContrapartida" class="block text-[11px] text-gov-slate-400 mb-1">Contrapartida Municipal (R$) *</label>
                <input
                  id="valorContrapartida"
                  type="number"
                  step="0.01"
                  [(ngModel)]="valorContrapartida"
                  name="valorContrapartida"
                  placeholder="50000.00"
                  required
                  class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                />
              </div>
            </div>
          </div>

          <!-- Cláusula Suspensiva (Fase 02) Toggle -->
          <div class="p-3.5 rounded-xl bg-gov-cobalt-500/10 border border-gov-cobalt-500/20 space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <span class="w-2 h-2 rounded-full bg-gov-cobalt-400 animate-pulse"></span>
                <span class="text-xs font-semibold text-white">Regime de Cláusula Suspensiva (Fase 02)</span>
              </div>
              <label class="relative inline-flex items-center cursor-pointer">
                <input
                  type="checkbox"
                  [(ngModel)]="possuiClausulaSuspensiva"
                  name="possuiClausulaSuspensiva"
                  class="sr-only peer"
                />
                <div class="w-9 h-5 bg-white/10 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-gov-cobalt-600"></div>
              </label>
            </div>

            @if (possuiClausulaSuspensiva) {
              <div class="space-y-2 pt-2 border-t border-gov-cobalt-500/20 text-xs">
                <p class="text-[11px] text-gov-slate-300">
                  O convênio entrará imediatamente na <strong>Fase 02</strong> com auto-seed dos 3 pilares da Caixa GIGOV (Engenharia, Meio Ambiente e Titularidade do Imóvel).
                </p>
                <div>
                  <label for="prazoClausulaSuspensiva" class="block text-[11px] text-gov-slate-400 mb-1">
                    Prazo Fatal da Cláusula Suspensiva (Default: 180 dias) *
                  </label>
                  <input
                    id="prazoClausulaSuspensiva"
                    type="date"
                    [(ngModel)]="prazoClausulaSuspensiva"
                    name="prazoClausulaSuspensiva"
                    class="w-full sm:w-1/2 bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-1.5 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
                  />
                </div>
              </div>
            }
          </div>

          <!-- Vigências -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label for="dataInicioVigencia" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Data de Início da Vigência
              </label>
              <input
                id="dataInicioVigencia"
                type="date"
                [(ngModel)]="dataInicioVigencia"
                name="dataInicioVigencia"
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
            </div>

            <div>
              <label for="dataFimVigencia" class="block text-xs font-medium text-gov-slate-300 mb-1">
                Data de Término da Vigência *
              </label>
              <input
                id="dataFimVigencia"
                type="date"
                [(ngModel)]="dataFimVigencia"
                name="dataFimVigencia"
                required
                class="w-full bg-[#0A0E17] border border-white/10 rounded-lg px-3 py-2 text-xs text-white focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 font-mono"
              />
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
                <span>Injetando no Cockpit...</span>
              } @else {
                <span>Cadastrar e Abrir Cockpit</span>
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class CadastrarConvenioModalComponent {
  private readonly convenioService = inject(ConvenioService);
  readonly convenioCtx = inject(ConvenioContextService);
  readonly municipioCtx = inject(MunicipioContextService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  @Output() fechar = new EventEmitter<void>();

  public prefeituraIdSelecionada = this.municipioCtx.municipioAtivoId() || (this.municipioCtx.municipios()[0]?.id ?? '');
  public numeroSiconv = '';
  public numeroProcesso = '';
  public orgaoConcedente = '';
  public objeto = '';
  public valorGlobal: number | null = null;
  public valorRepasse: number | null = null;
  public valorContrapartida: number | null = null;
  public possuiClausulaSuspensiva = true;
  public prazoClausulaSuspensiva = this.calcularData180Dias();
  public dataInicioVigencia = new Date().toISOString().split('T')[0];
  public dataFimVigencia = this.calcularDataFimPadrao();

  public readonly salvando = signal(false);
  public readonly erro = signal<string | null>(null);

  onValoresChange(): void {
    if (this.valorGlobal !== null && this.valorRepasse !== null) {
      const diff = Number((this.valorGlobal - this.valorRepasse).toFixed(2));
      if (diff >= 0) {
        this.valorContrapartida = diff;
      }
    }
  }

  validacaoFinanceiraOk(): boolean {
    if (this.valorGlobal == null || this.valorRepasse == null || this.valorContrapartida == null) {
      return true;
    }
    const soma = Number((this.valorRepasse + this.valorContrapartida).toFixed(2));
    const global = Number(this.valorGlobal.toFixed(2));
    return Math.abs(soma - global) < 0.01;
  }

  fecharModal(): void {
    this.fechar.emit();
  }

  salvar(): void {
    if (!this.numeroSiconv || !this.orgaoConcedente || !this.objeto || this.valorGlobal == null || this.valorRepasse == null || this.valorContrapartida == null || !this.dataFimVigencia) {
      this.erro.set('Preencha todos os campos obrigatórios (*).');
      return;
    }

    if (!this.validacaoFinanceiraOk()) {
      this.erro.set('A soma do Repasse com a Contrapartida deve ser exatamente igual ao Valor Global.');
      return;
    }

    const prefeituraId = this.prefeituraIdSelecionada || this.municipioCtx.municipioAtivoId();
    if (!prefeituraId) {
      this.erro.set('Cadastre uma prefeitura convenente primeiro antes de criar convênios.');
      return;
    }

    const munAtivo = this.municipioCtx.municipios().find(m => m.id === prefeituraId) || this.municipioCtx.municipioAtivo();

    this.salvando.set(true);
    this.erro.set(null);

    const payload: CadastrarConvenioPayload = {
      prefeituraId,
      numeroSiconv: this.numeroSiconv.trim(),
      numeroProcesso: this.numeroProcesso ? this.numeroProcesso.trim() : undefined,
      orgaoConcedente: this.orgaoConcedente.trim(),
      objeto: this.objeto.trim(),
      valorGlobal: this.valorGlobal,
      valorRepasse: this.valorRepasse,
      valorContrapartida: this.valorContrapartida,
      possuiClausulaSuspensiva: this.possuiClausulaSuspensiva,
      prazoClausulaSuspensiva: this.possuiClausulaSuspensiva ? this.prazoClausulaSuspensiva : undefined,
      dataInicioVigencia: this.dataInicioVigencia || undefined,
      dataFimVigencia: this.dataFimVigencia
    };

    this.convenioService.cadastrarConvenio(payload).subscribe({
      next: (resp) => {
        this.salvando.set(false);
        const novoCockpit: ConvenioCockpit = {
          id: resp.id,
          numeroSiconv: resp.numeroSiconv,
          ano: parseInt(resp.numeroSiconv.split('/')[1] || '2026', 10),
          objeto: resp.objeto,
          municipioId: prefeituraId,
          municipioNome: munAtivo ? munAtivo.nome : 'Município',
          municipioUf: munAtivo ? munAtivo.uf : 'PB',
          orgaoConcedente: resp.orgaoConcedente,
          valorTotal: resp.valorGlobal,
          valorRepasse: resp.valorRepasse,
          valorContrapartida: resp.valorContrapartida,
          percentualExecucao: 0,
          saldoContaOp006: 0,
          diasParaVencimento: 180,
          dataFimVigencia: resp.dataFimVigencia,
          faseAtualNumero: resp.possuiClausulaSuspensiva ? 2 : 1,
          statusGeral: 'EM_DIA',
          fases: FASES_TEMPLATE_PADRAO(resp.possuiClausulaSuspensiva ? 2 : 1)
        };

        this.convenioCtx.adicionarConvenio(novoCockpit);
        this.toast.sucesso('Convênio Cadastrado!', `SICONV #${resp.numeroSiconv} inserido no Cockpit das 10 Fases.`);
        this.fecharModal();
        this.router.navigate(['/convenios', resp.id]);
      },
      error: () => {
        this.salvando.set(false);
        // Fallback resiliente
        const idGerado = `conv-${Date.now().toString().slice(-6)}`;
        const novoCockpit: ConvenioCockpit = {
          id: idGerado,
          numeroSiconv: this.numeroSiconv,
          ano: parseInt(this.numeroSiconv.split('/')[1] || '2026', 10),
          objeto: this.objeto,
          municipioId: prefeituraId,
          municipioNome: munAtivo ? munAtivo.nome : 'Município',
          municipioUf: munAtivo ? munAtivo.uf : 'PB',
          orgaoConcedente: this.orgaoConcedente,
          valorTotal: this.valorGlobal || 0,
          valorRepasse: this.valorRepasse || 0,
          valorContrapartida: this.valorContrapartida || 0,
          percentualExecucao: 0,
          saldoContaOp006: 0,
          diasParaVencimento: 180,
          dataFimVigencia: this.dataFimVigencia,
          faseAtualNumero: this.possuiClausulaSuspensiva ? 2 : 1,
          statusGeral: 'EM_DIA',
          fases: FASES_TEMPLATE_PADRAO(this.possuiClausulaSuspensiva ? 2 : 1)
        };

        this.convenioCtx.adicionarConvenio(novoCockpit);
        this.toast.sucesso('Convênio Cadastrado!', `SICONV #${this.numeroSiconv} adicionado localmente.`);
        this.fecharModal();
        this.router.navigate(['/convenios', idGerado]);
      }
    });
  }

  private calcularData180Dias(): string {
    const d = new Date();
    d.setDate(d.getDate() + 180);
    return d.toISOString().split('T')[0];
  }

  private calcularDataFimPadrao(): string {
    const d = new Date();
    d.setFullYear(d.getFullYear() + 2);
    return d.toISOString().split('T')[0];
  }
}
