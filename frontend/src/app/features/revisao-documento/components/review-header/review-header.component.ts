import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RevisaoStateService } from '../../services/revisao-state.service';
import { AuthService } from '../../../../core/auth/auth.service';

@Component({
  selector: 'app-review-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="h-14 bg-gov-slate-900 border-b border-gov-slate-800 px-4 flex items-center justify-between text-gov-slate-100 select-none z-40">
      <!-- Lado Esquerdo: Logo e Navegação -->
      <div class="flex items-center gap-4">
        <a routerLink="/" class="flex items-center gap-2 font-bold text-base text-white tracking-tight hover:opacity-90">
          <span class="flex items-center justify-center w-7 h-7 rounded-lg bg-gov-cobalt-600 font-black text-sm">G</span>
          <span>Gov<span class="text-gov-cobalt-400">Flow</span></span>
        </a>

        <div class="h-4 w-px bg-gov-slate-700"></div>

        <div class="flex items-center gap-2 text-xs text-gov-slate-300">
          <span class="text-gov-slate-400 font-medium">Conferência Lado a Lado:</span>
          <span class="font-semibold text-white">{{ state.documentoAtual()?.nomeArquivoOriginal || 'Carregando...' }}</span>
        </div>
      </div>

      <!-- Centro: Informações do Convênio / Prefeitura -->
      <div class="hidden md:flex items-center gap-3 px-3 py-1 rounded-full bg-gov-slate-800/80 border border-gov-slate-700 text-xs">
        <span class="text-gov-slate-400">Convênio:</span>
        <span class="font-mono font-semibold text-gov-cobalt-300">
          {{ state.documentoAtual()?.convenioId ? state.documentoAtual()?.convenioId?.substring(0, 8) : 'Geral Transferegov' }}
        </span>
        <span class="text-gov-slate-500">•</span>
        <span class="text-gov-slate-400">Status:</span>
        <span class="font-semibold text-amber-400">Human-in-the-Loop</span>
      </div>

      <!-- Lado Direito: Drawer de Pendências e Perfil -->
      <div class="flex items-center gap-3">
        <!-- Botão de Fila de Pendências -->
        <button
          type="button"
          (click)="state.toggleDrawer()"
          class="relative px-3 py-1.5 rounded-lg bg-gov-slate-800 hover:bg-gov-slate-700 border border-gov-slate-700 text-xs font-semibold text-gov-slate-200 transition-colors flex items-center gap-2"
          title="Abrir fila de documentos pendentes [Alt + Q]"
        >
          <span>📋</span>
          <span>Fila Pendente</span>
          <span class="px-1.5 py-0.2 rounded-full bg-amber-500/20 text-amber-300 font-mono text-[11px] border border-amber-500/30">
            {{ state.totalPendentes() }}
          </span>
          <kbd class="hidden sm:inline-block text-[10px] text-gov-slate-400 font-mono bg-gov-slate-900 px-1 py-0.5 rounded">Alt+Q</kbd>
        </button>

        <!-- Perfil do Analista -->
        <div class="flex items-center gap-2 pl-2 border-l border-gov-slate-800 text-xs">
          <div class="text-right hidden sm:block">
            <div class="font-semibold text-gov-slate-200">{{ auth.usuario()?.nome || 'Analista' }}</div>
            <div class="text-[10px] text-gov-slate-400 font-mono">{{ auth.usuario()?.email || 'analista@govflow.com.br' }}</div>
          </div>
          <button
            type="button"
            (click)="auth.logout()"
            class="p-1.5 rounded-md text-gov-slate-400 hover:text-rose-400 hover:bg-gov-slate-800 transition-colors text-xs"
            title="Sair do sistema"
          >
            Sair
          </button>
        </div>
      </div>
    </header>
  `
})
export class ReviewHeaderComponent {
  public readonly state = inject(RevisaoStateService);
  public readonly auth = inject(AuthService);
}
