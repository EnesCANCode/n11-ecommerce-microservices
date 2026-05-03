package dev.enes.product.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.enes.product.config.KeycloakAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Keycloak Admin REST API üzerinden programatik olarak
 * satıcı (SELLER rolüne sahip) kullanıcıları oluşturur.
 * Harici bağımlılık gerektirmez — doğrudan HTTP isteği atar.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private final KeycloakAdminProperties props;
    private final ObjectMapper objectMapper;

    /**
     * Keycloak Master Realm'dan admin token alır.
     */
    private String getAdminToken() {
        RestTemplate rest = new RestTemplate();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("username", props.getAdminUsername());
        form.add("password", props.getAdminPassword());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<JsonNode> response = rest.postForEntity(
                props.getServerUrl() + "/realms/" + props.getMasterRealm() + "/protocol/openid-connect/token",
                new HttpEntity<>(form, headers),
                JsonNode.class
        );

        return response.getBody().get("access_token").asText();
    }

    /**
     * Belirtilen sayıda satıcıyı Keycloak'ta oluşturur ve UUID listesini döner.
     */
    public List<SellerInfo> createSellers(int count, String password) {
        List<SellerInfo> sellers = new ArrayList<>();
        String token = getAdminToken();
        RestTemplate rest = new RestTemplate();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(token);
        authHeaders.setContentType(MediaType.APPLICATION_JSON);

        String usersUrl = props.getServerUrl() + "/admin/realms/" + props.getRealm() + "/users";

        for (int i = 1; i <= count; i++) {
            String username = "seller_" + i;
            String email = "seller_" + i + "@n11.com";

            try {
                // Kullanıcı zaten var mı kontrol et
                ResponseEntity<JsonNode> searchResp = rest.exchange(
                        usersUrl + "?username=" + username + "&exact=true",
                        HttpMethod.GET,
                        new HttpEntity<>(authHeaders),
                        JsonNode.class
                );

                JsonNode users = searchResp.getBody();
                if (users != null && users.isArray() && !users.isEmpty()) {
                    String existingId = users.get(0).get("id").asText();
                    sellers.add(new SellerInfo(UUID.fromString(existingId), username, email));
                    log.info("Satıcı zaten mevcut, atlanıyor: {} ({})", username, existingId);
                    continue;
                }

                // Yeni kullanıcı oluştur
                Map<String, Object> userRep = new LinkedHashMap<>();
                userRep.put("username", username);
                userRep.put("email", email);
                userRep.put("firstName", "Mağaza");
                userRep.put("lastName", String.valueOf(i));
                userRep.put("enabled", true);
                userRep.put("emailVerified", true);
                userRep.put("credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false
                )));

                ResponseEntity<Void> createResp = rest.exchange(
                        usersUrl,
                        HttpMethod.POST,
                        new HttpEntity<>(objectMapper.writeValueAsString(userRep), authHeaders),
                        Void.class
                );

                if (createResp.getStatusCode() == HttpStatus.CREATED) {
                    // Location header'dan ID'yi al
                    String location = createResp.getHeaders().getFirst("Location");
                    String userId = location.substring(location.lastIndexOf('/') + 1);

                    // SELLER rolünü ata
                    assignSellerRole(rest, authHeaders, userId);

                    sellers.add(new SellerInfo(UUID.fromString(userId), username, email));
                    log.info("Satıcı oluşturuldu: {} ({})", username, userId);
                }
            } catch (Exception e) {
                log.warn("Satıcı oluşturulamadı: {} — {}", username, e.getMessage());
            }
        }

        log.info("Toplam {} satıcı Keycloak'ta hazır.", sellers.size());
        return sellers;
    }

    /**
     * Kullanıcıya SELLER realm rolünü atar.
     */
    private void assignSellerRole(RestTemplate rest, HttpHeaders authHeaders, String userId) {
        try {
            String rolesUrl = props.getServerUrl() + "/admin/realms/" + props.getRealm();

            // SELLER rolünü al
            ResponseEntity<JsonNode> roleResp = rest.exchange(
                    rolesUrl + "/roles/SELLER",
                    HttpMethod.GET,
                    new HttpEntity<>(authHeaders),
                    JsonNode.class
            );

            if (roleResp.getStatusCode() == HttpStatus.OK && roleResp.getBody() != null) {
                String roleJson = "[" + roleResp.getBody().toString() + "]";

                HttpHeaders h = new HttpHeaders();
                h.setBearerAuth(authHeaders.getFirst("Authorization").replace("Bearer ", ""));
                h.setContentType(MediaType.APPLICATION_JSON);

                rest.exchange(
                        rolesUrl + "/users/" + userId + "/role-mappings/realm",
                        HttpMethod.POST,
                        new HttpEntity<>(roleJson, authHeaders),
                        Void.class
                );
            }
        } catch (Exception e) {
            log.warn("SELLER rolü atanamadı (userId={}): {}", userId, e.getMessage());
        }
    }

    public record SellerInfo(UUID id, String username, String email) {}
}
