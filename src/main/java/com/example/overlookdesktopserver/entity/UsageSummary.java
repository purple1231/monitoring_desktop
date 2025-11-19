package com.example.overlookdesktopserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class AppSession {

    public AppSession() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pid;
    private String appName;
    private LocalDateTime startTime;
    private Long durationSeconds; // STOP 시 여기에 사용 시간이 기록됨
    private LocalDate usageDate; // 통계 조회를 위한 날짜
}