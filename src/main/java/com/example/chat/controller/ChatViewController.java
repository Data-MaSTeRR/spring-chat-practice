package com.example.chat.controller;

import com.example.chat.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatRoomService chatRoomService;

    /**
     * 루트 페이지 접속 시 채팅방 목록 페이지로 리다이렉트합니다.
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/chat-rooms";
    }

    /**
     * 전체 채팅방 목록 페이지를 반환합니다.
     */
    @GetMapping("/chat-rooms")
    public String chatRooms(Model model) {
        model.addAttribute("rooms", chatRoomService.getAllChatRooms());
        return "rooms";
    }

    /**
     * 채팅방 페이지를 반환합니다.
     * @param roomId 채팅방 ID
     * @param sender 보내는 사람 ID (실제로는 세션 등에서 가져와야 함)
     * @param model 뷰에 전달할 데이터
     * @return "chat" 뷰 (chat.html)
     */
    @GetMapping("/chat-room/{roomId}")
    public String chatRoom(@PathVariable Long roomId, @RequestParam Long sender, Model model) {
        model.addAttribute("roomId", roomId);
        model.addAttribute("sender", sender);
        return "chat";
    }
}
