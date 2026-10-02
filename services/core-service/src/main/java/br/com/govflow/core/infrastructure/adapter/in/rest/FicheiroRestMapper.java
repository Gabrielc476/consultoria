package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.domain.model.Documento;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoFicheiroResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.FicheiroDigitalResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.PastaFaseResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FicheiroRestMapper {

    public FicheiroDigitalResponse toResponse(FicheiroDigital ficheiro) {
        if (ficheiro == null) return null;

        List<PastaFaseResponse> fases = ficheiro.getFases().stream()
                .map(this::toResponse)
                .toList();

        return new FicheiroDigitalResponse(
                ficheiro.getConvenioId(),
                ficheiro.getPrefeituraId(),
                ficheiro.getTenantId(),
                ficheiro.getNumeroSiconv(),
                ficheiro.getObjeto(),
                ficheiro.getTotalArquivos(),
                ficheiro.getTamanhoTotalBytes(),
                fases
        );
    }

    public PastaFaseResponse toResponse(FicheiroDigital.PastaFase pasta) {
        if (pasta == null) return null;

        List<DocumentoFicheiroResponse> docs = pasta.documentos().stream()
                .map(this::toResponse)
                .toList();

        return new PastaFaseResponse(
                pasta.fase().name(),
                pasta.codigoFase(),
                pasta.nomePasta(),
                pasta.descricao(),
                pasta.quantidadeArquivos(),
                pasta.tamanhoTotalBytes(),
                docs
        );
    }

    public DocumentoFicheiroResponse toResponse(Documento doc) {
        if (doc == null) return null;

        String faseNome = doc.getFaseCicloVida() != null ? doc.getFaseCicloVida().name() : null;
        String faseDesc = doc.getFaseCicloVida() != null ? doc.getFaseCicloVida().getDescricao() : null;
        String catNome = doc.getCategoriaDocumento() != null ? doc.getCategoriaDocumento().name() : null;
        String catDesc = doc.getCategoriaDocumento() != null ? doc.getCategoriaDocumento().getDescricao() : null;
        String origem = doc.getOrigemCanal() != null ? doc.getOrigemCanal().name() : null;

        return new DocumentoFicheiroResponse(
                doc.getId(),
                doc.getConvenioId(),
                doc.getPrefeituraId(),
                faseNome,
                faseDesc,
                catNome,
                catDesc,
                doc.getPastaVirtual(),
                doc.getNomeArquivoOriginal(),
                doc.getContentType(),
                doc.getTamanhoBytes(),
                doc.getHashSha256(),
                doc.getStatus() != null ? doc.getStatus().name() : null,
                origem,
                doc.getTags(),
                doc.getCriadoPorUsuarioId(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }

    public br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoAuditoriaResponse toResponse(
            br.com.govflow.core.domain.model.documento.DocumentoAuditoria auditoria) {
        if (auditoria == null) return null;
        return new br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.DocumentoAuditoriaResponse(
                auditoria.getId(),
                auditoria.getDocumentoId(),
                auditoria.getUsuarioId(),
                auditoria.getAcao(),
                auditoria.getJustificativa(),
                auditoria.getSnapshotAnteriorJson(),
                auditoria.getSnapshotAtualJson(),
                auditoria.getRealizadoEm()
        );
    }
}
