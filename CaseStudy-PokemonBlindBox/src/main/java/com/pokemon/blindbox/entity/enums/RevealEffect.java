package com.pokemon.blindbox.entity.enums;

/**
 * 7 preset hiệu ứng mở thẻ (Reveal Effect) gắn trực tiếp với từng bậc Độ hiếm trong DB.
 * Frontend đọc động 100% từ API thay vì hardcode tên chuỗi độ hiếm.
 */
public enum RevealEffect {
    NONE,
    DOUBLE_RARE,
    ILLUSTRATION,
    ULTRA,
    SPECIAL_ILLUSTRATION,
    FUTURISTIC,
    MEGA_HYPER;

    public static RevealEffect fromStringOrDefault(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        try {
            return RevealEffect.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
