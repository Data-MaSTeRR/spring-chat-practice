package com.example.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 *
 * 목적:
 * - STOMP 프로토콜을 사용하는 WebSocket 메시지 브로커 구성
 * - 클라이언트-서버 간 실시간 양방향 통신 활성화
 * - SockJS를 통한 WebSocket 폴백 지원
 *
 * STOMP (Simple Text Oriented Messaging Protocol):
 * - WebSocket 위에서 동작하는 메시징 프로토콜
 * - pub/sub 패턴을 지원하여 메시지 라우팅이 간편
 * - 텍스트 기반 프로토콜로 디버깅이 용이
 *
 * 메시지 흐름:
 * 1. 클라이언트가 /ws-stomp 엔드포인트로 WebSocket 연결
 * 2. 클라이언트가 /app/* 경로로 메시지 전송 (예: /app/chat/message)
 * 3. @MessageMapping이 붙은 컨트롤러 메서드가 메시지 처리
 * 4. 서버가 /topic/* 경로로 메시지 브로드캐스트 (예: /topic/chat/room/1)
 * 5. 해당 경로를 구독 중인 클라이언트들이 메시지 수신
 */
@Configuration
@EnableWebSocketMessageBroker  // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * WebSocket 연결 엔드포인트 등록
     *
     * 클라이언트가 WebSocket 서버에 연결할 때 사용하는 URL 경로를 설정
     *
     * @param registry STOMP 엔드포인트 레지스트리
     *
     * 설정 내용:
     * - 엔드포인트: /ws-stomp
     *   클라이언트는 ws://localhost:8080/ws-stomp로 연결
     *
     * - setAllowedOriginPatterns("*")
     *   모든 Origin에서의 접근 허용 (CORS 설정)
     *   프로덕션에서는 특정 도메인만 허용하도록 변경 권장
     *   예: .setAllowedOrigins("https://example.com")
     *
     * - withSockJS()
     *   SockJS 프로토콜 활성화
     *   WebSocket을 지원하지 않는 브라우저에서 폴백 메커니즘 제공
     *   (폴링, 롱폴링, 스트리밍 등)
     *
     * 클라이언트 연결 예시 (JavaScript):
     * const socket = new SockJS('http://localhost:8080/ws-stomp');
     * const stompClient = Stomp.over(socket);
     * stompClient.connect({}, function(frame) {
     *     console.log('Connected: ' + frame);
     * });
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")  // CORS 허용 (개발 환경)
                .withSockJS();                  // SockJS 폴백 지원
    }

    /**
     * 메시지 브로커 설정
     *
     * STOMP 메시지 라우팅 규칙과 브로커 설정.
     * RabbitMQ를 외부 STOMP 브로커로 사용하여 여러 서버 인스턴스 간 메시지 공유를 가능하게 합니다.
     *
     * @param registry 메시지 브로커 레지스트리
     *
     * 설정 내용:
     * 1. enableStompBrokerRelay("/topic")
     *    - 외부 STOMP 브로커(RabbitMQ)를 사용하여 메시지 라우팅 활성화
     *    - "/topic"으로 시작하는 destination을 RabbitMQ가 처리
     *    - 클라이언트가 구독(subscribe)하는 경로
     *    - pub/sub 패턴으로 1:N 메시지 전달
     *
     *    - setRelayHost("localhost") 및 setRelayPort(61613)
     *      RabbitMQ STOMP 어댑터의 호스트와 포트 설정.
     *      docker-compose.yml에 정의된 RabbitMQ 서비스의 STOMP 포트(61613)와 일치해야 합니다.
     *
     *    사용 예시:
     *    - 서버: template.convertAndSend("/topic/chat/room/1", message)
     *    - 클라이언트: stompClient.subscribe("/topic/chat/room/1", callback)
     *
     * 2. setApplicationDestinationPrefixes("/app")
     *    - 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix
     *    - "/app"으로 시작하는 경로는 @MessageMapping 컨트롤러로 라우팅
     *    - 서버에서 메시지를 처리한 후 응답하는 요청-응답 패턴
     *
     *    사용 예시:
     *    - 클라이언트: stompClient.send("/app/chat/message", {}, messageBody)
     *    - 서버: @MessageMapping("/chat/message")가 처리
     *
     * 메시지 경로 구분:
     * - /app/*  : 클라이언트 → 서버 (요청)
     * - /topic/* : 서버 → 클라이언트 (브로드캐스트, RabbitMQ를 통해)
     * - /queue/* : 1:1 메시지 (개인 메시지, RabbitMQ를 통해)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 외부 STOMP 브로커(RabbitMQ)를 사용하여 메시지 라우팅
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("guest")    // 클라이언트 연결 시 RabbitMQ 사용자 이름
                .setClientPasscode("guest") // 클라이언트 연결 시 RabbitMQ 비밀번호
                .setSystemLogin("guest")    // 시스템 내부 연결 시 RabbitMQ 사용자 이름
                .setSystemPasscode("guest") // 시스템 내부 연결 시 RabbitMQ 비밀번호
                .setVirtualHost("/");       // RabbitMQ virtual host 설정

        // 애플리케이션 destination prefix 설정 (전송 경로)
        registry.setApplicationDestinationPrefixes("/app");
    }
}
