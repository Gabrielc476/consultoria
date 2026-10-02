import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { TriagemService } from '../../services/triagem.service';
import { TriagemItem } from '../../model/triagem.model';
import { CadastrarContatoDrawerComponent } from '../../components/cadastrar-contato-drawer/cadastrar-contato-drawer.component';
import { ToastService } from '../../../../core/ui/toast.service';

@Component({
  selector: 'app-triagem-inbox-page',
  standalone: true,
  imports: [CommonModule, FormsModule, CadastrarContatoDrawerComponent],
  template: `
    <div class="p-8 space-y-6 max-w-7xl mx-auto select-none font-sans">
      
      <!-- Cabeçalho da Página -->
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-white/10">
        <div>
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-xl bg-gov-cobalt-600/20 text-gov-cobalt-400 border border-gov-cobalt-500/30 flex items-center justify-center text-xl shadow-inner">
              📥
            </div>
            <div>
              <div class="flex items-center gap-2.5">
                <h1 class="text-xl font-bold text-white tracking-tight">Caixa de Triagem Omnicanal</h1>
                <span class="text-xs font-mono px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 font-bold">
                  {{ triagemService.totalPendentes() }} pendente(s)
                </span>
              </div>
              <p class="text-xs text-gov-slate-400 mt-0.5">
                Documentos e áudios de contatos vinculados aguardando confirmação de destino entre múltiplos convênios
              </p>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3">
          <button
            type="button"
            (click)="recarregar()"
            [disabled]="triagemService.carregando()"
            class="px-3.5 py-2 rounded-lg bg-white/5 hover:bg-white/10 text-gov-slate-300 hover:text-white border border-white/10 text-xs font-semibold flex items-center gap-2 transition-all cursor-pointer disabled:opacity-50"
          >
            <span [class.animate-spin]="triagemService.carregando()">🔄</span>
            <span>Atualizar</span>
          </button>
        </div>
      </div>

      <!-- Barra de Filtros e Resumo Rápido -->
      <div class="flex flex-col sm:flex-row items-center justify-between gap-4">
        <!-- Input de Busca -->
        <div class="relative w-full sm:w-80">
          <input
            type="text"
            [ngModel]="termoBusca()"
            (ngModelChange)="termoBusca.set($event)"
            placeholder="Buscar por telefone, nome ou convênio..."
            class="w-full pl-9 pr-3.5 py-2 rounded-lg bg-[#0A0E17] border border-white/10 text-xs text-white placeholder-gov-slate-500 focus:outline-none focus:border-gov-cobalt-500 transition-colors"
          />
          <span class="absolute left-3 top-2.5 text-gov-slate-500 text-xs">🔍</span>
        </div>

        <!-- Badges de Status -->
        <div class="flex items-center gap-3 text-xs w-full sm:w-auto justify-end">
          <div class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#0A0E17] border border-white/5">
            <span class="w-2 h-2 rounded-full bg-amber-500"></span>
            <span class="text-gov-slate-400">Ambiguidades de Convênio:</span>
            <span class="font-bold text-white font-mono">{{ totalAmbiguidades() }}</span>
          </div>
        </div>
      </div>

      <!-- Estado de Carregamento -->
      @if (triagemService.carregando() && triagemService.itensPendentes().length === 0) {
        <div class="p-16 text-center space-y-3 bg-[#0A0E17] rounded-2xl border border-white/5">
          <div class="w-8 h-8 border-3 border-gov-cobalt-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
          <p class="text-xs text-gov-slate-400">Consultando itens pendentes na Caixa de Triagem...</p>
        </div>
      }

      <!-- Empty State quando não há pendências -->
      @else if (itensFiltrados().length === 0) {
        <div class="p-16 text-center space-y-4 bg-[#0A0E17] rounded-2xl border border-white/5 max-w-xl mx-auto my-8">
          <div class="w-16 h-16 rounded-2xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center justify-center text-3xl mx-auto shadow-inner">
            ✨
          </div>
          <div class="space-y-1">
            <h3 class="text-base font-bold text-white">Tudo Limpo na Caixa de Triagem!</h3>
            <p class="text-xs text-gov-slate-400">
              Nenhum documento pendente de resolução de convênio no momento. Apenas arquivos de contatos vinculados transitam pelo sistema.
            </p>
          </div>
        </div>
      }

      <!-- Grid de Itens da Triagem -->
      @else {
        <div class="space-y-4">
          @for (item of itensFiltrados(); track item.id) {
            <div class="p-5 rounded-2xl bg-[#111827] border border-white/10 hover:border-white/20 transition-all shadow-xl space-y-4">
              
              <!-- Cabeçalho do Card: Remetente + Tags -->
              <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-white/5">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-sm font-bold text-white">
                    📱
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="text-xs font-bold text-white tracking-tight">
                        {{ item.senderName || item.pushName || 'Remetente Desconhecido' }}
                      </span>
                      <span class="text-[11px] font-mono text-gov-slate-400 bg-white/5 px-2 py-0.5 rounded border border-white/5">
                        {{ formatarTelefone(item.phoneNumber) }}
                      </span>
                    </div>
                    <span class="text-[10px] text-gov-slate-500 font-mono">
                      Recebido em {{ formatarData(item.createdAt) }}
                    </span>
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  @if (item.remetenteNovo) {
                    <span class="text-[10px] px-2.5 py-1 rounded-full font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center gap-1">
                      <span>⚠️</span>
                      <span>Remetente Novo Não Cadastrado</span>
                    </span>
                  } @else {
                    <span class="text-[10px] px-2.5 py-1 rounded-full font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center gap-1">
                      <span>👤</span>
                      <span>Contato Conhecido</span>
                    </span>
                  }

                  @if (item.motivoAmbiguidade && !item.remetenteNovo) {
                    <span class="text-[10px] px-2.5 py-1 rounded-full font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30 flex items-center gap-1">
                      <span>⚡</span>
                      <span>Ambiguidade de Convênio</span>
                    </span>
                  }
                </div>
              </div>

              <!-- Banner de Alerta de Ambiguidade -->
              @if (item.motivoAmbiguidade) {
                <div class="p-3 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-200 text-xs flex items-start gap-2.5">
                  <span class="text-base shrink-0">⚠️</span>
                  <div class="space-y-0.5">
                    <span class="font-bold block">Motivo da Triagem Necessária:</span>
                    <span class="text-amber-300/90 text-[11px] block">{{ item.motivoAmbiguidade }}</span>
                  </div>
                </div>
              }

              <!-- Corpo do Card: Documento Anexo + Transcrição / Resumo + Sugestão da IA -->
              <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 text-xs">
                
                <!-- Coluna 1: Documento Anexo -->
                <div class="p-3.5 rounded-xl bg-[#0A0E17] border border-white/5 space-y-2 flex flex-col justify-between">
                  <div>
                    <div class="flex items-center justify-between">
                      <span class="text-gov-slate-400 font-semibold text-[11px] uppercase tracking-wider">Documento Anexo</span>
                      <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-white/5 text-gov-slate-300 border border-white/5">
                        {{ formatarTamanho(item.documentoTamanhoBytes) }}
                      </span>
                    </div>
                    <div class="flex items-center gap-2 text-white font-medium mt-1">
                      <span class="text-base">{{ isImageItem(item) ? '🖼️' : '📄' }}</span>
                      <span class="truncate text-xs" [title]="item.documentoNomeOriginal || 'Arquivo'">
                        {{ item.documentoNomeOriginal || 'Documento sem nome' }}
                      </span>
                    </div>
                    <span class="text-[10px] font-mono text-gov-slate-500 block truncate mt-0.5">
                      {{ item.documentoContentType || 'application/octet-stream' }}
                    </span>
                  </div>

                  @if (item.documentoId) {
                    <button
                      type="button"
                      (click)="abrirPreview(item)"
                      class="w-full py-1.5 px-2 rounded-lg bg-gov-cobalt-600/20 hover:bg-gov-cobalt-600/35 text-gov-cobalt-300 hover:text-white border border-gov-cobalt-500/30 text-[11px] font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer mt-2"
                    >
                      <span>👁️</span>
                      <span>Visualizar {{ isImageItem(item) ? 'Foto/Imagem' : 'Documento' }}</span>
                    </button>
                  }
                </div>

                <!-- Coluna 2: Trecho da Conversa / Áudio Transcrito -->
                <div class="p-3.5 rounded-xl bg-[#0A0E17] border border-white/5 space-y-2">
                  <div class="flex items-center justify-between">
                    <span class="text-gov-slate-400 font-semibold text-[11px] uppercase tracking-wider">Contexto da Conversa</span>
                    <span class="text-[10px] text-gov-slate-500 font-mono">Whisper + OCR</span>
                  </div>
                  @if (item.conteudoResumo) {
                    <p class="text-[11px] text-gov-slate-300 italic line-clamp-3 bg-black/20 p-2 rounded-lg border border-white/5">
                      "{{ item.conteudoResumo }}"
                    </p>
                  } @else {
                    <p class="text-[11px] text-gov-slate-500 italic">
                      Nenhum texto adicional acompanhou este anexo.
                    </p>
                  }
                </div>

                <!-- Coluna 3: Classificação Sugerida pela IA -->
                <div class="p-3.5 rounded-xl bg-[#0A0E17] border border-white/5 space-y-2.5">
                  <div class="flex items-center justify-between">
                    <span class="text-gov-slate-400 font-semibold text-[11px] uppercase tracking-wider">Sugestão da IA</span>
                    <div class="flex items-center gap-1.5">
                      <span [class]="obterClasseBadgeConfianca(item.confidenceScore)" class="text-[10px] font-mono font-bold px-2 py-0.5 rounded-full border">
                        {{ (item.confidenceScore * 100).toFixed(0) }}% Confiança
                      </span>
                    </div>
                  </div>

                  <div>
                    <span class="text-[10px] text-gov-slate-500 block uppercase">Convênio Sugerido:</span>
                    @if (item.convenioSugeridoNumeroSiconv) {
                      <div class="flex items-center gap-1.5 text-white font-bold text-xs truncate">
                        <span class="font-mono text-gov-cobalt-400">SICONV {{ item.convenioSugeridoNumeroSiconv }}</span>
                      </div>
                      <span class="text-[11px] text-gov-slate-400 truncate block" [title]="item.convenioSugeridoObjeto || ''">
                        {{ item.convenioSugeridoObjeto || 'Objeto não especificado' }}
                      </span>
                    } @else {
                      <span class="text-[11px] text-rose-300 font-medium italic block">
                        Não foi possível inferir convênio único com certeza.
                      </span>
                    }
                  </div>

                  <div class="pt-1 border-t border-white/5">
                    <span class="text-[10px] text-gov-slate-500 block uppercase">Fase Destino:</span>
                    <span class="text-[11px] font-mono text-emerald-400 font-semibold block truncate">
                      {{ formatarFase(item.faseSugerida) }}
                    </span>
                  </div>
                </div>

              </div>

              <!-- Rodapé de Ações do Card -->
              <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-white/5">
                <div class="text-[11px] text-gov-slate-500">
                  ID Triagem: <span class="font-mono">{{ item.id.substring(0, 8) }}...</span>
                </div>

                <div class="flex items-center gap-2">
                  <!-- Botão Ignorar -->
                  <button
                    type="button"
                    (click)="ignorar(item)"
                    [disabled]="triagemService.carregando()"
                    class="px-3 py-1.5 rounded-lg border border-white/10 text-gov-slate-400 hover:text-white hover:bg-white/5 text-xs font-medium transition-colors cursor-pointer disabled:opacity-50"
                  >
                    Ignorar
                  </button>

                  <!-- Botão Cadastrar Contato & Vincular (Abre Quick Drawer) -->
                  <button
                    type="button"
                    (click)="abrirDrawer(item)"
                    [disabled]="triagemService.carregando()"
                    class="px-3.5 py-1.5 rounded-lg bg-gov-cobalt-600 hover:bg-gov-cobalt-500 text-white text-xs font-bold transition-all shadow-md shadow-gov-cobalt-950/40 flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                  >
                    <span>➕</span>
                    <span>Cadastrar Contato & Vincular</span>
                  </button>

                  <!-- Botão Confirmar Arquivamento (1 Clique) -->
                  @if (item.convenioSugeridoId) {
                    <button
                      type="button"
                      (click)="confirmarArquivamento(item)"
                      [disabled]="triagemService.carregando()"
                      class="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold transition-all shadow-md shadow-emerald-950/40 flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                    >
                      <span>✓</span>
                      <span>Confirmar Arquivamento (1 Clique)</span>
                    </button>
                  }
                </div>
              </div>

            </div>
          }
        </div>
      }

      <!-- Modal de Pré-visualização do Arquivo na Triagem -->
      @if (itemEmPreview(); as item) {
        <div
          class="fixed inset-0 z-50 flex items-center justify-center p-3 md:p-6 bg-black/85 backdrop-blur-md select-none font-sans overflow-hidden animate-in fade-in duration-200"
          (click)="fecharPreview()"
        >
          <div
            class="bg-[#111827] border border-white/10 rounded-2xl w-full max-w-5xl h-[88vh] flex flex-col shadow-2xl overflow-hidden"
            (click)="$event.stopPropagation()"
          >
            <!-- Header do Modal -->
            <header class="px-5 py-3 border-b border-white/10 flex items-center justify-between bg-white/[0.02]">
              <div class="flex items-center gap-3 min-w-0">
                <span class="text-xl">{{ isImageItem(item) ? '🖼️' : '📄' }}</span>
                <div class="truncate">
                  <h3 class="text-sm font-bold text-white truncate max-w-md">
                    {{ item.documentoNomeOriginal || 'Arquivo sem nome' }}
                  </h3>
                  <p class="text-[11px] text-gov-slate-400">
                    Enviado por <span class="text-white">{{ item.senderName || item.phoneNumber }}</span> em {{ formatarData(item.createdAt) }}
                  </p>
                </div>
              </div>

              <div class="flex items-center gap-2">
                @if (isImageItem(item)) {
                  <div class="flex items-center gap-1 bg-white/5 p-1 rounded-lg border border-white/10">
                    <button
                      type="button"
                      (click)="rotacionarPreview(-90)"
                      class="px-2 py-0.5 rounded hover:bg-white/10 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
                      title="Girar 90° Anti-horário"
                    >
                      ↺
                    </button>
                    <button
                      type="button"
                      (click)="rotacionarPreview(90)"
                      class="px-2 py-0.5 rounded hover:bg-white/10 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
                      title="Girar 90° Horário"
                    >
                      ↻
                    </button>
                    <div class="h-3 w-px bg-white/10 mx-0.5"></div>
                    <button
                      type="button"
                      (click)="ajustarZoomPreview(-0.15)"
                      class="px-2 py-0.5 rounded hover:bg-white/10 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
                      title="Reduzir Zoom"
                    >
                      -
                    </button>
                    <span class="font-mono text-xs text-slate-300 min-w-[36px] text-center">
                      {{ Math.round(zoomPreview() * 100) }}%
                    </span>
                    <button
                      type="button"
                      (click)="ajustarZoomPreview(0.15)"
                      class="px-2 py-0.5 rounded hover:bg-white/10 text-slate-300 text-xs font-bold transition-colors cursor-pointer"
                      title="Aumentar Zoom"
                    >
                      +
                    </button>
                    <button
                      type="button"
                      (click)="resetarVisaoPreview()"
                      class="px-2 py-0.5 rounded hover:bg-white/10 text-slate-400 text-[11px] transition-colors cursor-pointer"
                      title="Ajustar e Centralizar"
                    >
                      Ajustar
                    </button>
                  </div>
                }

                <a
                  [href]="obterUrlConteudo(item)"
                  target="_blank"
                  class="px-3 py-1.5 rounded-lg bg-white/5 hover:bg-white/10 text-slate-300 text-xs font-medium border border-white/10 transition-colors"
                >
                  Abrir Original ↗
                </a>

                <button
                  type="button"
                  (click)="fecharPreview()"
                  class="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-white/10 cursor-pointer"
                >
                  ✕
                </button>
              </div>
            </header>

            <!-- Corpo do Modal -->
            <main class="flex-1 bg-[#080d1a] relative overflow-hidden flex items-center justify-center p-3">
              @if (isImageItem(item)) {
                <div
                  class="relative w-full h-full flex items-center justify-center overflow-hidden"
                  [class.cursor-grab]="!isDraggingPreview()"
                  [class.cursor-grabbing]="isDraggingPreview()"
                  (mousedown)="iniciarArrastoPreview($event)"
                  (mousemove)="arrastarPreview($event)"
                  (mouseup)="finalizarArrastoPreview()"
                  (mouseleave)="finalizarArrastoPreview()"
                >
                  <div
                    class="relative transition-transform duration-75 select-none inline-block shadow-2xl rounded-lg border border-white/10"
                    [style.transform]="transformPreviewStyle()"
                    [style.transform-origin]="'center center'"
                  >
                    <img
                      [src]="obterUrlConteudo(item)"
                      [alt]="item.documentoNomeOriginal"
                      class="max-w-none block select-none pointer-events-none rounded max-h-[75vh]"
                    />
                  </div>

                  <!-- Dica -->
                  <div class="absolute bottom-3 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-black/60 backdrop-blur-md border border-white/10 text-[10px] text-slate-400 font-medium pointer-events-none flex items-center gap-2 shadow-lg z-20">
                    <span>🖱️ Arraste para mover</span>
                    <span *ngIf="rotacaoPreview() !== 0" class="text-amber-400 font-mono font-bold">• {{ rotacaoPreview() }}°</span>
                  </div>
                </div>
              } @else {
                <iframe
                  [src]="obterSafeUrl(item)"
                  class="w-full h-full rounded-xl border border-white/10 bg-white"
                  title="Pré-visualização do Documento"
                ></iframe>
              }
            </main>
          </div>
        </div>
      }

      <!-- Componente Quick Drawer de Cadastro de Contato Lateral -->
      <app-cadastrar-contato-drawer
        [item]="itemSelecionado()"
        [isOpen]="drawerAberto()"
        (closed)="fecharDrawer()"
        (saved)="onItemSalvo($event)"
      />

    </div>
  `
})
export class TriagemInboxPageComponent implements OnInit, OnDestroy {
  readonly Math = Math;
  readonly triagemService = inject(TriagemService);
  private readonly toast = inject(ToastService);
  private readonly sanitizer = inject(DomSanitizer);
  private timer: any = null;

