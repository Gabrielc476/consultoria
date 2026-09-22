import { Injectable, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { RevisaoApiService } from './revisao-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import {
  Documento,
  DocumentoResumo,
  DadosRevisaoAnalista,
  RetencaoItem
} from '../model/documento.model';

const FORMULARIO_INICIAL: DadosRevisaoAnalista = {
  tipoDocumento: 'NOTA_FISCAL_SERVICOS',
  numeroDocumento: '',
  serieDocumento: '',
  chaveAcessoNfe: '',
  dataEmissao: new Date().toISOString().substring(0, 10),
  cnpjCredor: '',
  razaoSocialCredor: '',
  descricaoServico: '',
  numeroEmpenho: '',
  valorBruto: 0,
  valorTotalDeducoes: 0,
  valorLiquido: 0,
  retencoes: [],
  memorizarRegraFornecedor: false
};

@Injectable({
  providedIn: 'root'
})
export class RevisaoStateService {
  private readonly api = inject(RevisaoApiService);
  private readonly auth = inject(AuthService);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  // Sinais de Estado
  public readonly documentoAtual = signal<Documento | null>(null);
  public readonly arquivoBlobUrl = signal<string | null>(null);
  public readonly carregandoArquivo = signal<boolean>(false);
  public readonly filaPendentes = signal<DocumentoResumo[]>([]);
  public readonly campoEmFoco = signal<string | null>(null);
  public readonly formulario = signal<DadosRevisaoAnalista>(FORMULARIO_INICIAL);
  public readonly carregando = signal<boolean>(false);
  public readonly salvando = signal<boolean>(false);
  public readonly modalRejeicaoAberto = signal<boolean>(false);
  public readonly drawerFilaAberto = signal<boolean>(false);

  // Sinais Computados (Reatividade Pura)
  public readonly valorTotalDeducoes = computed(() => {
    const retencoes = this.formulario().retencoes || [];
    return retencoes.reduce((acc, r) => acc + (Number(r.valorRetido) || 0), 0);
  });

  public readonly valorLiquidoCalculado = computed(() => {
    const bruto = Number(this.formulario().valorBruto) || 0;
    const deducoes = this.valorTotalDeducoes();
    return Math.max(0, Math.round((bruto - deducoes) * 100) / 100);
  });

  public readonly diferencaLiquido = computed(() => {
    const liquidoInformado = Number(this.formulario().valorLiquido) || 0;
    const calculado = this.valorLiquidoCalculado();
    return Math.round(Math.abs(liquidoInformado - calculado) * 100) / 100;
  });

  public readonly possuiDivergencia = computed(() => {
    return this.diferencaLiquido() > 0.02;
  });

  public readonly confidenceScores = computed(() => {
    return this.documentoAtual()?.extracaoSugerida?.scoresConfiancaCampos || {};
  });

  public readonly boundingBoxes = computed(() => {
    return this.documentoAtual()?.boundingBoxes || {};
  });

  public readonly totalPendentes = computed(() => {
    return this.filaPendentes().length;
  });

  public carregarDocumento(id: string): void {
    this.carregando.set(true);
    this.carregarArquivoBinario(id);
    this.api.getDocumento(id).subscribe({
      next: (doc) => {
        this.documentoAtual.set(doc);
        this.preencherFormularioComDados(doc);
        this.carregando.set(false);
      },
      error: (err) => {
        this.toast.erro('Falha ao Carregar Documento', 'Não foi possível buscar os dados do documento.');
        this.carregando.set(false);
      }
    });
  }

  public carregarArquivoBinario(id: string): void {
    if (this.arquivoBlobUrl()) {
      URL.revokeObjectURL(this.arquivoBlobUrl()!);
      this.arquivoBlobUrl.set(null);
    }
    this.carregandoArquivo.set(true);
    this.api.baixarArquivo(id).subscribe({
      next: (blob) => {
        const pdfBlob = blob.type === 'application/pdf' ? blob : new Blob([blob], { type: 'application/pdf' });
        const objectUrl = URL.createObjectURL(pdfBlob);
        this.arquivoBlobUrl.set(objectUrl);
        this.carregandoArquivo.set(false);
      },
      error: (err) => {
        console.error('Falha ao baixar arquivo binário:', err);
        this.carregandoArquivo.set(false);
      }
    });
  }

