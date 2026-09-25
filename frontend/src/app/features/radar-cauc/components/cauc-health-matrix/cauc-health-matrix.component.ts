import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CertidaoCaucItem } from '../../model/cauc.model';

@Component({
  selector: 'app-cauc-health-matrix',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex items-center gap-3">
      <!-- 4 Grupos de Certidões do CAUC (Total de 16 Requisitos) -->
      <div class="flex items-center gap-3 text-[10px]">
        <!-- Grupo 1: Obrigações Tributárias / CNDT / RFB -->
        <div class="flex flex-col items-center gap-1.5" title="Grupo I: Obrigações Tributárias, Previdenciárias e CNDT (4 itens)">
          <div class="flex items-center gap-1 bg-white/5 p-1 rounded-md border border-white/5">
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('1.1')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('1.2')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('1.3')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('1.4')"></span>
          </div>
          <span class="text-[9px] text-gov-slate-400 font-mono font-medium">TRIBUTOS</span>
        </div>

        <!-- Grupo 2: FGTS / Seguridade -->
        <div class="flex flex-col items-center gap-1.5" title="Grupo II: Regularidade FGTS e Seguridade Social (4 itens)">
          <div class="flex items-center gap-1 bg-white/5 p-1 rounded-md border border-white/5">
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('2.1')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('2.2')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('2.3')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('2.4')"></span>
          </div>
          <span class="text-[9px] text-gov-slate-400 font-mono font-medium">FGTS</span>
        </div>

        <!-- Grupo 3: Prestação de Contas Convênios STN / SICONV -->
        <div class="flex flex-col items-center gap-1.5" title="Grupo III: Prestação de Contas de Recursos Federais e TCE (4 itens)">
          <div class="flex items-center gap-1 bg-white/5 p-1 rounded-md border border-white/5">
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('3.1')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('3.2')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('3.3')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('3.4')"></span>
          </div>
          <span class="text-[9px] text-gov-slate-400 font-mono font-medium">CONTAS</span>
        </div>

        <!-- Grupo 4: SICONFI / Limites Constitucionais LRF -->
        <div class="flex flex-col items-center gap-1.5" title="Grupo IV: SICONFI, RREO, RGF e Limites de Saúde/Educação (4 itens)">
          <div class="flex items-center gap-1 bg-white/5 p-1 rounded-md border border-white/5">
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('4.1')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('4.2')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('4.3')"></span>
            <span class="w-2 h-2 rounded-full" [ngClass]="corDot('4.4')"></span>
          </div>
          <span class="text-[9px] text-gov-slate-400 font-mono font-medium">SICONFI</span>
        </div>
      </div>
    </div>
  `
})
export class CaucHealthMatrixComponent {
  @Input() certidoes: CertidaoCaucItem[] = [];

  corDot(codigo: string): string {
    const cert = this.certidoes.find(c => c.codigo === codigo);
    if (!cert) {
      return 'bg-emerald-400/90'; // Default saudável
    }
    if (cert.status === 'VENCIDA') {
      return 'bg-rose-500 shadow-sm animate-pulse';
    }
    if (cert.status === 'ALERTA') {
      return 'bg-amber-400';
    }
    return 'bg-emerald-400/90';
  }
}
