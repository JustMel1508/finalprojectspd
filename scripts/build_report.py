"""Create editable Russian report and PDF from actual project evidence.
Dependencies: reportlab, pillow; use installed system fonts, no runtime web requests.
"""
import sys,re,html,os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.tools/python'))
from reportlab.platypus import BaseDocTemplate,PageTemplate,Frame,Paragraph,Spacer,Table,TableStyle,Image,PageBreak,NextPageTemplate,Preformatted,KeepTogether
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4,landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

fontdir=Path(os.environ.get('WINDIR','C:/Windows'))/'Fonts'
pdfmetrics.registerFont(TTFont('Body',str(fontdir/'arial.ttf')))
pdfmetrics.registerFont(TTFont('Bold',str(fontdir/'arialbd.ttf')))
pdfmetrics.registerFont(TTFont('Mono',str(fontdir/'consola.ttf')))
pdfmetrics.registerFontFamily('Body',normal='Body',bold='Bold',italic='Body',boldItalic='Bold')
ink=colors.HexColor('#1c3546');teal=colors.HexColor('#24695f');muted=colors.HexColor('#65737a')
styles={
'body':ParagraphStyle('body',fontName='Body',fontSize=10.3,leading=15,textColor=ink,spaceAfter=10),
'h1':ParagraphStyle('h1',fontName='Bold',fontSize=25,leading=29,textColor=ink,spaceAfter=18),
'h2':ParagraphStyle('h2',fontName='Bold',fontSize=15,leading=19,textColor=teal,spaceBefore=10,spaceAfter=9),
'h3':ParagraphStyle('h3',fontName='Bold',fontSize=11.5,leading=16,textColor=ink,spaceAfter=8),
'cell':ParagraphStyle('cell',fontName='Body',fontSize=8.5,leading=12,textColor=ink),
'head':ParagraphStyle('head',fontName='Bold',fontSize=8.5,leading=12,textColor=colors.white),
'code':ParagraphStyle('code',fontName='Mono',fontSize=8.5,leading=12,textColor=ink,leftIndent=8,spaceAfter=12),
'caption':ParagraphStyle('caption',fontName='Body',fontSize=9,leading=13,textColor=muted,spaceAfter=10),
}

def markup(s):
    s=html.escape(s)
    s=re.sub(r'\*\*(.+?)\*\*',r'<b>\1</b>',s)
    return re.sub(r'\[([^\]]+)\]\((https?://[^\s)]+)\)',r'<link href="\2" color="#24695f">\1</link>',s)

def make_table(rows,width):
    n=len(rows[0]);widths=[width/n]*n
    if n==5:widths=[width*v for v in [.12,.19,.28,.24,.17]]
    if n==4:widths=[width*v for v in [.07,.37,.19,.37]]
    if n==3:widths=[width*v for v in [.32,.48,.20]]
    if n==2:widths=[width*.25,width*.75]
    data=[[Paragraph(markup(c),styles['head' if i==0 else 'cell']) for c in row] for i,row in enumerate(rows)]
    table=Table(data,colWidths=widths,repeatRows=1,hAlign='LEFT')
    table.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),teal),('ROWBACKGROUNDS',(0,1),(-1,-1),[colors.white,colors.HexColor('#eef2ed')]),('GRID',(0,0),(-1,-1),.5,colors.HexColor('#d9d9d9')),('VALIGN',(0,0),(-1,-1),'MIDDLE'),('LEFTPADDING',(0,0),(-1,-1),8),('RIGHTPADDING',(0,0),(-1,-1),8),('TOPPADDING',(0,0),(-1,-1),8),('BOTTOMPADDING',(0,0),(-1,-1),8)]))
    return table

def page_flow(text,wide):
    width=(landscape(A4)[0] if wide else A4[0])-88
    lines=text.strip().splitlines();flow=[];i=0
    while i<len(lines):
        line=lines[i].strip()
        if not line or line.startswith('<!--'):i+=1;continue
        if line.startswith('```'):
            code=[];i+=1
            while i<len(lines) and not lines[i].startswith('```'):code.append(lines[i]);i+=1
            flow.append(Preformatted('\n'.join(code),styles['code']));i+=1;continue
        if line.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].strip().startswith('|'):
                cells=[c.strip() for c in lines[i].strip().strip('|').split('|')]
                if not all(re.fullmatch(r'[-: ]+',c) for c in cells):rows.append(cells)
                i+=1
            flow.append(make_table(rows,width));flow.append(Spacer(1,12));continue
        image=re.match(r'!\[(.*?)\]\((.*?)\)',line)
        if image:
            img=Image(str((ROOT/'docs/report'/image[2]).resolve()))
            ratio=min(width/img.imageWidth,350/img.imageHeight)
            img.drawWidth=img.imageWidth*ratio;img.drawHeight=img.imageHeight*ratio;img.hAlign='CENTER';flow+=[img,Spacer(1,10)];i+=1;continue
        style='body'
        if line.startswith('### '):style='h3';line=line[4:]
        elif line.startswith('## '):style='h2';line=line[3:]
        elif line.startswith('# '):style='h1';line=line[2:]
        flow.append(Paragraph(markup(line),styles[style]));i+=1
    return flow

out=ROOT/'docs/report';out.mkdir(parents=True,exist_ok=True)
source=ROOT/'docs/report/REPORT.md';source.parent.mkdir(parents=True,exist_ok=True)
if not source.exists(): raise FileNotFoundError('Editable report source missing: '+str(source))
text=source.read_text(encoding='utf-8')
def decorate(canvas,doc):
    w,h=canvas._pagesize
    canvas.saveState();canvas.setFillColor(muted);canvas.setFont('Body',8)
    canvas.drawString(44,h-25,'HISTORY JOURNEY  /  SOFTWARE DESIGN PATTERNS')
    canvas.drawString(44,23,'Бахыт Карима · Адай Аида · Казбекова Айша')
    canvas.drawRightString(w-44,23,str(doc.page));canvas.restoreState()

pdf=out/'history-journey-report.pdf'
doc=BaseDocTemplate(str(pdf),title='Путешествие по истории Казахстана — Software Design Patterns',author='Бахыт Карима; Адай Аида; Казбекова Айша — AI-assisted implementation disclosed',subject='Технический отчёт: архитектура, шесть паттернов и фактические проверки',leftMargin=44,rightMargin=44,topMargin=49,bottomMargin=42)
templates=[]
for name,size in [('portrait',A4),('landscape',landscape(A4))]:
    templates.append(PageTemplate(id=name,pagesize=size,frames=[Frame(44,42,size[0]-88,size[1]-91,leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=decorate))
doc.addPageTemplates(templates)
story=[]
for index,part in enumerate(text.split('<!-- PAGE -->')):
    wide='<!-- LANDSCAPE -->' in part
    if index:story.extend([NextPageTemplate('landscape' if wide else 'portrait'),PageBreak()])
    story.extend(page_flow(part,wide))
doc.build(story)
print('Created',pdf)
