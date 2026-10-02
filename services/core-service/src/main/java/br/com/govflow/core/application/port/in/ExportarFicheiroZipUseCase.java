package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.documento.FaseCicloVida;

import java.io.OutputStream;
import java.util.UUID;

public interface ExportarFicheiroZipUseCase {

    void exportarConvenioIntegral(UUID convenioId, OutputStream outputStream);

    void exportarFaseEspecifica(UUID convenioId, FaseCicloVida fase, OutputStream outputStream);
}
