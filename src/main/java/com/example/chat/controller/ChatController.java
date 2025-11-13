package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket용 컨트롤러
 */

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate template;
    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {
        // Service에서 저장 + DTO 완성을 한번에 처리
        ChatMessageDto completedMessage = chatService.processAndSaveMessage(message);

        // Controller는 브로드캐스트만 담당
        template.convertAndSend("/topic/chat/room/" + completedMessage.getRoomId(), completedMessage);
    }

}
