package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.UserResponse;
import com.socialpulse.backend.exception.InvalidCredentialsException;
import com.socialpulse.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /** Token geçerli ama kullanıcı silinmişse oturum geçersiz sayılır (401). */
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getFullName()))
                .orElseThrow(InvalidCredentialsException::new);
    }
}
