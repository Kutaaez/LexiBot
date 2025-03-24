package org.example.telegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {
    private final String botToken;
    private final String botUsername;
    private final GeminiService geminiService;
    private final Map<Long, String> userQuestions = new HashMap<>();
    private final Map<Long, String> userCategories = new HashMap<>();
    private final Map<Long, Integer> processingMessages = new HashMap<>();
    private final LanguageService languageService = new LanguageService();

    public TelegramBot() {
        BotConfig config = new BotConfig();
        this.botToken = config.getTelegramBotToken();
        this.botUsername = config.getTelegramBotUsername();
        this.geminiService = new GeminiService(config.getGeminiApiKey());
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
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }
    private void editMessage(Long chatId, Integer messageId, String newText) {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        editMessage.setReplyMarkup(null);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) {
        Long chatId = message.getChatId();
        String text = message.getText().toLowerCase();
        String userLang = languageService.getUserLanguage(chatId);

        // Используем HashMap вместо Map.of()
        Map<String, Runnable> commands = new HashMap<>();
        commands.put("/start", () -> startInteraction(chatId));
        commands.put("привет", () -> startInteraction(chatId));
        commands.put("hello", () -> startInteraction(chatId));
        commands.put("сәлем", () -> startInteraction(chatId));
        commands.put("🌍 сменить язык", () -> sendLanguageSelection(chatId));
        commands.put("🌍 change language", () -> sendLanguageSelection(chatId));
        commands.put("🌍 тілді өзгерту", () -> sendLanguageSelection(chatId));
        commands.put("задать еще вопрос", () -> restartInteraction(chatId, userLang));
        commands.put("ask another question", () -> restartInteraction(chatId, userLang));
        commands.put("тағы бір сұрақ қою", () -> restartInteraction(chatId, userLang));
        commands.put("связаться", () -> sendResponse(chatId, MessageService.getMessage(userLang, "contact")));
        commands.put("contact", () -> sendResponse(chatId, MessageService.getMessage(userLang, "contact")));
        commands.put("байланысу", () -> sendResponse(chatId, MessageService.getMessage(userLang, "contact")));
        commands.put("достаточно", () -> sendPreliminaryAnswer(chatId));
        commands.put("enough", () -> sendPreliminaryAnswer(chatId));
        commands.put("жеткілікті", () -> sendPreliminaryAnswer(chatId));
        commands.put("я могу дополнить", () -> sendResponse(chatId, MessageService.getMessage(userLang, "detail_prompt")));
        commands.put("i can add more", () -> sendResponse(chatId, MessageService.getMessage(userLang, "detail_prompt")));
        commands.put("мен толықтыра аламын", () -> sendResponse(chatId, MessageService.getMessage(userLang, "detail_prompt")));

        // Проверяем, есть ли команда в мапе
        if (commands.containsKey(text)) {
            commands.get(text).run();
            return;
        }

        // Проверяем, выбрал ли пользователь категорию
        if (!userCategories.containsKey(chatId)) {
            sendCategorySelection(chatId);
        } else {
            userQuestions.put(chatId, userQuestions.getOrDefault(chatId, "") + " " + text);

            sendResponseWithInlineKeyboard(
                    chatId,
                    MessageService.getMessage(userLang, "choose"),
                    MessageService.getMessage(userLang, "enough"),
                    MessageService.getMessage(userLang, "i_can_add_more")
            );
        }
    }


    // Метод для начального взаимодействия
    private void startInteraction(Long chatId) {
        sendResponse(chatId, "Сәлем! / Привет! / Hi! \n Тіл таңданыз / Выберите язык / Choose language");
        sendLanguageSelection(chatId);
    }

    // Метод для повторного вопроса
    private void restartInteraction(Long chatId, String userLang) {
        sendResponse(chatId, MessageService.getMessage(userLang, "ask_again"));
        sendCategorySelection(chatId);
    }


    private void sendResponseWithInlineKeyboard(Long chatId, String messageText, String option1, String option2) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(messageText);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                List.of(createButton(option1, "enough")),
                List.of(createButton(option2, "add_more"))
        );

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения с inline-кнопками: " + e.getMessage());
        }
    }

    private void sendLanguageSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🌍 Выберите язык | Choose language | Тілді таңдаңыз:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(Collections.singletonList(createButton("🇷🇺 Русский", "set_lang_ru")));
        keyboard.add(Collections.singletonList(createButton("🇬🇧 English", "set_lang_en")));
        keyboard.add(Collections.singletonList(createButton("🇰🇿 Қазақша", "set_lang_kz")));

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            Message sentMessage = execute(message);
            processingMessages.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения: " + e.getMessage());
        }
    }

    private void sendResponseWithKeyboard(Long chatId, String text, String... buttonLabels) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow currentRow = new KeyboardRow();

        for (String label : buttonLabels) {
            if (currentRow.size() >= 3) { // Если в ряду уже 3 кнопки, создаем новый ряд
                keyboardRows.add(currentRow);
                currentRow = new KeyboardRow();
            }
            currentRow.add(label);
        }

        if (!currentRow.isEmpty()) {
            keyboardRows.add(currentRow);
        }

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки сообщения в чат " + chatId + ": " + e.getMessage());
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = callbackQuery.getData();
        String userLang = languageService.getUserLanguage(chatId);
        if (callbackData.equals("ask_again")) {
            restartInteraction(chatId, userLang);
            return;
        }
        if (callbackData.equals("contact")) {
            sendResponse(chatId, MessageService.getMessage(userLang, "contact"));
            return;
        }
        if (callbackData.startsWith("set_lang_")) {
            String newLang = switch (callbackData) {
                case "set_lang_ru" -> "ru";
                case "set_lang_en" -> "en";
                case "set_lang_kz" -> "kz";
                default -> userLang;
            };

            languageService.setUserLanguage(chatId, newLang);
            deleteMessage(chatId, messageId); // Удаляем сообщение с кнопками выбора языка
            sendResponse(chatId, MessageService.getMessage(newLang, "language_saved"));

            // Добавляем отправку категорий после выбора языка
            sendCategorySelection(chatId);
            return;
        }

        Map<String, String> categoryNames = Map.of(
                "auto", MessageService.getMessage(userLang, "auto"),
                "labor", MessageService.getMessage(userLang, "labor"),
                "real_estate", MessageService.getMessage(userLang, "real_estate"),
                "family", MessageService.getMessage(userLang, "family"),
                "other", MessageService.getMessage(userLang, "other")
        );

        if (categoryNames.containsKey(callbackData)) {
            userCategories.put(chatId, callbackData);
            sendResponse(chatId, MessageService.getMessage(userLang, "selected_category") + ": " +
                    categoryNames.get(callbackData) + ". " +
                    MessageService.getMessage(userLang, "ask_question"));
        }
        if (callbackData.equals("enough")) {
            if (userQuestions.containsKey(chatId) && userQuestions.get(chatId).length() > 0) {
                sendPreliminaryAnswer(chatId);
                userQuestions.remove(chatId); // Очистка после отправки
            } else {
                sendResponse(chatId, MessageService.getMessage(userLang, "no_text_to_send"));
            }
            return;
        }

