export interface DocumentoFicheiro {
  id: string;
  convenioId?: string;
  prefeituraId?: string;
  faseCicloVida: string;
  categoriaDocumento: string;
  categoriaDescricao?: string;
  pastaVirtual: string;
  nomeArquivoOriginal: string;
  contentType: string;
  tamanhoBytes: number;
  tamanhoFormatado?: string;
  hashSha256?: string;
  status: string;
  origemCanal: string;
  tags: string[];
  metadados?: Record<string, any>;
  criadoPorUsuarioId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PastaFase {
  fase: string;
  codigoFase: string;
  nomePasta: string;
  descricao: string;
  quantidadeArquivos: number;
  tamanhoTotalBytes: number;
  tamanhoTotalFormatado?: string;
  documentos: DocumentoFicheiro[];
}

export interface FicheiroDigital {
  convenioId: string;
  prefeituraId: string;
  tenantId: string;
  numeroSiconv: string;
  objeto: string;
  totalArquivos: number;
  tamanhoTotalBytes: number;
  tamanhoTotalFormatado?: string;
  fases: PastaFase[];
}

export interface PreviewDocumento {
  documentoId: string;
  url: string;
  expiraEmMinutos: number;
}

export interface MoverDocumentoPayload {
  novaFase: string;
  novaPastaVirtual?: string;
  justificativa: string;
}

export interface ExcluirDocumentoPayload {
  justificativa: string;
}

export interface FaseMeta {
  codigo: string;
  numero: string;
  nome: string;
  nomePasta: string;
  descricao: string;
  icone: string;
  corBadge: string;
}

export const FASES_CICLO_VIDA_CONFIG: FaseMeta[] = [
  {
    codigo: 'FASE_00_PROPOSTA',
    numero: '00',
    nome: 'Proposta e Plano de Trabalho',
    nomePasta: '00_Proposta_e_Plano_de_Trabalho',
    descricao: 'Manifestação de interesse, planos de trabalho e documentação preliminar de emendas',
    icone: 'document-text',
    corBadge: 'bg-slate-100 text-slate-700 border-slate-300'
  },
  {
    codigo: 'FASE_01_CELEBRACAO',
    numero: '01',
    nome: 'Celebração e Formalização',
    nomePasta: '01_Celebracao_e_Formalizacao',
    descricao: 'Termo de convênio assinado, publicação no DOU e abertura da conta vinculada',
    icone: 'sparkles',
    corBadge: 'bg-blue-100 text-blue-700 border-blue-300'
  },
  {
    codigo: 'FASE_02_CLAUSULA_SUSPENSIVA',
    numero: '02',
    nome: 'Cláusula Suspensiva e Engenharia',
    nomePasta: '02_Clausula_Suspensiva_e_Engenharia',
    descricao: 'Projetos de engenharia, titularidade dominial, licenças ambientais e laudos Mandatária',
    icone: 'clock',
    corBadge: 'bg-amber-100 text-amber-700 border-amber-300'
  },
  {
    codigo: 'FASE_03_LICITACAO',
    numero: '03',
    nome: 'Licitação e Contratação',
    nomePasta: '03_Licitacao_e_Contratacao',
    descricao: 'Edital, atas de sessão, homologação, contrato administrativo e emissão do AIO',
    icone: 'scale',
    corBadge: 'bg-indigo-100 text-indigo-700 border-indigo-300'
  },
  {
    codigo: 'FASE_04_EXECUCAO_FISICA',
    numero: '04',
    nome: 'Execução Física e Medições',
    nomePasta: '04_Execucao_Fisica_e_Medicoes',
    descricao: 'Boletins de medição da prefeitura, diários de obra e RAE da Caixa',
    icone: 'wrench-screwdriver',
    corBadge: 'bg-cyan-100 text-cyan-700 border-cyan-300'
  },
  {
    codigo: 'FASE_05_EXECUCAO_FINANCEIRA',
    numero: '05',
    nome: 'Execução Financeira e Pagamentos',
    nomePasta: '05_Execucao_Financeira_e_Pagamentos',
    descricao: 'Documentos hábeis (NF-e, recibos), comprovantes de liquidação OBTV e extratos',
    icone: 'currency-dollar',
    corBadge: 'bg-emerald-100 text-emerald-700 border-emerald-300'
  },
  {
    codigo: 'FASE_06_ALTERACOES_CONTRATUAIS',
    numero: '06',
    nome: 'Alterações Contratuais e Aditivos',
    nomePasta: '06_Alteracoes_Contratuais_e_Aditivos',
    descricao: 'Termos aditivos de vigência/valor, apostilamentos e pareceres de reprogramação',
    icone: 'arrow-path',
    corBadge: 'bg-purple-100 text-purple-700 border-purple-300'
  },
  {
    codigo: 'FASE_07_PRESTACAO_CONTAS',
    numero: '07',
    nome: 'Prestação de Contas Final (RCO)',
    nomePasta: '07_Prestacao_Contas_Final_RCO',
    descricao: 'Termo de recebimento definitivo, relatório de cumprimento do objeto e fotos da placa',
    icone: 'check-circle',
    corBadge: 'bg-teal-100 text-teal-700 border-teal-300'
  },
  {
    codigo: 'FASE_08_ENCERRAMENTO_FINANCEIRO',
    numero: '08',
    nome: 'Encerramento e Devolução de Saldo',
    nomePasta: '08_Encerramento_e_Devolucao_Saldo',
    descricao: 'GRU de recolhimento de saldo remanescente da União e comprovante saldo zero',
    icone: 'archive-box-x-mark',
    corBadge: 'bg-orange-100 text-orange-700 border-orange-300'
  },
  {
    codigo: 'FASE_09_PASSIVO_JURIDICO',
    numero: '09',
    nome: 'Passivo Jurídico e Tomada de Contas',
    nomePasta: '09_Passivo_Juridico_e_TCE',
    descricao: 'Notificações de glosa, defesas técnicas e registros de Tomada de Contas Especial (TCE)',
    icone: 'shield-exclamation',
    corBadge: 'bg-rose-100 text-rose-700 border-rose-300'
  }
];

export const CATEGORIAS_POR_FASE: Record<string, { valor: string; rotulo: string }[]> = {
  FASE_00_PROPOSTA: [
    { valor: 'CERTIDAO_CAUC', rotulo: 'Certidão CAUC / Regularidade Fiscal' },
    { valor: 'PROPOSTA_PLANO_TRABALHO', rotulo: 'Proposta Formal e Plano de Trabalho' },
    { valor: 'PARECER_TECNICO_EMENDA', rotulo: 'Parecer Técnico de Emenda / SIOP' },
    { valor: 'DOSSIE_DECLARACOES', rotulo: 'Dossiê de Declarações Governamentais' }
  ],
  FASE_01_CELEBRACAO: [
    { valor: 'TERMO_CONVENIO', rotulo: 'Termo de Convênio / Contrato de Repasse' },
    { valor: 'PUBLICACAO_DOU', rotulo: 'Publicação no Diário Oficial da União' },
    { valor: 'NOTIFICACAO_CONTA_VINCULADA', rotulo: 'Notificação de Abertura de Conta Op 006' }
  ],
  FASE_02_CLAUSULA_SUSPENSIVA: [
    { valor: 'PROJETO_ENGENHARIA', rotulo: 'Projeto Básico / Executivo de Engenharia' },
    { valor: 'PLANILHA_ORCAMENTARIA', rotulo: 'Planilha Orçamentária SINAPI / Curva ABC' },
    { valor: 'LICENCA_AMBIENTAL', rotulo: 'Licença Ambiental (LP/LI/LO)' },
    { valor: 'TITULARIDADE_IMOVEL', rotulo: 'Titularidade do Imóvel / Desapropriação' },
    { valor: 'SPA_LAE_CAIXA', rotulo: 'Síntese do Projeto Aprovado (SPA/LAE) Caixa' }
  ],
  FASE_03_LICITACAO: [
    { valor: 'LICITACAO', rotulo: 'Edital de Licitação e Parecer Jurídico' },
    { valor: 'ATA_HOMOLOGACAO_CERTAME', rotulo: 'Atas de Sessão e Homologação' },
    { valor: 'CONTRATO_ADMINISTRATIVO', rotulo: 'Contrato Administrativo de Execução' },
    { valor: 'PARECER_VRPL', rotulo: 'Parecer VRPL Mandatária' },
    { valor: 'AUTORIZACAO_INICIO_OBJETO', rotulo: 'Autorização de Início de Objeto (AIO)' }
  ],
  FASE_04_EXECUCAO_FISICA: [
    { valor: 'BOLETIM_MEDICAO', rotulo: 'Boletim de Medição de Obras' },
    { valor: 'RELATORIO_FOTOGRAFICO', rotulo: 'Relatório Fotográfico Georreferenciado' },
    { valor: 'DIARIO_OBRA', rotulo: 'Diário de Obra' },
    { valor: 'RAE_CAIXA', rotulo: 'Relatório de Acompanhamento (RAE) Caixa' }
  ],
  FASE_05_EXECUCAO_FINANCEIRA: [
    { valor: 'DOCUMENTO_HABIL', rotulo: 'Documento Hábil Fiscal (NF-e, NFS-e, Recibo)' },
    { valor: 'RETENCAO_TRIBUTARIA', rotulo: 'Guia de Retenções Tributárias (DARF, DAM, GPS)' },
    { valor: 'ORDEM_BANCARIA_OBTV', rotulo: 'Comprovante de Liquidação / OBTV' },
    { valor: 'EXTRATO_BANCARIO', rotulo: 'Extrato Bancário da Conta Op 006' }
  ],
  FASE_06_ALTERACOES_CONTRATUAIS: [
    { valor: 'TERMO_ADITIVO', rotulo: 'Termo Aditivo de Vigência / Valor' },
    { valor: 'APOSTILAMENTO', rotulo: 'Apostilamento de Reajuste (INCC/IPCA)' },
    { valor: 'PARECER_REPROGRAMACAO', rotulo: 'Parecer de Reprogramação da Mandatária' }
  ],
  FASE_07_PRESTACAO_CONTAS: [
    { valor: 'TERMO_RECEBIMENTO', rotulo: 'Termo de Recebimento Provisório / Definitivo' },
    { valor: 'RELATORIO_CUMPRIMENTO_OBJETO', rotulo: 'Relatório de Cumprimento do Objeto (RCO)' },
    { valor: 'PLACA_INAUGURACAO', rotulo: 'Registro Fotográfico da Placa de Inauguração' }
  ],
  FASE_08_ENCERRAMENTO_FINANCEIRO: [
    { valor: 'GUIA_RECOLHIMENTO_UNIAO', rotulo: 'GRU de Recolhimento de Saldos' },
    { valor: 'COMPROVANTE_SALDO_ZERO', rotulo: 'Comprovante Bancário Saldo Zero (Op 006)' }
  ],
  FASE_09_PASSIVO_JURIDICO: [
    { valor: 'NOTIFICACAO_GLOSA', rotulo: 'Notificação de Glosa / Irregularidade' },
    { valor: 'DEFESA_TECNICA', rotulo: 'Defesa Técnica / Recurso Administrativo' },
    { valor: 'REGISTRO_TCE', rotulo: 'Registro de Tomada de Contas Especial (TCE)' }
  ]
};
