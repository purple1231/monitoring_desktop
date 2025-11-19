package com.example.overlookdesktopserver.controller;

import com.example.overlookdesktopserver.dto.AppUsageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class AppUsageController {


    @PostMapping("/log")
    public String logActivity(@RequestBody AppUsageRequest request){

    }
}
