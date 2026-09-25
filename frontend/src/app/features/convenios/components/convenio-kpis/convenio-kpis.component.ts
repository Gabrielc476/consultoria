import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CurrencyBrlPipe } from '../../../../shared/pipes/currency-brl.pipe';
import { ConvenioCockpit } from '../../model/convenio-fase.model';

@Component({
  selector: 'app-convenio-kpis',
  standalone: true,
  imports: [CommonModule, CurrencyBrlPipe],
  template: `
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <!-- Card 1: Total do Repasse Federal -->
      <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm relative overflow-hidden group hover:border-white/20 transition-all">
        <div class="flex items-center justify-between text-gov-slate-400">
          <span class="text-[11px] font-semibold uppercase tracking-wider">Total Repasse Federal</span>
          <svg class="w-4 h-4 text-gov-slate-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect width="20" height="12" x="2" y="6" rx="2"/>
            <circle cx="12" cy="12" r="2"/>
            <path d="M6 12h.01M18 12h.01"/>
          </svg>
        </div>
        <div class="text-2xl font-bold font-mono text-white mt-2 tracking-tight">
          {{ convenio?.valorRepasse | currencyBrl }}
        </div>
        <div class="flex items-center gap-1.5 mt-3 pt-3 border-t border-white/5 text-[11px] text-gov-slate-400">
          <span>Contrapartida Municipal:</span>
          <span class="font-mono text-gov-slate-300 font-medium">{{ convenio?.valorContrapartida | currencyBrl }}</span>
        </div>
      </div>

      <!-- Card 2: Execução Física Acumulada (RAE) -->
      <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm relative overflow-hidden group hover:border-white/20 transition-all">
        <div class="flex items-center justify-between text-gov-slate-400">
          <span class="text-[11px] font-semibold uppercase tracking-wider">Execução Física (RAE)</span>
          <span class="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-emerald-500/15 text-emerald-400 border border-emerald-500/25">Em Dia</span>
        </div>
        <div class="flex items-baseline gap-2 mt-2">
          <div class="text-2xl font-bold font-mono text-white tracking-tight">
            {{ convenio?.percentualExecucao }}%
          </div>
          <span class="text-[11px] text-gov-slate-400">medido pela Caixa</span>
        </div>
        <!-- Barra de Progresso Fino Linear -->
        <div class="w-full bg-white/10 h-1.5 rounded-full mt-3 overflow-hidden">
          <div
            class="bg-emerald-500 h-full rounded-full transition-all duration-500"
            [style.width.%]="convenio?.percentualExecucao || 0"
          ></div>
        </div>
      </div>

      <!-- Card 3: Saldo Conta Vinculada Op 006 -->
      <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm relative overflow-hidden group hover:border-white/20 transition-all">
        <div class="flex items-center justify-between text-gov-slate-400">
          <span class="text-[11px] font-semibold uppercase tracking-wider">Saldo Conta Op 006</span>
          <span class="font-mono text-[10px] text-gov-cobalt-400 bg-gov-cobalt-500/15 px-1.5 py-0.5 rounded border border-gov-cobalt-500/25 font-semibold">Caixa</span>
        </div>
        <div class="text-2xl font-bold font-mono text-gov-cobalt-300 mt-2 tracking-tight">
          {{ convenio?.saldoContaOp006 | currencyBrl }}
        </div>
        <div class="flex items-center gap-1.5 mt-3 pt-3 border-t border-white/5 text-[11px] text-emerald-400">
          <svg class="w-3.5 h-3.5 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
            <polyline points="16 7 22 7 22 13"/>
          </svg>
          <span>Rendimentos de aplicação ativos</span>
        </div>
      </div>

      <!-- Card 4: Prazo Fatal de Vigência -->
      <div class="bg-[#111827] border border-white/10 rounded-xl p-5 shadow-sm relative overflow-hidden group hover:border-white/20 transition-all">
        <div class="flex items-center justify-between text-gov-slate-400">
          <span class="text-[11px] font-semibold uppercase tracking-wider">Prazo Fatal de Vigência</span>
          <span class="text-[10px] font-sans font-semibold px-2 py-0.5 rounded bg-rose-500/15 text-rose-400 border border-rose-500/25">Urgente</span>
        </div>
        <div class="text-2xl font-bold font-mono text-rose-400 mt-2 tracking-tight">
          {{ convenio?.diasParaVencimento }} dias
        </div>
        <div class="flex items-center justify-between mt-3 pt-3 border-t border-white/5 text-[11px] text-gov-slate-400">
          <span>Término da Vigência:</span>
          <span class="font-mono text-gov-slate-200 font-medium">{{ convenio?.dataFimVigencia }}</span>
        </div>
      </div>
    </div>
  `
})
export class ConvenioKpisComponent {
  @Input() convenio: ConvenioCockpit | null = null;
}
