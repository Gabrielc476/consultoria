package br.com.govflow.core.application.port.in;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase.UsuarioAutenticado;
import br.com.govflow.core.domain.model.PlanoConsultoria;

public interface CadastrarConsultoriaComAdminUseCase {

    record CadastrarConsultoriaComAdminCommand(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String emailContato,
            String telefoneContato,
            PlanoConsultoria plano,
            String nomeAdmin,
            String emailAdmin,
            String senhaAdmin,
            String celularAdmin
    ) {
    }

    UsuarioAutenticado cadastrar(CadastrarConsultoriaComAdminCommand command);
}
