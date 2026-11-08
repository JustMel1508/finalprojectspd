"""Prepare only intended deliverables, excluding tools, private state and Git metadata."""
import zipfile,shutil,hashlib,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
out=ROOT/'dist';out.mkdir(exist_ok=True)
excluded={'.git','.tools','target','data','dist','tmp','.idea','__pycache__','.pytest_cache'}
# Allowlist prevents a configurable user-state directory from entering the archive.
root_files=['pom.xml','mvnw','mvnw.cmd','build.ps1','run.ps1','README.md','CONTRIBUTIONS.md','THIRD_PARTY_NOTICES.md','.gitignore','.gitattributes']
files=[ROOT/name for name in root_files]
for folder in ['src','tests','.mvn','docs','scripts']:
    files.extend(p for p in (ROOT/folder).rglob('*') if p.is_file() and not any(part in excluded for part in p.relative_to(ROOT).parts) and p.suffix not in {'.log','.pyc'})
files=sorted(set(files))
with zipfile.ZipFile(out/'source.zip','w',zipfile.ZIP_DEFLATED) as z:
    for p in files:z.write(p,'history-journey/'+p.relative_to(ROOT).as_posix())
jar=ROOT/'target/history-journey-1.0.0.jar'
shutil.copy2(jar,out/jar.name)
pdf=ROOT/'docs/report/history-journey-report.pdf'
if pdf.exists():shutil.copy2(pdf,out/pdf.name)
manifest={p.name:{'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in out.iterdir() if p.is_file() and p.name!='SHA256SUMS.json'}
(out/'SHA256SUMS.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
with zipfile.ZipFile(out/'source.zip') as z:
    assert not any(any(part in excluded for part in Path(n).parts[1:]) for n in z.namelist())
    assert 'history-journey/mvnw.cmd' in z.namelist()
    assert sum('/content/chapter-' in n and n.endswith('.json') for n in z.namelist())==10
    assert not any('/defense/' in n or n.endswith('/PLAN.md') for n in z.namelist())
    assert 'history-journey/tests/integration/process_smoke.py' in z.namelist()
    assert 'history-journey/tests/ui/ui_logic_test.cjs' in z.namelist()
    assert 'history-journey/docs/report/history-journey-report.pdf' in z.namelist()
    assert any(n.startswith('history-journey/tests/java/') and n.endswith('Test.java') for n in z.namelist())
    assert not any(n.startswith(('history-journey/tools/','history-journey/src/test/','history-journey/output/')) for n in z.namelist())
print('Packaged',len(files),'source files; JAR:',jar.stat().st_size,'bytes')
