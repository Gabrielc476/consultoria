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

export interface ResumoCauc {
  totalMunicipios: number;
  totalRegulares: number;
  totalAlerta: number;
  totalVencidas: number;
  municipios: MunicipioRiscoCauc[];
}

export interface DossieCauc {
  prefeituraId: string;
  nomeMunicipio: string;
  uf: string;
  cnpj: string;
  statusGeral: 'ADIMPLENTE' | 'BLOQUEADO';
  certidoesRegulares: number;
  certidoesAlerta: number;
  certidoesVencidas: number;
  certidoes: CertidaoCaucItem[];
}

