package com.example.chat.service;

import com.example.chat.domain.ChatMessage;
import com.example.chat.dto.ChatMessageDto;
import com.example.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;

    @Transactional
    public ChatMessageDto processAndSaveMessage(ChatMessageDto dto) {
        // 1. 메시지 저장
        ChatMessage saved = saveMessage(dto);

        // 2. 발신자 이름 조회
        String senderName = userService.getUserName(dto.getSenderId());

        // 3. 완성된 DTO 반환 (원본 수정하지 않고 새로 생성)
        return ChatMessageDto.from(saved, senderName);
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessageDto dto) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(dto.getRoomId());
        message.setSenderId(dto.getSenderId());
        message.setMessage(dto.getMessage());

        return chatMessageRepository.save(message);
    }

}
