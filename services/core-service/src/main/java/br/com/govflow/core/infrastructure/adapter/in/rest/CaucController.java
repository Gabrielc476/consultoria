package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.application.port.in.AtualizarCertidaoCaucUseCase;
import br.com.govflow.core.application.port.in.AvaliarConformidadeCaucUseCase;
import br.com.govflow.core.application.port.in.ConsultarCaucUseCase;
import br.com.govflow.core.domain.model.CertidaoCauc;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarCertidaoCaucRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.CertidaoCaucResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DossieCaucResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ResultadoAvaliacaoCaucResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ResumoCaucResponse;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cauc")
@Tag(name = "Radar CAUC", description = "Monitoramento da regularidade fiscal e orçamentária dos municípios (LRF Art. 25)")
public class CaucController {

    private final ConsultarCaucUseCase consultarCaucUseCase;
    private final AtualizarCertidaoCaucUseCase atualizarCertidaoCaucUseCase;
    private final AvaliarConformidadeCaucUseCase avaliarConformidadeCaucUseCase;

    public CaucController(ConsultarCaucUseCase consultarCaucUseCase,
                          AtualizarCertidaoCaucUseCase atualizarCertidaoCaucUseCase,
                          AvaliarConformidadeCaucUseCase avaliarConformidadeCaucUseCase) {
        this.consultarCaucUseCase = consultarCaucUseCase;
        this.atualizarCertidaoCaucUseCase = atualizarCertidaoCaucUseCase;
        this.avaliarConformidadeCaucUseCase = avaliarConformidadeCaucUseCase;
    }

    @GetMapping("/prefeituras/{prefeituraId}")
    @Operation(summary = "Obter Dossiê CAUC da Prefeitura", description = "Retorna o extrato das 16 certidões fiscais e orçamentárias obrigatórias")
    public ResponseEntity<DossieCaucResponse> obterDossie(@PathVariable UUID prefeituraId) {
        var dossie = consultarCaucUseCase.obterDossiePrefeitura(prefeituraId);
        return ResponseEntity.ok(DossieCaucResponse.fromDto(dossie));
    }

    @GetMapping("/resumo")
    @Operation(summary = "Obter Resumo Geral do Radar CAUC", description = "Retorna matriz de saúde fiscal e contagem de risco para todos os municípios gerenciados")
    public ResponseEntity<ResumoCaucResponse> obterResumo() {
        UUID tenantId = TenantContext.getCurrentTenant();
        var resumo = consultarCaucUseCase.obterResumoConsultoria(tenantId);
        return ResponseEntity.ok(ResumoCaucResponse.fromDto(resumo));
    }

    @PostMapping("/prefeituras/{prefeituraId}/certidoes")
    @Operation(summary = "Cadastrar ou Atualizar Certidão CAUC", description = "Cadastra ou renova uma certidão específica da prefeitura")
    public ResponseEntity<CertidaoCaucResponse> cadastrarCertidao(
            @PathVariable UUID prefeituraId,
            @Valid @RequestBody CadastrarCertidaoCaucRequest request) {
        UUID tenantId = TenantContext.getCurrentTenant();

        var command = new AtualizarCertidaoCaucUseCase.CadastrarCertidaoCommand(
                tenantId,
                prefeituraId,
                request.tipoExigencia(),
                request.numeroCertidao(),
                request.dataEmissao(),
                request.dataValidade(),
                request.s3KeyComprovante(),
                request.situacao()
        );

        CertidaoCauc salva = atualizarCertidaoCaucUseCase.cadastrarOuAtualizarCertidao(command);
        URI uri = URI.create("/api/v1/cauc/prefeituras/" + prefeituraId + "/certidoes/" + salva.getId());
        return ResponseEntity.created(uri).body(CertidaoCaucResponse.fromDomain(salva));
    }

    @PostMapping("/avaliar")
    @Operation(summary = "Reavaliar Conformidade do CAUC Sob Demanda", description = "Executa a reavaliação de semáforos e disparo de alertas em tempo real")
    public ResponseEntity<ResultadoAvaliacaoCaucResponse> avaliarConformidade() {
        UUID tenantId = TenantContext.getCurrentTenant();
        var resultado = avaliarConformidadeCaucUseCase.avaliarTodasPrefeituras(tenantId);
        return ResponseEntity.ok(ResultadoAvaliacaoCaucResponse.fromDto(resultado));
    }
}
