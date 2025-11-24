package com.example.overlookdesktopserver.service;

import com.example.overlookdesktopserver.dto.AppUsageRequest;
import com.example.overlookdesktopserver.entity.AppSession;
import com.example.overlookdesktopserver.repository.AppSessionRepository;
import com.example.overlookdesktopserver.view.OutputView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppSaveServiceTest {

    @Mock
    private AppSessionRepository appSessionRepository;

    @Mock
    private OutputView outputView;

    @InjectMocks
    private AppSaveService appSaveService;

    @Test
    void 새_세션_저장() {
        // given
        Long pid = 1234L;
        LocalDateTime eventTime = LocalDateTime.of(2025, 11, 24, 20, 30);

        AppUsageRequest request = new AppUsageRequest();
        request.setPid(pid);
        request.setAppName("Spotify.exe");
        request.setAppCategory("MUSIC");
        request.setEventTime(eventTime);
        request.setEventType("START");

        // 이 PID로 활성 세션이 아직 없다
        when(appSessionRepository.findByPidAndDurationSecondsIsNull(pid))
                .thenReturn(Optional.empty());

        // when
        appSaveService.processAppEvent(request);

        // then
        ArgumentCaptor<AppSession> captor = ArgumentCaptor.forClass(AppSession.class);
        verify(appSessionRepository, times(1)).save(captor.capture());
        AppSession saved = captor.getValue();

        assertEquals(pid, saved.getPid());
        assertEquals("Spotify.exe", saved.getAppName());
        assertEquals("MUSIC", saved.getAppCategory());
        assertEquals(eventTime, saved.getStartTime());
        assertNull(saved.getEndTime());
        assertNull(saved.getDurationSeconds());

        // 출력 로그도 한 번 호출되었는지 확인
        verify(outputView, times(1)).sucessedPutSession(saved);
        verify(outputView, never()).pidIsPresent(anyLong());
    }

    @Test
    void 이미_활성_세션이_있으면_무시() {
        // given
        Long pid = 1234L;
        AppUsageRequest request = new AppUsageRequest();
        request.setPid(pid);
        request.setAppName("Spotify.exe");
        request.setAppCategory("MUSIC");
        request.setEventTime(LocalDateTime.now());
        request.setEventType("START");

        // 이미 활성 세션이 존재한다고 가정
        when(appSessionRepository.findByPidAndDurationSecondsIsNull(pid))
                .thenReturn(Optional.of(new AppSession()));

        // when
        appSaveService.processAppEvent(request);

        // then
        //save가 안되고 pidIsPresent가 호출됨
        verify(appSessionRepository, never()).save(any(AppSession.class));
        verify(outputView, times(1)).pidIsPresent(pid);
        verify(outputView, never()).sucessedPutSession(any());
    }

    @Test
    void 활성_세션이_있으면_종료() {
        // given
        Long pid = 4321L;
        LocalDateTime startTime = LocalDateTime.of(2025, 11, 24, 20, 0);
        LocalDateTime stopTime  = LocalDateTime.of(2025, 11, 24, 20, 10); // 10분 후

        AppUsageRequest request = new AppUsageRequest();
        request.setPid(pid);
        request.setAppName("Spotify.exe");
        request.setAppCategory("MUSIC");
        request.setEventTime(stopTime);
        request.setEventType("STOP");

        AppSession existing = new AppSession();
        existing.setPid(pid);
        existing.setAppName("Spotify.exe");
        existing.setAppCategory("MUSIC");
        existing.setStartTime(startTime);
        // endTime / duration은 아직 null (활성 세션)

        when(appSessionRepository.findByPidAndDurationSecondsIsNull(pid))
                .thenReturn(Optional.of(existing));

        // when
        appSaveService.processAppEvent(request);

        // then
        ArgumentCaptor<AppSession> captor = ArgumentCaptor.forClass(AppSession.class);
        verify(appSessionRepository, times(1)).save(captor.capture());
        AppSession saved = captor.getValue();

        assertEquals(stopTime, saved.getEndTime());
        long expectedSeconds = Duration.between(startTime, stopTime).getSeconds();
        assertEquals(expectedSeconds, saved.getDurationSeconds());

        verify(outputView, times(1)).sucessedFinishSession(saved);
        verify(outputView, never()).cannotfountPid(anyLong());
    }

}
