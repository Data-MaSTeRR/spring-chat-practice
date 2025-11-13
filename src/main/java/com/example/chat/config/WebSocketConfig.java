package com.example.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket에 연결할 때 사용할 엔드포인트를 등록합니다.
        // SockJS를 사용하면 WebSocket을 지원하지 않는 브라우저에서도 유사한 경험을 제공할 수 있습니다.
        registry.addEndpoint("/ws-stomp").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 브로커가 /topic 프리픽스를 가진 목적지를 처리하도록 설정합니다.
        // 클라이언트는 이 프리픽스가 붙은 경로를 구독하여 메시지를 수신합니다.
        registry.enableSimpleBroker("/topic");

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 프리픽스를 설정합니다.
        // @MessageMapping 어노테이션이 붙은 메소드가 이 프리픽스가 붙은 경로로 들어오는 메시지를 처리합니다.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
