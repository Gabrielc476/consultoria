export type SituacaoCauc = 'REGULAR' | 'ALERTA' | 'BLOQUEADO';

export interface Municipio {
  id: string;
  nome: string;
  uf: string;
  codigoIbge: string;
  cnpj: string;
  conveniosAtivos: number;
  prazosCriticos: number;
  situacaoCauc: SituacaoCauc;
}
