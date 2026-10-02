export interface BoundingBox {
  ymin: number;
  xmin: number;
  ymax: number;
  xmax: number;
}

export interface RetencaoItem {
  tipoTributo?: string;
  tipo?: string;
  aliquotaPercentual?: number | null;
  aliquota?: number | null;
  baseCalculo?: number | null;
  valorRetido?: number;
  valor?: number;
}

export interface MetadadoItem {
  chave: string;
  valor: string;
}

export type ArquetipoAuditoria =
  | 'FISCAL'                  // Fase 05: Documento Hábil, NF-e, NFS-e, Recibo, Retenções, OBTV, Extrato
  | 'ENGENHARIA'              // Fase 04: Boletim de Medição, Diário de Obra, Relatório Fotográfico, RAE Caixa
  | 'PROJETO_AMBIENTAL'       // Fase 02: Licença Ambiental (LP/LI/LO), Matrícula CRI, Projeto Básico, Planilha SINAPI, SPA Caixa
  | 'JURIDICO_LICITATORIO'    // Fases 01 e 03: Licitação, Homologação, Contrato Administrativo, Parecer VRPL, AIO, Termo Convênio, DOU
  | 'REGULARIDADE_PROPOSTA'   // Fases 00 e 01: Certidão CAUC/SIAFI, Proposta/Plano de Trabalho, Declarações, Conta Op 006
  | 'ALTERACOES_ADITIVOS'     // Fase 06: Termo Aditivo de Valor/Vigência, Apostilamento de Reajuste, Reprogramação
  | 'PRESTACAO_ENCERRAMENTO'  // Fases 07 e 08: Termo Recebimento, RCO, Placa de Inauguração, GRU Devolução, Saldo Zero
  | 'PASSIVO_TCE'             // Fase 09: Notificação SELIC 45 Dias, Notificação Glosa, Tomada de Contas Especial (TCE), Súmula 230 TCU
  | 'AGNOSTICO_UNIVERSAL';    // Categoria OUTROS ou qualquer novo tipo de documento / formato livre

export interface CenarioDemonstracao {
  id: string;
  nome: string;
  icone: string;
  faseRotulo: string;
  arquetipo: ArquetipoAuditoria;
  categoriaDocumento: string;
  descricao: string;
  dadosSimulados: Partial<DadosRevisaoAnalista>;
}

export interface ExtracaoSugerida {
  tipoDocumento: string;
  numeroDocumento: string;
  serieDocumento?: string | null;
  chaveAcessoNfe?: string | null;
  dataEmissao: string;
  cnpjCredor: string;
  razaoSocialCredor: string;
  descricaoServico?: string | null;
  numeroEmpenho?: string | null;
  valorBruto: number;
  valorTotalDeducoes: number;
  valorLiquido: number;
  retencoes: RetencaoItem[];
  confidenceScoreGeral: number;
  scoresConfiancaCampos?: Record<string, number>;
  consistenteMatematicamente: boolean;
  alertasInconsistencia?: string[];

  // 1. Campos de Engenharia & Medições
  numeroContrato?: string | null;
  periodoMedicao?: string | null;
  artFiscal?: string | null;
  engenheiroFiscal?: string | null;
  valorAcumuladoAnterior?: number | null;
  valorAcumuladoAtual?: number | null;
  saldoContratual?: number | null;
  percentualExecutado?: number | null;
  parecerFiscal?: string | null;

  // 2. Campos de Projetos & Licenciamento Ambiental
  tipoLicenca?: string | null;
  orgaoAmbiental?: string | null;
  numeroProcessoLicenca?: string | null;
  dataValidadeLicenca?: string | null;
  condicionantesAtendidas?: boolean | null;
  bdiPercentual?: number | null;

  // 3. Campos Jurídicos & Licitação
  modalidadeLicitacao?: string | null;
  numeroProcessoLicitatorio?: string | null;
  dataHomologacao?: string | null;
  publicacaoDouData?: string | null;
  publicacaoDouSecao?: string | null;
  parecerVrplAprovado?: boolean | null;
  numeroAio?: string | null;

  // 4. Campos de Regularidade, CAUC & Proposta
  tipoCertidao?: string | null;
  situacaoRegularidade?: string | null;
  dataValidadeCertidao?: string | null;
  numeroEmendaParlamentar?: string | null;
  dadosContaVinculada?: string | null;

  // 5. Campos de Aditivos & Reprogramação
  tipoAditivo?: string | null;
  numeroAditivo?: string | null;
  justificativaAditivo?: string | null;
  novaDataVigencia?: string | null;
  percentualAditamento?: number | null;
  indiceReajuste?: string | null;

  // 6. Campos de Prestação de Contas & Encerramento
  tipoRecebimentoObra?: string | null;
  comissaoRecebimento?: string | null;
  funcionalidadeAtestada?: boolean | null;
  placaInauguracaoInstalada?: boolean | null;
  codigoRecolhimentoGru?: string | null;
  valorDevolvidoGru?: number | null;
  saldoContaZeroConfirmado?: boolean | null;

