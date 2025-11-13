package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.ChatRoomDto;
import com.example.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Rest API용 컨트롤러
 */

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     * 채팅방 생성
     * @param hostId
     * @param guestId
     * @return
     */
    @ResponseStatus(code = HttpStatus.CREATED)
    @PostMapping("/rooms")
    public ChatRoomDto creatRoom(@RequestParam Long hostId,
                                 @RequestParam Long guestId) {
        return chatRoomService.createRoom(hostId, guestId);
    }

    /**
     * 과거 메시지 조회
     * @param roomId
     * @return
     */
    @ResponseStatus(code = HttpStatus.OK)
    @GetMapping("/rooms/{roomId}/messages")
    public List<ChatMessageDto> getMessages(@PathVariable Long roomId,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return chatRoomService.getMessages(roomId, page, size);
    }

    /**
     * 채팅방 목록 조회
     * @param userId
     * @return
     */
    @ResponseStatus(code = HttpStatus.OK)
    @GetMapping("/rooms/my")
    public List<ChatRoomDto> getMyRooms(@RequestParam Long userId) {
        return chatRoomService.getMyChatRooms(userId);
    }

}
