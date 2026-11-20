package com.pokemon.blindbox.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Springdoc OpenAPI 3 (Swagger UI tại /swagger-ui.html):
 * Phân nhóm rõ 5 phân hệ REST API:
 * 1. Admin
 * 2. Store/Gacha
 * 3. Inventory
 * 4. Marketplace/Auction
 * 5. Wallet
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pokemonBlindboxOpenAPI() {
        final String cookieAuthName = "JSESSIONID";
        return new OpenAPI()
                .info(new Info()
                        .title("Pokémon Blindbox E-Commerce, Gacha & Real-time Auction API")
                        .version("1.0.0")
                        .description("Tài liệu đặc tả RESTful API cho hệ thống Pokémon Blindbox: Quản trị Bộ thẻ (Delta Reign - 103 thẻ), Mở hộp/gói Gacha (@Version Optimistic Locking), Kho đồ & Tiến độ Master Set, Sàn giao dịch P2P & Đấu giá thời gian thực (STOMP WebSocket), và Ví điện tử tích hợp VNPay Sandbox (HMAC-SHA512).")
                        .contact(new Contact()
                                .name("Pokémon Blindbox Engineering Team")
                                .email("support@pokemon-blindbox.vn")))
                .addSecurityItem(new SecurityRequirement().addList(cookieAuthName))
                .components(new Components()
                        .addSecuritySchemes(cookieAuthName, new SecurityScheme()
                                .name("JSESSIONID")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .description("Session Cookie sau khi đăng nhập tại /login (satoshi/123456 hoặc admin/admin123)")));
    }

    @Bean
    public GroupedOpenApi allApisGroup() {
        return GroupedOpenApi.builder()
                .group("0. All APIs")
                .displayName("0. Tất cả API (/api/v1/**)")
                .pathsToMatch("/api/v1/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("1. Admin")
                .displayName("1. Admin (Quản trị Bộ thẻ, Độ hiếm, Cloudinary & Lô BoosterBox)")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi storeGachaApiGroup() {
        return GroupedOpenApi.builder()
                .group("2. Store-Gacha")
                .displayName("2. Store/Gacha (Mua Pack lẻ, Mua nguyên Box & Mở Gacha)")
                .pathsToMatch("/api/v1/boxes/**", "/api/v1/packs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi inventoryApiGroup() {
        return GroupedOpenApi.builder()
                .group("3. Inventory")
                .displayName("3. Inventory (Kho đồ, Bộ lọc Pokedex & Tiến độ Master Set 103 ô)")
                .pathsToMatch("/api/v1/binder/**")
                .build();
    }

    @Bean
    public GroupedOpenApi marketplaceAuctionApiGroup() {
        return GroupedOpenApi.builder()
                .group("4. Marketplace-Auction")
                .displayName("4. Marketplace/Auction (Sàn P2P, Đấu giá thời gian thực & Tra cứu chéo)")
                .pathsToMatch("/api/v1/market/**")
                .build();
    }

    @Bean
    public GroupedOpenApi walletApiGroup() {
        return GroupedOpenApi.builder()
                .group("5. Wallet")
                .displayName("5. Wallet (Ví điện tử nội bộ & Cổng thanh toán VNPay Sandbox IPN)")
                .pathsToMatch("/api/v1/wallets/**")
                .build();
    }
}
