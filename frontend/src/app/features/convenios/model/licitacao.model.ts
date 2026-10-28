export type ModalidadeLicitacao =
  | 'PREGAO_ELETRONICO'
  | 'CONCORRENCIA_ELETRONICA'
  | 'CONCURSO'
  | 'LEILAO'
  | 'DIALOGO_COMPETITIVO'
  | 'DISPENSA'
  | 'INEXIGIBILIDADE';

export type CriterioJulgamento =
  | 'MENOR_PRECO'
  | 'MAIOR_DESCONTO'
  | 'MELHOR_TECNICA'
  | 'TECNICA_E_PRECO'
  | 'MAIOR_LANCE';

export type RegimeExecucao =
  | 'EMPREITADA_PRECO_GLOBAL'
  | 'EMPREITADA_PRECO_UNITARIO'
  | 'CONTRATACAO_INTEGRADA'
  | 'CONTRATACAO_SEMI_INTEGRADA'
  | 'FORNECIMENTO';

export type SituacaoLicitacao =
  | 'PLANEJAMENTO'
  | 'EDITAL_PUBLICADO'
  | 'IMPUGNADO'
  | 'EM_DISPUTA'
  | 'JULGAMENTO'
  | 'HOMOLOGADA'
  | 'DESERTA'
  | 'FRACASSADA'
  | 'REVOGADA'
  | 'ANULADA';

export type StatusVrpl =
  | 'NAO_ENVIADO'
  | 'EM_ANALISE_CAIXA'
  | 'DILIGENCIA'
  | 'ACEITO_HOMOLOGADO'
  | 'REJEITADO';

export type StatusAio = 'NAO_EMITIDO' | 'SOLICITADO' | 'EMITIDO';

export interface Licitacao {
  id: string;
  tenantId: string;
  convenioId: string;
  prefeituraId: string;
  numeroProcessoAdministrativo?: string;
  numeroLicitacao: string;
  modalidade: ModalidadeLicitacao;
  criterioJulgamento?: CriterioJulgamento;
  regimeExecucao?: RegimeExecucao;
  objeto: string;
  valorEstimado: number;
  valorHomologado?: number;
  percentualDesconto?: number;
  valorEconomia?: number;
  situacao: SituacaoLicitacao;
  dataPublicacaoEdital?: string;
  dataAberturaPropostas?: string;
  dataHomologacao?: string;
  linkPncp?: string;
  linkTransferegov?: string;
  linkSistemaCompras?: string;
  numeroVrplTransferegov?: string;
  statusVrpl: StatusVrpl;
  dataEnvioVrpl?: string;
  dataAceiteVrpl?: string;
  parecerTecnicoVrpl?: string;
  observacoesDiligenciaVrpl?: string;
  numeroAio?: string;
  dataEmissaoAio?: string;
  statusAio: StatusAio;
  cnpjVencedor?: string;
  razaoSocialVencedor?: string;
  s3KeyEdital?: string;
  s3KeyTermoHomologacao?: string;
  s3KeyPropostaVencedora?: string;
  s3KeyAtaSessao?: string;
  s3KeyParecerVrpl?: string;
  s3KeyAutorizacaoAio?: string;
  podeCriarMedicao: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CadastrarLicitacaoPayload {
  numeroProcessoAdministrativo?: string;
  numeroLicitacao: string;
  modalidade: ModalidadeLicitacao;
  criterioJulgamento?: CriterioJulgamento;
  regimeExecucao?: RegimeExecucao;
  objeto: string;
  valorEstimado: number;
  dataPublicacaoEdital?: string;
  dataAberturaPropostas?: string;
  linkPncp?: string;
}

export interface HomologarLicitacaoPayload {
  valorHomologado: number;
  dataHomologacao: string;
  cnpjVencedor: string;
  razaoSocialVencedor: string;
}

export interface SubmeterVrplPayload {
  numeroVrplTransferegov: string;
  dataEnvioVrpl: string;
}

export interface RegistrarResultadoVrplPayload {
  resultado: StatusVrpl;
  parecerTecnico?: string;
  observacoesDiligencia?: string;
  dataAceiteVrpl?: string;
}

export interface EmitirAioPayload {
  numeroAio: string;
  dataEmissaoAio: string;
}
