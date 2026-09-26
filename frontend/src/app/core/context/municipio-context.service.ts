import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Municipio, SituacaoCauc } from './municipio.model';
import { API_ENDPOINTS } from '../api/api-endpoints';

/**
 * =========================================================================
 * 📌 MAPEAMENTO DE PLACEHOLDER - TASK-FE-02
 * -------------------------------------------------------------------------
 * O QUE É MOCK:
 * - A lista inicial MOCK_MUNICIPIOS abaixo simula prefeituras atendidas
 *   pela consultoria enquanto o backend não estiver ativo ou populado.
 *
 * O QUE SUBSTITUI NO FUTURO:
 * - O endpoint GET /api/v1/prefeituras (Task-03 do clickup_tasks_backlog.md)
 *   populado com as prefeituras reais cadastradas para o tenant da consultoria.
 * =========================================================================
 */
const MOCK_MUNICIPIOS: Municipio[] = [
  {
    id: 'mun-patos-01',
    nome: 'Patos',
    uf: 'PB',
    codigoIbge: '2510808',
    cnpj: '09.288.665/0001-38',
    conveniosAtivos: 5,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-sousa-02',
    nome: 'Sousa',
    uf: 'PB',
    codigoIbge: '2516201',
    cnpj: '08.924.032/0001-92',
    conveniosAtivos: 3,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-pombal-03',
    nome: 'Pombal',
    uf: 'PB',
    codigoIbge: '2512101',
    cnpj: '08.924.032/0001-90',
    conveniosAtivos: 3,
    prazosCriticos: 0,
    situacaoCauc: 'ALERTA'
  },
  {
    id: 'mun-monteiro-04',
    nome: 'Monteiro',
    uf: 'PB',
    codigoIbge: '2509701',
    cnpj: '09.073.628/0001-91',
    conveniosAtivos: 3,
    prazosCriticos: 1,
    situacaoCauc: 'REGULAR'
  },
  {
    id: 'mun-cajazeiras-05',
    nome: 'Cajazeiras',
    uf: 'PB',
    codigoIbge: '2503704',
    cnpj: '08.923.976/0001-04',
    conveniosAtivos: 3,
    prazosCriticos: 0,
    situacaoCauc: 'REGULAR'
  }
];

const STORAGE_KEY_MUNICIPIO_ATIVO = 'govflow_municipio_ativo_id';

@Injectable({
  providedIn: 'root'
})
export class MunicipioContextService {
  private readonly http = inject(HttpClient);

  readonly municipios = signal<Municipio[]>(MOCK_MUNICIPIOS);
  readonly municipioAtivoId = signal<string | null>(this.obterIdSalvo());

  readonly municipioAtivo = computed(() => {
    const id = this.municipioAtivoId();
    if (!id) return this.municipios()[0] || null;
    return this.municipios().find(m => m.id === id) || this.municipios()[0] || null;
  });

  readonly nomeMunicipioAtivoFormatado = computed(() => {
    const mun = this.municipioAtivo();
    return mun ? `Prefeitura de ${mun.nome} - ${mun.uf}` : 'Todas as Prefeituras';
  });

  constructor() {
    this.carregarMunicipios();
  }

  carregarMunicipios(): void {
    this.http.get<any[]>(API_ENDPOINTS.PREFEITURAS.BASE).subscribe({
      next: (dados) => {
        if (dados && Array.isArray(dados) && dados.length > 0) {
          const mapeados: Municipio[] = dados.map(item => ({
            id: item.id || item.codigoIbge,
            nome: item.nomeMunicipio || item.nome || item.razaoSocial,
            uf: item.uf || 'PB',
            codigoIbge: item.codigoIbge || '',
            cnpj: item.cnpj || '',
            conveniosAtivos: item.conveniosAtivos ?? 3,
            prazosCriticos: item.prazosCriticos ?? 0,
            situacaoCauc: (item.statusCauc as SituacaoCauc) || (item.situacaoCauc as SituacaoCauc) || 'REGULAR'
          }));
          this.municipios.set(mapeados);
        }
      },
      error: () => {
        // Fallback resiliente: Mantém MOCK_MUNICIPIOS
      }
    });
  }

  selecionarMunicipio(id: string | null): void {
    this.municipioAtivoId.set(id);
    if (id) {
      localStorage.setItem(STORAGE_KEY_MUNICIPIO_ATIVO, id);
    } else {
      localStorage.removeItem(STORAGE_KEY_MUNICIPIO_ATIVO);
    }
  }

  private obterIdSalvo(): string | null {
    try {
      return localStorage.getItem(STORAGE_KEY_MUNICIPIO_ATIVO) || 'mun-patos-01';
    } catch {
      return 'mun-patos-01';
    }
  }
}
