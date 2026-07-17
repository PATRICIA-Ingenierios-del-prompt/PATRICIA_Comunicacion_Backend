package com.patricia.comunicacion.infrastructure.http;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.domain.port.out.MembershipProvisioning;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheMemberEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.Set;

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
public class ParchesMembershipAdapter implements MembershipVerification, MembershipProvisioning {

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
    public void ensureMember(String channelId, String userId) {
        if (memberRepository.existsByParcheIdAndUserId(channelId, userId)) {
            return;
        }
        ParcheMemberEntity entity = new ParcheMemberEntity();
        entity.setParcheId(channelId);
        entity.setUserId(userId);
        try {
            memberRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            // Carrera con otra request creando el mismo DM: el índice único
            // idx_parche_member_lookup ya garantizó la fila. Idempotente.
            log.debug("Miembro ya registrado [channelId={}, userId={}]", channelId, userId);
        }
    }

    @Override
    public Set<String> findMembers(String channelId) {
        Set<String> userIds = memberRepository.findUserIdsByParcheId(channelId);
        return userIds != null ? userIds : Collections.emptySet();
    }

    @Override
    public String getChannelName(String channelId) {
        try {
            ParcheResponse response = restClient.get()
                    .uri("/api/parches/{parcheId}", channelId)
                    .retrieve()
                    .body(ParcheResponse.class);
            if (response != null && response.name() != null && !response.name().isBlank()) {
                return response.name();
            }
        } catch (Exception e) {
            log.debug("Parches Core no tiene el parche {} — asumiendo chat privado", channelId);
        }
        return "chat privado";
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
