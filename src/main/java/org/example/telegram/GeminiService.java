package org.example.telegram;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    private final String apiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public GeminiService() {
        BotConfig config = new BotConfig();
        this.apiKey = config.getGeminiApiKey();
    }

    public String getLegalAnswer(String userQuery, boolean isClarifying, boolean isEnoughInfo) throws IOException {
        String prompt;

        if (isEnoughInfo) {
            prompt = "Ты — юридический помощник, специализирующийся на законодательстве Казахстана. " +
                    "Отвечай кратко, четко и доступно, чтобы даже неподготовленный человек понял. " +
                    "Используй только актуальные статьи Кодексов РК. Проверяй, что статья действительно относится к теме. Если точной статьи нет, не придумывай." +
                    "Используй Telegram Markdown v2 для форматирования. " +
                    "Структура ответа:\n\n" +
                    "🚗 **[Краткое название ситуации]**\n\n" +
                    "**1️⃣ Что делать?**\n" +
                    "✅ Опиши кратко основные действия с номерами шагов и ссылками на законы.\n\n" +
                    "**2️⃣ Что запрещено?**\n" +
                    "🚫 Укажи главные ошибки, которые нельзя допускать, и их последствия.\n\n" +
                    "**3️⃣ Как выиграть спор?**\n" +
                    "💡 Объясни, в каких случаях можно доказать свою правоту (свидетели, камеры, документы и т. д.).\n\n" +
                    "**4️⃣ Вам нужна помощь?**\n" +
                    "⚖️ **Юристы LexTrust** помогут защитить права и подготовить документы.\n" +
                    "📞 **Бесплатная консультация** 👉 [Записаться](https://t.me/your_lawyer_bot)\n\n" +
                    "**5️⃣ Хороший вариант событий**\n" +
                    "🎯 Опиши идеальный исход (например, если есть запись с камеры или признание вины), укажи, какая статья закона поможет в этой ситуации.\n\n" +
                    "Теперь обработай следующий запрос пользователя: " + userQuery;
        } else {
            prompt = "Ты — юридический помощник, специализирующийся на законодательстве Казахстана. " +
                    "Отвечай только на юридические вопросы, используя актуальные законы. " +
                    "Если информации недостаточно, сначала задай уточняющие вопросы. " +
                    "Формулируй их кратко и понятно. Примеры:\n\n" +
                    "✅ **Когда и где произошло событие?**\n" +
                    "✅ **Есть ли свидетели или видеозапись?**\n" +
                    "✅ **Вызывали ли полицию?**\n" +
                    "✅ **Какие разногласия между сторонами?**\n\n" +
                    "Если случай сложный, предложи консультацию юриста. " +
                    "Теперь обработай следующий запрос пользователя: " + userQuery;
}

            if (isClarifying) {
            prompt += " Это повторный вопрос, теперь дай ответ на основе уже полученных данных.";
        }

        JSONObject requestJson = new JSONObject()
                .put("contents", new JSONArray()
                        .put(new JSONObject()
                                .put("parts", new JSONArray()
                                        .put(new JSONObject().put("text", prompt)))));

        RequestBody body = RequestBody.create(requestJson.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(API_URL + "?key=" + apiKey)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Ошибка запроса к ИИ: " + response.message();
            }

            JSONObject responseJson = new JSONObject(response.body().string());
            JSONArray candidates = responseJson.optJSONArray("candidates");
            if (candidates != null && candidates.length() > 0) {
                String aiResponse = candidates.getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");

                // Добавляем рекламу юр. компании в конец ответа
                String adText = "\n\n🔹 Если вам нужна профессиональная юридическая помощь, обратитесь в нашу компанию **LexTrust**! " +
                        "Мы предоставляем консультации по всем правовым вопросам. " +
                        "📞 Свяжитесь с нами прямо сейчас!";

                return aiResponse + adText;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Произошла ошибка при обработке ответа.";
        }

        return "Извините, не удалось получить ответ.";
    }
}