// Обработка кнопки "Я могу добавить еще"
        if (callbackData.equals("add_more")) {
            sendResponse(chatId, MessageService.getMessage(userLang, "detail_prompt"));
            return;
        }

    }



    private void sendFollowUpOptions(Long chatId) {
        final String userLang = languageService.getUserLanguage(chatId); // Определяем язык пользователя

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(MessageService.getMessage(userLang, "follow_up")); // Локализованный текст

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                List.of(createButton(MessageService.getMessage(userLang, "contact_us"), "contact")),
                List.of(createButton(MessageService.getMessage(userLang, "ask_more"), "ask_again"))
        );
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка отправки follow-up сообщений пользователю " + chatId + ": " + e.getMessage());
        }
    }



    private void sendPreliminaryAnswer(Long chatId) {
        final String userLang = languageService.getUserLanguage(chatId);

        // Отправляем сообщения об анализе и ожидании
        Message analyzingMessage = sendResponse(chatId, MessageService.getMessage(userLang, "analyzing"));
        Message waitingMessage = sendResponse(chatId, MessageService.getMessage(userLang, "waiting"));

        if (waitingMessage == null) return;

        processingMessages.put(chatId, waitingMessage.getMessageId());

        // Получаем запрос пользователя и категорию
        final String userQuery = userQuestions.getOrDefault(chatId, MessageService.getMessage(userLang, "unknown_question"));
        final String category = userCategories.getOrDefault(chatId, MessageService.getMessage(userLang, "other"));

        // Запрашиваем у ИИ юридический ответ
        final String aiResponse = geminiService.getLegalAnswer(languageService.getUserLanguage(chatId), userQuery, true, true, category);

        // Удаляем сообщение "Анализируем..."
        if (analyzingMessage != null) {
            deleteMessage(chatId, analyzingMessage.getMessageId());
        }

        // Редактируем сообщение "Ожидайте..." или отправляем новый ответ
        if (processingMessages.containsKey(chatId)) {
            int messageId = processingMessages.remove(chatId);
            editMessage(chatId, messageId, aiResponse + "\n\n⚖️ " + MessageService.getMessage(userLang, "legal_advice"));
        } else {
            sendResponse(chatId, aiResponse + "\n\n⚖️ " + MessageService.getMessage(userLang, "legal_advice"));
        }

        // Отправляем follow-up кнопки
        sendFollowUpOptions(chatId);

        // Очищаем временные данные
        userQuestions.remove(chatId);
        userCategories.remove(chatId);
    }





    private void sendCategorySelection(Long chatId) {
        final String language = languageService.getUserLanguage(chatId);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(MessageService.getMessage(language, "category_prompt"));

        // Создаем кнопки выбора категории
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = Arrays.asList(
                Arrays.asList(
                        createButton(MessageService.getMessage(language, "auto"), "auto"),
                        createButton(MessageService.getMessage(language, "labor"), "labor")
                ),
                Arrays.asList(
                        createButton(MessageService.getMessage(language, "real_estate"), "real_estate"),
                        createButton(MessageService.getMessage(language, "family"), "family")
                ),
                Collections.singletonList(
                        createButton(MessageService.getMessage(language, "other"), "other")
                )
        );

        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке выбора категории пользователю " + chatId + ": " + e.getMessage());
        }
    }




    private InlineKeyboardButton createButton(final String category, final String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(category);
        button.setCallbackData(callbackData);
        return button;
    }

    private Message sendResponse(final Long chatId, final String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            return execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при отправке сообщения пользователю " + chatId + ": " + e.getMessage());
            return null;
        }
    }


    private void editMessage(final Long chatId, final int messageId, final String newText) {
        try {
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(chatId.toString());
            editMessage.setMessageId(messageId);
            editMessage.setText(newText);
            execute(editMessage);
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при редактировании сообщения " + messageId + " для пользователя " + chatId + ": " + e.getMessage());
        }
    }

    private void deleteMessage(final Long chatId, final int messageId) {
        try {
            execute(new DeleteMessage(chatId.toString(), messageId));
        } catch (TelegramApiException e) {
            System.err.println("Ошибка при удалении сообщения " + messageId + " для пользователя " + chatId + ": " + e.getMessage());
        }
    }

}
