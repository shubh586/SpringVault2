package com.appvault.server.repository;

import com.appvault.server.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findByServiceNameAndActiveTrue(String serviceName);

    List<Policy> findByServiceNameOrServiceNameAndActiveTrue(String serviceName, String wildcard);

    List<Policy> findByActiveTrue();

    boolean existsByName(String name);
}
