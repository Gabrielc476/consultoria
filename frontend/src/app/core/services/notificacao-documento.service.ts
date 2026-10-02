import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RevisaoApiService } from '../../features/revisao-documento/services/revisao-api.service';
import { TriagemService } from '../../features/triagem/services/triagem.service';
import { ToastService } from '../ui/toast.service';
import { Documento } from '../../features/revisao-documento/model/documento.model';
import { TriagemItem } from '../../features/triagem/model/triagem.model';

export interface NotificacaoItem {
  id: string;
  documentoId?: string;
  tipo: 'ESTEIRA' | 'TRIAGEM';
  titulo: string;
  descricao: string;
  horario: Date;
  lida: boolean;
  link?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificacaoDocumentoService {
  private readonly apiDocumentos = inject(RevisaoApiService);
  private readonly triagemService = inject(TriagemService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  // Sinais reativos de Contagem e Lista de Notificações
  public readonly totalDocumentosEsteira = signal<number>(0);
  public readonly totalTriagemPendentes = signal<number>(0);
  public readonly notificacoes = signal<NotificacaoItem[]>([]);
  public readonly dropdownAberto = signal<boolean>(false);

  // Armazenamento em memória dos IDs conhecidos para detectar novos itens
  private readonly idsConhecidos = new Set<string>();
  private primeiraCarga = true;
  private intervaloTimer: any = null;

  constructor() {
    this.iniciarMonitoramento();
  }

  public iniciarMonitoramento(): void {
    this.verificarNovosItens();
    if (!this.intervaloTimer) {
      this.intervaloTimer = setInterval(() => {
        this.verificarNovosItens();
      }, 5000); // Polling automático a cada 5 segundos
    }
  }

  public pararMonitoramento(): void {
    if (this.intervaloTimer) {
      clearInterval(this.intervaloTimer);
      this.intervaloTimer = null;
    }
  }

  public alternarDropdown(): void {
    this.dropdownAberto.update(v => !v);
  }

  public fecharDropdown(): void {
    this.dropdownAberto.set(false);
  }

  public marcarTodasComoLidas(): void {
    this.notificacoes.update(lista => lista.map(n => ({ ...n, lida: true })));
  }

  public abrirDocumento(notificacao: NotificacaoItem): void {
    notificacao.lida = true;
    this.fecharDropdown();
    if (notificacao.link) {
      this.router.navigateByUrl(notificacao.link);
    }
  }

  public verificarNovosItens(): void {
    // 1. Busca documentos na esteira
    this.apiDocumentos.listarDocumentos(undefined, 0, 50).subscribe({
      next: (page) => {
        const itens: Documento[] = page.items || page.content || [];
        const pendentes = itens.filter(d => d.status === 'EM_CONFERENCIA');
        this.totalDocumentosEsteira.set(pendentes.length);

        itens.forEach(doc => {
          if (!this.idsConhecidos.has(doc.id)) {
            this.idsConhecidos.add(doc.id);
            if (!this.primeiraCarga) {
              this.dispararNotificacaoDocumento(doc);
            }
          }
        });

        // 2. Busca itens na triagem
        this.triagemService.carregarPendentes().subscribe({
          next: (triagemItens: TriagemItem[]) => {
            this.totalTriagemPendentes.set(triagemItens.length);
            triagemItens.forEach(t => {
              const key = 'triagem-' + t.id;
              if (!this.idsConhecidos.has(key)) {
                this.idsConhecidos.add(key);
                if (!this.primeiraCarga) {
                  this.dispararNotificacaoTriagem(t);
                }
              }
            });
            this.primeiraCarga = false;
          },
          error: () => {
            this.primeiraCarga = false;
          }
        });
      },
      error: () => {
        this.primeiraCarga = false;
      }
    });
  }

  private dispararNotificacaoDocumento(doc: Documento): void {
    const nome = doc.nomeArquivoOriginal || 'Documento sem nome';
    const numero = doc.extracaoSugerida?.numeroDocumento ? ` (${doc.extracaoSugerida.numeroDocumento})` : '';
    const titulo = `📄 Novo Documento na Esteira!`;
    const descricao = `${nome}${numero} recebido via WhatsApp e pronto para conferência.`;

    const item: NotificacaoItem = {
      id: Math.random().toString(36).substring(2, 9),
      documentoId: doc.id,
      tipo: 'ESTEIRA',
      titulo,
      descricao,
      horario: new Date(),
      lida: false,
      link: `/documentos/${doc.id}/revisar`
    };

    this.notificacoes.update(lista => [item, ...lista.slice(0, 19)]);
    this.toast.info(titulo, descricao, 6000);
    this.tocarAlertaSonoro();
  }

  private dispararNotificacaoTriagem(item: TriagemItem): void {
    const titulo = `📥 Novo Item na Caixa de Triagem!`;
    const remetente = item.senderName || item.pushName || item.phoneNumber || 'Remetente';
    const descricao = `Documento de ${remetente} aguardando validação de convênio.`;

    const notif: NotificacaoItem = {
      id: Math.random().toString(36).substring(2, 9),
      tipo: 'TRIAGEM',
      titulo,
      descricao,
      horario: new Date(),
      lida: false,
      link: `/triagem`
    };

    this.notificacoes.update(lista => [notif, ...lista.slice(0, 19)]);
    this.toast.aviso(titulo, descricao, 6000);
    this.tocarAlertaSonoro();
  }

  private tocarAlertaSonoro(): void {
    try {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      if (AudioContextClass) {
        const ctx = new AudioContextClass();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
        osc.frequency.exponentialRampToValueAtTime(880, ctx.currentTime + 0.15); // A5
        gain.gain.setValueAtTime(0.1, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.3);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.3);
      }
    } catch {
      // Bloqueio de autoplay do navegador ou sem suporte
    }
  }
}
