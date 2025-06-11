package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.component.SendAnAssistant;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final static String WELCOME_TEXT =
            "Привет! Я бот:)! Напоминаю вам о выполнении задачи в формате: 01.01.2022 20:00 Сделать домашнюю работу";

    private static final Pattern PATTERN = Pattern.compile("(\\d{2}.\\d{2}.\\d{4}\\s\\d{2}:\\d{2})(\\s+)(.+)");
    private static final LocalDateTime FORMATTER = LocalDateTime.parse("01.01.2022 20:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

    @Autowired
    private final TelegramBot telegramBot;
    private final NotificationTaskService notificationTaskService;
    private final SendAnAssistant sendAnAssistant;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskService notificationTaskService, SendAnAssistant sendAnAssistant) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
        this.sendAnAssistant = sendAnAssistant;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.forEach(update -> {
                logger.info("Processing update: {}", update);
                String text = update.message().text();
                Long chatId = update.message().chat().id();
                if ("/start".equals(text)) {
                    sendAnAssistant.sendMessage(chatId, WELCOME_TEXT);
                } else {
                    Matcher matcher = PATTERN.matcher(text);
                    LocalDateTime localDateTime;

                    if (matcher.find() && (localDateTime = parse(matcher.group(1))) != null) {
                        String message = matcher.group(3);
                        notificationTaskService.create(chatId, message, localDateTime);
                        sendAnAssistant.sendMessage(chatId, "Задача запланирована");
                    } else {
                        sendAnAssistant.sendMessage(chatId, "Некорректный формат сообщения!");
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @Nullable
    private LocalDateTime parse(String localDateTime) {
        try {
            return LocalDateTime.parse(localDateTime);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

