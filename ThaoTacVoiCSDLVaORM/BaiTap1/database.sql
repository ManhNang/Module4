-- ===============================================
-- Database Initialization for Music Streaming App
-- ===============================================

CREATE DATABASE IF NOT EXISTS `ungdungnghenhac`
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE `ungdungnghenhac`;

-- Create songs table
CREATE TABLE IF NOT EXISTS `songs` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(255) NOT NULL,
    `artist` VARCHAR(255) NOT NULL,
    `genre` VARCHAR(100),
    `file_path` VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sample initial songs
INSERT INTO `songs` (`name`, `artist`, `genre`, `file_path`) VALUES
('Nơi Này Có Anh', 'Sơn Tùng M-TP', 'V-Pop', ''),
('Waiting For You', 'MONO', 'Pop / RnB', ''),
('See Tình', 'Hoàng Thùy Linh', 'Dance-Pop', ''),
('Shape of You', 'Ed Sheeran', 'Pop', '');
