package com.example.overlookdesktopserver.dto;
import java.time.LocalDateTime;

public class AppUsageRequest {

    private long pid;
    private String appName;
    private String eventType;
    private LocalDateTime eventTime;
    private String appCategory;

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

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventTyppe) {
        this.eventType = eventTyppe;
    }

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getAppCategory() {
        return appCategory;
    }

    public void setAppCategory(String appCategory) {
        this.appCategory = appCategory;
    }
}
