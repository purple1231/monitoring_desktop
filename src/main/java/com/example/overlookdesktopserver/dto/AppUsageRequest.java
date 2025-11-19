package com.example.overlookdesktopserver.dto;
import java.time.LocalDateTime;

public class AppUsageRequest {

    private long pid;
    private String appName;
    private String eventTyppe;
    private LocalDateTime eventTime;


    public AppUsageRequest() {

    }


    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEventTyppe() {
        return eventTyppe;
    }

    public void setEventTyppe(String eventTyppe) {
        this.eventTyppe = eventTyppe;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}
