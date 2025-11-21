package com.example.overlookdesktopserver.service;


import com.example.overlookdesktopserver.dto.AppUsageRequest;
import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AppUsageService {


    private final AppSessionRepository appSessionRepository;

    public AppUsageService(AppSessionRepository appSessionRepository){
        this.appSessionRepository = appSessionRepository;
    }

    @Transactional
    public void processAppEvent(AppUsageRequest appUsageRequest){
        String eventType = appUsageRequest.getEventType();
        Long pid = appUsageRequest.getPid();

        if("START".equals(eventType)){
            putSession(pid, appUsageRequest);
        }
        else if("STOP".equals(eventType)){
            putEndtimeAndDuration(pid, appUsageRequest);
        }
    }

    public Map<String, Long> getAggregatedUsageStats(){
        Map<String, Long> finalStats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();


        //이미 종료된 세션들의 시간 합산
        appSessionRepository.findCompletedUsageStats().forEach(
                result -> {
                    String appName = (String) result[0];
                    Long totalDuration = (Long) result[1];
                    finalStats.put(appName, totalDuration);
                });

        //지금도 실행중인 세션(앱)이 있다면 현재시간 - 시작시간
        List<AppSession> activeSessions = appSessionRepository.findByDurationSecondsIsNull();

        for (AppSession session : activeSessions) {
            long liveDuration = Duration.between(session.getStartTime(), now).getSeconds();
            finalStats.merge(session.getAppName(), liveDuration, Long::sum);
        }
        return finalStats;
    }

    private void putSession(Long pid, AppUsageRequest appUsageRequest){
        //만약 이미 활성화된 같은 앱이 있다면 무시
        if(appSessionRepository.findByPidAndDurationSecondsIsNull(pid).isPresent()){
            //안된다고 로그띄우기
            return;
        }
        AppSession appSession = new AppSession();
        appSession.setPid(pid);
        appSession.setAppName(appUsageRequest.getAppName());
        appSession.setStartTime(appUsageRequest.getEventTime());
        appSession.setAppCategory(appUsageRequest.getAppCategory());
        appSessionRepository.save(appSession);
        //출력 로그 띄우기
    }

    private void putEndtimeAndDuration(Long pid, AppUsageRequest appUsageRequest){
        appSessionRepository.findByPidAndDurationSecondsIsNull(pid)
                .ifPresentOrElse(session -> {
                    long duration = Duration.between(session.getStartTime(), appUsageRequest.getEventTime()).getSeconds();

                    session.setEndTime(appUsageRequest.getEventTime());
                    session.setDurationSeconds(duration);
                    appSessionRepository.save(session);
                    //끝났다고 로그

                }, () -> {
                    //start 기록이 없다고 로그 띄우기
                });
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
