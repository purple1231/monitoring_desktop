package com.example.overlookdesktopserver.repository;

import com.example.overlookdesktopserver.entity.AppSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AppSessionRepository extends JpaRepository<AppSession, Long> {

    // 1. STOP 이벤트 처리용: 해당 PID의 미완료 세션(NULL)을 찾아 시간을 계산합니다.
    Optional<AppSession> findByPidAndDurationSecondsIsNull(Long pid);

    // 2. GET /stats 처리용: 현재 실행 중인 모든 미완료 세션(NULL)을 가져옵니다.
    List<AppSession> findByDurationSecondsIsNull();

    // 3. GET /stats 처리용: 종료된 세션의 시간을 합산합니다.
    @Query("SELECT l.appName, SUM(l.durationSeconds) FROM AppSession l " +
            "WHERE l.durationSeconds IS NOT NULL GROUP BY l.appName")
    List<Object[]> findCompletedUsageStats();

}










