package ru.dima.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.dima.controller.TelegramBot;

@Component
@RequiredArgsConstructor
public class MyScheduler {
    private final TelegramBot telegramBot;


    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        telegramBot.send();
    }
}
