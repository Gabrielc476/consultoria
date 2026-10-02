package br.com.govflow.core.application.port.out;

import br.com.govflow.core.domain.model.documento.FaseCicloVida;
import br.com.govflow.core.domain.model.documento.FicheiroDigital;
import br.com.govflow.core.domain.model.Documento;

import java.util.List;
import java.util.UUID;

public interface FicheiroRepositoryPort {

    FicheiroDigital carregarFicheiroDigital(UUID convenioId);

    List<Documento> listarDocumentosPorFase(UUID convenioId, FaseCicloVida fase);

    List<Documento> listarDocumentosPorPasta(UUID convenioId, String pastaVirtual);
}
