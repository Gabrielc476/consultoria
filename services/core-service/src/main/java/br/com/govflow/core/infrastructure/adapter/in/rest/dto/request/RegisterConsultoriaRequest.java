package br.com.govflow.core.infrastructure.adapter.in.rest.dto.request;

import br.com.govflow.core.domain.model.PlanoConsultoria;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterConsultoriaRequest(
        @NotBlank(message = "Razão Social é obrigatória.")
        @Size(max = 200, message = "Razão Social não pode ultrapassar 200 caracteres.")
        @Schema(description = "Razão Social da empresa de consultoria", example = "Planeja Brasil Consultoria Municipal Ltda")
        String razaoSocial,

        @Size(max = 150, message = "Nome Fantasia não pode ultrapassar 150 caracteres.")
        @Schema(description = "Nome Fantasia da consultoria", example = "Planeja Brasil")
        String nomeFantasia,

        @NotBlank(message = "CNPJ é obrigatório.")
        @Schema(description = "CNPJ da consultoria com ou sem máscara", example = "12.345.678/0001-90")
        String cnpj,

        @NotBlank(message = "E-mail do administrador é obrigatório.")
        @Email(message = "E-mail do administrador inválido.")
        @JsonAlias({"emailAnalista", "emailAdministrador"})
        @Schema(description = "E-mail de acesso do administrador da consultoria", example = "gestor@planejabrasil.com.br")
        String emailAdministrador,

        @NotBlank(message = "Senha é obrigatória.")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres.")
        @Schema(description = "Senha de acesso do administrador", example = "GovFlow2026!")
        String senha,

        @NotBlank(message = "Nome do administrador é obrigatório.")
        @Size(max = 100, message = "Nome do administrador não pode ultrapassar 100 caracteres.")
        @JsonAlias({"nomeAnalista", "nomeAdministrador"})
        @Schema(description = "Nome do administrador/gestor da conta da consultoria", example = "Carlos Eduardo Lima")
        String nomeAdministrador,

        @Schema(description = "Telefone celular para contato e WhatsApp", example = "(83) 98888-7777")
        @JsonAlias({"celularAdmin", "celular", "telefoneCelular", "telefoneContato"})
        String telefone,

        @Schema(description = "Plano contratado da consultoria", example = "PRO")
        PlanoConsultoria plano
) {
    @JsonIgnore
    public String celularAdmin() {
        return telefone;
    }

    @JsonIgnore
    public PlanoConsultoria getPlanoOrDefault() {
        return plano != null ? plano : PlanoConsultoria.PRO;
    }

    @JsonIgnore
    public String emailAnalista() {
        return emailAdministrador;
    }

    @JsonIgnore
    public String nomeAnalista() {
        return nomeAdministrador;
    }
}