  readonly termoBusca = signal<string>('');
  readonly itemSelecionado = signal<TriagemItem | null>(null);
  readonly drawerAberto = signal<boolean>(false);

  // Estados de Pré-visualização Inline
  readonly itemEmPreview = signal<TriagemItem | null>(null);
  readonly zoomPreview = signal<number>(1.0);
  readonly rotacaoPreview = signal<number>(0);
  readonly panPreviewX = signal<number>(0);
  readonly panPreviewY = signal<number>(0);
  readonly isDraggingPreview = signal<boolean>(false);
  private dragPreviewStartX = 0;
  private dragPreviewStartY = 0;
  private initialPanPreviewX = 0;
  private initialPanPreviewY = 0;

  readonly itensFiltrados = computed(() => {
    const termo = this.termoBusca().toLowerCase().trim();
    const itens = this.triagemService.itensPendentes();

    if (!termo) return itens;

    return itens.filter((i) => {
      const matchPhone = (i.phoneNumber || '').toLowerCase().includes(termo);
      const matchName = (i.senderName || i.pushName || '').toLowerCase().includes(termo);
      const matchConv = (i.convenioSugeridoNumeroSiconv || '').toLowerCase().includes(termo);
      const matchObjeto = (i.convenioSugeridoObjeto || '').toLowerCase().includes(termo);
      const matchDoc = (i.documentoNomeOriginal || '').toLowerCase().includes(termo);

      return matchPhone || matchName || matchConv || matchObjeto || matchDoc;
    });
  });

