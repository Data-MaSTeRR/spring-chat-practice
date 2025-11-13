package com.example.chat.service.kafka;

import com.example.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka Producer 서비스
 * 채팅 메시지를 Kafka 토픽으로 전송하는 역할을 담당
 *
 * 목적:
 * - WebSocket 메시지를 Kafka 브로커로 발행
 * - 분산 시스템 간 메시지 동기화
 * - 서버 확장성 및 메시지 영속성 보장
 */
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    /**
     * Kafka 메시지 전송을 위한 고수준 API
     * KafkaConfig에서 설정된 KafkaTemplate 빈을 주입받음
     *
     * 제네릭 타입:
     * - Key: String (메시지 키, 파티셔닝에 사용)
     * - Value: ChatMessageDto (전송할 메시지 객체)
     */
    private final KafkaTemplate<String, ChatMessageDto> kafkaTemplate;

    /**
     * 채팅 메시지를 Kafka 토픽으로 전송
     *
     * 동작 흐름:
     * 1. ChatMessageDto 객체를 JSON으로 직렬화
     * 2. "chat-topic" 토픽으로 메시지 전송
     * 3. Kafka 브로커가 메시지를 저장하고 컨슈머들에게 전달
     *
     * @param message 전송할 채팅 메시지 DTO
     *                - roomId: 채팅방 ID
     *                - senderId: 발신자 ID
     *                - senderName: 발신자 이름
     *                - message: 메시지 내용
     *                - createdAt: 메시지 생성 시간
     *
     * 특징:
     * - 비동기 전송: 메서드 호출 즉시 반환
     * - Fire-and-forget 방식
     *
     * 주의사항:
     * - "chat-topic" 토픽이 미리 생성되어 있어야 함
     */
    public void sendMessage(ChatMessageDto message) {
        kafkaTemplate.send("chat-topic", message);
    }
}
