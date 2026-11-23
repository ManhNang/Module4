-- =====================================================================
-- V15: Sự kiện ngẫu nhiên trên Đảo Pokémon (Random Drops) & Mảnh Pack
-- =====================================================================

CREATE TABLE IF NOT EXISTS sanctuary_user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pack_shards INT NOT NULL DEFAULT 0,
    daily_drop_date DATE NULL,
    daily_drops_claimed INT NOT NULL DEFAULT 0,
    last_drop_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sanctuary_profile_user UNIQUE (user_id),
    CONSTRAINT fk_sanctuary_profile_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sanctuary_random_drops (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    drop_type VARCHAR(32) NOT NULL,
    pos_x DOUBLE NOT NULL,
    pos_y DOUBLE NOT NULL,
    spawned_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    claimed BOOLEAN NOT NULL DEFAULT FALSE,
    claimed_at DATETIME(6) NULL,
    reward_type VARCHAR(20) NULL,
    reward_amount INT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    INDEX idx_sanctuary_drops_user_active (user_id, claimed, expires_at),
    CONSTRAINT fk_sanctuary_drop_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
