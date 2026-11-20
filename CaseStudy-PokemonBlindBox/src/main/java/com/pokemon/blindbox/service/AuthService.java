package com.pokemon.blindbox.service;

import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.entity.enums.Role;
import com.pokemon.blindbox.exception.BusinessValidationException;
import com.pokemon.blindbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * AuthService tách biệt nghiệp vụ đăng ký tài khoản khỏi AuthController (SOLID SRP, P1-07, P2-03).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.-]{3,32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.welcome-bonus:500000.00}")
    private BigDecimal welcomeBonus;

    @Transactional
    public User registerNewTrainer(String rawUsername, String rawEmail, String password, String confirmPassword) {
        String cleanUsername = rawUsername != null ? rawUsername.trim() : "";
        String cleanEmail = rawEmail != null ? rawEmail.trim() : "";

        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            throw new BusinessValidationException("Tên đăng nhập phải từ 3–32 ký tự và chỉ chứa chữ, số hoặc _, ., -");
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new BusinessValidationException("Địa chỉ email không đúng định dạng hợp lệ.");
        }
        if (password == null || password.length() < 6 || password.length() > 72) {
            throw new BusinessValidationException("Mật khẩu phải có từ 6 đến 72 ký tự.");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessValidationException("Mật khẩu xác nhận không khớp.");
        }
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername)) {
            throw new BusinessValidationException("Tên đăng nhập '" + cleanUsername + "' đã tồn tại. Vui lòng chọn tên khác!");
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new BusinessValidationException("Địa chỉ email '" + cleanEmail + "' đã được đăng ký!");
        }

        User savedUser = userRepository.save(User.builder()
                .username(cleanUsername)
                .email(cleanEmail)
                .password(passwordEncoder.encode(password))
                .role(Role.ROLE_USER)
                .build());

        walletService.createDefaultWallet(savedUser.getId(), welcomeBonus);
        log.info("Registered new trainer account: username={}, id={}", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }
}
