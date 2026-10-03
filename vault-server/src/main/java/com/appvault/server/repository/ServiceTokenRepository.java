package com.appvault.server.repository;

import com.appvault.server.model.ServiceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ServiceTokenRepository extends JpaRepository<ServiceToken, Long> {

    Optional<ServiceToken> findByTokenHashAndActiveTrue(String tokenHash);

    Optional<ServiceToken> findByServiceNameAndActiveTrue(String serviceName);

    boolean existsByServiceNameAndActiveTrue(String serviceName);
}
