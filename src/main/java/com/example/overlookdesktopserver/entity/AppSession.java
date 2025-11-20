package com.example.overlookdesktopserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class AppSession {

    public AppSession() {
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB Primary Key

    private Long pid; // 프로세스 ID (세션의 고유 식별자)
    private String appName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationSeconds; // NULL이면 활성, 값이 있으면 종료됨.
}