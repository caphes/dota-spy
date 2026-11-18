# Дота Шпион

Нативная офлайн-игра для Android от трёх человек на одном телефоне. Мирные игроки получают одну общую карту, а шпионы пытаются угадать её по обсуждению. В колоде все 127 героев из каталога Valve и 50 предметов Dota 2 с короткими русскими описаниями. [Каталог героев Dota 2](https://www.dota2.com/heroes)

Приложение написано на Java с Android SDK. Поддерживается Android 6.0 (API 23) и новее. Имя каждого игрока сохраняется в памяти устройства. Колода и изображения находятся в APK и доступны с первого запуска в авиарежиме.

## Скачать на телефон

На iPhone откройте [веб-версию «Дота Шпион»](https://caphes.github.io/dota-spy/) в Safari. Чтобы добавить игру на экран «Домой», выберите **Поделиться → На экран «Домой»**. После первой полной загрузки игру можно открыть без интернета. [Инструкция Apple](https://support.apple.com/guide/iphone/iphea86e5236/ios)

Откройте [страницу последнего релиза](https://github.com/caphes/dota-spy/releases/latest) на Android и скачайте APK из раздела **Assets**. Откройте файл из папки загрузок и разрешите установку из этого источника, если Android запросит разрешение.

## Сборка и установка

Для сборки нужны JDK 17 и Android SDK Platform 35 с Build-Tools 35.0.0. В Windows выполните из папки проекта:

```powershell
.\scripts\build-apk.cmd
```

Скрипт запускает тесты и собирает APK с тестовой подписью. Gradle Wrapper проверяет контрольную сумму загружаемого архива Gradle 8.13. Также можно открыть проект в Android Studio и выбрать **Build → Generate App Bundles or APKs → Generate APKs** или выполнить `sh gradlew testDebugUnitTest assembleDebug` на macOS и Linux. [Документация Android по сборке APK](https://developer.android.com/build/building-cmdline)

Файл находится в `app/build/outputs/apk/debug/app-debug.apk`. Передайте его на телефон, откройте из файлового менеджера и разрешите установку из этого источника, если Android запросит разрешение. При подключении телефона через USB и включённой отладке можно выполнить:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Во время сборки доступные иллюстрации загружаются с CDN Valve и упаковываются в APK. Карты без загруженной картинки получают локальную графику. Для сборки с локальной графикой используйте `.\scripts\build-apk.cmd --placeholder-art` или `sh gradlew assembleDebug -PplaceholderArt`. [Иллюстрация Pudge](https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/heroes/pudge.png) · [Иллюстрация Blink Dagger](https://cdn.cloudflare.steamstatic.com/apps/dota2/images/dota_react/items/blink.png)

Если в папке проекта есть `private-art/pudge.jpg`, сборка использует эту фотографию на карточке Pudge. Папка исключена из Git, поэтому сборка из чистого репозитория использует стандартный портрет Valve.

## Как играть

1. Добавьте или отредактируйте минимум три имени. Выберите героев, предметы или обе категории и укажите от 1 до 10 минут обсуждения. Доступен 1 шпион для 3–4 участников, 2 для 5–6 и 3 для 7 и более.
2. Передавайте телефон по порядку. Игрок открывает роль и нажимает **Скрыть и передать**. При сворачивании приложения карта закрывается, и тот же игрок может открыть её снова.
3. Как только последний игрок скроет карту, запустится обсуждение. Таймер можно приостановить, продолжить или завершить досрочно. Если время истечёт в фоне, экран завершения и сигнал появятся при возвращении в игру.
4. После сигнала нажмите **Раскрыть карты**. На экране появятся карта и имена шпионов. **Играть снова** возвращает в лобби.

Для проверки офлайн-режима включите авиарежим перед первым запуском установленного APK и сыграйте полную партию.

## Структура проекта

```text
dota-spy/
├── .github/workflows/ci.yml        # Проверка и сборка тестового APK
├── app/
│   ├── build.gradle
│   ├── card-art.gradle             # Подготовка иллюстраций
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/deck.json
│       │   ├── java/ru/dotaspy/game/
│       │   │   ├── MainActivity.java
│       │   │   ├── GameRules.java
│       │   │   ├── LocalData.java
│       │   │   └── Glass.java
│       │   └── res/
│       └── test/java/ru/dotaspy/game/GameRulesTest.java
├── scripts/build-apk.cmd
├── web/                           # Веб-приложение и сборка для GitHub Pages
├── gradle/wrapper/
├── gradlew / gradlew.bat
├── gradle.properties
├── settings.gradle
└── README.md
```

## GitHub Actions и релизы

GitHub Actions запускает тесты, проверяет подпись APK, наличие колоды и отсутствие разрешения на интернет. APK из CI доступен в **Actions → Сборка Android APK → Artifacts → dota-spy-android-debug**. Файл в разделе **Releases** можно скачать на телефон напрямую.

Веб-версия использует ту же колоду. Для локальной веб-сборки сначала соберите APK, затем выполните `node web/build.mjs` с Node.js 22. Результат появится в `web/dist/`. GitHub Pages получает иллюстрации из проверенного Android-релиза. При изменении набора карточек обновите ссылку на APK и его SHA-256 в `.github/workflows/pages.yml`.

Android принимает обновление поверх установленной версии, если оба APK подписаны одним и тем же ключом. [Документация Android по подписи](https://developer.android.com/studio/publish/app-signing)

«Дота Шпион» — фанатская игра. Названия и оригинальные иллюстрации Dota 2 принадлежат Valve.
