package com.umutyenidil.springwhatsapp.message;

import com.umutyenidil.springwhatsapp.chat.Chat;
import com.umutyenidil.springwhatsapp.chat.ChatRepository;
import com.umutyenidil.springwhatsapp.file.FileService;
import com.umutyenidil.springwhatsapp.file.FileUtils;
import com.umutyenidil.springwhatsapp.notification.Notification;
import com.umutyenidil.springwhatsapp.notification.NotificationService;
import com.umutyenidil.springwhatsapp.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    final MessageRepository messageRepository;
    final ChatRepository chatRepository;
    final MessageMapper messageMapper;
    final FileService fileService;
    final NotificationService notificationService;

    public void saveMessage(
            MessageRequest messageRequest
    ) {
        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        Message message = new Message();
        message.setContent(messageRequest.getContent());
        message.setChat(chat);
        message.setSenderId(messageRequest.getSenderId());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setType(messageRequest.getType());
        message.setState(MessageState.SENT);
        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .messageType(messageRequest.getType())
                .content(messageRequest.getContent())
                .senderId(messageRequest.getSenderId())
                .receiverId(messageRequest.getReceiverId())
                .type(NotificationType.MESSAGE)
                .chatName(chat.getChatName(message.getSenderId()))
                .build();

        notificationService.sendNotification(
                message.getReceiverId(),
                notification
        );
    }

    public List<MessageResponse> findChatMessages(
            String chatId
    ) {
        return messageRepository.findMessagesByChatId(chatId)
                .stream()
                .map(messageMapper::toMessageResponse)
                .toList();
    }

    @Transactional
    public void setMessagesToSeen(
            String chatId,
            Authentication authentication
    ) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        final String recipientId = getRecipientId(chat, authentication);

        messageRepository.setMessagesToSeenByChat(chatId, MessageState.SEEN);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .senderId(getSenderId(chat, authentication))
                .receiverId(recipientId)
                .type(NotificationType.SEEN)
                .build();

        notificationService.sendNotification(
                recipientId,
                notification
        );
    }

    public void uploadMediaMessage(
            String chatId,
            MultipartFile file,
            Authentication authentication
    ) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        final String senderId = getSenderId(chat, authentication);
        final String recipientId = getRecipientId(chat, authentication);

        final String filePath = fileService.saveFile(file, senderId);

        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setReceiverId(recipientId);
        message.setType(MessageType.IMAGE);
        message.setState(MessageState.SENT);
        message.setMediaFilePath(filePath);
        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .messageType(MessageType.IMAGE)
                .senderId(senderId)
                .receiverId(recipientId)
                .type(NotificationType.IMAGE)
                .media(FileUtils.readFileFromLocation(filePath))
                .build();

        notificationService.sendNotification(
                recipientId,
                notification
        );
    }

    private String getSenderId(
            Chat chat,
            Authentication authentication
    ) {
        if (chat.getSender().getId().equals(authentication.getName())) {
            return chat.getSender().getId();
        }

        return chat.getRecipient().getId();
    }

    private String getRecipientId(
            Chat chat,
            Authentication authentication
    ) {
        if (chat.getSender().getId().equals(authentication.getName())) return chat.getRecipient().getId();

        return chat.getSender().getId();
    }
}
