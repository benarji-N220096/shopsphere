import urllib.request
import json

req = urllib.request.Request('http://localhost:5173/api/v1/products')
try:
    with urllib.request.urlopen(req, timeout=5) as resp:
        print('Status:', resp.status)
        data = json.loads(resp.read().decode('utf-8'))
        print(f'Count: {len(data)}')
        for p in data:
            print(f"- {p.get('name')}: Price Rs {p.get('price')} | Img: {p.get('imageUrl')}")
except Exception as e:
    print('Error:', e)
