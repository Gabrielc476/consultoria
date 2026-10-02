package br.com.govflow.core.application.port.in;

import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoETriarRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.request.CadastrarContatoRequest;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.ContatoResponse;
import br.com.govflow.core.infrastructure.adapter.in.rest.dto.response.TriagemItemResponse;

import java.util.List;
import java.util.UUID;

public interface TriagemUseCase {

    List<TriagemItemResponse> listarPendentes();

    TriagemItemResponse cadastrarContatoETriar(UUID inboxId, CadastrarContatoETriarRequest request);

    TriagemItemResponse confirmarArquivamento(UUID inboxId, UUID convenioId, String faseCicloVida);

    TriagemItemResponse ignorarItem(UUID inboxId);

    ContatoResponse cadastrarContato(CadastrarContatoRequest request);

    List<ContatoResponse> listarContatos();
}