  // 7. Campos de Passivo Jurídico & TCE
  tipoNotificacaoPassivo?: string | null;
  numeroProcessoTce?: string | null;
  prazoFatalDias?: number | null;
  orgaoNotificante?: string | null;
  valorGlosaSelic?: number | null;
  sumula230Ajuizada?: boolean | null;

  // 8. Campos Agnósticos e Dinâmicos
  categoriaDocumento?: string | null;
  faseCicloVida?: string | null;
  tituloDocumento?: string | null;
  orgaoEmissor?: string | null;
  identificadorDocumento?: string | null;
  metadadosCustomizados?: MetadadoItem[];
}

export interface DadosRevisaoAnalista {
  tipoDocumento: string;
  numeroDocumento: string;
  serieDocumento?: string | null;
  chaveAcessoNfe?: string | null;
  dataEmissao: string;
  cnpjCredor: string;
  razaoSocialCredor: string;
  descricaoServico?: string | null;
  numeroEmpenho?: string | null;
  valorBruto: number;
  valorTotalDeducoes: number;
  valorLiquido: number;
  retencoes: RetencaoItem[];
  memorizarRegraFornecedor?: boolean;

  // 1. Campos de Engenharia & Medições
  numeroContrato?: string | null;
  periodoMedicao?: string | null;
  artFiscal?: string | null;
  engenheiroFiscal?: string | null;
  valorAcumuladoAnterior?: number | null;
  valorAcumuladoAtual?: number | null;
  saldoContratual?: number | null;
  percentualExecutado?: number | null;
  atestoFiscalConfirmado?: boolean;
  parecerFiscal?: string | null;

  // 2. Campos de Projetos & Licenciamento Ambiental
  tipoLicenca?: string | null;
  orgaoAmbiental?: string | null;
  numeroProcessoLicenca?: string | null;
  dataValidadeLicenca?: string | null;
  condicionantesAtendidas?: boolean | null;
  bdiPercentual?: number | null;

  // 3. Campos Jurídicos & Licitação
  modalidadeLicitacao?: string | null;
  numeroProcessoLicitatorio?: string | null;
  dataHomologacao?: string | null;
  publicacaoDouData?: string | null;
  publicacaoDouSecao?: string | null;
  parecerVrplAprovado?: boolean | null;
  numeroAio?: string | null;

  // 4. Campos de Regularidade, CAUC & Proposta
  tipoCertidao?: string | null;
  situacaoRegularidade?: string | null;
  dataValidadeCertidao?: string | null;
  numeroEmendaParlamentar?: string | null;
  dadosContaVinculada?: string | null;

  // 5. Campos de Aditivos & Reprogramação
  tipoAditivo?: string | null;
  numeroAditivo?: string | null;
  justificativaAditivo?: string | null;
  novaDataVigencia?: string | null;
  percentualAditamento?: number | null;
  indiceReajuste?: string | null;

  // 6. Campos de Prestação de Contas & Encerramento
  tipoRecebimentoObra?: string | null;
  comissaoRecebimento?: string | null;
  funcionalidadeAtestada?: boolean | null;
  placaInauguracaoInstalada?: boolean | null;
  codigoRecolhimentoGru?: string | null;
  valorDevolvidoGru?: number | null;
  saldoContaZeroConfirmado?: boolean | null;

  // 7. Campos de Passivo Jurídico & TCE
  tipoNotificacaoPassivo?: string | null;
  numeroProcessoTce?: string | null;
  prazoFatalDias?: number | null;
  orgaoNotificante?: string | null;
  valorGlosaSelic?: number | null;
  sumula230Ajuizada?: boolean | null;

  // 8. Campos Agnósticos e Dinâmicos
  categoriaDocumento?: string | null;
  faseCicloVida?: string | null;
  tituloDocumento?: string | null;
  orgaoEmissor?: string | null;
  identificadorDocumento?: string | null;
  metadadosCustomizados?: MetadadoItem[];
}

export interface Documento {
  id: string;
  tenantId: string;
  prefeituraId?: string | null;
  convenioId?: string | null;
  s3Bucket?: string | null;
  s3Key?: string | null;
  nomeArquivoOriginal: string;
  contentType?: string | null;
  tamanhoBytes?: number | null;
  status: string;
  categoriaDocumento?: string | null;
  faseCicloVida?: string | null;
  extracaoSugerida?: ExtracaoSugerida | null;
  boundingBoxes?: Record<string, BoundingBox> | null;
  dadosRevisao?: DadosRevisaoAnalista | null;
  motivoRejeicao?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface DocumentoResumo {
  id: string;
  nomeArquivoOriginal: string;
  status: string;
  createdAt: string;
  valorBruto?: number | null;
  numeroDocumento?: string | null;
  credor?: string | null;
}

export interface PageResponse<T> {
  content?: T[];
  items?: T[];
  pageNumber?: number;
  pageSize?: number;
  page?: number;
  size?: number;
  totalElements: number;
  totalPages: number;
}

export interface AprovarDocumentoPayload {
  analistaId: string;
  revisao: DadosRevisaoAnalista;
  observacao?: string | null;
}

export interface RejeitarDocumentoPayload {
  analistaId: string;
  motivo: string;
}
