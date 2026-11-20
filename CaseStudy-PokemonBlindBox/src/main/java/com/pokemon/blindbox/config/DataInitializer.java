package com.pokemon.blindbox.config;

import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.entity.Wallet;
import com.pokemon.blindbox.entity.enums.Role;
import com.pokemon.blindbox.repository.UserRepository;
import com.pokemon.blindbox.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Khởi tạo duy nhất 1 tài khoản Quản trị viên mặc định chuẩn BCryptPasswordEncoder:
 * - Admin: nangdzai2k / 123456 (ROLE_ADMIN)
 */
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        User admin = ensureUser(
                "nangdzai2k",
                "nangdzai2k@pokemon-blindbox.vn",
                "123456",
                Role.ROLE_ADMIN
        );

        ensureWallet(admin.getId(), BigDecimal.ZERO);
    }

    private User ensureUser(String username, String email, String rawPassword, Role role) {
        return userRepository.findByUsernameIgnoreCase(username)
                .map(existing -> {
                    existing.setRole(role);
                    if (!passwordEncoder.matches(rawPassword, existing.getPassword())) {
                        existing.setPassword(passwordEncoder.encode(rawPassword));
                    }
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User created = User.builder()
                            .username(username)
                            .email(email)
                            .password(passwordEncoder.encode(rawPassword))
                            .role(role)
                            .build();
                    User saved = userRepository.save(created);
                    log.info("Initialized default admin account: username={}, role={}, id={}",
                            saved.getUsername(), saved.getRole(), saved.getId());
                    return saved;
                });
    }

    private void ensureWallet(Long userId, BigDecimal defaultBalance) {
        if (walletRepository.findByUserId(userId).isEmpty()) {
            walletRepository.save(Wallet.builder()
                    .userId(userId)
                    .balance(defaultBalance)
                    .lockedBalance(BigDecimal.ZERO)
                    .build());
        }
    }
}
