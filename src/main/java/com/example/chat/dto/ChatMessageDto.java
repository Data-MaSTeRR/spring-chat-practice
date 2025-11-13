package com.example.chat.dto;

import com.example.chat.domain.ChatMessage;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 채팅 메시지 데이터 전송 객체 (DTO)
 *
 * 목적:
 * - WebSocket 메시지 송수신 시 사용
 * - Kafka 메시지 직렬화/역직렬화
 * - REST API 응답 데이터 전달
 * - 엔티티와 클라이언트 간 데이터 변환 계층 제공
 *
 * Serializable 구현 이유:
 * - Kafka를 통한 네트워크 전송을 위해 직렬화 필요
 * - Redis 캐싱 등 외부 저장소 사용 시 필요
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto implements Serializable {

    /**
     * 메시지 고유 ID
     * DB에 저장된 후 자동 생성되는 값
     */
    private Long messageId;

    /**
     * 채팅방 ID
     * 어떤 채팅방의 메시지인지 구분하는 식별자
     */
    private Long roomId;

    /**
     * 발신자 ID
     * 메시지를 보낸 사용자의 고유 ID
     */
    private Long senderId;

    /**
     * 발신자 이름
     * 클라이언트에서 발신자 정보를 표시하기 위한 필드
     * 엔티티에는 없지만 DTO에서 추가로 제공
     */
    private String senderName;

    /**
     * 메시지 내용
     * 사용자가 입력한 실제 채팅 메시지 텍스트
     */
    private String message;

    /**
     * 메시지 생성 시간
     * DB에서 자동으로 기록된 타임스탬프
     */
    private LocalDateTime createdAt;

    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     *
     * Entity -> DTO 변환 시 사용하며,
     * 엔티티에 없는 발신자 이름 정보를 추가로 주입받아 완성된 DTO를 생성
     *
     * @param entity ChatMessage 엔티티 (DB에서 조회한 메시지)
     * @param senderName 발신자 이름 (UserService에서 조회한 값)
     * @return 완성된 ChatMessageDto 객체
     *
     * 사용 예시:
     * ChatMessage entity = chatMessageRepository.findById(1L);
     * String senderName = userService.getUserName(entity.getSenderId());
     * ChatMessageDto dto = ChatMessageDto.from(entity, senderName);
     */
    public static ChatMessageDto from(ChatMessage entity, String senderName) {
        return ChatMessageDto.builder()
                .messageId(entity.getId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .senderName(senderName)
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}
