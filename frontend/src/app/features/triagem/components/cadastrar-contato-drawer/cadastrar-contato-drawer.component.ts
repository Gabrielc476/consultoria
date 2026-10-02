import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CadastrarContatoETriarPayload, TriagemItem } from '../../model/triagem.model';
import { TriagemService } from '../../services/triagem.service';
import { ConvenioResponseDto, ConvenioService } from '../../../convenios/services/convenio.service';
import { ToastService } from '../../../../core/ui/toast.service';
import { MunicipioContextService } from '../../../../core/context/municipio-context.service';

@Component({
  selector: 'app-cadastrar-contato-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen) {
      <!-- Backdrop translúcido -->
      <div
        class="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 transition-opacity animate-in fade-in duration-200 select-none font-sans"
        (click)="fechar()"
      ></div>

      <!-- Painel Lateral Deslizante (Slide-over da Direita) -->
      <div class="fixed top-0 right-0 h-full w-[520px] max-w-full bg-[#111827] border-l border-white/10 z-50 flex flex-col shadow-2xl overflow-hidden animate-in slide-in-from-right duration-250 font-sans select-none">
        
        <!-- Topo / Cabeçalho do Drawer -->
        <div class="px-6 py-4 border-b border-white/10 flex items-center justify-between bg-[#151D2F]">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center text-lg shadow-inner">
              👤
            </div>
            <div>
              <h2 class="text-sm font-bold text-white tracking-tight flex items-center gap-2">
                <span>Cadastrar Contato & Triar</span>
                <span class="text-[9px] font-mono px-1.5 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 font-bold uppercase">
                  1:N Convênios
                </span>
              </h2>
              <p class="text-[11px] text-gov-slate-400">
                Associe o fiscal/engenheiro e arquive o documento no Ficheiro Digital em 1 clique
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

        <!-- Conteúdo do Formulário Rolável -->
        <div class="flex-1 overflow-y-auto p-6 space-y-5">

          <!-- Card de Documento em Triagem -->
          @if (item) {
            <div class="p-3.5 rounded-xl bg-[#0A0E17] border border-white/5 space-y-2">
              <div class="flex items-center justify-between text-xs">
                <span class="text-gov-slate-400 font-medium">Documento em Triagem:</span>
                <span class="text-[10px] font-mono px-2 py-0.5 rounded bg-gov-cobalt-500/20 text-gov-cobalt-300 border border-gov-cobalt-500/30 font-bold">
                  {{ item.documentoContentType || 'Anexo' }}
                </span>
              </div>
              <div class="text-xs font-semibold text-white truncate flex items-center gap-2">
                <span class="text-base">📄</span>
                <span class="truncate">{{ item.documentoNomeOriginal || 'Arquivo sem nome' }}</span>
              </div>
              @if (item.conteudoResumo) {
                <p class="text-[11px] text-gov-slate-400 italic line-clamp-2 bg-black/20 p-2 rounded border border-white/5">
                  "{{ item.conteudoResumo }}"
                </p>
              }
            </div>
          }

          <!-- Mensagem de Erro -->
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

          <!-- Campo Telefone (Bloqueado) -->
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gov-slate-300 flex items-center justify-between">
              <span>Telefone WhatsApp (E.164)</span>
              <span class="text-[10px] text-gov-slate-500 font-mono">🔒 Bloqueado para integridade</span>
            </label>
            <input
              type="text"
              [value]="phoneNumber()"
              readonly
              disabled
              class="w-full px-3.5 py-2.5 rounded-lg bg-[#0A0E17]/60 border border-white/10 text-gov-slate-400 text-xs font-mono cursor-not-allowed"
            />
          </div>

          <!-- Campo Nome Completo -->
          <div class="space-y-1.5">
            <label class="text-xs font-semibold text-gov-slate-300 flex items-center justify-between">
              <span>Nome Completo do Contato <span class="text-rose-400">*</span></span>
              @if (item?.pushName) {
                <span class="text-[10px] text-emerald-400 font-medium">Sugerido via WhatsApp: {{ item?.pushName }}</span>
              }
            </label>
            <input
              type="text"
              [ngModel]="nome()"
              (ngModelChange)="nome.set($event)"
              placeholder="Ex: Eng. Carlos Alberto"
              class="w-full px-3.5 py-2.5 rounded-lg bg-[#0A0E17] border border-white/10 text-white placeholder-gov-slate-500 text-xs focus:outline-none focus:border-gov-cobalt-500 transition-colors"
            />
          </div>

          <!-- Grid: Papel / Função + Empresa / Órgão -->
          <div class="grid grid-cols-2 gap-3">
            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gov-slate-300">Papel / Função</label>
              <select
                [ngModel]="papel()"
                (ngModelChange)="papel.set($event)"
                class="w-full px-3 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs focus:outline-none focus:border-gov-cobalt-500 transition-colors"
              >
                <option value="FISCAL_ENGENHEIRO">Fiscal / Engenheiro</option>
                <option value="SECRETARIO_MUNICIPAL">Secretário Municipal</option>
                <option value="REPRESENTANTE_EMPREITEIRA">Repres. Empreiteira</option>
                <option value="AGENTE_CONSULTORIA">Agente Consultoria</option>
                <option value="OUTRO">Outro Representante</option>
              </select>
            </div>

            <div class="space-y-1.5">
              <label class="text-xs font-semibold text-gov-slate-300">Empresa ou Órgão</label>
              <input
                type="text"
                [ngModel]="empresaOuOrgao()"
                (ngModelChange)="empresaOuOrgao.set($event)"
                placeholder="Ex: Sec. de Obras / Construtora"
                class="w-full px-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white placeholder-gov-slate-500 text-xs focus:outline-none focus:border-gov-cobalt-500 transition-colors"
              />
            </div>
          </div>

          <!-- Vínculo N:N com Convênios da Consultoria -->
          <div class="space-y-2 pt-2 border-t border-white/10">
            <div class="flex items-center justify-between">
              <div>
                <label class="text-xs font-bold text-white block">
                  Vincular a Convênios Ativos (Relação 1:N)
                </label>
                <span class="text-[11px] text-gov-slate-400">
                  Marque as obras em que este contato atua e eleja a principal
                </span>
              </div>
              <span class="text-[11px] font-mono text-gov-cobalt-400 font-bold bg-gov-cobalt-500/10 px-2 py-0.5 rounded border border-gov-cobalt-500/20">
                {{ conveniosSelecionados().size }} selecionado(s)
              </span>
            </div>

            <div class="max-h-52 overflow-y-auto bg-[#0A0E17] border border-white/10 rounded-xl p-2.5 space-y-2 divide-y divide-white/5">
              @if (carregandoConvenios()) {
                <div class="p-4 text-center text-xs text-gov-slate-400">
                  Carregando convênios autorizados...
                </div>
              } @else if (conveniosDisponiveis().length === 0) {
                <div class="p-4 text-center text-xs text-gov-slate-400">
                  Nenhum convênio encontrado no seu escopo.
                </div>
              } @else {
                @for (conv of conveniosDisponiveis(); track conv.id) {
                  <div class="pt-2 first:pt-0 flex items-start justify-between gap-3 text-xs">
                    <label class="flex items-start gap-2.5 cursor-pointer text-gov-slate-300 hover:text-white flex-1 min-w-0">
                      <input
                        type="checkbox"
                        [checked]="conveniosSelecionados().has(conv.id)"
                        (change)="alternarConvenio(conv.id)"
                        class="mt-0.5 rounded bg-slate-800 border-slate-700 text-gov-cobalt-500 focus:ring-gov-cobalt-500/30"
                      />
                      <div class="min-w-0">
                        <div class="flex items-center gap-1.5">
                          <span class="font-mono font-bold text-white text-[11px]">SICONV {{ conv.numeroSiconv }}</span>
                          @if (item?.convenioSugeridoId === conv.id) {
                            <span class="text-[9px] px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30 font-bold">
                              ✨ Sugestão IA
                            </span>
                          }
                        </div>
                        <p class="text-[11px] text-gov-slate-400 truncate mt-0.5">{{ conv.objeto }}</p>
                      </div>
                    </label>

                    @if (conveniosSelecionados().has(conv.id)) {
                      <button
                        type="button"
                        (click)="definirPrincipal(conv.id)"
                        [class]="convenioPrincipalId() === conv.id
                          ? 'bg-gov-cobalt-600 text-white font-bold'
                          : 'bg-white/5 text-gov-slate-400 hover:text-white'"
                        class="text-[10px] px-2 py-0.5 rounded border border-white/10 transition-colors shrink-0 cursor-pointer"
                      >
                        {{ convenioPrincipalId() === conv.id ? '★ Principal' : 'Tornar Principal' }}
                      </button>
                    }
                  </div>
                }
              }
            </div>
          </div>

          <!-- Seção de Arquivamento Imediato no GED -->
          <div class="p-4 rounded-xl bg-gov-cobalt-950/20 border border-gov-cobalt-500/30 space-y-3">
            <label class="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                [ngModel]="arquivarDocumento()"
                (ngModelChange)="arquivarDocumento.set($event)"
                class="rounded bg-slate-800 border-slate-700 text-gov-cobalt-500 focus:ring-gov-cobalt-500/30 w-4 h-4"
              />
              <div class="text-xs">
                <span class="font-bold text-white block">Arquivar documento imediatamente no Ficheiro Digital</span>
                <span class="text-[11px] text-gov-slate-400 block">
                  Move do fluxo temporário para a pasta da fase oficial correspondente
                </span>
              </div>
            </label>

            @if (arquivarDocumento()) {
              <div class="space-y-1.5 pt-2 border-t border-white/10">
                <label class="text-[11px] font-semibold text-gov-slate-300 block">
                  Fase do Ciclo de Vida Oficial (00 a 09)
                </label>
                <select
                  [ngModel]="faseCicloVida()"
                  (ngModelChange)="faseCicloVida.set($event)"
                  class="w-full px-3 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-white text-xs focus:outline-none focus:border-gov-cobalt-500 transition-colors"
                >
                  <option value="00_HABILITACAO_E_PROPOSTA">Fase 00 - Habilitação e Proposta</option>
                  <option value="01_CELEBRACAO_E_FORMALIZACAO">Fase 01 - Celebração e Formalização</option>
                  <option value="02_CLAUSULA_SUSPENSIVA_E_ENGENHARIA">Fase 02 - Cláusula Suspensiva e Engenharia</option>
                  <option value="03_LICITACAO_E_CONTRATACAO">Fase 03 - Licitação e Contratação</option>
                  <option value="04_EXECUCAO_FISICA_E_MEDICOES">Fase 04 - Execução Física e Medições (Boletins)</option>
                  <option value="05_EXECUCAO_FINANCEIRA_E_PAGAMENTOS">Fase 05 - Execução Financeira (Notas Fiscais)</option>
                  <option value="06_ALTERACOES_CONTRATUAIS_E_ADITIVOS">Fase 06 - Alterações Contratuais e Aditivos</option>
                  <option value="07_PRESTACAO_CONTAS_E_TERMO_RECEBIMENTO">Fase 07 - Prestação de Contas</option>
                  <option value="08_ENCERRAMENTO_FINANCEIRO_E_SALDOS">Fase 08 - Encerramento Financeiro</option>
                  <option value="09_NOTIFICACOES_E_PASSIVO_JURIDICO">Fase 09 - Notificações e Passivo Jurídico</option>
                </select>
              </div>
            }
          </div>

        </div>

        <!-- Rodapé com Botões de Ação -->
        <div class="px-6 py-4 border-t border-white/10 bg-[#151D2F] flex items-center justify-end gap-3">
          <button
            type="button"
            (click)="fechar()"
            [disabled]="salvando()"
            class="px-4 py-2.5 rounded-lg border border-white/10 text-gov-slate-300 hover:text-white hover:bg-white/5 text-xs font-semibold transition-colors cursor-pointer disabled:opacity-50"
          >
            Cancelar
          </button>

          <button
            type="button"
            (click)="salvar()"
            [disabled]="salvando() || !podeSalvar()"
            class="px-5 py-2.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-all shadow-lg shadow-emerald-900/30 flex items-center gap-2 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
          >
            @if (salvando()) {
              <div class="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
              <span>Salvando...</span>
            } @else {
              <span>💾</span>
              <span>Salvar Contato e Arquivar (1 Clique)</span>
            }
          </button>
        </div>

      </div>
    }
  `
})
export class CadastrarContatoDrawerComponent implements OnInit, OnChanges {
  @Input() item: TriagemItem | null = null;
  @Input() isOpen = false;

  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<TriagemItem>();

  private readonly triagemService = inject(TriagemService);
  private readonly convenioService = inject(ConvenioService);
  private readonly municipioCtx = inject(MunicipioContextService);
  private readonly toast = inject(ToastService);

  readonly nome = signal<string>('');
  readonly phoneNumber = signal<string>('');
  readonly papel = signal<string>('FISCAL_ENGENHEIRO');
  readonly empresaOuOrgao = signal<string>('');
  readonly conveniosDisponiveis = signal<ConvenioResponseDto[]>([]);
  readonly conveniosSelecionados = signal<Set<string>>(new Set());
  readonly convenioPrincipalId = signal<string | null>(null);
  readonly faseCicloVida = signal<string>('04_EXECUCAO_FISICA_E_MEDICOES');
  readonly arquivarDocumento = signal<boolean>(true);
  readonly salvando = signal<boolean>(false);
  readonly carregandoConvenios = signal<boolean>(false);
  readonly erro = signal<string | null>(null);

  readonly podeSalvar = computed(() => {
    return this.nome().trim().length >= 2 &&
           this.conveniosSelecionados().size > 0 &&
           (!this.arquivarDocumento() || this.convenioPrincipalId() != null);
  });

  ngOnInit(): void {
    this.carregarConvenios();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['item'] && this.item) {
      this.inicializarDadosDoItem(this.item);
    }
    if (changes['isOpen'] && this.isOpen) {
      this.carregarConvenios();
      if (this.item) {
        this.inicializarDadosDoItem(this.item);
      }
    }
  }

  private inicializarDadosDoItem(item: TriagemItem): void {
    this.phoneNumber.set(item.phoneNumber || '');
    this.nome.set(item.senderName || item.pushName || '');
    this.faseCicloVida.set(item.faseSugerida || '04_EXECUCAO_FISICA_E_MEDICOES');
    this.arquivarDocumento.set(true);
    this.erro.set(null);

    const selecionados = new Set<string>();
    if (item.convenioSugeridoId) {
      selecionados.add(item.convenioSugeridoId);
      this.convenioPrincipalId.set(item.convenioSugeridoId);
    } else {
      this.convenioPrincipalId.set(null);
    }
    this.conveniosSelecionados.set(selecionados);
  }

  carregarConvenios(): void {
    this.carregandoConvenios.set(true);
    this.convenioService.listarConvenios().subscribe({
      next: (convs) => {
        this.conveniosDisponiveis.set(convs);
        this.carregandoConvenios.set(false);
      },
      error: () => {
        this.carregandoConvenios.set(false);
      }
    });
  }

  alternarConvenio(convId: string): void {
    const atual = new Set(this.conveniosSelecionados());
    if (atual.has(convId)) {
      atual.delete(convId);
      if (this.convenioPrincipalId() === convId) {
        const proximo = atual.values().next().value;
        this.convenioPrincipalId.set(proximo || null);
      }
    } else {
      atual.add(convId);
      if (!this.convenioPrincipalId()) {
        this.convenioPrincipalId.set(convId);
      }
    }
    this.conveniosSelecionados.set(atual);
  }

  definirPrincipal(convId: string): void {
    this.convenioPrincipalId.set(convId);
  }

  fechar(): void {
    this.closed.emit();
  }

  salvar(): void {
    if (!this.item) return;

    if (this.nome().trim().length < 2) {
      this.erro.set('Nome é obrigatório (mínimo 2 caracteres).');
      return;
    }

    if (this.conveniosSelecionados().size === 0) {
      this.erro.set('Selecione pelo menos um convênio para vincular ao contato.');
      return;
    }

    this.salvando.set(true);
    this.erro.set(null);

    const payload: CadastrarContatoETriarPayload = {
      nome: this.nome().trim(),
      phoneNumber: this.phoneNumber().trim(),
      papel: this.papel(),
      empresaOuOrgao: this.empresaOuOrgao().trim() || undefined,
      conveniosIds: Array.from(this.conveniosSelecionados()),
      convenioPrincipalId: this.convenioPrincipalId() || undefined,
      faseCicloVida: this.arquivarDocumento() ? this.faseCicloVida() : undefined,
      arquivarDocumento: this.arquivarDocumento()
    };

    this.triagemService.cadastrarContatoETriar(this.item.id, payload).subscribe({
      next: (itemResolvido) => {
        this.salvando.set(false);
        this.saved.emit(itemResolvido);
        this.fechar();
      },
      error: (err) => {
        this.salvando.set(false);
        const msg = err.error?.detail || err.error?.message || 'Falha ao processar cadastro e triagem';
        this.erro.set(msg);
      }
    });
  }
}
