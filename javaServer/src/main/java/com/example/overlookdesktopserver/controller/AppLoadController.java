package com.example.overlookdesktopserver.controller;

import com.example.overlookdesktopserver.service.AppLoadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class AppLoadController {

    private final AppLoadService appLoadService;
    public AppLoadController(AppLoadService appLoadService){
        this.appLoadService  =appLoadService;
    }

    // 1. 앱별 시간 보기 (전체)
    @GetMapping("/apps/all")
    public ResponseEntity<Map<String, Long>> getAllAppStats() {
        return ResponseEntity.ok(appLoadService.getAggregatedUsageStats());
    }

    // 2. 앱별 시간 보기 (특정 앱)
    @GetMapping("/apps/filter")
    public ResponseEntity<Map<String, Long>> getFilteredAppStats(@RequestParam("apps") List<String> apps) {
        return ResponseEntity.ok(appLoadService.getFilteredAppStats(apps));
    }

    // 3. 카테고리별 시간 보기 (전체)
    @GetMapping("/categories/all")
    public ResponseEntity<Map<String, Long>> getCategoryStats() {
        return ResponseEntity.ok(appLoadService.getMergedCategoryStats());
    }


}

