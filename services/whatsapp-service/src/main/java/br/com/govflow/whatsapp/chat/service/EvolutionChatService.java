package br.com.govflow.whatsapp.chat.service;

import br.com.govflow.whatsapp.chat.dto.ChatResumoDto;
import br.com.govflow.whatsapp.chat.dto.MensagemChatDto;
import br.com.govflow.whatsapp.config.WhatsAppProperties;
import br.com.govflow.whatsapp.domain.entity.ContatoConvenioEntity;
import br.com.govflow.whatsapp.domain.entity.ContatoEntity;
import br.com.govflow.whatsapp.domain.entity.MensagemInboundEntity;
import br.com.govflow.whatsapp.domain.repository.ContatoConvenioRepository;
import br.com.govflow.whatsapp.domain.repository.ContatoRepository;
import br.com.govflow.whatsapp.domain.repository.MensagemInboundRepository;
import br.com.govflow.whatsapp.inbound.dto.InboundMessageDto;
import br.com.govflow.whatsapp.inbound.service.AsyncMediaDispatcher;
import br.com.govflow.whatsapp.outbound.dto.MessageSentResult;
import br.com.govflow.whatsapp.outbound.strategy.EvolutionSenderStrategy;
import br.com.govflow.whatsapp.routing.dto.ConvenioCandidatoDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class EvolutionChatService {

    private static final Logger log = LoggerFactory.getLogger(EvolutionChatService.class);

    private final WhatsAppProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MensagemInboundRepository mensagemRepository;
    private final ContatoRepository contatoRepository;
    private final ContatoConvenioRepository contatoConvenioRepository;
    private final AsyncMediaDispatcher asyncMediaDispatcher;
    private final EvolutionSenderStrategy senderStrategy;

    public EvolutionChatService(
            WhatsAppProperties properties,
            ObjectMapper objectMapper,
            MensagemInboundRepository mensagemRepository,
            ContatoRepository contatoRepository,
            ContatoConvenioRepository contatoConvenioRepository,
            AsyncMediaDispatcher asyncMediaDispatcher,
            EvolutionSenderStrategy senderStrategy
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.mensagemRepository = mensagemRepository;
        this.contatoRepository = contatoRepository;
        this.contatoConvenioRepository = contatoConvenioRepository;
        this.asyncMediaDispatcher = asyncMediaDispatcher;
        this.senderStrategy = senderStrategy;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public List<ChatResumoDto> getRecentChats() {
        String instance = properties.getEvolution().getInstanceName();
        String url = String.format("%s/chat/findChats/%s", properties.getEvolution().getBaseUrl(), instance);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Falha ao buscar chats da Evolution API: HTTP {}", response.statusCode());
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                return Collections.emptyList();
            }

            // Carrega mapa de contatos da agenda da Evolution API
            Map<String, String> contactMap = fetchContactNamesMap(instance);
            Map<String, ChatResumoDto> phoneToChatMap = new LinkedHashMap<>();

            for (JsonNode chatNode : root) {
                String remoteJid = chatNode.path("remoteJid").asText("");
                if (remoteJid.endsWith("@g.us") || remoteJid.endsWith("@broadcast")) {
                    continue; // Ignora grupos e transmissões para privacidade
                }

                JsonNode lastMsg = chatNode.path("lastMessage");
                String remoteJidAlt = lastMsg.path("key").path("remoteJidAlt").asText("");
                String cleanPhone = extractPhone(remoteJid, remoteJidAlt);
                if (cleanPhone.isBlank()) {
                    continue;
                }

                boolean fromMe = lastMsg.path("key").path("fromMe").asBoolean(false);

                // Resolução do Nome Real do Contato (NUNCA o próprio usuário/"Você")
                String pushName = null;

                // 1. Tenta mapa de contatos da agenda do WhatsApp
                if (contactMap.containsKey(remoteJid)) {
                    pushName = contactMap.get(remoteJid);
                } else if (!remoteJidAlt.isBlank() && contactMap.containsKey(remoteJidAlt)) {
                    pushName = contactMap.get(remoteJidAlt);
                } else if (contactMap.containsKey(cleanPhone)) {
                    pushName = contactMap.get(cleanPhone);
                }

                // 2. Tenta pushName do próprio chatNode se for válido
                if ((pushName == null || pushName.isBlank() || isIgnoredName(pushName))) {
                    String chatPush = chatNode.path("pushName").asText(null);
                    if (chatPush != null && !chatPush.isBlank() && !isIgnoredName(chatPush)) {
                        pushName = chatPush;
                    }
                }

                // 3. Tenta lastMessage APENAS SE NÃO FOR fromMe
                if ((pushName == null || pushName.isBlank() || isIgnoredName(pushName)) && !fromMe) {
                    String lastPush = lastMsg.path("pushName").asText(null);
                    if (lastPush != null && !lastPush.isBlank() && !isIgnoredName(lastPush)) {
                        pushName = lastPush;
                    }
                }

                // 4. Se a última mensagem foi enviada por mim (fromMe == true) e ainda não temos nome, busca mensagem recebida
                if ((pushName == null || pushName.isBlank() || isIgnoredName(pushName)) && fromMe) {
                    pushName = findInboundPushName(instance, remoteJid, remoteJidAlt);
                }

                // 5. Fallback limpo: formatação do telefone (ex: +55 (83) 99999-8888)
                if (pushName == null || pushName.isBlank() || isIgnoredName(pushName)) {
                    pushName = formatPhoneNumber(cleanPhone);
                }

                String profilePicUrl = chatNode.path("profilePicUrl").asText(null);
                String lastMessage = extractLastMessageSnippet(lastMsg);
                long timestamp = lastMsg.path("messageTimestamp").asLong(0L);
                Instant lastTime = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now();

                boolean isCadastrado = contatoRepository.existsByPhoneNumber(cleanPhone);

                ChatResumoDto chatDto = new ChatResumoDto(
                        remoteJid,
                        cleanPhone,
                        pushName,
                        profilePicUrl,
                        lastMessage,
                        lastTime,
                        isCadastrado
                );

                // Deduplicação inteligente por telefone: preserva o registro mais recente
                if (!phoneToChatMap.containsKey(cleanPhone) ||
                        chatDto.lastMessageTime().isAfter(phoneToChatMap.get(cleanPhone).lastMessageTime())) {
                    phoneToChatMap.put(cleanPhone, chatDto);
                }
            }

            List<ChatResumoDto> result = new ArrayList<>(phoneToChatMap.values());
            result.sort(Comparator.comparing(ChatResumoDto::lastMessageTime).reversed());
            return result;
        } catch (Exception e) {
            log.error("Erro ao consultar chats recentes na Evolution API", e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> fetchContactNamesMap(String instance) {
        Map<String, String> contactMap = new HashMap<>();
        String url = String.format("%s/chat/findContacts/%s", properties.getEvolution().getBaseUrl(), instance);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray()) {
                    for (JsonNode c : root) {
                        String name = c.path("pushName").asText("");
                        if (name.isBlank()) name = c.path("verifiedName").asText("");
                        if (name.isBlank()) name = c.path("name").asText("");
                        name = name.trim();

                        if (!name.isBlank() && !isIgnoredName(name)) {
                            String jid = c.path("remoteJid").asText("");
                            if (!jid.isBlank()) {
                                contactMap.put(jid, name);
                                String phone = jid.replaceAll("@.*", "").replaceAll(":.*", "").replaceAll("[^0-9]", "");
                                if (!phone.isBlank()) {
                                    contactMap.put(phone, name);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Não foi possível carregar mapa de contatos da Evolution API: {}", e.getMessage());
        }
        return contactMap;
    }

    private String findInboundPushName(String instance, String remoteJid, String remoteJidAlt) {
        String targetJid = (remoteJid != null && !remoteJid.isBlank()) ? remoteJid : remoteJidAlt;
        if (targetJid == null || targetJid.isBlank()) return null;

        String url = String.format("%s/chat/findMessages/%s", properties.getEvolution().getBaseUrl(), instance);
        try {
            String jsonBody = String.format("{\"where\":{\"key\":{\"remoteJid\":\"%s\"}},\"limit\":15}", targetJid);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode records = root.path("messages").path("records");
                if (!records.isArray()) {
                    records = root.path("records");
                }
                if (records.isArray()) {
                    for (JsonNode m : records) {
                        boolean fromMe = m.path("key").path("fromMe").asBoolean(false);
                        if (!fromMe) {
                            String name = m.path("pushName").asText(null);
                            if (name != null && !name.isBlank() && !isIgnoredName(name)) {
                                return name.trim();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Erro ao consultar mensagens inbound para capturar pushName: {}", e.getMessage());
        }
        return null;
    }

    private boolean isIgnoredName(String name) {
        if (name == null) return true;
        String lower = name.trim().toLowerCase();
        return lower.equals("você") || lower.equals("voce") || lower.equals("null");
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) return "Contato WhatsApp";
        String clean = phone.replaceAll("[^0-9]", "");
        if (clean.length() == 13 && clean.startsWith("55")) {
            return String.format("+55 (%s) %s-%s", clean.substring(2, 4), clean.substring(4, 9), clean.substring(9));
        }
        if (clean.length() == 12 && clean.startsWith("55")) {
            return String.format("+55 (%s) %s-%s", clean.substring(2, 4), clean.substring(4, 8), clean.substring(8));
        }
        return "+" + clean;
    }

    @Transactional
    public List<MensagemChatDto> importLast10Messages(String phoneNumber, UUID tenantId, UUID contatoId) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        String instance = properties.getEvolution().getInstanceName();
        String targetJid = cleanPhone + "@s.whatsapp.net";

        log.info("Sincronizando últimas 10 mensagens para contato {} (tenant={}, contatoId={})",
                cleanPhone, tenantId, contatoId);

        try {
            // Busca o remoteJid real a partir dos chats caso utilize LID
            List<ChatResumoDto> recent = getRecentChats();
            for (ChatResumoDto c : recent) {
                if (c.phone().equals(cleanPhone) && c.remoteJid() != null && !c.remoteJid().isBlank()) {
                    targetJid = c.remoteJid();
                    break;
                }
            }

            String url = String.format("%s/chat/findMessages/%s", properties.getEvolution().getBaseUrl(), instance);
            String jsonBody = String.format("{\"where\":{\"key\":{\"remoteJid\":\"%s\"}},\"limit\":10}", targetJid);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("apikey", properties.getEvolution().getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode records = root.path("messages").path("records");
                if (!records.isArray()) {
                    records = root.path("messages");
                }

                if (records.isArray()) {
                    List<JsonNode> messageNodes = new ArrayList<>();
                    records.forEach(messageNodes::add);
                    // Processa em ordem cronológica
                    Collections.reverse(messageNodes);

                    List<ConvenioCandidatoDto> conveniosCandidatos = carregarConveniosCandidatos(contatoId);

                    for (JsonNode msgNode : messageNodes) {
                        processSingleHistoricalMessage(msgNode, cleanPhone, tenantId, contatoId, conveniosCandidatos);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Falha ao sincronizar mensagens históricas da Evolution para {}", cleanPhone, e);
        }

        return getMessagesForPhone(cleanPhone, tenantId);
    }

    public List<MensagemChatDto> getMessagesForPhone(String phoneNumber, UUID tenantId) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        List<MensagemInboundEntity> entities = mensagemRepository.findBySenderPhoneOrderByCreatedAtAsc(cleanPhone);

        return entities.stream()
                .map(this::toMensagemChatDto)
                .toList();
    }

    @Transactional
    public MensagemChatDto sendMessage(String phoneNumber, String texto, UUID tenantId, UUID contatoId) {
        String cleanPhone = phoneNumber.replaceAll("[^0-9]", "");
        log.info("Enviando mensagem para {} via Central do WhatsApp: {}", cleanPhone, texto);

        MessageSentResult result = senderStrategy.sendTextMessage(cleanPhone, texto);
        if (!result.success()) {
            throw new RuntimeException("Erro ao disparar mensagem via Evolution API: " + result.error());
        }

        MensagemInboundEntity entity = new MensagemInboundEntity();
        entity.setExternalMessageId("OUT_" + UUID.randomUUID());
        entity.setInstanceName(properties.getEvolution().getInstanceName());
        entity.setSenderPhone(cleanPhone);
        entity.setSenderName("GovFlow");
        entity.setMessageType("TEXT");
        entity.setContentText(texto);
        entity.setFromMe(true);
        entity.setProcessed(true);
        entity.setTenantId(tenantId);
        entity.setContatoId(contatoId);
        entity.setCreatedAt(Instant.now());

        MensagemInboundEntity saved = mensagemRepository.save(entity);
        return toMensagemChatDto(saved);
    }

    private void processSingleHistoricalMessage(
            JsonNode msgNode,
            String cleanPhone,
            UUID tenantId,
            UUID contatoId,
            List<ConvenioCandidatoDto> conveniosCandidatos
    ) {
        JsonNode keyNode = msgNode.path("key");
        String externalId = keyNode.path("id").asText(null);
        if (externalId == null || externalId.isBlank() || mensagemRepository.existsByExternalMessageId(externalId)) {
            return;
        }

        boolean fromMe = keyNode.path("fromMe").asBoolean(false);
        String senderName = msgNode.path("pushName").asText(fromMe ? "GovFlow" : cleanPhone);
        long timestamp = msgNode.path("messageTimestamp").asLong(0L);
        Instant createdAt = timestamp > 0 ? Instant.ofEpochSecond(timestamp) : Instant.now();

        JsonNode messageContent = msgNode.path("message");
        String messageType = "TEXT";
        String contentText = null;
        String mediaUrl = null;
        String mediaMimetype = null;
        String fileName = null;
        Long fileLength = 0L;

        if (messageContent.has("conversation")) {
            contentText = messageContent.path("conversation").asText();
        } else if (messageContent.has("extendedTextMessage")) {
            contentText = messageContent.path("extendedTextMessage").path("text").asText();
        } else if (messageContent.has("documentMessage")) {
            messageType = "DOCUMENT";
            JsonNode doc = messageContent.path("documentMessage");
            contentText = doc.path("caption").asText(null);
            fileName = doc.path("fileName").asText(doc.path("title").asText("documento.pdf"));
            mediaMimetype = doc.path("mimetype").asText("application/pdf");
            fileLength = doc.path("fileLength").asLong(0L);
            mediaUrl = doc.path("url").asText(null);
        } else if (messageContent.has("audioMessage")) {
            messageType = "AUDIO";
            JsonNode audio = messageContent.path("audioMessage");
            mediaMimetype = audio.path("mimetype").asText("audio/ogg");
            fileName = "audio_" + externalId + ".ogg";
            fileLength = audio.path("fileLength").asLong(0L);
            mediaUrl = audio.path("url").asText(null);
        }

        MensagemInboundEntity entity = new MensagemInboundEntity();
        entity.setExternalMessageId(externalId);
        entity.setInstanceName(properties.getEvolution().getInstanceName());
        entity.setSenderPhone(cleanPhone);
        entity.setSenderName(senderName);
        entity.setMessageType(messageType);
        entity.setContentText(contentText);
        entity.setMediaUrl(mediaUrl);
        entity.setMediaMimetype(mediaMimetype);
        entity.setFileSizeBytes(fileLength);
        entity.setFromMe(fromMe);
        entity.setTenantId(tenantId);
        entity.setContatoId(contatoId);
        entity.setProcessed(false);
        entity.setCreatedAt(createdAt);

        MensagemInboundEntity saved = mensagemRepository.save(entity);

        // Se for documento ou áudio recebido do contato (!fromMe), envia para o pipeline de IA
        if (!fromMe && (messageType.equals("DOCUMENT") || messageType.equals("AUDIO"))) {
            UUID prefeituraId = conveniosCandidatos.isEmpty() ? null : conveniosCandidatos.get(0).prefeituraId();
            InboundMessageDto dto = new InboundMessageDto(
                    properties.getEvolution().getInstanceName(),
                    externalId,
                    cleanPhone,
                    senderName,
                    messageType,
                    contentText,
                    mediaUrl,
                    mediaMimetype,
                    fileName,
                    fileLength,
                    msgNode.toString(),
                    false
            );

            asyncMediaDispatcher.processMediaAndDispatch(
                    saved.getId(),
                    dto,
                    tenantId,
                    prefeituraId,
                    contatoId,
                    conveniosCandidatos,
                    false
            );
        }
    }

    private List<ConvenioCandidatoDto> carregarConveniosCandidatos(UUID contatoId) {
        if (contatoId == null) return Collections.emptyList();
        List<ContatoConvenioEntity> links = contatoConvenioRepository.findByIdContatoId(contatoId);
        return links.stream()
                .map(l -> new ConvenioCandidatoDto(
                        l.getId().getConvenioId(),
                        l.getPrefeituraId(),
                        l.getPapelEspecifico(),
                        l.isPrincipal()
                ))
                .toList();
    }

    private String extractPhone(String remoteJid, String remoteJidAlt) {
        if (remoteJidAlt != null && !remoteJidAlt.isBlank() && remoteJidAlt.contains("@s.whatsapp.net")) {
            return remoteJidAlt.split("@")[0].replaceAll("[^0-9]", "");
        }
        if (remoteJid != null && !remoteJid.isBlank()) {
            return remoteJid.split("@")[0].split(":")[0].replaceAll("[^0-9]", "");
        }
        return "";
    }

    private String extractLastMessageSnippet(JsonNode msgNode) {
        if (msgNode == null || msgNode.isMissingNode() || msgNode.isNull()) {
            return "";
        }
        JsonNode message = msgNode.path("message");
        if (message.has("conversation")) {
            return message.path("conversation").asText();
        }
        if (message.has("extendedTextMessage")) {
            return message.path("extendedTextMessage").path("text").asText();
        }
        if (message.has("documentMessage")) {
            return "📄 " + message.path("documentMessage").path("fileName").asText("Documento");
        }
        if (message.has("audioMessage")) {
            return "🎤 Mensagem de áudio";
        }
        if (message.has("imageMessage")) {
            return "📷 Foto";
        }
        return "";
    }

    private MensagemChatDto toMensagemChatDto(MensagemInboundEntity entity) {
        String remetente = entity.isFromMe() ? "USUARIO" : "CONTATO";
        String fileName = null;
        if (entity.getS3Key() != null && entity.getS3Key().contains("_")) {
            fileName = entity.getS3Key().substring(entity.getS3Key().indexOf('_') + 1);
        }

        return new MensagemChatDto(
                entity.getId(),
                entity.getExternalMessageId(),
                entity.getSenderPhone(),
                entity.getSenderName(),
                remetente,
                entity.isFromMe(),
                entity.getMessageType(),
                entity.getContentText(),
                entity.getAudioTranscription(),
                entity.getS3Key(),
                entity.getMediaUrl(),
                entity.getMediaMimetype(),
                fileName,
                entity.getFileSizeBytes(),
                entity.getCreatedAt(),
                entity.isFromMe() ? "ENTREGUE" : "LIDO"
        );
    }
}
