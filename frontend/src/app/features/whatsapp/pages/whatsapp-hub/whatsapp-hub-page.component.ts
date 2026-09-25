import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { WhatsAppService } from '../../services/whatsapp.service';
import { WhatsAppContact, WhatsAppMessage } from '../../model/whatsapp.model';
import { ConfidenceBadgeComponent } from '../../../../shared/ui/confidence-badge/confidence-badge.component';

@Component({
  selector: 'app-whatsapp-hub-page',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ConfidenceBadgeComponent],
  template: `
    <div class="h-[calc(100vh-4rem)] flex overflow-hidden bg-[#0A0E17] select-none">
      <!-- PAINEL ESQUERDO: Lista de Conversas / Contatos (320px) -->
      <div class="w-80 sm:w-96 border-r border-white/10 bg-[#111827] flex flex-col shrink-0">
        <!-- Topo da Lista: Status Gateway & Busca -->
        <div class="p-4 border-b border-white/10 space-y-3">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-7 h-7 rounded-lg bg-emerald-500/20 text-emerald-400 flex items-center justify-center font-bold">
                <!-- Ícone WhatsApp -->
                <svg class="w-4 h-4" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12.04 2C6.58 2 2.13 6.45 2.13 11.91C2.13 13.66 2.59 15.36 3.45 16.86L2.05 22L7.3 20.62C8.75 21.41 10.38 21.83 12.04 21.83C17.5 21.83 21.95 17.38 21.95 11.92C21.95 9.27 20.92 6.78 19.05 4.91C17.18 3.03 14.69 2 12.04 2M12.05 3.67C14.25 3.67 16.31 4.53 17.87 6.09C19.42 7.65 20.28 9.72 20.28 11.92C20.28 16.46 16.58 20.15 12.04 20.15C10.56 20.15 9.11 19.76 7.85 19.01L7.55 18.83L4.43 19.65L5.26 16.61L5.06 16.29C4.24 14.99 3.8 13.47 3.8 11.91C3.81 7.37 7.5 3.67 12.05 3.67Z"/>
                </svg>
              </div>
              <div>
                <h3 class="text-xs font-bold text-white tracking-tight">Central WhatsApp</h3>
                <div class="flex items-center gap-1.5 text-[10px] text-emerald-400">
                  <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                  <span>Evolution API Online</span>
                </div>
              </div>
            </div>

            <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 border border-white/10 text-gov-slate-400">
              Patos / PB
            </span>
          </div>

          <!-- Busca de Conversas -->
          <div class="relative">
            <input
              type="text"
              [ngModel]="termoBusca()"
              (ngModelChange)="termoBusca.set($event)"
              placeholder="Buscar contato ou mensagem..."
              class="w-full pl-8 pr-3 py-1.5 bg-white/5 border border-white/10 rounded-lg text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
            />
            <svg class="w-3.5 h-3.5 text-gov-slate-500 absolute left-2.5 top-2.5 pointer-events-none" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="m21 21-4.3-4.3"/>
            </svg>
          </div>

          <!-- Filtros de Categoria -->
          <div class="flex gap-1 overflow-x-auto text-[11px] pb-0.5">
            <button
              type="button"
              (click)="filtroCategoria.set('TODOS')"
              class="px-2.5 py-1 rounded-md transition-colors shrink-0"
              [ngClass]="filtroCategoria() === 'TODOS' ? 'bg-gov-cobalt-600 text-white font-medium' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
            >
              Todos ({{ whatsService.contatos().length }})
            </button>
            <button
              type="button"
              (click)="filtroCategoria.set('OBRAS')"
              class="px-2.5 py-1 rounded-md transition-colors shrink-0"
              [ngClass]="filtroCategoria() === 'OBRAS' ? 'bg-gov-cobalt-600 text-white font-medium' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
            >
              Fiscais de Obra
            </button>
            <button
              type="button"
              (click)="filtroCategoria.set('FINANCAS')"
              class="px-2.5 py-1 rounded-md transition-colors shrink-0"
              [ngClass]="filtroCategoria() === 'FINANCAS' ? 'bg-gov-cobalt-600 text-white font-medium' : 'text-gov-slate-400 hover:text-white hover:bg-white/5'"
            >
              Finanças
            </button>
          </div>
        </div>

        <!-- Lista Rolável de Contatos -->
        <div class="flex-1 overflow-y-auto divide-y divide-white/5">
          @for (c of contatosFiltrados(); track c.id) {
            <div
              (click)="whatsService.selecionarContato(c.id)"
              class="p-3.5 flex items-start gap-3 cursor-pointer transition-colors relative group"
              [ngClass]="c.id === whatsService.contatoAtivoId() ? 'bg-gov-cobalt-600/15 border-l-2 border-gov-cobalt-500' : 'hover:bg-white/[0.03]'"
            >
              <!-- Avatar com Inicial e Status Online -->
              <div class="relative shrink-0">
                <div class="w-10 h-10 rounded-full {{ c.avatarCor }} text-white font-bold flex items-center justify-center text-xs shadow-sm">
                  {{ c.nome.charAt(0) }}
                </div>
                @if (c.online) {
                  <span class="absolute bottom-0 right-0 w-2.5 h-2.5 rounded-full bg-emerald-500 border-2 border-[#111827]"></span>
                }
              </div>

              <!-- Informações do Contato e Última Mensagem -->
              <div class="min-w-0 flex-1">
                <div class="flex items-center justify-between gap-1">
                  <span class="text-xs font-semibold truncate" [class.text-white]="c.id === whatsService.contatoAtivoId()" [class.text-gov-slate-200]="c.id !== whatsService.contatoAtivoId()">
                    {{ c.nome }}
                  </span>
                  <span class="text-[10px] text-gov-slate-500 shrink-0 font-mono">
                    {{ c.ultimaMensagemHora }}
                  </span>
                </div>

                <div class="text-[10px] text-gov-cobalt-400 font-medium truncate mt-0.5">
                  {{ c.cargo }}
                </div>

                <div class="text-[11px] text-gov-slate-400 truncate mt-1">
                  {{ c.ultimaMensagemTexto }}
                </div>

                @if (c.convenioVinculadoNumero) {
                  <div class="mt-1.5 flex items-center gap-1.5">
                    <span class="text-[9px] font-mono px-1.5 py-0.5 rounded bg-white/5 text-gov-slate-400 border border-white/5">
                      Conv. #{{ c.convenioVinculadoNumero }}
                    </span>
                  </div>
                }
              </div>

              <!-- Badge de Não Lidas -->
              @if (c.mensagensNaoLidas > 0) {
                <span class="w-4 h-4 rounded-full bg-emerald-500 text-white font-bold text-[9px] flex items-center justify-center shrink-0">
                  {{ c.mensagensNaoLidas }}
                </span>
              }
            </div>
          } @empty {
            <div class="p-8 text-center text-xs text-gov-slate-500">
              Nenhum contato encontrado.
            </div>
          }
        </div>
      </div>

      <!-- PAINEL CENTRAL: Thread de Mensagens -->
      <div class="flex-1 flex flex-col min-w-0 bg-[#0A0E17]">
        <!-- Cabeçalho do Chat Ativo -->
        <div class="h-16 px-6 border-b border-white/10 bg-[#111827] flex items-center justify-between gap-4 shrink-0">
          <div class="flex items-center gap-3 min-w-0">
            <div class="w-9 h-9 rounded-full {{ whatsService.contatoAtivo().avatarCor }} text-white font-bold flex items-center justify-center text-xs shrink-0">
              {{ whatsService.contatoAtivo().nome.charAt(0) }}
            </div>
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <h4 class="text-xs font-bold text-white truncate">
                  {{ whatsService.contatoAtivo().nome }}
                </h4>
                <span class="text-[10px] font-mono text-gov-slate-400 hidden sm:inline">
                  {{ whatsService.contatoAtivo().telefone }}
                </span>
              </div>
              <div class="text-[11px] text-gov-slate-400 truncate">
                {{ whatsService.contatoAtivo().cargo }} • <span class="text-emerald-400">{{ whatsService.contatoAtivo().ultimoVisto }}</span>
              </div>
            </div>
          </div>

          <!-- Ações e Link de Convênio -->
          <div class="flex items-center gap-2 shrink-0">
            @if (whatsService.contatoAtivo().convenioVinculadoId) {
              <a
                [routerLink]="['/convenios', whatsService.contatoAtivo().convenioVinculadoId]"
                class="px-3 py-1.5 rounded-lg bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/30 text-gov-cobalt-300 border border-gov-cobalt-500/30 text-xs font-semibold transition-colors inline-flex items-center gap-1.5"
                title="Abrir o Cockpit deste Convênio"
              >
                <!-- Ícone Cockpit -->
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H20v20H6.5a2.5 2.5 0 0 1-2.5-2.5Z"/>
                </svg>
                <span>Ver Cockpit (#{{ whatsService.contatoAtivo().convenioVinculadoNumero }})</span>
              </a>
            }
          </div>
        </div>

        <!-- Área de Mensagens (Rolável) -->
        <div class="flex-1 overflow-y-auto p-6 space-y-4">
          <!-- Separador de Início da Conversa Criptografada -->
          <div class="text-center">
            <span class="px-3 py-1 rounded-full bg-white/5 border border-white/5 text-[10px] text-gov-slate-400">
              🔒 Conversa integrada com Evolution API & Transferegov Service
            </span>
          </div>

          @for (m of whatsService.mensagensDoChatAtivo(); track m.id) {
            <!-- Mensagem do Contato (Inbound - Lado Esquerdo) -->
            @if (m.remetente === 'CONTATO') {
              <div class="flex flex-col items-start max-w-lg">
                <div class="p-3.5 rounded-2xl rounded-tl-sm bg-[#111827] border border-white/10 text-xs text-gov-slate-200 shadow-sm space-y-2.5">
                  <p class="leading-relaxed">{{ m.texto }}</p>

                  <!-- Card de Anexo (ex: NF ou Boletim de Medição) -->
                  @if (m.anexo) {
                    <div class="p-3 rounded-xl bg-white/5 border border-white/10 space-y-2">
                      <div class="flex items-center gap-2.5">
                        <div class="w-8 h-8 rounded-lg bg-rose-500/15 text-rose-400 border border-rose-500/25 flex items-center justify-center shrink-0">
                          <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                            <polyline points="14 2 14 8 20 8"/>
                          </svg>
                        </div>
                        <div class="min-w-0 flex-1">
                          <div class="font-medium text-white truncate text-xs">{{ m.anexo.nome }}</div>
                          <div class="text-[10px] text-gov-slate-400">{{ m.anexo.tamanho }} • {{ m.anexo.faseConvenio }}</div>
                        </div>
                      </div>

                      @if (m.anexo.scoreOcr) {
                        <div class="flex items-center justify-between pt-1 border-t border-white/5">
                          <div class="flex items-center gap-1.5 text-[10px] text-gov-slate-400">
                            <span>Score OCR:</span>
                            <app-confidence-badge [score]="m.anexo.scoreOcr"></app-confidence-badge>
                          </div>
                          @if (m.anexo.documentoId) {
                            <button
                              type="button"
                              (click)="abrirRevisao(m.anexo.documentoId)"
                              class="px-2.5 py-1 rounded bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-[10px] transition-colors inline-flex items-center gap-1"
                            >
                              <span>Revisar Lado a Lado</span>
                              <svg class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="9 18 15 12 9 6"/>
                              </svg>
                            </button>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>
                <span class="text-[10px] text-gov-slate-500 mt-1 ml-1 font-mono">{{ m.dataHora }}</span>
              </div>
            }

            <!-- Mensagem do Bot IA (Aviso do Sistema no Centro) -->
            @if (m.remetente === 'BOT_IA') {
              <div class="flex justify-center">
                <div class="max-w-md p-3 rounded-xl bg-gov-cobalt-950/40 border border-gov-cobalt-500/30 text-xs text-gov-cobalt-200 shadow-sm flex items-start gap-2.5">
                  <span class="text-sm shrink-0">🤖</span>
                  <div class="flex-1 leading-relaxed">
                    {{ m.texto }}
                  </div>
                </div>
              </div>
            }

            <!-- Mensagem Enviada pelo Consultor (Outbound - Lado Direito) -->
            @if (m.remetente === 'USUARIO') {
              <div class="flex flex-col items-end self-end max-w-lg">
                <div class="p-3.5 rounded-2xl rounded-tr-sm bg-gov-cobalt-600 text-white text-xs shadow-sm leading-relaxed">
                  {{ m.texto }}
                </div>
                <div class="flex items-center gap-1 mt-1 mr-1 text-[10px] text-gov-slate-500 font-mono">
                  <span>{{ m.dataHora }}</span>
                  <svg class="w-3 h-3 text-gov-cobalt-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </div>
              </div>
            }
          }
        </div>

        <!-- Barra Inferior: Macros Rápidas & Input de Mensagem -->
        <div class="p-4 border-t border-white/10 bg-[#111827] space-y-3">
          <!-- Macros de Resposta Rápida do Transferegov -->
          <div class="flex items-center gap-2 overflow-x-auto text-xs pb-1">
            <span class="text-[10px] font-semibold uppercase tracking-wider text-gov-slate-400 shrink-0">
              Disparos Rápidos:
            </span>
            <button
              type="button"
              (click)="whatsService.dispararMacroAlerta('PRAZO_15')"
              class="px-2.5 py-1 rounded-lg bg-rose-500/10 hover:bg-rose-500/20 text-rose-300 border border-rose-500/25 text-[11px] transition-colors shrink-0 flex items-center gap-1"
            >
              <span>⚠️ Cobrar Prazo (18 dias)</span>
            </button>
            <button
              type="button"
              (click)="whatsService.dispararMacroAlerta('SOLICITAR_ART')"
              class="px-2.5 py-1 rounded-lg bg-amber-500/10 hover:bg-amber-500/20 text-amber-300 border border-amber-500/25 text-[11px] transition-colors shrink-0 flex items-center gap-1"
            >
              <span>📋 Solicitar ART & Laudo</span>
            </button>
            <button
              type="button"
              (click)="whatsService.dispararMacroAlerta('CONFIRMAR_OBTV')"
              class="px-2.5 py-1 rounded-lg bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-300 border border-emerald-500/25 text-[11px] transition-colors shrink-0 flex items-center gap-1"
            >
              <span>💰 Confirmar Comando OBTV</span>
            </button>
          </div>

          <!-- Caixa de Entrada de Texto -->
          <div class="flex items-center gap-2">
            <input
              type="text"
              [(ngModel)]="textoNovaMensagem"
              (keyup.enter)="enviar()"
              placeholder="Digite uma mensagem para o fiscal municipal..."
              class="flex-1 bg-[#0A0E17] border border-white/10 rounded-xl px-4 py-2.5 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-gov-cobalt-500 transition-colors"
            />
            <button
              type="button"
              (click)="enviar()"
              class="px-4 py-2.5 rounded-xl bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs transition-colors flex items-center gap-1.5 cursor-pointer shadow-sm"
            >
              <span>Enviar</span>
              <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="22" y1="2" x2="11" y2="13"/>
                <polygon points="22 2 15 22 11 13 2 9 22 2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- PAINEL DIREITO: Dossiê do Contato e Vinculação ao Convênio (280px) -->
      <div class="w-72 border-l border-white/10 bg-[#111827] p-5 hidden xl:flex flex-col justify-between shrink-0">
        <div class="space-y-6">
          <div class="text-center">
            <div class="w-16 h-16 rounded-full {{ whatsService.contatoAtivo().avatarCor }} text-white font-bold text-xl flex items-center justify-center mx-auto shadow-md">
              {{ whatsService.contatoAtivo().nome.charAt(0) }}
            </div>
            <h4 class="text-sm font-bold text-white mt-3">{{ whatsService.contatoAtivo().nome }}</h4>
            <p class="text-xs text-gov-slate-400 mt-0.5">{{ whatsService.contatoAtivo().cargo }}</p>
            <span class="inline-block mt-2 text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 border border-white/10 text-gov-slate-300">
              {{ whatsService.contatoAtivo().municipio }}
            </span>
          </div>

          <!-- Informações de Contato -->
          <div class="p-3.5 rounded-xl bg-white/5 border border-white/5 space-y-2.5 text-xs">
            <div class="flex justify-between">
              <span class="text-gov-slate-400">Telefone:</span>
              <span class="font-mono text-gov-slate-200">{{ whatsService.contatoAtivo().telefone }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gov-slate-400">Canal:</span>
              <span class="text-emerald-400 font-medium">WhatsApp Business</span>
            </div>
            <div class="flex justify-between">
              <span class="text-gov-slate-400">Status:</span>
              <span class="text-gov-slate-200">{{ whatsService.contatoAtivo().ultimoVisto }}</span>
            </div>
          </div>

          <!-- Convênio Vinculado -->
          @if (whatsService.contatoAtivo().convenioVinculadoId) {
            <div class="p-4 rounded-xl bg-gov-cobalt-950/30 border border-gov-cobalt-500/25 space-y-3">
              <div class="flex items-center justify-between">
                <span class="text-[10px] font-semibold uppercase tracking-wider text-gov-cobalt-400">Convênio Vinculado</span>
                <span class="text-[10px] font-mono font-bold text-white">#{{ whatsService.contatoAtivo().convenioVinculadoNumero }}</span>
              </div>
              <p class="text-xs text-gov-slate-200 font-medium leading-snug">
                {{ whatsService.contatoAtivo().convenioVinculadoObjeto }}
              </p>
              <a
                [routerLink]="['/convenios', whatsService.contatoAtivo().convenioVinculadoId]"
                class="w-full py-2 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-semibold text-xs flex items-center justify-center gap-1.5 transition-colors shadow-sm"
              >
                <span>Acessar Cockpit</span>
                <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="9 18 15 12 9 6"/>
                </svg>
              </a>
            </div>
          }
        </div>

        <div class="pt-4 border-t border-white/10 text-center">
          <span class="text-[10px] text-gov-slate-500">
            Conectado ao Evolution API v2.3
          </span>
        </div>
      </div>
    </div>
  `
})
export class WhatsAppHubPageComponent {
  readonly whatsService = inject(WhatsAppService);
  private readonly router = inject(Router);

  public readonly termoBusca = signal<string>('');
  public readonly filtroCategoria = signal<'TODOS' | 'OBRAS' | 'FINANCAS' | 'JURIDICO'>('TODOS');
  public textoNovaMensagem = '';

  readonly contatosFiltrados = computed(() => {
    let lista = this.whatsService.contatos();
    const cat = this.filtroCategoria();
    if (cat !== 'TODOS') {
      lista = lista.filter(c => c.categoria === cat);
    }
    const busca = this.termoBusca().toLowerCase().trim();
    if (busca) {
      lista = lista.filter(c =>
        c.nome.toLowerCase().includes(busca) ||
        c.cargo.toLowerCase().includes(busca) ||
        c.ultimaMensagemTexto.toLowerCase().includes(busca) ||
        (c.convenioVinculadoNumero && c.convenioVinculadoNumero.includes(busca))
      );
    }
    return lista;
  });

  enviar(): void {
    if (!this.textoNovaMensagem.trim()) return;
    this.whatsService.enviarMensagem(this.textoNovaMensagem);
    this.textoNovaMensagem = '';
  }

  abrirRevisao(documentoId: string): void {
    this.router.navigate(['/documentos', documentoId, 'revisar']);
  }
}
