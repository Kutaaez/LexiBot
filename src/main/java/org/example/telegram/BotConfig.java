package org.example.telegram;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BotConfig {
    private final Properties properties = new Properties();

    public BotConfig() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("file application.properties it's not found!");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("we got problem when loading file", e);
        }
    }

    public String getTelegramBotToken() {
        return properties.getProperty("telegram.bot.token");
    }

    public String getTelegramBotUsername() {
        return properties.getProperty("telegram.bot.username");
    }

    public String getGeminiApiKey() {
        return properties.getProperty("gemini.api.key");
    }
}
