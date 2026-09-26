package br.com.govflow.core.application.port.in;

import br.com.govflow.core.domain.model.convenio.CondicionanteSuspensiva;
import br.com.govflow.core.domain.model.convenio.Convenio;
import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.util.UUID;

public interface UploadDocumentoCondicionanteUseCase {

    CondicionanteSuspensiva uploadDocumentoComprobatorio(UUID convenioId,
                                                         TipoCondicionanteSuspensiva tipo,
                                                         String nomeArquivo,
                                                         String contentType,
                                                         byte[] conteudo);

    CondicionanteSuspensiva uploadLaudoPendencias(UUID convenioId,
                                                  TipoCondicionanteSuspensiva tipo,
                                                  String nomeArquivo,
                                                  String contentType,
                                                  byte[] conteudo);

    Convenio uploadTermoRetirada(UUID convenioId,
                                 String nomeArquivo,
                                 String contentType,
                                 byte[] conteudo);
}
