package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "E-mail do analista ou usuário da consultoria", example = "analista@govflow.com.br")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "E-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "govflow123")
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
