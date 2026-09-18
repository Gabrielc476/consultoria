package br.com.govflow.core.infrastructure.adapter.in.rest;

import br.com.govflow.core.domain.model.*;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.DadosRevisaoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.RetencaoTributariaRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DocumentoRestMapper {

    public DocumentoResponse toResponse(Documento domain) {
        if (domain == null) {
            return null;
        }

        ExtracaoSugeridaResponse extracaoResponse = toExtracaoResponse(domain.getExtracaoSugerida());
        Map<String, BoundingBoxResponse> boxesResponse = toBoxesResponse(domain.getBoundingBoxes());
        DadosRevisaoResponse revisaoResponse = toRevisaoResponse(domain.getDadosRevisao());

        return new DocumentoResponse(
                domain.getId(),
                domain.getTenantId(),
                domain.getPrefeituraId(),
                domain.getConvenioId(),
                domain.getS3Bucket(),
                domain.getS3Key(),
                domain.getNomeArquivoOriginal(),
                domain.getContentType(),
                domain.getTamanhoBytes(),
                domain.getStatus().name(),
                extracaoResponse,
                boxesResponse,
                revisaoResponse,
                domain.getMotivoRejeicao(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public AuditoriaRevisaoResponse toAuditoriaResponse(AuditoriaRevisao domain) {
        if (domain == null) {
            return null;
        }

        Map<String, AlteracaoCampoResponse> diffMap = new LinkedHashMap<>();
        if (domain.getDiff() != null && domain.getDiff().alteracoes() != null) {
            for (Map.Entry<String, AlteracaoCampo> entry : domain.getDiff().alteracoes().entrySet()) {
                AlteracaoCampo alt = entry.getValue();
                diffMap.put(entry.getKey(), new AlteracaoCampoResponse(alt.campo(), alt.de(), alt.para()));
            }
        }

        return new AuditoriaRevisaoResponse(
                domain.getId(),
                domain.getTenantId(),
                domain.getDocumentoId(),
                domain.getAnalistaId(),
                domain.getAcao().name(),
                domain.getDataRevisao(),
                domain.getJustificativa(),
                diffMap,
                domain.getValoresOriginais(),
                domain.getValoresRevisados()
        );
    }

    public DadosRevisaoAnalista toDomain(DadosRevisaoRequest request) {
        if (request == null) {
            return null;
        }

        TipoDocumentoHabil tipo = null;
        if (request.tipoDocumento() != null && !request.tipoDocumento().isBlank()) {
            try {
                tipo = TipoDocumentoHabil.valueOf(request.tipoDocumento().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<RetencaoTributaria> retencoes = new ArrayList<>();
        if (request.retencoes() != null) {
            for (RetencaoTributariaRequest r : request.retencoes()) {
                try {
                    TipoRetencao tipoR = TipoRetencao.valueOf(r.tipo().trim().toUpperCase());
                    retencoes.add(new RetencaoTributaria(
                            tipoR,
                            r.aliquota(),
                            r.valor(),
                            1.0,
                            null
                    ));
                } catch (Exception ignored) {
                }
            }
        }

        return new DadosRevisaoAnalista(
                tipo,
                request.numeroDocumento(),
                request.serieDocumento(),
                request.chaveAcessoNfe(),
                request.dataEmissao(),
                request.cnpjCredor(),
                request.razaoSocialCredor(),
                request.descricaoServico(),
                request.numeroEmpenho(),
                request.valorBruto(),
                request.valorTotalDeducoes(),
                request.valorLiquido(),
                retencoes,
                request.observacao()
        );
    }

    private ExtracaoSugeridaResponse toExtracaoResponse(ExtracaoSugerida extracao) {
        if (extracao == null) {
            return null;
        }

        List<RetencaoResponse> retencoes = new ArrayList<>();
        if (extracao.retencoes() != null) {
            for (RetencaoTributaria r : extracao.retencoes()) {
                BoundingBoxResponse box = r.coordenadas() != null ?
                        new BoundingBoxResponse(r.coordenadas().ymin(), r.coordenadas().xmin(), r.coordenadas().ymax(), r.coordenadas().xmax()) : null;
                retencoes.add(new RetencaoResponse(r.tipo().name(), r.aliquota(), r.valor(), r.confianca(), box));
            }
        }

        return new ExtracaoSugeridaResponse(
                extracao.tipoDocumento() != null ? extracao.tipoDocumento().name() : null,
                extracao.numeroDocumento(),
                extracao.serieDocumento(),
                extracao.chaveAcessoNfe(),
                extracao.dataEmissao(),
                extracao.cnpjCredor(),
                extracao.razaoSocialCredor(),
                extracao.descricaoServico(),
                extracao.numeroEmpenho(),
                extracao.valorBruto(),
                extracao.valorTotalDeducoes(),
                extracao.valorLiquido(),
                retencoes,
                extracao.confidenceScoreGeral(),
                extracao.scoresConfiancaCampos(),
                extracao.consistenteMatematicamente(),
                extracao.alertasInconsistencia()
        );
    }

    private Map<String, BoundingBoxResponse> toBoxesResponse(Map<String, BoundingBox> boxes) {
        if (boxes == null) {
            return Collections.emptyMap();
        }
        Map<String, BoundingBoxResponse> response = new LinkedHashMap<>();
        for (Map.Entry<String, BoundingBox> entry : boxes.entrySet()) {
            BoundingBox b = entry.getValue();
            response.put(entry.getKey(), new BoundingBoxResponse(b.ymin(), b.xmin(), b.ymax(), b.xmax()));
        }
        return response;
    }

    private DadosRevisaoResponse toRevisaoResponse(DadosRevisaoAnalista revisao) {
        if (revisao == null) {
            return null;
        }

        List<RetencaoResponse> retencoes = new ArrayList<>();
        if (revisao.retencoes() != null) {
            for (RetencaoTributaria r : revisao.retencoes()) {
                retencoes.add(new RetencaoResponse(r.tipo().name(), r.aliquota(), r.valor(), r.confianca(), null));
            }
        }

        return new DadosRevisaoResponse(
                revisao.tipoDocumento() != null ? revisao.tipoDocumento().name() : null,
                revisao.numeroDocumento(),
                revisao.serieDocumento(),
                revisao.chaveAcessoNfe(),
                revisao.dataEmissao(),
                revisao.cnpjCredor(),
                revisao.razaoSocialCredor(),
                revisao.descricaoServico(),
                revisao.numeroEmpenho(),
                revisao.valorBruto(),
                revisao.valorTotalDeducoes(),
                revisao.valorLiquido(),
                retencoes,
                revisao.observacao()
        );
    }
}
