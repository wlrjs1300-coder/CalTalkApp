package com.caltalk.backend.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.caltalk.backend.common.error.DuplicateEmailException;
import com.caltalk.backend.common.error.PasswordMismatchException;
import com.caltalk.backend.user.User;
import com.caltalk.backend.user.UserRepository;

@Service
public class SignupService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);

        if (request.password().isBlank()) {
            throw new PasswordMismatchException(
                    "password",
                    "INVALID_PASSWORD",
                    "비밀번호는 공백만으로 구성할 수 없습니다."
            );
        }
        if (!request.password().equals(request.passwordConfirmation())) {
            throw new PasswordMismatchException(
                    "passwordConfirmation",
                    "PASSWORD_MISMATCH",
                    "비밀번호 확인이 일치하지 않습니다."
            );
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(request.password()));

        try {
            User savedUser = userRepository.saveAndFlush(user);
            return new SignupResponse(
                    savedUser.getEmail(),
                    savedUser.getTimezone(),
                    savedUser.getCreatedAt()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException(exception);
        }
    }
}