  readonly totalNovosRemetentes = computed(() => {
    return this.triagemService.itensPendentes().filter((i) => i.remetenteNovo).length;
  });

  readonly totalAmbiguidades = computed(() => {
    return this.triagemService.itensPendentes().filter((i) => i.motivoAmbiguidade && !i.remetenteNovo).length;
  });

  ngOnInit(): void {
    this.recarregar();
    this.timer = setInterval(() => {
      this.recarregar();
    }, 5000);
  }

  ngOnDestroy(): void {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  recarregar(): void {
    this.triagemService.carregarPendentes().subscribe();
  }

  abrirDrawer(item: TriagemItem): void {
    this.itemSelecionado.set(item);
    this.drawerAberto.set(true);
  }

  fecharDrawer(): void {
    this.drawerAberto.set(false);
    this.itemSelecionado.set(null);
  }

  onItemSalvo(_item: TriagemItem): void {
    this.recarregar();
  }

  confirmarArquivamento(item: TriagemItem): void {
    if (!item.convenioSugeridoId) {
      this.toast.erro('Convênio Ausente', 'Convênio não identificado para arquivamento direto.');
      return;
    }

    this.triagemService.confirmarArquivamento(item.id, item.convenioSugeridoId, item.faseSugerida).subscribe();
  }

  ignorar(item: TriagemItem): void {
    this.triagemService.ignorarItem(item.id).subscribe();
  }

  obterClasseBadgeConfianca(score: number): string {
    if (score >= 0.90) {
      return 'bg-emerald-500/20 text-emerald-300 border-emerald-500/30';
    } else if (score >= 0.70) {
      return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
    } else {
      return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
    }
  }

  formatarTelefone(phone?: string): string {
    if (!phone) return 'Sem telefone';
    if (phone.startsWith('55') && phone.length === 13) {
      // Formato: +55 (83) 99999-8888
      return `+55 (${phone.substring(2, 4)}) ${phone.substring(4, 9)}-${phone.substring(9)}`;
    }
    return phone;
  }

  formatarData(isoString?: string): string {
    if (!isoString) return '';
    try {
      const d = new Date(isoString);
      return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoString;
    }
  }

  formatarTamanho(bytes?: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
  }

  formatarFase(fase?: string): string {
    if (!fase) return '04 - Execução Física e Medições';
    return fase.replace(/_/g, ' ');
  }

  abrirPreview(item: TriagemItem): void {
    this.itemEmPreview.set(item);
    this.resetarVisaoPreview();
  }

  fecharPreview(): void {
    this.itemEmPreview.set(null);
  }

  isImageItem(item: TriagemItem): boolean {
    const ct = (item.documentoContentType || '').toLowerCase();
    const nome = (item.documentoNomeOriginal || '').toLowerCase();
    return ct.startsWith('image/') ||
      nome.endsWith('.png') ||
      nome.endsWith('.jpg') ||
      nome.endsWith('.jpeg') ||
      nome.endsWith('.webp') ||
      nome.endsWith('.bmp') ||
      nome.endsWith('.svg');
  }

  obterUrlConteudo(item: TriagemItem): string {
    return `/api/v1/documentos/${item.documentoId}/conteudo`;
  }

  obterSafeUrl(item: TriagemItem): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(this.obterUrlConteudo(item));
  }

