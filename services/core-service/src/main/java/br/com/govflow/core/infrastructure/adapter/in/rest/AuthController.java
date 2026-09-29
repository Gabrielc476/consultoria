package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AutenticarUsuarioUseCase;
import br.com.govflow.core.application.port.in.CadastrarConsultoriaComAdminUseCase;
import br.com.govflow.core.application.port.in.ObterUsuarioAutenticadoUseCase;
import br.com.govflow.core.domain.exception.CredenciaisInvalidasException;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.LoginRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RegisterConsultoriaRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.LoginResponse;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import br.com.govflow.core.infrastructure.interceptor.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Endpoints de autenticação segura com BCrypt e emissão de tokens JWT")
public class AuthController {

    private final AutenticarUsuarioUseCase autenticarUsuarioUseCase;
    private final CadastrarConsultoriaComAdminUseCase cadastrarConsultoriaComAdminUseCase;
    private final ObterUsuarioAutenticadoUseCase obterUsuarioAutenticadoUseCase;

    public AuthController(
            AutenticarUsuarioUseCase autenticarUsuarioUseCase,
            CadastrarConsultoriaComAdminUseCase cadastrarConsultoriaComAdminUseCase,
            ObterUsuarioAutenticadoUseCase obterUsuarioAutenticadoUseCase) {
        this.autenticarUsuarioUseCase = autenticarUsuarioUseCase;
        this.cadastrarConsultoriaComAdminUseCase = cadastrarConsultoriaComAdminUseCase;
        this.obterUsuarioAutenticadoUseCase = obterUsuarioAutenticadoUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar Login", description = "Autentica o usuário com conferência segura de senha via BCrypt e retorna token JWT")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var command = new AutenticarUsuarioUseCase.AutenticarUsuarioCommand(request.email(), request.senha());
        var auth = autenticarUsuarioUseCase.autenticar(command);
        return ResponseEntity.ok(toResponse(auth));
    }

    @PostMapping({"/register", "/register-consultoria"})
    @Operation(summary = "Cadastrar Consultoria (Tenant) & Administrador", description = "Cria a consultoria e persiste o primeiro usuário Administrador com senha protegida por BCrypt")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterConsultoriaRequest request) {
        var command = new CadastrarConsultoriaComAdminUseCase.CadastrarConsultoriaComAdminCommand(
                request.cnpj(),
                request.razaoSocial(),
                request.nomeFantasia(),
                request.emailAdministrador(),
                request.telefone(),
                request.getPlanoOrDefault(),
                request.nomeAdministrador(),
                request.emailAdministrador(),
                request.senha(),
                request.celularAdmin()
        );

        var auth = cadastrarConsultoriaComAdminUseCase.cadastrar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(auth));
    }

    @GetMapping("/me")
    @Operation(summary = "Perfil do Usuário Autenticado", description = "Retorna os dados completos do usuário logado, papéis e prefeituras atribuídas")
    public ResponseEntity<LoginResponse> obterPerfil() {
        UUID userId = UserContext.getUserId();
        UUID tenantId = UserContext.getTenantId();

        if (userId == null || tenantId == null) {
            // Se o TenantContext estiver populado mas o UserContext não (ex: chamada direta pelo mock/gateway sem X-User-Id)
            tenantId = tenantId != null ? tenantId : TenantContext.getCurrentTenant();
            if (userId == null || tenantId == null) {
                throw new CredenciaisInvalidasException("Sessão não identificada. Token ausente ou inválido.");
            }
        }

        var auth = obterUsuarioAutenticadoUseCase.obterPerfil(userId, tenantId);
        return ResponseEntity.ok(toResponse(auth));
    }

    private LoginResponse toResponse(AutenticarUsuarioUseCase.UsuarioAutenticado auth) {
        return new LoginResponse(
                auth.token(),
                auth.tokenType(),
                auth.id(),
                auth.nome(),
                auth.email(),
                auth.tenantId(),
                auth.nomeConsultoria(),
                auth.role().name(),
                auth.prefeiturasAtribuidasIds()
        );
    }
}
