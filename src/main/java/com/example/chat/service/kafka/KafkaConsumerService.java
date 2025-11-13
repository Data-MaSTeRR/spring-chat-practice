package com.example.chat.service.kafka;

import com.example.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer 서비스
 * Kafka 토픽에서 채팅 메시지를 수신하여 WebSocket으로 브로드캐스트
 *
 * 목적:
 * - Kafka에서 메시지를 실시간으로 수신
 * - 분산 서버 환경에서 메시지 동기화
 * - WebSocket을 통해 연결된 클라이언트들에게 메시지 전달
 *
 * 작동 방식:
 * 1. Kafka 토픽 "chat-topic"을 구독
 * 2. 새 메시지 수신 시 receiveMessage() 자동 호출
 * 3. 해당 채팅방의 WebSocket 구독자들에게 브로드캐스트
 */
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    /**
     * WebSocket 메시지 전송을 위한 템플릿
     * Spring WebSocket의 SimpMessagingTemplate을 주입받아
     * 특정 destination(채팅방)으로 메시지 브로드캐스트
     */
    private final SimpMessagingTemplate template;

    /**
     * Kafka 토픽에서 채팅 메시지를 수신하는 리스너
     *
     * 동작 흐름:
     * 1. Kafka 브로커의 "chat-topic" 토픽 모니터링
     * 2. 새 메시지 발행 시 자동으로 이 메서드 호출
     * 3. JSON을 ChatMessageDto로 자동 역직렬화 (KafkaConfig 설정)
     * 4. WebSocket을 통해 해당 채팅방 구독자들에게 전달
     *
     * @KafkaListener 설정:
     * - topics: "chat-topic" - 구독할 Kafka 토픽 이름
     * - groupId: "chat-group" - Consumer Group ID
     *   같은 그룹의 컨슈머들은 메시지를 분산 처리
     *   (서버 인스턴스가 여러 개여도 메시지는 한 번만 처리)
     *
     * @param message Kafka에서 수신한 채팅 메시지 DTO
     *                - roomId: 메시지가 속한 채팅방 ID
     *                - senderId: 발신자 ID
     *                - senderName: 발신자 이름
     *                - message: 메시지 내용
     *                - createdAt: 생성 시간
     *
     * WebSocket 전송:
     * - destination: /topic/chat/room/{roomId}
     * - 해당 채팅방을 구독 중인 모든 클라이언트가 수신
     *
     * 분산 환경 동작:
     * - 서버 A, B, C가 같은 "chat-group"으로 실행 중일 때
     * - Kafka 메시지는 A, B, C 중 하나의 서버만 수신
     * - 수신한 서버가 자신의 WebSocket 연결된 클라이언트들에게 전달
     * - 결과적으로 모든 클라이언트가 메시지 수신 (서버 간 동기화)
     *
     * 사용 예시:
     * 1. Producer가 Kafka로 메시지 전송
     * 2. 이 Consumer가 메시지 수신
     * 3. WebSocket으로 브로드캐스트
     * 4. 클라이언트 화면에 메시지 표시
     */
    @KafkaListener(topics = "chat-topic", groupId = "chat-group")
    public void receiveMessage(ChatMessageDto message) {
        // WebSocket destination 동적 생성
        // 예: /topic/chat/room/1, /topic/chat/room/2 등
        template.convertAndSend(
                "/topic/chat/room/" + message.getRoomId(),
                message
        );
    }
}
