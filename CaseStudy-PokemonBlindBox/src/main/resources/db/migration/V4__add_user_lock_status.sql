-- ============================================================================
-- Flyway Migration: V4__add_user_lock_status.sql
-- Nhiệm vụ: Bổ sung cột is_locked và index (is_locked, role) cho bảng users
--           phục vụ phân hệ Quản Lý Người Dùng (Admin Tab 5) — Idempotent.
-- ============================================================================

DROP PROCEDURE IF EXISTS migrate_v4_user_lock_status;
DELIMITER //
CREATE PROCEDURE migrate_v4_user_lock_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND COLUMN_NAME = 'is_locked'
    ) THEN
        ALTER TABLE users
            ADD COLUMN is_locked BOOLEAN NOT NULL DEFAULT FALSE AFTER role;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'users'
          AND INDEX_NAME = 'idx_users_locked_role'
    ) THEN
        CREATE INDEX idx_users_locked_role ON users (is_locked, role);
    END IF;
END //
DELIMITER ;

CALL migrate_v4_user_lock_status();
DROP PROCEDURE IF EXISTS migrate_v4_user_lock_status;
