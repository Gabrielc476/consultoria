import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { CnpjPipe } from '../../../../shared/pipes/cnpj.pipe';
import { validarCnpj, sugerirCnpjCorreto } from '../../../../shared/utils/cnpj-validator';

@Component({
  selector: 'app-audit-checklist',
  standalone: true,
  imports: [CommonModule, CurrencyBrlPipe, CnpjPipe],
  template: `
    <div class="rounded-xl border transition-all duration-300 p-4" [ngClass]="containerClasses()">
      <!-- Topo: Diagnóstico Geral da Auditoria -->
      <div class="flex items-center justify-between pb-3 mb-3 border-b" [ngClass]="borderClasses()">
        <div class="flex items-center gap-2.5">
          <div class="w-8 h-8 rounded-lg flex items-center justify-center text-base font-bold shadow-sm" [ngClass]="iconBadgeClasses()">
            @if (estaProntoParaAprovacao()) {
              <span>⚡</span>
            } @else {
              <span>⚠️</span>
            }
          </div>
          <div>
            <div class="flex items-center gap-2">
              <span class="text-xs uppercase tracking-wider font-bold" [ngClass]="headerTagClasses()">
                {{ estaProntoParaAprovacao() ? 'Pronto para Aprovação Expressa' : 'Revisão Recomendada' }}
              </span>
              <span class="text-[11px] px-1.5 py-0.2 rounded font-mono bg-gov-slate-900/60 text-gov-slate-300 border border-gov-slate-700">
                Score Global: {{ Math.round((state.documentoAtual()?.extracaoSugerida?.confidenceScoreGeral || 0.95) * 100) }}%
              </span>
            </div>
            <p class="text-xs text-gov-slate-300 mt-0.5">
              {{ estaProntoParaAprovacao() ? 'Todas as regras tributárias e fiscais fecharam com precisão.' : mensagemAlerta() }}
            </p>
          </div>
        </div>

        @if (estaProntoParaAprovacao()) {
          <div class="hidden sm:flex items-center gap-1.5 text-xs text-emerald-400 font-mono bg-emerald-950/80 px-2.5 py-1 rounded-md border border-emerald-500/30">
            <span>Tecle</span>
            <kbd class="px-1.5 py-0.5 rounded bg-emerald-900 text-emerald-200 text-[10px] font-bold">Espaço</kbd>
            <span>para aprovar</span>
          </div>
        }
      </div>

      <!-- Grid dos 3 Pilares de Integridade (Checklist de 3 Segundos) -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-2.5 text-xs">
        <!-- 1. Pilar Matemático -->
        <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border transition-all" [ngClass]="matematicaClasses()">
          <div class="flex items-center justify-between mb-1">
            <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
              <span>{{ state.possuiDivergencia() ? '❌' : '🟢' }}</span> Matemática Fiscal
            </span>
            <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded" [ngClass]="state.possuiDivergencia() ? 'bg-rose-900/60 text-rose-300' : 'bg-emerald-900/60 text-emerald-300'">
              {{ state.possuiDivergencia() ? 'Divergente' : 'Fechada 100%' }}
            </span>
          </div>
          <div class="text-[11px] text-gov-slate-400 font-mono">
            {{ state.formulario().valorBruto | currencyBrl }} - {{ state.valorTotalDeducoes() | currencyBrl }}
          </div>
          <div class="font-bold text-white font-mono mt-0.5 flex items-center justify-between">
            <span>= {{ state.formulario().valorLiquido | currencyBrl }}</span>
            @if (state.possuiDivergencia()) {
              <button
                type="button"
                (click)="state.sincronizarValorLiquido()"
                class="text-[10px] px-1.5 py-0.5 rounded bg-rose-600 hover:bg-rose-500 text-white font-semibold flex items-center gap-1 cursor-pointer"
                title="Ajustar automaticamente o líquido"
              >
                <span>⚡</span> Corrigir
              </button>
            }
          </div>
        </div>

        <!-- 2. Pilar do Credor / Fornecedor -->
        <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border transition-all" [ngClass]="credorClasses()">
          <div class="flex items-center justify-between mb-1">
            <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
              <span>{{ credorValido() ? '🟢' : (cnpjInvalido() ? '❌' : '🟡') }}</span> Credor / Prestador
            </span>
            <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded" [ngClass]="credorBadgeClasses()">
              {{ credorValido() ? 'Identificado' : (cnpjInvalido() ? 'DV Inválido' : 'Verificar') }}
            </span>
          </div>
          <div class="text-[11px] text-white font-medium truncate" [title]="state.formulario().razaoSocialCredor">
            {{ state.formulario().razaoSocialCredor || 'Razão social não informada' }}
          </div>
          <div class="text-[11px] font-mono mt-0.5 flex items-center justify-between" [ngClass]="cnpjInvalido() ? 'text-rose-400 font-semibold' : 'text-gov-slate-400'">
            <span>{{ state.formulario().cnpjCredor ? (state.formulario().cnpjCredor | cnpj) : 'CNPJ pendente' }}</span>
            @if (cnpjInvalido() && sugestaoCnpj()) {
              <button
                type="button"
                (click)="aplicarSugestaoCnpj()"
                class="text-[10px] px-1.5 py-0.5 rounded bg-amber-600 hover:bg-amber-500 text-white font-semibold flex items-center gap-1 cursor-pointer"
                title="Corrigir dígitos verificadores do CNPJ automaticamente"
              >
                <span>⚡</span> Corrigir
              </button>
            }
          </div>
        </div>

        <!-- 3. Pilar Orçamentário / Empenho -->
        <div class="p-2.5 rounded-lg bg-gov-slate-900/80 border border-gov-slate-700/80 transition-all">
          <div class="flex items-center justify-between mb-1">
            <span class="font-semibold text-gov-slate-300 flex items-center gap-1.5">
              <span>🟢</span> Nota de Empenho
            </span>
            <span class="text-[10px] uppercase font-mono px-1.5 py-0.2 rounded bg-gov-cobalt-900/60 text-gov-cobalt-300">
              Convênio
            </span>
          </div>
          <div class="text-[11px] text-white font-mono font-semibold truncate">
            {{ state.formulario().numeroEmpenho || 'Não informado' }}
          </div>
          <div class="text-[10px] text-gov-slate-400 mt-0.5">
            Sugerido do Convênio Transferegov
          </div>
        </div>
      </div>
    </div>
  `
})
export class AuditChecklistComponent {
  public readonly Math = Math;
  public readonly state = inject(RevisaoStateService);

