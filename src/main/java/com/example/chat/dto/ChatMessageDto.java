package com.example.chat.dto;

import com.example.chat.domain.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {

    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private String message;
    private LocalDateTime createdAt;

    public static ChatMessageDto from(ChatMessage entity, String senderName) {
        return ChatMessageDto.builder()
                .messageId(entity.getId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .senderName(senderName)
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
