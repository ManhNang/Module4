package com.pokemon.blindbox.repository;

import com.pokemon.blindbox.entity.User;
import com.pokemon.blindbox.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(Role role);

    @Query("""
            SELECT u FROM User u
            WHERE (:keyword IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR u.role = :role)
              AND (:locked IS NULL OR u.isLocked = :locked)
            ORDER BY u.id DESC
            """)
    Page<User> searchAdminUsers(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("locked") Boolean locked,
            Pageable pageable
    );
}
