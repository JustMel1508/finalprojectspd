"""Audits only this new project's deliverable files; never reads previous project sources."""
import json,re,hashlib,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
product=[ROOT/'README.md']+list((ROOT/'src').rglob('*'))+list((ROOT/'docs/report').rglob('*'))
forbidden=['NITEC','AIplatform','АО «НИТ»','Академия НИТ']
matches=[]
for p in product:
    if p.is_file() and p.suffix in {'.md','.java','.json','.html','.css','.js','.svg','.properties'}:
        text=p.read_text(encoding='utf-8')
        if any(term.casefold() in text.casefold() for term in forbidden):matches.append(str(p.relative_to(ROOT)))
chapters=[json.loads(p.read_text(encoding='utf-8')) for p in sorted((ROOT/'src/main/resources/content').glob('chapter-*.json'))]
sources=[]
for c in chapters:
    for s in c['sources']:
        if s['id'] not in {v['id'] for v in sources}:sources.append(s)
historical=['# Исторические источники\n','Дата доступа: 2026-11-08. Публикации проверены через Firecrawl; сохранены URL и подтверждённые факты. Полные статьи и изображения не включаются в продукт. NPC — собственная авторская учебная реконструкция. Каждый NPC и задание содержит sourceId; справка рассказчика опирается на источники главы.\n',
'| ID | Публикация / организация | URL | Поддерживаемые факты и ограничения |\n| --- | --- | --- | --- |']
facts=[
'Керей и Жанибек; Шу/Западное Жетысу; традиционные 1465–1466 как процесс. Есен-Буга умер в 1462, не участвует в сцене 1465–1466.',
'1643; Жангир-хан и помощь Жалантос-батыра; ущелье Орбулак. Оценки численности не используются.',
'Начало 1723, рамки часто 1723–1727; вынужденные перемещения. В источнике встречаются спорные детали, в вопросы включены только дата и последствие.',
'Кенесары Касымулы; 1837–1847, эпизод Акмолинского укрепления 1838. Завершение 1847 сообщает позднейший рассказчик.',
'Указ о тыловых работах как непосредственный повод; Тургай — крупный центр. Более широкие причины не сводятся к одному указу.',
'Второй Всеказахский съезд в Оренбурге, декабрь 1917; автономия и правительство Алаш-Орда. Страница на казахском; избегаем разницы календарных стилей, используем месяц.',
'К. С. Алдажуманов, Институт истории и этнологии им. Ч. Ч. Валиханова: 1931–1933, насильственная коллективизация, заготовки, гибель и отток населения. Начальная хронологическая страница не содержала нужного текста и заменена научной публикацией. Числа жертв не оцениваются.',
'Казахстан в Великой Отечественной войне 1941–1945; Алма-Ата и воинские части. Общая Вторая мировая — 1939–1945, периоды не смешиваются.',
'Юрий Гагарин, 12 апреля 1961, Восток-1, старт с Байконура. МИД РК, публикация к 60-летию.',
'16 декабря 1991 — закон о государственной независимости. GOV.KZ: хронология; конкретное название закона уточняется официальным первоисточником S10b.',
]
for s in sources:
    if s['id']=='S10b':fact='Первичный текст закона: дата 16 декабря 1991, государственная независимость, столица Алма-Ата в тексте исторического закона.'
    elif s['id']=='S08b':fact='Отдельное подтверждение работы тыла, одежды для фронта, формирования 316-й дивизии в Алма-Ате. Собственные короткие объяснения; численные показатели не оцениваются.'
    else:fact=facts[int(s['id'][1:])-1]
    historical.append(f"| {s['id']} | {s['title']} / {s['organization']} | {s['url']} | {fact} |")
(ROOT/'docs/HISTORICAL_SOURCES.md').write_text('\n\n'.join(historical[:2])+'\n'+'\n'.join(historical[2:])+'\n',encoding='utf-8')
tests=[]
for p in (ROOT/'target/surefire-reports').glob('TEST-*.xml'):
    r=ET.parse(p).getroot();tests.append({key:r.get(key) for key in ['name','tests','failures','errors','skipped']})
result={'date':'2026-11-08','excludedNameMatchesInProduct':matches,'chapters':len(chapters),'npcs':sum(len(c['npcs']) for c in chapters),
'quizTasks':sum(t['kind']=='QUIZ' for c in chapters for t in c['tasks']),'mapTasks':sum(t['kind']=='MAP' for c in chapters for t in c['tasks']),
'junit':tests,'uiStatus':'Not run: browser host unavailable; Computer Use stopped Chrome access because URL could not be determined',
'studentIndependentWork':'Not confirmed','sourceOrigin':'New AI-assisted implementation in separate history-journey directory'}
out=ROOT/'docs/verification';out.mkdir(parents=True,exist_ok=True)
(out/'audit-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
assert not matches,matches
assert len(chapters)==10 and result['npcs']==20 and result['quizTasks']==20 and result['mapTasks']==10
print(json.dumps(result,ensure_ascii=False,indent=2))
