package com.example.chat.repository;

import com.example.chat.domain.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 채팅방의 메시지 조회 (최신순, 페이징)
     * @param roomId
     * @param pageable
     * @return
     */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    /**
     * 특정 채팅방의 최신 메시지 단건 조회
     * @param roomId
     * @return
     */
    ChatMessage findTopByRoomIdOrderByCreatedAtDesc(Long roomId);

}