  public carregarFila(): void {
    this.api.listarDocumentos('EM_CONFERENCIA', 0, 50).subscribe({
      next: (page) => {
        const lista = page.items || page.content || [];
        const resumos: DocumentoResumo[] = lista.map(d => ({
          id: d.id,
          nomeArquivoOriginal: d.nomeArquivoOriginal,
          status: d.status,
          createdAt: d.createdAt,
          valorBruto: d.extracaoSugerida?.valorBruto,
          numeroDocumento: d.extracaoSugerida?.numeroDocumento,
          credor: d.extracaoSugerida?.razaoSocialCredor
        }));
        this.filaPendentes.set(resumos);
      },
      error: () => {
        // Ignora silenciosamente ou mantém lista vazia
      }
    });
  }

  public atualizarCampo<K extends keyof DadosRevisaoAnalista>(campo: K, valor: DadosRevisaoAnalista[K]): void {
    this.formulario.update(form => {
      const updated = { ...form, [campo]: valor };
      if (campo === 'valorBruto') {
        const deducoes = (updated.retencoes || []).reduce((acc, r) => acc + (Number(r.valorRetido) || 0), 0);
        updated.valorTotalDeducoes = deducoes;
        updated.valorLiquido = Math.max(0, Math.round(((Number(valor) || 0) - deducoes) * 100) / 100);
      }
      return updated;
    });
  }

  public atualizarRetencao(index: number, retencao: RetencaoItem): void {
    this.formulario.update(form => {
      const retencoes = [...form.retencoes];
      retencoes[index] = retencao;
      const deducoes = retencoes.reduce((acc, r) => acc + (Number(r.valorRetido) || 0), 0);
      const liquido = Math.max(0, Math.round(((Number(form.valorBruto) || 0) - deducoes) * 100) / 100);
      return {
        ...form,
        retencoes,
        valorTotalDeducoes: deducoes,
        valorLiquido: liquido
      };
    });
  }

  public adicionarRetencao(tipo = 'OUTROS'): void {
    this.formulario.update(form => {
      const nova: RetencaoItem = {
        tipoTributo: tipo,
        valorRetido: 0,
        aliquotaPercentual: 0
      };
      return {
        ...form,
        retencoes: [...form.retencoes, nova]
      };
    });
  }

  public removerRetencao(index: number): void {
    this.formulario.update(form => {
      const retencoes = form.retencoes.filter((_, i) => i !== index);
      const deducoes = retencoes.reduce((acc, r) => acc + (Number(r.valorRetido) || 0), 0);
      const liquido = Math.max(0, Math.round(((Number(form.valorBruto) || 0) - deducoes) * 100) / 100);
      return {
        ...form,
        retencoes,
        valorTotalDeducoes: deducoes,
        valorLiquido: liquido
      };
    });
  }

  public sincronizarValorLiquido(): void {
    const calculado = this.valorLiquidoCalculado();
    const deducoes = this.valorTotalDeducoes();
    this.formulario.update(form => ({
      ...form,
      valorTotalDeducoes: deducoes,
      valorLiquido: calculado
    }));
    this.toast.sucesso('Valor Líquido Sincronizado', `Ajustado para R$ ${calculado.toFixed(2)}.`);
  }

  public aprovar(): void {
    const doc = this.documentoAtual();
    if (!doc) return;

    const analista = this.auth.usuario();
    const analistaId = analista?.analistaId || '22222222-2222-2222-2222-222222222222';

    this.salvando.set(true);

    const retencoesMapeadas = (this.formulario().retencoes || []).map(r => ({
      tipo: r.tipoTributo || (r as any).tipo || 'OUTROS',
      valor: Number(r.valorRetido ?? (r as any).valor) || 0,
      aliquota: Number(r.aliquotaPercentual ?? (r as any).aliquota) || 0
    }));

    const payload = {
      analistaId,
      revisao: {
        ...this.formulario(),
        cnpjCredor: this.formulario().cnpjCredor?.trim() || '',
        valorTotalDeducoes: this.valorTotalDeducoes(),
        retencoes: retencoesMapeadas
      },
      observacao: 'Aprovado via interface de conferência lado a lado GovFlow'
    };

    this.api.aprovarDocumento(doc.id, payload).subscribe({
      next: (aprovado) => {
        this.toast.sucesso('Documento Aprovado!', `NF ${aprovado.extracaoSugerida?.numeroDocumento || doc.id} pronta para o Transferegov.`);
        this.salvando.set(false);
        this.avancarProximo(doc.id);
      },
      error: () => {
        this.salvando.set(false);
      }
    });
  }

