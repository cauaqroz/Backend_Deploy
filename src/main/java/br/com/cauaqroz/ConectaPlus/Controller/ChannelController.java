package br.com.cauaqroz.ConectaPlus.Controller;

import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import br.com.cauaqroz.ConectaPlus.model.Channel;
import br.com.cauaqroz.ConectaPlus.model.Message;
import br.com.cauaqroz.ConectaPlus.repository.ChannelRepository;
import br.com.cauaqroz.ConectaPlus.repository.MessageRepository;

@RestController
public class ChannelController {
    private final ChannelRepository channelRepository;
    private final MessageRepository messageRepository; 

    public ChannelController(ChannelRepository channelRepository, MessageRepository messageRepository) {
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository; 
    }

    @PostMapping("/channel")
    public Channel createChannel(@RequestBody Channel channel, @RequestHeader("userId") String userId) {
        channel.setMasterUserId(userId); // Define o usuário que está criando como mestre
        channelRepository.save(channel);
        return channel;
    }

    @PostMapping("/channel/{channelId}/authorize")
    public ResponseEntity<?> authorizeUser(@PathVariable String channelId, @RequestBody List<String> userIds, @RequestHeader("userId") String masterUserId) {
        Channel channel = channelRepository.findById(channelId).orElse(null);
        if (channel == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Canal não encontrado.");
        }
        if (!channel.getMasterUserId().equals(masterUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
        }
        List<String> allowedUserIds = channel.getAllowedUserIds();
        if (allowedUserIds == null) {
            allowedUserIds = new ArrayList<>();
        }
        List<String> accessRequests = channel.getAccessRequests();
        boolean updated = false;
        for (String userId : userIds) {
            if (!allowedUserIds.contains(userId)) {
                allowedUserIds.add(userId);
                updated = true;
            }
            if (accessRequests != null && accessRequests.contains(userId)) {
                accessRequests.remove(userId);
                updated = true;
            }
        }
        if (updated) {
            channel.setAllowedUserIds(allowedUserIds);
            channel.setAccessRequests(accessRequests);
            channelRepository.save(channel);
            return ResponseEntity.ok().body("Usuário(s) autorizado(s) com sucesso.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Nenhuma atualização necessária ou usuário(s) já autorizado(s).");
    }

    @PostMapping("/channel/{channelId}/deny-access")
public ResponseEntity<?> denyAccess(@PathVariable String channelId, @RequestBody List<String> userIds, @RequestHeader("userId") String masterUserId) {
    Channel channel = channelRepository.findById(channelId).orElse(null);
    if (channel == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Canal não encontrado.");
    }
    if (!channel.getMasterUserId().equals(masterUserId)) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
    }

    // Remove os IDs dos usuários das solicitações de acesso, se existirem
    List<String> accessRequests = channel.getAccessRequests();
    if (accessRequests != null) {
        accessRequests.removeAll(userIds);
        channel.setAccessRequests(accessRequests);
    }

    // Remove os IDs dos usuários da lista de usuários permitidos, se existirem
    List<String> allowedUserIds = channel.getAllowedUserIds();
    if (allowedUserIds != null) {
        allowedUserIds.removeAll(userIds);
        channel.setAllowedUserIds(allowedUserIds);
    }

    channelRepository.save(channel);
    return ResponseEntity.ok("Acesso removido para os usuários especificados.");
}

@PostMapping("/channel/{channelId}/request-access")
public String requestAccess(@PathVariable String channelId, @RequestHeader("userId") String userId) {
    Channel channel = channelRepository.findById(channelId).orElse(null);
    if (channel != null) {
        if (channel.getAccessRequests() == null) {
            channel.setAccessRequests(new ArrayList<>());
        }
        // Verifica se o userId já fez uma solicitação de acesso
        if (!channel.getAccessRequests().contains(userId)) {
            channel.getAccessRequests().add(userId);
            channelRepository.save(channel);
            return "Solicitação de acesso enviada para o canal: " + channelId;
        } else {
            return "Solicitação de acesso já foi enviada para o canal: " + channelId;
        }
    }
    return "Canal não encontrado.";
}

@GetMapping("/channel/{channelId}")
public ResponseEntity<?> joinChannel(@PathVariable String channelId, @RequestHeader("userId") String userId) {
    Channel channel = channelRepository.findById(channelId).orElse(null);
    if (channel == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Canal não encontrado.");
    }
    if (!channel.getMasterUserId().equals(userId) && (channel.getAllowedUserIds() == null || !channel.getAllowedUserIds().contains(userId))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado.");
    }
    List<Message> messages = messageRepository.findByChannelId(channelId);
    channel.setMessages(messages);
    return ResponseEntity.ok(channel);
}
    @GetMapping("/channel/{channelId}/access-requests")
public ResponseEntity<?> listAccessRequests(@PathVariable String channelId, @RequestHeader("userId") String userId) {
    Channel channel = channelRepository.findById(channelId).orElse(null);
    if (channel != null && channel.getMasterUserId().equals(userId)) {
        return ResponseEntity.ok(channel.getAccessRequests());
    }
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado ou canal não encontrado.");
}

    // Listar todas as mensagens da coleção "mensagens"
    @GetMapping("/messages")
    public List<Message> getAllMessages() {
        return messageRepository.findAll();
    }
  
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@RequestHeader("userId") String userId) {
        // Buscar todos os canais que o usuário participa ou criou
        List<Channel> channels = channelRepository.findAll().stream()
                .filter(channel -> (channel.getMasterUserId() != null && channel.getMasterUserId().equals(userId)) || 
                        (channel.getAllowedUserIds() != null && channel.getAllowedUserIds().contains(userId)))
                .collect(Collectors.toList());
    
        // Buscar novas mensagens nesses canais
        List<Message> newMessages = new ArrayList<>();
        for (Channel channel : channels) {
            List<Message> messages = messageRepository.findByChannelId(channel.getId());
            // Filtrar mensagens onde o senderId é diferente do userId
            messages.stream()
                    .filter(message -> !message.getSender().equals(userId))
                    .forEach(newMessages::add);
        }
    
        return ResponseEntity.ok(newMessages);
    }
    @GetMapping("/channels")
    public ResponseEntity<List<Channel>> getAllChannels() {
        List<Channel> channels = channelRepository.findAll();
        return ResponseEntity.ok(channels);
    }
}


