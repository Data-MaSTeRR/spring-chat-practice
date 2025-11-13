package com.example.chat.service;

import com.example.chat.domain.ChatMessage;
import com.example.chat.dto.ChatMessageDto;
import com.example.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 메시지 비즈니스 로직 처리 서비스
 *
 * 주요 책임:
 * - 채팅 메시지를 DB에 영속화
 * - DTO와 엔티티 간 변환 처리
 * - 발신자 정보 보완 (senderName 조회 및 추가)
 * - 트랜잭션 관리
 *
 * 트랜잭션 설정:
 * - 클래스 레벨: @Transactional(readOnly = true) - 기본 읽기 전용
 * - 메서드 레벨: @Transactional - 쓰기 작업 시 오버라이드
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    /**
     * 채팅 메시지 데이터 접근 계층
     * JPA Repository를 통한 CRUD 작업
     */
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 사용자 정보 조회 서비스
     * 발신자 ID로 사용자 이름을 조회하는데 사용
     */
    private final UserService userService;

    /**
     * 채팅 메시지 저장 및 완성된 DTO 반환 (통합 처리)
     *
     * 처리 흐름:
     * 1. 클라이언트로부터 받은 DTO를 엔티티로 변환하여 DB 저장
     * 2. 저장된 엔티티에서 자동 생성된 값 획득 (messageId, createdAt)
     * 3. 발신자 이름을 UserService에서 조회
     * 4. 모든 정보가 포함된 완성된 DTO 생성 및 반환
     *
     * @Transactional 설명:
     * - readOnly = false (쓰기 가능)
     * - DB 저장 작업과 조회 작업을 하나의 트랜잭션으로 묶음
     * - 실패 시 자동 롤백 보장
     *
     * @param dto 클라이언트로부터 받은 메시지 DTO
     *            - roomId: 채팅방 ID (필수)
     *            - senderId: 발신자 ID (필수)
     *            - message: 메시지 내용 (필수)
     *            - messageId: null (DB에서 생성)
     *            - senderName: null (서비스에서 조회)
     *            - createdAt: null (DB에서 생성)
     *
     * @return 완성된 ChatMessageDto 객체
     *         - messageId: DB에서 자동 생성된 PK
     *         - roomId: 요청 값 그대로
     *         - senderId: 요청 값 그대로
     *         - senderName: UserService에서 조회한 발신자 이름
     *         - message: 요청 값 그대로
     *         - createdAt: DB에서 @CreationTimestamp로 자동 생성
     *
     * 사용 예시:
     * ChatMessageDto inputDto = new ChatMessageDto();
     * inputDto.setRoomId(1L);
     * inputDto.setSenderId(2L);
     * inputDto.setMessage("안녕하세요");
     *
     * ChatMessageDto result = chatService.processAndSaveMessage(inputDto);
     * // result.getMessageId() -> 123 (DB 생성)
     * // result.getSenderName() -> "user2" (조회됨)
     * // result.getCreatedAt() -> 2025-11-13T14:30:00 (DB 생성)
     *
     * 예외 처리:
     * - 발신자가 존재하지 않을 경우: UserService에서 예외 발생
     * - DB 저장 실패: DataAccessException 발생 및 트랜잭션 롤백
     */
    @Transactional
    public ChatMessageDto processAndSaveMessage(ChatMessageDto dto) {
        // 1단계: 메시지를 DB에 저장
        // - DTO -> 엔티티 변환
        // - Repository를 통해 DB INSERT
        // - 저장된 엔티티 반환 (ID와 타임스탬프 포함)
        ChatMessage saved = saveMessage(dto);

        // 2단계: 발신자 이름 조회
        // - senderId로 User 테이블에서 이름 조회
        // - 엔티티에는 ID만 저장되지만, DTO에는 이름도 포함시킴
        // - 클라이언트에서 "user1이 메시지를 보냈습니다" 형태로 표시하기 위함
        String senderName = userService.getUserName(dto.getSenderId());

        // 3단계: 완성된 DTO 반환
        // - 원본 dto를 수정하지 않고 새로운 객체 생성 (불변성 유지)
        // - 정적 팩토리 메서드 from() 사용
        // - DB에서 생성된 값들(messageId, createdAt)과 조회한 senderName 모두 포함
        return ChatMessageDto.from(saved, senderName);
    }

    /**
     * 채팅 메시지를 DB에 저장 (내부 헬퍼 메서드)
     *
     * 동작:
     * - DTO의 필수 정보(roomId, senderId, message)를 엔티티로 변환
     * - JPA를 통해 DB INSERT 실행
     * - 엔티티의 @GeneratedValue로 ID 자동 생성
     * - @CreationTimestamp로 생성 시간 자동 기록
     *
     * @Transactional 설명:
     * - 쓰기 작업이므로 readOnly = false
     * - processAndSaveMessage()에서 호출되므로 같은 트랜잭션에 참여
     * - 독립적으로 호출될 수도 있으므로 별도 @Transactional 선언
     *
     * @param dto 저장할 메시지 정보가 담긴 DTO
     * @return 저장된 ChatMessage 엔티티 (ID와 createdAt 포함)
     *
     * DTO -> Entity 수동 변환 이유:
     * - 모든 필드를 복사하지 않음 (필수 필드만 설정)
     * - messageId, createdAt은 DB에서 자동 생성
     * - senderName은 엔티티에 없는 필드 (DTO 전용)
     *
     * 참고: Builder 패턴 사용 가능
     * return chatMessageRepository.save(
     *     ChatMessage.builder()
     *         .roomId(dto.getRoomId())
     *         .senderId(dto.getSenderId())
     *         .message(dto.getMessage())
     *         .build()
     * );
     */
    @Transactional
    public ChatMessage saveMessage(ChatMessageDto dto) {
        // 새 엔티티 객체 생성
        ChatMessage message = new ChatMessage();

        // DTO에서 필수 필드만 설정
        message.setRoomId(dto.getRoomId());
        message.setSenderId(dto.getSenderId());
        message.setMessage(dto.getMessage());

        // ID와 createdAt은 설정하지 않음 (자동 생성)

        // JPA를 통해 DB에 저장 및 영속화된 엔티티 반환
        return chatMessageRepository.save(message);
    }

}
