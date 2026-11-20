-- ============================================================================
-- Flyway Migration: V2__init_wallet_and_marketplace_schema.sql
-- Nhiệm vụ: Khởi tạo các bảng Tài chính (Ví, Giao dịch VNPay) & Sàn P2P / Đấu giá
-- ============================================================================

-- 1. Ví điện tử nội bộ (wallets) — DECIMAL(15,2), CHECK >= 0 & @Version
CREATE TABLE IF NOT EXISTS wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    locked_balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic Locking chống Race Condition khi cộng/trừ tiền',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallets_user_id UNIQUE (user_id),
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0 AND locked_balance >= 0)
);

-- 2. Lịch sử biến động ví & VNPay IPN (wallet_transactions)
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    vnp_txn_ref VARCHAR(64) NOT NULL COMMENT 'Mã tham chiếu duy nhất đảm bảo Idempotency cho VNPay IPN',
    vnp_transaction_no VARCHAR(64) NULL,
    vnp_bank_code VARCHAR(32) NULL,
    vnp_response_code VARCHAR(16) NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(15, 2) NOT NULL,
    balance_after DECIMAL(15, 2) NULL,
    description VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_wallet_tx_vnp_txn_ref UNIQUE (vnp_txn_ref),
    CONSTRAINT fk_wallet_tx_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_wallet_tx_amount_positive CHECK (amount > 0),
    INDEX idx_wallet_tx_wallet_status (wallet_id, status),
    INDEX idx_wallet_tx_wallet_created (wallet_id, created_at DESC),
    INDEX idx_wallet_tx_created_at (created_at)
);

-- 3. Tin rao bán P2P giá cố định (market_listings)
CREATE TABLE IF NOT EXISTS market_listings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    user_card_id BIGINT NOT NULL,
    price DECIMAL(15, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    buyer_id BIGINT NULL,
    sold_at TIMESTAMP NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_market_listing_user_card FOREIGN KEY (user_card_id) REFERENCES user_cards (id),
    CONSTRAINT chk_market_listing_price_positive CHECK (price > 0),
    INDEX idx_market_listings_status_created (status, created_at),
    INDEX idx_market_listings_seller_status (seller_id, status),
    INDEX idx_market_listings_user_card (user_card_id)
);

-- 4. Phiên Đấu Giá Thời Gian Thực (auctions)
CREATE TABLE IF NOT EXISTS auctions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id BIGINT NOT NULL,
    user_card_id BIGINT NOT NULL,
    starting_price DECIMAL(15, 2) NOT NULL,
    min_bid_increment DECIMAL(15, 2) NOT NULL,
    current_highest_bid DECIMAL(15, 2) NOT NULL,
    highest_bidder_id BIGINT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0 COMMENT 'Optimistic Locking (@Version) chống đè giá đấu giá',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_user_card FOREIGN KEY (user_card_id) REFERENCES user_cards (id),
    CONSTRAINT chk_auction_prices_positive CHECK (starting_price > 0 AND min_bid_increment > 0 AND current_highest_bid >= starting_price),
    INDEX idx_auctions_status_end_time (status, end_time),
    INDEX idx_auctions_seller_status (seller_id, status),
    INDEX idx_auctions_user_card (user_card_id)
);

-- 5. Lịch sử đặt giá (auction_bids)
CREATE TABLE IF NOT EXISTS auction_bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auction_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    masked_bidder_name VARCHAR(60) NOT NULL,
    bid_amount DECIMAL(15, 2) NOT NULL,
    bid_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auction_bid_auction FOREIGN KEY (auction_id) REFERENCES auctions (id) ON DELETE CASCADE,
    CONSTRAINT chk_auction_bid_amount_positive CHECK (bid_amount > 0),
    INDEX idx_auction_bids_auction_time (auction_id, bid_timestamp)
);
