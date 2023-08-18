package ru.dima.controller;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
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
    private static final String YES = "да";
    private static final String NO = "нет";
    private static final String STOP = "прекрати меня спамить";
    private static final String CALENDAR = "/calendar";

    private final String botUsername;
    ReplyKeyboardMarkup replyKeyboardMarkup;

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
        //Создаем один ряд кнопок и добавляем его в список
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRows.add(keyboardRow);
        //Добавляем одну кнопку с текстом "да" наш ряд
        keyboardRow.add(new KeyboardButton(YES));
        keyboardRow.add(new KeyboardButton(NO));
        keyboardRow.add(new KeyboardButton(STOP));
        keyboardRow.add(new KeyboardButton(CALENDAR));
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
            if (update.getCallbackQuery().getData().matches("\\d{2}-\\d{2}-\\d{4}")) {
                try {
                    execute(SendMessage.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .text(update.getCallbackQuery().getData())
                            .build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else {

                try {
                    execute(SendSticker.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .sticker(new InputFile(Words.randomGachiStikerFileId()))
                            .build());
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
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
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        header(buttons);

        LocalDate startOfMonth = LocalDate.now().minusDays(LocalDate.now().getDayOfMonth());

        List<InlineKeyboardButton> button = new ArrayList<>();
        buttons.add(button);

        for (int i = 0; i < startOfMonth.getDayOfWeek().getValue(); i++) {
            button.add(InlineKeyboardButton.builder()
                    .text(" ")
                    .callbackData("empty")
                    .build());
        }

        for (int i = 1; i <= startOfMonth.getMonth().maxLength(); i++) {
            if (button.size() == 7) {
                button = new ArrayList<>();
                buttons.add(button);
            }

            LocalDate day = startOfMonth.plusDays(i);

            button.add(InlineKeyboardButton.builder()
                    .text("" + i)
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

    private void header(List<List<InlineKeyboardButton>> buttons) {
        List<InlineKeyboardButton> button = new ArrayList<>();
        buttons.add(button);
        button.add(InlineKeyboardButton.builder()
                .text("месяц " + LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()))
                .callbackData("1")
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
