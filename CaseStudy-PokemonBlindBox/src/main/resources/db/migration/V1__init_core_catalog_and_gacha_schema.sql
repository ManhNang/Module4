-- ============================================================================
-- Flyway Migration: V1__init_core_catalog_and_gacha_schema.sql
-- Nhiệm vụ: Khởi tạo các bảng cốt lõi — Tài khoản, Danh mục thẻ, Hộp/Gói & Kho thẻ
-- ============================================================================

-- 1. Tài khoản người dùng & Phân quyền (ROLE_USER, ROLE_ADMIN)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(255) NOT NULL COMMENT 'Mã hóa bằng BCryptPasswordEncoder',
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    INDEX idx_users_role (role)
);

-- 2. Bộ sưu tập thẻ (card_sets) — Hỗ trợ đa bộ & Soft Delete (is_active)
CREATE TABLE IF NOT EXISTS card_sets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(20) NOT NULL,
    base_set_total INT NOT NULL COMMENT 'Tổng số thẻ chuẩn của bộ (VD: 103)',
    cards_per_pack INT NOT NULL COMMENT 'Số thẻ mặc định trong mỗi gói',
    packs_per_box INT NOT NULL COMMENT 'Số gói trong mỗi hộp Booster Box',
    pack_price DECIMAL(15, 2) NOT NULL,
    box_price DECIMAL(15, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_card_sets_code UNIQUE (code),
    CONSTRAINT chk_card_sets_positive CHECK (base_set_total > 0 AND cards_per_pack > 0 AND packs_per_box > 0)
);

-- 3. Độ hiếm (rarities) — Trọng số Gacha & cờ hiệu ứng Holographic 3D
CREATE TABLE IF NOT EXISTS rarities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    drop_weight INT NOT NULL COMMENT 'Trọng số rơi thẻ cho thuật toán Gacha',
    color_hex VARCHAR(7) NOT NULL COMMENT 'Mã màu HEX hiển thị UI',
    is_holographic BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Bật hiệu ứng 3D Tilt & Holographic Foil',
    CONSTRAINT uk_rarities_name UNIQUE (name),
    CONSTRAINT chk_rarities_weight CHECK (drop_weight >= 0)
);

-- 4. Loài / Phân loại thẻ (card_species)
CREATE TABLE IF NOT EXISTS card_species (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    is_beta BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_card_species_name UNIQUE (name)
);

-- 5. Khuôn mẫu thẻ bài (card_templates) — Hỗ trợ Soft Delete & Index Gacha/Pokedex
CREATE TABLE IF NOT EXISTS card_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    card_number INT NOT NULL COMMENT 'Số thứ tự thẻ kiểu INT (52, 98, 141) để ORDER BY chuẩn xác',
    image_url VARCHAR(500) NOT NULL,
    set_id BIGINT NOT NULL,
    rarity_id BIGINT NOT NULL,
    species_id BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_card_set_number UNIQUE (set_id, card_number),
    CONSTRAINT fk_card_template_set FOREIGN KEY (set_id) REFERENCES card_sets (id),
    CONSTRAINT fk_card_template_rarity FOREIGN KEY (rarity_id) REFERENCES rarities (id),
    CONSTRAINT fk_card_template_species FOREIGN KEY (species_id) REFERENCES card_species (id),
    INDEX idx_card_templates_set_rarity (set_id, rarity_id),
    INDEX idx_card_templates_set_active (set_id, is_active),
    INDEX idx_card_templates_species (species_id),
    INDEX idx_card_templates_name (name)
);

-- 6. Hộp bài (booster_boxes) — Snapshot cấu hình & @Version Optimistic Locking
CREATE TABLE IF NOT EXISTS booster_boxes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    box_code VARCHAR(50) NOT NULL,
    set_id BIGINT NOT NULL,
    total_packs INT NOT NULL,
    remaining_packs INT NOT NULL,
    can_buy_whole_box BOOLEAN NOT NULL DEFAULT TRUE,
    snapshot_box_price DECIMAL(15, 2) NOT NULL,
    snapshot_cards_per_pack INT NOT NULL DEFAULT 5 COMMENT 'Đóng băng số thẻ/gói chống 2N+1 Query',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_booster_boxes_code UNIQUE (box_code),
    CONSTRAINT fk_booster_box_set FOREIGN KEY (set_id) REFERENCES card_sets (id),
    CONSTRAINT chk_booster_boxes_remaining CHECK (remaining_packs >= 0 AND remaining_packs <= total_packs),
    INDEX idx_booster_boxes_set_status (set_id, status)
);

-- 7. Gói bài lẻ trong Hộp (booster_packs) — Đóng băng snapshot_cards_per_pack & @Version
CREATE TABLE IF NOT EXISTS booster_packs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    box_id BIGINT NOT NULL,
    position_in_box INT NOT NULL,
    snapshot_cards_per_pack INT NOT NULL COMMENT 'Đóng băng cấu hình số thẻ/pack tại thời điểm khởi tạo Box',
    snapshot_pack_price DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    buyer_id BIGINT NULL,
    purchased_at TIMESTAMP NULL,
    opened_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_box_pack_position UNIQUE (box_id, position_in_box),
    CONSTRAINT fk_booster_pack_box FOREIGN KEY (box_id) REFERENCES booster_boxes (id) ON DELETE CASCADE,
    INDEX idx_booster_packs_box_status (box_id, status),
    INDEX idx_booster_packs_buyer_status (buyer_id, status)
);

-- 8. Kho thẻ người chơi sở hữu (user_cards) — Có @Version chống Double-selling
CREATE TABLE IF NOT EXISTS user_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    card_template_id BIGINT NOT NULL,
    booster_pack_id BIGINT NOT NULL,
    is_locked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    acquired_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_card_template FOREIGN KEY (card_template_id) REFERENCES card_templates (id),
    CONSTRAINT fk_user_card_pack FOREIGN KEY (booster_pack_id) REFERENCES booster_packs (id),
    INDEX idx_user_cards_user_template (user_id, card_template_id),
    INDEX idx_user_cards_user_acquired (user_id, acquired_at DESC),
    INDEX idx_user_cards_pack (booster_pack_id)
);
