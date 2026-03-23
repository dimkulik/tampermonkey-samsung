# Tampermonkey для Samsung Internet

Аналог Tampermonkey — менеджер пользовательских скриптов для браузера Samsung Internet на Android.

## Возможности

- 📦 Установка `.user.js` скриптов по прямой ссылке
- ✏️ Встроенный редактор кода
- 🔄 Автообновление скриптов
- ✅ Включение/выключение скриптов
- 🌐 Внедрение скриптов в страницы Samsung Internet
- 🔧 Поддержка GM API: `GM_setValue`, `GM_getValue`, `GM_addStyle`, `GM_xmlhttpRequest`, `GM_notification`, `GM_openInTab`

## Сборка APK через GitHub Actions

1. Форкни этот репозиторий
2. Перейди во вкладку **Actions**
3. Выбери **Build APK** → **Run workflow**
4. Подожди ~3 минуты
5. Скачай APK из раздела **Artifacts** или **Releases**

## Установка

1. Скачай `app-debug.apk`
2. На телефоне: **Настройки → Приложения → Особые права → Установка неизвестных приложений** → разрешить для браузера/файлового менеджера
3. Установи APK
4. Открой **Samsung Internet → ⋮ → Расширения → Tampermonkey Samsung → Включить**

## Использование

### Установка скрипта по URL
1. Найди скрипт на [Greasy Fork](https://greasyfork.org)
2. Скопируй ссылку на `.user.js` файл
3. Открой Tampermonkey → **+** → **Установить по URL**
4. Вставь ссылку → **Установить**

### Создание своего скрипта
1. Открой Tampermonkey → **+** → **Создать новый скрипт**
2. Напиши код в редакторе
3. Нажми **Сохранить** (иконка дискеты)

### Формат скрипта
```javascript
// ==UserScript==
// @name         Мой скрипт
// @version      1.0.0
// @description  Описание
// @author       dimkulik
// @match        https://example.com/*
// @run-at       document-end
// ==/UserScript==

(function() {
    'use strict';
    // Твой код здесь
})();
```

## Структура проекта

```
tampermonkey-samsung/
├── app/src/main/
│   ├── java/com/dimkulik/tampermonkey/
│   │   ├── model/UserScript.kt        — модель данных скрипта
│   │   ├── service/ScriptInjectionService.kt — точка входа для Samsung Internet
│   │   ├── ui/MainActivity.kt         — главный экран
│   │   ├── ui/EditorActivity.kt       — редактор кода
│   │   ├── ui/AddScriptActivity.kt    — установка по URL
│   │   ├── ui/ScriptAdapter.kt        — список скриптов
│   │   └── utils/ScriptRepository.kt — хранилище скриптов
│   └── res/layout/                    — XML layouts
├── .github/workflows/build.yml        — GitHub Actions сборка
└── README.md
```
