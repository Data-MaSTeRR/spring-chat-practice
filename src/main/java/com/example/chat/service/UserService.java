package com.example.chat.service;

import com.example.chat.domain.User;
import com.example.chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 이름 조회
     * @param userId 사용자 ID
     * @return 사용자 이름 (존재하지 않으면 "Unknown")
     */
    public String getUserName(Long userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Unknown");
    }
}