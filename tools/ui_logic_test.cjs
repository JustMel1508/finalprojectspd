// Unit tests of game input and time-step logic with a mocked canvas/DOM.
// No browser is launched; these are not screenshots or a visual UI test.
const fs=require('node:fs'),vm=require('node:vm'),assert=require('node:assert/strict'),path=require('node:path');
const root=path.resolve(__dirname,'..'),listeners={};
const ctx=new Proxy({measureText:s=>({width:s.length*8}),createLinearGradient:()=>({addColorStop(){}})},{get:(t,k)=>k in t?t[k]:()=>{}});
const elements={'#modal':{open:false,addEventListener(){}},'#scene':{getContext:()=>ctx},'#interact':{},'#notice':{}};
const document={activeElement:{tagName:'BODY'},querySelector:s=>elements[s]||null,querySelectorAll:()=>[],getElementById:()=>null,addEventListener:(k,f)=>listeners[k]=f};
const sandbox={document,window:{addEventListener:(k,f)=>listeners[k]=f},requestAnimationFrame(){},console,localStorage:{getItem(){return null}},crypto:require('node:crypto').webcrypto};
vm.createContext(sandbox);
vm.runInContext(fs.readFileSync(path.join(root,'src/main/resources/static/app.js'),'utf8').replace(/boot\(\);\s*$/,''),sandbox);
const chapter=JSON.parse(fs.readFileSync(path.join(root,'src/main/resources/content/chapter-01.json'),'utf8'));
sandbox.fixture=chapter;
const run=code=>vm.runInContext(code,sandbox),checks=[];
const check=(name,fn)=>{fn();checks.push({check:name,status:'Passed'});};
function reset(){elements['#modal'].open=false;document.activeElement={tagName:'BODY'};run("state.page='game';state.player={avatar:'HERO'};state.active={order:1,state:'ACTIVE',visitedNpcs:[],scene:fixture};state.x=100;state.y=470;state.last=1;state.walkTo=null;state.busy=false;state.keys.clear();state.keys.add('ArrowRight');");}
check('Equal movement speed at 60 and 144 Hz',()=>{reset();run('for(let i=1;i<=60;i++)frame(1+i*1000/60)');const x60=run('state.x');reset();run('for(let i=1;i<=144;i++)frame(1+i*1000/144)');assert.ok(Math.abs(run('state.x')-x60)<.01);assert.ok(Math.abs(x60-270)<.01);});
check('Movement pauses while dialog is open',()=>{reset();elements['#modal'].open=true;run('frame(30)');assert.equal(run('state.x'),100);});
check('Lost focus clears held keys',()=>{reset();listeners.blur();assert.equal(run('state.keys.size'),0);});
check('Name input keeps WASD as text',()=>{reset();run('state.keys.clear()');document.activeElement={tagName:'INPUT'};let prevented=false;listeners.keydown({key:'w',preventDefault(){prevented=true;}});assert.equal(prevented,false);assert.equal(run('state.keys.size'),0);});
check('Russian keyboard layout moves character',()=>{reset();run('state.keys.clear()');listeners.keydown({key:'в',preventDefault(){}});run('frame(31)');assert.ok(run('state.x')>100);listeners.keyup({key:'в'});assert.equal(run('state.keys.size'),0);});
check('Obstacle collision stops movement',()=>{reset();run('state.x=fixture.obstacles[0].x-20;state.y=fixture.obstacles[0].y+25;for(let i=1;i<30;i++)frame(1+i*16)');assert.ok(run('state.x')<=chapter.obstacles[0].x-13);});
check('Diagonal speed is normalized',()=>{reset();run("state.keys.add('ArrowUp');frame(31)");const d=run('Math.hypot(state.x-100,state.y-470)');assert.ok(Math.abs(d-5.1)<.01);});
const output=path.join(root,'docs/verification/ui-logic-results.json');fs.writeFileSync(output,JSON.stringify({type:'Mocked unit tests, not browser QA',checks},null,2));console.log('Passed:',checks.length,'UI logic unit tests; visual browser test remains Not run');
