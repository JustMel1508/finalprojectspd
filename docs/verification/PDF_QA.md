# Проверка PDF

Отчёт создан ReportLab 5.1.0 с установленными Arial/Consolas; отдельные файлы шрифтов не распространяются. Для рендеринга использован официальный Apache PDFBox 3.0.5: Poppler в среде отсутствует. PDF содержит кириллический текст, таблицы, Java-фрагменты, URL источников и UML. Редактируемый исходник — docs/report/REPORT.md; пересборка существующего текста: `python tools/build_report.py --from-source`.

Команды из папки проекта:

```powershell
python tools/build_report.py
& "$env:JAVA_HOME\bin\java.exe" '-Dpdfbox.fontcache=.tools' -jar .tools/pdfbox-app.jar render '-i=output/pdf/history-journey-report.pdf' '-format=png' '-dpi=90' '-prefix=tmp/pdfs/page'
& "$env:JAVA_HOME\bin\java.exe" '-Dpdfbox.fontcache=.tools' -jar .tools/pdfbox-app.jar export:text '-i=output/pdf/history-journey-report.pdf' '-o=tmp/pdfs/report-text.txt'
```

Инструменты разработки находятся в .tools и не включаются в исходный архив. Для пересоздания PDF нужны Python, reportlab, pillow и установленные Windows Arial/Consolas; на другой ОС задайте разрешённые шрифты с кириллицей в build_report.py. Игровое приложение этих инструментов не требует.

Визуальная проверка всех отрисованных страниц: читаемая кириллица, переносы таблиц без обрезки, согласованные поля, номера страниц, читаемые диаграммы на альбомных листах. Полная последовательность вынесена в редактируемый и векторный отдельный файл; в отчёте два читаемых фрагмента. Извлечение текста проверяет обязательные разделы и имена команды. Гиперссылки источников сформированы как PDF link annotations.

Реальные скриншоты UI отсутствуют по указанной в отчёте блокировке Computer Use. Статус этого критерия: Not run; диаграммы не объявляются скриншотами.
