package br.com.govflow.transferegov.query.controller;

import br.com.govflow.transferegov.domain.compliance.StatusAdpf854;
import br.com.govflow.transferegov.query.dto.AuditoriaAdpf854ResponseDTO;
import br.com.govflow.transferegov.query.dto.EmendaEspecialDetalheDTO;
import br.com.govflow.transferegov.query.dto.EmendaEspecialResumoDTO;
import br.com.govflow.transferegov.query.service.EmendaEspecialQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transferegov/emendas-especiais")
@Tag(name = "Emendas Especiais (Pix) & Auditoria STF", description = "Endpoints de consulta e auditoria de conformidade com a ADPF 854 para transferências especiais")
public class EmendaEspecialQueryController {

    private final EmendaEspecialQueryService queryService;

    public EmendaEspecialQueryController(EmendaEspecialQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    @Operation(summary = "Lista Emendas Especiais (Pix) sincronizadas com paginação e filtros dinâmicos")
    public ResponseEntity<Page<EmendaEspecialResumoDTO>> listarEmendas(
            @RequestParam(name = "uf", required = false) String uf,
            @RequestParam(name = "cnpj", required = false) String cnpj,
            @RequestParam(name = "municipio", required = false) String municipio,
            @RequestParam(name = "ano", required = false) Integer ano,
            @RequestParam(name = "parlamentar", required = false) String parlamentar,
            @RequestParam(name = "statusAdpf854", required = false) StatusAdpf854 statusAdpf854,
            @PageableDefault(size = 20, sort = "valorTotal") Pageable pageable
    ) {
        Page<EmendaEspecialResumoDTO> page = queryService.listarEmendas(
                uf, cnpj, municipio, ano, parlamentar, statusAdpf854, pageable
        );
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{idOuCodigo}")
    @Operation(summary = "Busca detalhes de uma Emenda Pix específica por UUID, ID federal ou Código do Plano de Ação")
    public ResponseEntity<EmendaEspecialDetalheDTO> buscarPorIdOuCodigo(@PathVariable String idOuCodigo) {
        return queryService.buscarPorIdOuCodigo(idOuCodigo)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/auditoria-adpf854")
    @Operation(summary = "Retorna painel consolidado de auditoria de conformidade com a ADPF 854 do STF")
    public ResponseEntity<AuditoriaAdpf854ResponseDTO> obterPainelAuditoria(
            @RequestParam(name = "uf", required = false) String uf
    ) {
        AuditoriaAdpf854ResponseDTO response = queryService.obterPainelAuditoria(uf);
        return ResponseEntity.ok(response);
    }
}
