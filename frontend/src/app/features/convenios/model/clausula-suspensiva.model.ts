export type TipoCondicionante =
  | 'ENGENHARIA_PROJETOS_SINAPI'
  | 'LICENCIAMENTO_AMBIENTAL'
  | 'TITULARIDADE_IMOVEL';

export type StatusCondicionante =
  | 'PENDENTE'
  | 'EM_ANALISE_CAIXA'
  | 'DILIGENCIA_EMITIDA'
  | 'APROVADO';

export type CriticidadePrazo = 'REGULAR' | 'ATENCAO' | 'CRITICO' | 'EXPIRADO';

export interface CondicionanteItem {
  id: string;
  tipo: TipoCondicionante;
  descricaoTipo: string;
  status: StatusCondicionante;
  statusDescricao: string;
  numeroDocumentoComprobatorio?: string;
  dataAprovacao?: string;
  dataValidade?: string;
  observacoesAnaliseCaixa?: string;
  s3KeyDocumento?: string;
  dataLimiteSaneamento?: string;
  s3KeyLaudoPendencias?: string;
  valorOrcamentoAprovadoCaixa?: number;
  percentualBdiAprovado?: number;
  numeroArtRrt?: string;
  orgaoEmissor?: string;
}

export interface DossieClausulaSuspensiva {
  convenioId: string;
  prefeituraId?: string;
  numeroSiconv: string;
  numeroProcesso?: string;
  orgaoConcedente: string;
  objeto: string;
  valorGlobal: number;
  valorRepasse: number;
  valorContrapartida: number;
  possuiClausulaSuspensiva: boolean;
  prazoOriginal?: string;
  prorrogacaoSolicitada: boolean;
  novoPrazoProrrogado?: string;
  prazoFatalEfetivo?: string;
  diasRestantes: number;
  criticidade: CriticidadePrazo;
  criticidadeDescricao: string;
  superada: boolean;
  s3KeyTermoRetirada?: string;
  condicionantes: CondicionanteItem[];
}

export interface RegistrarDiligenciaPayload {
  observacoes: string;
  s3KeyLaudoPendencias?: string;
  dataLimiteSaneamento: string;
}

export interface AprovarCondicionantePayload {
  numeroDocumentoComprobatorio: string;
  dataAprovacao?: string;
  dataValidade?: string;
  valorOrcamentoAprovado?: number;
  percentualBdiAprovado?: number;
  numeroArtRrt?: string;
  orgaoEmissor?: string;
  s3KeyDocumento?: string;
}

export interface AtualizarCondicionantePayload {
  numeroDocumentoComprobatorio?: string;
  dataValidade?: string;
  orgaoEmissor?: string;
  valorOrcamentoAprovado?: number;
  percentualBdiAprovado?: number;
  numeroArtRrt?: string;
  observacoes?: string;
}

export interface SolicitarProrrogacaoPayload {
  novoPrazoProrrogado: string;
}

export interface SuperarClausulaPayload {
  s3KeyTermoRetirada: string;
}
