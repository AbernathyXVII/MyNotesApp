# Genera gli asset del selettore delle emoji di notE (app/src/main/assets/emoji)
# da emoji-test.txt di Unicode e dalle annotazioni CLDR (nomi e parole chiave
# nelle 8 lingue dell'app). Scarica da sé i file che servono:
#
#   python tools/emoji/make_emoji_assets.py [versione Unicode, di partenza 16.0]
#
#   emoji.txt        "@categoria" e poi un'emoji per riga; "	1" se accetta il colore della pelle
#   names_<lang>.txt "emoji	parole" (minuscole, separate da "|")
import io, json, os, re, sys, tempfile, urllib.request

HERE = tempfile.mkdtemp(prefix='emoji-')
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'assets', 'emoji')
os.makedirs(OUT, exist_ok=True)
VERSION = sys.argv[1] if len(sys.argv) > 1 else '16.0'
LANGS = ['en', 'it', 'de', 'fr', 'es', 'zh', 'ko', 'ja']
CLDR = 'https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/'

def download(url, name):
    with urllib.request.urlopen(url) as r, open(os.path.join(HERE, name), 'wb') as f:
        f.write(r.read())

download(f'https://unicode.org/Public/emoji/{VERSION}/emoji-test.txt', 'emoji-test.txt')
for lang in LANGS:
    download(CLDR + f'cldr-annotations-full/annotations/{lang}/annotations.json', f'ann_{lang}.json')
    download(CLDR + f'cldr-annotations-derived-full/annotationsDerived/{lang}/annotations.json', f'der_{lang}.json')

GROUPS = {
    'Smileys & Emotion': 'people',
    'People & Body': 'people',
    'Animals & Nature': 'nature',
    'Food & Drink': 'food',
    'Activities': 'activity',
    'Travel & Places': 'travel',
    'Objects': 'objects',
    'Symbols': 'symbols',
    'Flags': 'flags',
}
ORDER = ['people', 'nature', 'food', 'activity', 'travel', 'objects', 'symbols', 'flags']
TONES = [0x1F3FB, 0x1F3FC, 0x1F3FD, 0x1F3FE, 0x1F3FF]

LINE = re.compile(r'^([0-9A-F ]+?)\s*;\s*fully-qualified\s*#\s*\S+\s+E[\d.]+\s+(.*)$')
fq = []          # (categoria, codepoints, nome inglese)
group = None
fq_set = set()
for raw in io.open(os.path.join(HERE, 'emoji-test.txt'), encoding='utf-8'):
    raw = raw.rstrip('\n')
    if raw.startswith('# group:'):
        group = GROUPS.get(raw.split(':', 1)[1].strip())
        continue
    m = LINE.match(raw)
    if not m:
        continue
    cps = tuple(int(x, 16) for x in m.group(1).split())
    fq_set.add(cps)
    if group is None:
        continue
    fq.append((group, cps, m.group(2)))

def toned(cps, tone):
    rest = list(cps[1:])
    if rest and rest[0] == 0xFE0F:
        rest = rest[1:]
    return (cps[0], tone, *rest)

def s(cps):
    return ''.join(chr(c) for c in cps)

base = [(g, cps, name) for g, cps, name in fq if not any(c in TONES for c in cps)]
by_cat = {c: [] for c in ORDER}
for g, cps, name in base:
    tone_ok = all(toned(cps, t) in fq_set for t in TONES)
    by_cat[g].append((cps, name, tone_ok))

with io.open(os.path.join(OUT, 'emoji.txt'), 'w', encoding='utf-8', newline='\n') as f:
    for c in ORDER:
        f.write('@' + c + '\n')
        for cps, name, tone_ok in by_cat[c]:
            f.write(s(cps) + ('\t1' if tone_ok else '') + '\n')

total = sum(len(v) for v in by_cat.values())
print('emoji', total, {c: len(v) for c, v in by_cat.items()})

def load(lang):
    out = {}
    for prefix, key in (('ann', 'annotations'), ('der', 'annotationsDerived')):
        data = json.load(io.open(os.path.join(HERE, f'{prefix}_{lang}.json'), encoding='utf-8'))
        out.update(data[key]['annotations'])
    return out

english = {s(cps): name for g, cps, name in base}
for lang in LANGS:
    ann = load(lang)
    missing = 0
    with io.open(os.path.join(OUT, f'names_{lang}.txt'), 'w', encoding='utf-8', newline='\n') as f:
        for c in ORDER:
            for cps, name, tone_ok in by_cat[c]:
                e = s(cps)
                entry = ann.get(e) or ann.get(e.replace('\uFE0F', ''))
                words = []
                if entry:
                    words += entry.get('tts', []) + entry.get('default', [])
                else:
                    missing += 1
                if lang == 'en' or not entry:
                    words.append(english[e])
                seen = []
                for w in words:
                    w = w.strip().lower().replace('|', ' ')
                    if w and w not in seen:
                        seen.append(w)
                f.write(e + '\t' + '|'.join(seen) + '\n')
    size = os.path.getsize(os.path.join(OUT, f'names_{lang}.txt'))
    print(lang, 'senza nome', missing, 'byte', size)
