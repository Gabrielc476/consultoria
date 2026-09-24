package br.com.govflow.transferegov.query.controller;

import br.com.govflow.transferegov.persistence.repository.SincronizacaoConvenioRepository;
import br.com.govflow.transferegov.query.dto.ConvenioDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de Consulta (Read Side / CQRS-Light) para disponibilização de dados e projeções de convênios.
 */
@RestController
@RequestMapping("/api/v1/transferegov/convenios")
@Tag(name = "Convênios Transferegov (Query)", description = "Endpoints de consulta a convênios sincronizados e metadados de vigência (Read Side)")
public class ConvenioQueryController {

    private final SincronizacaoConvenioRepository convenioRepository;

    public ConvenioQueryController(SincronizacaoConvenioRepository convenioRepository) {
        this.convenioRepository = convenioRepository;
    }

    @GetMapping
    @Operation(summary = "Lista convênios sincronizados com paginação e filtros opcionais por UF, CNPJ ou Situação")
    public ResponseEntity<Page<ConvenioDTO>> listarConvenios(
            @RequestParam(name = "uf", required = false) String uf,
            @RequestParam(name = "cnpj", required = false) String cnpj,
            @RequestParam(name = "situacao", required = false) String situacao,
            @PageableDefault(size = 20, sort = "dataFimVigencia") Pageable pageable
    ) {
        Page<ConvenioDTO> page = convenioRepository.findByFiltros(uf, cnpj, situacao, pageable)
                .map(ConvenioDTO::fromEntity);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{nrConvenio}")
    @Operation(summary = "Busca detalhes de um convênio específico pelo número SICONV")
    public ResponseEntity<ConvenioDTO> buscarPorNumero(@PathVariable String nrConvenio) {
        return convenioRepository.findByNrConvenio(nrConvenio)
                .map(ConvenioDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
