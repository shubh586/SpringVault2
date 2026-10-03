package com.appvault.server.repository;

import com.appvault.server.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    Optional<Credential> findByPathAndSecretKeyAndEnvironment(String path, String secretKey, String environment);

    List<Credential> findByPathAndEnvironment(String path, String environment);

    List<Credential> findByEnvironment(String environment);

    List<Credential> findByPathStartingWithAndEnvironment(String pathPrefix, String environment);

    boolean existsByPathAndSecretKeyAndEnvironment(String path, String secretKey, String environment);
}
