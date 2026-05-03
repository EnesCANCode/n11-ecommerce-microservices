package dev.enes.product.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Keycloak Admin API bağlantı yapılandırması.
 * Config Server üzerinden (product-service.yml) okunur.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak.admin")
public class KeycloakAdminProperties {
    private String serverUrl;
    private String realm;
    private String masterRealm;
    private String adminUsername;
    private String adminPassword;
    private String clientId;
}
