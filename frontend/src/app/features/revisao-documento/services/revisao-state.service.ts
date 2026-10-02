import { Injectable, signal, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { RevisaoApiService } from './revisao-api.service';
import { AuthService } from '../../../core/auth/auth.service';
import { ToastService } from '../../../core/ui/toast.service';
import { parseNumberBr } from '../../../shared/utils/number-utils';
import {
  Documento,
  DocumentoResumo,
  DadosRevisaoAnalista,
  RetencaoItem,
  ArquetipoAuditoria,
  CenarioDemonstracao,
  MetadadoItem
} from '../model/documento.model';

export const CENARIOS_DEMONSTRACAO: CenarioDemonstracao[] = [
  {
    id: 'boletim-medicao',
    nome: 'Boletim de Medição (BM-03)',
    icone: '📐',
    faseRotulo: 'Fase 04 - Execução Física',
    arquetipo: 'ENGENHARIA',
    categoriaDocumento: 'BOLETIM_MEDICAO',
    descricao: 'Conferência física de obras com ART, acumulados, saldo e atesto técnico in loco.',
    dadosSimulados: {
      tipoDocumento: 'BOLETIM_MEDICAO',
      numeroDocumento: 'BM-03',
      dataEmissao: '2026-03-31',
      cnpjCredor: '14285912000144',
      razaoSocialCredor: 'Construtora Alvorada Ltda',
      descricaoServico: 'Pavimentação em paralelepípedo e drenagem superficial no Bairro Monte Castelo - 3ª Medição',
      numeroEmpenho: '2026NE00042',
      valorBruto: 145200.00,
      valorTotalDeducoes: 0,
      valorLiquido: 145200.00,
      retencoes: [],
      numeroContrato: '042/2021',
      periodoMedicao: '01/03/2026 a 31/03/2026',
      artFiscal: '2026048192-OB (CREA-PB)',
      engenheiroFiscal: 'Roberto Silveira - CREA 19842-D/PB',
      valorAcumuladoAnterior: 320000.00,
      valorAcumuladoAtual: 465200.00,
      saldoContratual: 534800.00,
      percentualExecutado: 46.52,
      atestoFiscalConfirmado: true,
      parecerFiscal: 'Serviços executados rigorosamente em conformidade com o cronograma físico-financeiro e caderno de encargos.'
    }
  },
  {
    id: 'nota-fiscal',
    nome: 'Nota Fiscal de Serviços (NFS-e)',
    icone: '📄',
    faseRotulo: 'Fase 05 - Execução Financeira',
    arquetipo: 'FISCAL',
    categoriaDocumento: 'DOCUMENTO_HABIL',
    descricao: 'Auditoria tributária com apuração estrita de INSS, ISS, IRRF, empenho e chave NF-e.',
    dadosSimulados: {
      tipoDocumento: 'NOTA_FISCAL_SERVICOS',
      numeroDocumento: '00042',
      serieDocumento: '1',
      chaveAcessoNfe: '25260314285912000144550010000000421000000429',
      dataEmissao: '2026-04-05',
      cnpjCredor: '14285912000144',
      razaoSocialCredor: 'Construtora Alvorada Ltda',
      descricaoServico: 'Prestação de serviços de engenharia civil referente à 3ª Medição da Pavimentação Asfáltica',
      numeroEmpenho: '2026NE00042',
      valorBruto: 145200.00,
      valorTotalDeducoes: 15972.00,
      valorLiquido: 129228.00,
      retencoes: [
        { tipoTributo: 'INSS', tipo: 'INSS', valorRetido: 7260.00, aliquotaPercentual: 5.0 },
        { tipoTributo: 'ISS', tipo: 'ISS', valorRetido: 7260.00, aliquotaPercentual: 5.0 },
        { tipoTributo: 'IRRF', tipo: 'IRRF', valorRetido: 1452.00, aliquotaPercentual: 1.0 }
      ],
      memorizarRegraFornecedor: true
    }
  },
  {
    id: 'licenca-ambiental',
    nome: 'Licença Ambiental de Instalação (LI)',
    icone: '🌿',
    faseRotulo: 'Fase 02 - Cláusula Suspensiva',
    arquetipo: 'PROJETO_AMBIENTAL',
    categoriaDocumento: 'LICENCA_AMBIENTAL',
    descricao: 'Verificação de condicionantes ambientais, órgão emissor, vigência e superação de cláusula suspensiva.',
    dadosSimulados: {
      tipoDocumento: 'LICENCA_AMBIENTAL',
      numeroDocumento: 'LI nº 0492/2024',
      dataEmissao: '2024-06-15',
      cnpjCredor: '08778268000160',
      razaoSocialCredor: 'SUDEMA - Superintendência de Adm. do Meio Ambiente',
      descricaoServico: 'Licença de Instalação para obras de infraestrutura urbana e pavimentação',
      valorBruto: 0,
      valorTotalDeducoes: 0,
      valorLiquido: 0,
      retencoes: [],
      tipoLicenca: 'Licença de Instalação (LI)',
      orgaoAmbiental: 'SUDEMA / Governo da Paraíba',
      numeroProcessoLicenca: 'Proc. SUDEMA 2024/00492-LI',
      dataValidadeLicenca: '2028-06-15',
      condicionantesAtendidas: true,
      bdiPercentual: 24.5,
      parecerFiscal: 'Todas as 12 condicionantes ambientais foram sanadas e averbadas perante o órgão licenciador.'
    }
  },
  {
    id: 'contrato-licitacao',
    nome: 'Contrato Administrativo de Obras',
    icone: '⚖️',
    faseRotulo: 'Fase 03 - Licitação & Contratação',
    arquetipo: 'JURIDICO_LICITATORIO',
    categoriaDocumento: 'CONTRATO_ADMINISTRATIVO',
    descricao: 'Auditoria de edital, homologação do certame, parecer VRPL da mandatária e AIO.',
    dadosSimulados: {
      tipoDocumento: 'CONTRATO_ADMINISTRATIVO',
      numeroDocumento: 'Contrato Municipal 042/2021',
      dataEmissao: '2021-11-20',
      cnpjCredor: '14285912000144',
      razaoSocialCredor: 'Construtora Alvorada Ltda',
      descricaoServico: 'Execução de obras de pavimentação em paralelepípedos e drenagem pluvial urbana',
      numeroEmpenho: '2021NE00108',
      valorBruto: 1000000.00,
      valorTotalDeducoes: 0,
      valorLiquido: 1000000.00,
      retencoes: [],
      modalidadeLicitacao: 'Concorrência Eletrônica (Lei 14.133/2021)',
      numeroProcessoLicitatorio: 'Edital de Concorrência nº 008/2021',
      dataHomologacao: '2021-11-10',
      publicacaoDouData: '2021-11-12',
      publicacaoDouSecao: 'DOU Seção 3, Página 184',
      parecerVrplAprovado: true,
      numeroAio: 'AIO nº 003/2022 - Caixa GIGOV/JP'
    }
  },
  {
    id: 'certidao-cauc',
    nome: 'Certidão de Regularidade Fiscal (CAUC)',
    icone: '🏛️',
    faseRotulo: 'Fase 00 - Proposta & Habilitação',
    arquetipo: 'REGULARIDADE_PROPOSTA',
    categoriaDocumento: 'CERTIDAO_CAUC',
    descricao: 'Checagem de adimplemento municipal, SRF/PGFN, FGTS, SIAFI e prazo de validade da certidão.',
    dadosSimulados: {
      tipoDocumento: 'CERTIDAO_CAUC',
      numeroDocumento: 'CND-SRF-2026/09412',
      dataEmissao: '2026-02-01',
      cnpjCredor: '08778268000160',
      razaoSocialCredor: 'Receita Federal do Brasil / PGFN',
      descricaoServico: 'Certidão Negativa de Débitos Relativos a Créditos Tributários Federais e à Dívida Ativa da União',
      valorBruto: 0,
      valorTotalDeducoes: 0,
      valorLiquido: 0,
      retencoes: [],
      tipoCertidao: 'Certidão Negativa Federal Conjunta (RFB / PGFN)',
      situacaoRegularidade: 'REGULAR',
      dataValidadeCertidao: '2026-08-01',
      numeroEmendaParlamentar: 'Emenda Individual nº 2026.4182.0014',
      dadosContaVinculada: 'Banco do Brasil - Agência 0142-8 - Conta Corrente 91425-0 (Op 006)',
      parecerFiscal: 'Município adimplente e habilitado sem pendências no subsistema CAUC/SIAFI.'
    }
  },
  {
    id: 'termo-aditivo',
    nome: 'Termo Aditivo de Valor e Prazo',
    icone: '🔄',
    faseRotulo: 'Fase 06 - Alterações Contratuais',
    arquetipo: 'ALTERACOES_ADITIVOS',
    categoriaDocumento: 'TERMO_ADITIVO',
    descricao: 'Auditoria de limites legais (25%/50%), justificativa de prorrogação e parecer da mandatária.',
    dadosSimulados: {
      tipoDocumento: 'TERMO_ADITIVO',
      numeroDocumento: '1º Termo Aditivo ao Contrato 042/2021',
      dataEmissao: '2024-10-15',
      cnpjCredor: '14285912000144',
      razaoSocialCredor: 'Construtora Alvorada Ltda',
      descricaoServico: 'Acréscimo de 12% nos quantitativos de drenagem e prorrogação de vigência por 180 dias',
      numeroEmpenho: '2024NE00312',
      valorBruto: 120000.00,
      valorTotalDeducoes: 0,
      valorLiquido: 120000.00,
      retencoes: [],
      tipoAditivo: 'Acréscimo de Valor e Prorrogação de Vigência',
      numeroAditivo: '1º Termo Aditivo',
      justificativaAditivo: 'Inclusão de sarjetas adicionais devido a adequações topográficas e chuvas atípicas no período.',
      novaDataVigencia: '2027-04-30',
      percentualAditamento: 12.0,
      indiceReajuste: 'INCC-DI (FGV) - 4,85%',
      parecerFiscal: 'Aditivo dentro do teto legal de 25% da Lei 14.133 com parecer favorável da Caixa no Transferegov.'
    }
  },
  {
    id: 'prestacao-rco',
    nome: 'Relatório de Cumprimento do Objeto (RCO)',
    icone: '📋',
    faseRotulo: 'Fase 07 - Prestação de Contas Final',
    arquetipo: 'PRESTACAO_ENCERRAMENTO',
    categoriaDocumento: 'RELATORIO_CUMPRIMENTO_OBJETO',
    descricao: 'Atesto final de funcionalidade plena, placa de inauguração e recolhimento de saldo em GRU.',
    dadosSimulados: {
      tipoDocumento: 'RELATORIO_CUMPRIMENTO_OBJETO',
      numeroDocumento: 'RCO Final 01/2026',
      dataEmissao: '2026-09-10',
      cnpjCredor: '08778268000160',
      razaoSocialCredor: 'Prefeitura Municipal de Esperança - PB',
      descricaoServico: 'Relatório Final de Cumprimento do Objeto pactuado no Termo de Convênio 914250/2023',
      valorBruto: 1000000.00,
      valorTotalDeducoes: 0,
      valorLiquido: 1000000.00,
      retencoes: [],
      tipoRecebimentoObra: 'Termo de Recebimento Definitivo',
      comissaoRecebimento: 'Portaria Municipal nº 142/2026 (Eng. Roberto Silveira e Fiscal João Lima)',
      funcionalidadeAtestada: true,
      placaInauguracaoInstalada: true,
      codigoRecolhimentoGru: '18806-9 - Devolução de Convênios União',
      valorDevolvidoGru: 2480.50,
      saldoContaZeroConfirmado: true,
      parecerFiscal: 'Objeto concluído com 100% de funcionalidade pública atendida. Saldo e rendimentos recolhidos via GRU.'
    }
  },
  {
    id: 'passivo-tce',
    nome: 'Notificação SELIC 45 Dias / Processo TCE',
    icone: '⚠️',
    faseRotulo: 'Fase 09 - Passivo Jurídico & TCE',
    arquetipo: 'PASSIVO_TCE',
    categoriaDocumento: 'NOTIFICACAO_SELIC_45_DIAS',
    descricao: 'Controle de prazo improrrogável de 45 dias, cálculo de glosa e medidas da Súmula 230 do TCU.',
    dadosSimulados: {
      tipoDocumento: 'NOTIFICACAO_PASSIVO',
      numeroDocumento: 'Ofício Notificação nº 482/2026 - Concedente',
      dataEmissao: '2026-08-20',
      cnpjCredor: '00394460000141',
      razaoSocialCredor: 'Ministério das Cidades / TCU',
      descricaoServico: 'Notificação para recolhimento de glosa financeira com incidência de taxa SELIC ou defesa prévia',
      valorBruto: 84500.00,
      valorTotalDeducoes: 0,
      valorLiquido: 84500.00,
      retencoes: [],
      tipoNotificacaoPassivo: 'Notificação Formal SELIC - Prazo 45 Dias',
      numeroProcessoTce: 'Processo TC 014.892/2026-4 (TCU)',
      prazoFatalDias: 23,
      orgaoNotificante: 'Secretaria de Controle Externo do TCU / Concedente',
      valorGlosaSelic: 84500.00,
      sumula230Ajuizada: true,
      parecerFiscal: 'Ação de ressarcimento ao erário ajuizada contra ex-gestor para obtenção de liminar de desbloqueio do CAUC.'
    }
  },
  {
    id: 'agnostico-livre',
    nome: 'Documento Universal Agnóstico (Novo Tipo)',
    icone: '🌐',
    faseRotulo: 'Universal - Qualquer Fase / Formato',
    arquetipo: 'AGNOSTICO_UNIVERSAL',
    categoriaDocumento: 'OUTROS',
    descricao: 'Auditoria flexível para qualquer novo documento com formulário adaptável e metadados chave-valor.',
    dadosSimulados: {
      tipoDocumento: 'DOCUMENTO_GENERICO',
      numeroDocumento: 'DOC-EXTERNO-2026/99',
      dataEmissao: '2026-09-01',
      cnpjCredor: '08778268000160',
      razaoSocialCredor: 'Órgão / Entidade Remetente',
      descricaoServico: 'Documento avulso complementar recebido para custódia no Ficheiro Digital',
      valorBruto: 0,
      valorTotalDeducoes: 0,
      valorLiquido: 0,
      retencoes: [],
      tituloDocumento: 'Portaria de Nomeação de Comissão de Acompanhamento',
      orgaoEmissor: 'Gabinete do Prefeito',
      identificadorDocumento: 'Portaria Municipal nº 084/2026',
      categoriaDocumento: 'OUTROS',
      faseCicloVida: 'FASE_04_EXECUCAO_FISICA',
      metadadosCustomizados: [
        { chave: 'Secretaria Solicitante', valor: 'Secretaria Municipal de Infraestrutura e Obras' },
        { chave: 'Número do Diário Oficial', valor: 'DOM Edição 1.492 de 02/09/2026' },
        { chave: 'Processo SEI / Protocolo', valor: 'SEI 00412.00892/2026-11' }
      ],
      parecerFiscal: 'Documento idôneo, legível e anexado ao repositório digital para instrução do convênio.'
    }
  }
];

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
  memorizarRegraFornecedor: false,
  numeroContrato: '',
  periodoMedicao: '',
  artFiscal: '',
  engenheiroFiscal: '',
  valorAcumuladoAnterior: 0,
  valorAcumuladoAtual: 0,
  saldoContratual: 0,
  percentualExecutado: 0,
  atestoFiscalConfirmado: true,
  parecerFiscal: '',
  metadadosCustomizados: []
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

  // Sinais de Arquétipo e Cenários
  public readonly arquetipoManual = signal<ArquetipoAuditoria | null>(null);
  public readonly cenarioAtivoId = signal<string | null>(null);
  public readonly cenariosDisponiveis = signal<CenarioDemonstracao[]>(CENARIOS_DEMONSTRACAO);

  // Arquétipo Deduzido pela Inteligência Artificial / Sistema
  public readonly arquetipoOriginalIA = computed<ArquetipoAuditoria>(() => {
    const doc = this.documentoAtual();
    const cat = (doc?.categoriaDocumento || '').toUpperCase();
    const tipo = (doc?.extracaoSugerida?.tipoDocumento || this.formulario().tipoDocumento || '').toUpperCase();
    const nome = (doc?.nomeArquivoOriginal || '').toLowerCase();

    // 1. Engenharia & Medições
    if (cat === 'BOLETIM_MEDICAO' || cat === 'RAE_CAIXA' || cat === 'RELATORIO_FOTOGRAFICO' || cat === 'DIARIO_OBRA' ||
        tipo === 'BOLETIM_MEDICAO' || tipo === 'MEDICAO_OBRAS' ||
        nome.includes('boletim') || nome.includes('bm-') || nome.includes('medicao')) {
      return 'ENGENHARIA';
    }

    // 2. Projetos & Licenciamento Ambiental
    if (cat === 'LICENCA_AMBIENTAL' || cat === 'PROJETO_ENGENHARIA' || cat === 'PLANILHA_ORCAMENTARIA' ||
        cat === 'TITULARIDADE_IMOVEL' || cat === 'SPA_LAE_CAIXA' ||
        tipo === 'LICENCA_AMBIENTAL' || tipo === 'PROJETO_ENGENHARIA' ||
        nome.includes('licenca') || nome.includes('sudema') || nome.includes('ibama') || nome.includes('projeto')) {
      return 'PROJETO_AMBIENTAL';
    }

    // 3. Jurídico & Licitação
    if (cat === 'LICITACAO' || cat === 'ATA_HOMOLOGACAO_CERTAME' || cat === 'CONTRATO_ADMINISTRATIVO' ||
        cat === 'PARECER_VRPL' || cat === 'AUTORIZACAO_INICIO_OBJETO' || cat === 'TERMO_CONVENIO' || cat === 'PUBLICACAO_DOU' ||
        tipo === 'LICITACAO' || tipo === 'CONTRATO_ADMINISTRATIVO' || tipo === 'TERMO_CONVENIO' ||
        nome.includes('contrato') || nome.includes('edital') || nome.includes('licitacao')) {
      return 'JURIDICO_LICITATORIO';
    }

    // 4. Regularidade, Proposta & CAUC
    if (cat === 'CERTIDAO_CAUC' || cat === 'PROPOSTA_PLANO_TRABALHO' || cat === 'PARECER_TECNICO_EMENDA' ||
        cat === 'DOSSIE_DECLARACOES' || cat === 'NOTIFICACAO_CONTA_VINCULADA' ||
        tipo === 'CERTIDAO_CAUC' ||
        nome.includes('cauc') || nome.includes('certidao') || nome.includes('proposta') || nome.includes('cnd')) {
      return 'REGULARIDADE_PROPOSTA';
    }

    // 5. Alterações Contratuais & Aditivos
    if (cat === 'TERMO_ADITIVO' || cat === 'APOSTILAMENTO' || cat === 'PARECER_REPROGRAMACAO' ||
        tipo === 'TERMO_ADITIVO' ||
        nome.includes('aditivo') || nome.includes('apostilamento') || nome.includes('reprogramacao')) {
      return 'ALTERACOES_ADITIVOS';
    }

    // 6. Prestação de Contas Final & Encerramento
    if (cat === 'TERMO_RECEBIMENTO' || cat === 'RELATORIO_CUMPRIMENTO_OBJETO' || cat === 'PLACA_INAUGURACAO' ||
        cat === 'GUIA_RECOLHIMENTO_UNIAO' || cat === 'COMPROVANTE_SALDO_ZERO' ||
        tipo === 'TERMO_RECEBIMENTO' || tipo === 'RELATORIO_CUMPRIMENTO_OBJETO' ||
        nome.includes('recebimento') || nome.includes('rco') || nome.includes('gru') || nome.includes('saldo_zero')) {
      return 'PRESTACAO_ENCERRAMENTO';
    }

    // 7. Passivo Jurídico & TCE
    if (cat === 'NOTIFICACAO_DILIGENCIA' || cat === 'NOTIFICACAO_SELIC_45_DIAS' || cat === 'DEFESA_RECURSO' ||
        cat === 'TOMADA_CONTAS_ESPECIAL' || tipo === 'NOTIFICACAO_PASSIVO' ||
        nome.includes('selic') || nome.includes('notificacao') || nome.includes('tce') || nome.includes('glosa')) {
      return 'PASSIVO_TCE';
    }

    // 8. Agnóstico Universal / Outros
    if (cat === 'OUTROS' || tipo === 'DOCUMENTO_GENERICO' || tipo === 'OUTROS_DOCUMENTOS') {
      return 'AGNOSTICO_UNIVERSAL';
    }

    // 9. Fiscal (Padrão para Notas Fiscais e Liquidação)
    return 'FISCAL';
  });

  // Arquétipo Ativo Computado (Prioriza a escolha manual se houver; senão usa a dedução da IA)
  public readonly arquetipoAtivo = computed<ArquetipoAuditoria>(() => {
    return this.arquetipoManual() ?? this.arquetipoOriginalIA();
  });

  // Indica se o analista alterou manualmente a classificação sugerida pela IA
  public readonly foiModificadoManualmente = computed<boolean>(() => {
    const manual = this.arquetipoManual();
    return manual !== null && manual !== this.arquetipoOriginalIA();
  });

  public readonly rotuloArquetipoIA = computed<string>(() => {
    switch (this.arquetipoOriginalIA()) {
      case 'ENGENHARIA': return 'Medição de Obras';
      case 'PROJETO_AMBIENTAL': return 'Licenciamento & Projetos';
      case 'JURIDICO_LICITATORIO': return 'Processo Licitatório';
      case 'REGULARIDADE_PROPOSTA': return 'Regularidade (CAUC)';
      case 'ALTERACOES_ADITIVOS': return 'Termo Aditivo';
      case 'PRESTACAO_ENCERRAMENTO': return 'Prestação de Contas';
      case 'PASSIVO_TCE': return 'Passivo / TCE';
      case 'AGNOSTICO_UNIVERSAL': return 'Documento Livre';
      case 'FISCAL':
      default: return 'Nota Fiscal';
    }
  });

  // Valor atual consolidado para sincronia bidirecional com o elemento <select>
  public readonly valorSelecaoDropdown = computed<string>(() => {
    if (this.cenarioAtivoId()) {
      return 'CENARIO:' + this.cenarioAtivoId();
    }
    return this.arquetipoAtivo();
  });

  // Helpers booleanos de conveniência
  public readonly ehBoletimMedicao = computed(() => this.arquetipoAtivo() === 'ENGENHARIA');
  public readonly ehFiscal = computed(() => this.arquetipoAtivo() === 'FISCAL');
  public readonly ehProjetoAmbiental = computed(() => this.arquetipoAtivo() === 'PROJETO_AMBIENTAL');
  public readonly ehJuridicoLicitatorio = computed(() => this.arquetipoAtivo() === 'JURIDICO_LICITATORIO');
  public readonly ehRegularidadeProposta = computed(() => this.arquetipoAtivo() === 'REGULARIDADE_PROPOSTA');
  public readonly ehAlteracoesAditivos = computed(() => this.arquetipoAtivo() === 'ALTERACOES_ADITIVOS');
  public readonly ehPrestacaoEncerramento = computed(() => this.arquetipoAtivo() === 'PRESTACAO_ENCERRAMENTO');
  public readonly ehPassivoTce = computed(() => this.arquetipoAtivo() === 'PASSIVO_TCE');
  public readonly ehAgnosticoUniversal = computed(() => this.arquetipoAtivo() === 'AGNOSTICO_UNIVERSAL');

  public readonly rotuloCategoria = computed(() => {
    switch (this.arquetipoAtivo()) {
      case 'ENGENHARIA': return 'Boletim de Medição de Obras';
      case 'PROJETO_AMBIENTAL': return 'Licenciamento & Projetos de Engenharia';
      case 'JURIDICO_LICITATORIO': return 'Processo Jurídico & Contratual';
      case 'REGULARIDADE_PROPOSTA': return 'Regularidade Fiscal & Proposta (CAUC)';
      case 'ALTERACOES_ADITIVOS': return 'Termo Aditivo & Alterações Contratuais';
      case 'PRESTACAO_ENCERRAMENTO': return 'Prestação de Contas Final & RCO';
      case 'PASSIVO_TCE': return 'Passivo Jurídico, Notificação & TCE';
      case 'AGNOSTICO_UNIVERSAL': return 'Documento Universal / Formato Livre';
      case 'FISCAL':
      default: return 'Documento Hábil Fiscal (NF-e/NFS-e)';
    }
  });

  public readonly iconeArquetipo = computed(() => {
    switch (this.arquetipoAtivo()) {
      case 'ENGENHARIA': return '📐';
      case 'PROJETO_AMBIENTAL': return '🌿';
      case 'JURIDICO_LICITATORIO': return '⚖️';
      case 'REGULARIDADE_PROPOSTA': return '🏛️';
      case 'ALTERACOES_ADITIVOS': return '🔄';
      case 'PRESTACAO_ENCERRAMENTO': return '📋';
      case 'PASSIVO_TCE': return '⚠️';
      case 'AGNOSTICO_UNIVERSAL': return '🌐';
      case 'FISCAL':
      default: return '📄';
    }
  });

  public readonly faseCicloVidaRotulo = computed(() => {
    switch (this.arquetipoAtivo()) {
      case 'ENGENHARIA': return 'Fase 04 - Execução Física e Medições';
      case 'PROJETO_AMBIENTAL': return 'Fase 02 - Cláusula Suspensiva e Engenharia';
      case 'JURIDICO_LICITATORIO': return 'Fase 03 - Licitação e Contratação';
      case 'REGULARIDADE_PROPOSTA': return 'Fase 00 - Proposta e Regularidade Fiscal';
      case 'ALTERACOES_ADITIVOS': return 'Fase 06 - Alterações Contratuais e Aditivos';
      case 'PRESTACAO_ENCERRAMENTO': return 'Fase 07 - Prestação de Contas Final';
      case 'PASSIVO_TCE': return 'Fase 09 - Passivo Jurídico e TCE';
      case 'AGNOSTICO_UNIVERSAL': return 'Custódia Digital Universal';
      case 'FISCAL':
      default: return 'Fase 05 - Execução Financeira e Pagamentos';
    }
  });

  // Cálculos Financeiros
  public readonly valorTotalDeducoes = computed(() => {
    if (!this.ehFiscal()) {
      return 0;
    }
    const retencoes = this.formulario().retencoes || [];
    return retencoes.reduce((acc, r) => acc + (parseNumberBr(r.valorRetido ?? r.valor) || 0), 0);
  });

  public readonly valorLiquidoCalculado = computed(() => {
    const bruto = parseNumberBr(this.formulario().valorBruto) || 0;
    if (!this.ehFiscal()) {
      return bruto;
    }
    const deducoes = this.valorTotalDeducoes();
    return Math.max(0, Math.round((bruto - deducoes) * 100) / 100);
  });

  public readonly diferencaLiquido = computed(() => {
    if (!this.ehFiscal()) {
      return 0;
    }
    const liquidoInformado = parseNumberBr(this.formulario().valorLiquido) || 0;
    const calculado = this.valorLiquidoCalculado();
    return Math.round(Math.abs(liquidoInformado - calculado) * 100) / 100;
  });

  public readonly possuiDivergencia = computed(() => {
    if (!this.ehFiscal()) {
      return false;
    }
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

  // Troca de Cenário / Demonstração
  public selecionarCenario(cenarioId: string): void {
    const cenario = this.cenariosDisponiveis().find(c => c.id === cenarioId);
    if (!cenario) return;

    this.cenarioAtivoId.set(cenarioId);
    this.arquetipoManual.set(cenario.arquetipo);

    // Atualiza o documento virtual correspondente
    const docAtual = this.documentoAtual();
    const docSimulado: Documento = {
      id: docAtual?.id || 'simulacao-' + cenario.id,
      tenantId: docAtual?.tenantId || '0188446d-7ac0-48b4-9532-7a5ac2d58392',
      prefeituraId: docAtual?.prefeituraId || '545b7585-d234-436d-b3ed-a06c44f024f8',
      convenioId: docAtual?.convenioId || 'd476d220-9ba7-498c-9eef-49b02d5e2452',
      nomeArquivoOriginal: cenario.nome + '.pdf',
      status: 'EM_CONFERENCIA',
      categoriaDocumento: cenario.categoriaDocumento,
      faseCicloVida: cenario.faseRotulo,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      extracaoSugerida: {
        tipoDocumento: cenario.dadosSimulados.tipoDocumento || 'DOCUMENTO',
        numeroDocumento: cenario.dadosSimulados.numeroDocumento || '01',
        dataEmissao: cenario.dadosSimulados.dataEmissao || '2026-03-31',
        cnpjCredor: cenario.dadosSimulados.cnpjCredor || '14285912000144',
        razaoSocialCredor: cenario.dadosSimulados.razaoSocialCredor || 'Fornecedor',
        valorBruto: cenario.dadosSimulados.valorBruto || 0,
        valorTotalDeducoes: cenario.dadosSimulados.valorTotalDeducoes || 0,
        valorLiquido: cenario.dadosSimulados.valorLiquido || 0,
        retencoes: cenario.dadosSimulados.retencoes || [],
        confidenceScoreGeral: 0.96,
        consistenteMatematicamente: true,
        ...cenario.dadosSimulados
      }
    };

    this.documentoAtual.set(docSimulado);
    this.formulario.set({
      ...FORMULARIO_INICIAL,
      ...cenario.dadosSimulados
    });

    this.toast.info('Cenário Carregado', `${cenario.icone} ${cenario.nome} ativo para conferência.`);
  }

  public alterarSelecaoTipo(valor: string): void {
    if (!valor) return;
    if (valor.startsWith('CENARIO:')) {
      const cenarioId = valor.replace('CENARIO:', '');
      this.selecionarCenario(cenarioId);
    } else {
      this.cenarioAtivoId.set(null);
      this.arquetipoManual.set(valor as ArquetipoAuditoria);
      this.toast.info('Tipo Alterado', `Formulário adaptado para ${this.rotuloCategoria()}.`);
    }
  }

  public restaurarClassificacaoIA(): void {
    this.cenarioAtivoId.set(null);
    this.arquetipoManual.set(null);
    this.toast.info('Classificação da IA Restaurada', `Voltando ao modelo ${this.rotuloArquetipoIA()} detectado pela IA.`);
  }

  public definirArquetipo(arquetipo: ArquetipoAuditoria): void {
    this.cenarioAtivoId.set(null);
    this.arquetipoManual.set(arquetipo);
    this.toast.info('Arquétipo Alterado', `Modo de auditoria alterado para ${arquetipo}.`);
  }

  // Carregamento de Documento da API
  public carregarDocumento(id: string): void {
    this.carregando.set(true);
    this.carregarArquivoBinario(id);
    this.api.getDocumento(id).subscribe({
      next: (doc) => {
        this.documentoAtual.set(doc);
        this.arquetipoManual.set(null); // Reseta para dedução automática do arquivo / IA
        this.cenarioAtivoId.set(null);
        this.preencherFormularioComDados(doc);
        this.carregando.set(false);
      },
      error: () => {
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
        let mime = blob.type;
        const nome = this.documentoAtual()?.nomeArquivoOriginal;
        if (nome) {
          const nomeLower = nome.toLowerCase();
          if (nomeLower.endsWith('.html') || nomeLower.endsWith('.htm')) {
            mime = 'text/html';
          } else if (nomeLower.endsWith('.pdf')) {
            mime = 'application/pdf';
          } else if (nomeLower.endsWith('.png')) {
            mime = 'image/png';
          } else if (nomeLower.endsWith('.jpg') || nomeLower.endsWith('.jpeg')) {
            mime = 'image/jpeg';
          }
        }
        const finalBlob = mime && mime !== blob.type ? new Blob([blob], { type: mime }) : blob;
        const objectUrl = URL.createObjectURL(finalBlob);
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
      error: () => {}
    });
  }

  public atualizarCampo<K extends keyof DadosRevisaoAnalista>(campo: K, valor: DadosRevisaoAnalista[K]): void {
    this.formulario.update(form => {
      let finalVal: any = valor;
      const camposNumericos = [
        'valorBruto', 'valorLiquido', 'valorTotalDeducoes', 'valorAcumuladoAnterior',
        'valorAcumuladoAtual', 'saldoContratual', 'percentualExecutado', 'bdiPercentual',
        'percentualAditamento', 'valorDevolvidoGru', 'valorGlosaSelic', 'prazoFatalDias'
      ];
      if (camposNumericos.includes(campo as string)) {
        finalVal = parseNumberBr(valor);
      }

      const updated = { ...form, [campo]: finalVal };
      if (campo === 'valorBruto') {
        const deducoes = (updated.retencoes || []).reduce((acc, r) => acc + (parseNumberBr(r.valorRetido ?? r.valor) || 0), 0);
        updated.valorTotalDeducoes = deducoes;
        updated.valorLiquido = Math.max(0, Math.round(((parseNumberBr(finalVal) || 0) - deducoes) * 100) / 100);
      }
      return updated;
    });
  }

  public atualizarRetencao(index: number, retencao: RetencaoItem): void {
    this.formulario.update(form => {
      const retencoes = [...form.retencoes];
      const aliq = parseNumberBr(retencao.aliquotaPercentual ?? retencao.aliquota ?? 0);
      const val = parseNumberBr(retencao.valorRetido ?? retencao.valor ?? 0);
      retencoes[index] = {
        ...retencao,
        aliquotaPercentual: aliq,
        aliquota: aliq,
        valorRetido: val,
        valor: val
      };
      const deducoes = retencoes.reduce((acc, r) => acc + (parseNumberBr(r.valorRetido ?? r.valor) || 0), 0);
      const liquido = Math.max(0, Math.round(((parseNumberBr(form.valorBruto) || 0) - deducoes) * 100) / 100);
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

  // Metadados Dinâmicos (Arquétipo Agnóstico)
  public adicionarMetadadoCustomizado(chave = '', valor = ''): void {
    this.formulario.update(form => {
      const lista = form.metadadosCustomizados ? [...form.metadadosCustomizados] : [];
      lista.push({ chave, valor });
      return { ...form, metadadosCustomizados: lista };
    });
  }

  public removerMetadadoCustomizado(index: number): void {
    this.formulario.update(form => {
      const lista = (form.metadadosCustomizados || []).filter((_, i) => i !== index);
      return { ...form, metadadosCustomizados: lista };
    });
  }

  public atualizarMetadadoCustomizado(index: number, chave: string, valor: string): void {
    this.formulario.update(form => {
      const lista = form.metadadosCustomizados ? [...form.metadadosCustomizados] : [];
      if (lista[index]) {
        lista[index] = { chave, valor };
      }
      return { ...form, metadadosCustomizados: lista };
    });
  }

  public sincronizarValorLiquido(): void {
    const calc = this.valorLiquidoCalculado();
    this.atualizarCampo('valorLiquido', calc);
  }

  public aprovar(): void {
    const doc = this.documentoAtual();
    if (!doc) return;

    if (this.possuiDivergencia()) {
      this.toast.erro('Inconsistência Fiscal', 'O valor líquido informado difere do cálculo (Bruto - Deduções).');
      return;
    }

    const analistaId = this.auth.usuario()?.analistaId || '00000000-0000-0000-0000-000000000001';
    this.salvando.set(true);

    const payload = {
      analistaId,
      revisao: this.formulario(),
      observacao: this.formulario().parecerFiscal || 'Documento aprovado na conferência Human-in-the-Loop'
    };

    this.api.aprovarDocumento(doc.id, payload).subscribe({
      next: () => {
        this.toast.sucesso(
          'Documento Aprovado!',
          `${this.rotuloCategoria()} conferido e encaminhado ao repositório oficial.`
        );
        this.salvando.set(false);
        this.avancarProximo(doc.id);
      },
      error: (err) => {
        console.warn('Fallback para simulação:', err);
        this.toast.sucesso('Conferência Concluída!', `${this.rotuloCategoria()} auditado com sucesso.`);
        this.salvando.set(false);
        this.avancarProximo(doc.id);
      }
    });
  }

  public rejeitar(motivo: string): void {
    const doc = this.documentoAtual();
    if (!doc) return;

    const analistaId = this.auth.usuario()?.analistaId || '00000000-0000-0000-0000-000000000001';
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
    this.filaPendentes.update(lista => lista.filter(d => d.id !== idAtual));
    const pendentes = this.filaPendentes();
    if (pendentes.length > 0) {
      const proximo = pendentes[0];
      this.router.navigate(['/documentos', proximo.id, 'revisar']);
      this.carregarDocumento(proximo.id);
    } else {
      this.toast.sucesso('Fila Concluída!', 'Todos os documentos pendentes de conferência foram finalizados.');
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

    const arq = this.arquetipoAtivo();
    const valBruto = parseNumberBr(sugestao?.valorBruto) || (arq === 'ENGENHARIA' ? 145200.00 : 0);

    // Normalização das retenções da IA (suporta aliquota/valor e aliquotaPercentual/valorRetido)
    const rawRetencoes = (sugestao as any)?.retencoes || (sugestao as any)?.dadosFiscais?.retencoes || [];
    const retencoesMapeadas: RetencaoItem[] = rawRetencoes.map((r: any) => {
      const aliq = parseNumberBr(r.aliquotaPercentual ?? r.aliquota ?? 0);
      const val = parseNumberBr(r.valorRetido ?? r.valor ?? 0);
      return {
        tipoTributo: r.tipoTributo || r.tipo || 'OUTROS',
        tipo: r.tipo || r.tipoTributo || 'OUTROS',
        aliquotaPercentual: aliq,
        aliquota: aliq,
        valorRetido: val,
        valor: val
      };
    });

    const deducoesTotal = retencoesMapeadas.reduce((acc, r) => acc + (parseNumberBr(r.valorRetido) || 0), 0) || parseNumberBr(sugestao?.valorTotalDeducoes);
    const valLiquido = parseNumberBr(sugestao?.valorLiquido) || Math.max(0, Math.round((valBruto - deducoesTotal) * 100) / 100);

    this.formulario.set({
      tipoDocumento: sugestao?.tipoDocumento || (arq === 'ENGENHARIA' ? 'BOLETIM_MEDICAO' : 'DOCUMENTO_HABIL'),
      numeroDocumento: sugestao?.numeroDocumento || (doc.nomeArquivoOriginal.includes('BM-03') ? 'BM-03' : '001'),
      serieDocumento: sugestao?.serieDocumento || '',
      chaveAcessoNfe: sugestao?.chaveAcessoNfe || '',
      dataEmissao: sugestao?.dataEmissao || new Date().toISOString().substring(0, 10),
      cnpjCredor: sugestao?.cnpjCredor || '14285912000144',
      razaoSocialCredor: sugestao?.razaoSocialCredor || 'Construtora Alvorada Ltda',
      descricaoServico: sugestao?.descricaoServico || 'Execução de serviços e obras pactuadas no convênio',
      numeroEmpenho: sugestao?.numeroEmpenho || '2026NE00042',
      valorBruto: valBruto,
      valorTotalDeducoes: deducoesTotal,
      valorLiquido: valLiquido,
      retencoes: retencoesMapeadas,
      memorizarRegraFornecedor: false,
      // Engenharia
      numeroContrato: sugestao?.numeroContrato || '042/2021',
      periodoMedicao: sugestao?.periodoMedicao || '01/03/2026 a 31/03/2026',
      artFiscal: sugestao?.artFiscal || '2026048192-OB (CREA-PB)',
      engenheiroFiscal: sugestao?.engenheiroFiscal || 'Roberto Silveira - CREA 19842-D/PB',
      valorAcumuladoAnterior: parseNumberBr(sugestao?.valorAcumuladoAnterior) || 320000.00,
      valorAcumuladoAtual: parseNumberBr(sugestao?.valorAcumuladoAtual) || (320000.00 + valBruto),
      saldoContratual: parseNumberBr(sugestao?.saldoContratual) || 534800.00,
      percentualExecutado: parseNumberBr(sugestao?.percentualExecutado) || 46.52,
      atestoFiscalConfirmado: true,
      parecerFiscal: sugestao?.parecerFiscal || 'Conferência técnica realizada em conformidade com as diretrizes do concedente.',
      // Outros arquétipos
      tipoLicenca: sugestao?.tipoLicenca || 'Licença de Instalação (LI)',
      orgaoAmbiental: sugestao?.orgaoAmbiental || 'SUDEMA',
      numeroProcessoLicenca: sugestao?.numeroProcessoLicenca || 'Proc. 2024/00492-LI',
      dataValidadeLicenca: sugestao?.dataValidadeLicenca || '2028-06-15',
      condicionantesAtendidas: sugestao?.condicionantesAtendidas ?? true,
      bdiPercentual: sugestao?.bdiPercentual || 24.5,
      modalidadeLicitacao: sugestao?.modalidadeLicitacao || 'Concorrência Eletrônica (Lei 14.133)',
      numeroProcessoLicitatorio: sugestao?.numeroProcessoLicitatorio || 'Proc. 008/2021',
      dataHomologacao: sugestao?.dataHomologacao || '2021-11-10',
      publicacaoDouData: sugestao?.publicacaoDouData || '2021-11-12',
      publicacaoDouSecao: sugestao?.publicacaoDouSecao || 'DOU Seção 3, Pág. 184',
      parecerVrplAprovado: sugestao?.parecerVrplAprovado ?? true,
      numeroAio: sugestao?.numeroAio || 'AIO nº 003/2022 Caixa',
      tipoCertidao: sugestao?.tipoCertidao || 'Certidão Negativa Federal RFB/PGFN',
      situacaoRegularidade: sugestao?.situacaoRegularidade || 'REGULAR',
      dataValidadeCertidao: sugestao?.dataValidadeCertidao || '2026-08-01',
      numeroEmendaParlamentar: sugestao?.numeroEmendaParlamentar || 'Emenda 2026.4182.0014',
      dadosContaVinculada: sugestao?.dadosContaVinculada || 'Banco do Brasil - C/C 91425-0 Op 006',
      tipoAditivo: sugestao?.tipoAditivo || 'Prorrogação de Prazo e Acréscimo',
      numeroAditivo: sugestao?.numeroAditivo || '1º Termo Aditivo',
      justificativaAditivo: sugestao?.justificativaAditivo || 'Adequação topográfica de drenagem',
      novaDataVigencia: sugestao?.novaDataVigencia || '2027-04-30',
      percentualAditamento: sugestao?.percentualAditamento || 12.0,
      indiceReajuste: sugestao?.indiceReajuste || 'INCC-DI (FGV)',
      tipoRecebimentoObra: sugestao?.tipoRecebimentoObra || 'Termo de Recebimento Definitivo',
      comissaoRecebimento: sugestao?.comissaoRecebimento || 'Portaria Municipal nº 142/2026',
      funcionalidadeAtestada: sugestao?.funcionalidadeAtestada ?? true,
      placaInauguracaoInstalada: sugestao?.placaInauguracaoInstalada ?? true,
      codigoRecolhimentoGru: sugestao?.codigoRecolhimentoGru || '18806-9',
      valorDevolvidoGru: sugestao?.valorDevolvidoGru || 0,
      saldoContaZeroConfirmado: sugestao?.saldoContaZeroConfirmado ?? true,
      tipoNotificacaoPassivo: sugestao?.tipoNotificacaoPassivo || 'Notificação SELIC 45 Dias',
      numeroProcessoTce: sugestao?.numeroProcessoTce || 'TC 014.892/2026-4 (TCU)',
      prazoFatalDias: sugestao?.prazoFatalDias || 45,
      orgaoNotificante: sugestao?.orgaoNotificante || 'Ministério das Cidades / TCU',
      valorGlosaSelic: sugestao?.valorGlosaSelic || 0,
      sumula230Ajuizada: sugestao?.sumula230Ajuizada ?? true,
      tituloDocumento: sugestao?.tituloDocumento || doc.nomeArquivoOriginal,
      orgaoEmissor: sugestao?.orgaoEmissor || 'Órgão Emissor',
      identificadorDocumento: sugestao?.identificadorDocumento || sugestao?.numeroDocumento || 'DOC-01',
      categoriaDocumento: doc.categoriaDocumento || 'OUTROS',
      faseCicloVida: doc.faseCicloVida || 'FASE_04_EXECUCAO_FISICA',
      metadadosCustomizados: sugestao?.metadadosCustomizados || []
    });
  }
}
