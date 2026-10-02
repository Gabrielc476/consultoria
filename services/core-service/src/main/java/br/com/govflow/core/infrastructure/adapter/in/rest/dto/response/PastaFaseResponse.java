package br.com.govflow.core.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

public record PastaFaseResponse(
        String fase,
        String codigoFase,
        String nomePasta,
        String descricao,
        int quantidadeArquivos,
        long tamanhoTotalBytes,
        List<DocumentoFicheiroResponse> documentos
) {}
