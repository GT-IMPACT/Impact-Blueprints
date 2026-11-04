# GUI 3D Engine

Сборка: Gradle 9.7.1 на JDK 25. Для Minecraft 1.7.10 также нужен установленный JDK 8.

```shell
git submodule update --init --recursive
./gradlew build
```

Параметры мода и публикации задаются в `impactMod` в `build.gradle.kts`.
Версия Forgelin переопределяется в каталоге `libs` в `settings.gradle.kts`.
ID мода — `gui3dengine`, пакет — `space.impact.gui3dengine`, имя Maven-артефакта — `Gui3dEngine`.
Версия мода берётся из `RELEASE_VERSION`, затем `VERSION`, затем Git.
Для публикации используются `IMPACT_MAVEN_USER` и `IMPACT_MAVEN_PASSWORD`.
