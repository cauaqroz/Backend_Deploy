package br.com.cauaqroz.ConectaPlus.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Message {
    //Conteudo da Mensagem
    private String content;

    //Usuario que Esta Enviando a Mensagem
    private String sender;

    //Id do Canal, para saber a qual canal a mensagem pertence
    private String channelId;

    //Data e Horda de Criação da Mensagem
    @CreatedDate
    private LocalDateTime createdDate;
    
    private String senderName;

    //Getter e Setter  para o Id do Canal
    public String getChannelId() {
        return channelId;
    }
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    //Getter e Setter  para o Conteudo
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    //Getter e Setter  para o Remetente
    public String getSender() {
        return sender;
    }
    public void setSender(String sender) {
        this.sender = sender;
    }

    //Getter para a Data de Criação
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}


