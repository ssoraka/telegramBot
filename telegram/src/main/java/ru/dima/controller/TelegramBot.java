package ru.dima.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.dima.configuration.BotProperties;
import ru.dima.util.Words;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Component
public class TelegramBot extends TelegramLongPollingBot {
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final String YES = "да";
    private static final String NO = "нет";
    private static final String STOP = "прекрати меня спамить";
    private static final String CALENDAR = "/calendar";
    private static final String RECORD = "/record";

    private final String botUsername;
    ReplyKeyboardMarkup replyKeyboardMarkup;
    private Map<LocalDateTime, Boolean> records = new ConcurrentHashMap<>();

    Map<String, String> chats = new ConcurrentHashMap<>();

    public TelegramBot(BotProperties properties) {
        super(properties.getBotToken());
        this.botUsername = properties.getBotUsername();
        initKeyboard();
    }

    void initKeyboard()
    {
        //Создаем объект будущей клавиатуры и выставляем нужные настройки
        replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true); //подгоняем размер
        replyKeyboardMarkup.setOneTimeKeyboard(false); //скрываем после использования

        //Создаем список с рядами кнопок
        ArrayList<KeyboardRow> keyboardRows = new ArrayList<>();
        //Добавляем одну кнопку с текстом "да" наш ряд
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(YES), new KeyboardButton(NO))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(STOP))));
        keyboardRows.add(new KeyboardRow(List.of(new KeyboardButton(CALENDAR))));
        //добавляем лист с одним рядом кнопок в главный объект
        replyKeyboardMarkup.setKeyboard(keyboardRows);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            //Извлекаем из объекта сообщение пользователя
            Message inMess = update.getMessage();
            //Достаем из inMess id чата пользователя
            String chatId = inMess.getChatId().toString();
            //Получаем текст сообщения пользователя, отправляем в написанный нами обработчик
            SendMessage message = parseMessage(chatId, inMess.getText());

            log.info("{} {}: {}", inMess.getFrom().getUserName(), inMess.getFrom().getFirstName(), inMess.getText());

            if (inMess.getText().equals(STOP)) {
                chats.remove(chatId);
            } else {
                chats.put(chatId, inMess.getFrom().getUserName());
            }

            //Отправка в чат
            try {
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }


        } else if (update.hasCallbackQuery()) {
            try {
                if (update.getCallbackQuery().getData().matches(RECORD + "/\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2}:\\d{2}")) {
                    String dateText = update.getCallbackQuery().getData().replace(RECORD + "/", "");
                    LocalDateTime date = LocalDateTime.parse(dateText, DATE_TIME_FORMATTER);
                    records.put(date, true);

                    execute(EditMessageReplyMarkup.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .replyMarkup(dayTable(date.toLocalDate()))
                            .build());

                    execute(SendMessage.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .text("Вы записаны на " + dateText)
                            .build());

                } else if (update.getCallbackQuery().getData().matches(CALENDAR + "/\\d{2}-\\d{2}-\\d{4}")) {
                    String dateText = update.getCallbackQuery().getData().replace(CALENDAR + "/", "");
                    LocalDate date = LocalDate.parse(dateText, DATE_FORMATTER);
                    execute(EditMessageReplyMarkup.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .replyMarkup(calendar(date))
                            .build());
                } else if (update.getCallbackQuery().getData().matches("\\d{2}-\\d{2}-\\d{4}")) {
                    LocalDate date = LocalDate.parse(update.getCallbackQuery().getData(), DATE_FORMATTER);
                    execute(EditMessageReplyMarkup.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .messageId(update.getCallbackQuery().getMessage().getMessageId())
                            .replyMarkup(dayTable(date))
                            .build());
                } else {
                    execute(SendSticker.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .sticker(new InputFile(Words.randomGachiStikerFileId()))
                            .build());
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

    }

    private InlineKeyboardMarkup inline() {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> buttons1 = new ArrayList<>();
        buttons1.add(InlineKeyboardButton.builder()
                .text("Гачи")
                .callbackData("гачи стикер")
                .build());
        buttons.add(buttons1);

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
        return markupKeyboard;
    }

    private InlineKeyboardMarkup calendar() {
        return calendar(LocalDate.now());
    }
    private InlineKeyboardMarkup calendar(LocalDate date) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        LocalDate startOfMonth = LocalDate.of(date.getYear(), date.getMonth().getValue(), 1);


        header(buttons, startOfMonth);

        List<InlineKeyboardButton> button = new ArrayList<>();
        buttons.add(button);

        for (int i = 1; i < startOfMonth.getDayOfWeek().getValue(); i++) {
            button.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("empty")
                    .build());
        }

        for (int i = 0; i < startOfMonth.getMonth().maxLength(); i++) {
            if (button.size() == 7) {
                button = new ArrayList<>();
                buttons.add(button);
            }

            LocalDate day = startOfMonth.plusDays(i);

            button.add(InlineKeyboardButton.builder()
                    .text("" + (i + 1))
                    .callbackData(startOfMonth.plusDays(i).format(DATE_FORMATTER))
                    .build());
        }

        while (button.size() < 7) {
            button.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("empty")
                    .build());
        }


        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
        return markupKeyboard;
    }




    private InlineKeyboardMarkup dayTable(LocalDate date) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        LocalDateTime time = LocalDateTime.of(date, LocalTime.of(9, 0, 0));

        for (int i = 0; i < 10; i++) {
            if (records.containsKey(time.plusHours(i))) {
                continue;
            }

            List<InlineKeyboardButton> button = new ArrayList<>();
            buttons.add(button);
            button.add(InlineKeyboardButton.builder()
                    .text(time.plusHours(i).format(DATE_TIME_FORMATTER))
                    .callbackData(RECORD + "/" + time.plusHours(i).format(DATE_TIME_FORMATTER))
                    .build());
        }

        List<InlineKeyboardButton> button = new ArrayList<>();
        buttons.add(button);
        button.add(InlineKeyboardButton.builder()
                .text("назад")
                .callbackData(CALENDAR + "/" + date.format(DATE_FORMATTER))
                .build());

        InlineKeyboardMarkup markupKeyboard = new InlineKeyboardMarkup();
        markupKeyboard.setKeyboard(buttons);
        return markupKeyboard;
    }


    private void header(List<List<InlineKeyboardButton>> buttons, LocalDate startOfMonth) {
        List<InlineKeyboardButton> button = new ArrayList<>();
        buttons.add(button);
        button.add(InlineKeyboardButton.builder()
                .text("<")
                .callbackData(CALENDAR + "/" + startOfMonth.minusMonths(1).format(DATE_FORMATTER))
                .build());
        button.add(InlineKeyboardButton.builder()
                .text("месяц " + startOfMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                .callbackData("1")
                .build());
        button.add(InlineKeyboardButton.builder()
                .text(">")
                .callbackData(CALENDAR + "/" + startOfMonth.plusMonths(1).format(DATE_FORMATTER))
                .build());

        button = new ArrayList<>();
        buttons.add(button);
        for (String s : new String[]{"П", "В", "С", "Ч", "П", "С", "В"}) {
            button.add(InlineKeyboardButton.builder()
                    .text(s)
                    .callbackData("1")
                    .build());
        }
    }


    public void send() {
        List<String> delete = new ArrayList<>();

        for (Map.Entry<String, String> e : chats.entrySet()) {
            String word = Words.randomBadWord();

            SendMessage message = SendMessage.builder()
                    .chatId(e.getKey())
                    .text(String.format("%s %s!",e.getValue(), word))
                    .replyMarkup(inline())
                    .protectContent(true)
                    .build();
            try {
                execute(message);
                log.info("Уведомил {}, что он(а) {}", e.getValue(), word);
            } catch (TelegramApiException ex) {
                log.info("{}-{} тебя заблочил: \n{}", e.getValue(), Words.randomBadWord(), ex.getMessage());
                ex.printStackTrace();
                delete.add(e.getKey());
            }
        }
        delete.forEach(key -> chats.remove(key));
    }

    public SendMessage parseMessage(String chatId, String textMsg) {
        String response;
        ReplyKeyboard replyKeyboard = replyKeyboardMarkup;

        //Сравниваем текст пользователя с нашими командами, на основе этого формируем ответ
        if (textMsg.equals("/start"))
            response = "Привет.";
        else if(textMsg.equals(NO))
            response = "Пидора ответ!";
        else if(textMsg.equals(YES))
            response = "Пизда!";
        else if(textMsg.equals(STOP))
            response = "Сорян, умолк.";
        else if(textMsg.equals(CALENDAR)) {
            response = "Календарь";
            replyKeyboard = calendar();
        } else
            response = "Что ты несешь?";

        return SendMessage.builder()
                .chatId(chatId)
                .text(response)
                .replyMarkup(replyKeyboard)
                .protectContent(true)
                .build();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
