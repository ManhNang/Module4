package com.pokemon.blindbox.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.pokemon.blindbox.exception.BusinessValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * CloudinaryService chuyên trách upload ảnh thẻ bài 2D lên Cloudinary CDN
 * và trả về đường dẫn HTTPS (secure_url) lưu vào CardTemplate.imageUrl.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L; // 5MB

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:pokemon-blindbox/cards}")
    private String baseFolder;

    /**
     * Upload file ảnh thẻ bài 2D lên Cloudinary và trả về imageUrl (secure_url).
     *
     * @param file       MultipartFile ảnh thẻ bài
     * @param setCode    Mã bộ sưu tập (ví dụ: "DLR") để phân thư mục rõ ràng
     * @param cardNumber Số thứ tự thẻ (ví dụ: 98)
     * @return URL ảnh HTTPS trên Cloudinary
     */
    public String uploadCardImage(MultipartFile file, String setCode, Integer cardNumber) {
        validateImageFile(file);

        String sanitizedSetCode = (setCode != null && !setCode.isBlank())
                ? setCode.trim().toLowerCase()
                : "general";
        String publicId = String.format(
                "%s/%s/card_%03d_%s",
                baseFolder,
                sanitizedSetCode,
                cardNumber != null ? cardNumber : 0,
                UUID.randomUUID().toString().substring(0, 8)
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image",
                            "format", "webp"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Uploaded 2D card image to Cloudinary: publicId={}, secureUrl={}", publicId, secureUrl);
            return secureUrl;
        } catch (IOException ex) {
            log.error("Failed to upload card image to Cloudinary for setCode={}, cardNumber={}", setCode, cardNumber, ex);
            throw new BusinessValidationException("Không thể tải ảnh thẻ bài lên Cloudinary: " + ex.getMessage());
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("File ảnh thẻ bài không được để trống.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessValidationException("Dung lượng ảnh thẻ bài vượt quá giới hạn 5MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessValidationException("Định dạng ảnh không hợp lệ. Chỉ chấp nhận JPG, PNG hoặc WEBP.");
        }
        // Kiểm tra Magic Bytes thực tế của file để chống giả mạo Content-Type header (P2-02)
        try {
            byte[] header = file.getInputStream().readNBytes(12);
            if (!hasValidImageMagicBytes(header)) {
                throw new BusinessValidationException("Nội dung file ảnh không hợp lệ (sai chữ ký tệp JPG/PNG/WEBP).");
            }
        } catch (IOException ex) {
            throw new BusinessValidationException("Không thể đọc nội dung file ảnh để xác thực.");
        }
    }

    private boolean hasValidImageMagicBytes(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // JPEG: FF D8 FF
        boolean isJpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        // PNG: 89 50 4E 47
        boolean isPng = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47;
        // WEBP: RIFF....WEBP
        boolean isWebp = bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return isJpeg || isPng || isWebp;
    }
}
