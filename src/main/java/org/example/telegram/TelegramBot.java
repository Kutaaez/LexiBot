package org.example.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final GeminiService geminiService;

    private final Map<Long, String> userQuestions = new HashMap<>();

    public TelegramBot() {
        BotConfig config = new BotConfig();
        this.botToken = config.getTelegramBotToken();
        this.botUsername = config.getTelegramBotUsername();
        this.geminiService = new GeminiService();
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String userMessage = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            // Приветствие без кнопок
            if (userMessage.equalsIgnoreCase("/start") || userMessage.equalsIgnoreCase("привет")) {
                sendResponse(chatId, "👋 Привет! Я ваш юридический помощник. Напишите ваш вопрос, и я помогу разобраться.");
                return;
            }

            // Обработчик кнопок
            if (userMessage.equalsIgnoreCase("Достаточно")) {
                sendPreliminaryAnswer(chatId);
                return;
            }
            if (userMessage.equalsIgnoreCase("Я могу дополнить")) {
                sendResponse(chatId, "📝 Пожалуйста, уточните ваш вопрос. Какие детали важны?");
                return;
            }

            // Запоминаем вопрос пользователя
            userQuestions.put(chatId, userMessage);

            // Запрашиваем у пользователя уточнение или готовность к ответу (кнопки)
            sendResponseWithKeyboard(chatId,
                    "Я могу дать предварительный ответ на основе законов Казахстана или уточнить детали. Как вам удобнее?",
                    "Достаточно", "Я могу дополнить");
        }
    }

    private void sendPreliminaryAnswer(Long chatId) {
        String userQuery = userQuestions.getOrDefault(chatId, "Неизвестный вопрос");

        SendMessage initialMessage = new SendMessage();
        initialMessage.setChatId(chatId.toString());
        initialMessage.setText("🔎 Анализирую ваш вопрос... Ожидайте ⏳");

        try {
            Message sentMessage = execute(initialMessage);

            // Запрос к Gemini
            String aiResponse = geminiService.getLegalAnswer(userQuery, true, true);
            String fullResponse = aiResponse + "\n\n⚖️ В сложных случаях рекомендуем обратиться к профессиональным юристам.";

            // Добавляем кнопку с ссылкой на юристов
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            List<InlineKeyboardButton> row = new ArrayList<>();

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("📞 Связаться с юристами LexTrust");
            button.setUrl("https://lextrust.kz");

            row.add(button);
            keyboard.add(row);
            markup.setKeyboard(keyboard);

            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(sentMessage.getMessageId());
            editMessage.setText(fullResponse);
            editMessage.setReplyMarkup(markup);

            execute(editMessage);
        } catch (TelegramApiException | IOException e) {
            sendResponse(chatId, "❌ Ошибка при обработке запроса: " + e.getMessage());
        }
    }

    private void sendResponse(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private void sendResponseWithKeyboard(Long chatId, String text, String option1, String option2) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(option1);
        if (option2 != null) row.add(option2);
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }
}
