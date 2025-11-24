package com.example.overlookdesktopserver.service;


import com.example.overlookdesktopserver.dto.AppUsageRequest;
import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;
import com.example.overlookdesktopserver.view.OutputView;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AppSaveService {


    private final AppSessionRepository appSessionRepository;
    private final OutputView outputView;

    public AppSaveService(AppSessionRepository appSessionRepository, OutputView outputView){
        this.appSessionRepository = appSessionRepository;
        this.outputView = outputView;
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


    private void putSession(Long pid, AppUsageRequest appUsageRequest){
        //만약 이미 활성화된 같은 앱이 있다면 무시
        if(appSessionRepository.findByPidAndDurationSecondsIsNull(pid).isPresent()){
            outputView.pidIsPresent(pid);
            return;
        }
        AppSession appSession = new AppSession();
        appSession.setPid(pid);
        appSession.setAppName(appUsageRequest.getAppName());
        appSession.setStartTime(appUsageRequest.getEventTime());
        appSession.setAppCategory(appUsageRequest.getAppCategory());
        appSessionRepository.save(appSession);
        outputView.sucessedPutSession(appSession);

    }

    private void putEndtimeAndDuration(Long pid, AppUsageRequest appUsageRequest){
        appSessionRepository.findByPidAndDurationSecondsIsNull(pid)
                .ifPresentOrElse(session -> {
                    long duration = Duration.between(session.getStartTime(), appUsageRequest.getEventTime()).getSeconds();

                    session.setEndTime(appUsageRequest.getEventTime());
                    session.setDurationSeconds(duration);
                    appSessionRepository.save(session);
                    outputView.sucessedFinishSession(session);

                }, () -> {
                    //start 기록이 없다고 로그 띄우기
                    outputView.cannotfountPid(pid);

                });
    }

}
