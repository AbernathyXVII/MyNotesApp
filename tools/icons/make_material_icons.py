# Genera MaterialIconSet.kt: i nomi delle icone piene di Material (la libreria
# material-icons-extended dell'app) e la funzione che dal nome dà l'icona, per la
# scheda "Icons" del selettore delle icone di notE.
#
#   python tools/icons/make_material_icons.py
#
# Legge i sorgenti delle due librerie (di base ed estesa) dalla cache di Gradle
# (la versione è quella di app/build.gradle.kts). Le icone che Material ha spostato fra quelle che si
# specchiano nelle lingue da destra a sinistra (AutoMirrored) si prendono da lì.
import glob, io, os, re, zipfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
gradle = io.open(os.path.join(ROOT, 'app', 'build.gradle.kts'), encoding='utf-8').read()
version = re.search(r'material-icons-extended:([\d.]+)', gradle).group(1)
cache = os.path.join(os.path.expanduser('~'), '.gradle', 'caches', 'modules-2', 'files-2.1', 'androidx.compose.material')
# Le icone più comuni (Home, Star, Settings…) stanno nella libreria di base,
# le altre in quella estesa: servono tutte e due.
zips = []
for library in ['material-icons-core-android', 'material-icons-extended-android']:
    jars = glob.glob(os.path.join(cache, library, version, '*', f'{library}-{version}-sources.jar'))
    assert jars, f'Sorgenti di {library} {version} non trovati: scaricarli una volta con Gradle o Android Studio'
    zips.append(zipfile.ZipFile(jars[0]))

def names_in(folder):
    out = []
    for z in zips:
        for n in z.namelist():
            m = re.match(r'commonMain/androidx/compose/material/icons/' + folder + r'/([^/]+)\.kt$', n)
            if m:
                out.append(m.group(1))
    return out

filled = names_in('filled')
mirrored = set(names_in('automirrored/filled'))
# Quelle che cominciano con un numero (10k, 12mp…) vanno in fondo: sono le meno usate.
names = sorted(set(filled), key=lambda n: (n.lstrip('_')[0].isdigit(), n.lower().lstrip('_')))

def ref(name):
    return ('Icons.AutoMirrored.Filled.' if name in mirrored else 'Icons.Filled.') + name

groups = {}
for name in names:
    key = name.lstrip('_')[0].upper()
    groups.setdefault(key, []).append(name)

lines = [
    '// Generato da tools/icons/make_material_icons.py: non modificarlo a mano.',
    'package com.gabriele.notionlocal.ui.screen',
    '',
    'import androidx.compose.material.icons.Icons',
    'import androidx.compose.material.icons.automirrored.filled.*',
    'import androidx.compose.material.icons.filled.*',
    'import androidx.compose.ui.graphics.vector.ImageVector',
    '',
    f'/** I nomi delle {len(names)} icone piene di Material {version}, in ordine alfabetico, quelle coi numeri in fondo: la scheda "Icons" del selettore. */',
    'internal val MATERIAL_ICON_NAMES: List<String> = listOf(',
]
for i in range(0, len(names), 8):
    lines.append('    ' + ', '.join(f'"{n}"' for n in names[i:i + 8]) + ',')
lines += [
    ')',
    '',
    '/**',
    ' * L\'icona di Material col suo nome ("Home"), o null per un nome che non c\'è',
    ' * (per esempio di una versione della libreria più nuova). Le icone si',
    ' * disegnano solo quando servono: qui c\'è solo il nome.',
    ' */',
    'internal fun materialIconByName(name: String): ImageVector? = when (name.trimStart(\'_\').firstOrNull()?.uppercaseChar()) {',
]
for key in sorted(groups):
    lines.append(f"    '{key}' -> icons{key}(name)")
lines += ['    else -> null', '}', '']
for key in sorted(groups):
    lines.append(f'private fun icons{key}(name: String): ImageVector? = when (name) {{')
    for name in groups[key]:
        lines.append(f'    "{name}" -> {ref(name)}')
    lines += ['    else -> null', '}', '']

out = os.path.join(ROOT, 'app', 'src', 'main', 'java', 'com', 'gabriele', 'notionlocal', 'ui', 'screen', 'MaterialIconSet.kt')
io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines))
print(len(names), 'icone,', len(mirrored), 'da AutoMirrored, gruppi', ''.join(sorted(groups)))
