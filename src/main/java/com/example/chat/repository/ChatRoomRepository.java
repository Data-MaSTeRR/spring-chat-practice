package com.example.chat.repository;

import com.example.chat.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 사용자 간의 채팅방을 찾음
     * @param hostId
     * @param guestId
     * @return
     */
    @Query("SELECT cr FROM ChatRoom cr WHERE " +
            "(cr.hostId = :hostId AND cr.guestId = :guestId) OR " +
            "(cr.hostId = :guestId AND cr.guestId = :hostId)")
    Optional<ChatRoom> findByTwoUsers(@Param("hostId") Long hostId,
                                      @Param("guestId") Long guestId);

    /**
     * 특정 사용자가 속한 모든 채팅방 조회
     * @param hostId
     * @param guestId
     * @return
     */
    List<ChatRoom> findByHostIdOrGuestId(Long hostId, Long guestId);

}
