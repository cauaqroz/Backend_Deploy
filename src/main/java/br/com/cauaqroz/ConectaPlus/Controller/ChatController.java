package br.com.cauaqroz.ConectaPlus.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import br.com.cauaqroz.ConectaPlus.model.Channel;
import br.com.cauaqroz.ConectaPlus.model.Message;
import br.com.cauaqroz.ConectaPlus.model.User;
import br.com.cauaqroz.ConectaPlus.repository.ChannelRepository;
import br.com.cauaqroz.ConectaPlus.repository.MessageRepository;
import br.com.cauaqroz.ConectaPlus.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class ChatController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    // Enviar mensagem para um canal específico
@MessageMapping("/chat/{channelId}")
@SendTo("/channel/{channelId}")
public Message send(@DestinationVariable String channelId, Message message) throws Exception {
        // Passo 1: Verificar se o usuário existe
        String senderId = message.getSender();
        Optional<User> user = userRepository.findById(senderId);
        if (user.isPresent()) {
            message.setSenderName(user.get().getName());
        } else {
            throw new Exception("Usuário não encontrado");
        }
    // Passo 2: Buscar o canal pelo channelId
    Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new Exception("Canal não encontrado"));

    // Passo 3: Verificar se o usuário tem permissão
    if (!channel.getMasterUserId().equals(senderId) && !channel.getAllowedUserIds().contains(senderId)) {
        throw new Exception("Usuário não tem permissão para enviar mensagens neste canal.");
    }

    // Se o usuário tem permissão, continuar com o processo de salvar a mensagem
    message.setChannelId(channelId);
    messageRepository.save(message);

    if (channel.getMessages() == null) {
        channel.setMessages(new ArrayList<>());
    }
    channel.getMessages().add(message);
    channelRepository.save(channel);

    return message;
}

    // Endpoint para enviar mensagem via POST
    @PostMapping("/chat/{channelId}")
    public Message sendPost(@PathVariable String channelId, @RequestBody Message message) throws Exception {
        return this.send(channelId, message);
    }
//pare aqui
    // Listar mensagens de um canal específico
    @GetMapping("/chat/{channelId}/messages")
    public List<Message> getMessages(@PathVariable String channelId) throws Exception {
        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new Exception("Canal não encontrado"));
        return channel.getMessages();
    }

    // Tratamento de exceções para canal não encontrado
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e) {
        return e.getMessage();
    }
}

