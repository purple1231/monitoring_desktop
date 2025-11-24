package com.example.overlookdesktopserver.service;

import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppLoadServiceTest {

    @Mock
    private AppSessionRepository appSessionRepository;

    @InjectMocks
    private AppLoadService appLoadService;


    @Test
    void 중첩시간이_있는_앱별_사용시간_계산() {
        // given
        LocalDateTime base = LocalDateTime.of(2025, 11, 24, 10, 0, 0);

        AppSession s1 = new AppSession();
        s1.setAppName("Spotify.exe");
        s1.setAppCategory("MUSIC");
        s1.setStartTime(base);                      // 10:00
        s1.setEndTime(base.plusMinutes(10));  // 10:10
        s1.setDurationSeconds(600L);

        AppSession s2 = new AppSession();
        s2.setAppName("Spotify.exe");
        s2.setAppCategory("MUSIC");
        s2.setStartTime(base.plusMinutes(5));       // 10:05
        s2.setEndTime(base.plusMinutes(20));  // 10:20
        s2.setDurationSeconds(900L);

        // 두 세션은 10:00~10:20 까지 겹치는 구간이 있다 -> 병합 후 총 시간은 20분 = 1200초
        when(appSessionRepository.findAll()).thenReturn(Arrays.asList(s1, s2));

        // when
        Map<String, Long> result = appLoadService.getAggregatedUsageStats();

        // then
        assertNotNull(result);
        assertTrue(result.containsKey("Spotify.exe"));
        assertEquals(1200L, result.get("Spotify.exe")); // 20분 = 1200초
    }


    @Test
    void 앱_이름_대소문자_처리() {
        // given
        LocalDateTime base = LocalDateTime.of(2025, 11, 24, 11, 0, 0);

        AppSession s1 = new AppSession();
        s1.setAppName("Spotify.exe");
        s1.setAppCategory("MUSIC");
        s1.setStartTime(base);
        s1.setEndTime(base.plusMinutes(5));
        s1.setDurationSeconds(300L);

        when(appSessionRepository.findAll()).thenReturn(List.of(s1));

        // when
        Map<String, Long> filtered = appLoadService.getFilteredAppStats(List.of("spotify.exe"));

        // then
        assertNotNull(filtered);
        assertTrue(filtered.containsKey("spotify.exe")); // 키는 요청에 맞춰 들어감
        assertEquals(300L, filtered.get("spotify.exe"));
    }


    @Test
    void 중첩시간이_있는_카테고리별_사용시간_계산() {
        // given
        LocalDateTime base = LocalDateTime.of(2025, 11, 24, 12, 0, 0);

        AppSession s1 = new AppSession();
        s1.setAppName("Spotify.exe");
        s1.setAppCategory("MUSIC");
        s1.setStartTime(base);
        s1.setEndTime(base.plusMinutes(10));
        s1.setDurationSeconds(600L);

        AppSession s2 = new AppSession();
        s2.setAppName("code.exe");
        s2.setAppCategory("CODE");
        s2.setStartTime(base.plusMinutes(5));
        s2.setEndTime(base.plusMinutes(15));
        s2.setDurationSeconds(600L);

        when(appSessionRepository.findAll()).thenReturn(List.of(s1, s2));

        // when
        Map<String, Long> result = appLoadService.getMergedCategoryStats();

        // then
        assertNotNull(result);
        assertEquals(600L, result.get("MUSIC"));
        assertEquals(600L, result.get("CODE"));
    }
}
