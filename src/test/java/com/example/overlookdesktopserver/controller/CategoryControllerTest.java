package com.example.overlookdesktopserver.controller;

import com.example.overlookdesktopserver.dto.CategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;
    private static final String BASE_URL = "/api/v1/game/category";


    @Test
    void 카테고리Get테스트() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity(BASE_URL, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // Flask가 CODE / GAME / MUSIC / PRODUCT 반환하므로 이를 검증
        assertThat(response.getBody()).containsKeys("CODE", "GAME", "MUSIC", "PRODUCT");
    }


    @Test
    void 카테고리추가_Post_테스트() {


        CategoryRequest request = new CategoryRequest();
        request.setAppName("TestApp.exe");
        request.setCategoryName("CODE");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CategoryRequest> entity = new HttpEntity<>(request, headers);

        // Spring → Flask → Spring 순으로 응답 돌아옴
        ResponseEntity<String> response =
                restTemplate.postForEntity(BASE_URL, entity, String.class);

        // 201 CREATED이면 성공
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
                .contains("Request forwarded to Flask successfully");
    }
}
