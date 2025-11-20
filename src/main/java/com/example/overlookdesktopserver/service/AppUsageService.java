package com.example.overlookdesktopserver.service;


import com.example.overlookdesktopserver.dto.AppUsageRequest;
import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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




}
