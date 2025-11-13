package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.service.ChatService;
import com.example.chat.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP 메시지 처리 컨트롤러
 *
 * 목적:
 * - 클라이언트로부터 WebSocket 메시지를 수신
 * - 메시지를 DB에 저장
 * - Kafka를 통해 분산 서버 환경에서 메시지 동기화
 *
 * 메시지 흐름:
 * 1. 클라이언트 → WebSocket (/app/chat/message)
 * 2. ChatService → DB 저장 + 데이터 보완
 * 3. KafkaProducer → Kafka 브로커로 전송
 * 4. KafkaConsumer → 모든 서버 인스턴스가 수신
 * 5. WebSocket Broadcasting → 연결된 클라이언트들에게 전달
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    /**
     * Kafka로 메시지를 전송하는 Producer 서비스
     * 분산 환경에서 다른 서버 인스턴스들과 메시지를 공유하기 위해 사용
     */
    private final KafkaProducerService kafkaProducerService;

    /**
     * 채팅 메시지 비즈니스 로직 처리 서비스
     * - DB 저장
     * - 발신자 이름 조회 및 DTO 보완
     */
    private final ChatService chatService;

    /**
     * WebSocket STOMP 메시지 핸들러
     * 클라이언트가 /app/chat/message로 메시지를 전송하면 호출됨
     *
     * @MessageMapping 설명:
     * - 클라이언트 전송 경로: /app/chat/message
     * - "/app" prefix는 WebSocketConfig에서 설정된 applicationDestinationPrefix
     * - 실제 매핑 경로: "/chat/message"
     *
     * 동작 과정:
     * 1. 클라이언트에서 메시지 전송
     *    예: stompClient.send("/app/chat/message", {}, JSON.stringify(message))
     *
     * 2. ChatService에서 처리
     *    - DB에 메시지 저장
     *    - senderId로 발신자 이름 조회
     *    - 완성된 DTO 반환 (messageId, senderName, createdAt 추가)
     *
     * 3. Kafka로 전송
     *    - "chat-topic" 토픽으로 메시지 발행
     *    - 모든 서버 인스턴스의 KafkaConsumer가 메시지 수신
     *    - 각 서버가 자신의 WebSocket 연결된 클라이언트들에게 브로드캐스트
     *
     * @param message 클라이언트가 전송한 채팅 메시지 DTO
     *                - roomId: 채팅방 ID (필수)
     *                - senderId: 발신자 ID (필수)
     *                - message: 메시지 내용 (필수)
     *                - messageId: null (서버에서 생성)
     *                - senderName: null (서버에서 조회)
     *                - createdAt: null (서버에서 생성)
     *
     * 반환값 없음 (void):
     * - @SendTo를 사용하지 않음 (동적 destination 필요)
     * - Kafka를 통한 비동기 브로드캐스트 방식 사용
     * - KafkaConsumerService가 실제 WebSocket 전송 담당
     *
     * 분산 환경 장점:
     * - 서버 A에 연결된 사용자와 서버 B에 연결된 사용자가 같은 채팅방에서 대화 가능
     * - 서버 증설 시에도 코드 수정 없이 확장 가능
     * - Kafka가 메시지 영속성 보장 (서버 재시작 시에도 메시지 유실 방지)
     *
     * 예외 처리:
     * - DB 저장 실패 시: ChatService에서 예외 발생, Kafka 전송 안 됨
     * - Kafka 전송 실패 시: 비동기이므로 클라이언트는 알 수 없음 (로깅 필요)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {
        // 1단계: DB 저장 + DTO 완성
        // - ChatMessage 엔티티로 변환하여 DB 저장
        // - messageId (PK), createdAt (타임스탬프) 자동 생성
        // - senderId로 UserService 조회하여 senderName 추가
        ChatMessageDto completedMessage = chatService.processAndSaveMessage(message);

        // 2단계: Kafka로 메시지 전송
        // - "chat-topic" 토픽으로 발행
        // - JSON 직렬화되어 Kafka 브로커에 저장
        // - 모든 Consumer Group "chat-group" 인스턴스 중 하나가 수신
        // - 수신한 서버가 WebSocket으로 브로드캐스트
        kafkaProducerService.sendMessage(completedMessage);
    }

}
