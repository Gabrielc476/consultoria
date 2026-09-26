import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MunicipioContextService } from '../../context/municipio-context.service';
import { ConvenioContextService } from '../../../features/convenios/services/convenio-context.service';
import { AuthService } from '../../auth/auth.service';

/**
 * =========================================================================
 * 📌 MAPEAMENTO DE PLACEHOLDER - TASK-FE-03 (Header)
 * -------------------------------------------------------------------------
 * O QUE É MOCK:
 * - O contador de notificações (ex: 3 alertas não lidos) é local.
 * - A busca global (Ctrl+K) pesquisa convênios/documentos em memória.
 *
 * O QUE SUBSTITUI NO FUTURO:
 * - Notificações em tempo real conectadas com RabbitMQ / WebSockets.
 * - Busca indexada global conectada ao Core Service e Transferegov Service.
 * =========================================================================
 */
@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <header class="h-16 bg-[#111827] border-b border-white/10 px-6 flex items-center justify-between gap-4 select-none shrink-0 z-30">
      <!-- Lado Esquerdo: Seletor Global de Município & Convênio Ativo -->
      <div class="flex items-center gap-3">
        <!-- 1. Município Switcher -->
        <div class="relative">
          <button
            type="button"
            (click)="menuMunicipiosAberto.set(!menuMunicipiosAberto())"
            class="flex items-center gap-2.5 px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/10 text-xs font-medium text-white transition-all focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 cursor-pointer"
          >
            <!-- Ícone de Brasão / Edifício Municipal -->
            <svg class="w-3.5 h-3.5 text-gov-cobalt-400 shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect width="16" height="20" x="4" y="2" rx="2" ry="2"/>
              <path d="M9 22v-4h6v4"/>
              <path d="M8 6h.01"/>
              <path d="M16 6h.01"/>
              <path d="M12 6h.01"/>
              <path d="M12 10h.01"/>
              <path d="M12 14h.01"/>
              <path d="M16 10h.01"/>
              <path d="M16 14h.01"/>
              <path d="M8 10h.01"/>
              <path d="M8 14h.01"/>
            </svg>
            <span class="font-semibold text-gov-slate-100 tracking-tight">
              {{ municipioCtx.nomeMunicipioAtivoFormatado() }}
            </span>
            <svg class="w-3.5 h-3.5 text-gov-slate-400 transition-transform" [class.rotate-180]="menuMunicipiosAberto()" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="m6 9 6 6 6-6"/>
            </svg>
          </button>

          <!-- Dropdown Flutuante de Seleção de Município -->
          @if (menuMunicipiosAberto()) {
            <div class="absolute left-0 mt-2 w-64 bg-[#111827] border border-white/10 rounded-xl shadow-2xl py-1.5 z-50 divide-y divide-white/5 animate-in fade-in duration-100">
              <div class="px-3 py-1.5 text-[10px] font-semibold text-gov-slate-400 uppercase tracking-wider">
                Alternar Prefeitura Ativa
              </div>
              <div class="py-1 max-h-60 overflow-y-auto">
                @for (mun of municipioCtx.municipios(); track mun.id) {
                  <button
                    type="button"
                    (click)="trocarMunicipio(mun.id)"
                    class="w-full text-left px-3 py-2 text-xs flex items-center justify-between hover:bg-white/5 transition-colors group cursor-pointer"
                    [ngClass]="{'bg-gov-cobalt-500/10': municipioCtx.municipioAtivoId() === mun.id}"
                  >
                    <div class="flex items-center gap-2">
                      <span class="w-1.5 h-1.5 rounded-full" [class.bg-gov-cobalt-400]="municipioCtx.municipioAtivoId() === mun.id" [class.bg-gov-slate-600]="municipioCtx.municipioAtivoId() !== mun.id"></span>
                      <span class="font-medium" [class.text-gov-cobalt-300]="municipioCtx.municipioAtivoId() === mun.id" [class.text-gov-slate-200]="municipioCtx.municipioAtivoId() !== mun.id">
                        {{ mun.nome }} - {{ mun.uf }}
                      </span>
                    </div>
                    <div class="flex items-center gap-1.5">
                      <span
                        class="text-[9px] font-semibold px-1.5 py-0.5 rounded"
                        [ngClass]="{
                          'bg-emerald-500/15 text-emerald-400 border border-emerald-500/25': mun.situacaoCauc === 'REGULAR',
                          'bg-amber-500/15 text-amber-400 border border-amber-500/25': mun.situacaoCauc === 'ALERTA',
                          'bg-rose-500/15 text-rose-400 border border-rose-500/25': mun.situacaoCauc === 'BLOQUEADO'
                        }"
                        [title]="'Situação Fiscal CAUC: ' + mun.situacaoCauc"
                      >
                        {{ mun.situacaoCauc === 'BLOQUEADO' ? 'BLOQUEADO' : mun.situacaoCauc }}
                      </span>
                      <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-white/5 text-gov-slate-400">
                        {{ mun.conveniosAtivos }} convênios
                      </span>
                    </div>
                  </button>
                }
              </div>
            </div>
          }
        </div>

        <!-- 2. Convênio Ativo Pill (Atalho) -->
        <a
          routerLink="/convenios"
          class="hidden md:flex items-center gap-2 px-2.5 py-1 rounded-md bg-white/5 hover:bg-white/10 border border-white/5 text-xs text-gov-slate-300 transition-colors"
          title="Ver Cockpit do Convênio Selecionado"
        >
          <span class="w-2 h-2 rounded-full bg-gov-cobalt-400"></span>
          <span class="font-mono text-gov-cobalt-300 font-bold">#{{ convenioCtx.convenioAtivo().numeroSiconv }}</span>
          <span class="text-gov-slate-400 text-[11px] truncate max-w-[140px]">{{ convenioCtx.convenioAtivo().objeto }}</span>
        </a>
      </div>

      <!-- Lado Direito: Busca Global (Ctrl+K) & Atalhos -->
      <div class="flex items-center gap-3">
        <!-- Campo de Busca Estilo Linear / Spotlight -->
        <div class="relative w-52 sm:w-72">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gov-slate-500">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.3-4.3"/>
            </svg>
          </div>
          <input
            type="text"
            placeholder="Buscar convênio, nota ou credor..."
            class="w-full pl-9 pr-12 py-1.5 bg-white/5 border border-white/10 rounded-lg text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 focus:border-gov-cobalt-500 transition-all font-sans"
          />
          <div class="absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none">
            <kbd class="px-1.5 py-0.5 text-[10px] font-mono text-gov-slate-400 bg-white/5 border border-white/10 rounded">⌘K</kbd>
          </div>
        </div>

        <!-- Atalho Central WhatsApp -->
        <button
          type="button"
          (click)="irParaWhatsApp()"
          class="relative p-2 rounded-lg text-gov-slate-400 hover:text-white hover:bg-white/5 transition-colors focus:outline-none cursor-pointer"
          title="Abrir Central de Mensagens WhatsApp"
        >
          <svg class="w-4 h-4 text-emerald-400" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19.01L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 14.99 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67Z"/>
          </svg>
          <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
        </button>

        <!-- Sininho de Notificações com Badge -->
        <button
          type="button"
          (click)="irParaRadar()"
          class="relative p-2 rounded-lg text-gov-slate-400 hover:text-white hover:bg-white/5 transition-colors focus:outline-none cursor-pointer"
          title="Ver Alertas e Prazos Críticos"
        >
          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9"/>
            <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0"/>
          </svg>
          <span class="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-rose-500"></span>
        </button>

        <!-- Divisor Sutil -->
        <div class="w-px h-6 bg-white/10"></div>

        <!-- Consultoria Tenant Badge -->
        <div class="flex items-center gap-2 text-xs">
          <div class="text-right hidden sm:block">
            <div class="font-medium text-white text-[11px] leading-tight">Consultoria Aliança</div>
            <div class="text-[10px] text-gov-slate-400 leading-tight">Patos & Sertão PB</div>
          </div>
        </div>
      </div>
    </header>
  `
})
export class HeaderComponent {
  readonly municipioCtx = inject(MunicipioContextService);
  readonly convenioCtx = inject(ConvenioContextService);
  readonly router = inject(Router);

  readonly menuMunicipiosAberto = signal(false);

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.menuMunicipiosAberto.set(false);
    }
  }

  trocarMunicipio(id: string): void {
    this.municipioCtx.selecionarMunicipio(id);
    this.menuMunicipiosAberto.set(false);
  }

  irParaRadar(): void {
    this.router.navigate(['/radar-cauc']);
  }

  irParaWhatsApp(): void {
    this.router.navigate(['/whatsapp']);
  }
}
