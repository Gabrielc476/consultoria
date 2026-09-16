package br.com.govflow.whatsapp.routing.service;

import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoPrefeituraRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ContactResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ContactResolutionService.class);

    private final ContatoPrefeituraRepository contatoRepository;

    public ContactResolutionService(ContatoPrefeituraRepository contatoRepository) {
        this.contatoRepository = contatoRepository;
    }

    public record ResolvedContact(
            UUID tenantId,
            UUID prefeituraId,
            String nomeContato,
            String cargo,
            boolean resolved
    ) {
        public static ResolvedContact unresolved() {
            return new ResolvedContact(null, null, null, null, false);
        }

        public static ResolvedContact of(ContatoPrefeituraEntity entity) {
            return new ResolvedContact(
                    entity.getTenantId(),
                    entity.getPrefeituraId(),
                    entity.getNomeContato(),
                    entity.getCargo(),
                    true
            );
        }
    }

    @Transactional(readOnly = true)
    public ResolvedContact resolve(String rawPhoneNumber) {
        String cleanPhone = sanitizePhone(rawPhoneNumber);
        if (cleanPhone.isBlank()) {
            return ResolvedContact.unresolved();
        }

        // 1. Busca exata por número ativo
        Optional<ContatoPrefeituraEntity> contatoOpt = contatoRepository.findByPhoneNumberAndAtivoTrue(cleanPhone);
        if (contatoOpt.isPresent()) {
            log.info("Contato resolvido com sucesso: {} -> Prefeitura: {}, Tenant: {}",
                    cleanPhone, contatoOpt.get().getPrefeituraId(), contatoOpt.get().getTenantId());
            return ResolvedContact.of(contatoOpt.get());
        }

        // 2. Tratamento de variação do 9º dígito móvel brasileiro (DDI 55 + DDD 2 dígitos + 8 ou 9 dígitos)
        Optional<ContatoPrefeituraEntity> flexOpt = resolveBrazilianNineDigitVariation(cleanPhone);
        if (flexOpt.isPresent()) {
            log.info("Contato resolvido via normalização de 9º dígito: {} -> Prefeitura: {}, Tenant: {}",
                    cleanPhone, flexOpt.get().getPrefeituraId(), flexOpt.get().getTenantId());
            return ResolvedContact.of(flexOpt.get());
        }

        log.warn("Remetente não cadastrado no sistema ou inativo: {}. Mensagem será tratada como unidentified.", cleanPhone);
        return ResolvedContact.unresolved();
    }

    public String sanitizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        return rawPhone.replaceAll("[^0-9]", "");
    }

    private Optional<ContatoPrefeituraEntity> resolveBrazilianNineDigitVariation(String phone) {
        // Se possui 13 dígitos e começa com 55 (ex: 55 83 9 9999-9999), tenta buscar sem o 9 (12 dígitos: 55 83 9999-9999)
        if (phone.startsWith("55") && phone.length() == 13) {
            String withoutNine = phone.substring(0, 4) + phone.substring(5);
            return contatoRepository.findByPhoneNumberAndAtivoTrue(withoutNine);
        }
        // Se possui 12 dígitos e começa com 55 (ex: 55 83 9999-9999), tenta buscar adicionando o 9 (13 dígitos: 55 83 9 9999-9999)
        if (phone.startsWith("55") && phone.length() == 12) {
            String withNine = phone.substring(0, 4) + "9" + phone.substring(4);
            return contatoRepository.findByPhoneNumberAndAtivoTrue(withNine);
        }
        return Optional.empty();
    }
}