  rotacionarPreview(delta: number): void {
    this.rotacaoPreview.update(r => (r + delta + 360) % 360);
  }

  ajustarZoomPreview(delta: number): void {
    this.zoomPreview.update(z => Math.max(0.3, Math.min(4.0, Math.round((z + delta) * 100) / 100)));
  }

  resetarVisaoPreview(): void {
    this.zoomPreview.set(1.0);
    this.rotacaoPreview.set(0);
    this.panPreviewX.set(0);
    this.panPreviewY.set(0);
  }

  iniciarArrastoPreview(e: MouseEvent): void {
    if (e.button !== 0) return;
    this.isDraggingPreview.set(true);
    this.dragPreviewStartX = e.clientX;
    this.dragPreviewStartY = e.clientY;
    this.initialPanPreviewX = this.panPreviewX();
    this.initialPanPreviewY = this.panPreviewY();
    e.preventDefault();
  }

  arrastarPreview(e: MouseEvent): void {
    if (!this.isDraggingPreview()) return;
    this.panPreviewX.set(this.initialPanPreviewX + (e.clientX - this.dragPreviewStartX));
    this.panPreviewY.set(this.initialPanPreviewY + (e.clientY - this.dragPreviewStartY));
  }

  finalizarArrastoPreview(): void {
    this.isDraggingPreview.set(false);
  }

  transformPreviewStyle(): string {
    return `translate(${this.panPreviewX()}px, ${this.panPreviewY()}px) scale(${this.zoomPreview()}) rotate(${this.rotacaoPreview()}deg)`;
  }
}
