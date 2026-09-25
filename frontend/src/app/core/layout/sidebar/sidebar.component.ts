import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <aside class="w-64 h-screen bg-[#111827] border-r border-white/10 flex flex-col justify-between shrink-0 select-none">
      <!-- Topo: Marca GovFlow -->
      <div>
        <div class="h-16 px-5 border-b border-white/10 flex items-center gap-3">
          <div class="w-8 h-8 rounded-lg bg-gov-cobalt-600 flex items-center justify-center font-bold text-white shadow-sm text-sm shrink-0">
            <svg class="w-4 h-4 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="12 2 2 7 12 12 22 7 12 2"/>
              <polyline points="2 17 12 22 22 17"/>
              <polyline points="2 12 12 17 22 12"/>
            </svg>
          </div>
          <div class="min-w-0">
            <div class="font-bold text-sm text-white tracking-tight leading-none flex items-center gap-1.5">
              <span>GovFlow</span>
              <span class="text-[9px] font-mono px-1.5 py-0.5 rounded bg-gov-cobalt-500/20 text-gov-cobalt-400 font-bold border border-gov-cobalt-500/30">PRO</span>
            </div>
            <div class="text-[11px] text-gov-slate-400 mt-1 font-normal truncate">Copiloto Transferegov</div>
          </div>
        </div>

        <!-- Menu de Navegação Principal -->
        <nav class="p-3 space-y-1.5 text-xs">
          <div class="px-3 pt-2 pb-1 text-[10px] font-semibold text-gov-slate-500 uppercase tracking-wider">
            Gestão Municipal
          </div>

          <!-- Cockpit do Convênio (Fases 0 a 9) -->
          <a
            routerLink="/convenios"
            [routerLinkActiveOptions]="{exact: true}"
            routerLinkActive="bg-gov-cobalt-600/15 text-gov-cobalt-300 font-semibold border border-gov-cobalt-500/30 shadow-sm"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gov-slate-300 hover:text-white hover:bg-white/5 transition-all group cursor-pointer"
          >
            <svg class="w-4 h-4 shrink-0 transition-colors group-hover:text-white text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z"/>
              <path d="M6 6h10"/>
              <path d="M6 10h10"/>
              <path d="M6 14h6"/>
            </svg>
            <span>Cockpit do Convênio</span>
          </a>

          <!-- Lista Geral de Convênios -->
          <a
            routerLink="/convenios/lista"
            routerLinkActive="bg-gov-cobalt-600/15 text-gov-cobalt-300 font-semibold border border-gov-cobalt-500/30 shadow-sm"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gov-slate-300 hover:text-white hover:bg-white/5 transition-all group cursor-pointer"
          >
            <svg class="w-4 h-4 shrink-0 transition-colors group-hover:text-white text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect width="7" height="7" x="3" y="3" rx="1"/>
              <rect width="7" height="7" x="14" y="3" rx="1"/>
              <rect width="7" height="7" x="14" y="14" rx="1"/>
              <rect width="7" height="7" x="3" y="14" rx="1"/>
            </svg>
            <span>Lista de Convênios</span>
          </a>

          <!-- Central WhatsApp (Mensageria & Fiscais) -->
          <a
            routerLink="/whatsapp"
            routerLinkActive="bg-gov-cobalt-600/15 text-gov-cobalt-300 font-semibold border border-gov-cobalt-500/30 shadow-sm"
            class="flex items-center justify-between px-3 py-2.5 rounded-lg text-gov-slate-300 hover:text-white hover:bg-white/5 transition-all group cursor-pointer"
          >
            <div class="flex items-center gap-3">
              <svg class="w-4 h-4 shrink-0 text-emerald-400" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19.01L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 14.99 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67Z"/>
              </svg>
              <span>Central WhatsApp</span>
            </div>
            <span class="w-4 h-4 rounded-full bg-emerald-500 text-white font-bold text-[9px] flex items-center justify-center shadow-sm">
              2
            </span>
          </a>

          <!-- Esteira de Documentos (WhatsApp / OCR) -->
          <a
            routerLink="/documentos"
            routerLinkActive="bg-gov-cobalt-600/15 text-gov-cobalt-300 font-semibold border border-gov-cobalt-500/30 shadow-sm"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gov-slate-300 hover:text-white hover:bg-white/5 transition-all group cursor-pointer"
          >
            <svg class="w-4 h-4 shrink-0 transition-colors group-hover:text-white text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/>
              <polyline points="14 2 14 8 20 8"/>
              <line x1="16" y1="13" x2="8" y2="13"/>
              <line x1="16" y1="17" x2="8" y2="17"/>
              <line x1="10" y1="9" x2="8" y2="9"/>
            </svg>
            <span>Esteira de Documentos</span>
          </a>

          <!-- Radar CAUC & Prazos (LRF 25) -->
          <a
            routerLink="/radar-cauc"
            routerLinkActive="bg-gov-cobalt-600/15 text-gov-cobalt-300 font-semibold border border-gov-cobalt-500/30 shadow-sm"
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-gov-slate-300 hover:text-white hover:bg-white/5 transition-all group cursor-pointer"
          >
            <svg class="w-4 h-4 shrink-0 transition-colors group-hover:text-white text-gov-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="m4.93 4.93 4.24 4.24"/>
              <path d="m14.83 9.17 4.24-4.24"/>
              <path d="m14.83 14.83 4.24 4.24"/>
              <path d="m9.17 14.83-4.24 4.24"/>
              <circle cx="12" cy="12" r="2"/>
            </svg>
            <span class="flex-1">Radar CAUC & Prazos</span>
            <span class="w-2 h-2 rounded-full bg-rose-500 animate-pulse" title="Prazos Críticos Pendentes"></span>
          </a>
        </nav>
      </div>

      <!-- Rodapé da Sidebar: Status de Conexão e Perfil -->
      <div class="p-4 border-t border-white/10 space-y-3">
        <!-- Badge de Conectividade do WhatsApp (Evolution API) -->
        <div class="px-3 py-2 rounded-lg bg-white/5 border border-white/5 flex items-center justify-between text-[11px]">
          <div class="flex items-center gap-2 text-gov-slate-300">
            <span class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span class="font-medium">WhatsApp Ativo</span>
          </div>
          <span class="font-mono text-[10px] text-gov-slate-400 bg-white/5 px-1.5 py-0.5 rounded">Online</span>
        </div>

        <!-- Usuário Logado & Logout -->
        <div class="flex items-center justify-between pt-1 text-xs text-gov-slate-400">
          <div class="flex items-center gap-2.5 min-w-0">
            <div class="w-7 h-7 rounded-full bg-gov-cobalt-600/30 border border-gov-cobalt-500/40 flex items-center justify-center text-[11px] font-bold text-gov-cobalt-300 shrink-0">
              {{ (auth.usuario()?.nome || 'A').charAt(0) }}
            </div>
            <div class="min-w-0">
              <div class="truncate text-gov-slate-200 text-xs font-medium">{{ auth.usuario()?.nome || 'Consultor' }}</div>
              <div class="text-[10px] text-gov-slate-500 truncate">Analista Sênior</div>
            </div>
          </div>
          <button
            type="button"
            (click)="auth.logout()"
            class="text-rose-400 hover:text-rose-300 text-xs hover:underline cursor-pointer ml-2 shrink-0"
            title="Encerrar Sessão"
          >
            Sair
          </button>
        </div>
      </div>
    </aside>
  `
})
export class SidebarComponent {
  readonly auth = inject(AuthService);
}
