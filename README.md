# АлгоКраска — сборка APK через GitHub

В проекте оставлена только сборка тестового Android APK.

## Загрузка в GitHub

1. Создайте пустой репозиторий.
2. Распакуйте архив.
3. Загрузите в репозиторий всё содержимое распакованной папки.
4. В корне репозитория должны быть видны:
   - `.github`
   - `app`
   - `build.gradle`
   - `settings.gradle`
   - `gradle.properties`
5. Откройте `Actions`.
6. Выберите `Build APK`.
7. Нажмите `Run workflow`.

После успешной сборки скачайте артефакт:

`AlgoKraska-debug-apk`

Внутри находится:

`app-debug.apk`

## Конфигурация сборки

- Android Gradle Plugin: 8.7.3
- Gradle: 8.9
- Java: 17
- compileSdk: 35
- targetSdk: 35
- minSdk: 24

Файлы и настройки для Google Play в этот архив не включены.
