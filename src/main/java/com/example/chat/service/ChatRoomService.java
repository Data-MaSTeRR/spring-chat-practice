package com.example.chat.service;

import com.example.chat.domain.ChatMessage;
import com.example.chat.domain.ChatRoom;
import com.example.chat.dto.ChatMessageDto;
import com.example.chat.dto.ChatRoomDto;
import com.example.chat.repository.ChatMessageRepository;
import com.example.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅방 관리 비즈니스 로직 서비스
 *
 * 주요 책임:
 * - 채팅방 생성 및 중복 체크
 * - 채팅방 목록 조회
 * - 채팅방의 과거 메시지 조회 (페이징 지원)
 * - 엔티티와 DTO 간 변환
 *
 * 트랜잭션 설정:
 * - 클래스 레벨: @Transactional(readOnly = true) - 기본 읽기 전용
 * - 메서드 레벨: @Transactional - 쓰기 작업 시 오버라이드
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    /**
     * 채팅방 데이터 접근 계층
     * JPA Repository를 통한 ChatRoom 엔티티 CRUD
     */
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 채팅 메시지 데이터 접근 계층
     * 채팅방의 과거 메시지 조회에 사용
     */
    private final ChatMessageRepository chatMessageRepository;

    /**
     * 사용자 정보 조회 서비스
     * 채팅방의 호스트 및 게스트 이름 조회에 사용
     */
    private final UserService userService;

    /**
     * 채팅방 생성 (중복 체크 포함)
     *
     * 동작:
     * 1. 두 사용자 간에 이미 채팅방이 있는지 확인
     * 2. 있으면 기존 채팅방 정보 반환
     * 3. 없으면 새 채팅방 생성 후 반환
     *
     * 중복 체크 로직:
     * - findByTwoUsers(hostId, guestId)는 양방향 검색
     * - (host=1, guest=2) 또는 (host=2, guest=1) 모두 같은 채팅방으로 간주
     * - 사용자 순서와 관계없이 동일한 채팅방 반환
     *
     * @Transactional 설명:
     * - readOnly = false (쓰기 가능)
     * - 새 채팅방 생성 시 DB INSERT 실행
     * - 실패 시 자동 롤백
     *
     * @param hostId 채팅방 개설자 ID
     * @param guestId 채팅 상대방 ID
     * @return ChatRoomDto 생성된 또는 기존 채팅방 정보
     *
     * 사용 예시:
     * ChatRoomDto room = chatRoomService.createRoom(1L, 2L);
     * // 첫 호출: 새 채팅방 생성
     * // 두 번째 호출: 기존 채팅방 반환 (중복 생성 방지)
     *
     * 반환 DTO 내용:
     * - roomId: 채팅방 ID
     * - hostId, hostName: 개설자 정보
     * - guestId, guestName: 상대방 정보
     */
    @Transactional
    public ChatRoomDto createRoom(Long hostId, Long guestId) {
        // Optional을 활용한 함수형 스타일 처리
        return chatRoomRepository.findByTwoUsers(hostId, guestId)
                // 이미 존재하는 채팅방이 있으면 DTO로 변환하여 반환
                .map(room -> convertToDto(room))
                // 채팅방이 없으면 새로 생성
                .orElseGet(() -> {
                    // 새 채팅방 엔티티 생성
                    ChatRoom newRoom = new ChatRoom();
                    newRoom.setHostId(hostId);
                    newRoom.setGuestId(guestId);

                    // DB에 저장 (ID 자동 생성)
                    ChatRoom saved = chatRoomRepository.save(newRoom);

                    // 엔티티를 DTO로 변환하여 반환
                    return convertToDto(saved);
                });
    }

    /**
     * 특정 채팅방의 과거 메시지 조회 (페이징 지원)
     *
     * 동작:
     * 1. 페이징 정보로 PageRequest 생성
     * 2. 채팅방 ID로 메시지 조회 (최신순 정렬)
     * 3. 각 메시지의 발신자 이름 조회 및 DTO 변환
     *
     * 페이징 처리:
     * - Spring Data JPA의 Pageable 사용
     * - DB에서 필요한 범위의 데이터만 조회 (성능 최적화)
     * - 대량의 메시지가 있어도 효율적으로 처리
     *
     * @param roomId 조회할 채팅방 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 메시지 수
     * @return List<ChatMessageDto> 페이징된 메시지 목록
     *
     * 사용 예시:
     * // 첫 번째 페이지, 10개씩 조회
     * List<ChatMessageDto> messages = chatRoomService.getMessages(1L, 0, 10);
     *
     * // 두 번째 페이지, 10개씩 조회
     * List<ChatMessageDto> nextMessages = chatRoomService.getMessages(1L, 1, 10);
     *
     * 반환 데이터:
     * - 최신 메시지부터 정렬 (createdAt DESC)
     * - 각 메시지에 발신자 이름 포함
     * - 페이지 크기만큼만 반환
     *
     * 성능 고려사항:
     * - N+1 문제 주의: 각 메시지마다 발신자 이름 조회
     * - 개선 방안: UserService에 캐싱 적용 또는 Batch 조회
     */
    public List<ChatMessageDto> getMessages(Long roomId, int page, int size) {
        // 페이징 정보 생성 (page: 페이지 번호, size: 페이지 크기)
        Pageable pageable = PageRequest.of(page, size);

        // DB에서 채팅방의 메시지 조회 (최신순 정렬)
        // findByRoomIdOrderByCreatedAtDesc는 Repository에 정의된 쿼리 메서드
        Page<ChatMessage> messages = chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        // Stream API를 사용한 엔티티 → DTO 변환
        return messages.stream()
                .map(msg -> {
                    // 각 메시지의 발신자 ID로 이름 조회
                    String senderName = userService.getUserName(msg.getSenderId());

                    // 엔티티와 발신자 이름으로 DTO 생성
                    return ChatMessageDto.from(msg, senderName);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 참여 중인 채팅방 목록 조회
     *
     * 동작:
     * - 사용자가 호스트이거나 게스트인 모든 채팅방 검색
     * - 양방향 검색: hostId = userId OR guestId = userId
     *
     * @param userId 조회할 사용자 ID
     * @return List<ChatRoomDto> 사용자가 참여 중인 채팅방 목록
     *
     * 사용 예시:
     * List<ChatRoomDto> myRooms = chatRoomService.getMyChatRooms(1L);
     * // user1이 참여 중인 모든 채팅방 반환
     * // user1이 개설한 방 + user1이 초대된 방 모두 포함
     *
     * 반환 데이터:
     * - 각 채팅방의 호스트/게스트 정보
     * - 이름까지 포함된 완성된 DTO
     */
    public List<ChatRoomDto> getMyChatRooms(Long userId) {
        // Repository의 OR 조건 쿼리 메서드 사용
        // WHERE hostId = userId OR guestId = userId
        List<ChatRoom> rooms = chatRoomRepository
                .findByHostIdOrGuestId(userId, userId);

        // Stream을 사용하여 엔티티를 DTO로 변환
        return rooms.stream()
                .map(this::convertToDto)  // 메서드 레퍼런스 사용
                .collect(Collectors.toList());
    }

    /**
     * 모든 채팅방 목록 조회
     *
     * 용도:
     * - 관리자 페이지에서 전체 채팅방 모니터링
     * - 채팅방 목록 UI에서 사용
     *
     * @return List<ChatRoomDto> 시스템의 모든 채팅방
     *
     * 주의사항:
     * - 채팅방 수가 많을 경우 성능 이슈 발생 가능
     * - 프로덕션에서는 페이징 처리 권장
     *
     * 개선 방안:
     * public Page<ChatRoomDto> getAllChatRooms(Pageable pageable) {
     *     return chatRoomRepository.findAll(pageable)
     *             .map(this::convertToDto);
     * }
     */
    public List<ChatRoomDto> getAllChatRooms() {
        return chatRoomRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * ChatRoom 엔티티를 ChatRoomDto로 변환하는 헬퍼 메서드
     *
     * 처리 과정:
     * 1. 호스트 ID로 사용자 이름 조회
     * 2. 게스트 ID로 사용자 이름 조회
     * 3. 엔티티 정보와 이름을 결합하여 DTO 생성
     *
     * @param room 변환할 ChatRoom 엔티티
     * @return ChatRoomDto 완성된 DTO 객체
     *
     * DTO에 포함되는 정보:
     * - roomId: 채팅방 ID
     * - hostId: 개설자 ID
     * - hostName: 개설자 이름 (추가 조회)
     * - guestId: 상대방 ID
     * - guestName: 상대방 이름 (추가 조회)
     *
     * 엔티티에는 ID만 있지만, DTO에는 사용자 이름까지 포함시켜
     * 클라이언트에서 추가 조회 없이 바로 표시할 수 있도록 함
     */
    private ChatRoomDto convertToDto(ChatRoom room) {
        // 호스트와 게스트의 이름 조회
        String hostName = userService.getUserName(room.getHostId());
        String guestName = userService.getUserName(room.getGuestId());

        // 정적 팩토리 메서드를 사용하여 DTO 생성
        return ChatRoomDto.from(room, hostName, guestName);
    }
}
