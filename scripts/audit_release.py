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
source_notes=(ROOT/'docs/HISTORICAL_SOURCES.md').read_text(encoding='utf-8')
for source in sources:
    assert source['id'] in source_notes and source['url'] in source_notes, 'Missing historical source: '+source['id']
tests=[]
for p in (ROOT/'target/surefire-reports').glob('TEST-*.xml'):
    r=ET.parse(p).getroot();tests.append({key:r.get(key) for key in ['name','tests','failures','errors','skipped']})
result={'date':'2026-11-08','excludedNameMatchesInProduct':matches,'chapters':len(chapters),'npcs':sum(len(c['npcs']) for c in chapters),
'quizTasks':sum(t['kind']=='QUIZ' for c in chapters for t in c['tasks']),'mapTasks':sum(t['kind']=='MAP' for c in chapters for t in c['tasks']),
'junit':tests,'uiStatus':'Not run: browser access host unavailable; new-tab request to local game failed',
'studentIndependentWork':'Not confirmed','sourceOrigin':'New AI-assisted implementation in separate history-journey directory'}
out=ROOT/'docs/verification';out.mkdir(parents=True,exist_ok=True)
(out/'audit-results.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
assert not matches,matches
assert len(chapters)==10 and result['npcs']==20 and result['quizTasks']==20 and result['mapTasks']==10
print(json.dumps(result,ensure_ascii=False,indent=2))
