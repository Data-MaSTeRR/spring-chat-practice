package com.example.chat.service;

import com.example.chat.domain.ChatMessage;
import com.example.chat.domain.ChatRoom;
import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.ChatRoomDto;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserService userService;

    // 채팅방 생성 (중복 체크 포함)
    @Transactional
    public ChatRoomDto createRoom(Long hostId, Long guestId) {
        // 이미 존재하는 방 체크
        return chatRoomRepository.findByTwoUsers(hostId, guestId)
                .map(room -> convertToDto(room))
                .orElseGet(() -> {
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setHostId(hostId);
                    newRoom.setGuestId(guestId);
                    ChatRoom saved = chatRoomRepository.save(newRoom);
                    return convertToDto(saved);
                });
    }

    // 과거 메시지 조회
    public List<ChatMessageDto> getMessages(Long roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> messages = chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        return messages.stream()
                .map(msg -> {
                    String senderName = userService.getUserName(msg.getSenderId());
                    return ChatMessageDto.from(msg, senderName);
                })
                .collect(Collectors.toList());
    }

    // 내 채팅방 목록
    public List<ChatRoomDto> getMyChatRooms(Long userId) {
        List<ChatRoom> rooms = chatRoomRepository
                .findByHostIdOrGuestId(userId, userId);

        return rooms.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 모든 채팅방 목록
    public List<ChatRoomDto> getAllChatRooms() {
        return chatRoomRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ChatRoomDto convertToDto(ChatRoom room) {
        String hostName = userService.getUserName(room.getHostId());
        String guestName = userService.getUserName(room.getGuestId());
        return ChatRoomDto.from(room, hostName, guestName);
    }
}
