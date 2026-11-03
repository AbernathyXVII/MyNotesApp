# Scarica i dizionari del correttore di notE (app/src/main/assets/proofing):
# i file di Hunspell dei pacchetti npm "dictionary-*" di wooorm, presi da
# jsDelivr, ognuno con la sua licenza.
#
#   python tools/proofing/get_dictionaries.py
#
# Le versioni sono fissate: quelle con cui il correttore è stato provato
# (SpellDictionaryTest). Cambiandole, rifare girare i test.
#
#   proofing/<cartella>/index.aff   le regole (prefissi, desinenze, suggerimenti)
#   proofing/<cartella>/index.dic   le radici
#   proofing/<cartella>/LICENSE.txt la licenza del dizionario
#
# Licenze: inglese (SCOWL) MIT e BSD; italiano GPL-3.0; tedesco GPL-2.0 o
# GPL-3.0; francese MPL-2.0; spagnolo GPL-3.0, LGPL-3.0 o MPL-1.1.
import os, urllib.request

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'assets', 'proofing')

DICTIONARIES = [
    # pacchetto, versione, cartella
    ('dictionary-en', '4.0.0', 'en_US'),
    ('dictionary-en-gb', '3.0.0', 'en_GB'),
    ('dictionary-it', '2.0.0', 'it'),
    ('dictionary-de', '3.0.0', 'de'),
    ('dictionary-fr', '3.0.0', 'fr'),
    ('dictionary-es', '4.0.0', 'es'),
]

for package, version, folder in DICTIONARIES:
    target = os.path.join(OUT, folder)
    os.makedirs(target, exist_ok=True)
    for remote, local in (('index.aff', 'index.aff'), ('index.dic', 'index.dic'), ('license', 'LICENSE.txt')):
        url = f'https://cdn.jsdelivr.net/npm/{package}@{version}/{remote}'
        with urllib.request.urlopen(url) as r, open(os.path.join(target, local), 'wb') as f:
            f.write(r.read())
    print(folder, 'ok')
