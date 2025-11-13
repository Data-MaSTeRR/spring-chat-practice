package com.example.chat.config;

import com.example.chat.dto.ChatMessageDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 메시지 브로커 설정 클래스
 * - Producer: 채팅 메시지를 Kafka로 전송
 * - Consumer: Kafka에서 메시지를 수신하여 처리
 */
@Configuration
public class KafkaConfig {

    /**
     * Kafka 브로커 서버 주소
     * application.yml에서 spring.kafka.bootstrap-servers 값을 주입
     * 예: localhost:9092
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafka Producer 설정을 담당하는 Factory Bean
     *
     * @return ProducerFactory<String, ChatMessageDto>
     *         - Key: String 타입 (예: 채팅방 ID)
     *         - Value: ChatMessageDto 객체 (채팅 메시지)
     */
    @Bean
    public ProducerFactory<String, ChatMessageDto> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Kafka 브로커 주소 설정
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Key 직렬화: String을 바이트 배열로 변환
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Value 직렬화: ChatMessageDto 객체를 JSON으로 변환
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka 메시지 전송을 위한 고수준 API 제공
     * Service 계층에서 주입받아 사용
     *
     * 사용 예시:
     * kafkaTemplate.send("chat-messages", messageDto);
     *
     * @return KafkaTemplate<String, ChatMessageDto>
     */
    @Bean
    public KafkaTemplate<String, ChatMessageDto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Kafka Consumer 설정을 담당하는 Factory Bean
     * Kafka 토픽에서 메시지를 수신하고 역직렬화
     *
     * @return ConsumerFactory<String, ChatMessageDto>
     *         - Key: String 타입
     *         - Value: ChatMessageDto 객체
     */
    @Bean
    public ConsumerFactory<String, ChatMessageDto> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Kafka 브로커 주소 설정
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Consumer Group ID: 같은 그룹의 컨슈머들이 메시지를 분산 처리
        // 여러 서버 인스턴스가 동일한 그룹 ID로 실행되면 부하 분산됨
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-group");

        // Key 역직렬화: 바이트 배열을 String으로 변환
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Value 역직렬화: JSON을 ChatMessageDto 객체로 변환
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // 신뢰할 수 있는 패키지 설정 (보안)
        // "*"는 모든 패키지 허용 (개발 환경)
        // 프로덕션에서는 "com.example.chat.dto"처럼 명시적으로 지정 권장
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // ConsumerFactory 생성 시 Deserializer 인스턴스와 타입 정보 제공
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),                    // Key Deserializer
                new JsonDeserializer<>(ChatMessageDto.class) // Value Deserializer (타입 명시)
        );
    }

    /**
     * @KafkaListener 어노테이션을 사용하는 리스너 컨테이너 Factory
     * 메시지 수신 및 병렬 처리를 담당
     *
     * 사용 예시:
     * {@code
     * @KafkaListener(topics = "chat-messages", groupId = "chat-group")
     * public void listen(ChatMessageDto message) {
     *     // 메시지 처리 로직
     * }
     * }
     *
     * @return ConcurrentKafkaListenerContainerFactory<String, ChatMessageDto>
     *         - 동시성 제어를 통한 병렬 메시지 처리 지원
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageDto> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ChatMessageDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // ConsumerFactory 설정 주입
        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}
