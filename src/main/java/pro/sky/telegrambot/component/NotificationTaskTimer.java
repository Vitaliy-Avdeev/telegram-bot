package pro.sky.telegrambot.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

@Component
public class NotificationTaskTimer {
    private final NotificationTaskRepository notificationTaskRepository;
    private final SendAnAssistant sendAnAssistant;

    public NotificationTaskTimer(NotificationTaskRepository notificationTaskRepository, SendAnAssistant sendAnAssistant) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.sendAnAssistant = sendAnAssistant;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)

    @Transactional
    public void task() {
        notificationTaskRepository.findAllByNotificationDateTime(
                LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
        ).forEach(notificationTask -> {
            sendAnAssistant.sendMessage(notificationTask.getUserId(), notificationTask.getMessage());
            notificationTaskRepository.delete(notificationTask);
        });
    }
}