  public readonly cnpjInvalido = computed(() => {
    const cnpj = this.state.formulario().cnpjCredor;
    if (!cnpj) return false;
    return !validarCnpj(cnpj);
  });

  public readonly sugestaoCnpj = computed(() => {
    const cnpj = this.state.formulario().cnpjCredor;
    return sugerirCnpjCorreto(cnpj);
  });

  public readonly credorValido = computed(() => {
    const form = this.state.formulario();
    return !!(form.cnpjCredor && form.razaoSocialCredor && validarCnpj(form.cnpjCredor));
  });

  public readonly estaProntoParaAprovacao = computed(() => {
    return !this.state.possuiDivergencia() && this.credorValido();
  });

  public readonly containerClasses = computed(() => {
    if (this.estaProntoParaAprovacao()) {
      return 'bg-gradient-to-r from-emerald-950/40 via-gov-slate-900/70 to-gov-slate-900/90 border-emerald-500/30';
    }
    return 'bg-gradient-to-r from-amber-950/40 via-gov-slate-900/70 to-gov-slate-900/90 border-amber-500/40';
  });

  public readonly borderClasses = computed(() => {
    return this.estaProntoParaAprovacao() ? 'border-emerald-500/20' : 'border-amber-500/20';
  });

  public readonly headerTagClasses = computed(() => {
    return this.estaProntoParaAprovacao() ? 'text-emerald-400' : 'text-amber-400';
  });

  public readonly iconBadgeClasses = computed(() => {
    if (this.estaProntoParaAprovacao()) {
      return 'bg-emerald-600 text-white shadow-emerald-900/50';
    }
    return 'bg-amber-600 text-white shadow-amber-900/50';
  });

  public readonly matematicaClasses = computed(() => {
    if (this.state.possuiDivergencia()) {
      return 'border-rose-500/80 bg-rose-950/30 ring-1 ring-rose-500/50';
    }
    return 'border-emerald-500/30';
  });

  public readonly credorClasses = computed(() => {
    if (this.cnpjInvalido()) {
      return 'border-rose-500/80 bg-rose-950/20 ring-1 ring-rose-500/40';
    }
    return 'border-gov-slate-700/80';
  });

  public readonly credorBadgeClasses = computed(() => {
    if (this.credorValido()) {
      return 'bg-emerald-900/60 text-emerald-300';
    }
    if (this.cnpjInvalido()) {
      return 'bg-rose-900/60 text-rose-300';
    }
    return 'bg-amber-900/60 text-amber-300';
  });

  public readonly mensagemAlerta = computed(() => {
    if (this.state.possuiDivergencia()) {
      return `Divergência de cálculo: diferença de ${this.state.diferencaLiquido().toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })} no líquido.`;
    }
    if (this.cnpjInvalido()) {
      return 'O CNPJ do credor possui dígitos verificadores inválidos. Corrija antes de aprovar.';
    }
    if (!this.credorValido()) {
      return 'Dados cadastrais do credor incompletos.';
    }
    return 'Verifique as informações sinalizadas antes de aprovar.';
  });

  public aplicarSugestaoCnpj(): void {
    const sug = this.sugestaoCnpj();
    if (sug) {
      this.state.atualizarCampo('cnpjCredor', sug);
    }
  }
}

