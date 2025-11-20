package com.example.overlookdesktopserver.controller;

import com.example.overlookdesktopserver.dto.AppUsageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/game")
public class AppUsageController {


    @PostMapping("/log")
    public ResponseEntity<Void> logActivity(@RequestBody AppUsageRequest request){
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
