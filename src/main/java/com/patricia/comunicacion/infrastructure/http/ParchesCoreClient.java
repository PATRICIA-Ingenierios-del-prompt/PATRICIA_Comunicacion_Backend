package com.patricia.comunicacion.infrastructure.http;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Adaptador HTTP que verifica membresía consultando Parches Core
 * síncronamente (RestClient nativo de Spring 6).
 */
@Slf4j
@Component
public class ParchesCoreClient implements MembershipVerification {

    private final RestClient restClient;

    public ParchesCoreClient(@Value("${services.parches-core.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void verify(String parcheId, String userId) {
        try {
            MembershipResponse response = restClient.get()
                    .uri("/api/v1/parches/{parcheId}/members/{userId}/verify", parcheId, userId)
                    .header("Authorization", "Bearer internal-service-token")
                    .retrieve()
                    .body(MembershipResponse.class);

            if (response == null || !response.isMember()) {
                throw new MembershipException(userId, parcheId);
            }
        } catch (HttpClientErrorException.Forbidden | HttpClientErrorException.NotFound e) {
            throw new MembershipException(userId, parcheId);
        } catch (MembershipException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Parches Core no disponible, modo dev: permitiendo acceso [parcheId={}, userId={}]",
                    parcheId, userId);
        }
    }

    @Override
    public boolean isParchePrivate(String parcheId) {
        try {
            ParcheInfoResponse response = restClient.get()
                    .uri("/api/v1/parches/{parcheId}/info", parcheId)
                    .header("Authorization", "Bearer internal-service-token")
                    .retrieve()
                    .body(ParcheInfoResponse.class);
            return response != null && response.isPrivate();
        } catch (Exception e) {
            log.warn("Parches Core no disponible, asumiendo parche privado [parcheId={}]", parcheId);
            return true;
        }
    }

    public record MembershipResponse(boolean isMember) {}
    public record ParcheInfoResponse(String id, String name, boolean isPrivate) {}
}
