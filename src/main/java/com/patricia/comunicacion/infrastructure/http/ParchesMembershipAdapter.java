package com.patricia.comunicacion.infrastructure.http;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptador de membresía con dos fuentes distintas:
 * <p>
 * - verify(): consulta la caché local (tabla parche_member), alimentada por
 *   los eventos parche.created / parche.member.joined / parche.member.expelled.
 *   Antes esto llamaba a un endpoint de Parches que nunca existió
 *   (/api/v1/parches/{id}/members/{id}/verify) y caía siempre en modo dev,
 *   dejando pasar a cualquiera sin verificar nada en realidad.
 * <p>
 * - isParchePrivate(): ese dato (visibility) no viaja en ningún evento, así
 *   que sigue siendo una llamada HTTP — pero corregida para apuntar al
 *   endpoint real GET /api/parches/{parcheId}, que sí existe en Parches.
 */
@Slf4j
@Component
public class ParchesMembershipAdapter implements MembershipVerification {

    private final ParcheMemberJpaRepository memberRepository;
    private final RestClient restClient;

    public ParchesMembershipAdapter(ParcheMemberJpaRepository memberRepository,
                                     @Value("${services.parches-core.url}") String baseUrl) {
        this.memberRepository = memberRepository;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public void verify(String parcheId, String userId) {
        if (!memberRepository.existsByParcheIdAndUserId(parcheId, userId)) {
            throw new MembershipException(userId, parcheId);
        }
    }

    @Override
    public boolean isParchePrivate(String parcheId) {
        try {
            ParcheResponse response = restClient.get()
                    .uri("/api/parches/{parcheId}", parcheId)
                    .retrieve()
                    .body(ParcheResponse.class);
            return response != null && "PRIVATE".equalsIgnoreCase(response.visibility());
        } catch (Exception e) {
            log.warn("Parches Core no disponible, asumiendo parche privado [parcheId={}]", parcheId);
            return true;
        }
    }

    /**
     * Espejo parcial de ingprompt.patricia.parches...ParcheResponse — solo
     * necesitamos el campo visibility, Jackson ignora el resto.
     */
    public record ParcheResponse(String name, String description, String category,
                                  String visibility, String status,
                                  int maxCapacity, int memberCount) {}
}
