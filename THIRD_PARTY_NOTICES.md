# Внешние материалы и инструменты

| Материал | Источник / автор | Условия / использование |
| --- | --- | --- |
| Natural Earth v5.1.2, 1:110m | Tom Patterson, Nathaniel Vaughn Kelso, contributors; naturalearthdata.com | Public domain. Извлечена геометрия Казахстана; преобразована в SVG. См. docs/MAP_DATA.md |
| Maven Wrapper 3.3.4 и Maven 3.9.12 | Apache Software Foundation; maven.apache.org | Apache License 2.0. Официальные mvnw, mvnw.cmd и properties созданы wrapper plugin; дистрибутив скачивается отдельно |
| Spring Boot 3.5.7 / Spring Framework / Jackson / Tomcat / Hibernate Validator | Maven Central, соответствующие проекты | Лицензии транзитивных зависимостей сохраняются в JAR META-INF и pom артефактов; преимущественно Apache 2.0, Jakarta-зависимости имеют собственные условия. Не объявляются собственным кодом |
| JUnit Jupiter 5.12.2 | junit.org | EPL 2.0; только тестирование |
| PlantUML 1.2025.10 | plantuml.com | GPL; используется как внешний инструмент отрисовки. Сам инструмент не включён в source.zip/JAR. UML-тексты проекта созданы с AI-помощью |
| ReportLab 5.1.0, Pillow 13.0.0 | pypi.org, официальные проекты | Внешние инструменты подготовки PDF, не часть игрового JAR; ReportLab BSD, Pillow HPND и notices дистрибутива. Wheel получены из локального кеша официального PyPI |
| Apache PDFBox 3.0.5 | Apache Software Foundation, pdfbox.apache.org | Apache License 2.0; рендеринг PDF и извлечение текста. Инструмент не включён в source.zip/JAR |
| Системные шрифты Segoe UI / Arial | Microsoft, установленные Windows fonts | В игре указаны CSS-семейства с fallback; файлы шрифтов не распространяются. В PDF используется установленный Arial с разрешённым в шрифте embedding; шрифты не включаются отдельно в исходники |
| Исторические публикации | Qazaqstan tarihy / GOV.KZ / К. С. Алдажуманов | Используются для проверки фактов. Статьи, фотографии, видео и оформление не копируются. Собственные краткие тексты и ссылки: docs/HISTORICAL_SOURCES.md |

Новый код, тексты и рисунки: AI-assisted implementation, Codex, 2026-11-08; самостоятельное студенческое авторство требует подтверждения. Лицензию нового проекта команда выбирает отдельно. Полные публикации источников не включены в архив.
