package br.com.govflow.whatsapp.routing.service;

import br.com.govflow.whatsapp.domain.entity.ContatoConvenioEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoPrefeituraEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoConvenioRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoPrefeituraRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoRepository;
import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ContactResolutionService {

    private static final Logger log = LoggerFactory.getLogger(ContactResolutionService.class);

    private final ContatoRepository contatoRepository;
    private final ContatoConvenioRepository contatoConvenioRepository;
    private final ContatoPrefeituraRepository legacyContatoRepository;

    public ContactResolutionService(
            ContatoRepository contatoRepository,
            ContatoConvenioRepository contatoConvenioRepository,
            ContatoPrefeituraRepository legacyContatoRepository
    ) {
        this.contatoRepository = contatoRepository;
        this.contatoConvenioRepository = contatoConvenioRepository;
        this.legacyContatoRepository = legacyContatoRepository;
    }

    public record ResolvedContact(
            UUID contatoId,
            UUID tenantId,
            UUID prefeituraId,
            String nomeContato,
            String papel,
            String empresaOuOrgao,
            List<ConvenioCandidatoDto> conveniosCandidatos,
            boolean resolved,
            boolean remetenteNovo
    ) {
        public ResolvedContact(UUID tenantId, UUID prefeituraId, String nomeContato, String papel, boolean resolved) {
            this(null, tenantId, prefeituraId, nomeContato, papel, null, List.of(), resolved, !resolved);
        }

        public static ResolvedContact unresolved() {
            return new ResolvedContact(null, null, null, null, null, null, List.of(), false, true);
        }

        public static ResolvedContact of(ContatoEntity entity, List<ConvenioCandidatoDto> convenios) {
            UUID prefId = convenios.stream()
                    .filter(ConvenioCandidatoDto::principal)
                    .map(ConvenioCandidatoDto::prefeituraId)
                    .findFirst()
                    .orElseGet(() -> convenios.isEmpty() ? null : convenios.get(0).prefeituraId());

            return new ResolvedContact(
                    entity.getId(),
                    entity.getTenantId(),
                    prefId,
                    entity.getNome(),
                    entity.getPapel(),
                    entity.getEmpresaOuOrgao(),
                    convenios,
                    true,
                    false
            );
        }

        public boolean isContatoVinculado() {
            return resolved && conveniosCandidatos != null && !conveniosCandidatos.isEmpty();
        }

        public static ResolvedContact ofLegacy(ContatoPrefeituraEntity entity) {
            return new ResolvedContact(
                    entity.getId(),
                    entity.getTenantId(),
                    entity.getPrefeituraId(),
                    entity.getNomeContato(),
                    entity.getCargo(),
                    entity.getDepartamento(),
                    List.of(),
                    true,
                    false
            );
        }
    }

    @Transactional(readOnly = true)
    public ResolvedContact resolve(String rawPhoneNumber) {
        if (rawPhoneNumber != null && (rawPhoneNumber.contains("@g.us") || rawPhoneNumber.length() > 16)) {
            log.info("Remetente ignorado por ser grupo do WhatsApp ou canal ({})", rawPhoneNumber);
            return ResolvedContact.unresolved();
        }

        String cleanPhone = sanitizePhone(rawPhoneNumber);
        if (cleanPhone.isBlank()) {
            return ResolvedContact.unresolved();
        }

        // 1. Busca no modelo desacoplado (tb_contatos) por número exato
        Optional<ContatoEntity> contatoOpt = contatoRepository.findByPhoneNumberAndAtivoTrue(cleanPhone);
        if (contatoOpt.isPresent()) {
            return buildResolvedContact(contatoOpt.get(), cleanPhone);
        }

        // 2. Normalização de 9º dígito no modelo desacoplado (tb_contatos)
        Optional<ContatoEntity> flexOpt = resolveBrazilianNineDigitVariation(cleanPhone);
        if (flexOpt.isPresent()) {
            return buildResolvedContact(flexOpt.get(), cleanPhone);
        }

        // 3. Fallback para modelo legado tb_contatos_prefeitura
        if (legacyContatoRepository != null) {
            Optional<ContatoPrefeituraEntity> legacyOpt = legacyContatoRepository.findByPhoneNumberAndAtivoTrue(cleanPhone);
            if (legacyOpt.isPresent()) {
                log.info("Contato resolvido via tabela legada: {} -> Prefeitura: {}, Tenant: {}",
                        cleanPhone, legacyOpt.get().getPrefeituraId(), legacyOpt.get().getTenantId());
                return ResolvedContact.ofLegacy(legacyOpt.get());
            }

            Optional<ContatoPrefeituraEntity> legacyFlexOpt = resolveLegacyNineDigit(cleanPhone);
            if (legacyFlexOpt.isPresent()) {
                log.info("Contato resolvido via normalização na tabela legada: {} -> Prefeitura: {}, Tenant: {}",
                        cleanPhone, legacyFlexOpt.get().getPrefeituraId(), legacyFlexOpt.get().getTenantId());
                return ResolvedContact.ofLegacy(legacyFlexOpt.get());
            }
        }

        log.warn("Remetente não cadastrado no sistema ou inativo: {}. Mensagem será tratada como remetente_novo (Triagem).", cleanPhone);
        return ResolvedContact.unresolved();
    }

    private ResolvedContact buildResolvedContact(ContatoEntity contato, String cleanPhone) {
        List<ContatoConvenioEntity> vinculos = contatoConvenioRepository.findByIdContatoId(contato.getId());
        List<ConvenioCandidatoDto> candidatos = vinculos.stream()
                .map(v -> new ConvenioCandidatoDto(
                        v.getConvenioId(),
                        v.getPrefeituraId(),
                        v.getPapelEspecifico(),
                        v.isPrincipal()
                ))
                .toList();

        log.info("Contato 1:N resolvido com sucesso: {} ({}) -> {} convênios vinculados, Tenant: {}",
                cleanPhone, contato.getNome(), candidatos.size(), contato.getTenantId());

        return ResolvedContact.of(contato, candidatos);
    }

    public String sanitizePhone(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        return rawPhone.replaceAll("[^0-9]", "");
    }

    private Optional<ContatoEntity> resolveBrazilianNineDigitVariation(String phone) {
        if (phone.startsWith("55") && phone.length() == 13) {
            String withoutNine = phone.substring(0, 4) + phone.substring(5);
            return contatoRepository.findByPhoneNumberAndAtivoTrue(withoutNine);
        }
        if (phone.startsWith("55") && phone.length() == 12) {
            String withNine = phone.substring(0, 4) + "9" + phone.substring(4);
            return contatoRepository.findByPhoneNumberAndAtivoTrue(withNine);
        }
        return Optional.empty();
    }

    private Optional<ContatoPrefeituraEntity> resolveLegacyNineDigit(String phone) {
        if (phone.startsWith("55") && phone.length() == 13) {
            String withoutNine = phone.substring(0, 4) + phone.substring(5);
            return legacyContatoRepository.findByPhoneNumberAndAtivoTrue(withoutNine);
        }
        if (phone.startsWith("55") && phone.length() == 12) {
            String withNine = phone.substring(0, 4) + "9" + phone.substring(4);
            return legacyContatoRepository.findByPhoneNumberAndAtivoTrue(withNine);
        }
        return Optional.empty();
    }
}
