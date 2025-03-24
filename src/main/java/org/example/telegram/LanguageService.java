package org.example.telegram;

import java.util.HashMap;
import java.util.Map;

public class LanguageService {
    private final Map<Long, String> userLanguages = new HashMap<>();
    private static final String DEFAULT_LANGUAGE = "ru";

    public String getUserLanguage(Long chatId) {
        return userLanguages.getOrDefault(chatId, DEFAULT_LANGUAGE);
    }

    public void setUserLanguage(Long chatId, String language) {
        userLanguages.put(chatId, language);
    }
}
