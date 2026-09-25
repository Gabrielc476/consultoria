export type NivelRisco = 'CRITICO' | 'ATENCAO' | 'REGULAR';

export type TipoPrazo = 'CLAUSULA_SUSPENSIVA' | 'FIM_VIGENCIA' | 'PRESTACAO_CONTAS';

export interface AlertaConvenio {
  convenioId: string;
  nrConvenio: string;
  idProposta?: string;
  municipio: string;
  uf: string;
  cnpjProponente: string;
  nomeProponente: string;
  objeto?: string;
  situacaoConvenio?: string;
  nivelRisco: NivelRisco;
  tipoPrazoMaisProximo?: TipoPrazo;
  prazoMaisProximo?: string;
  diasRestantes?: number;
  dataFimVigencia?: string;
  diasFimVigencia?: number;
  dataSuspensiva?: string;
  diasSuspensiva?: number;
  dataLimitePrestacaoContas?: string;
  diasPrestacaoContas?: number;
  valorGlobal?: number;
  valorRepasse?: number;
}

export interface ResumoRadar {
  totalMonitorados: number;
  totalCriticos: number;
  totalAtencao: number;
  totalRegulares: number;
  dataReferencia: string;
}

export interface MunicipioRadar {
  municipio: string;
  uf: string;
  cnpjProponente: string;
  nomeProponente: string;
  totalConvenios: number;
  totalCriticos: number;
  totalAtencao: number;
  totalRegulares: number;
  maiorRisco: NivelRisco;
}

export interface RadarPrazosResponse {
  resumo: ResumoRadar;
  agrupamentoPorMunicipio: MunicipioRadar[];
  alertas: AlertaConvenio[];
}

export interface FiltrosRadarRequest {
  uf?: string;
  cnpj?: string;
  municipio?: string;
  nivelRisco?: NivelRisco;
  dataReferencia?: string;
}
