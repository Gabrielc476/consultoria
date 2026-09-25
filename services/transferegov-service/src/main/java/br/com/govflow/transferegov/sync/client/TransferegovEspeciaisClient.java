package br.com.govflow.transferegov.sync.client;

import br.com.govflow.transferegov.sync.client.dto.BeneficiarioEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.PlanoAcaoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.PlanoTrabalhoEspecialDTO;
import br.com.govflow.transferegov.sync.client.dto.RelatorioGestaoEspecialDTO;

import java.util.List;

public interface TransferegovEspeciaisClient {

    List<BeneficiarioEspecialDTO> consultarBeneficiarios(String uf, String cnpj);

    List<PlanoAcaoEspecialDTO> consultarPlanosAcao(Long idBeneficiario, Integer ano);

    List<PlanoTrabalhoEspecialDTO> consultarPlanosTrabalho(Long idPlanoAcao);

    List<RelatorioGestaoEspecialDTO> consultarRelatoriosGestao(Long idPlanoAcao);
}
