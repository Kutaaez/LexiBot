package org.example.telegram;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GeminiService {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro:generateContent";
    private final String apiKey;
    private final OkHttpClient client;
    private final LanguageService languageService = new LanguageService();


    public GeminiService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String handleApiError(Exception e) {
        System.err.println("Ошибка в API-запросе: " + e.getMessage());
        return "Произошла ошибка при запросе к ИИ. Попробуйте позже.";
    }

    public String getLegalAnswer(long chatid,String userQuery, boolean isClarifying, boolean isEnoughInfo, String category) {
        try {
            String prompt = buildPromptForCategory(chatid, userQuery, isEnoughInfo, category, isClarifying);
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
                if (!response.isSuccessful() || response.body() == null) {
                    return "Ошибка запроса к ИИ: " + (response.body() != null ? response.body().string() : "Нет данных");
                }

                JSONObject responseJson = new JSONObject(response.body().string());
                JSONArray candidates = responseJson.optJSONArray("candidates");

                if (candidates != null && candidates.length() > 0) {
                    JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
                    if (content != null) {
                        JSONArray parts = content.optJSONArray("parts");
                        if (parts != null && parts.length() > 0) {
                            String aiResponse = parts.getJSONObject(0).optString("text", "Нет ответа");
                            return aiResponse + getAdText();
                        }
                    }
                }
            } catch (IOException | JSONException e) {
                return handleApiError(e);
            }
        } catch (Exception e) {
            return handleApiError(e);
        }

        return "Извините, не удалось получить ответ.";
    }

    private String getAdText() {
        return "\n\n🔹 Если вам нужна профессиональная юридическая помощь, обратитесь в LexTrust! " +
                "📞 Бесплатная консультация 👉 [Записаться](https://t.me/your_lawyer_bot)";
    }

    private String buildPromptForCategory(Long chatId, String userQuery, boolean isEnoughInfo, String category, boolean isClarifying) {
        String categoryTitle;
        String legalContext;
        String userLanguage = languageService.getUserLanguage(chatId);
        String languageNote = switch (userLanguage) {
            case "ru" -> "Ответь на русском языке.";
            case "en" -> "Respond in English.";
            case "kz" -> "Жауап қазақ тілінде болсын.";
            default -> "Ответь на русском языке.";
        };

        switch (category.toLowerCase()) {
            case "автоавария":
                categoryTitle = "🚗 Автоавария";
                legalContext = "Ты опытный юрист по ДТП в Казахстане. Отвечай полно, четко и структурированно. " +
                        "Используй актуальные законы РК: ПДД, КоАП РК, ГК РК, УК РК и страховое законодательство. " +
                        "Обязательно ссылайся на статьи законов и судебную практику. " +
                        "Делай ответы понятными для обычных людей, но с юридической точностью. ";
                break;
            case "трудовое право":
                categoryTitle = "💼 Трудовое право";
                legalContext = "Ты эксперт по трудовому праву Казахстана. Отвечай четко, юридически точно и структурированно. " +
                        "Используй Трудовой кодекс РК, Гражданский кодекс РК, КоАП РК и Конституцию РК. " +
                        "Объясняй права работника и работодателя, порядок увольнения, выплаты, компенсации и споры по зарплате. " +
                        "Приводи ссылки на статьи законов и судебную практику. ";
                break;
            case "недвижимость":
                categoryTitle = "🏡 Недвижимость";
                legalContext = "Ты эксперт по недвижимости в Казахстане. Давай точные юридические ответы, ссылаясь на Гражданский кодекс РК, " +
                        "Закон РК 'О государственной регистрации прав на недвижимое имущество', Закон РК 'О жилищных отношениях' и Земельный кодекс РК. " +
                        "Разъясняй порядок купли-продажи, аренды, наследования, регистрации недвижимости и прав собственности. ";
                break;
            case "семейное право":
                categoryTitle = "👨‍👩‍👧‍👦 Семейное право";
                legalContext = "Ты опытный юрист по семейному праву Казахстана. Давай точные юридические ответы, ссылаясь на Семейный кодекс РК, " +
                        "Гражданский процессуальный кодекс РК (если речь о суде), КоАП РК (административные штрафы за неисполнение обязанностей) и ГК РК " +
                        "(имущественные отношения). " +
                        "Разъясняй бракоразводные процессы, раздел имущества, алименты, опеку и усыновление. ";
                break;
            default:
                categoryTitle = "❓ Общие вопросы";
                legalContext = "Ты квалифицированный юрист, работающий по законодательству Казахстана. Отвечай юридически точно, " +
                        "используя актуальные законы и судебную практику. " +
                        "Если вопрос связан с ДТП – применяй КоАП РК, ПДД и страховые нормы. " +
                        "Если это недвижимость – используй ГК РК, законы о регистрации и судебную практику. " +
                        "При вопросах по семейному праву – ссылайся на СК РК, ГПК РК и ГК РК. ";
                break;
        }

        return languageNote  + "\n\n" +
                legalContext + "\n\n" +
                "Структура ответа:\n\n" +
                categoryTitle + "\n\n" +
                "1. Что делать?\n" +
                "✅ Опиши основные шаги, которые нужно предпринять, с учетом законов Казахстана.\n\n" +
                "2. Что запрещено?\n" +
                "🚫 Укажи действия, которые могут привести к штрафам или нарушению закона, с конкретными статьями.\n\n" +
                "3. Как выиграть дело?\n" +
                "💡 Дай советы по защите своих прав, приведи примеры судебной практики и ссылки на статьи законов.\n\n" +
                "4. Хороший вариант событий\n" +
                "🎯 Как выглядит идеальный исход ситуации, если следовать твоим рекомендациям. Включая статьи связанное с категорие и темой вопроса. \n\n" +
                "5. Что нарушенно?" +
                "Обьясни что нарушил данное действие с учестом законов Казахастана с конкретными статьями коротко, одним абзацом" +
                (isClarifying ? "Это повторный вопрос, теперь дай ответ на основе уже полученных данных.\n\n" : "") +
                "Обязательно включай статьи законодательства Республики Казахстан в ответ. Четче различай виды ответственности (Гражданско-правовая, административная, уголовная, дисциплинарная, материальная, конституционная.)\n\n" +
                "Теперь обработай следующий запрос пользователя без форматирование текста: " + userQuery;
    }
}
