import i18n from "i18next";
import { initReactI18next } from "react-i18next";
import translationCs from "./locales/cs/translation.json";
import translationEn from "./locales/en/translation.json";

i18n
    .use(initReactI18next)
    .init({
        resources: {
            cs: { translation: translationCs },
            en: { translation: translationEn }
        },
        lng: localStorage.getItem("lang") || "en",
        fallbackLng: "en",
        interpolation: { escapeValue: false }
    });

export default i18n;
