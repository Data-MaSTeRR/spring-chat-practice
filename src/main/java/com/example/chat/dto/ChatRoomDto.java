package com.example.chat.dto;

import com.example.chat.domain.ChatRoom;
import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {

    private Long roomId;
    private Long hostId;
    private String hostName;
    private Long guestId;
    private String guestName;

    public static ChatRoomDto from(ChatRoom entity, String hostName, String guestName) {
        return ChatRoomDto.builder()
                .roomId(entity.getId())
                .hostId(entity.getHostId())
                .hostName(hostName)
                .guestId(entity.getGuestId())
                .guestName(guestName)
                .build();
    }

}
