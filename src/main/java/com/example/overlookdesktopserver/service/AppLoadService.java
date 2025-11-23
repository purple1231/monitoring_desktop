package com.example.overlookdesktopserver.service;

import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AppLoadService {

    private final AppSessionRepository appSessionRepository;

    public AppLoadService(AppSessionRepository appSessionRepository){
        this.appSessionRepository = appSessionRepository;
    }



    private long mergeAndCalculate(List<AppSession> sessions) {
        if (sessions.isEmpty()) return 0;

        // 1. AppSession 목록을 Interval 목록으로 변환 (EndTime 처리)
        List<Interval> intervals = changeInterval(sessions);

        // 2. 시작 시간 기준으로 정렬
        intervals.sort(Comparator.comparing(i -> i.start));

        // 3. 병합 로직 시작
        long totalDurationSeconds = mergeInterval(intervals);

        return totalDurationSeconds;
    }



    private List<Interval> changeInterval(List<AppSession> sessions){
        LocalDateTime now = LocalDateTime.now();

        List<Interval> intervals = sessions.stream()
                .map(s -> new Interval(
                        s.getStartTime(),
                        getEndTimeForSession(s, now)
                ))
                .collect(Collectors.toList());

        return intervals;
    }

    private long mergeInterval(List<Interval> intervals){
        Interval currentMerged = intervals.get(0);
        long totalDurationSeconds = 0;

        for (int i = 1; i < intervals.size(); i++) {
            Interval next = intervals.get(i);

            // 다음 시작 시간이 현재 끝 시간보다 빠르거나 같다면
            if (!next.start.isAfter(currentMerged.end)) {
                //다음 끝 시간이 현재 끝 시간보다 늦다면 확장
                if (next.end.isAfter(currentMerged.end)) {
                    currentMerged.end = next.end;
                }
                continue;
            }

            //현재까지 병합된 구간의 총 시간을 계산하여 누적
            totalDurationSeconds += Duration.between(currentMerged.start, currentMerged.end).getSeconds();
            currentMerged = next;
        }
        //마지막으로 남아있는 구간의 시간을 합산
        totalDurationSeconds += Duration.between(currentMerged.start, currentMerged.end).getSeconds();

        return totalDurationSeconds;
    }


    // 시간 구간을 저장하기 위한 임시 클래스
    private static class Interval {
        LocalDateTime start;
        LocalDateTime end;

        public Interval(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }


    private LocalDateTime getEndTimeForSession(AppSession session, LocalDateTime now) {
        // durationSeconds가 NULL이면 현재 실행 중이므로 'now'를 끝 시간으로 간주
        if (session.getDurationSeconds() == null) {
            return now;
        }
        return session.getEndTime();

    }




}
