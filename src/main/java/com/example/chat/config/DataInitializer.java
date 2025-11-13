package com.example.chat.config;

import com.example.chat.domain.ChatMessage;
import com.example.chat.domain.ChatRoom;
import com.example.chat.domain.User;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ChatRoomRepository;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 더미 사용자 생성
        User user1 = new User(null, "user1");
        User user2 = new User(null, "user2");
        User user3 = new User(null, "user3");

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);

        // 더미 채팅방 생성
        ChatRoom room1 = new ChatRoom();
        room1.setHostId(user1.getId());
        room1.setGuestId(user2.getId());
        chatRoomRepository.save(room1);

        ChatRoom room2 = new ChatRoom();
        room2.setHostId(user1.getId());
        room2.setGuestId(user3.getId());
        chatRoomRepository.save(room2);

        // 더미 메시지 생성 (room1)
        ChatMessage message1 = new ChatMessage();
        message1.setRoomId(room1.getId());
        message1.setSenderId(user1.getId());
        message1.setMessage("안녕하세요, user2님!");
        chatMessageRepository.save(message1);

        ChatMessage message2 = new ChatMessage();
        message2.setRoomId(room1.getId());
        message2.setSenderId(user2.getId());
        message2.setMessage("네, 안녕하세요 user1님. 반갑습니다.");
        chatMessageRepository.save(message2);
    }
}
