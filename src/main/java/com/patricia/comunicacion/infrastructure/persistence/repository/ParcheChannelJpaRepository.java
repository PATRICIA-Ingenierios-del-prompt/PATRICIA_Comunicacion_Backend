package com.patricia.comunicacion.infrastructure.persistence.repository;

import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheChannelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParcheChannelJpaRepository extends JpaRepository<ParcheChannelEntity, String> {
}
