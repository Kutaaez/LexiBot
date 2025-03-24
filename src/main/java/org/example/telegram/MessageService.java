package org.example.telegram;

import java.util.HashMap;
import java.util.Map;

public class MessageService {
    private static final Map<String, Map<String, String>> messages = new HashMap<>();

    static {
        Map<String, String> ru = new HashMap<>();
        ru.put("start", "👋 Привет! Я ваш юридический помощник. Выберите категорию вопроса!");
        ru.put("choose_category", "Пожалуйста, выберите категорию вашего вопроса:");
        ru.put("contact", "Наши контакты: тг, сайт...");
        ru.put("ask_again", "Я ваш юридический помощник. Выберите категорию вопроса!");
        ru.put("category_prompt", "Пожалуйста, выберите категорию вашего вопроса:");
        ru.put("auto", "🚗 Автоавария");
        ru.put("labor", "💼 Трудовое право");
        ru.put("real_estate", "🏡 Недвижимость");
        ru.put("family", "👨‍👩‍👧‍👦 Семейное право");
        ru.put("other", "❓ Другое");
        ru.put("selected_category","Вы выбрали категорию");
        ru.put("ask_question","Теперь напишите ваш вопрос.");
        ru.put("analyzing", "🔎 Анализирую ваш вопрос... Ожидайте");
        ru.put("waiting", "⏳");
        ru.put("unknown_question", "Неизвестный вопрос");
        ru.put("legal_advice", "В сложных случаях рекомендуем обратиться к профессиональным юристам.");
        ru.put("error", "Ошибка при обработке запроса");
        ru.put("follow_up", "Что вы хотите сделать дальше?");
        ru.put("contact_us", "Связаться с нами");
        ru.put("ask_more", "Задать ещё вопрос");
        ru.put("language_saved", "✅ Ваш язык сохранен.");
        ru.put("clarify_question", "Пожалуйста, уточните ваш вопрос.");
        ru.put("enough", "Достаточно");
        ru.put("i_can_add_more", "Я могу дополнить");
        ru.put("detail_prompt","Пожалуйста, уточните ваш вопрос.");
        ru.put("choose", "Дополните ваш вопрос. Если информации достаточно, нажмите кнопку 'Достаточно'.");

        //----
        Map<String, String> en = new HashMap<>();
        en.put("start", "👋 Hello! I'm your legal assistant. Choose your question category!");
        en.put("choose_category", "Please select your question category:");
        en.put("contact", "Our contacts: tg,site...");
        en.put("ask_again", "I'm your legal assistant. Choose your question category!");
        en.put("category_prompt", "Please select the category of your question:");
        en.put("auto", "🚗 Car accident");
        en.put("labor", "💼 Labor law");
        en.put("real_estate", "🏡 Real estate");
        en.put("family", "👨‍👩‍👧‍👦 Family law");
        en.put("other", "❓ Other");
        en.put("selected_category","You have chosen the category");
        en.put("ask_question","Now write your question.");
        en.put("analyzing", "🔎 Analyzing your question... Please wait");
        en.put("waiting", "⏳");
        en.put("unknown_question", "Unknown question");
        en.put("legal_advice", "In complex cases, we recommend consulting professional lawyers.");
        en.put("error", "Error processing request");
        en.put("follow_up", "What would you like to do next?");
        en.put("contact_us", "Contact us");
        en.put("ask_more", "Ask another question");
        en.put("language_saved", "✅ Your language has been saved.");
        en.put("clarify_question", "Please clarify your question.");
        en.put("enough", "Enough");
        en.put("i_can_add_more", "I can add more");
        en.put("detail_prompt","Please provide more details.");
        en.put("choose", "Clarify your question. If the information is sufficient, press the 'Enough' button.");







        //----
        Map<String, String> kz = new HashMap<>();
        kz.put("start", "👋 Сәлем! Мен сіздің заңгерлік көмекшіңізмін. Сұрақ санатын таңдаңыз!");
        kz.put("choose_category", "Өтінеміз, сұрақ санатын таңдаңыз:");
        kz.put("contact", "Біздің байланыстар:");
        kz.put("ask_again", "Мен сіздің заңгерлік көмекшіңізмін. Сұрақ санатын таңдаңыз!");
        kz.put("category_prompt", "Сұрағыңыздың санатын таңдаңыз:");
        kz.put("auto", "🚗 Жол апаты");
        kz.put("labor", "💼 Еңбек құқығы");
        kz.put("real_estate", "🏡 Жылжымайтын мүлік");
        kz.put("family", "👨‍👩‍👧‍👦 Отбасы құқығы");
        kz.put("other", "❓ Басқа");
        kz.put("selected_category","Сіз санатты таңдадыңыз");
        kz.put("ask_question","Енді өз сұрағыңызды жазыңыз.");
        kz.put("analyzing", "🔎 Сұрағыңызды талдап жатырмын... Күтіңіз");
        kz.put("waiting", "⏳");
        kz.put("unknown_question", "Белгісіз сұрақ");
        kz.put("legal_advice", "Күрделі жағдайларда кәсіби заңгерлерге жүгінуге кеңес береміз.");
        kz.put("error", "Сұранысты өңдеу кезінде қате орын алды");
        kz.put("follow_up", "Келесі не істегіңіз келеді?");
        kz.put("contact_us", "Бізбен байланысу");
        kz.put("ask_more", "Тағы сұрақ қою");
        kz.put("language_saved", "✅ Тіліңіз сақталды.");
        kz.put("clarify_question", "Сұрағыңызды нақтылаңыз.");
        kz.put("enough", "Жеткілікті");
        kz.put("i_can_add_more", "Мен толықтыра аламын");
        kz.put("detail_prompt", "Сұрағыңызды нақтылаңыз.");
        kz.put("choose","Сұрағыңызды нақтылаңыз. Әлде жеткілікті ақпарат берсеңіз жеткілікті батырмасын басыңыз№ " );
        //----
        messages.put("ru", ru);
        messages.put("en", en);
        messages.put("kz", kz);
    }

    public static String getMessage(String lang, String key) {
        return messages.getOrDefault(lang, messages.get("ru")).getOrDefault(key, key);
    }

}
