export type StatusCertidao = 'REGULAR' | 'ALERTA' | 'VENCIDA';

export interface CertidaoCaucItem {
  codigo: string; // Ex: '1.1', '1.2'
  grupo: 'TRIBUTOS_FGTS' | 'PRESTACAO_CONTAS' | 'SICONFI_FISCAL' | 'LIMITES_CONSTITUCIONAIS';
  nome: string;
  orgaoEmissor: string;
  status: StatusCertidao;
  dataValidade: string;
  diasParaVencer: number;
}

export interface MunicipioRiscoCauc {
  id: string;
  nome: string;
  uf: string;
  conveniosAtivos: number;
  certidoesRegulares: number;
  certidoesAlerta: number;
  certidoesVencidas: number;
  proximoPrazoFatal: {
    descricao: string;
    diasRestantes: number;
    dataLimite: string;
    tipo: 'SUSPENSIVA' | 'RCO' | 'SELIC' | 'ADITIVO';
  };
  certidoes: CertidaoCaucItem[];
}
