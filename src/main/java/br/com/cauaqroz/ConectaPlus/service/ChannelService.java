package br.com.cauaqroz.ConectaPlus.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.cauaqroz.ConectaPlus.model.Channel;
import br.com.cauaqroz.ConectaPlus.repository.ChannelRepository;

public class ChannelService {
    private final ChannelRepository channelRepository;

    public ChannelService(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    public Channel createChannel(String masterUserId) {
        Channel newChannel = new Channel();
        newChannel.setMasterUserId(masterUserId);
        newChannel.setAllowedUserIds(new ArrayList<>(Collections.singletonList(masterUserId))); // Inicializa a lista com o masterUserId
        channelRepository.save(newChannel);
        return newChannel;
    }

    public void addUserToChannel(String channelId, String userId) {
        Channel channel = channelRepository.findById(channelId).orElse(null);
        if (channel != null) {
            List<String> allowedUserIds = channel.getAllowedUserIds();
            if (!allowedUserIds.contains(userId)) {
                allowedUserIds.add(userId);
                channel.setAllowedUserIds(allowedUserIds);
                channelRepository.save(channel);
            }
        }
    }

    public boolean isUserAllowedInChannel(String channelId, String userId) {
        Channel channel = channelRepository.findById(channelId).orElse(null);
        if (channel != null) {
            return channel.getAllowedUserIds().contains(userId);
        }
        return false;
    }
}