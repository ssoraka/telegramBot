package ru.dima.controller;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendSticker;
import org.telegram.telegrambots.meta.api.methods.stickers.GetStickerSet;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.dima.configuration.BotProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final String YES = "да";
    private static final String NO = "нет";
    private static final String STOP = "прекрати меня спамить";

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
            String response = parseMessage(inMess.getText());

            if (inMess.getText().equals(STOP)) {
                chats.remove(chatId);
            } else {
                chats.put(chatId, inMess.getFrom().getUserName());
            }

            //Добавляем в наше сообщение id чата а также наш ответ
            SendMessage message = SendMessage.builder()
                    .chatId(chatId)
                    .text(response)
                    .replyMarkup(replyKeyboardMarkup)
                    .protectContent(true)
                    .build();


            //Отправка в чат
            try {
                execute(message);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }


        } else if (update.hasCallbackQuery()) {
            try {
                execute(SendSticker.builder()
                        .chatId(update.getCallbackQuery().getMessage().getChatId())
                        .sticker(new InputFile("CAACAgIAAxkBAAIC5mTY6hD1yq5hm5B8DvMsoQZThGXuAAJ9FgACMPYAAUgXnlYRZQ8jSzAE"))
                        .build());
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

    public void send() {
        List<String> delete = new ArrayList<>();

        for (Map.Entry<String, String> e : chats.entrySet()) {
            SendMessage message = SendMessage.builder()
                    .chatId(e.getKey())
                    .text(e.getValue() + " лох!")
                    .replyMarkup(inline())
                    .protectContent(true)
                    .build();
            try {
                execute(message);
                System.out.println(String.format("Уведомил %s", e.getValue()));
            } catch (TelegramApiException ex) {
                System.out.println(String.format("%s тебя заблочил: \n%s", e.getValue(), ex.getMessage()));
                ex.printStackTrace();
                delete.add(e.getKey());
            }
        }
        delete.forEach(key -> chats.remove(key));
    }

    public String parseMessage(String textMsg) {
        String response;

        //Сравниваем текст пользователя с нашими командами, на основе этого формируем ответ
        if(textMsg.equals("/start"))
            response = "Привет.";
        else if(textMsg.equals(NO))
            response = "Пидора ответ!";
        else if(textMsg.equals(YES))
            response = "Пизда!";
        else if(textMsg.equals(STOP))
            response = "Сорян, умолк.";
        else
            response = "Что ты несешь?";

        return response;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
