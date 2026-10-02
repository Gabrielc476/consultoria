package br.com.govflow.core.infrastructure.adapter.out.persistence;

import br.com.govflow.core.application.port.out.ConvenioRepositoryPort;
import br.com.govflow.core.application.port.out.FicheiroRepositoryPort;
import br.com.govflow.core.domain.exception.ConvenioNaoEncontradoException;
import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.infrastructure.adapter.out.persistence.entity.DocumentoJpaEntity;
import br.com.govflow.core.infrastructure.adapter.out.persistence.mapper.DocumentoPersistenceMapper;
import br.com.govflow.core.infrastructure.adapter.out.persistence.repository.SpringDataDocumentoRepository;
import br.com.govflow.core.infrastructure.interceptor.TenantContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FicheiroRepositoryAdapter implements FicheiroRepositoryPort {

    private final SpringDataDocumentoRepository documentoRepository;
    private final ConvenioRepositoryPort convenioRepository;
    private final DocumentoPersistenceMapper mapper;

    public FicheiroRepositoryAdapter(SpringDataDocumentoRepository documentoRepository,
                                    ConvenioRepositoryPort convenioRepository,
                                    DocumentoPersistenceMapper mapper) {
        this.documentoRepository = documentoRepository;
        this.convenioRepository = convenioRepository;
        this.mapper = mapper;
    }

    @Override
    public FicheiroDigital carregarFicheiroDigital(UUID convenioId) {
        Convenio convenio = convenioRepository.buscarPorId(convenioId)
                .orElseThrow(() -> new ConvenioNaoEncontradoException(convenioId));

        UUID tenantId = TenantContext.getCurrentTenant();
        List<DocumentoJpaEntity> entities;
        if (tenantId != null) {
            entities = documentoRepository.findByTenantIdAndConvenioId(tenantId, convenioId);
        } else {
            entities = documentoRepository.findByConvenioId(convenioId);
        }

        List<Documento> documentos = entities.stream()
                .map(mapper::toDomain)
                .toList();

        Map<FaseCicloVida, List<Documento>> docsPorFase = documentos.stream()
                .collect(Collectors.groupingBy(Documento::getFaseCicloVida));

        List<FicheiroDigital.PastaFase> pastas = new ArrayList<>();
        for (FaseCicloVida fase : FaseCicloVida.values()) {
            List<Documento> docsFase = docsPorFase.getOrDefault(fase, Collections.emptyList());
            long totalBytes = docsFase.stream()
                    .mapToLong(d -> d.getTamanhoBytes() != null ? d.getTamanhoBytes() : 0L)
                    .sum();

            pastas.add(new FicheiroDigital.PastaFase(
                    fase,
                    fase.getCodigo(),
                    fase.getNomePasta(),
                    fase.getDescricao(),
                    docsFase.size(),
                    totalBytes,
                    docsFase
            ));
        }

        return new FicheiroDigital(
                convenio.getId(),
                convenio.getPrefeituraId(),
                convenio.getTenantId(),
                convenio.getNumeroSiconv(),
                convenio.getObjeto(),
                pastas
        );
    }

    @Override
    public List<Documento> listarDocumentosPorFase(UUID convenioId, FaseCicloVida fase) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String faseStr = fase != null ? fase.name() : FaseCicloVida.FASE_05_EXECUCAO_FINANCEIRA.name();
        List<DocumentoJpaEntity> entities;
        if (tenantId != null) {
            entities = documentoRepository.findByTenantIdAndConvenioIdAndFaseCicloVida(tenantId, convenioId, faseStr);
        } else {
            entities = documentoRepository.findByConvenioIdAndFaseCicloVida(convenioId, faseStr);
        }
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Documento> listarDocumentosPorPasta(UUID convenioId, String pastaVirtual) {
        UUID tenantId = TenantContext.getCurrentTenant();
        String pasta = (pastaVirtual != null && !pastaVirtual.isBlank()) ? pastaVirtual : "/";
        List<DocumentoJpaEntity> entities;
        if (tenantId != null) {
            entities = documentoRepository.findByTenantIdAndConvenioIdAndPastaVirtual(tenantId, convenioId, pasta);
        } else {
            entities = documentoRepository.findByConvenioId(convenioId).stream()
                    .filter(e -> pasta.equals(e.getPastaVirtual()))
                    .toList();
        }
        return entities.stream().map(mapper::toDomain).toList();
    }
}
