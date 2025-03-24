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

        switch (text) {
            case "/start", "привет", "hello", "сәлем" -> {
                sendResponse(chatId,"Сәлем! / Привет! / Hi! \n Тіл таңданыз / Выберите язык / Choose language");
                sendLanguageSelection(chatId);
            }
            case "🌍 сменить язык", "🌍 change language", "🌍 тілді өзгерту" -> sendLanguageSelection(chatId);
            case "🇷🇺 русский", "🇬🇧 english", "🇰🇿 қазақша"  -> {
                languageService.setUserLanguage(chatId, "ru");
                sendResponse(chatId, MessageService.getMessage(userLang, "language_saved"));
                sendCategorySelection(chatId);

            }
            case "задать еще вопрос", "ask another question", "тағы бір сұрақ қою" -> {
                sendResponse(chatId, MessageService.getMessage(userLang, "ask_again"));
                sendCategorySelection(chatId);
            }
            case "связаться", "contact", "байланысу" -> sendResponse(chatId, MessageService.getMessage(userLang, "contact"));
            case "достаточно", "enough", "жеткілікті" -> sendPreliminaryAnswer(chatId);
            case "я могу дополнить", "i can add more", "мен толықтыра аламын" ->
                    sendResponse(chatId, MessageService.getMessage(userLang, "detail_prompt"));

            default -> {
                if (!userCategories.containsKey(chatId)) {
                    sendCategorySelection(chatId);
                } else {
                    userQuestions.put(chatId, text);
                    sendResponseWithKeyboard(chatId, MessageService.getMessage(userLang, "clarify_question"),
                            MessageService.getMessage(userLang, "enough"),
                            MessageService.getMessage(userLang, "i_can_add_more"));
                }
            }
        }
    }

    private void sendLanguageSelection(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("🌍 Выберите язык | Choose language | Тілді таңдаңыз:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                List.of(createButton("🇷🇺 Русский", "set_lang_ru")),
                List.of(createButton("🇬🇧 English", "set_lang_en")),
                List.of(createButton("🇰🇿 Қазақша", "set_lang_kz"))
        );
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            Message sentMessage = execute(message);
            processingMessages.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
        KeyboardRow row = new KeyboardRow();
        for (String label : buttonLabels) {
            row.add(label);
        }
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String callbackData = callbackQuery.getData();
        String userLang = languageService.getUserLanguage(chatId);

        try {
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
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    private void sendFollowUpOptions(Long chatId) {
        String userLang = languageService.getUserLanguage(chatId); // Определяем язык пользователя

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(MessageService.getMessage(userLang, "follow_up")); // Локализованный текст

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                Arrays.asList(createButton(MessageService.getMessage(userLang, "contact_us"), "contact")),
                Arrays.asList(createButton(MessageService.getMessage(userLang, "ask_again"), "ask_again"))
        );
        markup.setKeyboard(keyboard);

        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendPreliminaryAnswer(Long chatId) {
        try {
            String userLang = languageService.getUserLanguage(chatId);

            Message analyzingMessage = sendResponse(chatId, MessageService.getMessage(userLang, "analyzing"));
            Message waitingMessage = sendResponse(chatId, MessageService.getMessage(userLang, "waiting"));

            if (waitingMessage == null) return;

            processingMessages.put(chatId, waitingMessage.getMessageId());

            String userQuery = userQuestions.getOrDefault(chatId, MessageService.getMessage(userLang, "unknown_question"));
            String category = userCategories.getOrDefault(chatId, MessageService.getMessage(userLang, "other"));
            String aiResponse = geminiService.getLegalAnswer(chatId, userQuery, true, true, category);

            deleteMessage(chatId, analyzingMessage.getMessageId());

            if (processingMessages.containsKey(chatId)) {
                int messageId = processingMessages.remove(chatId);
                editMessage(chatId, messageId, aiResponse + "\n\n⚖️ " + MessageService.getMessage(userLang, "legal_advice"));
            } else {
                sendResponse(chatId, aiResponse + "\n\n⚖️ " + MessageService.getMessage(userLang, "legal_advice"));
            }

            sendFollowUpOptions(chatId);
            userQuestions.remove(chatId);
            userCategories.remove(chatId);
        } catch (TelegramApiException e) {
            sendResponse(chatId, "❌ " + MessageService.getMessage(languageService.getUserLanguage(chatId), "error") + ": " + e.getMessage());
        }
    }




    private void sendCategorySelection(Long chatId) {
        String language = languageService.getUserLanguage(chatId);
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(MessageService.getMessage(language, "category_prompt"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = List.of(
                Arrays.asList(createButton(MessageService.getMessage(language, "auto"), "auto"),
                        createButton(MessageService.getMessage(language, "labor"), "labor")),
                Arrays.asList(createButton(MessageService.getMessage(language, "real_estate"), "real_estate"),
                        createButton(MessageService.getMessage(language, "family"), "family")),
                Collections.singletonList(createButton(MessageService.getMessage(language, "other"), "other"))
        );
        markup.setKeyboard(keyboard);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    private InlineKeyboardButton createButton(String category, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton(category);
        button.setCallbackData(callbackData);
        return button;
    }

    private Message sendResponse(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Можно заменить на логирование
            return null;
        }
    }


    private void editMessage(Long chatId, int messageId, String newText) throws TelegramApiException {
        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId.toString());
        editMessage.setMessageId(messageId);
        editMessage.setText(newText);
        execute(editMessage);
    }

    private void deleteMessage(Long chatId, int messageId) throws TelegramApiException {
        execute(new DeleteMessage(chatId.toString(), messageId));
    }
}
