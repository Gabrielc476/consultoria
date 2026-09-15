package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.Consultoria;
import br.com.govflow.core.domain.model.PlanoConsultoria;

public interface CadastrarConsultoriaUseCase {

    Consultoria cadastrar(CadastrarConsultoriaCommand command);

    record CadastrarConsultoriaCommand(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String emailContato,
            String telefoneContato,
            PlanoConsultoria plano
    ) {}
}
