package br.com.govflow.core.domain.event;

import br.com.govflow.core.domain.model.StatusCertidao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento disparado no RabbitMQ (exchange: govflow.events, routingKey: core.cauc.alerta)
 * quando uma certidão do CAUC entra em alerta preventivo (D-10, D-5) ou encontra-se vencida.
 */
public record AlertaCertidaoCaucEvent(
        UUID tenantId,
        UUID prefeituraId,
        String nomeMunicipio,
        String uf,
        String tipoExigencia,
        String codigoExigencia,
        String nomeExigencia,
        StatusCertidao statusCertidao,
        Integer diasParaVencer,
        LocalDate dataValidade,
        String mensagemAlerta,
        Instant timestamp
) {
    public static AlertaCertidaoCaucEvent criar(
            UUID tenantId,
            UUID prefeituraId,
            String nomeMunicipio,
            String uf,
            String tipoExigencia,
            String codigoExigencia,
            String nomeExigencia,
            StatusCertidao statusCertidao,
            Integer diasParaVencer,
            LocalDate dataValidade
    ) {
        String msg;
        if (statusCertidao == StatusCertidao.VENCIDA) {
            msg = String.format("URGENTE: A certidão %s (%s) da Prefeitura de %s-%s está VENCIDA desde %s! Risco imediato de bloqueio de repasses federais.",
                    codigoExigencia, nomeExigencia, nomeMunicipio, uf, dataValidade);
        } else {
            msg = String.format("ALERTA CAUC: A certidão %s (%s) da Prefeitura de %s-%s vencerá em %d dias (%s). Providenciar renovação imediata.",
                    codigoExigencia, nomeExigencia, nomeMunicipio, uf, diasParaVencer, dataValidade);
        }

        return new AlertaCertidaoCaucEvent(
                tenantId,
                prefeituraId,
                nomeMunicipio,
                uf,
                tipoExigencia,
                codigoExigencia,
                nomeExigencia,
                statusCertidao,
                diasParaVencer,
                dataValidade,
                msg,
                Instant.now()
        );
    }
}
