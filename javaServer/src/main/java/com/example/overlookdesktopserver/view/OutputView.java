package com.example.overlookdesktopserver.view;

import com.example.overlookdesktopserver.entity.AppSession;
import org.springframework.stereotype.Component;

@Component
public class OutputView {

    public void pidIsPresent(Long pid){
        System.out.println("이미 활성화된 앱이 있습니다." + pid.toString());
    }

    public void cannotfountPid(Long pid){
        System.out.println("이 세션의 start 기록이 없습니다.." + pid.toString());
    }

    public void sucessedPutSession(AppSession appSession){
        System.out.println("세션이 저장되었습니다." + appSession.getAppName() + appSession.getPid());
    }

    public void sucessedFinishSession(AppSession appSession){
        System.out.println("세션이 종료되었습니다." + appSession.getAppName() + appSession.getPid());
    }

    public void categoryAddSuccess(String appName, String categoryName) {
        System.out.println(appName + " → " + categoryName + " 등록 성공");
    }

    public void categoryAddFail(String appName, String categoryName, String reason) {
        System.out.println("[Category FAIL] " + appName + " → " + categoryName + " 실패: " + reason);
    }

    public void categoryLoadFail(String reason) {
        System.out.println("카테고리 조회 실패: " + reason);
    }


}
