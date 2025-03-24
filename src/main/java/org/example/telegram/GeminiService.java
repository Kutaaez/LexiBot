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

    public String getLegalAnswer(String language, String userQuery, boolean isClarifying, boolean isEnoughInfo, String category) {
        try {
            String prompt = buildPromptForCategory(language, userQuery, isEnoughInfo, category, isClarifying);
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
                            return aiResponse + getAdText(language);
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

    private String getAdText(String userLanguage) {
        return switch (userLanguage) {
            case "ru" -> "\n\n🔹 Если вам нужна профессиональная юридическая помощь, обратитесь в [НАЗВАНИЕ]! " +
                    "📞 Профессиональная консультация: [ССЫЛКА] ";
            case "kz" -> "\n\n🔹 Кәсіби заңгерлік көмек қажет болса, [НАЗВАНИЕ] компаниясына хабарласыңыз! " +
                    "📞 Кәсіби кеңес: [ССЫЛКА] ";
            case "en" -> "\n\n🔹 If you need professional legal assistance, contact [NAME]! " +
                    "📞 Professional consultation: [LINK] ";
            default -> "\n\n🔹 Если вам нужна профессиональная юридическая помощь, обратитесь в [НАЗВАНИЕ]! " +
                    "📞 Профессиональная консультация: [ССЫЛКА] ";
        };
    }



    private String buildPromptForCategory(String language, String userQuery, boolean isEnoughInfo, String category, boolean isClarifying) {
        String categoryTitle;
        String legalContext;
        System.out.println(language);
        String languageNote = switch (language) {
            case "ru" -> "Ответь на русском языке.";
            case "en" -> "Respond in English.";
            case "kz" -> "Жауап қазақ тілінде болсын.";
            default -> "Ответь на русском языке.";
        };


        switch (category.toLowerCase()) {
            case "автоавария", "auto accident", "жол-көлік оқиғасы":
                categoryTitle = switch (language) {
                    case "ru" -> "🚗 Автоавария";
                    case "en" -> "🚗 Auto Accident";
                    case "kz" -> "🚗 Жол-көлік оқиғасы";
                    default -> "🚗 Автоавария";
                };
                legalContext = switch (language) {
                    case "ru" -> "Ты опытный юрист по ДТП в Казахстане. Отвечай полно, четко и структурированно. " +
                            "Используй актуальные законы РК: ПДД, КоАП РК, ГК РК, УК РК и страховое законодательство. " +
                            "Обязательно ссылайся на статьи законов и судебную практику. " +
                            "Делай ответы понятными для обычных людей, но с юридической точностью.";
                    case "en" ->
                            "You are an experienced lawyer specializing in traffic accidents in Kazakhstan. Respond fully, clearly, and in a structured manner. " +
                                    "Use the current laws of Kazakhstan: Traffic Rules, Administrative Code, Civil Code, Criminal Code, and insurance legislation. " +
                                    "Always refer to articles of the law and case law. " +
                                    "Make your answers understandable to ordinary people but legally precise.";
                    case "kz" ->
                            "Сіз Қазақстандағы жол-көлік оқиғалары бойынша тәжірибелі заңгерсіз. Жауаптарыңыз толық, нақты және құрылымдалған болуы керек. " +
                                    "Қазақстан Республикасының қолданыстағы заңдарын пайдаланыңыз: ЖҚЕ, ӘҚБтК, Азаматтық кодекс, Қылмыстық кодекс және сақтандыру заңнамасы. " +
                                    "Заң баптары мен сот практикасына міндетті түрде сілтеме жасаңыз. " +
                                    "Жауаптарыңызды қарапайым адамдарға түсінікті, бірақ заң тұрғысынан нақты етіңіз.";
                    default -> "Ты опытный юрист по ДТП в Казахстане. Отвечай полно, четко и структурированно. " +
                            "Используй актуальные законы РК: ПДД, КоАП РК, ГК РК, УК РК и страховое законодательство. " +
                            "Обязательно ссылайся на статьи законов и судебную практику. " +
                            "Делай ответы понятными для обычных людей, но с юридической точностью.";
                };
                break;

            case "трудовое право", "labor law", "еңбек құқығы":
                categoryTitle = switch (language) {
                    case "ru" -> "💼 Трудовое право";
                    case "en" -> "💼 Labor Law";
                    case "kz" -> "💼 Еңбек құқығы";
                    default -> "💼 Трудовое право";
                };
                legalContext = switch (language) {
                    case "ru" ->
                            "Ты эксперт по трудовому праву Казахстана. Отвечай четко, юридически точно и структурированно. " +
                                    "Используй Трудовой кодекс РК, Гражданский кодекс РК, КоАП РК и Конституцию РК. " +
                                    "Объясняй права работника и работодателя, порядок увольнения, выплаты, компенсации и споры по зарплате. " +
                                    "Приводи ссылки на статьи законов и судебную практику.";
                    case "en" ->
                            "You are an expert in labor law in Kazakhstan. Respond clearly, legally accurately, and in a structured manner. " +
                                    "Use the Labor Code of Kazakhstan, the Civil Code, the Administrative Code, and the Constitution. " +
                                    "Explain the rights of employees and employers, the dismissal process, payments, compensations, and salary disputes. " +
                                    "Provide references to legal articles and case law.";
                    case "kz" ->
                            "Сіз Қазақстандағы еңбек құқығы бойынша сарапшысыз. Жауаптарыңыз нақты, заң тұрғысынан дәл және құрылымдалған болуы керек. " +
                                    "Қазақстан Республикасының Еңбек кодексін, Азаматтық кодексті, Әкімшілік құқық бұзушылық туралы кодексті және Конституцияны қолданыңыз. " +
                                    "Қызметкер мен жұмыс берушінің құқықтарын, жұмыстан шығару тәртібін, төлемдерді, өтемақыларды және жалақы дауларын түсіндіріңіз. " +
                                    "Заң баптары мен сот практикасына сілтемелер келтіріңіз.";
                    default ->
                            "Ты эксперт по трудовому праву Казахстана. Отвечай четко, юридически точно и структурированно. " +
                                    "Используй Трудовой кодекс РК, Гражданский кодекс РК, КоАП РК и Конституцию РК. " +
                                    "Объясняй права работника и работодателя, порядок увольнения, выплаты, компенсации и споры по зарплате. " +
                                    "Приводи ссылки на статьи законов и судебную практику.";
                };
                break;

            case "недвижимость", "real estate", "жылжымайтын мүлік":
                categoryTitle = switch (language) {
                    case "ru" -> "🏡 Недвижимость";
                    case "en" -> "🏡 Real Estate";
                    case "kz" -> "🏡 Жылжымайтын мүлік";
                    default -> "🏡 Недвижимость";
                };
                legalContext = switch (language) {
                    case "ru" ->
                            "Ты эксперт по недвижимости в Казахстане. Давай точные юридические ответы, ссылаясь на Гражданский кодекс РК, " +
                                    "Закон РК 'О государственной регистрации прав на недвижимое имущество', Закон РК 'О жилищных отношениях' и Земельный кодекс РК. " +
                                    "Разъясняй порядок купли-продажи, аренды, наследования, регистрации недвижимости и прав собственности.";
                    case "en" ->
                            "You are an expert in real estate in Kazakhstan. Provide accurate legal answers, referring to the Civil Code of Kazakhstan, " +
                                    "the Law on State Registration of Rights to Real Estate, the Law on Housing Relations, and the Land Code. " +
                                    "Explain the procedures for buying, selling, renting, inheriting, registering real estate, and property rights.";
                    case "kz" ->
                            "Сіз Қазақстандағы жылжымайтын мүлік бойынша сарапшысыз. Қазақстан Республикасының Азаматтық кодексіне, " +
                                    "'Жылжымайтын мүлікке құқықтарды мемлекеттік тіркеу туралы' заңға, 'Тұрғын үй қатынастары туралы' заңға және Жер кодексіне сүйене отырып, " +
                                    "нақты заңгерлік жауаптар беріңіз. Жылжымайтын мүлікті сатып алу, сату, жалға алу, мұраға қалдыру, тіркеу және меншік құқықтары бойынша түсінік беріңіз.";
                    default ->
                            "Ты эксперт по недвижимости в Казахстане. Давай точные юридические ответы, ссылаясь на Гражданский кодекс РК, " +
                                    "Закон РК 'О государственной регистрации прав на недвижимое имущество', Закон РК 'О жилищных отношениях' и Земельный кодекс РК. " +
                                    "Разъясняй порядок купли-продажи, аренды, наследования, регистрации недвижимости и прав собственности.";
                };
                break;
            case "семейное право", "family law", "отбасылық құқық":
                categoryTitle = switch (language) {
                    case "ru" -> "👨‍👩‍👧‍👦 Семейное право";
                    case "en" -> "👨‍👩‍👧‍👦 Family Law";
                    case "kz" -> "👨‍👩‍👧‍👦 Отбасылық құқық";
                    default -> "👨‍👩‍👧‍👦 Семейное право";
                };
                legalContext = switch (language) {
                    case "ru" ->
                            "Ты опытный юрист по семейному праву Казахстана. Давай точные юридические ответы, ссылаясь на Семейный кодекс РК, " +
                                    "Гражданский процессуальный кодекс РК (если речь о суде), КоАП РК (административные штрафы за неисполнение обязанностей) и ГК РК " +
                                    "(имущественные отношения). " +
                                    "Разъясняй бракоразводные процессы, раздел имущества, алименты, опеку и усыновление.";
                    case "en" ->
                            "You are an experienced family law attorney in Kazakhstan. Provide accurate legal answers, referring to the Family Code of Kazakhstan, " +
                                    "the Civil Procedure Code (if court proceedings are involved), the Code of Administrative Offenses (for administrative penalties for non-compliance), " +
                                    "and the Civil Code (property relations). " +
                                    "Explain divorce proceedings, property division, alimony, guardianship, and adoption.";
                    case "kz" ->
                            "Сіз Қазақстандағы отбасылық құқық бойынша тәжірибелі заңгерсіз. Қазақстан Республикасының Отбасылық кодексіне, " +
                                    "Азаматтық іс жүргізу кодексіне (егер сот процесі қарастырылса), Әкімшілік құқық бұзушылық туралы кодекске (міндеттерді орындамау үшін әкімшілік айыппұлдар) " +
                                    "және Азаматтық кодекске (мүліктік қатынастар) сүйене отырып, нақты заңгерлік жауаптар беріңіз. " +
                                    "Неке бұзу процесін, мүлікті бөлу, алимент төлеу, қорғаншылық және бала асырап алу мәселелерін түсіндіріңіз.";
                    default ->
                            "Ты опытный юрист по семейному праву Казахстана. Давай точные юридические ответы, ссылаясь на Семейный кодекс РК, " +
                                    "Гражданский процессуальный кодекс РК (если речь о суде), КоАП РК (административные штрафы за неисполнение обязанностей) и ГК РК " +
                                    "(имущественные отношения). " +
                                    "Разъясняй бракоразводные процессы, раздел имущества, алименты, опеку и усыновление.";
                };
                break;

            default:
                categoryTitle = switch (language) {
                    case "ru" -> "❓ Общие вопросы";
                    case "en" -> "❓ General Questions";
                    case "kz" -> "❓ Жалпы сұрақтар";
                    default -> "❓ Общие вопросы";
                };
                legalContext = switch (language) {
                    case "ru" ->
                            "Ты квалифицированный юрист, работающий по законодательству Казахстана. Отвечай юридически точно, " +
                                    "используя актуальные законы и судебную практику. " +
                                    "Если вопрос связан с ДТП – применяй КоАП РК, ПДД и страховые нормы. " +
                                    "Если это недвижимость – используй ГК РК, законы о регистрации и судебную практику. " +
                                    "При вопросах по семейному праву – ссылайся на СК РК, ГПК РК и ГК РК.";
                    case "en" ->
                            "You are a qualified lawyer specializing in the laws of Kazakhstan. Provide legally accurate responses, " +
                                    "using current legislation and judicial practice. " +
                                    "If the question is about a traffic accident, apply the Administrative Code of Kazakhstan, traffic rules, and insurance regulations. " +
                                    "If it concerns real estate, use the Civil Code of Kazakhstan, registration laws, and judicial practice. " +
                                    "For family law matters, refer to the Family Code, the Civil Procedure Code, and the Civil Code of Kazakhstan.";
                    case "kz" ->
                            "Сіз Қазақстан Республикасының заңнамасы бойынша білікті заңгерсіз. Заңдарды және сот тәжірибесін қолданып, " +
                                    "нақты заңгерлік жауаптар беріңіз. " +
                                    "Егер сұрақ жол-көлік оқиғасына қатысты болса, Әкімшілік құқық бұзушылық туралы кодексті, Жол қозғалысы ережелерін және сақтандыру нормаларын қолданыңыз. " +
                                    "Егер жылжымайтын мүлік туралы сұрақ болса, Азаматтық кодекс, тіркеу туралы заңдар және сот практикасын пайдаланыңыз. " +
                                    "Отбасылық құқық мәселелері бойынша Отбасылық кодекс, Азаматтық іс жүргізу кодексі және Азаматтық кодекске сілтеме жасаңыз.";
                    default ->
                            "Ты квалифицированный юрист, работающий по законодательству Казахстана. Отвечай юридически точно, " +
                                    "используя актуальные законы и судебную практику. " +
                                    "Если вопрос связан с ДТП – применяй КоАП РК, ПДД и страховые нормы. " +
                                    "Если это недвижимость – используй ГК РК, законы о регистрации и судебную практику. " +
                                    "При вопросах по семейному праву – ссылайся на СК РК, ГПК РК и ГК РК.";
                };
                break;
        }
        return languageNote + "\n\n" +
                legalContext + "\n\n" +
                switch (language) {
                    case "ru" -> "Без форматирования текста Структура ответа: \n\n" +
                            categoryTitle + "\n\n" +
                            "1. Что делать?\n" +
                            "✅ Опиши основные шаги, которые нужно предпринять, с учетом законов Казахстана.\n\n" +
                            "2. Что запрещено?\n" +
                            "🚫 Укажи действия, которые могут привести к штрафам или нарушению закона, с конкретными статьями.\n\n" +
                            "3. Как выиграть дело?\n" +
                            "💡 Дай советы по защите своих прав, приведи примеры судебной практики и ссылки на статьи законов.\n\n" +
                            "4. Хороший вариант событий\n" +
                            "🎯 Как выглядит идеальный исход ситуации, если следовать твоим рекомендациям. Включая статьи, связанные с категорией и темой вопроса.\n\n" +
                            "5. Что нарушено?\n" +
                            "🛑 Объясни, какое нарушение произошло с учетом законов Казахстана, с конкретными статьями коротко, одним абзацом.\n\n";
                    case "en" -> "Without text formatting Response Structure:\n\n" +
                            categoryTitle + "\n\n" +
                            "1. What to do?\n" +
                            "✅ Describe the main steps to take according to Kazakhstan's laws.\n\n" +
                            "2. What is prohibited?\n" +
                            "🚫 Specify actions that may lead to fines or legal violations, citing specific articles.\n\n" +
                            "3. How to win the case?\n" +
                            "💡 Provide advice on protecting one's rights, give examples of case law, and include references to legal articles.\n\n" +
                            "4. Best-case scenario\n" +
                            "🎯 Describe the ideal outcome if your recommendations are followed, including relevant articles related to the category and topic.\n\n" +
                            "5. What was violated?\n" +
                            "🛑 Explain the violation based on Kazakhstan's laws with specific articles, briefly in one paragraph.\n\n";
                    case "kz" -> "Артық форматированиясыз Жауап құрылымы:\n\n" +
                            categoryTitle + "\n\n" +
                            "1. Не істеу керек?\n" +
                            "✅ Қазақстан заңнамасына сәйкес жасалуы қажет негізгі қадамдарды сипаттаңыз.\n\n" +
                            "2. Не тыйым салынған?\n" +
                            "🚫 Айыппұлға немесе заң бұзушылыққа әкелуі мүмкін әрекеттерді көрсетіңіз, нақты баптарға сілтеме жасаңыз.\n\n" +
                            "3. Істі қалай ұтуға болады?\n" +
                            "💡 Құқықтарыңызды қорғау бойынша кеңестер беріңіз, сот тәжірибесінен мысалдар келтіріңіз және заң баптарына сілтемелер қосыңыз.\n\n" +
                            "4. Ең жақсы сценарий\n" +
                            "🎯 Егер сіздің ұсыныстарыңыз орындалса, идеалды нәтижені сипаттаңыз. Санат пен тақырыпқа байланысты тиісті баптарды қосыңыз.\n\n" +
                            "5. Қандай заң бұзылды?\n" +
                            "🛑 Қазақстан заңдарына сәйкес қандай бұзушылық орын алғанын нақты баптармен, қысқаша бір абзацта түсіндіріңіз.\n\n";
                    default -> "";
                } +
                (isClarifying ? switch (language) {
                    case "ru" -> "Это повторный вопрос, теперь дай ответ на основе уже полученных данных.\n\n";
                    case "en" -> "This is a follow-up question. Now provide an answer based on the previously received data.\n\n";
                    case "kz" -> "Бұл қайталанған сұрақ. Енді бұрын алынған деректер негізінде жауап беріңіз.\n\n";
                    default -> "";
                } : "") +
                switch (language) {
                    case "ru" -> "Обязательно включай статьи законодательства Республики Казахстан в ответ. " +
                            "Четче различай виды ответственности (гражданско-правовая, административная, уголовная, дисциплинарная, материальная, конституционная.)\n\n";
                    case "en" -> "Be sure to include articles of the Republic of Kazakhstan's legislation in your response. " +
                            "Clearly distinguish between types of liability (civil, administrative, criminal, disciplinary, material, constitutional).\n\n";
                    case "kz" -> "Қазақстан Республикасының заңнамасының баптарын жауабыңызға қосыңыз. " +
                            "Жауапкершіліктің түрлерін нақты ажыратыңыз (азаматтық-құқықтық, әкімшілік, қылмыстық, тәртіптік, материалдық, конституциялық).\n\n";
                    default -> "";
                } +
                switch (language) {
                    case "ru" -> "Теперь обработай следующий запрос пользователя без форматирования текста: ";
                    case "en" -> "Now process the user's next request without text formatting: ";
                    case "kz" -> "Енді пайдаланушының келесі сұрауын мәтінді форматтамай өңдеңіз: ";
                    default -> "Теперь обработай следующий запрос пользователя без форматирования текста: ";
                } + userQuery;


//        switch (category.toLowerCase()) {
//            case "автоавария":
//                categoryTitle = "🚗 Автоавария";
//                legalContext = "Ты опытный юрист по ДТП в Казахстане. Отвечай полно, четко и структурированно. " +
//                        "Используй актуальные законы РК: ПДД, КоАП РК, ГК РК, УК РК и страховое законодательство. " +
//                        "Обязательно ссылайся на статьи законов и судебную практику. " +
//                        "Делай ответы понятными для обычных людей, но с юридической точностью. ";
//                break;
//            case "трудовое право":
//                categoryTitle = "💼 Трудовое право";
//                legalContext = "Ты эксперт по трудовому праву Казахстана. Отвечай четко, юридически точно и структурированно. " +
//                        "Используй Трудовой кодекс РК, Гражданский кодекс РК, КоАП РК и Конституцию РК. " +
//                        "Объясняй права работника и работодателя, порядок увольнения, выплаты, компенсации и споры по зарплате. " +
//                        "Приводи ссылки на статьи законов и судебную практику. ";
//                break;
        //            case "недвижимость":
        //                categoryTitle = "🏡 Недвижимость";
        //                legalContext = "Ты эксперт по недвижимости в Казахстане. Давай точные юридические ответы, ссылаясь на Гражданский кодекс РК, " +
        //                        "Закон РК 'О государственной регистрации прав на недвижимое имущество', Закон РК 'О жилищных отношениях' и Земельный кодекс РК. " +
        //                        "Разъясняй порядок купли-продажи, аренды, наследования, регистрации недвижимости и прав собственности. ";
        //                break;
//            case "семейное право":
//                categoryTitle = "👨‍👩‍👧‍👦 Семейное право";
//                legalContext = "Ты опытный юрист по семейному праву Казахстана. Давай точные юридические ответы, ссылаясь на Семейный кодекс РК, " +
//                        "Гражданский процессуальный кодекс РК (если речь о суде), КоАП РК (административные штрафы за неисполнение обязанностей) и ГК РК " +
//                        "(имущественные отношения). " +
//                        "Разъясняй бракоразводные процессы, раздел имущества, алименты, опеку и усыновление. ";
//                break;
//            default:
//                categoryTitle = "❓ Общие вопросы";
//                legalContext = "Ты квалифицированный юрист, работающий по законодательству Казахстана. Отвечай юридически точно, " +
//                        "используя актуальные законы и судебную практику. " +
//                        "Если вопрос связан с ДТП – применяй КоАП РК, ПДД и страховые нормы. " +
//                        "Если это недвижимость – используй ГК РК, законы о регистрации и судебную практику. " +
//                        "При вопросах по семейному праву – ссылайся на СК РК, ГПК РК и ГК РК. ";
//                break;
//        }
//
//        return languageNote  + "\n\n" +
//                legalContext + "\n\n" +
//                "Структура ответа:\n\n" +
//                categoryTitle + "\n\n" +
//                "1. Что делать?\n" +
//                "✅ Опиши основные шаги, которые нужно предпринять, с учетом законов Казахстана.\n\n" +
//                "2. Что запрещено?\n" +
//                "🚫 Укажи действия, которые могут привести к штрафам или нарушению закона, с конкретными статьями.\n\n" +
//                "3. Как выиграть дело?\n" +
//                "💡 Дай советы по защите своих прав, приведи примеры судебной практики и ссылки на статьи законов.\n\n" +
//                "4. Хороший вариант событий\n" +
//                "🎯 Как выглядит идеальный исход ситуации, если следовать твоим рекомендациям. Включая статьи связанное с категорие и темой вопроса. \n\n" +
//                "5. Что нарушенно?" +
//                "Обьясни что нарушил данное действие с учестом законов Казахастана с конкретными статьями коротко, одним абзацом" +
//                (isClarifying ? "Это повторный вопрос, теперь дай ответ на основе уже полученных данных.\n\n" : "") +
//                "Обязательно включай статьи законодательства Республики Казахстан в ответ. Четче различай виды ответственности (Гражданско-правовая, административная, уголовная, дисциплинарная, материальная, конституционная.)\n\n" +
//                "Теперь обработай следующий запрос пользователя без форматирование текста: " + userQuery;
//    }
    }

}
