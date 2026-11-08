"""Real separate-process API and restart verification, standard Python only.
Usage: python tests/integration/process_smoke.py --java /path/to/java [--jar /path/to/jar]
"""
import argparse,json,subprocess,tempfile,time,uuid,urllib.request,urllib.error,socket
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
args=argparse.ArgumentParser()
args.add_argument('--java',default='java');args.add_argument('--jar',default=str(ROOT/'target/history-journey-1.0.0.jar'))
args=args.parse_args()
content=[json.loads(p.read_text(encoding='utf-8')) for p in sorted((ROOT/'src/main/resources/content').glob('chapter-*.json'))]
checks=[]
def check(label,condition):
    assert condition,label
    checks.append({'check':label,'status':'Passed'})
scratch=ROOT/'.tools'
scratch.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix='history-process-',dir=scratch) as folder:
    work=Path(folder)
    with socket.socket() as sock:
        sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]
    url=f'http://127.0.0.1:{port}/api'
    # Avoid inheriting host proxy settings for loopback requests.
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    def request(path,method='GET',body=None,status=200,raw=False):
        req=urllib.request.Request(url+path,method=method,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
        try:
            response=opener.open(req,timeout=15);code=response.status;payload=response.read().decode('utf-8')
        except urllib.error.HTTPError as e:code=e.code;payload=e.read().decode('utf-8')
        assert code==status,(path,code,payload)
        return payload if raw else json.loads(payload) if payload else None
    log=open(work/'server.log','w',encoding='utf-8')
    def launch():
        process=subprocess.Popen([args.java,'-jar',str(Path(args.jar).resolve()),f'--server.port={port}',f'--game.data-dir={work / "data"}'],cwd=work,stdout=log,stderr=subprocess.STDOUT)
        deadline=time.monotonic()+50
        while time.monotonic()<deadline:
            if process.poll() is not None:raise RuntimeError((work/'server.log').read_text(encoding='utf-8'))
            try:
                if request('/health')['ready']:return process
            except Exception:time.sleep(.3)
        process.terminate();raise RuntimeError('Server startup timeout')
    def stop(process):
        process.terminate()
        try:process.wait(timeout=12)
        except subprocess.TimeoutExpired:process.kill();process.wait(timeout=5)
    process=launch()
    succeeded=False
    try:
        for resource in ['/','/app.js','/style.css','/assets/kazakhstan.svg','/assets/compass.svg']:
            with opener.open(f'http://127.0.0.1:{port}'+resource,timeout=15) as response:
                assert response.status==200 and response.read(),resource
        check('HTML, JavaScript, CSS, map and icon are served locally',True)
        for name in ['', '   ','x'*41]:request('/players','POST',{'name':name,'avatar':'HERO'},400)
        check('HTTP validation of empty/blank/long names',True)
        p=request('/players','POST',{'name':'Проверка процесса','avatar':'HEROINE'},201)['id'];base='/players/'+p
        for c in request(base+'/map'):check('Overview title masked '+c['id'],c['displayTitle']==f'Уровень {c["order"]}')
        request(base+'/attempts','POST',{'chapterId':'c10','mode':'LEARNING'},409)
        request('/players/missing/map',status=404)
        a=request(base+'/attempts','POST',{'chapterId':'c1','mode':'LEARNING'},201);aid=a['id'];ap=base+'/attempts/'+aid
        for n in content[0]['npcs']:request(ap+'/talk','POST',{'npcId':n['id']})
        first=content[0]['tasks'][0]
        request(ap+'/answers','POST',{'requestId':str(uuid.uuid4()),'taskId':first['id'],'optionId':first['correctOptionId']})
        request('/players/'+p,'PUT',{'name':'После перезапуска','avatar':'HERO'})
        stop(process);process=launch()
        active=request(base+'/active')
        check('Separate Java process restores attempt, NPCs and answers',active['id']==aid and len(active['visitedNpcs'])==2 and active['acceptedTasks']==1)
        check('Profile rename and avatar persist',any(x['id']==p and x['name']=='После перезапуска' and x['avatar']=='HERO' for x in request('/players')))
        for t in content[0]['tasks'][1:]:request(ap+'/answers','POST',{'requestId':str(uuid.uuid4()),'taskId':t['id'],'optionId':t['correctOptionId']})
        result=request(ap+'/finish','POST');request(ap+'/finish','POST')
        check('Finish idempotent and title revealed',result['result']['passed'] and request(base+'/summary')['completedAttempts']==1 and result['displayTitle'].startswith(content[0]['title']))
        txt=request(ap+'/export',raw=True);check('UTF-8 export matches saved statistics','Заданий: 3; верно: 3; ответов: 3; ошибок: 0' in txt)
        # Required restart journey: mastered chapter 1, partially answered chapter 2.
        second=content[1]
        a=request(base+'/attempts','POST',{'chapterId':second['id'],'mode':'ASSESSMENT'},201)
        second_id=a['id'];second_path=base+'/attempts/'+second_id
        for n in second['npcs']:request(second_path+'/talk','POST',{'npcId':n['id']})
        t=second['tasks'][0]
        second_body={'requestId':str(uuid.uuid4()),'taskId':t['id'],'optionId':t['correctOptionId']}
        request(second_path+'/answers','POST',second_body)
        stop(process);process=launch()
        resumed=request(base+'/active');progress=request(base+'/map')
        check('Restart with chapter 1 mastered restores partial chapter 2',resumed['id']==second_id and resumed['chapterId']=='c2' and resumed['mode']=='ASSESSMENT' and len(resumed['visitedNpcs'])==2 and resumed['acceptedTasks']==1)
        check('Restart retains revealed chapter 1 and next-level access',progress[0]['displayTitle'].startswith(content[0]['title']) and progress[1]['status']=='AVAILABLE' and progress[2]['status']=='LOCKED')
        check('Restart retains previous result and profile',request(base+'/summary')['completedChapters']==1 and any(x['id']==aid and x['passed'] for x in request(base+'/history')) and any(x['id']==p and x['name']=='После перезапуска' and x['avatar']=='HERO' for x in request('/players')))
        retried=request(second_path+'/answers','POST',second_body)
        check('Duplicate answer after process restart keeps same stage',retried['acceptedTasks']==1 and retried['currentTask']['id']==second['tasks'][1]['id'])
        for t in second['tasks'][1:]:request(second_path+'/answers','POST',{'requestId':str(uuid.uuid4()),'taskId':t['id'],'optionId':t['correctOptionId']})
        second_result=request(second_path+'/finish','POST')
        check('Resumed chapter 2 completes and opens chapter 3',second_result['result']['passed'] and request(base+'/map')[2]['status']=='AVAILABLE')
        # Revisit passed chapter, fail assessment; title and unlocked progress must remain.
        a=request(base+'/attempts','POST',{'chapterId':'c1','mode':'ASSESSMENT'},201);ap=base+'/attempts/'+a['id']
        for n in content[0]['npcs']:request(ap+'/talk','POST',{'npcId':n['id']})
        for index,t in enumerate(content[0]['tasks']):
            option=next(o['id'] for o in t['options'] if o['id']!=t['correctOptionId']) if index==0 else t['correctOptionId']
            response=request(ap+'/answers','POST',{'requestId':str(uuid.uuid4()),'taskId':t['id'],'optionId':option})
            check('Assessment response hides correctness '+t['id'],response['result'] is None and 'correctOptionId' not in json.dumps(response))
        failure=request(ap+'/finish','POST');check('Failed assessment retained, previously revealed title remains',not failure['result']['passed'] and failure['displayTitle'].startswith(content[0]['title']))
        for c in content[2:]:
            a=request(base+'/attempts','POST',{'chapterId':c['id'],'mode':'ASSESSMENT'},201);ap=base+'/attempts/'+a['id']
            for n in c['npcs']:request(ap+'/talk','POST',{'npcId':n['id']})
            for t in c['tasks']:
                body={'requestId':str(uuid.uuid4()),'taskId':t['id'],'optionId':t['correctOptionId']}
                first_response=request(ap+'/answers','POST',body);retry=request(ap+'/answers','POST',body);check('Idempotent API answer '+t['id'],first_response==retry)
            result=request(ap+'/finish','POST');check('Chapter '+c['id']+' complete via HTTP',result['result']['passed'])
        summary=request(base+'/summary');check('Ten chapters and final boundary',summary['finished'] and summary['completedChapters']==10 and summary['completedAttempts']==11 and summary['failedAttempts']==1)
        check('Full summary export', 'Пройдено: 10 / 10' in request(base+'/export',raw=True))
        p2=request('/players','POST',{'name':'Независимый','avatar':'HERO'},201)['id'];check('Independent progress',request('/players/'+p2+'/summary')['completedChapters']==0)
        a=request(base+'/attempts','POST',{'chapterId':'c10','mode':'LEARNING'},201);request(base+'/attempts/'+a['id']+'/cancel','POST');check('Cancellation keeps ten mastered chapters',request(base+'/summary')['completedChapters']==10)
        # Corrupt file in this disposable directory, preserve it and surface recovery action.
        stop(process);(work/'data/save.json').write_text('{broken',encoding='utf-8')
        process=subprocess.Popen([args.java,'-jar',str(Path(args.jar).resolve()),f'--server.port={port}',f'--game.data-dir={work / "data"}'],cwd=work,stdout=log,stderr=subprocess.STDOUT)
        for _ in range(100):
            try:health=request('/health');break
            except Exception:time.sleep(.2)
        check('Corruption reported without overwriting file',not health['ready'] and (work/'data/save.json').read_text()=='{broken')
        request('/players',status=503)
        succeeded=True
    finally:
        stop(process);log.close()
        if not succeeded:print((work/'server.log').read_text(encoding='utf-8')[-7000:])
out=ROOT/'docs/verification/process-results.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'checks':checks,'count':len(checks),'jar':Path(args.jar).name,'java':args.java},ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Passed: {len(checks)} process/API checks, including real restart and ten chapters')
