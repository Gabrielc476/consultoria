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
