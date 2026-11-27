package com.pokemon.blindbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sepay")
@Getter
@Setter
public class SePayProperties {
    private String bankCode = "MB";
    private String accountNumber = "";
    private String accountName = "";
    private String webhookApiKey = "";
    private String qrTemplate = "compact";
    private String codePrefix = "PKM";
}
