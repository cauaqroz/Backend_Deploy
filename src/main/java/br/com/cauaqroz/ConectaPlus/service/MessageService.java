package br.com.cauaqroz.ConectaPlus.service;

import br.com.cauaqroz.ConectaPlus.model.Message;
import br.com.cauaqroz.ConectaPlus.model.Channel;
import br.com.cauaqroz.ConectaPlus.model.User;
import br.com.cauaqroz.ConectaPlus.repository.MessageRepository;
import br.com.cauaqroz.ConectaPlus.repository.ChannelRepository;
import br.com.cauaqroz.ConectaPlus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class MessageService implements IMessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger logger = Logger.getLogger(MessageService.class.getName());

    @Override
    public Message sendMessage(String channelId, Message message) throws Exception {
        logger.info("Iniciando envio de mensagem...");
        String senderId = message.getSender();
        Optional<User> user = userRepository.findById(senderId);
        if (user.isPresent()) {
            message.setSenderName(user.get().getName());
        } else {
            logger.severe("Usuário não encontrado: " + senderId);
            throw new Exception("Usuário não encontrado");
        }

        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> {
            logger.severe("Canal não encontrado: " + channelId);
            return new Exception("Canal não encontrado");
        });

        if (!channel.getMasterUserId().equals(senderId) && !channel.getAllowedUserIds().contains(senderId)) {
            logger.severe("Usuário não tem permissão para enviar mensagens neste canal: " + senderId);
            throw new Exception("Usuário não tem permissão para enviar mensagens neste canal.");
        }

        message.setChannelId(channelId);
        messageRepository.save(message);

        if (channel.getMessages() == null) {
            channel.setMessages(new ArrayList<>());
        }
        channel.getMessages().add(message);
        channelRepository.save(channel);

        logger.info("Mensagem enviada com sucesso.");
        return message;
    }

    @Override
    public List<Message> getMessagesByChannelId(String channelId) throws Exception {
        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new Exception("Canal não encontrado"));
        return channel.getMessages();
    }
}