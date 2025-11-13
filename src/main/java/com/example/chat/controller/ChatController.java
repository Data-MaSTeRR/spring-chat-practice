package com.example.chat.controller;

import com.example.chat.dto.ChatMessageDto;
import com.example.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate; // SimpMessagingTemplate 추가
import org.springframework.stereotype.Controller;

/**
 * WebSocket STOMP 메시지 처리 컨트롤러
 *
 * 목적:
 * - 클라이언트로부터 WebSocket 메시지를 수신
 * - 메시지를 DB에 저장
 * - RabbitMQ를 통해 분산 서버 환경에서 메시지 동기화 및 브로드캐스트
 *
 * 메시지 흐름:
 * 1. 클라이언트 → WebSocket (/app/chat/message)
 * 2. ChatService → DB 저장 + 데이터 보완
 * 3. SimpMessagingTemplate → RabbitMQ (STOMP Broker Relay)로 전송
 * 4. RabbitMQ → 모든 서버 인스턴스에게 메시지 분배
 * 5. 각 서버는 자신에게 연결된 클라이언트들에게 WebSocket으로 메시지 전달
 */
@Controller
@RequiredArgsConstructor
public class ChatController {

    /**
     * STOMP 메시지를 WebSocket 클라이언트 또는 외부 브로커로 전송하는 템플릿
     * RabbitMQ (STOMP Broker Relay)를 통해 메시지를 브로드캐스트하는 데 사용
     */
    private final SimpMessagingTemplate template; // SimpMessagingTemplate 주입

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
     * 3. RabbitMQ로 브로드캐스트
     *    - SimpMessagingTemplate을 사용하여 "/topic/chat/room/{roomId}" 목적지로 메시지 전송
     *    - WebSocketConfig에 설정된 StompBrokerRelay를 통해 RabbitMQ로 메시지가 중계됨
     *    - RabbitMQ는 해당 토픽을 구독 중인 모든 서버 인스턴스에게 메시지를 분배
     *    - 각 서버는 자신에게 연결된 WebSocket 클라이언트들에게 메시지를 전달
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
     * - 메시지는 SimpMessagingTemplate을 통해 비동기적으로 RabbitMQ로 전달되고,
     *   RabbitMQ를 통해 모든 구독자에게 브로드캐스트됩니다.
     *
     * 분산 환경 장점:
     * - 서버 A에 연결된 사용자와 서버 B에 연결된 사용자가 같은 채팅방에서 대화 가능
     * - 서버 증설 시에도 코드 수정 없이 확장 가능
     * - RabbitMQ가 메시지 라우팅과 브로드캐스트를 담당하여 서버 간 동기화 문제 해결
     *
     * 예외 처리:
     * - DB 저장 실패 시: ChatService에서 예외 발생, 메시지 브로드캐스트 안 됨
     * - RabbitMQ 전송 실패 시: 비동기이므로 클라이언트는 알 수 없음 (로깅 및 재시도 로직 필요)
     */
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {
        // 1단계: DB 저장 + DTO 완성
        ChatMessageDto completedMessage = chatService.processAndSaveMessage(message);

        // 2단계: RabbitMQ (STOMP Broker Relay)를 통해 메시지 브로드캐스트
        // - "/topic/chat.room.{roomId}" 목적지로 메시지 전송
        //   (RabbitMQ STOMP는 routing key에 슬래시(/) 대신 점(.)을 사용)
        // - WebSocketConfig에 설정된 StompBrokerRelay가 이 메시지를 RabbitMQ로 중계
        // - RabbitMQ는 이 메시지를 구독 중인 모든 서버 인스턴스에게 전달
        template.convertAndSend("/topic/chat.room." + completedMessage.getRoomId(), completedMessage);
    }

}