  public rejeitar(motivo: string): void {
    const doc = this.documentoAtual();
    if (!doc) return;

    if (!motivo || motivo.trim().length < 5) {
      this.toast.aviso('Motivo Obrigatório', 'Informe uma justificativa de ao menos 5 caracteres para a rejeição.');
      return;
    }

    const analista = this.auth.usuario();
    const analistaId = analista?.analistaId || '22222222-2222-2222-2222-222222222222';

    this.salvando.set(true);

    this.api.rejeitarDocumento(doc.id, { analistaId, motivo }).subscribe({
      next: () => {
        this.toast.aviso('Documento Rejeitado', 'O documento foi marcado como rejeitado.');
        this.salvando.set(false);
        this.fecharModalRejeicao();
        this.avancarProximo(doc.id);
      },
      error: () => {
        this.salvando.set(false);
      }
    });
  }

  public avancarProximo(idAtual: string): void {
    // Atualiza a fila removendo o que acabou de ser processado
    this.filaPendentes.update(lista => lista.filter(d => d.id !== idAtual));

    const pendentes = this.filaPendentes();
    if (pendentes.length > 0) {
      const proximo = pendentes[0];
      this.router.navigate(['/documentos', proximo.id, 'revisar']);
      this.carregarDocumento(proximo.id);
    } else {
      this.toast.sucesso('Fila Zerada!', 'Todos os documentos em conferência foram concluídos.');
      this.carregarFila();
    }
  }

  public focarCampo(nomeCampo: string | null): void {
    this.campoEmFoco.set(nomeCampo);
  }

  public toggleDrawer(): void {
    this.drawerFilaAberto.update(v => !v);
  }

  public abrirModalRejeicao(): void {
    this.modalRejeicaoAberto.set(true);
  }

  public fecharModalRejeicao(): void {
    this.modalRejeicaoAberto.set(false);
  }

  private preencherFormularioComDados(doc: Documento): void {
    const sugestao = doc.extracaoSugerida;
    const revisaoPrevia = doc.dadosRevisao;

    if (revisaoPrevia) {
      this.formulario.set({ ...revisaoPrevia });
      return;
    }

    if (sugestao) {
      this.formulario.set({
        tipoDocumento: sugestao.tipoDocumento || 'NOTA_FISCAL_SERVICOS',
        numeroDocumento: sugestao.numeroDocumento || '',
        serieDocumento: sugestao.serieDocumento || '',
        chaveAcessoNfe: sugestao.chaveAcessoNfe || '',
        dataEmissao: sugestao.dataEmissao || new Date().toISOString().substring(0, 10),
        cnpjCredor: sugestao.cnpjCredor || '',
        razaoSocialCredor: sugestao.razaoSocialCredor || '',
        descricaoServico: sugestao.descricaoServico || '',
        numeroEmpenho: sugestao.numeroEmpenho || '',
        valorBruto: Number(sugestao.valorBruto) || 0,
        valorTotalDeducoes: Number(sugestao.valorTotalDeducoes) || 0,
        valorLiquido: Number(sugestao.valorLiquido) || 0,
        retencoes: (sugestao.retencoes || []).map((r: any) => ({
          tipoTributo: r.tipoTributo || r.tipo || 'OUTROS',
          aliquotaPercentual: r.aliquotaPercentual ?? r.aliquota ?? 0,
          baseCalculo: r.baseCalculo,
          valorRetido: Number(r.valorRetido ?? r.valor) || 0
        })),
        memorizarRegraFornecedor: false
      });
    } else {
      this.formulario.set({
        ...FORMULARIO_INICIAL,
        numeroDocumento: doc.nomeArquivoOriginal
      });
    }
  }
}
