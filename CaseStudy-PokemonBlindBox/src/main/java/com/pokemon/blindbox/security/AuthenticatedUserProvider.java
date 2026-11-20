package com.pokemon.blindbox.security;

import com.pokemon.blindbox.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Truy xuất thông tin User đang đăng nhập trực tiếp từ SecurityContextHolder / @AuthenticationPrincipal.
 * Tuyệt đối không fallback về userId mặc định khi chưa xác thực (P0-08).
 */
@Component
@RequiredArgsConstructor
public class AuthenticatedUserProvider {

    private final UserRepository userRepository;

    public Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails details) {
            return Optional.of(details);
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userRepository.findByUsernameIgnoreCase(username).map(CustomUserDetails::new);
        }
        return Optional.empty();
    }

    /**
     * Lấy userId của người dùng đang đăng nhập từ SecurityContext.
     * Bỏ qua mọi fallbackUserId từ client payload để chống giả mạo danh tính (P0-08).
     */
    public Long resolveCurrentUserId(Long ignoredFallbackUserId) {
        return getRequiredUserId();
    }

    public Long getRequiredUserId() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getId)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để thực hiện thao tác này!"));
    }

    public String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::getUsername)
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Yêu cầu đăng nhập để thực hiện thao tác này!"));
    }
}
