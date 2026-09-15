package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.PlanoConsultoria;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarConsultoriaRequest(
        @NotBlank(message = "CNPJ é obrigatório.")
        String cnpj,

        @NotBlank(message = "Razão Social é obrigatória.")
        @Size(max = 200, message = "Razão Social não pode ultrapassar 200 caracteres.")
        String razaoSocial,

        @NotBlank(message = "Nome Fantasia é obrigatório.")
        @Size(max = 150, message = "Nome Fantasia não pode ultrapassar 150 caracteres.")
        String nomeFantasia,

        @NotBlank(message = "E-mail de contato é obrigatório.")
        @Email(message = "E-mail de contato deve ser válido.")
        String emailContato,

        String telefoneContato,

        PlanoConsultoria plano
) {}
