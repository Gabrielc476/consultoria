export type StatusFase = 'CONCLUIDA' | 'EM_ANDAMENTO' | 'PENDENTE' | 'BLOQUEADA';

export interface FaseConvenio {
  numero: number;
  codigo: string;
  nome: string;
  subtitulo: string;
  status: StatusFase;
  prazoFatal?: string;
  responsavel?: string;
  descricao: string;
}

export interface ConvenioCockpit {
  id: string;
  numeroSiconv: string;
  ano: number;
  objeto: string;
  municipioId?: string;
  municipioNome: string;
  municipioUf: string;
  orgaoConcedente: string;
  valorTotal: number;
  valorRepasse: number;
  valorContrapartida: number;
  percentualExecucao: number;
  saldoContaOp006: number;
  diasParaVencimento: number;
  dataFimVigencia: string;
  faseAtualNumero: number;
  statusGeral?: 'EM_DIA' | 'ALERTA' | 'CRITICO';
  fases: FaseConvenio[];
}
