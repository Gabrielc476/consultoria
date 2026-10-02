import { Component, EventEmitter, Input, OnInit, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WhatsAppService } from '../../services/whatsapp.service';
import { ChatResumo, ContatoResponse } from '../../model/whatsapp.model';
import { ConvenioResponseDto, ConvenioService } from '../../../convenios/services/convenio.service';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-cadastrar-contato-whatsapp-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen) {
      <!-- Backdrop translúcido -->
      <div
        class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 transition-opacity animate-in fade-in duration-200 select-none font-sans"
        (click)="fechar()"
      ></div>

      <!-- Modal Centralizado -->
      <div class="fixed inset-0 z-50 flex items-center justify-center p-4 select-none font-sans">
        <div
          class="bg-[#111827] border border-white/10 rounded-2xl w-[640px] max-w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden animate-in zoom-in-95 duration-200"
          (click)="$event.stopPropagation()"
        >
          <!-- Cabeçalho do Modal -->
          <div class="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-[#151D2F]">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center text-lg shadow-inner">
                📱
              </div>
              <div>
                <h2 class="text-sm font-bold text-white tracking-tight flex items-center gap-2">
                  <span>Vincular Contato do WhatsApp</span>
                  <span class="text-[9px] font-mono px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-bold uppercase">
                    1:N Convênios & LGPD
                  </span>
                </h2>
                <p class="text-[11px] text-gov-slate-400">
                  Cadastre o número para liberar a ingestão, importar as últimas 10 mensagens e ativar a IA
                </p>
              </div>
            </div>

            <button
              type="button"
              (click)="fechar()"
              class="text-gov-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-white/5 transition-colors cursor-pointer"
            >
              ✕
            </button>
          </div>

          <!-- Abas de Navegação: Conversas Recentes vs Manual -->
          <div class="flex border-b border-white/10 bg-[#0D131F] px-6 text-xs">
            <button
              type="button"
              (click)="abaAtiva.set('RECENTES')"
              class="py-3 px-4 font-semibold border-b-2 transition-colors cursor-pointer flex items-center gap-2"
              [ngClass]="abaAtiva() === 'RECENTES' ? 'border-emerald-500 text-emerald-400' : 'border-transparent text-gov-slate-400 hover:text-white'"
            >
              <span>Conversas Recentes na Evolution API</span>
              @if (whatsService.chatsRecentes().length > 0) {
                <span class="px-1.5 py-0.2 rounded-full bg-emerald-500/20 text-emerald-400 text-[10px]">
                  {{ whatsService.chatsRecentes().length }}
                </span>
              }
            </button>

            <button
              type="button"
              (click)="abaAtiva.set('MANUAL')"
              class="py-3 px-4 font-semibold border-b-2 transition-colors cursor-pointer flex items-center gap-2"
              [ngClass]="abaAtiva() === 'MANUAL' ? 'border-emerald-500 text-emerald-400' : 'border-transparent text-gov-slate-400 hover:text-white'"
            >
              <span>Formulário de Cadastro</span>
            </button>
          </div>

          <!-- Conteúdo Rolável -->
          <div class="flex-1 overflow-y-auto p-6 space-y-5">
            
            <!-- ABA 1: CONVERSAS RECENTES -->
            @if (abaAtiva() === 'RECENTES') {
              <div class="space-y-3">
                <div class="flex items-center justify-between text-xs text-gov-slate-400">
                  <span>Selecione uma conversa detectada na instância do WhatsApp:</span>
                  <button
                    type="button"
                    (click)="recarregarChatsRecentes()"
                    class="text-[11px] text-emerald-400 hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <span>🔄 Atualizar lista</span>
                  </button>
                </div>

                @if (whatsService.carregandoChatsRecentes()) {
                  <div class="py-12 text-center text-xs text-gov-slate-400 space-y-2">
                    <div class="w-6 h-6 border-2 border-emerald-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
                    <p>Consultando conversas na Evolution API...</p>
                  </div>
                } @else {
                  <div class="space-y-2 max-h-80 overflow-y-auto pr-1">
                    @for (chat of whatsService.chatsRecentes(); track chat.phone) {
                      <div
                        (click)="selecionarChatRecente(chat)"
                        class="p-3 rounded-xl bg-white/5 hover:bg-white/10 border border-white/5 transition-all cursor-pointer flex items-center justify-between gap-3 group"
                        [class.opacity-50]="chat.isCadastrado"
                      >
                        <div class="flex items-center gap-3 min-w-0">
                          <div class="w-10 h-10 rounded-full bg-emerald-600/30 text-emerald-300 font-bold flex items-center justify-center shrink-0 text-xs border border-emerald-500/20">
                            {{ chat.name ? chat.name.charAt(0) : '?' }}
                          </div>
                          <div class="min-w-0">
                            <div class="flex items-center gap-2">
                              <span class="text-xs font-semibold text-white truncate">{{ chat.name }}</span>
                              <span class="text-[10px] font-mono text-gov-slate-400">{{ formatarTelefoneExibicao(chat.phone) }}</span>
                            </div>
                            <p class="text-[11px] text-gov-slate-400 truncate mt-0.5">
                              {{ chat.lastMessage || 'Conversa sem texto prévio' }}
                            </p>
                          </div>
                        </div>

                        <div class="shrink-0 flex items-center gap-2">
                          @if (chat.isCadastrado) {
                            <span class="text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                              ✓ Já Cadastrado
                            </span>
                          } @else {
                            <button
                              type="button"
                              class="text-[11px] font-bold px-3 py-1.5 rounded-lg bg-emerald-600 group-hover:bg-emerald-500 text-white transition-colors"
                            >
                              Selecionar
                            </button>
                          }
                        </div>
                      </div>
                    } @empty {
                      <div class="py-8 text-center text-xs text-gov-slate-400 bg-white/[0.02] rounded-xl border border-white/5 space-y-1">
                        <p class="font-medium text-gov-slate-300">Nenhum chat recente encontrado</p>
                        <p class="text-[11px] text-gov-slate-500">
                          Envie uma mensagem para a instância ou utilize o formulário de cadastro manual.
                        </p>
                      </div>
                    }
                  </div>
                }
              </div>
            }

            <!-- ABA 2: FORMULÁRIO DE CADASTRO -->
            @if (abaAtiva() === 'MANUAL') {
              <form class="space-y-4" (submit)="$event.preventDefault(); salvar()">
                <!-- Alerta LGPD e Privacidade -->
                <div class="p-3.5 rounded-xl bg-emerald-500/10 border border-emerald-500/25 text-xs text-emerald-300 flex items-start gap-2.5">
                  <span class="text-base shrink-0">🛡️</span>
                  <div class="leading-relaxed">
                    <span class="font-bold">Privacidade do Agente & LGPD:</span>
                    Apenas contatos cadastrados têm suas mensagens armazenadas e processadas. Ao cadastrar, as <strong>últimas 10 mensagens</strong> serão importadas retroativamente e quaisquer anexos (áudios/documentos) serão direcionados para a IA de Contexto.
                  </div>
                </div>

                @if (erro()) {
                  <div class="p-3 rounded-lg bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs flex items-center gap-2">
                    <span>⚠️ {{ erro() }}</span>
                  </div>
                }

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <!-- Nome Completo -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-semibold text-gov-slate-300">
                      Nome Completo *
                    </label>
                    <input
                      type="text"
                      [(ngModel)]="formNome"
                      name="nome"
                      placeholder="Ex: Eng. Roberto Farias"
                      class="w-full px-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    />
                  </div>

                  <!-- Telefone (E.164) -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-semibold text-gov-slate-300">
                      Telefone WhatsApp (com DDI e DDD) *
                    </label>
                    <input
                      type="text"
                      [(ngModel)]="formTelefone"
                      name="telefone"
                      placeholder="Ex: 5583999998888"
                      class="w-full px-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs font-mono placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    />
                  </div>
                </div>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <!-- Cargo / Papel -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-semibold text-gov-slate-300">
                      Papel / Função *
                    </label>
                    <select
                      [(ngModel)]="formPapel"
                      name="papel"
                      class="w-full px-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    >
                      <option value="Fiscal de Obras">Fiscal de Obras</option>
                      <option value="Secretário de Obras">Secretário de Obras</option>
                      <option value="Secretário de Finanças">Secretário de Finanças</option>
                      <option value="Engenheiro Responsável">Engenheiro Responsável</option>
                      <option value="Procurador Jurídico">Procurador Jurídico</option>
                      <option value="Prefeito Municipal">Prefeito Municipal</option>
                      <option value="Diretor de Construtora">Diretor de Construtora</option>
                      <option value="Outro">Outro</option>
                    </select>
                  </div>

                  <!-- Órgão ou Empresa -->
                  <div class="space-y-1.5">
                    <label class="text-xs font-semibold text-gov-slate-300">
                      Órgão ou Empresa
                    </label>
                    <input
                      type="text"
                      [(ngModel)]="formEmpresa"
                      name="empresa"
                      placeholder="Ex: SMOP / Prefeitura de Patos"
                      class="w-full px-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs placeholder-gov-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
                    />
                  </div>
                </div>

                <!-- Vínculo N:N com Convênios -->
                <div class="space-y-2 pt-2 border-t border-white/10">
                  <div class="flex items-center justify-between">
                    <label class="text-xs font-semibold text-gov-slate-300 flex items-center gap-1.5">
                      <span>Convênios Vinculados (Relação 1:N)</span>
                      <span class="text-[10px] text-gov-slate-400 font-normal">
                        ({{ conveniosSelecionados().length }} selecionado(s))
                      </span>
                    </label>
                    <span class="text-[10px] text-gov-slate-400 italic">
                      Marque o convênio principal para a IA de contexto
                    </span>
                  </div>

                  @if (carregandoConvenios()) {
                    <div class="py-4 text-center text-xs text-gov-slate-400">
                      Carregando convênios...
                    </div>
                  } @else {
                    <div class="max-h-48 overflow-y-auto space-y-1.5 pr-1 divide-y divide-white/5">
                      @for (conv of listaConvenios(); track conv.id) {
                        <div class="pt-1.5 first:pt-0 flex items-start gap-2.5 p-2 rounded-lg hover:bg-white/[0.02]">
                          <!-- Checkbox -->
                          <input
                            type="checkbox"
                            [checked]="isConvenioSelecionado(conv.id)"
                            (change)="toggleConvenio(conv.id)"
                            class="mt-1 rounded border-white/20 text-emerald-600 focus:ring-emerald-500"
                          />

                          <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2">
                              <span class="text-xs font-semibold text-white">
                                Conv. #{{ conv.numeroSiconv }}
                              </span>
                              @if (convenioPrincipalId() === conv.id) {
                                <span class="text-[9px] font-bold px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                                  ★ Principal
                                </span>
                              }
                            </div>
                            <p class="text-[11px] text-gov-slate-400 truncate mt-0.5">
                              {{ conv.objeto }}
                            </p>
                          </div>

                          @if (isConvenioSelecionado(conv.id)) {
                            <button
                              type="button"
                              (click)="definirPrincipal(conv.id)"
                              class="text-[10px] text-gov-slate-400 hover:text-amber-400 font-medium shrink-0 pt-0.5 cursor-pointer"
                              [title]="convenioPrincipalId() === conv.id ? 'Convênio Principal' : 'Tornar este convênio o Principal'"
                            >
                              {{ convenioPrincipalId() === conv.id ? '★ Principal' : '☆ Tornar Principal' }}
                            </button>
                          }
                        </div>
                      } @empty {
                        <div class="p-3 text-center text-xs text-gov-slate-500">
                          Nenhum convênio cadastrado na consultoria.
                        </div>
                      }
                    </div>
                  }
                </div>
              </form>
            }
          </div>

          <!-- Rodapé de Ações -->
          <div class="px-6 py-4 border-t border-white/10 bg-[#151D2F] flex items-center justify-between gap-3">
            <button
              type="button"
              (click)="fechar()"
              class="px-4 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-gov-slate-300 text-xs font-semibold transition-colors cursor-pointer"
            >
              Cancelar
            </button>

            @if (abaAtiva() === 'MANUAL') {
              <button
                type="button"
                (click)="salvar()"
                [disabled]="salvando()"
                class="px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white font-bold text-xs shadow-md transition-colors flex items-center gap-2 cursor-pointer"
              >
                @if (salvando()) {
                  <span class="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span>Vinculando & Importando...</span>
                } @else {
                  <span>✓ Vincular Contato & Importar Histórico</span>
                }
              </button>
            } @else {
              <button
                type="button"
                (click)="abaAtiva.set('MANUAL')"
                class="px-5 py-2.5 rounded-xl bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white font-bold text-xs shadow-md transition-colors cursor-pointer"
              >
                Ir para Formulário Manual →
              </button>
            }
          </div>
        </div>
      </div>
    }
  `
})
export class CadastrarContatoWhatsappModalComponent implements OnInit {
  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();
  @Output() contatoCadastrado = new EventEmitter<ContatoResponse>();

  readonly whatsService = inject(WhatsAppService);
  private readonly convenioService = inject(ConvenioService);
  private readonly toast = inject(ToastService);

  readonly abaAtiva = signal<'RECENTES' | 'MANUAL'>('RECENTES');
  readonly listaConvenios = signal<ConvenioResponseDto[]>([]);
  readonly carregandoConvenios = signal<boolean>(false);
  readonly conveniosSelecionados = signal<string[]>([]);
  readonly convenioPrincipalId = signal<string | null>(null);
  readonly salvando = signal<boolean>(false);
  readonly erro = signal<string | null>(null);

  formNome = '';
  formTelefone = '';
  formPapel = 'Fiscal de Obras';
  formEmpresa = '';

  ngOnInit(): void {
    this.carregarConvenios();
    this.recarregarChatsRecentes();
  }

  carregarConvenios(): void {
    this.carregandoConvenios.set(true);
    this.convenioService.listarConvenios().subscribe({
      next: (lista) => {
        this.listaConvenios.set(lista || []);
        this.carregandoConvenios.set(false);
        if (lista && lista.length > 0 && this.conveniosSelecionados().length === 0) {
          // Pré-seleciona o primeiro por conveniência
          this.conveniosSelecionados.set([lista[0].id]);
          this.convenioPrincipalId.set(lista[0].id);
        }
      },
      error: () => this.carregandoConvenios.set(false)
    });
  }

  recarregarChatsRecentes(): void {
    this.whatsService.carregarChatsRecentesEvolution().subscribe();
  }

  selecionarChatRecente(chat: ChatResumo): void {
    this.formTelefone = chat.phone;
    this.formNome = (chat.name && chat.name !== chat.phone && !chat.name.startsWith('+') && chat.name.toLowerCase() !== 'você' && chat.name.toLowerCase() !== 'voce') ? chat.name : '';
    this.abaAtiva.set('MANUAL');
  }

  toggleConvenio(convId: string): void {
    const selecionados = [...this.conveniosSelecionados()];
    const index = selecionados.indexOf(convId);
    if (index >= 0) {
      selecionados.splice(index, 1);
      if (this.convenioPrincipalId() === convId) {
        this.convenioPrincipalId.set(selecionados.length > 0 ? selecionados[0] : null);
      }
    } else {
      selecionados.push(convId);
      if (!this.convenioPrincipalId()) {
        this.convenioPrincipalId.set(convId);
      }
    }
    this.conveniosSelecionados.set(selecionados);
  }

  definirPrincipal(convId: string): void {
    if (!this.isConvenioSelecionado(convId)) {
      this.toggleConvenio(convId);
    }
    this.convenioPrincipalId.set(convId);
  }

  isConvenioSelecionado(convId: string): boolean {
    return this.conveniosSelecionados().includes(convId);
  }

  formatarTelefoneExibicao(telefone?: string): string {
    if (!telefone) return '';
    const limpo = telefone.replace(/\D/g, '');
    if (limpo.length === 13 && limpo.startsWith('55')) {
      return `+55 (${limpo.slice(2, 4)}) ${limpo.slice(4, 9)}-${limpo.slice(9)}`;
    }
    return `+${limpo}`;
  }

  fechar(): void {
    this.erro.set(null);
    this.closed.emit();
  }

  salvar(): void {
    this.erro.set(null);

    const nome = this.formNome.trim();
    const telefone = this.formTelefone.replace(/\D/g, '');

    if (!nome) {
      this.erro.set('O nome do contato é obrigatório.');
      return;
    }
    if (!telefone || telefone.length < 10) {
      this.erro.set('Informe um número de telefone WhatsApp válido com DDI e DDD.');
      return;
    }

    this.salvando.set(true);

    const payload = {
      nome,
      phoneNumber: telefone,
      papel: this.formPapel,
      empresaOuOrgao: this.formEmpresa.trim() || undefined,
      conveniosIds: this.conveniosSelecionados(),
      convenioPrincipalId: this.convenioPrincipalId() || undefined
    };

    this.whatsService.cadastrarContatoEImportar(payload).subscribe({
      next: (resp) => {
        this.salvando.set(false);
        this.toast.sucesso(
          'Contato & Mensagens Vinculados',
          `Contato "${resp.nome}" vinculado com sucesso! As últimas 10 mensagens foram importadas da Evolution API.`
        );
        this.contatoCadastrado.emit(resp);
        this.fechar();
      },
      error: (err) => {
        this.salvando.set(false);
        const msg = err.error?.detail || err.error?.message || 'Falha ao vincular contato.';
        this.erro.set(msg);
      }
    });
  }
}
