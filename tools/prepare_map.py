"""Extract only Kazakhstan geometry from Natural Earth v5.1.2, public domain."""
import json,hashlib
from pathlib import Path
root=Path(__file__).resolve().parents[1]
raw=(root/'.tools/natural-earth.geojson').read_bytes()
data=json.loads(raw)
feature=next(f for f in data['features'] if f['properties']['ADMIN']=='Kazakhstan')
out=root/'src/main/resources/static/assets'
out.mkdir(parents=True,exist_ok=True)
geo={'type':'Feature','properties':{'name':'Kazakhstan','source':'Natural Earth v5.1.2, 1:110m, public domain'},'geometry':feature['geometry']}
(out/'kazakhstan.geojson').write_text(json.dumps(geo,separators=(',',':')),encoding='utf-8')
geometry=feature['geometry']
polygons=geometry['coordinates'] if geometry['type']=='MultiPolygon' else [geometry['coordinates']]
paths=[]
for polygon in polygons:
    path=' '.join('M'+'L'.join(f'{25+(lon-46)*20:.2f},{35+(56-lat)*20:.2f}' for lon,lat in ring)+'Z' for ring in polygon)
    paths.append(f'<path class="country" d="{path}"/>')
(out/'kazakhstan.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 440"><title>Современный контур Казахстана</title>'+''.join(paths)+'</svg>',encoding='utf-8')
(root/'docs/MAP_DATA.md').write_text('''# Картографические данные

Natural Earth v5.1.2, ne_110m_admin_0_countries, масштаб 1:110m.
Источник: https://github.com/nvkelso/natural-earth-vector/blob/v5.1.2/geojson/ne_110m_admin_0_countries.geojson
Условия: https://www.naturalearthdata.com/about/terms-of-use/ — public domain.
Авторы проекта данных: Tom Patterson, Nathaniel Vaughn Kelso и участники Natural Earth.
Дата доступа: 2026-11-08. SHA-256 загруженного полного набора: '''+hashlib.sha256(raw).hexdigest()+'''.

Из полного набора извлечена только геометрия Казахстана. Локально: assets/kazakhstan.geojson и производный SVG, прямоугольная проекция. Набор не используется для точной навигации. Метки сцен приблизительные и созданы отдельно по современным географическим ориентирам. Оренбург вынесен на вставку. Исторические границы не изображаются.
''',encoding='utf-8')
print('Map geometry extracted and SVG generated')
