package com.example.overlookdesktopserver.repository;

import com.example.overlookdesktopserver.entity.UsageSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppSessionRepository extends JpaRepository<UsageSummary, Long> {

    Optional<UsageSummary> findById(String appName);

}










