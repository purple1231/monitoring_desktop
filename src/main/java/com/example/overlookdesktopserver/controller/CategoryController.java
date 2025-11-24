package com.example.overlookdesktopserver.controller;

import com.example.overlookdesktopserver.dto.CategoryRequest;
import com.example.overlookdesktopserver.view.OutputView;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/game/category")
public class CategoryController {

    private final RestTemplate restTemplate;
    private final OutputView outputView;


    private static final String PYTHON_AGENT_URL = "http://localhost:5000/api/v1/game/category";


    //RestTemplate 주입
    @Autowired
    public CategoryController(RestTemplate restTemplate, OutputView outputView) {
        this.restTemplate = restTemplate;
        this.outputView = outputView;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<String>>> getAllCategoriesFromFlask() {
        try {
            // Flask 에 요청하여 모든 카테고리별 앱 이름들을 입력받음
            ResponseEntity<Map<String, List<String>>> response = restTemplate.exchange(
                    PYTHON_AGENT_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, List<String>>>() {}
            );

            // Flask에서 받은 응답을 그대로 클라이언트에게 반환.
            return new ResponseEntity<>(response.getBody(), response.getStatusCode());

        } catch (Exception e) {
            // 통신 오류 발생 시 500 반환
            outputView.categoryLoadFail(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @PostMapping
    public ResponseEntity<String> addAppToCategory(@RequestBody CategoryRequest request) {
        String app = request.getAppName();
        String category = request.getCategoryName();

        try {
            // flask에 요청 -> string 받기
            String responseBody = restTemplate.postForObject(PYTHON_AGENT_URL, request, String.class);
            outputView.categoryAddSuccess(app, category);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Request forwarded to Flask successfully. Response: " + responseBody);

        } catch (Exception e) {
            outputView.categoryAddFail(app, category, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error communicating with Flask Agent.");
        }
    }
}