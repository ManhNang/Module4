package com.pokemon.blindbox.security;

import com.pokemon.blindbox.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filter cưỡng bức đăng xuất tức thì (Force-Kick) đối với tài khoản bị Admin khóa (isLocked = true).
 * Sử dụng bộ nhớ đệm TTL ngắn (5 giây) kết hợp xóa cache tức thì từ AdminUserService.updateUserLock()
 * để vừa bảo vệ hiệu năng DB vừa đá văng phiên đăng nhập ngay lập tức.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LockedUserKickFilter extends OncePerRequestFilter {

    private static final long CACHE_TTL_MS = 5_000L;
    private static final int MAX_CACHE_SIZE = 10_000;

    private final UserRepository userRepository;
    private final Map<Long, CachedLockState> lockStateCache = new ConcurrentHashMap<>();

    private record CachedLockState(boolean locked, long expiresAtMs) {}

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/login")
                || uri.startsWith("/register")
                || uri.startsWith("/error")
                || uri.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            Long userId = userDetails.getId();
            if (userId != null && isUserCurrentlyLocked(userId)) {
                log.warn("Force-kicking locked user id={} ({}) on URI {}", userId, userDetails.getUsername(), request.getRequestURI());
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                clearCookie(response, "JSESSIONID");
                clearCookie(response, "remember-me");
                clearCookie(response, "XSRF-TOKEN");

                String uri = request.getRequestURI();
                if (uri.startsWith("/api/")) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"status\":403,\"error\":\"ACCOUNT_LOCKED\",\"message\":\"Tài khoản của bạn đã bị Quản trị viên khóa!\"}");
                } else {
                    response.sendRedirect(request.getContextPath() + "/login?locked=true");
                }
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    public void evictUserLockCache(Long userId, Boolean knownLockedState) {
        if (userId == null) return;
        if (knownLockedState != null) {
            putCacheSafely(userId, knownLockedState, System.currentTimeMillis());
        } else {
            lockStateCache.remove(userId);
        }
    }

    private boolean isUserCurrentlyLocked(Long userId) {
        long now = System.currentTimeMillis();
        CachedLockState cached = lockStateCache.get(userId);
        if (cached != null && cached.expiresAtMs() > now) {
            return cached.locked();
        }
        boolean locked = userRepository.findById(userId)
                .map(u -> Boolean.TRUE.equals(u.getIsLocked()))
                .orElse(false);
        putCacheSafely(userId, locked, now);
        return locked;
    }

    private void putCacheSafely(Long userId, boolean locked, long now) {
        if (lockStateCache.size() > MAX_CACHE_SIZE) {
            lockStateCache.entrySet().removeIf(e -> e.getValue().expiresAtMs() <= now);
            if (lockStateCache.size() > MAX_CACHE_SIZE) {
                lockStateCache.clear();
            }
        }
        lockStateCache.put(userId, new CachedLockState(locked, now + CACHE_TTL_MS));
    }

    private void clearCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
