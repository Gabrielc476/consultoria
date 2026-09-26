package br.com.govflow.core.domain.exception;

import br.com.govflow.core.domain.model.convenio.TipoCondicionanteSuspensiva;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClausulaSuspensivaNaoPodeSerSuperadaException extends DomainException {

    public ClausulaSuspensivaNaoPodeSerSuperadaException(UUID convenioId, List<TipoCondicionanteSuspensiva> pendentes) {
        super("CLAUSULA_SUSPENSIVA_BLOQUEADA",
                String.format("A Cláusula Suspensiva do convênio %s não pode ser superada. Os seguintes pilares da Caixa ainda não foram aprovados: [%s]",
                        convenioId,
                        pendentes.stream().map(TipoCondicionanteSuspensiva::getDescricao).collect(Collectors.joining(", "))));
    }
}
