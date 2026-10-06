# notE — Notion-clone locale per Android

(nome interno del progetto/pacchetto: NotionLocal — invariato, cambia
solo il nome mostrato sul telefono)

App di note/database stile Notion, con **tutti i dati salvati sul
dispositivo** (SQLite via Room). Nessuna rete, nessun account, nessun
cloud.

---

## ⚠️ LEGGIMI PRIMA — contesto per chi riprende il progetto

Questa sezione esiste perché il progetto è stato sviluppato in una
lunga conversazione, e senza questo contesto è facile "correggere"
cose che sono scelte deliberate, o ritentare strade già rivelatesi
senza uscita. Ambiente di test: **Samsung Galaxy S25 Ultra, tastiera
Samsung** (rilevante: vedi sotto).

### Dove vive il progetto (dal 24/09/2026)

**La copia di riferimento è il repository GitHub
`AbernathyXVII/MyNotesApp`**, non più la cartella
`Downloads\NotionLocal` sul PC dell'utente. Quella cartella non è
collegata a GitHub e da quel giorno può essere più vecchia del
repository: per riprendere il lavoro sul PC **si clona il repository**
(vedi "Come aprirla") e si lavora lì.

Il progetto ora si sviluppa in **due modi alternati**:

- **Claude Code sul PC**, col telefono collegato via adb: si compila,
  si installa, si legge `logcat`, si prova tutto sul dispositivo. È il
  modo in cui è nato tutto quello che c'è in "Cronologia".
- **Sessioni cloud** (claude.ai/code), quando il limite settimanale di
  Code sul PC è esaurito. Girano su un server remoto che **non vede né
  il PC né il telefono**: si può scrivere il codice, non installarlo né
  provarlo. Compilarlo si può **solo se l'ambiente cloud lascia passare
  `dl.google.com`** (SDK e librerie AndroidX arrivano da lì): il
  24/09/2026 all'inizio era bloccato, poi l'utente l'ha aggiunto ai
  domini consentiti e da lì si è compilato. Come si fa: "Compilare in
  una sessione cloud", qui sotto.

### Compilare in una sessione cloud

Il server cloud riparte da zero a ogni sessione: SDK e Gradle vanno
rimessi ogni volta (circa cinque minuti). Serve che l'ambiente lasci
passare `dl.google.com` e `maven.google.com` (menu della sessione →
"Edit cloud environment" → Network access). Poi:

```
# SDK: cmdline-tools dall'indirizzo che dà
# https://dl.google.com/android/repository/repository2-3.xml
# (voce "cmdline-tools;latest", controllare lo sha1), scompattati in
# /opt/android-sdk/cmdline-tools/latest, quindi:
yes | sdkmanager --sdk_root=/opt/android-sdk --licenses
sdkmanager --sdk_root=/opt/android-sdk "platforms;android-35" "build-tools;34.0.0" "platform-tools"

# Gradle: la versione del progetto (gradle/wrapper/gradle-wrapper.properties,
# oggi 8.7) da https://services.gradle.org/distributions/, con lo sha256.
# Il progetto non ha gradlew: si usa quello scaricato.
ANDROID_HOME=/opt/android-sdk gradle --no-daemon --max-workers=2 \
  -Dorg.gradle.internal.repository.max.retries=8 \
  -Dorg.gradle.internal.repository.initial.backoff=3000 assembleDebug
```

**Trappola di Maven Central**: dal server cloud `repo.maven.apache.org`
risponde spesso **429 (troppe richieste)**, e dal 24/09/2026
pomeriggio anche `repo1.maven.org`. Si aggira con uno script in
`~/.gradle/init.d/` (fuori dal progetto, non va nel repository) che
sostituisce quegli indirizzi con il **mirror di Maven Central su Google
Cloud Storage**, `https://maven-central.storage-download.googleapis.com/maven2/`
(stessi file, e non ha mai risposto 429), e che passa lo stesso
indirizzo ai test (`systemProperty 'robolectric.dependency.repo.url'`
per ogni task `Test`: Robolectric scarica da sé il suo Android); e con i
parametri qui sopra (meno download in parallelo, più tentativi con
attesa). Ogni giro tiene in cache quello che ha già scaricato.

**I test** (`testDebugUnitTest`) girano anche lì, compresi quelli del
repository su un database vero (Robolectric, `PageRepositoryTest`): sono
l'unico modo di provare in cloud le operazioni che toccano i dati. La
prima volta Robolectric scarica circa 150 MB.

**Guardare una schermata senza telefono** *(dal 24/09/2026)*:
`WidgetsRenderTest` disegna i widget della barra laterale con
Robolectric e salva le immagini in `app/build/widget-screenshots/`, che
poi si aprono e si guardano. Lo stesso schema (`@GraphicsMode(NATIVE)`,
la finestra disegnata a mano su una bitmap, perché `captureToImage` lì
non finisce mai) serve per qualsiasi altro pezzo di interfaccia. Ha già
scovato un errore di impaginazione prima del telefono.

**L'APK che esce da lì non va installato sopra l'app del telefono**:
è firmato con una chiave di debug nata sul server, diversa da quella
del PC, e Android costringerebbe a disinstallare l'app — note comprese. Ogni modifica fatta lì è segnata **"(da
  verificare sul telefono)"** e il **"Diario delle sessioni cloud"**
  più in basso elenca, sessione per sessione, le richieste dell'utente
  parola per parola, cosa è stato fatto e cosa va provato. **Chi
  riprende sul PC dopo una sessione cloud parte da lì**: prima si
  provano sul telefono le voci ancora da verificare, poi si va avanti.

### ⛔ Il confine: da qui comincia il lavoro fatto in cloud

**Ultimo stato provato sul telefono** = il codice come stava nella
cartella `Downloads\NotionLocal` del PC il **24/09/2026**, caricato su
GitHub così com'era:

- archivio originale: commit `83f7b61` ("Add files via upload") sul
  ramo `main` — non cambia più, è la prova di com'era;
- stesso codice estratto nel repository: commit `4a35c64` ("Importa il
  progetto notE (NotionLocal) dall'archivio caricato"), identico
  all'archivio salvo gli a-capo.

**Tutto quello che viene dopo `4a35c64` è lavoro cloud: scritto e
compilato (per la sessione del 24/09/2026: vedi Diario), MAI installato
né provato sul telefono.** Va
trattato come una bozza finché non passa la procedura qui sotto. Per
vedere in un colpo solo tutte le modifiche al codice fatte in cloud:
`git diff 4a35c64 HEAD -- app/` (se quel commit non si trova più,
per esempio dopo un "Squash and merge", si confronta col contenuto
dell'archivio in `83f7b61`).

**Aggiornamento del 24/09/2026 sera — ripresa sul PC.** Il lavoro cloud
è stato **provato sul telefono** con la build del PC: le migrazioni
24→29 su un database vero, e le voci "Da verificare" in buona parte
(esito voce per voce nel Diario, sopra l'elenco; i tre bug trovati e
corretti in Cronologia, "Ripresa sul PC del 24/09/2026"). Sul telefono
ora c'è la build **firmata con la chiave di debug del PC** (non più
quella del cloud: notE non era installata), con note di prova: le
prossime installazioni dal PC la aggiornano senza chiedere di
disinstallare. Restano da provare le voci segnate 🟡 e ⏸️.

**Dal 24/09/2026 (richiesta 38) sul telefono può esserci l'APK fatto in
cloud** (commit `0557c35`), installato dall'utente dopo aver
disinstallato notE, con le sole note di prova. È firmato con una chiave
di debug nata sul server cloud (e ogni sessione cloud ne crea una nuova):
**la prossima installazione da Android Studio sul PC chiederà di nuovo
di disinstallare**, e le note sul telefono andranno perse — prima si
copia il database (vedi "Nota tecnica") se dentro c'è qualcosa che
serve. Da lì in poi la chiave torna quella del PC e le installazioni
successive dal PC tengono le note.

### Ripresa sul PC dopo il cloud — procedura obbligatoria

Richiesta esplicita dell'utente: il lavoro cloud **va ricontrollato
alla perfezione sul telefono** prima di costruirci sopra.

1. **Clonare il repository** in una cartella nuova (vedi "Come
   aprirla") e sincronizzare Gradle con la JDK 21.
2. **Collegare il Galaxy S25 Ultra** col debug USB o il debug wireless
   e controllare che `adb devices` lo veda.
3. **Copiare il database dal telefono PRIMA di installare** (comandi
   e trappole in "Nota tecnica": su Windows passa da `cmd /c`, servono
   anche `-wal` e `-shm`). Obbligatorio se il lavoro cloud ha cambiato
   lo schema; conviene sempre, perché lì dentro ci sono note vere.
4. **Installare da Android Studio (▶ Run)**. La build firmata con la
   chiave di debug del PC si installa sopra l'app esistente e **le note
   restano**. Un APK compilato altrove (per esempio in cloud) ha una
   firma diversa, Android costringe a disinstallare e **le note si
   perdono**: non farlo.
5. **Se lo schema è cambiato**, verificare che la migrazione sia
   avvenuta davvero (`user_version` e colonna in fondo al
   `CREATE TABLE`, vedi "Nota tecnica").
6. **Provare una per una, sul telefono e con `logcat` aperto, tutte le
   voci "Da verificare sul telefono"** del Diario delle sessioni cloud,
   dalla più vecchia alla più recente. Rileggere anche il diff del
   punto sopra con occhio critico: chi l'ha scritto non ha potuto
   vederlo girare. Massima attenzione a `MergedTextRunField`,
   `updateRun`, alla mappatura delle posizioni e alle migrazioni —
   sono i punti dove un errore arriva ai dati (vedi "Strade già
   tentate").
7. **Aggiornare il README con l'esito**: accanto a ogni voce provata
   "(verificato sul telefono il gg/mm/aaaa)", e ogni problema trovato
   diventa un `[Bug fix]` in Cronologia.
8. Solo a quel punto si riprende con lavoro nuovo.

### Decisioni di design deliberate (non sono bug)

**I "blocchi di testo scorrevole" sono uniti in un campo condiviso.**
Paragrafi, titoli e elementi di lista consecutivi vivono in UN SOLO
`BasicTextField` (vedi `MergedTextRunField`), non uno per blocco. Sono
righe separate nel database, ma un unico campo nell'editor. Questa è
la scelta architetturale più importante del progetto ed è stata presa
DOPO aver provato l'alternativa: con un campo per blocco, selezionare
testo a cavallo di due blocchi e usare backspace per unirli sono
impossibili, perché sono due campi che non si parlano. Non è un
problema risolvibile con una correzione mirata — ci sono stati diversi
tentativi falliti prima di capirlo.

**Checkbox, toggle, tabelle, divisori, link sono "isole".** Restano
campi separati, fuori dal flusso di testo continuo, gestiti da
`BlockRow`. È deliberato: un elemento interattivo (una casella
cliccabile, un toggle comprimibile) non si "scrive" dentro un flusso
di testo con la stessa naturalezza. La scelta è stata discussa
esplicitamente con l'utente, che ha preferito questa via alla
complessità di integrarli nel testo continuo.

**Il campo di testo unito comincia con un a-capo in più, nascosto, e
serve.** `RUN_LEAD`: il testo di `MergedTextRunField` è sempre `"\n"` +
una riga per blocco. Quell'a-capo non appartiene a nessun blocco, non
viene mostrato (la mappatura delle posizioni lo salta) e non viene
salvato. Esiste per un solo motivo: dare al backspace premuto a inizio
della PRIMA riga un carattere da cancellare, così quel gesto produce un
cambiamento di testo. Senza, il testo resterebbe identico e non ci
sarebbe nulla da osservare, dato che gli eventi-tasto non arrivano.
**Non rimuoverlo pensando che sia un residuo**, e se lo si sostituisce,
sostituirlo solo con un altro separatore di parole — un carattere che
non spezza le parole viene inghiottito dalla tastiera (vedi "Strade già
tentate"). Conseguenza pratica: il cursore a posizione zero è
impossibile per qualsiasi altra via, ed è proprio ciò che rende il
riconoscimento del gesto inequivocabile.

**Il rientro (Tab) del testo scorrevole è un livello visivo, non
nidificazione vera.** `BlockEntity.indentLevel` è un numero, come Tab
in Word. Per le isole (toggle) resta invece la nidificazione vera via
`parentBlockId`. Semplificazione deliberata, conseguenza del punto
sopra.

### Strade già tentate che NON funzionano

**Dare spazio a una riga sola dentro il campo di testo condiviso,
toccando la trasformazione visiva.** Provate due strade: uno
`ParagraphStyle(lineHeight = ...)` sulle righe con la casella, e un
carattere invisibile ingrandito in fondo al marcatore. In tutti e due i
casi il campo ha **smesso di disegnare**: righe vuote sullo schermo,
nessun errore in `logcat`, nessun crash. E il guaio vero viene dopo —
quella vuotezza è stata presa per una modifica dell'utente e **scritta
nel database**, cancellando il testo dei blocchi e trasformando le
caselle in paragrafi. Lo spazio si mette invece come `lineHeight` nel
`textStyle` **dell'intero campo** (vedi `CHECKBOX_LINE_HEIGHT`), che non
tocca né il testo trasformato né la traduzione delle posizioni.

Lezione più larga: il campo condiviso **riscrive i blocchi a partire da
quello che ha in mano**. Se la trasformazione sbaglia, il danno non
resta sullo schermo, arriva ai dati. Qualsiasi modifica lì dentro va
provata con il database sotto controllo, non solo guardando la pagina.

**Avviare l'app con `monkey` per provarla via adb.**
`adb shell monkey -p com.gabriele.notionlocal -c android.intent.category.LAUNCHER 1`
è l'idioma diffuso per "apri l'app", ma quel numero finale è il
**conteggio di eventi casuali da iniettare**, non un numero di
ripetizioni: ogni avvio spara un tocco o un tasto a caso dentro l'app.
Il 22/09/2026 quattro avvii così hanno fuso cinque caselle della pagina
radice in una sola e l'hanno svuotata — perdita di dati vera, che
sembrava un difetto dell'app. Si avvia con
`adb shell am start -n com.gabriele.notionlocal/.MainActivity`.
Vale anche la regola generale: **guidare l'app con tocchi alla cieca a
coordinate fisse**, senza uno screenshot di controllo fra un passaggio e
l'altro, fa danni sui dati veri. Prima si copia il database.

**Intercettare il tasto Backspace via `onPreviewKeyEvent` o
`onKeyEvent`.** Provato due volte, in entrambe le fasi (tunnel e
bubble). Sulla tastiera Samsung non arriva alcun evento rilevabile per
"backspace su campo vuoto" — la tastiera comunica in un modo che salta
del tutto il sistema di eventi-tasto di Compose. Peggio: la semplice
presenza di quell'intercettazione sembrava disturbare l'input normale.
Rimosso del tutto. Oggi backspace-tra-blocchi funziona perché è una
CONSEGUENZA del testo che cambia nel campo unito (vedi
`PageEditorViewModel.updateRun`, ramo "meno righe che blocchi"), non
perché qualcuno intercetta un tasto.

**Usare uno ZERO-WIDTH SPACE come carattere nascosto in testa al campo
di testo unito.** Tentato il 2026-09-18 per risolvere "backspace non
toglie il marcatore al primo elemento". L'idea di fondo era giusta e
oggi è in uso (vedi `RUN_LEAD` sopra), ma **il carattere scelto era
sbagliato**: lo zero-width space non spezza le parole, quindi la
tastiera Samsung lo ingloba nella parola che sta componendo e lo
sostituisce insieme al resto ad ogni tasto. Il codice se ne accorgeva e
rimetteva il testo a posto, cioè litigava con la tastiera ad ogni
carattere, azzerando la regione di composizione. Sintomi osservati sul
dispositivo: primo Invio ignorato (il secondo funzionava), elenchi che
non continuavano, backspace che cancellava molto più del dovuto —
esattamente il disturbo all'input già visto con l'intercettazione dei
tasti. Sostituito con un a-capo, che le parole le spezza sempre.
**Morale: qualunque carattere nascosto si usi lì dentro deve essere un
separatore di parole, altrimenti l'IME se lo mangia.**

**Capire "quale riga è cambiata" dal solo confronto fra prima e dopo.**
Non basta, e il caso che lo dimostra è banale: **una riga vuota
inserita in mezzo ad altre righe vuote è identica a tutte le sue
vicine.** Il confronto allora attribuiva l'inserimento all'ultima riga
del gruppo, e la riga nuova ereditava il tipo dal blocco lì in fondo
invece che da quello su cui si stava scrivendo. Sintomo: `0.` (o
qualsiasi numero) creava la lista, ma l'Invio dopo dava un paragrafo e
la lista si fermava — e solo su pagine che sotto avevano righe vuote,
che è la ragione per cui a volte si presentava e a volte no. Ora il
campo passa anche **dov'è il cursore** (`caretLine`): la modifica non
può essere dopo di lui, e l'ambiguità sparisce.

**Attenzione alla diagnosi comoda.** Lo stesso sintomo era stato
attribuito prima al ritardo del salvataggio (il database indietro di
qualche decina di millisecondi rispetto al campo). Era una spiegazione
plausibile, coerente col fatto che pilotando l'app da adb non si
riproduceva — e sbagliata: la correzione fatta su quella base non ha
cambiato niente, e solo il log ha mostrato la vera causa (l'Invio
finiva `mid=25` quando il cursore era alla riga 20). Il confronto con
il testo del campo (`linesBefore`) è rimasto perché è comunque più
corretto del confronto col testo salvato, ma **non era quello il
problema**.

**Fidarsi della lista di blocchi che la UI passa al ViewModel.** La
UI può essere indietro di un istante (es. si scrive subito dopo un
Invio, prima della ricomposizione). Il codice originale scartava la
modifica in silenzio in quel caso — causa profonda di una serie di
sintomi apparentemente scollegati (Invio da premere due volte, liste
che non continuavano, tipo di blocco ereditato a caso). `updateRun`
ora ricostruisce il gruppo reale dallo stato corrente. **Se si
toccano `updateRun` o `MergedTextRunField`, non reintrodurre
l'assunzione che la lista ricevuta sia aggiornata.**

**Risincronizzare il campo col database ad ogni emissione.** Faceva
sovrascrivere l'a capo appena digitato quando il salvataggio era
ancora in viaggio. Ora `MergedTextRunField` tiene traccia di
`pendingText` per distinguere "database indietro" da "contenuto
cambiato davvero da fuori" (Annulla/Ripristina, segnalato da
`externalChangeTick`).

**Impostare `pendingText` quando non è partito nessun salvataggio.**
`onValueChange` scatta anche quando si sposta solo il cursore o si
seleziona. Se lì si mette `pendingText`, dal database non torna mai
niente che lo azzeri, e da quel momento il campo ignora ogni
cambiamento fatto **da fuori** (menu del blocco: Duplicate, Turn into):
mostra le righe vecchie, e alla prima lettera `updateRun` confronta
righe e blocchi che non combaciano e **cancella o sposta blocchi**
(trovato sul telefono il 24/09/2026: sparita la copia appena
duplicata). `pendingText` si mette **solo se il testo è cambiato**. Chi
aggiunge un'azione che cambia i blocchi di un gruppo da fuori deve
chiedersi che cosa vedrà il campo quando arriva il nuovo stato.

**Rimettere una foto dei blocchi nell'ordine di `orderIndex`.**
`orderIndex` riparte da 0 dentro ogni toggle, quindi in una lista
ordinata solo per lui un figlio può venire prima del suo genitore; se
la si reinserisce così, la chiave esterna verso il genitore la rifiuta
e l'app si chiude (Annulla, fino al 24/09/2026). Per reinserire blocchi
si usa `inDocumentOrder`: genitori prima, figli subito dopo.

**Salvare i blocchi uno alla volta quando la modifica ne tocca
tanti.** Room avvisa chi osserva la tabella **ad ogni scrittura**:
rinumerando i venti blocchi sotto a quello diviso, un solo Invio
faceva ridisegnare la pagina venti volte di fila. Dentro quella
tempesta il campo su cui si sta scrivendo viene ricomposto di
continuo, il fuoco non arriva al blocco nuovo e le lettere successive
finiscono in quello vecchio. Le modifiche che toccano più blocchi
vanno in **una transazione sola** (`PageRepository.applyBlockChanges`),
e la rinumerazione è **una query sola**
(`BlockDao.shiftOrderIndexes`), non una riga alla volta: così l'avviso
è uno, e i numeri li calcola il database invece della copia in memoria
della UI — che due Invii ravvicinati rendevano vecchia, lasciando due
blocchi con lo stesso numero.

**Dare il fuoco a una riga che non si vede.** `LazyColumn` costruisce
solo le righe dentro lo schermo: il blocco nato premendo Invio in
fondo alla parte visibile spuntava dietro la tastiera, non veniva mai
costruito, e una riga che non esiste non può prendersi il cursore.
Verificato col log: la richiesta di fuoco partiva, e fra le righe
composte quella nuova non c'era. Prima del fuoco bisogna **portare la
riga dentro lo schermo** (`animateScrollToItem`); costruita quella, il
fuoco la trova.

**Tenere la pagina in una variabile letta una volta sola, e salvarla
intera ad ogni modifica.** È come stavano `DatabaseViewModel` e
`PageEditorViewModel`, ed è una perdita di dati silenziosa: la stessa
pagina può stare aperta in due schermate insieme (un database dentro
una pagina e lo stesso database a schermo intero, o la stessa pagina
due volte nella pila di navigazione), ognuna col suo ViewModel. Quella
rimasta indietro non mostra solo un titolo vecchio: alla prima
modifica — anche di tutt'altro campo, perché si scrive la riga intera
— lo rimette nel database, e il nome nuovo sparisce. **Se si aggiunge
un ViewModel che tiene una pagina, la si segue con
`PageRepository.observePage`, non con `getPage`.** E il campo di testo
del titolo tiene una copia locale finché ha il fuoco: il valore che
torna dal database arriva un attimo dopo e, se nel frattempo è stato
battuto un altro tasto, rimetterebbe il testo indietro mangiandosi il
carattere (è la stessa trappola del `pendingText` qui sopra).

**Intercettare il tasto indietro con un `BackHandler` mentre la
tastiera è aperta.** Non arriva. Con la tastiera a schermo il tasto (o
il gesto) indietro lo riceve **per prima la tastiera**, che se lo mangia
per chiudersi, e all'app non arriva niente: il `BackHandler` non viene
mai chiamato. Verificato col log — l'evento di fuoco arrivava
(`hasFocus=true`), il gesto no. Serve solo per il caso in cui il cursore
è acceso ma la tastiera è già chiusa. Se quello che si vuole è reagire
al "l'utente ha finito di scrivere", il segnale giusto è **la tastiera
che si chiude** (`WindowInsets.isImeVisible`), non il gesto.

### Misurare la fluidità, non guardarla

Lo scatto nello scorrimento della settimana è costato **tre correzioni
sbagliate di fila**, tutte plausibili e tutte inutili, perché il modo di
verificare era sbagliato: `adb shell input swipe` produce un
trascinamento perfettamente lineare e senza spinta, molto più gentile di
un dito vero, e le misure sui fotogrammi risultavano pulite mentre
l'utente continuava a vedere lo scatto.

Gli strumenti che invece dicono la verità:

```
adb shell dumpsys gfxinfo com.gabriele.notionlocal reset
(fai i gesti)
adb shell dumpsys gfxinfo com.gabriele.notionlocal
```

Conta i fotogrammi persi davvero. Sul calendario a settembre 2026 dava
**3,6% di fotogrammi persi e 99° percentile a 23 ms**: su uno schermo a
120 Hz il budget è 8 ms, quindi lo "scatto" non era un errore di logica
ma lavoro di troppo per fotogramma.

Per vedere *dove* si muove il contenuto, la correlazione incrociata fra
fotogrammi successivi (non il centroide dei pixel chiari, che si sposta
da solo appena entra contenuto nuovo):

```
adb shell screenrecord --time-limit 4 /sdcard/r.mp4
ffmpeg -i r.mp4 -vsync 0 -vf "crop=1440:46:0:1488,format=gray" -f rawvideo strip.raw
```

e poi si cerca lo spostamento che massimizza la sovrapposizione dei
profili di colonna. Uno scorrimento sano dà una serie monotona che
decelera; uno scatto dà un fotogramma fermo seguito da un passo doppio.

**Attenzione a due trappole**: `screenrecord` chiude il file riemettendo
un fotogramma vecchio, quindi l'ultimo fotogramma mostra sempre la
schermata di partenza (artefatto, non l'app); e una build di debug di
Compose perde più fotogrammi di una release — qui 6,2% contro 3,2%,
misurato firmando la release con la chiave di debug.

### Peculiarità dell'ambiente di test

- La **tastiera Samsung** ha un comportamento documentato e
  problematico con Compose: caratteri che si duplicano quando il campo
  attivo cambia troppo a ridosso di un tasto premuto. Mitigato con
  `IME_SETTLE_DELAY_MS` (60ms) prima di ogni spostamento di focus in
  `PageEditorViewModel`. Non è garantito che risolva in ogni caso — la
  causa vive nella tastiera, non nel codice.
- `keyboardController?.show()` va chiamato **solo** se la tastiera non
  è già visibile: chiamarlo a tastiera aperta disturba l'input.
- **`Modifier.clickable` non risponde solo al tocco**: si attiva anche
  col tasto Invio quando un elemento interno ha il focus. Su un
  contenitore che racchiude campi di testo è una trappola — usare
  `detectTapGestures`. Ha già causato un bug (vedi cronologia)
- Il cursore di `BasicTextField` è **nero di default** — invisibile su
  sfondo scuro finché non si imposta `cursorBrush` esplicitamente.

### Cosa l'utente ha chiesto di tenere per ultimo

- **Selezione di testo che scavalca le isole** (es. dal testo dentro
  un toggle al testo fuori). Discusso: richiederebbe di unire anche i
  contenuti dei toggle nel campo condiviso. L'utente ha accettato di
  rimandarlo a fine progetto.
- **Evidenziazione di selezione stretta sul testo** invece che allargata
  a tutta la larghezza (Notion fa così, Android di default no).
  Verificato fattibile (disegnando l'evidenziazione a mano da
  `TextLayoutResult`), ma è puramente estetico e l'utente ha chiesto
  di metterlo in fondo alla lista.

### Regola di lavoro concordata

**Aggiornare questo README ad ogni modifica dell'app**, anche se non
verificata sul dispositivo, etichettando ogni voce come
`[Nuova funzionalità]` o `[Bug fix]`.

Ribadita ed estesa dall'utente il 24/09/2026: **va scritta filo per
filo e per segno QUALSIASI cosa** — ogni modifica, ogni aggiunta, ogni
cosa tolta, ogni bug trovato (anche se non ancora corretto), e anche i
cambiamenti che non toccano l'app (configurazione, repository, questo
stesso README). Per questi ultimi si usano due etichette in più:
`[Rimosso]` per ciò che viene tolto dall'app e `[Progetto]` per ciò che
sta attorno al codice. Nelle sessioni cloud si trascrivono anche **le
richieste dell'utente** (vedi "Diario delle sessioni cloud"). Lo scopo
dichiarato: tornando su Claude Code non si deve ricominciare niente da
capo.

**Dal 30/09/2026 ogni voce della Cronologia è una versione** (chiesto
dall'utente, vedi v0.131 e v0.134): il titolo è `### 🆕 v0.N —
gg/mm/aaaa · hh:mm — Titolo`, con 🆕 e il numero dopo per una richiesta
che porta qualcosa di nuovo o toglie qualcosa, 🔧 e un decimale in più
(v0.N.1, v0.N.2…) per una che corregge soltanto. Il giorno e l'ora sono quelli (italiani)
del commit che chiude la voce; la voce nuova va in cima. La Cronologia è
anche **Settings → Patch Notes** nell'app, che la copia a ogni
compilazione, e `PatchNotesTest` controlla che numeri, date e ordine
tornino.

---

## Come aprirla

1. Installa **Android Studio**: https://developer.android.com/studio
2. Prendi il progetto da GitHub: `File → New → Project from Version
   Control`, URL `https://github.com/AbernathyXVII/MyNotesApp`, e
   scegli una cartella **nuova** (non la vecchia
   `Downloads\NotionLocal`, che non è collegata al repository). Se il
   progetto è già stato clonato, basta aprirne la cartella con
   `File → Open` e aggiornarla con `Git → Pull`.
   **Il ramo giusto**: il lavoro cloud sta su
   `claude/funny-ramanujan-uuwahs` finché la sua pull request non è
   unita in `main`. Se non lo è, dopo aver clonato si passa a quel ramo
   (`git checkout claude/funny-ramanujan-uuwahs`, o in basso a destra in
   Android Studio) — su `main` ci sarebbe il codice di prima del cloud
3. Se richiesto, seleziona una JDK 21 per Gradle (non la più recente
   disponibile — Gradle 8.7 supporta fino alla 21). Va rifatto ad ogni
   estrazione fresca del progetto: l'impostazione vive in `.idea/`
4. Aspetta la sincronizzazione Gradle (la prima volta scarica le
   dipendenze — serve connessione internet)
5. Collega il telefono con debug USB attivo, premi ▶ Run

**Vincolo di versione da non toccare**: `kotlinx-serialization-json` è
fissata a **1.6.3**. Dalla 1.7.0 richiede Kotlin 2.0+, mentre il
progetto è su Kotlin 1.9.24 in tutta la configurazione.

## Cosa fa l'app oggi

- **Un'unica pagina radice** ("My Space", titolo modificabile) fa da
  Home — non esiste una schermata Home separata: si comporta come
  qualsiasi altra pagina, con lo stesso editor
- **Testo scorrevole unificato**: paragrafi, elenchi puntati e
  numerati e caselle consecutivi vivono in un unico campo di testo
  condiviso — selezione e backspace-tra-blocchi funzionano in modo
  nativo, come in un editor di testo normale
- **Isole separate**: checkbox, toggle annidabili, tabelle semplici
  ridimensionabili, divisori, blocchi Page/Database (creano una
  sottopagina o un database e ci navigano dentro subito, come in
  Notion)
- **Backspace a inizio riga** smonta la riga un gradino alla volta:
  prima toglie il marcatore di elenco, poi i rientri uno per uno, e
  solo quando non resta niente fonde con la riga sopra — come in
  Notion, anche sul primo elemento in cima alla pagina
- **Scrivere `1.` a inizio blocco** crea un elenco numerato; scrivendo
  un altro numero (`5.`, e anche `0.`) l'elenco parte da lì e prosegue
  normalmente. Solo se il numero è l'unica cosa scritta nel blocco
- **Invio** crea un nuovo blocco ereditando il tipo se è un elenco;
  **incollare testo multi-riga** separa correttamente in più blocchi
- **Rientro (Tab)** per il testo scorrevole è un livello visivo sul
  paragrafo; per le isole (toggle) è nidificazione vera. Sugli elenchi
  numerati il livello di rientro decide anche la numerazione: `1.`,
  `1.1`, `1.1.1`
- **Formattazione inline ("Aa")**: grassetto, corsivo, sottolineato,
  barrato, spoiler e *(sessione cloud del 24/09/2026, provato sul
  telefono il 24/09/2026)* pedice e apice — funziona sia nel testo unito (la selezione, anche se
  attraversa più blocchi, viene tradotta per ciascuno) sia nelle isole
- **Undo/Redo** per testo e struttura dei blocchi (non celle di
  tabelle/database), con la digitazione continua raggruppata in un
  unico passo
- **Numerazione delle liste** su più livelli (`1.`, `1.1`, `1.1.1`)
  guidata dal rientro; **tenendo premuto sul numero** si apre il menu
  della lista — far ripartire il conteggio da lì, rimetterlo in fila,
  togliere il numero, passare al pallino — come su OneNote
- **Indenta/disindenta, sposta su/giù, Undo/Redo, menu "+", Aa** dalla
  barra sopra la tastiera (scorrevole orizzontalmente)
- **Il menu del blocco** *(sessione cloud del 24/09/2026, provato in
  parte sul telefono il 24/09/2026)*: i **sei puntini** all'inizio della barra aprono il menu
  della riga in cui sta il cursore — Turn into (le voci del "/"), Color
  su tutta la riga, Duplicate. **Tenendo premuto il collegamento a una
  pagina** si apre quello della pagina: Color, Edit icon, preferiti,
  Rename, Duplicate, Move to, Move to trash. Per un database (sei puntini
  nella sua barra, o collegamento tenuto premuto) in cima anche Turn into
  page, Turn into simple / complex database, Lock database, Open as page
- **Tocco ovunque sotto il titolo** per iniziare/continuare a scrivere
- **Database semplice** *(sessione cloud del 24/09/2026, provato in
  parte sul telefono il 24/09/2026)*: "Simple database" nei menu "/" e "+" crea un database
  con tutto quello che hanno gli altri — viste, filtri, ordinamento,
  raggruppamento, proprietà — ma **le cui righe sono solo testo e non
  diventano mai pagine**: niente OPEN, niente icone o copertine di riga;
  toccando una riga si apre la sua scheda, col nome da scrivere e tutte
  le proprietà. Una cosa che Notion non ha
- **Ricerca dentro un database** *(sessione cloud del 24/09/2026,
  provata in parte sul telefono il 24/09/2026)*: la lente accanto a Sort apre lì una barra;
  scrivendo restano le righe che hanno la parola nel nome, in una
  proprietà o dentro la loro pagina, con il nome illuminato di giallo se
  è lì, o tutta la riga se la parola è dentro. In tutte e sei le viste
- **Viste collegate** *(sessione cloud del 24/09/2026, provate in parte
  sul telefono il 24/09/2026)*: "Linked view of data source" nei menu "/" e "+" mette in
  una pagina **lo stesso database** che sta in un'altra — righe,
  proprietà, pagine e quello che c'è scritto dentro sono gli stessi — con
  filtro, ordinamento, raggruppamento, vista e proprietà nascoste suoi.
  Il titolo "↗ Nome" porta al database principale
- **Widget in cima alla barra laterale** *(sessione cloud del
  24/09/2026, provati in parte sul telefono il 24/09/2026)*: fusi orari (orologi
  analogici o digitali, e un elenco sempre da ovest a est), avanzamento
  di anno, mese, settimana, giorno e date proprie, contatore, pomodoro
  con sessione e pausa (e dal 25/09/2026 il suo suono, scelto fra le
  suonerie del telefono), e dal 25/09/2026 **cronometro** coi giri e
  **timer** (anche più d'uno insieme), come quelli dell'orologio Samsung;
  i timer aggiunti col "+", dal secondo in poi, si eliminano col cestino
  in alto a destra, il primo resta sempre.
  Una freccetta nasconde tutta la sezione, e la scelta resta
- **Database dentro le pagine**: creandone uno dal menu "+" compare
  nella pagina e si modifica lì; l'icona con le frecce lo apre a
  schermo intero, e dalle impostazioni lo si elimina
- **Database** con la schermata modellata sulla UI mobile di Notion:
  titolo modificabile, tabella scorrevole, `+ Add property` in coda
  alle colonne, `OPEN` per aprire una riga come pagina vera,
  `+ New page` in fondo. Toccando un'intestazione si apre la finestra
  della proprietà (nome, tipo con ricerca, elimina; dal 24/09/2026 non
  più "sposta", che sta nel menu dell'intestazione tenuta premuta). Dodici
  tipi: Text, Number, Select, Multi-select, Date, Checkbox, **Color cell**
  (dal 26/09/2026: la cella piena di un colore, con sopra del testo se si
  vuole), URL, Email, Phone, Created time, Last edited time
- **Sei viste per i database** — Table, List, Board, **Calendar**,
  **Timeline** e **Gallery**, una per volta, scelte da Settings → Layout
  e ricordate per ogni database
- **Gallery** *(sessione cloud del 24/09/2026, provata in parte sul
  telefono il 24/09/2026: mancano le copertine)*: le pagine come schede in griglia, con in alto la
  **copertina** della pagina (con l'inquadratura scelta in
  "Reposition") oppure **l'inizio del suo testo**, scritto come nella
  pagina. Sotto, nome e proprietà. Dalle impostazioni della vista si
  sceglie **Card preview** (None / Page cover / Page content) e **Card
  size** (Small / Medium / Large: tre, due o una scheda per riga su un
  telefono in verticale). Si crea anche dal menu "/" con "Gallery view".
  Dal 25/09/2026, tenendo premuta una scheda, **"Reposition cover"**
  sposta e ingrandisce la copertina nella sola scheda
- **Data con ora facoltativa**: l'interruttore "Include time" nella
  finestra della data aggiunge l'orario all'inizio e alla fine. Spento,
  la data vale il giorno intero. **Selettori come quelli di Samsung**
  (dal 25/09/2026): il mese a griglia con le frecce e lo scorrimento
  orizzontale, toccando il mese le rotelle di giorno, mese e anno; l'ora a
  rotelle, un minuto alla volta. La data di fine si chiede con
  l'interruttore "End date"
- **Disegno a mano** (dal 25/09/2026): "Draw" nei tre puntini di una
  pagina. Penna, matita, pennarello ed evidenziatore, con spessore e
  colore; gomma per tratti o a punto; Annulla e Ripeti. La S Pen (con la
  pressione, e il tasto laterale per cancellare) e un dito disegnano, due
  dita scorrono; il palmo appoggiato mentre si usa la penna non lascia
  segni. I disegni seguono le righe, il database o il titolo su cui li si è
  fatti
- **Notifiche** (dal 25/09/2026): la riga **`/notification 22.10.2026
  19:41 Buttare l'immondizia`** (Invio, e diventa una riga con la
  campanella; senza ora alle 9:00; senza testo mostra il nome della
  pagina), le date dei database con **"Include notification"** (col nome
  della pagina della riga, all'inizio e alla fine), e la fine di timer e
  fasi del pomodoro. Arrivano anche ad app chiusa, e toccandole si apre la
  pagina
- **Quaderni come in OneNote** (dal 26/09/2026): "Notebooks" nella barra
  laterale, con quaderni, gruppi di sezioni, sezioni colorate e pagine che
  si allungano man mano che si scrive, col giorno e l'ora sotto il titolo.
  Un mondo a parte dal menu principale: non si cercano da lì e non si
  collegano alle sue pagine
- **Pagine e database bloccati** (dal 26/09/2026): "Lock with password"
  nei tre puntini; si aprono con l'impronta o con la password, scelta la
  prima volta e cambiata o tolta da Settings. Chiudono anche quello che
  hanno dentro; un database bloccato dentro una pagina diventa un
  collegamento, e nella galleria la loro scheda mostra un lucchetto bianco
  al posto della copertina. Aperte, hanno un lucchetto chiuso accanto ai
  tre puntini
- **Timeline**: il calendario srotolato. Un asse del tempo che scorre
  in orizzontale, una riga per pagina con la barra intera (non spezzata
  a fine settimana), i nomi in una colonna ferma a sinistra, i fine
  settimana in ombra e una riga verticale sull'ora di adesso. Si apre
  già posizionata su adesso, e il pulsante **Today** ci riporta
- **Sette ingrandimenti della Timeline** — Hours, Day, Week, Bi-week,
  Month, Quarter, Year — scelti dalla stessa finestra di Notion
  ("Select a zoom level") e ricordati per ogni database
- **Calendario ad anno, mese o settimana**, con le pagine disegnate come
  **barre lunghe quanto i giorni che coprono**: una proprietà data può
  avere inizio e fine ("Date started"/"Date finished"), e la barra le
  rispetta, spezzandosi a fine settimana e restando squadrata dal lato
  dove prosegue. Toccando un giorno vuoto si crea lì una pagina, nella
  settimana si passa da una all'altra **trascinando col dito**, e
  l'anno mostra dodici mesi in miniatura con i giorni occupati cerchiati
- **Icona e copertina per ogni pagina**, radice compresa: si toccano
  per cambiarle, e l'immagine arriva dalla **galleria del telefono**
  ("Upload"), di cui l'app tiene una copia. Un PNG trasparente resta
  trasparente. (Fino al 25/09/2026 anche da un link: tolto su
  richiesta, e con lui il permesso di rete — l'app non si collega a
  niente)
- **La copertina si inquadra**: da "Reposition" la si sposta col dito
  in tutte le direzioni e la si ingrandisce pizzicando, fino a
  quattro volte. Il file non viene mai ritagliato né ridotto — nel
  database finiscono solo tre numeri, e l'immagine resta intera
- **Ordinamento delle righe** per una qualsiasi proprietà, crescente o
  decrescente, dall'icona Sort: alfabetico per le parole, numerico per i
  numeri, cronologico per le date
- **Raggruppamento della tabella** per una qualsiasi proprietà: ogni
  gruppo ha la sua intestazione col valore, il conteggio e il suo
  "+ New page", che crea la pagina già dentro quel gruppo
- **Date che si ripetono**, con le scorciatoie di Google Calendar e una
  schermata per le regole su misura. La pagina resta una sola: compare
  su tutte le date della regola e modificarla le cambia tutte
- **Filtri** sulle proprietà: un intervallo di numeri (da x a y, con un
  estremo facoltativo), i giorni di una data (uno solo o da un giorno a
  un altro), i tag di un Select o MultiSelect, e le caselle spuntate o
  non spuntate. Si aprono dall'icona a sinistra di Sort o dalla voce
  "Filter" nelle impostazioni — è la stessa finestra — e si buttano col
  cestino in alto a destra
- **Colonne nascondibili**: tenendo premuta l'intestazione di una
  proprietà compare "Hide", e in "Property visibility" (impostazioni
  della vista) ci sono le due liste *Shown in table* e *Hidden in
  table* da cui rimetterle a posto, una per volta o tutte insieme
- **Colonne che si spostano dal menu dell'intestazione**: tenendola
  premuta, **in cima al menu**, sopra "Edit property", ci sono **"Move
  to left"** e **"Move to right"**. La prima colonna dopo Name ha "Move to left" grigia —
  Name resta sempre la prima — e l'ultima ha grigia "Move to right"
  *(documentata il 24/09/2026: era nel codice ma non qui; provata sul
  telefono il 24/09/2026, vedi Cronologia)*
- **Titolo del database nascondibile** quando sta dentro una pagina,
  dalle impostazioni della vista. A schermo intero resta sempre visibile
- **Ricerca full-text** tra titoli e contenuto dei blocchi
- **Font e dimensione del testo selezionato** *(sessione cloud del
  24/09/2026 per tutta la pagina; **dal 25/09/2026 solo sul testo
  selezionato**, provato sul telefono lo stesso giorno)*: nella barra Aa,
  prima di B, la voce del font e quella della dimensione, come su
  OneNote; mostrano quelli del testo dove sta il cursore. Si seleziona un
  pezzo di testo e si sceglie: cambia **solo quel pezzo**, come il colore
  o il grassetto, e un altro pezzo può avere un altro font. Senza niente
  di selezionato dicono "Select some text first". Sedici font in tre
  gruppi — occidentali, cinesi, giapponesi — che sono **sosia liberi**
  dei font commerciali chiesti; dimensione da 5 a 72, di partenza 16,
  **scritta a mano** in una finestrella. Nelle impostazioni **non** ci
  sono più "Font" e "Font size" per tutto il Workspace (tolte il
  25/09/2026, su richiesta)
- **Niente titoli (Heading)** dal 24/09/2026: tolti dall'app su
  richiesta dell'utente, ora che la dimensione del testo si sceglie
  dalla barra Aa. Quelli che c'erano sono diventati testo normale
- **Conteggio del testo** *(sessione cloud del 24/09/2026, provato sul
  telefono il 24/09/2026)*: in fondo al menu dei tre puntini di una pagina c'è
  **"X words"**; toccandola si apre lì sotto l'elenco come nella
  finestra "Word Count" di Word — parole, lettere, numeri, lettere e
  numeri insieme, caratteri (tutti i segni, simboli compresi) senza e
  con spazi, righe (blocchi con del testo), caratteri
  giapponesi, cinesi e lettere coreane. Conta solo il testo scritto
  nella pagina: niente database, niente titolo
- **Move to** *(sessione cloud del 24/09/2026, provato in parte sul
  telefono il 24/09/2026)* dai tre puntini di una pagina o di un database, e dal menu
  del blocco: l'albero della barra laterale con la ricerca in cima, da
  cui si sceglie qualsiasi pagina, comprese quelle dei database; la
  pagina finisce **in fondo**. Un database arriva sempre come
  collegamento a pagina. Sulla pagina principale la voce è grigia
- **Duplicate** dal menu dei tre puntini, con la scelta di dove mettere
  la copia: accanto all'originale (per una riga di database, una riga
  nuova), in fondo al menu principale o dentro una pagina qualsiasi,
  comprese quelle delle righe di tutti i database. Si copia tutto,
  sottopagine e immagini comprese
- ~~**Tema scuro fisso in tutta l'app**, coi grigi di Notion: `#191919`
  per le pagine, `#202020` per le finestre dal basso, `#252525` per i
  riquadri delle opzioni. Niente tema chiaro e niente colori dinamici~~
  **Superato** (corretto il 24/09/2026): in Settings → Theme c'è
  **Dark / Light / System** (vedi la voce "Theme" in Cronologia). I
  grigi di Notion qui sopra restano quelli del tema scuro, che è ancora
  quello di partenza; niente colori dinamici

## Limiti noti

- **Quaderni** (dal 26/09/2026):
  - eliminare una pagina, una sezione o un quaderno è per sempre: il
    cestino è del menu principale, e un cestino dei quaderni non c'è
  - nei quaderni non si cerca (la ricerca è quella del menu principale,
    che le loro pagine non le vede)
  - dal menu di OneNote mancano Export, Move or Copy, Merge e Copy Link;
    le pagine non si ordinano per data o nome e non hanno sottopagine
  - la pagina è quella dell'editor, che si scrive dall'alto in basso: non
    si scrive in un punto qualsiasi del foglio come in OneNote
- **Pagine bloccate con la password** (dal 26/09/2026):
  - l'impronta c'è solo da Android 11 in su; prima, solo la password
  - una notifica ("/notification" o una data con "Include notification")
    mostra il suo testo anche se sta in una pagina bloccata
  - dimenticata la password, la si rifà col blocco schermo del telefono:
    se il telefono non ne ha uno, le pagine bloccate restano chiuse
- **Un pezzo di testo più grande allunga tutte le righe del suo
  gruppo**, non solo la sua (dal 25/09/2026, con la dimensione sul testo
  selezionato). Le righe di testo scorrevole consecutive stanno in un
  campo solo con **un'altezza di riga unica** — è quella che tiene i
  segni degli elenchi tutti allo stesso passo — e per darla riga per
  riga servirebbe uno `ParagraphStyle` per riga dentro la
  trasformazione, la strada che ha già fatto smettere di disegnare il
  campo e perdere del testo (vedi "Strade già tentate"). Così l'altezza
  del gruppo si allunga quanto serve al pezzo più grande (`lineScaleFor`),
  e torna com'era appena quel pezzo torna a 16. Un gruppo finisce dove
  c'è un blocco che non è testo scorrevole (tabella, toggle, collegamento
  a pagina, divisore…)
- **Copiare con "Seleziona tutto" da un blocco di testo unito porta con
  sé un a-capo in più** in testa (il `RUN_LEAD`, vedi sopra). Toglierlo
  richiederebbe di intercettare copia/taglia
- **Un numero decimale a inizio blocco diventa un elenco**: scrivendo
  `1.5` la conversione scatta già sul punto, prima che il `5` arrivi.
  Conseguenza diretta di aver scelto il punto come innesco invece dello
  spazio (`1. `, come fanno Notion e Word). Scelta confermata
  dall'utente: i sottolivelli si fanno col rientro, non scrivendo
  `1.1` a mano
- **Una riga rientrata che va a capo da sola** riparte dal margine
  invece di restare allineata sotto al testo. Conseguenza del rientro
  fatto con spazi nel prefisso invece che con `TextIndent`, che invece
  sapeva rientrare anche le righe di continuazione
- **Fondere due elementi di elenco richiede due backspace**: il
  primo toglie il marcatore, il secondo fonde. È il comportamento di
  Notion ed è la conseguenza diretta della scelta sopra, non una svista
- **Selezione e backspace-tra-blocchi non funzionano dentro un
  toggle**: i suoi contenuti restano sul vecchio modello a campo
  singolo per ciascuno
- ~~La selezione non attraversa il confine di una casella da
  spuntare.~~ **Risolto**: le caselle sono entrate nel campo di testo
  condiviso, quindi selezionare fra una casella e l'altra è selezionare
  del testo come in qualsiasi altro punto. Resta il limite dentro un
  *toggle*, dove le caselle figlie sono ancora righe a sé
- ~~**Colore del testo** non disponibile da nessuna parte.~~
  **Superato**: c'è il pennello accanto ad Aa (vedi "Colore del testo
  e colore dietro al testo" in Cronologia). Resta vero per i campi
  della voce qui sotto
- **Titolo pagina, celle delle tabelle semplici e celle dei database**
  sono testo completamente piatto, senza formattazione né colore:
  portarceli richiede di costruire per ciascuno l'infrastruttura già
  fatta per i blocchi
- Nei menu "+" e "/" (dal 24/09/2026 hanno le stesse voci), **Callout,
  Quote, Link to page (come voce separata da "Page"), Image, Video,
  Audio, Code, File, Web bookmark e Database - Full page** ci sono ma
  sono grigie: non sono state
  costruite — alcune richiedono capacità nuove (gestione file,
  embedding media). ~~Anche Linked view of data source.~~ **Superato**
  (24/09/2026, richiesta 36): la vista collegata c'è
- **Database: "Property visibility" non riordina e non cerca.** Su
  Notion le due liste hanno le maniglie per trascinare le colonne e un
  campo di ricerca in cima; qui l'ordine si cambia solo tenendo
  premuta l'intestazione della colonna (Move to left / Move to right),
  e la ricerca non c'è, perché con
  una manciata di proprietà sarebbe un campo di testo da riempire per
  scorrere una lista che si vede già tutta
- **Database semplice: si sceglie quando lo si crea**, e non si può
  trasformare un database normale in semplice o viceversa. Un database
  normale ha righe che magari sono già pagine piene di testo, e farle
  diventare "solo testo" vorrebbe dire decidere cosa farne
- **Database semplice: dalla ricerca si arriva al database, non alla
  riga.** Il risultato dice il nome della riga e in quale database sta,
  e aprendolo si apre il database intero: la riga va cercata lì. Nella
  barra laterale il database semplice non ha figli, perché le sue righe
  non sono pagine
- **Database: una colonna nascosta non si sposta** *(dal 24/09/2026)*.
  Spostare si fa tenendo premuta l'intestazione, e una colonna nascosta
  un'intestazione non ce l'ha; il suo "Move left / Move right" dentro la
  finestra della proprietà, che era l'altro modo, è stato tolto su
  richiesta dell'utente. Per spostarla la si rimostra (Property
  visibility), la si sposta e la si nasconde di nuovo. Lo stesso vale
  per le viste senza intestazioni (bacheca, elenco, galleria...): lì
  l'ordine delle proprietà sulle schede si cambia dalla tabella
- **Database: la visibilità è una sola per tutto il database**, non una
  per vista come su Notion: nascondendo una colonna nella tabella
  sparisce anche dalle schede della bacheca e dell'elenco. Qui un
  database ha una vista sola per volta, quindi tenerne una copia per
  ognuna vorrebbe dire salvare impostazioni che nessuno ha mai scelto
- **Database: ordinamento solo su una proprietà per volta**. Notion permette di concatenare più ordinamenti; qui il secondo criterio a parità di valore è fisso
- **Database: l'ordinamento non tocca calendario e linea del tempo**, dove le pagine stanno già in ordine di data — che è l'ordine che serve lì
- ~~**Database: la ricerca non esiste.** L'icona di Notion che sta lì
  accanto alla vista è stata deliberatamente omessa finché non
  funziona, perché un'icona che non fa niente è peggio di un'icona
  assente.~~ **Superato** (24/09/2026, richiesta 35): la lente accanto a
  Sort c'è. I filtri ci sono, su una proprietà per volta
- **Database: manca la vista Chart.** Ci sono Table, Board, List,
  Calendar, Timeline e (dal 24/09/2026) Gallery; Chart si aggiungerà
  quando disegnerà davvero qualcosa, come le altre
- **Gallery: le schede si costruiscono tutte insieme**, immagini
  comprese, come nelle altre viste: non è una griglia "pigra", perché
  il database dentro una pagina sta in una lista che scorre già e una
  griglia che scorre per conto suo lì dentro non si può mettere. Con
  qualche decina di pagine non si nota; con centinaia di copertine
  l'apertura rallenta e la memoria cresce. Ogni immagine comunque si
  legge già rimpicciolita alla misura della scheda
- **Gallery: delle impostazioni di Notion ci sono solo Card preview e
  Card size.** Mancano "Fit image" (mostrare l'immagine intera invece
  di riempire il riquadro — qui vale sempre l'inquadratura scelta nella
  pagina), "Show page icon", "Wrap all properties", "Open pages in", il
  raggruppamento e l'anteprima da una proprietà "Files & media", che
  l'app non ha. Le schede non si trascinano per riordinarle
- **Gallery: le proprietà sulle schede sono testo semplice**, una per
  riga e tagliata se lunga, come sulle schede della bacheca: i tag non
  sono colorati. "Created time" e "Last edited time" non compaiono,
  come nella bacheca
- **Gallery: l'anteprima "Page content" mostra solo il testo**
  (paragrafi, titoli, elenchi, caselle, toggle chiusi con la freccia):
  niente tabelle, divisori, collegamenti a pagine o database, né il
  contenuto dentro i toggle. Il testo è senza formattazione (niente
  grassetto, corsivo o colori); **gli spoiler restano coperti**, ogni
  lettera diventa un quadratino `▒`. Una pagina senza copertina, con
  "Page cover", mostra il riquadro vuoto
- **Timeline: manca l'ingrandimento "5 anni"** che Notion ha. Escluso
  apposta: a quella scala un mese è largo due millimetri e non si
  distingue più niente
- **Date: niente formato della data e fuso orario** nella finestra della
  data, che Notion ha: qui seguono le impostazioni dell'app. Il
  promemoria c'è dal 25/09/2026 ("Include notification"), ma a un'ora
  fissa — quella della data, o le 9:00 — e non "un giorno prima" o "un'ora
  prima" come su Notion
- **Date: l'ora vale per l'inizio e per la fine insieme**, non si può
  accenderla solo su uno dei due
- **Timeline: l'asse copre al massimo quattrocento caselle** attorno
  alle date che ci sono, e se le date sono più larghe la finestra si
  stringe attorno a oggi. Le caselle non sono pigre, vengono composte
  tutte insieme, quindi senza tetto una data sbagliata nell'anno
  tremila ne costruirebbe un milione
- **Timeline: le barre non si trascinano** per spostare o allungare
  una pagina, come nel calendario. La data si cambia dalla cella
- **Timeline: scorrere partendo dalla colonna dei nomi non fa
  niente**, perché quella colonna è ferma apposta. Il gesto va
  iniziato sull'asse
- **Calendario: le barre non si trascinano** per spostare o allungare
  una pagina. La data si cambia dalla cella, come nelle altre viste
- **Calendario: oltre venti pagine datate nel database le altre
  diventano un "+N"** che non si può aprire, e si guardano dalla
  tabella. Ogni pagina si tiene la sua riga per sempre, anche nelle
  settimane in cui non compare, quindi le righe sono tante quante le
  pagine datate: senza un tetto un database pieno renderebbe altissima
  ogni settimana. È il prezzo delle righe fisse per sempre, scelto
  sapendolo
- **Calendario: ogni settimana è alta quanto tutta la griglia delle
  righe**, anche se è vuota. Discende dalla stessa scelta: se una
  settimana si stringesse sulle pagine che contiene, la stessa pagina
  non starebbe più alla stessa altezza in tutte
- **Calendario: nella vista ad anno non ci sono barre né titoli**, solo
  i giorni occupati cerchiati. A dodici mesi per schermata non ci sarebbe
  spazio per scriverci sopra niente di leggibile. Un giorno lì non si
  tocca per creare una pagina: si tocca il mese e lo si apre da vicino
- **Calendario: solo la settimana si trascina col dito.** Il mese e
  l'anno si cambiano con le frecce
- **Database: le schede della bacheca non si trascinano** da una
  colonna all'altra. Per spostare una riga si cambia il suo tag
- **Database: tipi di proprietà non realizzabili in locale.** Person,
  Created by, Last edited by (non esistono account né utenti), Files &
  media, Button, Place, e tutte le Connections (Google Drive, Figma,
  GitHub, Zendesk) e l'AI Autofill, che richiedono rete e servizi
  esterni — contro il principio dell'app. **Relation, Rollup e Formula
  sarebbero invece possibili** e sono i candidati sensati da
  aggiungere: ognuno è un progetto a sé
- **Database: aprendo una riga come pagina non si vedono le sue
  proprietà.** In Notion stanno in cima alla pagina e si modificano da
  lì; qui i valori vivono solo nella tabella
- ~~**Copertina: non si può spostare né ritagliare.**~~ **Superato**:
  "Reposition" la sposta e la ingrandisce (vedi "Copertina —
  inquadratura" in Cronologia)
- **Immagini: non c'è una raccolta da cui scegliere**, come le copertine pronte di Notion. Si mette la propria
- **Un'immagine mandata con Telegram come "foto" perde la trasparenza.**
  Telegram la comprime in JPEG, che la trasparenza non ce l'ha: l'icona
  arriva con lo sfondo bianco già dentro, e l'app non può toglierlo senza
  rovinare le icone che hanno parti bianche. Va mandata come **file**
  (su Telegram per PC: togliere "Comprimi immagini"), o scaricata
  direttamente sul telefono. Verificato il 25/09/2026 su un'icona di
  Flaticon: il file ricevuto era `IMG_…jpg`, e mandato come file è
  rimasto trasparente
- **Nelle finestrelle che salgono dal basso la barrettina Copia / Incolla
  è dell'app**, non del telefono (vedi Cronologia del 25/09/2026, "quattro
  bug"): ha Cut, Copy, Paste e Select all con le scritte di Android, ma
  non le voci in più che Samsung aggiunge altrove
- **Font: nessuno è quello vero.** Helvetica, Arial, Calibri, Cambria,
  Consolas, Georgia, Verdana, Times New Roman, Futura, Garamond e SimSun
  (dal 25/09/2026 al posto di MS YaHei, che col suo sosia era identico
  al font di sistema) sono commerciali: il telefono non li ha e l'app non li può
  distribuire. Li disegnano dei sosia liberi (tabella in Cronologia,
  "Pagine — font e dimensione del testo"); cinque hanno le stesse
  identiche misure dell'originale, gli altri somigliano. **Verdana** non
  ha un sosia fedele (Noto Sans è il più vicino), **Helvetica** usa
  Inter per non essere identico ad Arial, **Kaiti** è un kaishu a
  pennello (Ma Shan Zheng). Il **Lishu** non c'è: un Lishu libero non
  esiste, ed è stato tolto su scelta dell'utente
- **Font cinesi e giapponesi: la prima volta serve internet** e Google
  Play Services. Pesano 10-20 MB l'uno e si scaricano da Google quando
  una pagina li usa per la prima volta; poi restano sul telefono. Finché
  non arrivano — o se la prima volta si è offline — il testo si vede col
  font di sistema e cambia da solo quando il font c'è. Nel menu di
  scelta i loro nomi non sono scritti col loro font, per non scaricarli
  tutti solo aprendo il menu
- **Font e dimensione valgono per tutta la pagina**, non per un pezzo
  di testo selezionato: è quello che ha chiesto l'utente ("selezionando
  automaticamente tutto quanto"). Il **titolo della pagina** prende il
  font ma non la dimensione (a 72 non ci starebbe nello schermo); i
  **database dentro la pagina** restano come sono; i **quadratini delle
  caselle** e le frecce dei toggle restano della loro misura
- **La dimensione è in sp**, l'unità di Android, non in punti
  tipografici come Word e OneNote: il 16 di partenza dell'app non
  corrisponde all'11 di OneNote
- **Conteggio del testo: le parole si contano come in Word**, cioè
  qualunque cosa fra due spazi: anche "=" o "[]" da soli sono una
  parola. Scelto per dare **gli stessi numeri di Word** sugli stessi
  appunti, che è il confronto che l'utente ha fatto vedere
- **Conteggio del testo: una riga tutta in kanji, senza nemmeno un
  hiragana o un katakana, finisce fra i caratteri cinesi.** Da un
  ideogramma solo non si capisce la lingua, e si decide riga per riga:
  basta un kana nella riga perché i suoi ideogrammi contino come
  giapponesi (vedi `textStatsOf`)
- **Conteggio del testo: il titolo della pagina non conta**, e nemmeno
  i database dentro la pagina né i nomi delle pagine collegate. Il
  testo dentro i toggle invece conta anche quando sono chiusi, e gli
  spoiler contano come testo normale. Il conteggio si fa quando si apre
  il menu: non si aggiorna mentre il menu resta aperto (che è anche
  l'unico momento in cui non si può scrivere)
- **Menu del blocco: "Turn into" di una pagina o di un database non è
  costruito** (la voce c'è, grigia), e nemmeno "Color" di un database
  dentro la pagina. Il colore di un collegamento vale solo per quel
  collegamento, non per la pagina. **"Edit icon" cambia l'immagine
  dell'icona**, come ovunque nell'app: l'emoji di una pagina non si
  sceglie ancora da nessuna parte
- ~~**Annulla dopo un "Duplicate" di una pagina** (o di un toggle con
  dentro una sottopagina) toglie il collegamento alla copia, ma la copia
  resta: la si ritrova dalla ricerca. Annulla segue i blocchi della
  pagina, non le pagine~~ **Superato per le pagine** (richiesta 34):
  Annulla manda la copia nel Cestino. Resta vero per il **Duplicate di
  un toggle** dal menu del blocco che ha dentro una sottopagina: Annulla
  toglie la copia del toggle, e la copia della sottopagina resta, da
  ritrovare con la ricerca
- **Move to fuori da un database perde le proprietà della riga**: la
  pagina esce dal database, come su Notion, e i valori delle colonne
  restano con la riga, che si toglie. Lo dice la riga di spiegazione
  quando si sposta la pagina di una riga. Nome e contenuto restano
- ~~**Annulla non riporta indietro un "Move to" né un "Move to
  trash"**~~ **Superato** (richiesta 34): li riporta indietro, e anche
  "Duplicate". Quando si fanno dai tre puntini, l'Annulla va alla
  **prossima pagina che si apre** (si torna indietro, o si entra nella
  copia): se da lì si va da un'altra parte senza annullare, lo si trova
  nella prima pagina che si apre dopo — è "Annulla l'ultima cosa fatta".
  Dal database a schermo intero non si annulla niente lì (non ha la
  barra): lo si fa dalla pagina in cui si torna
- **Annulla non vive oltre la pagina aperta**: chiusa l'app, o lasciata
  la pagina abbastanza a lungo perché Android la dimentichi, la sua
  cronologia se ne va, come prima
- **"Turn into simple database" non si annulla**: le pagine delle righe
  se ne vanno per sempre, come chiesto (richiesta 31). Per questo, se
  hanno del contenuto, prima c'è una conferma col loro numero
- **Ricerca dentro un database**: le **date e le caselle** non si
  cercano, e nemmeno le date automatiche di creazione e modifica (il
  valore salvato non è quello che si legge). Nelle proprietà e nelle
  pagine si illumina la riga intera, non la parola: dentro la cella o
  la pagina la parola non è segnata. Il testo delle pagine si legge
  quando si apre la ricerca o si torna sulla schermata: se la pagina di
  una riga cambia mentre la ricerca è aperta su un'altra schermata (la
  stessa pagina aperta due volte), lo si vede al ritorno. Le sottopagine
  delle pagine delle righe contano solo per il nome, non per quello che
  hanno dentro
- **Viste collegate**: **l'ordine delle proprietà**, il loro nome, tipo
  e opzioni e "Center" sono del database, quindi valgono anche per tutte
  le sue viste (Notion l'ordine lo tiene per vista); per vista sono
  layout, filtro, ordinamento, raggruppamento, galleria, calendario,
  linea del tempo e quali proprietà si vedono. Il **filtro è uno solo**
  per vista (una proprietà, uno o più valori), come nei database. Una
  vista non ha un nome suo: mostra sempre quello del database. Non si
  può fare una vista di una vista (si collega il database vero)
- **Disegno: dentro un gruppo di righe il disegno non segue la singola
  riga.** È attaccato al gruppo intero (le righe fra due isole, come i
  divisori): scrivendo righe nuove *dentro* il gruppo, sopra al disegno,
  il testo scende e il disegno no. Per questo l'utente mette i divisori
  come confini. Niente selezione a lazo né zoom, e sui database a schermo
  intero non si disegna
- **Notifiche: la riga "/notification" legge la data col giorno
  prima** (`22.10` è il 22 ottobre), qualunque sia il formato della data
  scelto nelle impostazioni. E `9.05` è il 9 maggio, non le 9:05: per
  l'ora, meglio i due punti (`9:05`)
- **Widget: i nomi delle città sono quelli inglesi del sistema** ("Rome",
  "Beijing", "Kolkata"), gli stessi in tutte le lingue: tradurli
  vorrebbe un elenco di centinaia di città in otto lingue. Anche la sigla
  del fuso è quella inglese ("CEST", "PDT"), l'unica che esiste per
  tutti
- **Widget: niente "Quarter" e "Life" della foto** fra le barre fisse:
  non erano nella richiesta; si fanno con "Aggiungi una data" (per
  "Life", da una data di nascita a una di arrivo)
- **Widget: niente Annulla.** Un widget tolto (dopo la conferma) o un
  contatore azzerato non tornano indietro
- **Widget: si spostano coi tre puntini**, Sposta su e Sposta giù, non
  trascinandoli
- **Export/backup** (Markdown+CSV, PDF) progettato ma non implementato.
  Decisione già presa con l'utente: **Markdown+CSV come formato
  principale** (il PDF non conserva la struttura, quindi è inadatto al
  recupero dei contenuti), **PDF come seconda funzione separata**, non
  due rami della stessa
- **App Windows + sincronizzazione**: discusso, non iniziato.
  **Decisione presa (2026-09-18): "forse un giorno"** — non si tocca lo
  schema esistente adesso, ma ogni entità nuova va progettata già
  pronta per la sincronizzazione (identificatore stabile, momento
  dell'ultima modifica). Costo quasi nullo ora, evita una migrazione
  dolorosa se l'obiettivo diventa reale

## Diario delle sessioni cloud

Una voce per sessione cloud, la più recente in cima. Per ognuna: le
richieste dell'utente trascritte parola per parola, cosa è stato fatto
e cosa resta da provare sul telefono. Le modifiche all'app finiscono
**anche** in "Cronologia degli aggiornamenti", come sempre; qui c'è il
filo della conversazione che le ha prodotte.

### Sessione cloud del 24/09/2026 — trasloco su GitHub e vista Gallery

**Contesto.** Limite settimanale di Claude Code esaurito sul PC;
l'utente ha a disposizione una sessione cloud e vuole continuare lì lo
sviluppo, per poi riprendere sul PC col telefono collegato. Il server
cloud non raggiunge né il PC né il telefono, e non ha un emulatore
(manca la virtualizzazione hardware): scrivere e compilare sì,
installare e provare no. Il repository GitHub era vuoto (solo un
README di una riga).

**Richieste dell'utente, parola per parola:**

1. «Ciao»
2. «Saresti in grado di continuare qui il lavoro che stavamo facendo
   sulla mia app notE? Ho ricevuto 100 di crediti su una sessione in
   cloud.»
3. «Sia in chat normale che su Code, utilizzavi una cartella che hai
   creato te (nella mia cartella Downloads sul mio PC con Windows 11)
   alla quale, tramite il collegamento di Android Studio e il Web
   Debugging sul mio S25 Ultra, avevi accesso modificando il codice ed
   operando direttamente sul telefono. Facevi tutte le prove,
   controllavi il log, ed eri in grado di accorgerti di tutti i bugfix
   del momento. Come posso fare?»
4. «Ok, quindi io posso dirti di fare le modifiche al codice che ti
   dico, di aggiungere qualsiasi tipologia di feature alla mia app di
   note, ma non puoi installare l'APK e controllare i bug. Giusto?»
5. «Dimmi una cosa: se io ti metto la cartella sulla repo di Github
   collegata a questa sessione, implementi tutte le feature che ti
   dico, poi io quando il limite settimanale in Code mi si resetta,
   posso prendere quella cartella e riusarla in Code in modo che poi
   posso vedere tutti i bugfix da fare sempre tramite Code?»
6. «Aspetta però, io sono inesperto con Github: come posso caricare la
   cartella nella repo? Posso metterla così com'è oppure devo metterla
   in uno zip?»
7. «Mi dice " Yowza, that's a big file. Try again with a file smaller
   than 25MB. ", ma il progetto pesa 600MB»
8. «Non posso caricarti direttamente qui lo zip?»
9. «Facciamo così, ti mando gli screenshot di tutto quanto e mi guidi
   tu su cosa cancellare» (con gli screenshot delle proprietà di
   `.gradle`, `.idea`, `app` e `gradle`)
10. «1.78MB Non ci credo. Quanta robaccia inutile c'era? Dimmi se leggi
    lo zip nella repo»
11. «Una cosa, mi raccomando: rispetta quello che in Code facevi ossia
    filo e per segno ogni modifica, bug (se riesci a trovarli),
    aggiunta, cose tolte, QUALSIASI COSA, la scrivi sul ReadMe
    (ovviamente anche con i prompt che ti dico qui in cloud). In modo
    che poi, quando ritornerò su Claude Code, potrò avere il progetto
    completo al quale hai lavorato senza ricominciare tutto da capo.
    Chiaro?»
12. «Mi confermi che hai delineato da qui l'inizio del lavoro in cloud
    e che, una volta ripreso in Code, dovrà essere ricontrollato alla
    perfezione per essere funzionabile sul telefono installandolo
    direttamente tramite l'APK collegato tutto tramite Android Studio e
    il Web Debugging del telefono?»
13. «Vista in Gallery» (scelta fra le due cose in sospeso proposte:
    la vista Gallery e l'export in Markdown + CSV)
14. «Com'è che sblocco il download dell'APK?»
15. «Dove si trova quest'opzione?» (con lo screenshot della barra del
    titolo della sessione)
16. «Come dici te non c'è. Qual è?» (con gli screenshot dei due menu)
17. «Cosa faccio?» (con lo screenshot della finestra "Edit cloud
    environment")
18. «Mi dice "Couldn't parse "dl.google.com". Use KEY=value format"»
    — il dominio era finito nel riquadro "Environment variables", che
    vuole righe `CHIAVE=valore`; i domini vanno nel campo che compare
    sotto "Network access" scegliendo un livello personalizzato
19. «Funziona ora? Hai accesso a questi domini?» — sì: dopo la modifica
    dell'utente `dl.google.com` e `maven.google.com` rispondevano
    **già in questa stessa sessione**, nonostante l'avviso della
    finestra ("apply to new sessions")
20. «Stavo chiedendo a Code una feature. Ti ri-incollo qui il prompt:
    Dammi la possibilità di spostare le colonne dei database tenendoci
    premuto sopra e sotto a Edit property ci metterai Move to left e
    Move to right. Di base, la seconda colonna, qualsiasi essa sia, avrà
    la voce Move to left grigia, perché non può essere spostata a
    sinistra di quella principale, ossia Name (la colonna dove si
    trovano le pagine)» — **la funzione era già nel codice arrivato dal
    PC**, esattamente come descritta (`PropertyHeaderCell`), ma il
    README non ne parlava: con ogni probabilità Code l'aveva scritta e
    il limite è arrivato prima dell'aggiornamento del README e della
    prova sul telefono. Documentata in Cronologia ("Colonne — spostarle
    tenendo premuta l'intestazione") e aggiunta alle verifiche qui sotto
21. «Ah ok, perdonami, non mi ricordavo di averlo già chiesto, e quando
    tengo premuto vedo solo Edit property e Hide. La feature di spostare
    la colonna stava dentro Edit property, ecco perché non la vedevo.
    Vorrei che Move to left e Move to right fossero presenti sopra Edit
    property appena apre la finestra quando tengo premuto. Mi confermi
    che è così ora nel codice?» — **Scoperta importante: l'app
    installata sul telefono è più vecchia del codice arrivato dal PC.**
    Sul telefono il menu dell'intestazione ha solo Edit property e
    Hide, mentre nel codice ha anche Move to left e Move to right:
    Code le ha scritte ma non le ha mai installate. Quindi **nel codice
    importato possono esserci altre modifiche mai provate**, oltre a
    questa: tornando sul PC, dopo la copia del database, conviene
    confrontare l'app con la Cronologia e non dare per scontato che
    tutto quello che c'è scritto sia stato visto funzionare. Chiesto
    all'utente se le due voci vanno **sopra** Edit property (come dice
    adesso) o **sotto** (come diceva la richiesta 20, ed è come sono
    nel codice)
22. «B) Spostarle sopra: Move to left, Move to intestazione right, Edit
    property, (Center), Hide.» — scelta B fra le due proposte (la
    parola "intestazione" in mezzo era un refuso nella proposta,
    copiato nella risposta). Fatto: le due voci ora sono in cima al
    menu dell'intestazione, vedi Cronologia
23. «Ed ovviamente rimuovi la voce Move to right che sta dentro Edit
    property» — nella finestra di Edit property comparivano "Move left"
    e "Move right" una alla volta secondo la colonna; tolte tutte e
    due (Cronologia, voce `[Rimosso]`), con la conseguenza che una
    colonna nascosta non si sposta più senza rimostrarla
24. «Dammi la possibilità di usare il Sort anche sulla colonna Name,
    quella principale dei database» — **c'era già**: "Name" è la prima
    voce della finestra del Sort (`SortSheet`, `SORT_BY_NAME`). Alla
    domanda se intendesse un menu tenendo premuta l'intestazione Name
    o una voce mancante nella finestra, l'utente ha risposto: «Errore
    mio, tenendo premuto su Name non usciva niente. Non ho visto
    nell'icona Sort. Perdonami». **Nessuna modifica al codice.** Resta
    vero che tenere premuta l'intestazione "Name" non fa niente
    (`NameHeaderCell` non ha gesti): se un giorno servisse, lì
    potrebbero stare "Sort ascending / Sort descending"
25. (Proposto all'utente un APK di prova installabile accanto all'app
    vera, per vedere le novità senza toccare le note.) «Quello alla
    fine, per il momento non mi va di provare ogni volta. Ti ringrazio
    per il pensiero. Nuova feature. Mettere in fondo alla pagina delle
    impostazioni della pagina (quella che si apre con i tre pallini in
    alto a destra) una voce chiamata "X words", con X che di base
    rappresenta il numero totale delle parole presenti nella pagina
    (come vedi nella foto la sezione cerchiata in rosso). Poi, toccando
    quella voce, mi comparirà un'altra mini finestra (come vedi la
    sezione cerchiata in verde sempre nella foto), o decidi tu come
    pensi sia meglio farla vedere (se magari allungando quella voce
    facendone apparire altre sotto, dimmi te poi vediamo se mi piace)
    che farà vedere sempre quante parole come primissima voce, quante
    lettere, quanti numeri, quanti caratteri totali (ossia lettere e
    numeri insieme) senza spazi, quanti caratteri totali (ossia lettere
    e numeri insieme) con gli spazi, quante linee (ossia in realtà
    quanti blocchi), quanti caratteri giapponesi, quanti caratteri
    cinesi e quante lettere coreane sono contenute in quella pagina.
    Ovviamente queste parole sono quello che ho scritto io manualmente,
    tranne tutto ciò che riguarda i database (quindi le voci dei
    database e cose standard che io non scrivo)» — con lo screenshot di
    un documento Word (appunti di tedesco) dove erano cerchiati il
    contatore "2652 words" della barra in basso e la finestra "Word
    Count". **APK di prova: non fatto**, su sua scelta. **Conteggio:
    fatto**, vedi Cronologia "Pagine — il conteggio del testo"; tre
    scelte da far confermare: l'elenco che si apre sotto la voce invece
    di una finestra, i caratteri contati come in Word (punteggiatura
    compresa), il titolo della pagina escluso
26. «1. Allora metti due voci distinte: lettere e numeri (insieme)
    totali, e caratteri totali (lettere, numeri e tutti gli altri
    simboli, parentesi, punti, virgole, asterischi, eccetera; 2. No no,
    titoli e intestazioni dei databse no» — 1: aggiunta la voce
    **"Lettere e numeri"** (solo lettere e cifre) accanto ai caratteri,
    che contano già ogni segno; restano le due voci dei caratteri senza
    e con spazi della richiesta 25. 2: confermato quello che c'era — il
    titolo della pagina e tutto quello che riguarda i database (titoli,
    intestazioni delle colonne, righe, celle) **non** si contano. I
    titoli H1/H2/H3 scritti nel corpo della pagina invece contano,
    perché sono testo dell'utente: detto all'utente, in attesa di
    conferma
27. «Mettimi la possibilità di modificare il font (metti Helvetica,
    Garamond, Arial, Verdana, Georgia, Calibri, Futura, Times New Roman,
    Cambria e Consolas. Qui ci metti un divider e ci metti Songti,
    Kaiti, MS Yahei e Lishu. Anche qui metti un divider e ci metti
    Mincho, Gothic e Kaisei) e la grandezza dei caratteri in ogni pagina
    (selezionando automaticamente tutto quanto e inserendo un numero a
    piacimento da 5 a 72. Quello fisso e standard è già quello che hai
    messo tu, anzi dimmelo per favore, sono curioso). Mettimi la
    selezione del font e la grandezza del font in Aa prima di B con due
    voci, quella del font con il nome del font in uso in quel momento
    (con la possibilità di toccarci sopra e di modificarlo) e quella
    della grandezza del font con il numero della grandezza in uso in
    quel momento (con la possibilità di toccarci sopra e di modificarlo)
    come fa Onenote nella foto che ti mando» — con lo screenshot delle
    due caselle di OneNote "Calibri" e "11". Risposto: il corpo di
    partenza è **16** (sp), i titoli 28, 22 e 18. Spiegato che i font
    chiesti sono commerciali, con la tabella dei sosia liberi, e fatte
    tre domande. Risposte dell'utente, parola per parola:
    - nomi nel menu: «Il nome del sosia mettilo tra parentesi solo
      quando si apre la finestra di selezione manuale del font. In Aa
      metti solo il nome originale»;
    - da dove arrivano i font: «Misto (Recommended)» — occidentali
      dentro l'app, cinesi e giapponesi scaricati la prima volta;
    - Lishu, che non ha un sosia libero: «Toglierlo».

    Fatto: vedi Cronologia "Pagine — font e dimensione del testo". Da
    far confermare: la dimensione scelta da un elenco 5-72 invece che
    scritta, "Predefinito" in cima all'elenco dei font, Inter per
    Helvetica
28. «1. Fammi la selezione del carattere con una piccola casella dove
    scrivo io il numero manualmente con la tastiera. 2. Ti ringrazio.
    Ovviamente lasciala nella lista. Giusto, sono un cretino. Ora che ci
    penso, se ti chiedo di modificare la grandezza del font, tutti gli
    Heading (e i Toggle Heading che ti stavo per chiedere) non hanno
    senso. Perdonami. Ti chiedo di rimuovere a questo punto tutti gli
    Heading, sia in + che in / , a prescindere dall'intera app» — alla
    domanda su cosa fare dei titoli già scritti («Testo in grassetto»,
    «Testo normale», «Lasciarli come sono») ha risposto: **«Testo
    normale»**. Fatto tutto: vedi Cronologia "Pagine — la dimensione si
    scrive a mano, e via i titoli (Heading)"
29. «Quando tocco l'icona del + sulla toolbar, mi appaiono le feature che
    ti ho chiesto di implementare qualche tempo fa. Ma più recentemente
    ti ho chiesto di usare le stesse più altre nuove se scrivo / e poi
    subito il nome della feature. Prima di tutto, aggiungimi in + quelle
    che ci sono nuove scrivendo / (quelle non implementate, scrivimele
    comunque ma grigie), e cerca di sincronizzare la lista delle feature
    da fare tra + e /, e rispetta anche l'ordine. In + fai due colonne di
    feature invece che una con / , quindi la prima di / sarà la prima a
    sinistra di +, la seconda di / sarà la prima a destra di +,
    eccetera» — fatto: il "+" legge lo stesso catalogo del "/", vedi
    Cronologia "Menu '+' — le stesse voci del menu '/'"
30. «Una cosa che Notion (e forse a dire la verità nessun altro) non ha
    mai avuto e che ho sempre desiderato che avesse: i database
    semplici. Non intendo le tabelle semplici solo con righe e colonne
    tipo Excel, ma proprio un classico database con tutte quante le
    opzioni che consentono di modificare vista e impostazioni di un
    database classico, quindi con filtri, sort, grouping, eccetera ma
    senza tutto ciò che riguarda le impostazioni delle pagine (che ad
    ora penso che non ce ne siano), per far sì che le voci principali
    siano solo testo e non siano pagine. Detto questo, attiva la voce
    Simple database in / e +» — fatto: vedi Cronologia "Database
    semplice — le righe sono solo testo"
31. «1) Di base, mettimi nella toolbar un'icona (a sinistra dell'icona
    Tabulazione a sinistra) con due colonne di puntini, tre verticali a
    sinistra e tre verticali a destra (come vedi nella prima foto), per
    far sì che io modifichi il blocco dove si trova il text cursor
    2) Le opzioni saranno quelle che abbiamo implementato finora, ossia:
    Turn into (con le opzioni che ci sono in / e +, sempre quello che
    abbiamo implementato finora, il resto fallo grigio), Color, Edit
    icon, Add to favorites, Rename, Duplicate, Move to, Move to trash.
    3) Concedimi di modificare e di farmi apparire questa finestra con
    le opzioni che ti ho scritto sopra tenendo premuto su una pagina in
    un campo testo;
    4) Se è un database, in alto aggiungimi Turn into page, Turn into
    simple database (con la consapevolezza che se ci sono pagine con
    del testo, lo perderò per sempre, quindi farò attenzione io), Turn
    into complex database (per far sì che le semplici voci diventino
    pagine effettive di un database classico), Lock database, Open as
    page, e poi giù tutte le altre che ti ho scritto sopra.» — con due
    foto di Notion: il menu di un database dentro una pagina e l'elenco
    "Turn into". Fatto: vedi Cronologia "Il menu del blocco — i sei
    puntini"
32. «Una cosa, ovviamente Edit icon, Add to favorites, Move to e Move to
    trash fammele solo per le pagine. Per i semplici campi di testo non
    mi servono» — scritto mentre il lavoro sulla 31 era in corso. Fatto:
    per una riga di testo il menu ha solo Turn into, Color e Duplicate
    (anche Rename è rimasto alle pagine: vedi Cronologia)
33. «Penso che già ci sia, ma dammi la possibilità di muovere la pagina
    corrente (TRANNE QUELLA DEL MENU PRINCIPALE, o mi dici "The main
    menu cannot be moved" oppure l'opzione solo nel menu principale me
    la fai in grigio, dimmi te poi decido io) in altre pagine. Una volta
    toccato su Move to, mi apparirà una barra di ricerca (simile alla
    visualizzazione gerarchica della sidebar) che mi consentirà di
    cercare la pagina o il database (selezionando la pagina di
    quest'ultimo) dove andrò a mettere la pagina che voglio spostare. Di
    default, sempre in fondo. Se voglio spostare un database, mettimelo
    sempre di default come pagina, poi se lo voglio convertire in
    database aperto e visualizzabile lo deciderò io» — fatto: vedi
    Cronologia "Move to — l'albero con la ricerca". Per la pagina
    principale proposta la voce grigia (messa così); il messaggio resta
    l'alternativa, la scelta è dell'utente
34. «Tu dici l'undo? È un problema così grande che non riesci a farlo?
    Sia del Duplicate che del Move» — no, fatto: vedi Cronologia
    "Annulla e Ripristina anche per Move to, Duplicate e Move to trash"
    (anche Move to trash, che usa lo stesso meccanismo)
35. «Metti vicino all'icona Sort un'icona Search sui database, per
    cercare titoli delle pagine o qualsiasi cosa all'interno delle
    pagine. Come fa Notion, appena tocco l'icona della lente
    d'ingrandimento, mi appare lì subito una barra dove scrivo la parola
    e si illuminano o i titoli delle pagine o le pagine dove si trova
    quello che ho scritto» — fatto: vedi Cronologia "Ricerca dentro un
    database". Come su Notion, mentre si cerca le righe senza la parola
    si nascondono (scelta da confermare con l'utente)
36. «Dammi la possibilità di sincronizzare un database in una pagina su
    un'altra: per esempio, su una pagina A avrò un database X con tutti i
    miei giochi, con le proprietà Select per scrivere le piattaforme,
    una Date per la release date, e il resto. Poi creo una pagina B (non
    all'interno del database X principale dei giochi, magari sempre
    riguardante essi o anche dentro la pagina A) chiamata PS5, una
    chiamata PC-Steam, ed un'altra chiamata Switch 2 (questi sono solo
    esempi). In queste pagine io non dovrò riscrivere tutto il database
    principale, ma dovrò semplicemente rimettere lo stesso database X in
    ognuna di queste pagine (richiamandolo da dove si trova tramite
    l'opzione Linked view of data source, che metterai anche nel + della
    toolbar), ma con il Grouping delle Properties in base a quello che
    voglio vedere, quindi nella pagina PS5 vedrò il database principale
    solo con i giochi della PS5 (perché ho selezionato di vedere solo le
    pagine con il Select PS5 (come vedi nelle foto). Fai sì poi che se
    tocco il titolo del database (come nella foto 'Database principale')
    mi reindirizza al database principale. Ovviamente, di base, con
    Linked view of data source io vedrò lo stesso identico database X
    (ovviamente con tutto sincronizzato, le proprietà, le pagine, quello
    che c'è scritto dentro, eccetera), poi sarò io a modificare i filtri
    necessari per rendere quel database X vedibile su alcuni Select che
    io desidero» — con quattro foto di Notion: il database principale
    raggruppato per piattaforma, la vista collegata "↗ Database
    principale" in un'altra pagina, la scelta della fonte ("Existing data
    sources") e la voce "Linked view of data source" nel menu "/".
    Fatto: vedi Cronologia "Viste collegate — lo stesso database in
    un'altra pagina"
37. «Quando apro la sidebar, nella parte più in alto c'è già la sezione
    dei widget all'interno di essa che ti ho fatto mettere qualche giorno
    fa.
    1) Innanzitutto, mettimi un'iconcina piccola per nascondere l'intero
    spazio dove si trovano i widget, come se fosse un semplice toggle che
    nasconde o riapre l'intera finestra dei widget. Questa scelta rimarrà
    anche se chiudo l'app o la sidebar.
    2) Tra i widget principali che desidero, ci saranno quelli dei fusi
    orari, per esempio un orologio digitale o analogico con i vari fusi
    orari come vedi nella prima foto.
    3) Potrò poi decidere io tenendo premuto su di esso se vedere
    l'orologio analogico o digitale di quale fuso orario preferisco.
    4) Sotto gli orologi, concedimi anche di creare una lista con i nomi
    dei fusi orari come vedi nella seconda foto (ricordati, SEMPRE
    ordinati dal GMT più a sinistra al GMT quello più a destra del
    mondo).
    5) Tra i widget, fammi anche creare una Life Progress Bar (come vedi
    nella terza foto) con delle barre orizzontali (con il colore che
    posso cambiare io manualmente tenendoci premuto sopra) che
    rappresentano il completamento di quanto manca a quella data, ossia
    quanto manca alla fine dell'anno, del mese, della settimana, del
    giorno, e di altre date aggiuntive che potrò manualmente creare io
    (le prime quattro sono ovviamente di default)
    6) Fammi anche creare un counter button (come vedi nella quarta
    foto), con tre pulsantini: un + per aumentare di uno il numero del
    counter, un - per abbassarlo di uno, e una freccetta roteante per
    resettare a 0 il counter
    7) Fammi anche creare una schermata rettangolare stondata per un
    pomodoro timer (quinta foto) con un timer che posso manualmente
    inserire io toccandoci sopra, il nome della sessione, un triangolino
    pieno verso destra pieno classico da pulsante Play, un pulsantino
    Reset, ed un'icona di un ingranaggio con un'opzione per modificare
    quanto dura la sessione di studio e con un'altra per modificare
    quanto dura quella di riposo.» — con cinque foto: (1) una griglia di
    orologi analogici (Los Angeles, New York, Cairo, Beijing, Seoul,
    Canberra) con dentro l'ora digitale e "Sun Sep 20 • PDT"; (2) un
    elenco "Honolulu Time 1:57am ☾ … New Zealand 11:57pm", da ovest a
    est; (3) barre verdi con la cornice, "Year: 73%", "Month: 80%",
    "Week: 52%", "Day: 62%", "Quarter: 93%", "Life: Not set"; (4) un
    contatore "− 0 +" con "reset" sotto; (5) un pomodoro "25 + 5":
    "25:00", "Session", play e reset, Break Length 5 e Session Length 25
    con le frecce. Fatto: vedi Cronologia "Widget nella barra laterale".
    Scelte fatte senza chiedere, da confermare con l'utente: i widget si
    aggiungono da "+ Aggiungi widget" e la sezione parte vuota; orologi
    ed elenco stanno nello stesso widget "Fusi orari"; ore in 24 ore; il
    pomodoro passa da solo da sessione a pausa e viceversa; il contatore
    può andare sotto zero; le barre partono verdi come nella foto.
    "Quarter" e "Life" della foto non erano nella richiesta e non ci
    sono: si fanno con "Aggiungi una data"
38. «Ok, ho quasi finito i crediti gratis. Grazie mille dell'ottimo
    lavoro che hai fatto. Ora mi dici come posso installare l'APK di
    questa build sul telefono? Stai tranquillo per i dati, non ho ancora
    scritto nulla di importante, solo testo o database di prova» — dato
    all'utente l'APK compilato in cloud (commit `0557c35`, build di
    debug) con i passi per installarlo: disinstallare prima notE (firma
    diversa), permettere l'installazione da quella app, installare.
    L'utente ha accettato di perdere le note di prova. Vedi "⛔ Il
    confine" per cosa cambia sul PC
39. «Facciamo una cosa, ora ti chiedo solo un'ultima cosa leggera
    (spero): Possibilità di inserire testo super e subscripted (metti
    l'icona con una x e il 2 piccolo in basso a destra per l'opzione
    subscript e l'icona di una x con un 2 piccolo in alto a destra per il
    superscript in Aa)» — con una foto della barra di formattazione di
    OneNote: B, I, U, barrato, poi il pulsante "x₂" con la freccetta che
    apre Subscript e Superscript. Fatto: vedi Cronologia "Pedice e apice
    nella barra Aa". Qui sono due pulsanti affiancati invece di uno col
    menu, come chiesto (un'icona per ciascuno)
40. «Ultimissa richiesta, poi ti saluto e ritornerò in Code lunedì: lì
    cosa dovrò dare, lo zip oppure in downloads metterò la cartella
    scaricata e aggiornata da Github oppure l'APK? Oppure cosa?» —
    risposto: nessuno dei tre. Né lo zip né l'APK: su Code si **clona il
    repository** con git (lo fa Code stesso), così ha il codice, la
    storia per `git diff 4a35c64` e può ripubblicare; lo zip di GitHub
    non ha la storia, l'APK non ha il codice. Prima conviene **unire la
    pull request** del ramo `claude/funny-ramanujan-uuwahs` in `main`
    dal sito; se non è unita, si clona quel ramo. Aggiunto il ramo al
    passo 2 di "Come aprirla", che non lo diceva

**Cosa è stato fatto:**

- **[Progetto]** Il progetto è arrivato su GitHub come archivio
  `NotionLocal - Copy.7z` caricato dal sito (ramo `main`). Il caricamento
  dal sito accetta file fino a 25 MB e la cartella ne pesava circa 400:
  erano quasi tutti file rigenerabili — `app\build` (circa 365 MB di
  compilazioni) e `.gradle` (33 MB di cache). Tolti quelli, più `.idea`
  e `local.properties`, restano **65 file, circa 2 MB**, di cui circa
  24.000 righe di Kotlin
- **[Progetto]** L'archivio è stato estratto e il progetto messo nella
  **radice del repository**, così Android Studio apre direttamente la
  cartella clonata. L'archivio è stato tolto (resta nella storia del
  ramo `main`)
- **[Progetto]** Aggiunto `.gitignore`: `build/`, `.gradle/`,
  `.kotlin/`, `.idea/`, `*.iml`, `local.properties`, APK/AAB e **chiavi
  di firma** (`*.jks`, `*.keystore`), che non devono mai finire su
  GitHub
- **[Progetto]** Aggiunto `.gitattributes` (`* text=auto`): sei file
  (`README.md`, `AppDatabase.kt`, `ColorPickerSheet.kt`,
  `DatabaseViewScreen.kt`, `PageEditorScreen.kt`,
  `PageEditorViewModel.kt`) avevano gli a-capo di Windows e tutti gli
  altri quelli di Linux. Nel repository ora sono tutti uniformi; su
  Windows Git li rimette alla maniera di Windows quando clona. Il
  contenuto dei file non è cambiato
- **[Progetto]** Il lavoro delle sessioni cloud va sul ramo
  `claude/funny-ramanujan-uuwahs` e arriva su `main` con una pull
  request che l'utente unisce dal sito
- **[Progetto]** README: aggiunte le sezioni "Dove vive il progetto" e
  questo diario, estesa la "Regola di lavoro concordata" (etichette
  `[Rimosso]` e `[Progetto]`, richieste dell'utente trascritte), e il
  passo "prendi il progetto da GitHub" in "Come aprirla"
- **[Progetto]** README: corrette quattro affermazioni rimaste indietro
  rispetto al codice e alla Cronologia stessa — lo schema è alla
  versione **24** e non 21 (`AppDatabase`, `version = 24`, ultima
  migrazione 23→24); il colore del testo **c'è** (il pennello); i
  filtri dei database **ci sono**; la copertina **si inquadra**
  ("Reposition"). Le tre voci dei Limiti noti sono barrate o corrette,
  non cancellate
- **[Progetto]** README: aggiunti in "LEGGIMI PRIMA" **il confine**
  fra codice provato sul telefono e lavoro cloud (commit `83f7b61` e
  `4a35c64`) e la **procedura obbligatoria di ripresa sul PC**: copia
  del database, installazione da Android Studio con la chiave del PC,
  prova di ogni voce da verificare con `logcat` aperto, esito scritto
  qui. Richiesta dell'utente al punto 12

- **[Progetto]** **In questa sessione l'app non si è potuta
  compilare.** Le impostazioni di rete dell'ambiente cloud bloccano
  `dl.google.com`, da cui arrivano sia l'SDK Android sia **tutte** le
  librerie AndroidX (`maven.google.com` rimanda lì; bloccate anche le
  copie di JetBrains e Aliyun). Raggiungibili solo Maven Central e il
  portale dei plugin Gradle. Tentativo fatto: compilare il solo Kotlin
  dell'app in un progetto a parte, con `android-all` di Robolectric
  (da Maven Central) al posto di `android.jar` — fattibile perché il
  codice non usa né `R` né `BuildConfig` — ma senza AndroidX non si
  arriva a niente. Il progetto di prova è rimasto fuori dal repository.
  **Il codice della Gallery è stato quindi solo riletto a mano**, riga
  per riga (tipi, import, rami dei `when`, ambiti di Compose, query
  SQL). All'utente è stato proposto di aggiungere `dl.google.com` ai
  domini consentiti (richieste 14-17). **Dove si trova**: nella barra
  del titolo della sessione, la freccetta accanto al nome della
  sessione → **"Edit cloud environment"** → **Network access** (era su
  "Trusted", che non comprende `dl.google.com`). La finestra avverte
  che le modifiche valgono per le sessioni nuove, ma **in pratica hanno
  funzionato subito anche in questa** (richiesta 19). Se in futuro non
  fosse così: nuova sessione cloud, che riparte dal ramo
  `claude/funny-ramanujan-uuwahs` (o da `main`, se nel frattempo la
  pull request è stata unita) e da questo README
- **[Progetto]** **Poi, sbloccato `dl.google.com` (richieste 14-19),
  l'app si è compilata.** Installati in cloud SDK (piattaforma 35,
  build-tools 34.0.0, controllati gli sha1/sha256 pubblicati) e
  Gradle 8.7; aggirato il 429 di Maven Central (vedi "Compilare in una
  sessione cloud"). Risultati:
  - **il codice arrivato dal PC** (`4a35c64`, in una copia a parte)
    compila: `assembleDebug` riuscito. Serviva a sapere che l'ambiente
    funziona prima di giudicare le modifiche;
  - **il codice con le modifiche di questa sessione** (Gallery,
    spostamento colonne) compila: `assembleDebug` riuscito, e Room ha
    controllato durante la compilazione le due query nuove
    (`observeRowCovers`, `observeRowPreviewBlocks`) contro lo schema;
  - rispetto al codice di partenza c'è **un solo avviso nuovo** del
    compilatore: `Icons.Filled.Notes` è deprecata (usata per la voce
    "Page content"). Il resto dell'app la usa già in altri quattro
    punti, quindi è rimasta per coerenza;
  - **la migrazione 24→25 è stata simulata** su SQLite: creato un
    database con lo schema della versione 24 (preso dal codice generato
    da Room per `4a35c64`), con dentro una pagina, applicate le due
    `ALTER TABLE` della migrazione e confrontate tutte e sette le
    tabelle con lo schema che Room si aspetta per la 25: **identiche**
    (nome, tipo, obbligatorietà, valore predefinito, chiave), e la
    pagina era ancora lì con i due campi nuovi vuoti. Non sostituisce
    la prova sul telefono, ma toglie di mezzo il rischio peggiore:
    un'app che rifiuta di partire
- **[Nuova funzionalità]** La **vista Gallery**: dettagli in
  Cronologia, "Database — vista Gallery". Schema del database alla
  versione **25** (migrazione 24→25)
- **[Progetto]** README: un'altra affermazione superata corretta — "Tema
  scuro fisso, niente tema chiaro" in "Cosa fa l'app oggi", mentre
  Settings → Theme offre Dark / Light / System (`AppSettings.themeMode`,
  e la voce "Theme" in Cronologia). Barrata con la nota "Superato"

**Da verificare sul telefono** (dall'1 al 14 la vista Gallery, il 15
lo spostamento delle colonne, il 16 il conteggio del testo, il 17 e il
18 font e dimensione, il 19 i titoli tolti, il 20 il menu "+", il 21 il
database semplice, il 22 il menu del blocco, il 23 le due correzioni
trovate scrivendolo, il 24 il nuovo Move to, il 25 Annulla dopo una
pagina spostata o cancellata, il 26 Annulla e Ripristina di Move to,
Duplicate e Move to trash, il 27 la ricerca dentro un database, il 28
le viste collegate, il 29 i widget della barra laterale, il 30 pedice e
apice; prima di
installare, **copia del database**: questa build cambia lo schema cinque
volte — 24→25→26→27→28→29):

**Esito della prova sul telefono del 24/09/2026** (✅ provato e
funziona, 🟡 provato in parte: dopo i due punti quello che manca, ⏸️ non
ancora provato; i tre bug trovati sono in Cronologia, "Ripresa sul PC
del 24/09/2026"):
- 1 🟡 compila sul PC da riga di comando; Android Studio non aperto
- 2 ✅ migrazione 24→25 (e tutte fino alla 29) su un database vero
- 3 ✅ — 4 ✅ — 5 ✅
- 6 ✅ copertine (provate dall'utente il 24/09/2026 sera; dal 25/09 si
  possono anche riposizionare nella sola scheda)
- 7 🟡 testo, elenco numerato, casella, toggle ✅; mancano spoiler ed
  elenchi rientrati o partiti da un numero scelto
- 8 ✅ Small 3, Medium 2, Large 1, altezze uguali, ultima riga non
  allargata; in orizzontale più schede per riga (provata dall'utente)
- 9 ✅ — 10 ✅
- 11 🟡 ordinamento ✅; filtro non provato
- 12 ⏸️ servono molte schede con copertina
- 13 ✅
- 14 ✅ confermate dall'utente: piccole, medie e grandi, medie di
  partenza
- 15 🟡 ordine delle voci, grigie agli estremi, spostamento, Edit
  property senza Move ✅; mancano colonna nascosta in mezzo, tabella
  raggruppata, Lock view
- 16 ✅ (valori controllati a mano); giapponese, cinese e coreano solo
  dai test automatici
- 17 🟡 ✅ anche la modalità aereo (provata dall'utente); MS YaHei non si
  distingueva dal font di sistema e il 25/09 è stato sostituito da SimSun;
  mancano caselle, tabelle, collegamenti, grassetto/corsivo simulati
- 18 ✅ dopo la correzione della voce vuota; mancano caselle col dito,
  elenchi a corpo grande, riapertura dell'app
- 19 ✅ — 20 🟡 provata solo To-do dal "+" su una riga di testo
- 21 🟡 creazione, riga senza OPEN, nome, scheda senza Icon ✅; mancano
  altre viste, filtri, barra laterale, ricerca generale, Duplicate
- 22 🟡 barra, menu della riga, Turn into → Page, Duplicate (con la
  correzione), menu del collegamento e del database, Turn into simple /
  complex database con la conferma giusta ✅; mancano Color, Edit icon,
  preferiti, Rename, Move to trash, Lock database, Turn into database
- 23 🟡 toggle cambiato dal "+" ✅; la tabella con Annulla solo dal test
- 24 🟡 dal menu del blocco ✅, pagina principale grigia ✅; mancano i tre
  puntini, i database, le righe spostate fuori
- 25 ✅
- 26 🟡 Move to dal menu del blocco con Annulla e Ripristina ✅ (anche
  Annulla di un Duplicate di riga di testo, voce 22); mancano Duplicate e
  Move to trash di pagine, i tre puntini, le righe dei database, il
  cestino svuotato
- 27 🟡 lente, parola nel nome e dentro la pagina, "No results", X ✅;
  mancano accenti, proprietà, le altre viste, database semplice
- 28 🟡 creazione identica, vista indipendente, riga nuova nel database
  vero, titolo che apre l'originale, Delete solo della vista ✅; mancano
  filtro per Select, menu del blocco sulla vista, cestino dell'originale
- 29 🟡 sezione con la freccetta, "+ Add widget" coi quattro widget,
  orologio analogico sul fuso dell'app coi secondi che avanzano,
  percentuali di Avanzamento giuste (controllate a mano), contatore (+,
  −, azzera, il numero resta), pomodoro (▶ corre e diventa ⏸, la
  freccetta lo rimette a 25:00), sezione nascosta che resta tale dopo
  aver chiuso l'app ✅. Sotto l'orologio si legge "GMT+02:00" e non la
  sigla "CEST" che la voce si aspettava (da guardare). Il pomodoro a fine
  fase suona e passa alla pausa ✅ (provato dall'utente e il 25/09 da qui,
  dopo le correzioni del secondo in più e del suono infinito). Orologi
  aggiunti dall'utente e l'elenco dei fusi ✅ (visti il 25/09). Mancano:
  menu degli orologi, date proprie e colori, rinomina, pomodoro ad app
  chiusa, tema chiaro
- 30 🟡 pedice, apice, scambio, riga che non si allarga ✅;
  mancano gli altri punti

1. **Compila anche sul PC?** In cloud sì (`assembleDebug` riuscito),
   ma con Gradle 8.7 da riga di comando: va confermato in Android
   Studio.
2. **La migrazione 24→25** è avvenuta davvero: `user_version` = 25, e
   `galleryCardPreview` e `galleryCardSize` in fondo al `CREATE TABLE`
   di `pages` (vedi "Nota tecnica"). Tutte le note ancora al loro posto.
3. Settings → Layout mostra **sei riquadri** su due file, l'ultimo
   "Gallery"; sceglierlo chiude la finestra e mostra le schede.
4. Menu "/" → **"Gallery view"** non è più grigia e crea un database già
   in galleria.
5. **Tocco** su una scheda → si apre la pagina; **tocco prolungato** →
   le azioni della riga.
6. **Page cover**: una pagina con copertina la mostra nella scheda, con
   la stessa inquadratura di "Reposition"; **nessun angolo vuoto** con
   Small, Medium e Large, anche con copertine spostate o ingrandite
   molto. Una pagina senza copertina lascia il riquadro vuoto.
   Cambiando la copertina dentro la pagina e tornando indietro, la
   scheda è già aggiornata.
7. **Page content**: titoli, elenchi puntati e numerati (anche
   rientrati e partiti da un numero scelto) scritti come nella pagina;
   caselle ☐/☑; toggle con ▸; **spoiler coperti da ▒**; niente righe
   vuote in testa. Scrivendo nella pagina e tornando indietro, la
   scheda è aggiornata.
8. **Card size** sull'S25 Ultra in verticale: 3 / 2 / 1 schede per riga;
   girando il telefono, di più. Schede della stessa riga **alte
   uguali**; l'ultima riga non allarga le schede.
9. La galleria **dentro una pagina** (non solo a schermo intero):
   nessun crash, la pagina scorre normalmente.
10. **Lock view** spegne "Card preview" e "Card size".
11. **Filtro e ordinamento** valgono anche nella galleria.
12. **Fluidità**: una galleria con molte copertine (misurare con
    `dumpsys gfxinfo`, vedi "Misurare la fluidità"); e scrivere in
    un'altra pagina **mentre un database in galleria è rimasto indietro
    nella navigazione** non deve rallentare la tastiera.
13. **Tema chiaro**: schede e riquadri dell'anteprima leggibili.
14. Chiedere all'utente se gli vanno bene le scelte segnate in
    Cronologia (anteprima di partenza, misure delle schede).
15. **Colonne, menu dell'intestazione** (non è della Gallery: era già
    nel codice, richieste 20-22): tenendo premuta l'intestazione
    compaiono **nell'ordine** Move to left, Move to right, Edit
    property, (Center), Hide; la
    prima colonna dopo Name ha "Move to left" grigia, l'ultima "Move to
    right" grigia; con "Lock view" grigie tutte e due; spostando, le
    celle seguono la loro colonna; con una colonna nascosta in mezzo si
    salta quella; con la tabella raggruppata funziona da ogni gruppo.
    La finestra di **Edit property** (toccando l'intestazione) **non**
    ha più "Move left" / "Move right": sotto il tipo c'è solo "Delete
    property" (richiesta 23).
16. **Conteggio del testo** (richieste 25-26): in fondo al menu dei tre
    puntini di una pagina c'è "X words"; toccandola si apre l'elenco
    di dieci voci (con "Lettere e numeri" subito dopo "Numeri"),
    ritoccandola si chiude; il menu scorre. Confrontare i numeri con
    Word incollandoci lo stesso testo (parole e caratteri dovrebbero
    coincidere; le righe no, perché qui sono blocchi). Provare una
    pagina con un toggle chiuso, una tabella, un database dentro (che
    **non** deve contare), testo giapponese, cinese e coreano. Su un
    database a schermo intero la voce **non** c'è.
17. **Font** (richiesta 27): nella barra Aa, prima di B, la voce del
    font dice "Predefinito" su una pagina mai toccata; l'elenco ha
    Predefinito, poi i dieci occidentali (scritti ognuno col suo font,
    col sosia fra parentesi), un divisore, i tre cinesi, un divisore, i
    tre giapponesi; **la tastiera resta aperta** e dopo la scelta si
    continua a scrivere dove si era. Il testo cambia font ovunque:
    paragrafi, titoli, elenchi, caselle, toggle (anche aperti), tabelle,
    collegamenti a pagine; il titolo della pagina cambia font ma non
    misura. Grassetto e corsivo si vedono (Consolas e Kaiti li simulano).
    **Un font cinese o giapponese**: la prima volta, con internet, il
    testo passa dal font di sistema a quello scelto dopo qualche
    istante; poi funziona anche in modalità aereo. Tornare a
    "Predefinito" rimette tutto com'era.
18. **Dimensione** (richieste 27-28): la voce dice "16"; toccandola si
    apre la finestrella con la casella, il numero già selezionato e la
    **tastiera dei numeri**; accetta solo cifre, "Fatto" si accende solo
    fra 5 e 72 e sotto la casella "Da 5 a 72" diventa rosso fuori dai
    limiti; il tasto Fatto della tastiera conferma. Dopo, si tocca la
    pagina per riprendere a scrivere. A 32 tutto (segni degli elenchi,
    righe) è grande il doppio; a 5 e a 72 niente si sovrappone
    né si taglia; le caselle si spuntano ancora bene col dito;
    selezione, backspace fra righe, Invio e numerazione degli elenchi
    funzionano come a 16 (è il campo unito: vedi "Strade già tentate").
    Scegliere 16 riporta la pagina come prima. La scelta resta dopo
    aver chiuso e riaperto l'app, ed è diversa per ogni pagina.
19. **Titoli tolti** (richiesta 28): nel menu "+" e nel menu "/" non c'è
    più nessun Heading. **Nelle pagine che avevano dei titoli** (è il
    motivo principale della copia del database): dopo l'installazione
    sono righe di testo normale, col loro testo, e restano uniti al testo
    intorno nel campo condiviso (selezione e backspace li attraversano).
    Aprire tutte le pagine che si ricorda avessero titoli e verificare che
    l'app non si chiuda: se si chiudesse, la migrazione 26→27 non è
    passata (leggere `logcat`).
20. **Menu "+"** (richiesta 29): stesse voci del "/" nello stesso ordine,
    su due colonne riga per riga (tabella in Cronologia), le non
    costruite grigie e non toccabili; il menu scorre e la tastiera resta
    aperta. Provare ogni voce attiva **dal "+"** su una riga di testo e
    su un'isola (il titolo di un toggle, una casella dentro un toggle):
    Text, elenchi, To-do, Toggle, Page (entra nella pagina nuova), Table,
    Divider, e ogni vista di database (deve nascere con la vista scelta,
    non sempre come tabella). Il "/" deve comportarsi come prima.
21. **Database semplice** (richiesta 30): da "/" e da "+" nasce una
    tabella senza OPEN; il nome si scrive toccandolo; tenendolo premuto
    si apre la scheda (nome, proprietà, Delete, niente Icon). In tutte le
    altre viste un tocco apre la scheda e **mai** una pagina. Filtro,
    ordinamento, raggruppamento, nascondere e spostare colonne, Lock
    view funzionano come negli altri database. Galleria senza "Card
    preview". "Nuova riga" al posto di "Nuova pagina". Nella barra
    laterale il database non ha la freccetta dei figli; cercando il nome
    di una riga il risultato apre il database (e cercare due righe dello
    stesso database non fa chiudere l'app). "Duplicate" non propone le
    sue righe come destinazione. I database già esistenti sono rimasti
    normali, con OPEN.
22. **Menu del blocco** (richieste 31-32).
    - **La barra**: i sei puntini sono la prima icona, prima del
      rientro a sinistra, e ci sono solo nella barra normale (non in
      quella di Aa). Toccandoli con il cursore in un paragrafo, in un
      elenco, in una casella, nel titolo di un toggle o in una riga
      dentro un toggle: la tastiera si chiude e si apre il menu **di
      quella riga** (il titolo del menu è il suo testo), con **solo**
      Turn into, Color, Duplicate.
    - **Turn into**: le voci sono quelle del "/" nello stesso ordine,
      le grigie non si toccano, il tipo attuale ha la spunta (toccarlo
      chiude e basta). Provare **con del testo nella riga**: ogni tipo
      di testo tiene il testo; Page → collegamento a una pagina che si
      chiama come il testo, **senza entrarci**; Table → il testo nella
      prima cella; Divider → il testo nella riga sotto il divisore, col
      cursore lì; ogni vista di database → database con quel nome e
      quella vista; Simple database → database semplice. Da un toggle
      **con righe dentro** verso qualunque altra cosa: le righe escono
      sotto, nessuna sparisce.
    - **Color**: si apre la finestra del pennello; il colore va su
      tutta la riga (testo o sfondo, la scelta in alto), anche senza
      aver selezionato niente; si vede subito, senza toccare la riga.
      Annulla lo toglie.
    - **Duplicate**: la copia compare subito sotto, uguale. Un toggle
      aperto con righe dentro (e, se c'è, un collegamento a una
      sottopagina dentro): la copia ha le sue righe, e la sottopagina
      della copia è **un'altra pagina** (scrivendoci, l'originale non
      cambia). Annulla toglie la copia.
    - **Pagina (dito tenuto sul collegamento)**: il tocco normale apre
      la pagina come prima; la pressione lunga apre il menu con Turn
      into grigio, Color, Edit icon, Add to favorites, Rename,
      Duplicate, Move to, Move to trash. Color colora il nome **di quel
      collegamento** (la stessa pagina altrove resta bianca). Edit icon:
      l'icona cambia nella riga **subito**. Add to favorites → la pagina
      compare nei Preferiti, e riaprendo il menu la voce dice "Remove
      from favorites". Rename: la finestrella ha il nome selezionato e
      la tastiera; confermato, il nome cambia subito nella riga, nella
      barra laterale e dentro la pagina. Duplicate → "accanto
      all'originale" mette la copia **sotto questo collegamento**
      (provare con una pagina collegata due volte), "dentro una pagina"
      dice dove è finita; in entrambi i casi si resta qui. Move to → il
      collegamento sparisce da qui e compare in fondo alla pagina
      scelta. Move to trash → conferma, poi la pagina è nel Cestino.
      (Annulla dopo queste tre: voce 26.)
    - **Database dentro la pagina**: i sei puntini all'inizio della sua
      riga degli strumenti, prima del riquadro della vista, anche col
      nome nascosto e con Lock view. Il menu ha in cima Turn into page
      (il database si apre a schermo intero e nella pagina resta il
      collegamento), Turn into simple database / Turn into complex
      database (quella attuale grigia con la spunta), Lock database
      (l'interruttore; bloccato, le righe non si modificano), Open as
      page; sotto Turn into e Color grigi e le voci della pagina.
      **Turn into simple database** su un database con pagine di riga
      scritte: compare la conferma col numero giusto; confermando, OPEN
      sparisce, i nomi e le proprietà delle righe restano, le pagine
      delle righe non sono né nella barra laterale né nel Cestino né
      nella ricerca, e l'app non si chiude. Su un database le cui righe
      non sono mai state aperte, nessuna conferma. **Turn into complex
      database**: torna OPEN, le righe si aprono come pagine (nuove,
      vuote) e compaiono nella barra laterale.
    - **Collegamento a un database** (dopo Turn into page), tenuto
      premuto: stesso menu, con in cima **Turn into database**, che lo
      rimette dentro la pagina.
    - **Pagina bloccata** o nel Cestino: la pressione lunga sul
      collegamento apre la pagina e basta, e il database dentro la
      pagina non ha i sei puntini.
23. **Le due correzioni trovate scrivendo il menu del blocco**:
    - **Annulla con una tabella nella pagina**: scrivere qualcosa nelle
      celle, poi scrivere una lettera in un paragrafo e premere Annulla
      (e poi Ripristina). Le celle devono avere ancora il loro testo:
      prima di questa correzione si svuotavano.
    - **Toggle con righe dentro, cambiato dal "+"** in Text (o in un
      elenco, in una casella): le righe che aveva dentro devono
      comparire subito sotto, allo stesso livello, nell'ordine in cui
      erano. Prima sparivano alla vista.
24. **Move to** (richiesta 33), da tutte e tre le strade: tre puntini di
    una pagina, tre puntini di un database a schermo intero, menu del
    blocco (pressione lunga su un collegamento).
    - Sulla **pagina principale** la voce c'è, grigia, e non si tocca.
    - Toccandola si apre l'albero come quello della barra laterale, con
      "Cerca una pagina" in cima e la riga di spiegazione (diversa per
      una pagina, un database, la pagina di una riga). I database si
      aprono come cartelle e non si scelgono; si sceglie una delle loro
      pagine, anche di una riga **mai aperta** (la sua pagina nasce). I
      database semplici non hanno pagine da scegliere.
    - **La pagina che si sposta** si vede grigia al suo posto, non si
      tocca e non si apre; cercando, lei e le sue sottopagine **non
      compaiono**. Provare a cercare una sua sottopagina: non deve
      esserci. Nemmeno le righe dei database che ha dentro.
    - Scelta una pagina: la pagina spostata compare **in fondo** a
      quella, e sparisce da dove stava; un avviso dice "Spostata in …".
      Dai tre puntini si torna indietro, dal menu del blocco si resta.
      Scegliendo la pagina in cui sta già: finisce in fondo (anche se
      prima era dentro un toggle).
    - **Un database** — anche uno che si vedeva aperto dentro la pagina —
      arriva come **collegamento a pagina**; tenendolo premuto, "Turn
      into database" lo riapre dentro la pagina.
    - **La pagina di una riga** spostata fuori: la riga sparisce dal
      database (con le sue proprietà, come dice la spiegazione), la
      pagina tiene nome e contenuto, compare nella barra laterale sotto
      la pagina scelta e non più sotto il database; riaprendola, niente
      proprietà in cima.
    - La barra laterale, riaperta, mostra tutto al posto giusto.
25. **Annulla dopo pagine cancellate o spostate altrove** (correzione
    trovata scrivendo il 24, **confermata dai test**: vedi Cronologia):
    - **Il caso che faceva chiudere l'app**: in una pagina con un
      database dentro, eliminarlo dalle sue impostazioni (Delete), poi
      Annulla. L'app **non** deve chiudersi; il database non torna (è
      cancellato per davvero), il resto della pagina sì.
26. **Annulla e Ripristina di Move to, Duplicate e Move to trash**
    (richiesta 34). Annulla e Ripristina sono le frecce della barra, che
    si vede col cursore in una riga.
    - **Dal menu del blocco** (dito tenuto su un collegamento), in una
      pagina con del testo sopra e sotto il collegamento: Move to →
      Annulla → il collegamento torna **nello stesso punto**, e non c'è
      più nella pagina di destinazione; Ripristina → di nuovo in fondo
      alla destinazione. Duplicate → Annulla → la copia sparisce da qui
      ed è nel Cestino; Ripristina → torna sotto l'originale e non è più
      nel Cestino. Move to trash → Annulla → fuori dal Cestino e di
      nuovo al suo posto.
    - Un collegamento **dentro un toggle**, spostato e poi annullato:
      torna dentro il toggle, allo stesso posto.
    - **Dai tre puntini di una pagina**: Move to → si torna indietro; lì
      Annulla la rimette dov'era. Move to trash → idem. Duplicate → si
      entra nella copia; lì Annulla manda la copia nel Cestino e **torna
      indietro da solo**.
    - **Dai tre puntini di un database a schermo intero**: Move to →
      tornati alla pagina, Annulla lo rimette dov'era, **aperto dentro
      la pagina** se lo era prima.
    - **La pagina di una riga** spostata fuori dal database e poi
      annullata: la riga ricompare nel database **con i valori delle
      sue proprietà**, e la pagina ha di nuovo le proprietà in cima.
    - **Duplicate della pagina di una riga** (accanto all'originale) →
      Annulla: la riga nuova sparisce dal database; Ripristina: torna,
      coi suoi valori.
    - Alternare più volte Annulla e Ripristina, mescolando lettere
      scritte e pagine spostate: tutto torna nell'ordine giusto, e l'app
      non si chiude. Toccare Annulla due volte velocissimo: stesso
      risultato di due tocchi lenti.
    - Svuotare il Cestino dopo un Duplicate annullato, poi Ripristina:
      non succede niente (la copia non c'è più) e l'app non si chiude.
27. **Ricerca dentro un database** (richiesta 35), sia a schermo intero
    sia dentro una pagina.
    - Nella riga degli strumenti la **lente** sta subito dopo Sort (anche
      con Lock view). Toccandola la riga lascia il posto alla barra, con
      il cursore già dentro e la tastiera aperta; la X la chiude, la
      parola si cancella e tornano tutte le righe e la riga degli
      strumenti.
    - Scrivendo, a ogni lettera: restano le righe con la parola **nel
      nome** (lettere illuminate di giallo, maiuscole e minuscole non
      contano, accenti compresi: "PERCHÉ" trova "perché"), **in una
      proprietà** di testo, numero, selezione, selezione multipla,
      URL, email, telefono, o **dentro la pagina** della riga — anche in
      un toggle chiuso, in una tabella, nel nome di una sottopagina; in
      questi due casi è **tutta la riga** a essere illuminata, di un
      giallo più leggero. Date e caselle non si cercano.
    - Provare **tutte e sei le viste**: tabella (anche raggruppata),
      bacheca, elenco, galleria, calendario (anche "senza data"), linea
      del tempo (nomi a sinistra e barre).
    - Nessuna riga trovata: sotto la barra "Nessun risultato".
    - Aprire una pagina trovata, **scriverci una parola nuova**, tornare
      indietro: la ricerca è ancora lì, la tastiera **non** si apre da
      sola, e cercando la parola nuova la pagina si trova.
    - Con la ricerca aperta, "Nuova pagina" (o un tocco su un giorno del
      calendario, o "+" in una colonna della bacheca): la riga nuova
      **si vede**, anche se è vuota.
    - Database semplice: si cercano nome e proprietà (le righe non hanno
      pagine).
    - Una galleria o una bacheca con molte righe: scrivere nella barra
      non deve rallentare la tastiera.
28. **Viste collegate** (richiesta 36). **Prima di tutto la migrazione
    28→29**: dopo l'installazione l'app si apre, tutte le note e i
    database ci sono (un test automatico l'ha già provata su un database
    vero, ma non sul telefono).
    - Il caso dell'utente: una pagina A col database "Giochi" (Select
      "Piattaforma", una Date, altre proprietà, qualche gioco con testo
      dentro). Una pagina "PS5": menu "/" → "Linked view of data source"
      (non più grigia) → si apre l'elenco dei database con "In A" sotto
      "Giochi", e un campo per cercare → toccato "Giochi", nella pagina
      compare **lo stesso database, identico**: stessa vista, stessi
      filtri, raggruppamento, ordinamento, proprietà nascoste. Lo stesso
      dal "+" della barra, e da "Turn into" nel menu del blocco (con del
      testo nella riga: il testo resta, la vista nasce sotto).
    - Il titolo è **"↗ Giochi"** e non si scrive; toccandolo si apre il
      database "Giochi" a schermo intero. L'icona, se c'è, è quella di
      Giochi.
    - Nella vista "PS5": filtro Piattaforma = PS5 → solo i giochi PS5.
      In A il database mostra ancora tutto. Raggruppare, ordinare,
      cambiare vista (bacheca, galleria…), nascondere una proprietà:
      cambia **solo** nella pagina PS5.
    - **Tutto sincronizzato**: scrivere il nome di un gioco, cambiare una
      proprietà, aprire un gioco e scriverci dentro — da una parte o
      dall'altra — si vede subito anche nell'altra. Aggiungere o
      rinominare una proprietà vale per tutte e due (è una proprietà del
      database).
    - "Nuova pagina" nella vista PS5: il gioco nasce **nel database
      Giochi**, già con Piattaforma = PS5, e si vede in tutte e due.
    - Menu del blocco sulla vista (sei puntini nella sua barra): in cima
      niente "Turn into page"; Turn into simple / complex database, Lock
      database, Rename, Edit icon agiscono **su Giochi**; Duplicate, Move
      to, Move to trash e i preferiti sulla vista soltanto.
    - Impostazioni della vista → Delete: la finestra dice che se ne va
      **solo la vista**; confermando, il database Giochi e i suoi giochi
      restano dove sono.
    - La vista **non** compare nella barra laterale né nella ricerca
      generale (lì c'è Giochi, una volta sola), e una pagina che ha solo
      viste non ha il triangolino.
    - Buttare Giochi nel cestino: nella pagina PS5, al posto della vista,
      "Il database mostrato qui è stato cancellato o è nel cestino", coi
      sei puntini per toglierla. Ripristinando Giochi, la vista torna.
    - Nel database principale, con un filtro qualsiasi: "Nuova pagina"
      crea una riga che si vede (prende il valore del filtro, se è una
      selezione).
    - Scrivere una lettera in una pagina, poi aprire una sua sottopagina
      dalla barra laterale (non dal collegamento) e spostarla altrove coi
      tre puntini; tornare alla prima pagina passando da un'altra, e
      premere Annulla finché si può: la lettera sparisce, e il
      collegamento alla sottopagina **non** ricompare lì (la sottopagina
      sta dove è stata messa, una sola volta).
29. **Widget della barra laterale** (richiesta 37). Non toccano il
    database delle note: stanno in un file di preferenze a parte
    (`widgets`), quindi un errore qui non arriva alle note.
    - Aprire la barra: in cima "Widget" con **la freccetta su** a destra
      e sotto "+ Aggiungi widget". Toccando la freccetta resta solo la
      riga "Widget" con la freccetta giù; **chiudere la barra, chiudere
      l'app** (anche dalle recenti) e riaprirla: è ancora chiusa. Di
      nuovo la freccetta: tornano tutti, com'erano.
    - "+ Aggiungi widget" → Fusi orari, Avanzamento, Contatore, Pomodoro:
      ognuno arriva in fondo, in un riquadro arrotondato col suo nome e i
      tre puntini (Sposta su / Sposta giù / Rimuovi, con la conferma; sul
      contatore anche Rinomina). Spostarli e toglierli; chiudere e
      riaprire l'app: ordine e contenuto restano.
    - **Fusi orari**: nasce con un orologio analogico sul fuso dell'app.
      Le lancette si muovono ogni secondo (anche i secondi); dentro al
      quadrante l'ora "16:05:09" e sotto il giorno nella lingua dell'app
      con la sigla del fuso ("gio 24 set • CEST"). **Tenuto premuto**:
      Analogico / Digitale (con la spunta su quello attuale), Cambia fuso
      orario, Rimuovi. "Aggiungi orologio": due orologi per riga.
    - La scelta del fuso: tutti i fusi del mondo **da ovest a est**
      (Pacific/Midway o Pago Pago in cima, Kiritimati in fondo), ognuno
      con "America · GMT-7" sotto; cercare "tokyo", "asia", "GMT+9",
      "5:30" (Kolkata).
    - **Elenco** ("Aggiungi all'elenco"): aggiungere a caso, per esempio
      Tokyo, Honolulu, Rome, New York, Kolkata — si mettono **sempre** da
      ovest a est (Honolulu, New York, Rome, Kolkata, Tokyo), con l'ora e
      il sole (dalle 6 alle 17:59) o la luna. Tenuta premuta, una riga si
      toglie.
    - **Avanzamento**: Anno, Mese, Settimana, Giorno con la percentuale,
      in verde, barre tutte lunghe uguali. Controllare i numeri a mano:
      la settimana comincia il lunedì; il giorno a mezzogiorno fa 50%.
      **Tenuta premuta** una barra: Colore → la tavolozza dei colori
      (senza il selettore "testo / sfondo"); cambia barra e percentuale.
      "Aggiungi una data": nome, Da, A (con il calendario); con A prima di
      Da, "Fatto" è spento. Una data da ieri a domani ≈ 50%. Tenuta
      premuta: Colore, Modifica, Elimina (le quattro fisse non si
      eliminano).
    - **Contatore**: + e − di uno (anche sotto zero), la freccetta che
      gira azzera. Rinominato dai tre puntini, il nome sta sopra al
      numero. Il numero resta chiudendo l'app.
    - **Pomodoro**: "25:00", "Sessione", e tre pulsanti tondi: ▶, la
      freccetta, l'ingranaggio. ▶ → corre e diventa ⏸. **Chiudere la
      barra e tornare dopo un minuto**: il tempo è andato avanti giusto.
      Toccando il tempo si scrive a mano (minuti e secondi, tastiera dei
      numeri), anche mentre corre. Toccando "Sessione" si rinomina.
      L'ingranaggio: Durata della sessione e Durata della pausa, con − e
      +; mettere 1 e 1 per provare in fretta.
    - **Fine della fase**: con sessione a 1 minuto, avviare e aspettare
      **con la barra chiusa**, in una pagina: allo zero **suona** il
      suono delle notifiche scelto nelle impostazioni (o quello del
      telefono) e comincia da sola la pausa, in verde. Con le notifiche
      messe a tacere nelle impostazioni dell'app non suona.
    - Avviare, **chiudere l'app** e riaprirla dopo qualche minuto: il
      timer è al punto giusto (anche passato alla fase dopo), senza
      suonare per quello che è finito mentre era chiusa. La freccetta lo
      rimette a 25:00 fermo, sulla sessione.
    - Tema chiaro (Settings → Theme → Light): i widget si leggono,
      lancette comprese.
    - Aperta la barra, lo scorrimento deve restare fluido anche con
      orologi e pomodoro che si aggiornano ogni secondo.
30. **Pedice e apice** (richiesta 39). Nessuna migrazione: il testo già
    scritto deve aprirsi identico (grassetti, colori, spoiler compresi).
    - Aa: dopo S ci sono **x₂** e **x²**, con la x all'altezza di B, U,
      S. Selezionare il "2" di "H2O" → x₂: il 2 diventa piccolo e
      scende. Il "2" di "mc2" → x²: piccolo e in alto. Toccarlo di nuovo
      lo toglie; sullo stesso testo, l'altro pulsante li **scambia**.
    - Scrivendo subito dopo un pedice si continua in pedice (come per il
      grassetto); per tornare normale si seleziona e si ritocca.
    - Selezione che attraversa più righe del testo unito, in una casella
      da spuntare, in un toggle: funziona uguale.
    - Con un pedice in grassetto e colorato restano tutte e tre le cose.
    - **Cursore e selezione** a cavallo di un pedice: nessuno sfasamento
      (il cursore si mette davvero dove si tocca); la riga non si
      allarga né si sovrappone a quella sopra o sotto.
    - Corpo della pagina grande (Aa → dimensione): pedice e apice
      crescono insieme al resto. Annulla dopo un pedice lo toglie.

## Cronologia degli aggiornamenti

**Le patch notes dell'app** (dal 30/09/2026, chiesto dall'utente). Ogni
richiesta dell'utente è una **versione**, dalla prima, v0.1, a oggi: 🆕
da v0.1 a v0.2, v0.3… se porta qualcosa di nuovo o toglie qualcosa
(`[Nuova funzionalità]`, `[Rimosso]`); 🔧 una **correzione** della
versione prima (da v0.8 a v0.8.1, v0.8.2…) se corregge soltanto
(`[Bug fix]`) o verifica. La più recente sta in cima; sotto il titolo,
come sempre, la richiesta parola per parola e le modifiche, ognuna con la
sua etichetta.

Accanto al numero, il giorno e l'ora (italiana) in cui la versione è
stata chiusa: quella del commit, o, per il lavoro che commit non ne ha
(fino al 23/09/2026), quella dell'ultima risposta di Claude prima della
richiesta dopo, letta dalle conversazioni salvate sul PC. Delle prime
versioni, fatte in un'altra chat fra l'8 e il 18/09/2026, non restano né
l'ora né l'ordine, e lo dicono; di due si sa il momento dai file del
progetto.

Lo stesso testo, identico, è in **Settings → Patch Notes** dell'app, che
lo copia da qui a ogni compilazione (vedi "Nota tecnica"); il numero
della versione più recente è anche quello dell'app.

Le voci nate nelle sessioni cloud (quelle del 24/09/2026, da "Database —
vista Gallery" a "Pedice e apice nella barra Aa") portano scritto che
**vanno ancora verificate sul telefono**: quando lo sono, si aggiunge "(verificato sul telefono il
gg/mm/aaaa)" accanto al titolo.

### 🆕 v0.148 — 06/10/2026 · 10:15 — "Link to page" e "Database - Full page"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 419 test
passati; commit solo in locale)*

Dalla stessa richiesta della v0.146, le due voci, parola per parola:
> «Link to page (un collegamento ad una pagina all'interno del Workspace
> come vedi nella quarta foto)»

> «Database full page (un database che abbia come apertura di default
> quella della pagina, dove rispetto alle altre non sarà inline e me la
> farà aprire direttamente come pagina come vedi nell'undicesima foto)»

- **[Nuova funzionalità]** **"Link to page"** (dal menu "/" e dal "+"): si
  sceglie una pagina dall'albero del Workspace, o cercandola per nome (lo
  stesso di "Move to"; anche le pagine delle righe dei database), e la riga
  diventa un **rimando** a lei: la sua icona con una **freccetta** e il nome
  sottolineato, sempre quelli di adesso. Toccandolo si apre la pagina (un
  database si apre a pagina intera). **La pagina non si sposta**: resta dov'è
  nell'albero, e questa non diventa sua madre
- **[Nuova funzionalità]** Tenendo premuto il rimando: **"Open"**, **"Change
  page"** (si sceglie un'altra pagina), "Duplicate" e "Delete" (toglie il
  rimando, non la pagina). Se la pagina finisce nel cestino il rimando lo
  dice ("In the trash") e non la apre; se viene eliminata per sempre diventa
  "Deleted page"
- **[Nuova funzionalità]** **"Database - Full page"**: nasce un database che
  **si apre subito a pagina intera**, e nella pagina resta il suo
  collegamento (l'icona della tabella e il nome), come un database dopo
  "Turn into page". Da "Turn into" il testo della riga diventa il nome del
  database
- **[Modificato]** "Link to page" non è in "Turn into": il testo della riga
  andrebbe perso
- **[Progetto]** `BlockType.PAGE_MENTION`, con la pagina in
  `BlockAttrs.pageId` e non in `linkedPageId` (che costruisce l'albero);
  `PageMentionBlock.kt`; `PageRepository.makeFullPageDatabase`. Il menu dei
  sei puntini dei blocchi che non sono testo è `ObjectBlockMenu`, che
  useranno anche immagini, video, audio e file. Test: `LinkToPageTest` (4)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/link" → "Link to page" → "Food" fa il rimando con la freccetta;
  toccandolo si apre Food (solo guardata, niente toccato), e tenendolo
  premuto c'è il suo menu; "/full" → "Database - Full page" apre il database
  nuovo a pagina intera, e tornando indietro resta il collegamento
  "Untitled" con la tabella. Le pagine dell'utente e le righe di Main
  calendar sono come prima della prova

### 🆕 v0.147 — 06/10/2026 · 10:08 — "Code": il codice col suo linguaggio e i suoi colori
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 415 test
passati; commit solo in locale)*

Dalla stessa richiesta della v0.146, la voce del codice, parola per parola:
> «Code (una finestrella che mi consente di scrivere del codice da
> programmazione con la scelta di selezionare i più usati linguaggi di
> programmazione come vedi nell'ottava foto)»

- **[Nuova funzionalità]** **"Code"** (dal menu "/" e dal "+"): un
  **riquadro** col carattere a larghezza fissa. In alto a sinistra il
  **linguaggio** (di partenza JavaScript, come su Notion): toccandolo si
  apre l'elenco dei sessanta più usati, con la ricerca in cima e il segno
  accanto a quello scelto. In alto a destra il pulsante che **copia tutto il
  codice** ("Code copied")
- **[Nuova funzionalità]** **Il codice si colora** secondo il linguaggio,
  coi colori dei temi "One" (scuro e chiaro): parole chiave viola, stringhe
  verdi, commenti grigi in corsivo, numeri arancioni, funzioni azzurre, tipi
  gialli. "Plain Text" e "Diff" restano senza colori
- **[Nuova funzionalità]** Nel codice **l'Invio va sempre a capo dentro**, e
  la tastiera non mette maiuscole e non corregge le parole. Mentre si scrive
  codice la barra non mostra "Aa" né il pennello, e i sei puntini non hanno
  "Color": i colori sono quelli del linguaggio. Hanno invece "Delete"
- **[Nuova funzionalità]** **Backspace in un codice vuoto** lo fa tornare una
  riga di testo; **"Turn into"** verso il testo fa di ogni riga di codice una
  riga di testo
- **[Progetto]** `CodeBlock.kt` (il blocco e la scelta del linguaggio),
  `CodeHighlight.kt` (`CODE_LANGUAGES` e `highlightCode`); il linguaggio sta
  in `BlockAttrs.language`. Test: `CodeHighlightTest` (5)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/code" fa il riquadro "JavaScript"; `const x = 1`, `// nota`,
  `alert(x)` su tre righe, colorati; "Python" dalla ricerca "pyth" cambia i
  colori (lì `const` e `//` non vogliono dire niente); il pulsante copia
  ("Code copied"); "Turn into → Text" fa tre righe di testo; backspace in
  un codice vuoto lo fa tornare testo. Le righe di Main calendar sono come
  prima della prova

### 🆕 v0.146 — 06/10/2026 · 09:58 — "Callout" e "Quote": il riquadro con l'icona e la citazione
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 410 test
passati; commit solo in locale)*

L'utente, parola per parola (con undici foto di Notion):
> «Quando tocco il + nella Toolbar e quando scrivo /, ci sono delle feature
> che ti ho fatto scrivere ma che non hai ancora implementato. Ora è il
> momento di farlo. Attiva:
> - Callout (una finestrella ingrandibile con un colore in background di
>   default diverso da quello dell'app come vedi nella prima foto dove posso
>   scriverci qualcosa che attiri l'attenzione. Mettici anche un'icona che
>   posso modificare come vedi nella seconda foto, in base all'aggiunta delle
>   icone che ti ho fatto mettere)
> - Quote (una semplice linea verticale spessa come vedi nella terza foto)
> - Link to page (un collegamento ad una pagina all'interno del Workspace
>   come vedi nella quarta foto)
> - Image (mettere un'immagine come vedi nella quinta foto esclusivamente con
>   un upload diretto: posso redimensionarla, aprirla, zoomarla, scaricarla,
>   e spostarla da qualche altra parte di default sempre in fondo alla
>   pagina di destinazione)
> - Video (mettere un video tramite upload o tramite link come vedi nella
>   sesta foto, che mi permette o di riprodurlo direttamente sull'embed
>   dell'app, con play, pause, resume, e la barra di riproduzione che mi
>   consente di andare avanti o indietro, tipo con il pallino come Youtube, o
>   di aprirlo o nel browser o sull' app corrispondente se esiste)
> - Audio (mettere un file audio che posso riprodurre direttamente sull'app
>   con play, pause, resume, e la barra di riproduzione che mi consente di
>   andare avanti o indietro, come vedi nella settima foto)
> - Code (una finestrella che mi consente di scrivere del codice da
>   programmazione con la scelta di selezionare i più usati linguaggi di
>   programmazione come vedi nell'ottava foto)
> - File (mettere un file che posso aprire o scaricare ovunque io sia come
>   vedi nella nona foto)
> - Web bookmark (mettere un link che posso aprire nel mio browser
>   predefinito come vedi nella decima foto)
> - Database full page (un database che abbia come apertura di default
>   quella della pagina, dove rispetto alle altre non sarà inline e me la
>   farà aprire direttamente come pagina come vedi nell'undicesima foto)»

Alla domanda di Claude (l'app dal 25/09/2026 non usa Internet: per i video
da link e l'anteprima dei segnalibri serve di nuovo) la risposta è stata
**"Sì, rimettilo"**.

Le voci si costruiscono a gruppi, ognuno con la sua versione: questa è la
prima, **Callout** e **Quote**.

- **[Nuova funzionalità]** **"Callout"** (dal menu "/" e dal "+"): un
  **riquadro** con lo sfondo grigio più chiaro della pagina, l'icona a
  sinistra (di partenza la lampadina 💡, come su Notion) e il testo accanto.
  **L'Invio va a capo dentro il riquadro**, che si allunga; un Invio su una
  riga vuota in fondo esce: la riga vuota se ne va e sotto nasce una riga di
  testo. Il testo si formatta come quello delle altre righe (grassetto,
  colori, corpo)
- **[Nuova funzionalità]** **L'icona del riquadro**, toccandola (o dai sei
  puntini, "Edit icon"): le emoji della famiglia "Callout" del selettore di
  Notion (💡 👉 ☝️ 👌 🔑 🚧 ⚠️ 🔥 📌 ✂️ ❓ 🚫 ⛔ ⏰ ☎️ 🚨 ♻️ ✅ 🔒 📎 📖 🗣️ ➡️ 📢
  🛠️ ⚙️), "Other emoji" per scriverne una qualsiasi con la tastiera,
  **"Upload"** per un'immagine dal telefono come per le icone delle pagine, e
  "Remove" per lasciarlo senza icona
- **[Nuova funzionalità]** **Lo sfondo del riquadro**, dai sei puntini
  ("Background color"): lo stesso selettore dei colori di tutta l'app; "No
  color" rimette il grigio
- **[Nuova funzionalità]** **"Quote"**: la **riga verticale spessa** a
  sinistra, alta quanto il testo, e il suggerimento grigio "Empty quote"
  finché è vuota. Anche qui l'Invio va a capo dentro e quello su una riga
  vuota in fondo esce
- **[Nuova funzionalità]** **Backspace a inizio riquadro o citazione** li fa
  tornare testo normale, come su Notion, **con tutte le loro righe**: ogni
  riga diventa una riga di testo (lo stesso con "Turn into" verso un altro
  tipo). Un altro backspace la unisce alla riga sopra
- **[Nuova funzionalità]** Nei **sei puntini** di riquadro e citazione, oltre
  alle voci delle righe di testo, **"Delete"**: sono scatole a sé
- **[Modificato]** Toccando sotto un riquadro o una citazione in fondo alla
  pagina si scrive su una riga nuova, invece di rientrarci dentro
- **[Modificato]** **L'app può di nuovo usare Internet** (permesso tolto il
  25/09/2026): servirà ai video presi da un link e alle anteprime dei
  segnalibri delle prossime versioni. Le note restano sul telefono
- **[Progetto]** Database alla **versione 52**: `blocks.attrsJson`, le voci
  in più dei blocchi nuovi in JSON (`BlockAttrs`: file, indirizzo, nome,
  larghezza, lingua, icona, colore, pagina…). I tipi nuovi sono già tutti
  nell'enum (`CALLOUT`, `QUOTE`, `CODE`, `IMAGE`, `VIDEO`, `AUDIO`, `FILE`,
  `BOOKMARK`, `PAGE_MENTION`). I file dei blocchi si copiano con la pagina o
  il blocco duplicati e coi template, e se ne vanno con la pagina cancellata
  per sempre. `PageRepository.convertMultiline` divide le righe;
  `CalloutParts.kt` ha l'icona e la sua finestra. Test: `CalloutQuoteTest`
  (5) e la migrazione 51→52 in `MigrationTest`
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): "/callout" fa il riquadro con la lampadina; "Prima", Invio,
  "Seconda riga" restano dentro, su due righe; due Invio in fondo escono;
  "/quote" fa la riga spessa con "Empty quote", poi due righe; 🔥 dalla
  finestra dell'icona; lo sfondo blu da "Background color"; backspace a
  inizio citazione la fa tornare due righe di testo. **Alla prima prova
  l'Invio dentro il riquadro lo spezzava in due**: quando la tastiera
  rimanda lo stesso testo per spostare il cursore, il suo a-capo veniva
  preso per un Invio. Corretto. Il database del telefono è passato alla 52
  senza perdere niente

### 🔧 v0.145.1 — 06/10/2026 · 09:26 — "/database" sotto un divisore non chiude più l'app
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 404 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «In never_underestimate_feelings, ho creato un database, poi fatto Add
> below, aggiunto un Divider, scritto /database, toccato Database inline e
> il database è stato creato, ma l'app è crashata. È per caso quel bug di
> cui mi hai parlato ieri?»

- **[Bug]** Sì, era il crash del 03/10/2026 alle 19:31: il database
  "Foreign languages" di never_underestimate_feelings è nato in quello
  stesso secondo. Una riga sola sotto un divisore (o sotto un database) è
  un gruppo di testo tutto suo; scegliendo **"Database - Inline"** (o
  "Simple database", o una delle viste) dal menu **"/"** o dal **"+"**, la
  riga diventava un database e il suo campo di scrittura spariva **col
  cursore dentro**: la tastiera continuava a chiedergli dov'era il cursore
  e l'app si chiudeva (`LayoutCoordinate operations are only valid when
  isAttached is true`, lo stesso errore già risolto per toggle e divisore).
  Ora il cursore si spegne prima, e la tastiera si chiude: nel database non
  c'è niente da scrivere. Lo stesso per **"Table"**, che aveva lo stesso
  difetto. "Turn into" dai sei puntini non lo aveva: lì il cursore si
  spegne già prima di aprire il menu
- **[Progetto]** `applyInsertAction`: `focusManager.clearFocus()` prima di
  `convertToDatabaseLink` e prima di trasformare la riga in tabella, come
  già faceva "Linked view of data source". Il crash dipende dalla tastiera
  vera e non si riproduce nei test sul PC
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta): con la v0.145 i passi dell'utente (database, "Add below",
  "/divider", "/database" sotto il divisore, "Database - Inline") hanno
  chiuso l'app con lo stesso errore del 03/10; con la correzione, gli
  stessi passi creano il database senza crash. Provati anche "/table" e
  "/page" su una riga sola fra due isole: niente crash. Le righe di Main
  calendar sono come prima della prova

### 🆕 v0.145 — 06/10/2026 · 09:13 — "Title position": il titolo sotto l'icona, accanto o nascosto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 404 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nelle opzioni di ogni pagina (i tre puntini orizzontali), consentimi di
> scegliere (tramite una voce chiamata Title position) se:
> 1) spostarmi il titolo di ogni pagina (anche quello del menu) a destra
> dell'icona, così ho più spazio sotto (add cover lo metti in alto nelle
> opzioni di ogni pagina);
> 2) dove sta adesso;
> 3) nasconderlo proprio»

Alle due domande di Claude (la scelta vale per una pagina o per tutte? "Add
cover" in cima ai tre puntini sempre o solo col titolo accanto?):
> «Ora che ci penso è meglio se la metti opzione in Settings sopra
> Animations. Per tutte insieme»

> «Sempre nei tre puntini»

- **[Nuova funzionalità]** In **Settings**, sopra "Animations", la voce
  **"Title position"**, che vale per tutte le pagine insieme, anche quella
  del menu principale: **Below the icon** (come prima, ed è la scelta di
  partenza), **Next to the icon** (il titolo a destra dell'icona, sulla
  stessa riga: tutto il resto della pagina sale) e **Hidden** (il titolo
  non si vede; la pagina si rinomina con "Rename" dal menu del suo blocco
  nella pagina madre o, per una riga, dalla cella del database). Le
  pagine dei quaderni, che non hanno icona, tengono il titolo dov'è
- **[Modificato]** **"Add cover"** non sta più accanto all'icona: è la
  prima voce dei **tre puntini** di ogni pagina, sempre, qualunque sia la
  posizione del titolo. Non c'è quando la pagina ha già una copertina (si
  cambia toccandola), nel cestino e nelle pagine dei quaderni
- **[Progetto]** `AppSettings.titlePosition` (`TitlePosition`: `BELOW_ICON`,
  `BESIDE_ICON`, `HIDDEN`, salvata in "title_position"); nell'editor il
  campo del titolo è `TitleField`, messo nella riga dell'icona o sotto;
  `PageOptionsSheet(onAddCover)`. `TitlePositionTest`: la voce sta sopra
  "Animations" e la scelta si salva
- Verificato sul telefono: in Settings "Title position" sopra "Animations",
  con le tre scelte; con "Next to the icon" "My Personal Workspace" accanto
  al cappello, e in una riga di prova di Main calendar "Untitled" accanto ad
  "Add icon"; nei tre puntini di quella riga "Add cover" in cima, che apre
  la scelta della copertina; con "Hidden" il titolo sparisce dal menu e
  dalla pagina della riga. Poi rimesso "Below the icon" e tolta la riga di
  prova: le righe di Main calendar sono come prima della prova

### 🆕 v0.144 — 05/10/2026 · 22:58 — Il divisore tenuto premuto: "Add above", "Add below" e "Delete"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 403 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Quando tengo premuto su un divider, fai sì che appaiano le impostazioni
> che appaiono quando premo sull'icona dei sei puntini (tre puntini
> verticali in due colonne) dei database che vedi in foto, ossia Add
> above, Add below e Delete. Tutte le altre, naturalmente essendo non
> coerenti con un divider, non le mettere»

- **[Nuova funzionalità]** Tenendo premuto un **divisore** si apre il menu
  dei blocchi (quello dei sei puntini), col titolo "Divider" e solo tre
  voci: **Add above** e **Add below** (una riga vuota sopra o sotto, col
  cursore dentro) e **Delete** (lo toglie subito; Annulla lo rimette).
  Prima il tocco lungo chiedeva soltanto "Delete this divider?". In una
  pagina bloccata il tocco lungo non fa niente, come per gli altri
  blocchi
- **[Rimosso]** La domanda "Delete this divider?"
- **[Progetto]** `DividerBlockContent(onLongPress)` apre `BlockActionsHost`,
  che per un divisore mostra `DividerBlockMenu`. `AddLineTest`: un test in
  più (una riga sopra e una sotto il divisore, e "Delete" che toglie solo
  lui)
- Verificato sul telefono, in una riga di prova di Main calendar (poi
  tolta) con "Sopra" e un divisore: tenendolo premuto, il menu "Divider"
  con "Add above", "Add below" e "Delete"; "Add above" ha messo una riga
  fra "Sopra" e il divisore col cursore dentro, "Add below" una subito
  sotto, "Delete" ha tolto il divisore lasciando le righe. Alla prima
  prova il titolo diceva "Text": corretto. Le righe di Main calendar sono
  come prima della prova

### 🆕 v0.143 — 05/10/2026 · 22:48 — Gli ultimi dieci colori usati, sotto il campo Hex
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 402 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Sotto il box dove scrivo il codice esadecimale del selettore del
> colore, OVUNQUE, mettici gli ultimi dieci colori usati»

- **[Nuova funzionalità]** Nel selettore del colore, sotto il campo
  **Hex**, ci sono i **"Recent colors"**: gli ultimi dieci colori
  applicati, il più recente per primo, in una fila di tondi. Toccandone
  uno il selettore va su quel colore (si applica con "Apply", come gli
  altri); quello uguale al colore scelto ha l'anello bianco
  - **ovunque**: il selettore è lo stesso per il colore del testo e dello
    sfondo, delle celle colorate, dei tag, delle penne del disegno, delle
    barre e degli anelli dei numeri, delle barre dell'avanzamento — è
    l'unico campo Hex dell'app
  - un colore si ricorda quando lo si applica; uno già nella fila sale in
    cima senza doppioni; restano anche chiudendo l'app. Finché non se ne è
    applicato nessuno, la fila non c'è
- **[Progetto]** `AppSettings.recentColors` e `rememberColor` (chiave
  `recent_colors`), `withRecentColor` (in cima, senza doppioni, al massimo
  `RECENT_COLORS_MAX` = 10); `RecentColorsRow` in `ColorPickerSheet`, che
  ricorda il colore su "Apply". `RecentColorsTest`: 2 test (la lista dei
  dieci, senza doppioni e solo colori veri; "Apply" lo ricorda e il tondo
  lo rimette)
- Verificato sul telefono, col colore di un tag "Prova" su una proprietà
  di Main calendar aggiunta per la prova e poi tolta: prima nessuna fila;
  applicati un blu (#040369) e un marrone (#562316), riaprendo c'erano
  "Recent colors" coi due tondi, il marrone per primo con l'anello;
  toccando il blu il campo Hex è passato a #040369. I due colori della
  prova restano fra i recenti. Nel database è cambiata solo l'ora di
  ultima modifica della riga di prova

### 🆕 v0.142 — 05/10/2026 · 22:27 — "Move to database": prima di spostare, il nome della pagina nuova
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 400 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ora che ci penso, sul fatto di spostare una pagina o un database in un
> altro database, c'è una cosa che non ha senso, ma non è mai colpa tua:
> se io ti chiedo di spostare una pagina o un database in un altro
> database, si crea una riga, ossia una pagina. Dammi la possibilità,
> prima di completare lo spostamento, di rinominare la nuova pagina nel
> quale ci sarà quella pagina o quel database che ho spostato»

- **[Nuova funzionalità]** In "Move to database", scelto il database,
  compare **"Name of the new page"**: il nome della pagina nuova, già
  scritto (quello di adesso) e tutto selezionato, con sopra cosa
  succederà. "Move" sposta, "Cancel" torna alla scelta del database
  - **una pagina** diventa lei la pagina nuova: il nome scritto è il suo
    nome nuovo (Annulla rimette quello di prima)
  - **un database** finisce dentro una pagina nuova col nome scritto, e
    tiene il suo nome
  - un nome lasciato vuoto tiene quello di adesso; un database senza nome
    lascia il campo vuoto, con "Untitled" in grigio
- **[Progetto]** `PageRepository.moveIntoDatabase(rowTitle)`;
  `PageChange.MovedToDatabase` ricorda il nome scelto (per Ripristina) e
  quello di prima (per Annulla). `MoveToDatabasePicker` chiede il nome
  (`NewRowNameDialog`) prima di chiamare `onPick(destination, rowTitle)`.
  `MoveToDatabaseTest`: 3 test in più (pagina rinominata con Annulla e
  Ripristina, il nome della pagina di un database, il nome vuoto)
- Verificato sul telefono, in Main calendar: una sottopagina "Bozza" di
  una riga di prova, "Move to database" → Main calendar → la finestra col
  nome "Bozza" selezionato → "Appunti finali" → "Move": è diventata la
  riga "Appunti finali". Poi un database senza nome creato nella riga di
  prova, dal menu del blocco → Main calendar → "Archivio": la riga
  "Archivio" con dentro il database
- **Un errore mio nella pulizia, rimediato**: togliendo le righe di prova,
  la lista era scorsa e ho eliminato per sbaglio la riga senza nome di
  Main calendar delle 18:25 (non mia). L'ho rimessa com'era — stessa
  riga, stessa pagina, stesso posto e stessa data — sul database del
  telefono, con l'app chiusa e dopo il backup, col permesso dell'utente.
  Tolte poi le righe di prova: le righe di Main calendar sono identiche a
  prima della prova

### 🔧 v0.141.1 — 05/10/2026 · 22:15 — Backspace sulla riga vuota sotto un divisore, e "Search databases"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 397 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Dopo controlla questo bug. Quando sto su un blocco da campo di testo
> vuoto e precisamente sotto un divider, faccio poi Backspace, non mi fa
> nulla. Fai sì che io possa cancellare il testo e poi si toglie la
> tastiera, perché il divider non può essere focussato»

(E prima, «Sì falle»: le cose rimaste della v0.141 — provare sul
telefono lo spostamento di un database, togliere le righe di prova, la
scritta della ricerca.)

- **[Bug fix]** Su una **riga vuota subito sotto un divisore**, senza una
  riga di testo sotto di lei (in fondo alla pagina, o con sotto una
  tabella, un database…), il Backspace non faceva niente. Ora **la riga
  se ne va e la tastiera si chiude**: il divisore non si può scrivere, e
  non c'è un'altra riga su cui spostare il cursore. Il divisore resta.
  Com'era: con una riga di testo sotto, la riga vuota se ne va e il
  cursore sale su quella; su una riga col testo il primo Backspace
  "prende" il divisore e il secondo lo toglie
- **[Bug fix]** In "Move to database" il campo di ricerca diceva "Search
  pages", ma cerca database: ora dice **"Search databases"**
- **[Progetto]** `BackspaceOutcome.REMOVED_CLOSE_KEYBOARD`: il campo lascia
  il fuoco e chiude la tastiera prima che la riga sparisca (un campo
  distrutto col fuoco fa chiudere l'app). `DividerBackspaceTest`: il test
  che diceva "la riga vuota in fondo resta" è diventato "se ne va e la
  tastiera si chiude", più due test nuovi (con un database sotto; con del
  testo resta). `SearchField(placeholder)`, `Strings.searchDatabases`
- Verificato sul telefono, in una riga di prova di Main calendar
  ("Contenitore"): inserito un divisore, sotto la riga vuota col cursore,
  Backspace — la riga è sparita, il divisore è rimasto, la tastiera si è
  chiusa, nessun crash. Poi la prova che mancava della v0.141: un
  database creato lì dentro, dal menu del blocco "Move to database" (col
  campo "Search databases") → Main calendar: è nata una riga nuova, e la
  sua pagina mostra dentro il database aperto; da "Contenitore" è
  sparito. Tolte poi le tre righe di prova ("Contenitore", "Sotto" e
  quella del database); le righe "Untitled" e "SaaS" di Main calendar,
  nate alle 18:25 e 18:26, non sono mie e sono rimaste com'erano

### 🆕 v0.141 — 05/10/2026 · 18:20 — "Move to database": una pagina o un database diventa una pagina di un database
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, per una
pagina e — con la v0.141.1 — per un database; 395 test passati; commit
solo in locale)*

L'utente, parola per parola:
> «Dammi la possibilità, tramite un'opzione specifica, di spostare una
> pagina o un database complesso in un database, e di ricrearlo come se
> fosse una pagina aggiunta a quel database»

- **[Nuova funzionalità]** **"Move to database"**, sotto "Move to": nei
  tre puntini di una pagina e di un database, e nel menu di un
  collegamento a pagina o a database. Si apre l'albero delle pagine:
  **toccando un database lo si sceglie** (la freccia lo apre), le pagine
  si aprono come cartelle per arrivare ai database che hanno dentro, i
  database semplici sono grigi (le loro righe non sono pagine), e
  cercando si trovano i database
  - **una pagina** diventa una pagina nuova del database, in fondo, col
    suo nome e tutto il contenuto; lascia il posto in cui stava
  - **un database complesso** finisce in una pagina nuova del database
    scelto, con il suo nome e la sua icona, e **dentro di lei c'è lui,
    aperto** (non una copia): le righe si aprono come pagine di testo,
    quindi un database non può essere lui stesso la pagina di una riga
  - la pagina di una riga lascia il suo database; una vista collegata
    vale come il database che mostra; non si può spostare qualcosa in un
    database che sta dentro di lei
  - **Annulla** rimette tutto dov'era (la pagina nata per un database se
    ne va); Ripristina lo rifà
- **[Progetto]** `PageRepository.moveIntoDatabase`, `PageChange.MovedToDatabase`
  (con Annulla e Ripristina), `searchDatabaseDestinations`,
  `PageTreeNode.isSimpleDatabase`, `imageCopier` (la copia dell'icona,
  dalla `ViewModelFactory`). `DestinationPickerSheet(pickDatabases)`,
  `MoveToDatabasePicker`. `MoveToDatabaseTest`: 6 test (pagina, database,
  pagina di una riga, i no, la vista collegata, la ricerca)
- Verificato sul telefono, in Main calendar: una riga "Contenitore" con
  dentro una sottopagina "Sotto"; dai tre puntini di "Sotto", "Move to
  database" → Main calendar: "Sotto" è diventata l'ultima riga di Main
  calendar, e da "Contenitore" il collegamento è sparito. Lo spostamento
  di un database l'ho provato col telefono dopo, con la v0.141.1 (vedi lì)

### 🔧 v0.140.1 — 05/10/2026 · 18:06 — Rinominare un tag: la tastiera non si chiude più alla prima lettera
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 389 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Se voglio rinominare un Select, appena scrivo una lettera o un numero
> nel rename di un select di un database, la tastiera scompare subito, ma
> quando lo scrivo per la prima volta non dà problemi»

- **[Bug fix]** Nella modifica di un tag (Select e Multi-select), scrivendo
  nel nome la tastiera si chiudeva alla prima lettera o cifra. Il motivo:
  ogni lettera rinominava subito il tag, e l'elenco dei tag arriva dal
  database un attimo dopo; in quell'attimo la modifica cercava il tag col
  nome nuovo, non lo trovava, tornava per un istante all'elenco e il campo
  di testo spariva. Creando un tag non succedeva perché lì si scrive in un
  altro campo, che non rinomina niente. Scrivendo veloce, poi, un rinomina
  poteva partire dal nome vecchio prima che il precedente fosse salvato,
  e perdersi
  - ora **si scrive quanto si vuole**: la pastiglia in cima mostra il nome
    mentre lo si scrive, e il tag **si rinomina una volta sola, uscendo
    dalla modifica** — con "Done", tornando indietro o chiudendo la
    finestra. Le righe che lo usano prendono il nome nuovo in quel momento
  - un nome lasciato vuoto tiene quello di prima; "Delete" elimina senza
    rinominare
- **[Progetto]** `TagOptionEditor`: il nome resta nel campo e si salva in
  `commitName` (da "Done" e all'uscita, `DisposableEffect`).
  `TagRenameTest`: 3 test (con le opzioni che non cambiano, come in
  quell'attimo: scrivendo lettera per lettera la modifica resta aperta e
  il tag non si rinomina; "Done" rinomina una volta; chiudendo vale il
  nome scritto)
- Verificato sul telefono, in Main calendar con una proprietà Select e un
  tag "Prova" aggiunti per la prova e poi tolti: scritti "1", "2", "3" e
  "X" uno alla volta, dopo ognuno la tastiera era ancora aperta (e due
  cancellazioni pure); "Done" ha rinominato il tag in "Prova12", e
  scrivendo "A" e chiudendo con "indietro" è diventato "Prova12A". Nel
  database è cambiata solo l'ora di ultima modifica della riga di prova

### 🆕 v0.140 — 05/10/2026 · 17:57 — Una seconda regola di ordinamento nei database
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, dopo il
backup del database; 386 test passati; commit solo in locale)*

L'utente, parola per parola:
> «Permettimi di selezionare una seconda regola per il sorting di
> un'altra proprietà dei database. Ovviamente rispettando prima quella che
> è già stata inserita e poi quella nuova»

- **[Nuova funzionalità]** Nella finestra **Sort** di un database, sotto
  la regola che c'è, ora c'è **"Then sort by"**: con "Add a second sort"
  si sceglie un'altra proprietà (o il nome), e **conta solo fra le righe
  che per la prima sono pari**. Esempio: per piattaforma, e dentro ogni
  piattaforma per voto
  - si sceglie fra tutte le proprietà tranne quella della prima regola, e
    parte crescente; toccandola si cambia verso, come la prima, e ha il
    suo "Remove sort"
  - **togliendo la prima, la seconda sale al suo posto**, col suo verso;
    scegliendo per la prima la proprietà della seconda, la seconda se ne
    va (ordinerebbe due volte per la stessa cosa)
  - scegliendo la proprietà della prima regola la finestra ora resta
    aperta, perché sotto c'è la seconda
  - vale per ogni vista (Table, Board, List, Gallery…) e per le viste
    collegate, ognuna con le sue regole; un database duplicato le copia
  - le celle vuote restano in fondo anche nella seconda regola
- **[Progetto]** Database alla **versione 51**: `pages.thenSortColumnId` e
  `thenSortDescending` (migrazione 50 → 51: nessun database ne ha una, e
  restano ordinati come prima). `DatabaseViewModel.sortRows` confronta per
  le regole in fila (`sortRulesOf`, `compareForSort`); `setThenSort`;
  `setSort(null)` fa salire la seconda. `SortSheet` con la scelta della
  seconda regola. Nuovo `schema/v50.sql` per `MigrationTest` (la prova
  dalla 50: l'ordinamento di prima resta, nessuna seconda regola).
  `SecondSortTest`: 4 test (la seconda ordina i pari della prima; la
  prima viene prima, anche cambiandole verso; togliendo la prima sale la
  seconda; la seconda vuole una prima e un'altra proprietà)
- Verificato sul telefono (Galaxy S25 Ultra), dopo il backup del
  database: la migrazione alla 51 non ha perso niente (56 pagine prima e
  dopo). In Main calendar, con una proprietà Number e tre righe aggiunte
  per la prova (Beta 1, Alfa 1, Gamma 2) e poi tolte: per Number le righe
  erano Beta, Alfa, Gamma e in fondo quella senza numero; con "Then sort
  by" Name diventano Alfa, Beta, Gamma; con Name al contrario Beta, Alfa;
  tolta la prima, "Name · Descending" è salita al suo posto. Tolto tutto,
  il database è come prima (cambiata solo l'ora di ultima modifica della
  vista di Main calendar)

### 🆕 v0.139 — 05/10/2026 · 17:40 — Il colore dei tag dallo spettro, come in tutti gli altri posti
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 381 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Fai sì che nella selezione del colore dei tag delle proprietà Select dei
> database ci sia lo stesso spettro di colori che c'è in tutti gli altri
> posti»

- **[Nuova funzionalità]** Nella modifica di un tag (Select e
  Multi-select, i tre puntini accanto al tag) l'elenco dei dieci colori
  di Notion è diventato una riga **Color**, col quadratino del colore e
  il suo codice: toccandola si apre **lo stesso selettore** del testo,
  delle celle colorate, delle penne e delle barre — lo spettro con la
  tinta e la luminosità, Rosso/Verde/Blu ed Hex. "Default" torna al
  grigio dei tag di Notion. I tag nuovi prendono ancora, a giro, i dieci
  colori di Notion
- **[Nuova funzionalità]** La scritta di un tag è **nera sui colori
  chiari** (un giallo acceso, per esempio), bianca sugli altri: la stessa
  regola delle celle colorate. Prima era sempre bianca, che coi dieci
  colori scuri di Notion andava bene
- **[Progetto]** `TagOptionEditor` apre `ColorPickerSheet`; `TagChip`
  sceglie la scritta con `darkTextOn`. `TagCell` è `internal` per i test;
  tolta la scritta "Colors", che non usava più nessuno. `TagColorTest`: 3
  test (la riga Color col codice del colore, un colore qualsiasi scritto
  in Hex che arriva al tag, "Default" che torna al grigio)
- Verificato sul telefono, in Main calendar con una proprietà Select e un
  tag "Prova" aggiunti per la prova e poi eliminati: la riga "Color
  #373735", lo spettro, un giallo (#FBF304) toccato nello spettro e nella
  striscia della luminosità, "Apply": il tag giallo con la scritta nera,
  nella modifica e nella tabella. Nel database è cambiata solo l'ora di
  ultima modifica della riga di prova

### 🆕 v0.138 — 05/10/2026 · 17:30 — Le notifiche tutte in Settings, e fra i suoni anche le suonerie
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 378 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nella sidebar c'è l'opzione Notifications, ma c'è anche in Settings.
> Toglila nella sidebar e metti quello che hai messo di nuovo in Settings.
> Poi, tra i suoni delle notifiche, metti anche le Ringtones»

- **[Rimosso]** La voce **Notifications** della barra laterale, e la sua
  pagina (nate con la v0.133)
- **[Nuova funzionalità]** In **Settings** la sezione che si chiamava
  "App sounds" (suono e notifiche spente) ora si chiama **Notifications**
  e ha tutto quello che c'era nella pagina: il suono, **Vibration**,
  **Priority**, **App icon badge**, **Send a test notification**, "Turn
  off notifications" e "Turn off for a while", e in cima, se Android le ha
  spente per l'app, la riga per riaccenderle. Sotto, la nota su cosa
  valgono (timer e pomodoro suonano sempre)
- **[Nuova funzionalità]** Nella scelta del suono delle notifiche, oltre ai
  suoni di notifica (la voce ora si chiama "Notification sounds (default
  and downloaded)", prima "Phone sounds") ci sono le **suonerie**:
  **"Ringtones (default and downloaded)"** apre l'elenco delle suonerie
  del telefono, quelle di serie e quelle aggiunte. Una suoneria è fatta
  per ripetersi: come ogni suono dell'app, si suona una volta sola e per
  pochi secondi
- **[Progetto]** `NotificationsScreen.kt` è diventato
  `NotificationSettings.kt`: `NotificationSettingsSection`, che Settings
  mette sotto la sua intestazione, e `NotificationSoundDialog`. Tolti la
  rotta `Routes.NOTIFICATIONS`, `SidebarActions.onNotifications` e la
  scritta "App sounds". `NotificationSettingsUiTest`: 3 test (in Settings
  ci sono tutte le scelte e non c'è più "App sounds"; "Ringtones" apre
  l'elenco delle suonerie; la barra laterale non ha più "Notifications")
- Verificato sul telefono: nella barra laterale, dopo Templates, Backup e
  Trash, nessun "Notifications"; in Settings la sezione Notifications
  con tutte le voci; "Notification sound" mostra "Notification sounds" e
  "Ringtones", e "Ringtones" (scelto SecSoundPicker con "Just once") apre
  l'elenco "Ringtones" di Samsung, con le suonerie personalizzate e
  quelle di serie. Chiuso senza sceglierne nessuna: il suono resta quello
  predefinito

### 🆕 v0.137 — 05/10/2026 · 17:20 — La vista List senza le linee fra le righe
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 375 test
passati; commit solo in locale)*

L'utente, parola per parola (con l'immagine di una vista List di Notion):
> «Nella visualizzazione List dei database, togli le righe orizzontali tra
> una riga e l'altra»

- **[Rimosso]** Nella vista **List** dei database non c'è più la linea
  sotto ogni riga: le righe si separano con lo spazio, come su Notion.
  Il resto della riga (icona, nome, i primi valori a destra, tocco e
  tocco lungo) è com'era
- Verificato sul telefono, in Main calendar con una vista List e una
  seconda riga aggiunte per la prova e poi tolte: le due righe una sotto
  l'altra, senza linee. Main calendar è tornato sulla vista Table, e il
  database è identico a prima della prova

### 🆕 v0.136 — 05/10/2026 · 17:14 — Le celle URL: un tocco apre il link, il tocco lungo lo modifica, copia o incolla
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 375 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Una modifica. Se sulla cella di un database c'è un link, il semplice
> tocco mi consente di aprire quel link. Se tengo premuto, mi consente di
> modificare il link e di copiarne un altro»

E poi:
> «Se vuoi fai tutte le prove che vuoi nel database in menu Main calendar»

- **[Nuova funzionalità]** Una cella **URL** con un indirizzo dentro non si
  scrive più toccandola: è un link, sottolineato come quelli delle celle
  Page link
  - **un tocco lo apre** col browser del telefono. Scritto senza l'inizio
    ("bulbapedia.bulbagarden.net"), si apre con "https://" davanti; se
    nessuna app lo sa aprire, lo dice ("This link can't be opened")
  - **il tocco lungo** (con la vibrazione) dà un menu:
    - **Edit link**: si scrive nella cella stessa, col cursore in fondo,
      anche quando il link non ci sta tutto. La pagina porta la cella
      sopra la tastiera
    - **Copy link**: lo copia. Da Android 13 il "copiato" lo dice il
      telefono; prima, un avviso dell'app
    - **Paste link**: mette al suo posto quello copiato (su una riga sola,
      senza spazi ai lati). C'è solo se c'è qualcosa da incollare
  - vale ovunque si vede la cella: nella tabella, nella scheda di una riga
    e nelle proprietà in cima alla pagina di una riga. In un database con
    la vista o il contenuto bloccati restano solo aprire e "Copy link"
  - una cella URL vuota si scrive toccandola, come prima
- **[Progetto]** `TextLikeCell` ha `isLink` (lo passa `CellContent` per
  le colonne URL); `linkUri`, `openLink`, `copyLink`, `hasClipText` e
  `pastedLink` in `CellText.kt`; le scritte in `DbStrings`, in tutte le
  lingue dell'app. `CellLinkTest`: 5 test (l'indirizzo con o senza
  "https://", il tocco che apre, il tocco lungo con Copy e Paste, Edit
  link che fa scrivere nella cella, la cella vuota che si scrive come
  prima)
- Verificato sul telefono, in Main calendar con una proprietà URL
  aggiunta per la prova e poi eliminata: scritto
  "bulbapedia.bulbagarden.net", il tocco ha aperto Bulbapedia in Firefox;
  il tocco lungo ha dato "Edit link" e "Copy link" ("Paste link" non c'era:
  niente da incollare), e dopo "Copy link" anche "Paste link"; "Edit link"
  ha aggiunto "/wiki/Ivysaur" e "Paste link" ha rimesso il link copiato.
  Alla prima prova "Edit link" apriva il riquadro del testo intero, che
  in fondo alla pagina restava sotto la tastiera: ora si scrive nella
  cella, che resta in vista. Nel database è cambiata solo l'ora di ultima
  modifica della riga di prova

### 🆕 v0.135 — 30/09/2026 · 19:53 — Il Timer nella tendina: una notifica fissa col tempo che scorre
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 370 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Saresti in grado di farmi sotto forma di notifica il passare del tempo
> del Timer come lo fa quello di Samsung, oppure di pinnarmi in qualche
> modo il timer come notifica "permanente" che rimane lì finché la tolgo
> io con il timer che pian piano scorre?»

E poi:
> «Vai vai scusa»

- **[Nuova funzionalità]** Ogni timer partito dal widget Timer ha la sua
  **notifica fissa** nella tendina, come il timer di Samsung:
  - **in corsa**: il conto alla rovescia accanto al nome dell'app, "Timer
    · 3 m" come titolo e sotto "Ends at 19:37"; i pulsanti **Pause** e
    **Cancel**. Il tempo lo fa scorrere Android, quindi scorre anche ad
    app chiusa, senza che l'app debba aggiornare la notifica ogni secondo
  - **in pausa**: "Paused · 2:37", fermo; i pulsanti **Resume** e
    **Cancel**. "Cancel" riporta il timer alle rotelle, come
    l'azzeramento del widget
  - **finito**: se ne va, e arriva "Time's up" come prima
  - **resta finché la si toglie**: uno scorrimento la toglie senza fermare
    il timer (che poi suona lo stesso), e non torna, neanche riaprendo
    l'app, finché il timer non cambia: pausa, ripresa o di nuovo "Start"
  - toccandola si apre l'app **con la barra laterale aperta**, dove sta il
    widget
  - se il telefono la toglie (riaccendendosi, o aggiornando l'app) mentre
    il timer corre, si rimette da sola
  - un canale suo, **"Running timers"**, silenzioso: niente suono, niente
    vibrazione, niente pallino sull'icona. Con le notifiche spente
    dall'app ("Turn off notifications") non compare
- **[Nuova funzionalità]** Su Android 16 la notifica chiede di essere una
  **"live update"** (il permesso `POST_PROMOTED_NOTIFICATIONS`, "Show live
  updates"), e sul telefono Android la accetta: ha il segno
  `PROMOTED_ONGOING`. La pastiglia col tempo nella barra di stato e la Now
  Bar, però, Samsung (One UI 8.5) le dà per ora solo alle app della sua
  lista; per le altre c'è, a quanto pare, un'opzione fra quelle per
  sviluppatori. Non l'ho toccata: è un'impostazione del telefono
- **[Progetto]** `TimerNotifications` (mette, cambia e toglie le
  notifiche confrontando lo stato di prima e di dopo a ogni modifica di
  `WidgetStore`) e `TimerActionReceiver` (i pulsanti e lo scorrimento via,
  anche ad app chiusa: il timer cambia con `TimerNotifications.handle`,
  poi la sveglia di Android si rimette alla fine nuova);
  `CountdownTimer.notificationDismissed`; `WidgetStore.restoreTimerNotifications`
  all'avvio e dopo un riavvio; `NotificationTarget.openWidgets` per la
  barra laterale. `TimerNotificationsTest`: 9 test (quali timer hanno la
  notifica, il cronometro all'indietro fino alla fine, la pausa col tempo
  fermo, i pulsanti, via quando finisce o si annulla, lo scorrimento via
  che non torna, la richiesta di "live update"). Il test dei pulsanti
  chiama `handle` e non il receiver: la sveglia, rimessa in sottofondo,
  apriva il database dei test e faceva fallire a caso quelli dopo
- Verificato sul telefono (Galaxy S25 Ultra, Android 16), con un secondo
  timer da 3 minuti aggiunto col "+" e poi eliminato: la notifica compare
  alla partenza e scorre; "Pause" e "Resume" dalla tendina fermano e
  fanno ripartire il widget; alla fine se ne va e arriva "Time's up"; **ad
  app chiusa** (processo terminato) resta e scorre, e "Pause" funziona
  (l'app parte solo per quello); tolta con uno scorrimento non torna
  riaprendo l'app, e torna riprendendo il timer; toccandola si apre la
  barra laterale sul widget; "Cancel" la toglie e riporta il timer alle
  rotelle. Rifatte "Pause" e "Cancel" con l'APK finale. Stato dei widget
  e database uguali a prima della prova

### 🆕 v0.134 — 30/09/2026 · 19:22 — La storia intera, da v0.1 a oggi: una versione per ogni richiesta
*(Claude Code sul PC; 361 test passati; verificato sul telefono lo stesso
giorno, con la v0.135; commit solo in locale)*

L'utente, parola per parola:
> «Ho fatto caso ad una cosa: hai racchiuso tutto quello fatto prima di
> Github nella 0.1 tutto insieme in un bloccone enome. Non va bene. Tutto
> quello fatto prima di Github collegalo a quello fatto dopo Github e fai
> così cancella dalla concezione dell'app "quello fatto prima/ dopo
> Github": d'ora in poi sarà tutto unito. Inizia dalla 0.1 dalla
> primissima feature implementata e poi continua fino ad oggi (e
> RICORDATI: ad ogni modifica/ bug fix aggiorna le Patch Notes).
> Ovviamente, riiniziando da capo, continua sempre a rispettare 0.1 -> 0.2
> per feature nuove e modifiche, 0.1 -> 0.1.1 per i bug fix»

- **[Progetto]** README: la vecchia 📦 v0.1, un blocco solo con tutto il
  lavoro arrivato su GitHub il 24/09, è diventata **79 versioni**, dalla
  **v0.1** alla **v0.63**, una per richiesta, in ordine di tempo; quelle
  che c'erano già vengono dopo, rinumerate: dalla **v0.64** (era v0.2,
  "Database — vista Gallery") alla **v0.133** (era v0.71), stesso ordine
  e stessi titoli. In tutto **166 versioni**. Non c'è più un "prima" e un
  "dopo" GitHub
  - **Da dove vengono l'ordine e le ore**: le conversazioni di Claude
    Code salvate sul PC (dal 18/09 sera al 23/09) hanno ogni richiesta
    dell'utente con la sua ora. Ogni richiesta è diventata una versione
    con **la richiesta parola per parola**, come quelle dopo; una
    segnalazione di un difetto è una correzione (🔧), o fa parte della
    versione a cui si stava lavorando. L'ora è quella dell'ultima risposta
    di Claude prima della richiesta dopo (una pausa di più di cinque ore,
    una notte, chiude il lavoro). Il testo delle voci è quello di prima,
    spostato: ogni voce della vecchia v0.1 è finita in una versione sola,
    nessuna persa e nessuna doppia
  - **Le prime**, fatte in un'altra chat («Home») dall'8 al 18/09: delle
    loro richieste non c'è traccia. Quali fossero lo dice il README com'era
    il 18/09, letto in una di quelle conversazioni: sono le versioni dalla
    v0.2 alla v0.6.2, con "fra l'8 e il 18/09/2026 · ora non registrata",
    nell'ordine in cui le elencava. Due momenti si sanno dalle date dei
    file dell'archivio caricato su GitHub: **v0.1 "Nasce notE"**, l'8/09
    alle 09:19 (i primi file del progetto), e **v0.7**, l'icona dalla foto
    e il nome "notE", il 17/09 alle 22:37
  - Scritte adesso, perché prima non avevano una voce a sé: la riga della
    v0.1 (cos'è notE, dalle prime righe del README) e quella della v0.11.1
    (il backspace che toglie un rientro per volta, già descritto nella
    voce del backspace)
  - Aggiornate l'introduzione della Cronologia (come si leggono numeri e
    ore), la "Regola di lavoro concordata" e la frase sulle sessioni in
    cloud. Le versioni v0.131–v0.133 raccontano la numerazione di allora e
    sono rimaste com'erano
- **[Progetto]** App: le Patch Notes leggono anche "ora non registrata"
  (`UNKNOWN_TIME`); niente più 📦 (`PatchKind` ha solo versioni nuove e
  correzioni). `PatchNotesTest`: la prima versione è la v0.1 dell'8/09
  alle 09:19, quelle senza ora sono solo le prime, e dove l'ora c'è non
  torna mai indietro
- Verificato sul telefono (alle 19:42, installata la v0.135; prima era
  scollegato): Settings → Patch Notes, "Oldest first": in cima la v0.1
  "Nasce notE" 08/09/2026 · 09:19, poi le versioni dell'altra chat con
  "fra l'8 e il 18/09/2026 · ora non registrata", la v0.7 del 17/09 alle
  22:37 e dalla v0.8 (18/09 alle 21:19) quelle con la richiesta
  dell'utente dentro, parola per parola; "Newest first": in cima la più
  recente

### 🆕 v0.133 — 30/09/2026 · 18:55 — La pagina "Notifications": suono, vibrazione, priorità e pallino sull'icona
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 361 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Attiva l'opzione Notifications nella sidebar. Dentro, mi metterai le
> varie opzioni che ogni app su Android o iPhone mettono, ossia : * scelta
> del suono delle notifiche (sia suoni di default che suoni scaricati di
> terze parti) * vibrazione * priorità: bassa, normale, alta * badge
> sull'icona dell'app * selezione delle azioni per le notifiche (quelle che
> appaiono nel riquadro della notifica in basso), ossia * completa *
> rimanda * apri nota (che mi aprirà la pagina, perché di default se tocco
> la notifica mi apre la schermata o il database collegato alla notifica)
> * archivia * elimina»

Alle domande su cosa dovessero fare "Completa", "Archivia" ed "Elimina",
poi:
> «Togli le azioni alle notifiche, e quando tocco una notifica fammi
> vedere la pagina o la riga appartenente alla notifica. Perdonami, ora che
> ci penso non hanno senso le azioni»

- **[Nuova funzionalità]** La voce **"Notifications"** della barra
  laterale, spenta finora, apre la pagina delle notifiche:
  - **"Notification sound"**: "Phone sounds (default and downloaded)"
    apre l'elenco dei suoni del telefono — quelli di serie e quelli
    scaricati, anche da altre app: sul telefono dell'utente Android chiede
    se sceglierli con SecSoundPicker o con Zedge —, "Choose from files" un
    file audio qualsiasi, "Use default sound" e "Play" come prima. È la
    stessa finestra di Settings → "Notification sound", che ora ha anche
    lei i suoni del telefono
  - **"Vibration"**, accesa o spenta
  - **"Priority"**: **Low** (in silenzio, solo nella tendina), **Normal**
    (suono e vibrazione, nella tendina) o **High** (anche il riquadro in
    cima allo schermo). Di suo High, com'era prima
  - **"App icon badge"**: il pallino sull'icona di notE finché c'è una
    notifica da leggere
  - **"Send a test notification"**: una notifica subito, con le scelte di
    adesso, per sentire e vedere com'è
  - "Turn off notifications" e "Turn off for a while", le stesse di
    Settings; in cima, se Android ha spento le notifiche di notE, una riga
    che lo dice e porta alle impostazioni del telefono
  - Valgono per i promemoria di pagine e database ("/notification" e
    "Include notification"). **Timer e pomodoro** sono sveglie: suonano e
    compaiono sempre, com'erano (lo dice la nota in fondo alla pagina)
- **[Nuova funzionalità]** **Toccando la notifica di una riga di un
  database semplice** si apre il database **con la scheda di quella riga**;
  prima si apriva solo il database. Il resto com'era: la notifica di una
  data di un database apre la pagina della riga, quella di una riga
  "/notification" la sua pagina
- **[Rimosso]** Le azioni nel riquadro delle notifiche (Complete, Snooze,
  Open note, Archive, Delete) non ci sono: chieste e poi tolte
  dall'utente prima di essere fatte
- **[Progetto]** Android non lascia cambiare priorità, vibrazione e
  pallino a un canale di notifiche che c'è già: ogni combinazione ha il
  suo canale ("reminders.high.vibrate.badge", …, `Notifier.remindersChannel`),
  e scegliendone un'altra quello vecchio si toglie — anche "reminders", il
  canale di prima —, così nelle impostazioni del telefono ce n'è sempre uno
  solo. Con Low l'app non suona. Le scelte si salvano in `AppSettings`
  (`notificationVibrate`, `notificationPriority`, `notificationBadge`).
  Codice nuovo: `ui/screen/NotificationsScreen.kt` (la pagina e la finestra
  del suono, `NotificationSoundDialog`), `Routes.NOTIFICATIONS`,
  `NotificationTarget.rowToShow`; i testi nelle otto lingue. Test nuovo
  `NotificationSettingsTest` (5): i valori di partenza, il salvataggio,
  un canale diverso per ognuna delle 12 combinazioni, il canale che segue
  le scelte (importanza, vibrazione, pallino) con quelli vecchi tolti e il
  canale dei timer intatto, la notifica di prova sul canale giusto
- Verificato sul telefono: la voce della barra apre la pagina; "Send a
  test notification" con High compare in cima allo schermo (importanza 4),
  con Low solo nella tendina (importanza 2); il canale nuovo c'è e
  "reminders" risulta tolto; "Phone sounds" apre la scelta fra
  SecSoundPicker e Zedge e poi l'elenco coi suoni Samsung e quello
  scaricato dall'utente ("Smt3 title screen"), chiuso senza scegliere; il
  tocco sulla notifica di "Esempio" di "SuduS al contrario" (database
  semplice) apre il database con la sua scheda. Poi la priorità rimessa su
  High; suono, vibrazione e pallino mai cambiati; la riga "Esempio" non
  risulta modificata (ultima modifica sempre del 26/09)

### 🆕 v0.132 — 30/09/2026 · 18:33 — Patch Notes: l'elenco dei titoli, e una pagina per ogni versione
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 356 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Una cosa: dato che la pagina delle Patch Notes è iper mega lunga, puoi
> far sì di creare una pagina per ogni titolo grande che hai scritto?
> Lascia icone, nome della versione, data e ora al titolo, poi apro la
> pagina di quel titolo e mostrerà il testo ad esso appartenente»

- **[Nuova funzionalità]** Settings → Patch Notes ora è **l'elenco dei
  titoli**: per ogni versione l'emoji, il numero, il giorno e l'ora, il
  titolo e una freccia, uno sotto l'altro; in cima resta l'introduzione
  che spiega i numeri. **Toccando un titolo si apre la sua pagina**: in
  cima lo stesso titolo (emoji, numero, giorno e ora, titolo), sotto il
  testo di quella versione — la richiesta, le modifiche, le verifiche —
  come nel README. Il nome della pagina, in alto, è il numero della
  versione
- **[Nuova funzionalità]** Tornando indietro da una versione l'elenco
  **resta dov'era**; riparte dalla cima solo cambiando l'ordine ("Newest
  first" / "Oldest first", come prima)
- **[Progetto]** `PatchNotesScreen` (l'elenco, `VersionRow`) e
  `PatchNoteScreen` (la pagina di una versione), rotta
  `Routes.PATCH_NOTE` ("patchNote/{version}"). Le Patch Notes si leggono
  una volta sola (`PatchNotesAsset.loaded`): la pagina di una versione si
  apre subito, senza rileggere i 400 KB del file. Test nuovo
  `PatchNotesPagesTest` (2): nell'elenco le due versioni più recenti una
  sotto l'altra senza il testo della prima, e il tocco apre quella giusta;
  la pagina di una versione mostra il suo testo e non quello delle altre
- Verificato sul telefono: l'elenco coi titoli (🆕 v0.67 "Color columns"
  30/09/2026 · 13:34, 🔧 v0.67.1…); toccato v0.65 "Long text", la pagina
  col titolo e sotto la citazione dell'utente e le quattro voci; col gesto
  indietro l'elenco è dove l'avevo lasciato (v0.62 nello stesso punto dopo
  averla aperta); "Oldest first" riparte dalla cima con 📦 v0.1, "Newest
  first" con v0.69

### 🆕 v0.131 — 30/09/2026 · 18:22 — Le Patch Notes: ogni voce una versione con giorno e ora, e Settings → Patch Notes
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 354 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Vorrei chiederti una feature non feature per me molto importante,
> qualcosa che riguarda il ReadMe adesso: ho già visto che all'interno di
> esso lo stai facendo bene nella Cronologia aggiornamenti, con il titolo
> della modifica principale (come se servisse da riassunto) più le
> modifiche specifiche sotto. Ottimo. Se riesci, vorrei ogni modifica
> fatta fino ad ora complessiva anche di data e ora, e come se ogni mio
> prompt diventasse un'aggiunta della versione dell'app (tipo quando si
> passa dalla 0.1 alla 0.2) ed ogni bug fix diventasse una mini aggiunta
> decimale della versione (tipo nuova versione 0.1 ma subito dopo bug fix
> e diventa 0.1.1), come se nel ReadMe ci fossero varie Patch Notes. Cerca
> ovviamente di mantenere lo stesso stile del ReadMe che hai avuto finora,
> ma con queste piccole aggiunte visive e di dettagli più specifici che ti
> ho chiesto. Un'altra cosa riguardante il ReadMe, una chicca molto carina
> che mi è venuta in mente, è quello di creare in Settings, in fondo, una
> voce chiamata Patch Notes, con la lista completa e uguale spiccicata di
> tutte le modifiche come le hai scritte nel Read Me, ovviamente con data
> e ora di tutto quanto, che poi si aggiornerà ogni volta che modificherai
> anche una singola riga di codice. Di default, la lista delle modifiche
> sarà ordinata dall'alto più recenti e verso il basso più vecchie»

- **[Progetto]** README: la Cronologia diventa le **patch notes**
  dell'app. Ogni voce ha un titolo nuovo, "### 🆕 v0.68 — 30/09/2026 ·
  15:09 — …": il numero della versione, il giorno e l'ora (italiana) del
  commit che l'ha chiusa, presi dalla storia di git. 🆕 è una versione
  nuova (qualcosa di nuovo o di tolto), 🔧 una correzione (solo `[Bug
  fix]`, o una verifica), numerata sulla versione prima (v0.67.1). Le 81
  voci di prima sono diventate 85 versioni, da v0.1 a v0.68, di cui 17
  correzioni
  - La parte scritta prima del trasloco su GitHub, divisa per argomento e
    senza date, è **📦 v0.1** ("fino al 24/09/2026 · 11:26", l'ora
    dell'archivio caricato): delle richieste di allora non c'è traccia
    una per una
  - La voce del 25/09 "le prove dell'utente, e quello che ne è venuto"
    raccoglieva cinque richieste con cinque commit: ora sono cinque
    versioni (v0.16 le prove della sera prima, 🔧 v0.16.1 il pomodoro che
    partiva in ritardo, v0.17 l'ora nei fusi orari, v0.18 cronometro e
    timer, v0.19 via i tempi pronti del timer). Il testo è lo stesso,
    spostato; cambiano solo i rimandi ("vedi v0.19")
  - Dove una voce aveva più commit di codice (il conteggio del testo, 12:28
    e 12:32) vale l'ora dell'ultimo; i commit solo del README (le verifiche
    scritte dopo) non la spostano. "Invio in fretta" era datata 26/09 ma
    il suo commit è del 28/09 alle 19:33: vale il commit
  - Tutto il resto è com'era: sotto ogni titolo la richiesta parola per
    parola e le modifiche con le loro etichette. Aggiornate la "Regola di
    lavoro concordata" (ogni voce è una versione), la "Nota tecnica" e la
    "Struttura del progetto"
- **[Nuova funzionalità]** **Settings → Patch Notes**, in fondo alle
  impostazioni, col numero della versione di adesso sotto. Apre la
  Cronologia **identica** a quella di questo README: il testo con
  grassetto, corsivo, barrato e codice, le richieste dell'utente come
  citazioni, gli elenchi (anche rientrati) e le tabelle; ogni versione ha
  in cima l'emoji, il numero (azzurro per le versioni nuove, grigio per le
  correzioni), il giorno e l'ora, e il titolo. Di suo la più recente sta
  in cima; **"Newest first" / "Oldest first"** in alto a destra inverte
  l'ordine
- **[Progetto]** L'app **non ha una copia sua** da tenere aggiornata: a
  ogni compilazione il task `patchNotes` (`app/build.gradle.kts`) copia la
  sezione "Cronologia degli aggiornamenti" del README fra gli asset
  (`patch_notes.md`), quindi ogni voce scritta qui è nell'app appena la si
  ricompila. Anche la **versione dell'app** viene da qui: `versionName` è
  il numero della voce più recente, `versionCode` quante versioni ci sono
  (prima erano "0.1.0" e 1)
- **[Progetto]** Codice nuovo: `data/patchnotes/PatchNotes.kt` (divide il
  Markdown in versioni e blocchi) e `ui/screen/PatchNotesScreen.kt` (la
  schermata, e grassetto, corsivo, barrato e codice dentro le righe);
  `Routes.PATCH_NOTES`; i testi nelle otto lingue. Test nuovo
  `PatchNotesTest` (7): l'app ha la Cronologia uguale al README; ogni
  versione ha numero, giorno e ora; i numeri si susseguono (v0.N+1 dopo
  una versione, v0.N.1, v0.N.2 dopo le correzioni); l'ora non torna mai
  indietro; la versione dell'app è l'ultima; la lettura del Markdown. **È
  anche il controllo della Cronologia**: una voce scritta senza numero o
  fuori ordine fa fallire i test
- Verificato sul telefono: in fondo a Settings "Patch Notes" con sotto la
  versione; aperta, l'introduzione e poi 🆕 v0.68 "Il time-blocking come
  Google Calendar…" 30/09/2026 · 15:09, con la citazione, il corsivo e il
  codice come qui; "Oldest first" porta in cima 📦 v0.1 coi suoi elenchi e
  i nomi del codice

### 🆕 v0.130 — 30/09/2026 · 15:09 — Il time-blocking come Google Calendar: la modalità Day del calendario
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 347 test
passati; commit solo in locale)*

L'utente, con tre foto di Google Calendar (la settimana con la griglia
delle ore e gli eventi colorati — Dormire, Colazione, Corsetta, Coreano…;
la finestrella che si apre toccando un quadratino vuoto della domenica;
la finestra completa di "More options"), parola per parola:
> «Saresti in grado di creare lo stesso sistema del Timeblocking degli
> eventi che ha Google Calendar? Questa è una cosa che sicuramente
> utilizzerò tantissimo sul programma su Windows o sulla Webapp. Ossia,
> nella modalità giorno del calendario (che aggiungerai a Year, Month e
> Week), io posso toccare sulle celle vuote che vedi nella domenica della
> prima foto, e poi mi comparirà una finestra come nella seconda foto.
> More option avranno le opzioni classiche del repeating dell'evento che
> vedi nella terza foto»

Alle due domande di Claude ha risposto: **nell'app Android adesso** (la
versione Windows e quella web non esistono ancora, vedi "App Windows +
sincronizzazione") e **il colore si sceglie evento per evento**, come su
Google Calendar. Poi ha precisato: «Intendevo di non considerare la
situazione del programma Windows. Ho già risposto alle tue domande,
continua.»

- **[Nuova funzionalità]** Il calendario ha un quarto pulsante, **Day**,
  dopo Year, Month e Week: un giorno solo, con le ventiquattro ore una
  sotto l'altra divise in **quadratini da mezz'ora**. Ogni pagina con
  un'ora è un **blocco alto quanto dura**, del suo colore, col nome e le
  ore ("Corsetta, 16:00 – 16:30" su una riga se è corto); quelle senza ora
  (o lunghe un giorno o più) stanno in una **striscia in cima**. Gli
  eventi che si accavallano stanno **affiancati**; quelli che passano la
  mezzanotte si vedono in tutti e due i giorni. Oggi ha la **linea rossa
  di adesso**; si apre un'ora prima di adesso. Le ore si scorrono su e giù
  in un riquadro che arriva al fondo dello schermo, i giorni col dito a
  destra e a sinistra (e con le frecce e Today, come sempre); le ore a
  sinistra restano ferme
- **[Nuova funzionalità]** **Toccando un quadratino vuoto** si apre la
  finestra rapida della seconda foto: "Add title" con la tastiera già
  aperta, il giorno e le ore (mezz'ora dal quadratino toccato; si tocca il
  giorno o un'ora per cambiarli, con i selettori di Samsung della
  proprietà Date), "Does not repeat", il colore, e sotto **"More
  options"** e **"Save"**. Mentre la si scrive l'evento si vede già nel
  suo quadratino, "(No title)" finché non ha un nome. Salvando nasce una
  pagina del database con quel nome, quella data e quel colore (la
  proprietà data si crea al volo se non c'è, come toccando un giorno)
- **[Nuova funzionalità]** **"More options"** è la finestra della terza
  foto: il nome; **"All day"**; inizio e fine, giorno e ora (spostando
  l'inizio la fine lo segue e l'evento dura quanto prima; una fine prima
  dell'inizio non si accetta); la **ripetizione** con le scelte di Google
  Calendar — Daily, Weekly on…, Monthly on…, Annually on…, Every weekday
  (Monday to Friday), Custom… — le stesse della proprietà Date;
  "Include notification"; il **colore**. I colori sono gli undici di
  Google Calendar (Tomato, Flamingo, Tangerine, Banana, Sage, Basil,
  Peacock, Blueberry, Lavender, Grape, Graphite) più "Default", il grigio
  di sempre
- **[Nuova funzionalità]** **"Edit event"**: tenendo premuta una pagina
  nel calendario, in cima alle azioni, apre "More options" su di lei (per
  una che si ripete, su tutta la serie: è una pagina sola). Si scrive solo
  quello che cambia: salvata com'era, la pagina non risulta modificata.
  Toccare un blocco apre la sua pagina, come nelle altre viste
- **[Nuova funzionalità]** Anche **le barre del mese e della settimana**
  hanno il colore del loro evento
- **[Bug fix]** Nella settimana (e nel mese) una pagina che si ripete più
  volte nella stessa settimana — ogni giorno, per esempio — **compariva una
  volta sola**, l'ultima, e le altre finivano nel "+N": le volte della
  stessa pagina stavano nella stessa riga e si coprivano. Ora ci sono
  tutte (c'era già prima: con "ogni settimana" non si vedeva)
- **[Progetto]** Schema alla **versione 50**: `database_rows.eventColor`
  ("#RRGGBB", null per il colore di sempre), migrazione 49→50 che lo
  lascia vuoto dappertutto. Cambiarlo aggiorna `updatedAt`, come chiede la
  regola per la sincronizzazione futura. La modalità `CalendarMode.DAY` si
  salva col nome come le altre, senza migrazione. L'evento si salva nella
  cella della data nel formato di sempre (`encodeDateRange`), quindi i
  filtri, le notifiche e le altre viste lo leggono già. Codice nuovo:
  `data/entity/CalendarEvent.kt` (colori, l'evento che si scrive, dove va
  ogni blocco nella giornata e come si affiancano) e
  `ui/screen/CalendarDay.kt` (la griglia, la finestra rapida, "More
  options"); `DatabaseViewModel.saveEvent`. Test nuovi: `CalendarEventTest`
  (16), `EventSaveTest` (4), `MigrationTest.from49NoRowHasAnEventColor`
- **[Bug fix]** (trovato provando, prima del commit) Scorrendo i giorni
  col dito il giorno si fermava a una ventina di pixel dal bordo, con un
  filo del giorno accanto. Il pager contava duecentomila pagine: a
  centoventi milioni di pixel i suoi numeri con la virgola vanno a passi
  di otto e l'ultimo tratto non si faceva. Ora sono ventimila, come la
  settimana (ventisette anni per parte; più in là il pager riparte da solo
  sullo stesso giorno)
- Verificato sul telefono sulla vista di prova "Videogiochi" di "Titolo
  tre H3" messa in Calendar → Day: griglia, linea rossa, tocco sul
  quadratino delle 16:00 → finestra rapida → "Corsetta prova", Sage,
  Save → blocco verde; un secondo quadratino → "Studio prova" → More
  options → Daily, Peacock, inizio alle 16:00 (la fine passa da sola alle
  16:30) → Save → i due blocchi affiancati; giovedì e venerdì c'è solo
  "Studio prova"; nella settimana "Studio" da mercoledì a sabato e
  "Corsetta" verde; "Edit event" su "Studio prova" → Grape → viola; su
  "Corsetta prova" → All day → nella striscia in cima. Nel database: le
  due righe col colore, la data giusta e il valore del filtro (PS5). Poi
  le due righe di prova cancellate e la vista rimessa in Table: database
  uguale al backup, tranne la modalità del calendario della vista di
  prova (prima vuota, ora "Month", che è quella che si vedeva) e la sua
  data di modifica. Il calendario "Untitled" della stessa pagina,
  dell'utente, non è stato toccato

### 🔧 v0.129.1 — 30/09/2026 · 13:48 — "Wrap in the cell": vanno a capo anche i titoli delle colonne
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

L'utente, con la foto di una tabella di Notion (testi a capo dentro le
celle, anche nella riga dei titoli: "Campaign Name", "Content Type"),
parola per parola:
> «Continua a non funzionare quello che dicevo io dell'andare a capo nella
> stessa cella. Deve fare una cosa del genere che vedi nella foto»

- **[Bug fix]** Con "Wrap in the cell" andava a capo il testo delle celle
  ma non quello dei **titoli delle colonne**, che restava su una riga e si
  perdeva oltre il bordo: ora va a capo anche lui, e la riga dei titoli si
  alza quanto il più lungo, con le linee fra le colonne fino in fondo
- Controllato prima sul telefono che nelle celle funzionasse: sulla vista
  di prova, col modo acceso, il nome "Esempio nuovo con un titolo lungo" va
  su tre righe dentro la cella mentre lo si scrive, e tutta la riga si
  allunga. Nel database dell'utente nessuna vista aveva il modo acceso;
  "SuduS al contrario" ha "Fit to content" su Number e Date (quelle colonne
  si allargano quanto il testo, quindi non vanno a capo) e adesso mostra la
  sua vista Board: non toccato
- Verificato sul telefono sulla vista di prova, con la colonna Text
  rinominata per prova "Text di prova lungo" e ristretta a 80: il titolo va
  su quattro righe e la riga dei titoli si alza; la data col periodo va su
  due righe e la riga si allunga. Poi nome, larghezza e modo rimessi:
  database uguale, tranne "Last edited" di "Esempio nuovo" e le date di
  modifica

### 🆕 v0.129 — 30/09/2026 · 13:34 — "Color columns": la bacheca colorata come su Notion
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

L'utente, con la foto di una bacheca di Notion (colonne "1", "2", "3" e "No
Select", ognuna tinta del colore del suo tag), parola per parola:
> «Io intendevo un qualcosa del genere per i colori del Board»

- **[Nuova funzionalità]** "Color cards" diventa "**Color columns**" e fa
  come Notion: acceso, **la colonna intera** ha un velo del colore del suo
  tag; in cima c'è il tag stesso (la pastiglia colorata) e il numero delle
  pagine nel suo colore; le schede hanno un velo più carico e un bordino,
  con le scritte di sempre; "New" è largo quanto la colonna, col bordo e la
  scritta del colore. La colonna senza valore è grigia, come il colore
  "Default". Spento, la bacheca è quella di sempre. Prima, acceso, le
  schede erano piene del colore del tag
- **[Progetto]** Nessun cambio di schema (la stessa
  `pages.colorBoardCards`). I colori si mescolano allo sfondo della pagina
  (`lerp`): colonna 25%, schede 45%, bordi 65%; il numero e "New" sono il
  colore del tag schiarito del 35%. Così si leggono anche col tema chiaro
- Verificato sul telefono sulla vista "Videogiochi" di "Titolo tre H3"
  messa in Board, col filtro allargato per prova a PC, SWITCH e XBOX:
  colonne PS5 blu, PC grigia, SWITCH rossa, XBOX verde, coi loro tag in
  cima, le schede più cariche col bordino e "New" colorato. Poi filtro di
  nuovo su PS5, "Color columns" spento e vista rimessa in Table: database
  uguale, tranne la data di modifica della vista

### 🆕 v0.128 — 30/09/2026 · 13:23 — "Color cards": le schede della bacheca del colore del loro tag
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 326 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nella visualizzazione dei database Board, consentimi di far colorare i
> rettangolini delle pagine dello stesso colore di quello assegnato al
> Select usato per la board»

- **[Nuova funzionalità]** Nelle impostazioni di una bacheca, sotto "Group
  by", l'interruttore "**Color cards**" (per vista, spento di suo). Acceso,
  ogni scheda prende il colore del tag della sua colonna — quello scelto
  per il Select con cui la bacheca è raggruppata — e il nome e le altre
  proprietà sulla scheda diventano chiari come sui tag, per leggersi sul
  colore. La colonna delle pagine senza valore resta del colore di sempre
- **[Progetto]** **Database alla versione 48 → 49**:
  `pages.colorBoardCards`. `BoardCard` riceve il colore del gruppo e, se
  c'è, lo stende e schiarisce le scritte (`MaterialTheme` con
  `onBackground` e `onSurfaceVariant` chiari). Test in `WrapCellsTest` e
  `MigrationTest` 48→49, col file `v48.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 49
  senza errori) sulla vista "Videogiochi" di "Titolo tre H3" messa in
  Board: l'interruttore c'è sotto "Group by"; acceso, le tre schede della
  colonna PS5 diventano blu come il tag PS5 (#3A6590) e si leggono. Con
  PC, SWITCH e XBOX aggiunti per prova al filtro della vista, ogni colonna
  ha le schede del suo colore: grigio, rosso, verde. Poi (dopo che il
  telefono si era scollegato e ricollegato) filtro di nuovo su PS5, "Color
  cards" spento e vista rimessa in Table: database uguale alla copia,
  tranne la colonna nuova e la data di modifica della vista

### 🆕 v0.127 — 30/09/2026 · 10:49 — "Long text": tre puntini, colonna allargata o a capo; il riquadro grigio centrato
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 324 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «1) La barra in background con il grigiume, centramale alla posizione
> dove si trova la cella, proprio sopra di essa;
> 2) Ecco vedi, non mi sono spiegato bene io. L'opzione consente:
> * o di vedere i tre puntini se il testo va oltre la larghezza;
> * o di allargare la cella quanto basta per vedere tutto il testo;
> * o di far vedere il testo sotto la stessa cella andando a capo ma
>   rimanendo sempre nella stessa cella, come se all'interno di essa ci
>   fossero più righe. In questa situazione, l'altezza della riga deve
>   essere per tutta la riga, non solo per la cella dove si trova il testo»

- **[Nuova funzionalità]** L'interruttore "Wrap cells" diventa la voce
  "**Long text**" con tre scelte, nell'ordine chiesto: "**Show …**" (tre
  puntini, di suo; toccando la cella compare il testo intero), "**Widen
  the column**" (ogni colonna si allarga quanto basta perché si legga
  tutto, nome e intestazione compresi; più stretta di così non va, più
  larga resta come la si è fatta) e "**Wrap in the cell**" (il testo va a
  capo dentro la cella, e **tutta la riga** si allunga: le altre celle
  restano a metà della riga più alta)
- **[Bug fix]** Il riquadro grigio col testo intero è **centrato sulla
  cella**, in orizzontale e in verticale, proprio sopra di lei (prima
  partiva dal suo angolo in alto a sinistra); per il nome, centrato su
  tutta la cella, OPEN compreso
- **[Progetto]** `CellOverflow` (TRUNCATE, WIDEN, WRAP) in
  `PageEntity.cellOverflow`, che sta nella stessa colonna `wrapCells` della
  versione 48 (0 e 1 erano "no" e "sì", cioè i tre puntini e l'a capo):
  nessuna migrazione, schema di Room identico. `shownColumnWidth` fa le
  larghezze di "Widen the column"; test in `WrapCellsTest`
- Verificato sul telefono sulla vista "Videogiochi" di "Titolo tre H3", con
  "Sdrogo" allungato per prova a "Sdrogo lungo di prova ancora di piu":
  coi tre puntini, toccandolo il riquadro grigio compare centrato sulla
  cella; con "Widen the column" la colonna Text mostra tutto il testo e la
  data col periodo si legge intera; con "Wrap in the cell" il testo va su
  due righe dentro la cella e tutta la riga si allunga. Poi di nuovo "Show
  …" e "Sdrogo" rimesso dal riquadro: database uguale, tranne "Last edited"
  della riga e la data di modifica della vista

### 🆕 v0.126 — 30/09/2026 · 10:35 — "Wrap cells", i tre puntini e il testo intero di una cella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 322 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Concedimi di far sì, tramite l'attivazione di un'opzione nelle settings
> di ogni database, che tutto ciò che è scritto in una cella possa entrare
> in uno spazio più alto, dipendentemente da quanto scrivo. Mi spiego
> meglio: quando io allargo o rimpicciolisco la larghezza delle colonne,
> tramite l'opzione nuova che metterai nelle setting dei database, posso
> scegliere se il testo scritto in quella cella (che in realtà può non
> essere visualizzato perché ho rimpicciolito troppo) verrà visualizzato in
> uno spazio più alto (per far sì che tutto quanto venga visualizzato)
> oppure con i tre puntini di sospensione per indicare che il titolo di
> quella pagina (o qualsiasi cosa scritta in qualsiasi cella) continua.
> Toccando ogni cella dove c'è del testo che continua e che di base non può
> essere completamente visto, mi fa apparire un piccolo grigiume in
> background (sulla stessa identica posizione dove c'era quel testo) sul
> quale fa apparire il testo completo. Ovviamente, tutte le celle sulle
> quali io posso scrivere qualsiasi cosa, avranno un minimo di larghezza
> meno del quale non si può rimpicciolire»

- **[Nuova funzionalità]** Nelle impostazioni di una tabella, sotto
  "Group", l'interruttore "**Wrap cells**" (per vista, come le larghezze).
  Acceso: il testo delle celle va a capo e **la riga si allunga** quanto la
  sua cella più alta (nome, testo, numero, collegamento, email, telefono,
  data, durata, "Page link", cella colorata, data di creazione e modifica;
  i tag vanno a capo uno sotto l'altro); le linee fra le colonne seguono
  tutta la riga. Scrivendo in una cella, il testo va a capo lì dentro
- **[Nuova funzionalità]** Spento (di suo): quello che non ci sta finisce
  coi **tre puntini**. Toccando una cella di testo, di numero, di
  collegamento, email o telefono, o il nome di una riga, col testo tagliato,
  compare **nello stesso punto un riquadro grigio col testo intero**, dove
  si scrive come nella cella; si chiude con "Done" della tastiera, toccando
  fuori o col gesto indietro. Se il testo ci sta, si scrive nella cella
  come prima (col cursore in fondo). La data, la durata e "Page link"
  tagliate si aprono col loro tocco di sempre, che le mostra intere
- **[Nuova funzionalità]** **Larghezza minima** delle colonne in cui si
  scrive: 80 per testo, numero, collegamento, email, telefono e cella
  colorata, 120 per Name (c'è OPEN). Né trascinando il segnalino né con "Fit
  to content" si va sotto; le misure salvate prima, più strette, si
  allargano fin lì. Le altre colonne come prima (48; "Image" resta 44)
- **[Progetto]** **Database alla versione 48**: `pages.wrapCells`. Nuovo
  `CellText.kt`: `LocalWrapCells`, `TextLikeCell` (le celle in cui si
  scrive: testo semplice finché non le si tocca), `FullTextPopup` (il
  riquadro grigio). In "Wrap cells" la riga della tabella è alta quanto la
  cella più alta (`IntrinsicSize.Min`). `minColumnWidth` in
  `ColumnWidths.kt`. Test in `WrapCellsTest`, `CellTextRenderTest`
  (disegnata davvero: i puntini, il riquadro, la scrittura, l'a capo) e
  `MigrationTest` 47→48, col file `v47.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 48
  senza errori) sulla vista "Videogiochi" di "Titolo tre H3": la data col
  periodo finisce coi puntini; Text ristretta col segnalino si ferma a 80;
  "Sdrogo" allungato per prova diventa "Sdrogo lu…", toccandolo compare il
  riquadro grigio lì sopra col testo intero e la tastiera; cancellata la
  parte in più nel riquadro, "Done" lo chiude. "Wrap cells" acceso: la data
  va su due righe e la riga si allunga, nome, OPEN e tag a metà. Poi tutto
  rimesso com'era: database uguale, tranne la colonna nuova e "Last
  edited" della riga "Esempio nuovo"

### 🆕 v0.125 — 30/09/2026 · 10:18 — Ritoccando un giorno col pallino, il pallino se ne va
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 316 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nella selezione della data del calendario nella proprietà Date, se
> ritocco un giorno che ho toccato e che è visualizzato con un pallino
> arancione, mi toglie il pallino»

- **[Nuova funzionalità]** Nel calendario della proprietà Date, toccando
  di nuovo il giorno scelto (col pallino arancione) il pallino se ne va.
  Con "End date" acceso: ritoccando la fine resta solo l'inizio;
  ritoccando l'inizio resta solo la fine. Senza più niente scelto, "Done"
  svuota la cella, come prima. Gli altri tocchi sono quelli di sempre
- **[Progetto]** La regola sta in `pickCalendarDay` (`DayPick.kt`), usata
  da `DateEditorSheet`; test in `DayPickTest`
- Verificato sul telefono sulla data della riga "2" del database di prova
  (il 15/09): ritoccato il 15, il pallino sparisce; con "End date" e il
  periodo 15-18, ritoccando il 18 resta il 15, ritoccando il 15 resta il
  18. Poi "Cancel": database identico

### 🆕 v0.124 — 30/09/2026 · 10:11 — "Today" nel calendario delle date
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 313 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nella selezione della data del calendario nella proprietà Date, mettimi
> Today, che mi porta al giorno corrente»

- **[Nuova funzionalità]** In alto a destra del calendario, accanto alle
  frecce del mese, c'è "**Today**", in arancione come il numero di oggi:
  riporta il calendario al mese di oggi, anche dalle rotelle di giorno,
  mese e anno (che si chiudono). Non sceglie il giorno: lo si tocca come
  gli altri, così serve anche per arrivare, per esempio, a domani. Vale in
  ogni calendario dell'app (la proprietà Date, "Set date" della durata e
  della ripetizione), perché è lo stesso
- **[Progetto]** In `SamsungCalendar` (`SamsungPickers.kt`)
- Verificato sul telefono sulla data della riga "2" del database di prova:
  da giugno 2026 "Today" riporta a settembre 2026 col 30 segnato; dalle
  rotelle portate al 2021, "Today" chiude le rotelle e torna a settembre
  2026. Poi "Cancel": database identico

### 🆕 v0.123 — 30/09/2026 · 09:04 — In una pagina vuota il cursore va sulla prima riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 313 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «In un campo vuoto o nuovo, se premo in un punto a casa della pagina,
> fai sì che in realtà il text cursor mi vada al primissimo blocco, e non
> mi fa creare il text cursor lì in fondo, cosicché non ci sia un spazio
> vuoto sopra inutilissimo»

- **[Nuova funzionalità]** Le **righe vuote in fondo alla pagina** (i
  paragrafi senza testo dopo l'ultima cosa scritta; in una pagina vuota,
  tutte) non prendono più il cursore dove cade il dito: toccandone una, o
  toccando sotto l'ultima riga, il cursore va sulla **prima** di loro. In
  una pagina vuota o nuova è la primissima riga, e sopra non resta vuoto.
  Una riga vuota in mezzo al testo resta toccabile come sempre (è uno
  spazio voluto), e l'Invio continua a fare spazio: vale solo per lo
  spostamento del cursore, non per il testo che cambia. Un punto elenco
  vuoto non è una riga vuota
- **[Progetto]** `PageEditorViewModel.trailingBlankLines` e
  `trailingBlankRedirect` (coi testi del campo appena scritti, che il
  database non ha ancora); `focusLastBlock` va sulla prima riga vuota in
  fondo invece che sull'ultima; nel campo unito lo spostamento del solo
  cursore su una di quelle righe è portato sulla prima. Test in
  `BlankTapTest`
- Verificato sul telefono sulla pagina della riga "2" del database di
  prova (una riga vuota; My Space non toccata). Con 5 righe vuote fatte
  con l'Invio: toccando sotto tutte, la "Z" scritta finisce sulla prima
  riga (prima andava sulla quinta); toccando la quarta riga, la "W"
  finisce sulla prima; con la "W" scritta, toccando di nuovo la quarta la
  "V" va sulla seconda, subito sotto. Poi la pagina rimessa com'era, una
  riga vuota: database uguale, tranne 3 voci di "Updates" di quella pagina

### 🔧 v0.122.1 — 30/09/2026 · 08:44 — Il corpo del titolo di un toggle ora cambia
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 308 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Non mi modifica la grandezza del font di un titolo del toggle»

- **[Bug fix]** Riprodotto sulla pagina di prova: selezionato il titolo di
  un toggle e scelto 30 in "Font size" dalla barra Aa, la barra diceva 30
  e nel database c'era 30, ma il titolo restava piccolo; alla prima
  lettera scritta il 30 spariva anche dal database. Il titolo di un toggle
  (come la casella da spuntare e la notifica) ha un campo suo che tiene la
  sua copia della formattazione, e quella salvata la rileggeva solo senza
  il fuoco: la finestrella del corpo il fuoco non lo toglie. Ora font,
  corpo e colore messi dalla barra Aa e dal pennello arrivano subito anche
  a quel campo, e restano scrivendo
- **[Bug fix]** Col titolo grande, la freccetta del toggle (e la casella,
  e la campanella) scende a metà della prima riga invece di restare in
  cima
- **[Progetto]** `PageEditorViewModel.externalSpans`: il blocco e i suoi
  span nuovi, da `transformRun` (font e corpo) e `applyColorToRun`
  (colore); il campo dei blocchi a sé li prende se il testo è lo stesso.
  `markerDrop` nel campo: metà dell'allungamento della riga. Test in
  `ToggleTitleSizeTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (My Space
  non toccata; il toggle "SaaS" lì è dell'utente): prima della correzione
  il bug si ripete uguale. Dopo: "Provax" a 30 si ingrandisce subito,
  scrivendo "y" resta a 30 (`[{"text":"Provaxy","size":30}]`), la
  freccetta sta a metà del titolo. Poi il toggle rimesso com'era, una
  riga vuota: database uguale, tranne 3 voci di "Updates" della pagina di
  prova

### 🆕 v0.122 — 30/09/2026 · 08:33 — Nuova proprietà "Duration": quanto passa fra due date
*(Claude Code sul PC; verificato sul telefono; 306 test passati; commit
solo in locale)*

L'utente, parola per parola:
> «Fammi una proprietà chiamata Duration che mi consente di calcolare la
> differenza in anni, mesi, giorni, ore e minuti tra una data e l'altra.
> Quando tocco una nuova colonna e seleziono Duration, mi chiederà
> ovviamente di selezionare la data di inizio (che personalmente e
> manualmente selezionerò io) e la data di fine (stessa cosa). Utilizziamo
> un esempio per tutti i casi, ossia data di inizio 12 febbraio 1996 00:00,
> data di fine sarà 29 settembre 2026 21:22. Nelle impostazioni di questa
> proprietà, dammi la possibilità di mostrare:
> * in anni (se è possibile), omettendo ogni decimale e il resto di mesi,
>   giorni eccetera. Duration sarà 30 anni;
> * in anni e mesi (se è possibile), omettendo ogni decimale e il resto in
>   giorni, ore eccetera. Duration sarà 30 anni e 8 mesi;
> * in anni, mesi e giorni (se è possibile), omettendo ogni decimale e il
>   resto in ore e minuti. Duration sarà 30 anni, 8 mesi e 17 giorni;
> * in anni, mesi, giorni e ore (se è possibile), omettendo ogni decimale e
>   il resto in minuti. Duration sarà 30 anni, 8 mesi, 17 giorni e 21 ore;
> * in anni, mesi, giorni, ore e minuti (se è possibile). Duration sarà 30
>   anni, 8 mesi, 17 giorni, 21 ore e 22 minuti»

- **[Nuova funzionalità]** Nuovo tipo di proprietà **Duration** (dopo
  Date nell'elenco dei tipi). Toccando una cella si apre la sua finestra:
  in cima quanto dura, sotto "Start date", "Start time", "End date", "End
  time", ognuna col suo selettore (il calendario, con le rotelle di giorno,
  mese e anno toccando il nome del mese, e l'orologio). L'ora parte da
  00:00, la data da oggi. "Done" salva solo con tutte e due le date e la
  fine dopo l'inizio (se no avvisa "The end comes before the start");
  "Clear" svuota la cella
- **[Nuova funzionalità]** In "Edit property", "**Show as**" con le cinque
  scelte chieste, ognuna con l'esempio dell'utente sotto: "Years" (30
  years), "Years and months", "Years, months and days", "Years, months,
  days and hours", "Years, months, days, hours and minutes" (di suo). Il
  resto si lascia fuori, senza decimali. Le unità a zero si saltano ("1
  year and 17 days"); tutte a zero, lo zero dell'ultima ("0 years")
- **Nota sull'esempio**: dal 12/02/1996 00:00 al 29/09/2026 21:22 i mesi
  sono **7**, non 8: al 12/02/2026 sono 30 anni, al 12/09/2026 altri 7
  mesi, poi 17 giorni fino al 29/09. Con 8 mesi si arriverebbe al
  12/10/2026. L'app scrive quindi "30 years, 7 months, 17 days, 21 hours
  and 22 minutes"
- **[Nuova funzionalità]** La durata è scritta nella lingua dell'app
  ("30 anni, 7 mesi e 17 giorni" in italiano, col singolare per 1; in
  cinese e giapponese attaccata, "30年7か月17日"). La colonna nasce larga
  320 perché ci stia tutto l'esempio; "Fit to content" misura la durata.
  Le righe si ordinano per quanto durano (senza la fine in fondo); non si
  raggruppa né si filtra per durata (il valore sono due istanti). Nelle
  schede e nelle proprietà in cima alla pagina di una riga si legge la
  durata
- **[Progetto]** **Database alla versione 47**:
  `database_columns.durationFormat` (null = fino ai minuti). Il valore
  della cella è quello di una data col periodo e l'ora
  (`encodeDateRange`, "inizio|fine|t"): una colonna Date con un periodo,
  cambiata in Duration, mostra subito quanto dura, e al contrario. I conti
  in `Duration.kt` (`durationBetween`: i mesi contati tutti insieme
  dall'inizio, sull'ora dell'orologio, così il giorno del cambio dell'ora
  legale resta un giorno), le parole in `DurationWords.kt`, la cella e la
  finestra in `DurationCell.kt`, "Show as" in `NumberProperty.kt`. Test
  in `DurationTest`, `DurationColumnTest` e `MigrationTest` 46→47, col file
  `v46.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 47
  senza errori) sulla vista collegata "Videogiochi" di "Titolo tre H3": col
  "+" il tipo Duration c'è e la colonna nasce; nella cella della riga "1"
  scelti 12/02/1996 00:00 e 29/09/2026 21:22 (anno, mese e giorno con le
  rotelle); la finestra e poi la cella scrivono "30 years, 7 months, 17
  days, 21 hours and 22 minutes". Le cinque scelte di "Show as" cambiano la
  cella in "30 years", "30 years and 7 months", "30 years, 7 months and 17
  days", "30 years, 7 months, 17 days and 21 hours" e di nuovo completa.
  Dopo la prova sono cambiati due dettagli: la data non ancora scelta ora
  parte da oggi (prima partiva dall'altra data, cioè dal 1996 per la fine)
  e la colonna nasce larga 320 invece di 260 (a 260 la durata completa si
  troncava). Poi la colonna riportata alla sua larghezza e cancellata: il
  database è uguale alla copia, tranne "Last edited" della riga "1", la
  data di modifica della vista e le modifiche fatte intanto dall'utente
  (il toggle "SaaS" in My Space, una data e le colonne di "SuduS al
  contrario")

### 🆕 v0.121 — 29/09/2026 · 20:32 — La colonna Name si sposta dove si vuole
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 291 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Permettimi di spostare ovunque io voglia anche la colonna Name, ossia
> quella dove sono presenti le pagine (dei database complessi) o le voci
> (dei database semplici)»

- **[Nuova funzionalità]** Tenendo premuta l'intestazione **Name**, in cima
  al menu ci sono "**Move to left**" e "**Move to right**", come nelle altre
  colonne. Name si sposta di un posto alla volta, con le sue celle, i
  pulsanti OPEN, le sotto-righe e il suo calcolo; le altre colonne le
  passano accanto con i loro "Move to left/right". Le voci sono grigie
  quando Name è già la prima o l'ultima, e con "Lock view"
- **[Nuova funzionalità]** Il posto di Name vale per **tutte le viste** del
  database, come l'ordine delle colonne: spostata da una vista collegata,
  si sposta anche nel database. Le colonne nascoste non contano (Name salta
  quelle che non si vedono). Una proprietà nuova nasce sempre all'estrema
  destra, accanto al "+", anche con Name in fondo. "Resize column" su Name
  mette il segnalino sul suo bordo destro, dovunque stia. Copiando il
  database, Name resta dov'è
- **[Progetto]** **Database alla versione 46**: `pages.nameColumnOrder`, sul
  database che ha i dati: Name sta subito prima della prima colonna con
  `orderIndex` uguale o più alto; -1 (tutti i database di prima) = la
  prima. Così spostando Name le colonne non cambiano numero
  (`NameColumn.kt`: `namePosition`, `tableKeys`, `nameOrderAt`).
  `DatabaseTableState.orderAfterMove` prende il posto di
  `neighbourForMove`; `moveColumn` accetta `NAME_COLUMN_KEY` e salva
  colonne e Name insieme (`DatabaseRepository.reorderColumns`). Nella
  tabella intestazione, righe e calcoli mettono Name al suo posto con
  `TableSlots`. Test in `NameColumnMoveTest` e `MigrationTest` 45→46, col
  file `v45.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 46
  senza errori) sulla vista collegata "Videogiochi" della pagina di prova
  "Titolo tre H3": nel menu di Name "Move to left" è grigio; "Move to
  right" la porta dopo Select, poi dopo Text (il calcolo "NOT EMPTY" resta
  sotto Text), poi in fondo dopo Date, dove "Move to right" diventa grigio.
  "Resize column" lì mette il segnalino sul bordo destro di Name; il gesto
  indietro lo chiude. Tre "Move to left" la riportano prima. Il database è
  uguale alla copia, tranne la colonna nuova `nameColumnOrder` (-1) su ogni
  pagina

### 🔧 v0.120.1 — 29/09/2026 · 20:00 — Divisori: cancellando le righe vuote sotto, il divisore resta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 281 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ho scoperto un bug: stavo sulla riga sopra Titolo uno H1 e cancellando
> lo spazio vuoto fino al divider accade che me lo cancella. Prova a
> simularlo tu per capire, e risolvimi questo bug»

- **[Bug fix]** Riprodotto sulla pagina di prova: divisore, riga vuota,
  "Titolo"; col cursore sulla riga vuota, il backspace **toglieva il
  divisore** e la riga vuota restava (la regola messa per "sotto un divider
  non posso cancellare nulla", letta male: si volevano cancellare le righe
  vuote, non il divisore). Ora sulla **riga vuota** subito sotto un
  divisore il backspace **toglie la riga**: il divisore resta, la riga di
  sotto sale al suo posto col cursore all'inizio, la tastiera resta aperta
- **[Bug fix]** Su una **riga col testo** subito sotto un divisore, come su
  Notion: il primo backspace "**prende**" il divisore, che si **accende
  d'azzurro**, e solo il secondo lo toglie. Così continuando a premere il
  divisore non sparisce senza avvisare. Dopo 3 secondi senza il secondo
  backspace si spegne da solo. Una riga vuota che è l'ultima della pagina
  sotto un divisore resta dov'è
- **[Progetto]** `PageEditorViewModel.backspaceUnderDivider`,
  `armedDivider` (acceso per `ARMED_DIVIDER_MS`), `DividerBlockContent` lo
  disegna acceso; test in `DividerBackspaceTest`
- Verificato sul telefono, sulla pagina di prova (My Space non toccata):
  prima della correzione il bug si ripete uguale. Dopo: la riga vuota se
  ne va e il divisore resta; una "K" scritta finisce all'inizio di
  "Titolo"; il backspace lì accende il divisore senza toglierlo; dopo 3
  secondi è di nuovo normale; due backspace di fila lo tolgono. Poi la
  pagina rimessa com'era (database e 22 righe vuote): uguale, tranne una
  riga vuota rifatta e 2 voci di "Updates"

### 🆕 v0.120 — 29/09/2026 · 19:47 — "Image" a tutto schermo, e il gesto indietro chiude il segnalino delle colonne
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 278 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Sì, fai che toccandola mi permetta di decidere se vederla a tutto
> schermo o cambiarla.
> Un'altra cosa: quando modifico la larghezza di una colonna, permettimi di
> usare la gesture per andare indietro per far sì che io possa finire le
> modifiche»

- **[Nuova funzionalità]** Toccando un'immagine di una cella "Image" si
  sceglie: "**View full screen**", "Upload" per cambiarla, "Remove". A
  tutto schermo l'immagine si vede intera sul nero; con due dita la si
  ingrandisce e la si sposta, con due tocchi torna com'era; si chiude con
  la X o col gesto indietro. Una cella vuota apre direttamente "Upload"
- **[Nuova funzionalità]** Col segnalino di "Resize column" acceso, il
  **gesto indietro** lo chiude e lascia la tabella com'è, invece di uscire
  dalla pagina. Senza segnalino il gesto fa quello di sempre
- **[Progetto]** `PageImageSheet` ha la voce facoltativa `onView`;
  `FullScreenImage` in `ImageCell.kt`; `BackHandler` in `TableLayout`
  finché il segnalino c'è
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): col segnalino su Select, il gesto indietro lo toglie e
  si resta sulla pagina. Con un'immagine di prova in una colonna "Image"
  creata apposta: il tocco apre "View full screen" / "Upload" / "Remove";
  a tutto schermo si vede intera; si chiude col gesto indietro e con la X.
  L'ingrandimento a due dita non si può fare dal PC: da provare col dito.
  Poi immagine tolta, proprietà cancellata, immagine di prova tolta dal
  telefono: database uguale a prima tranne "Last edited" della riga "1"

### 🆕 v0.119 — 29/09/2026 · 19:37 — Nuova proprietà "Image": una cella quadrata con un'immagine
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 278 test
passati; commit solo in locale)*

L'utente, parola per parola (con la foto del Pokédex di Notion, dove la
seconda colonna ha le figure dei Pokémon):
> «Nuova proprietà: anche se si vedrano piccole, questa nuova proprietà
> chiamata Image sarà una cella a forma di quadrato dove si vedrà
> un'immagine che metto come icona ma con la grandezza di immagine normale
> (guarda l'esempio del database dei Pokémon nella foto, ossia la seconda
> proprietà da sinistra, ossia le immagini nella colonna con il quadratino
> e il pennello dentro)»

- **[Nuova funzionalità]** Fra i tipi di proprietà c'è "**Image**". La
  colonna nasce **quadrata**, larga quanto è alta una riga; la cella mostra
  l'immagine **intera**, piccola, senza ritagliarla (una figura più alta
  che larga si vede tutta, col suo fondo). Toccando la cella si apre la
  stessa finestra dell'icona di una pagina: "**Upload**" dalla galleria e,
  se c'è già, "**Remove**". L'immagine si salva **com'è**, non rimpicciolita
- La colonna si allarga col segnalino come le altre (e vicino al quadrato
  ci si riaggancia); "Fit to content" la tiene quadrata. Nelle proprietà in
  cima alla pagina di una riga c'è lo stesso riquadro, e nelle schede di
  bacheca, galleria e lista una miniatura. Non si raggruppa e non va nel
  grafico
- **[Progetto]** `ColumnType.IMAGE`, il valore è il nome del file in
  `page-images`; `ImageCell.kt`; `PageImage` ora sa anche mostrare
  un'immagine intera (`ContentScale.Fit`). Ogni cella ha il suo file:
  copiando una riga o un database il file si copia, così togliendo
  l'immagine da una riga non sparisce dall'altra; "Remove" cancella il
  file. Test in `ImageColumnTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima), con un'immagine di prova creata apposta (200×300, una
  Poké Ball su fondo blu) messa in una cartella a parte del telefono: la
  colonna "Image" nasce quadrata; "Upload", scelta dall'album di prova, la
  mostra intera nella cella; il file salvato è identico all'originale.
  "Remove" la toglie e cancella il file (la cartella delle immagini è
  tornata com'era). Poi la proprietà cancellata e l'immagine di prova tolta
  dal telefono: il database è uguale a prima, tranne "Last edited" della
  riga "1". (Durante la prova il mio script ha creato per sbaglio una
  proprietà Text vuota, subito cancellata)

### 🆕 v0.118 — 29/09/2026 · 19:23 — Life progress: le barre riempiono il widget
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 273 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Allargami le barre della Life progress bar fino a che riempiono la loro
> sezione dedicata (ovviamente fai rispettare il limite se attivo i
> decimali)»

- **[Nuova funzionalità]** La scritta accanto alle barre non ha più una
  larghezza fissa (112, più del necessario, con uno spazio vuoto a
  destra): è **larga quanto la più lunga del widget**, e tutto il resto va
  alle barre, che ora arrivano fin lì. La si misura **al 100%**
  ("Month: 100%"), il caso peggiore, così non cambia mentre le percentuali
  salgono: le barre non si spostano e restano lunghe uguali
- **Col decimale** la misura è "100,0%": le barre si accorciano di quel
  tanto, e il numero ci sta sempre. Un nome lunghissimo di una data si
  ferma a 140 e si accorcia coi puntini
- Verificato sul telefono: le barre passano da circa 540 a 690 pixel,
  subito prima di "Year: 74%"; col decimale 660, tutte uguali, con
  "Month: 96,0%" intero. Il decimale lasciato spento, com'era

### 🆕 v0.117 — 29/09/2026 · 19:19 — Life progress: il decimale sulle percentuali, da accendere e spegnere
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 273 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Fai sì che io possa attivare o disattivare il decimale sui numeri della
> Life progress bar nella Sidebar»

- **[Nuova funzionalità]** Nel menu "⋯" del widget **Life progress** della
  barra laterale c'è "**Show decimals**", con l'interruttore: acceso, le
  percentuali hanno un decimale ("Year: 74,3%" invece di "Year: 74%"), col
  separatore scelto nelle impostazioni (74,3 o 74.3); spento, tornano
  intere. Si conta sempre per difetto, così il 100% arriva solo alla fine.
  Vale per tutte le barre del widget, anche le date aggiunte a mano, e
  resta com'era riaprendo l'app
- Col decimale le scritte si allungano di ",0": le barre si accorciano
  tutte di quel tanto, così restano lunghe uguali
- **[Progetto]** `LifeProgressWidget.decimals` (un widget salvato prima
  parte spento) e `progressPercentText`; test in `WidgetsTest`
- Verificato sul telefono: acceso, "Year: 74,3%", "Month: 95,3%",
  "Week: 22,7%", "Day: 59,1%", barre tutte uguali; ancora acceso dopo aver
  riaperto l'app; spento, di nuovo "Year: 74%". Lasciato spento, com'era

### 🆕 v0.116 — 29/09/2026 · 14:06 — Nuova proprietà "Page link": il collegamento a un'altra pagina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 271 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nuova proprietà: collegamento ad un'altra pagina. In un database, quando
> seleziono la nuova proprietà chiamata "Page link", posso cercare tramite
> una ricerca gerarchica nel Workspace (con la possibilità di ricercare
> all'interno di esso il nome della pagina scrivendo nella barra di
> ricerca) la pagina che potrà essere aperta se la tocco, e all'interno di
> quella cella ci sarà il collegamento a quella pagina (può anche essere
> all'interno dello stesso database dove sono contenuti sia la pagina da
> collegare sia la pagina a cui devo collegare la pagina originale)»

- **[Nuova funzionalità]** Fra i tipi di proprietà c'è "**Page link**"
  (icona ↗). Toccando la cella si apre "**Page link**": l'**albero del
  Workspace**, con le pagine che si aprono come nella barra laterale e i
  database che mostrano le loro righe, e in cima la **barra di ricerca**
  per trovare la pagina per nome (sotto ogni risultato, dove sta). Si
  sceglie qualsiasi pagina, **anche quella di un'altra riga dello stesso
  database**; una riga mai aperta si sceglie lo stesso, e la sua pagina
  nasce allora
- Nella cella si vede la pagina collegata, con la sua icona e il **titolo
  di adesso** (sottolineato): rinominata altrove, qui si aggiorna da sola.
  **Toccandola la si apre.** Toccando il resto della cella si cambia
  pagina, o la si toglie con "**Remove link**" in cima. Se la pagina non
  c'è più, "Page not found"
- Vale anche nelle **proprietà in cima alla pagina di una riga**, e nelle
  schede di bacheca, galleria e lista ("↗ titolo"). Si ordina per titolo;
  "Fit to content" misura il titolo. Non si filtra e non si raggruppa (i
  gruppi e le barre del grafico si chiamerebbero con un codice)
- **[Progetto]** `ColumnType.PAGE_LINK`, il valore è l'id della pagina;
  `PageLinkCell.kt` (`PageLinkHost` per aprire e scegliere,
  `LocalLinkedPages` per i titoli, che il ViewModel segue con
  `PageDao.observeByIds`). La scelta è quella di "Duplicate"/"Move to"
  (`DestinationPickerSheet`, ora con una voce per togliere). Copiando un
  database, un collegamento alla pagina di una sua riga passa alla copia di
  quella pagina. Nessun cambio di schema. Test in `PageLinkTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): aggiunta la proprietà "Page link" a "Videogiochi"; la
  cella di "1" apre l'albero, "Videogiochi" aperto mostra le sue righe,
  scelta "2": nella cella "2" sottolineato, e toccandolo si apre la pagina
  "2". Lì, nelle proprietà in cima, "Page link" cercando "Esempio" trova
  "Esempio nuovo — In Videogiochi", e la sceglie; toccando la cella fuori
  dal titolo "Remove link" la toglie. Poi la proprietà cancellata: il
  database è uguale a prima, tranne "Last edited" delle righe "1" e "2",
  che sono state scritte durante la prova

### 🔧 v0.115.1 — 29/09/2026 · 13:53 — Sotto-righe: "Move up" e "Move down" solo sotto la loro riga (verifica)
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; nessuna
modifica al codice; commit solo in locale)*

L'utente, parola per parola:
> «Ovviamente concedimi di spostare sopra e sotto anche le sub row, ma solo
> all'interno della principale riga alla quale appartengono le sub row (a
> patto che tu non lo abbia già fatto)»

- Era già così dalla voce precedente: una sotto-riga si sposta solo fra le
  sorelle, sotto la sua riga principale; sulla prima "Move up" è grigio,
  sull'ultima "Move down"
- Verificato sul telefono sulla pagina di prova "Titolo tre H3", in una
  vista "Prova" senza ordinamento creata apposta: "Alfa" e "Beta" sotto
  "1"; su "Alfa" (la prima) "Move up" grigio; "Move down" la porta sotto
  "Beta", sempre sotto "1", e le righe principali non si muovono; lì
  "Move down" diventa grigio (non passa sotto "2"). Poi rimessa su, le due
  sotto-righe cancellate e la vista tolta: il database è identico a prima

### 🆕 v0.115 — 29/09/2026 · 13:48 — Tabelle: le sotto-righe ("Create sub row")
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 266 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Un'idea: saresti in grado di fare le Sub-rows? Ti spiego con un esempio:
> se tengo premuto sulla riga dove scrivo marmellata, ho la possibilità di
> creare altre righe dove metterò i vari gusti, quindi albicocca, pesca,
> fragola, eccetera. Queste sub rows saranno attivate tenendo premuto su
> una riga e toccando "Create sub row". Ovviamente le sub row create devono
> essere correlate alla row principale dalla quale sono nate, e saranno
> apribili e chiudibili con una freccetta stile Toggle sulla riga
> principale, e saranno di per sé pagine a parte con le loro proprietà
> perfettamente funzionanti. Nella voce "Calculate" mettimi anche
> un'opzione che mi consente di calcolare o meno anche i dati delle sub
> rows, di default disattivata»

- **[Nuova funzionalità]** Tenendo premuto il nome di una riga della
  tabella, in cima alle azioni, "**Create sub row**": nasce una riga nuova
  **sotto quella**, rientrata, e il suo nome si apre subito per scriverlo.
  Si può fare anche su una sotto-riga (un livello più in là)
- Le sotto-righe sono **righe del database come le altre**: OPEN apre la
  loro pagina, le proprietà si scrivono nelle celle, compaiono anche nelle
  altre viste. Nascono coi valori dei gruppi della riga (per starle sotto
  anche in una tabella raggruppata) e col valore del filtro, come ogni riga
  nuova
- Sul nome della riga principale c'è la **freccia** (▾ / ▸) che apre e
  chiude le sue sotto-righe; resta com'era anche riaprendo l'app. Quando
  nella tabella c'è almeno una sotto-riga, ogni nome lascia il posto della
  freccia, così i nomi restano in colonna
- **"Move up" / "Move down"** spostano una sotto-riga fra le sue sorelle,
  una riga principale fra le principali (con le sue sotto-righe dietro)
- Cancellando una riga **le sue sotto-righe restano**, un livello più su:
  il messaggio di conferma lo dice
- **[Nuova funzionalità]** Nella finestra di **Calculate**, sotto i
  calcoli, l'interruttore "**Include sub rows**", **spento di suo**: i
  calcoli contano solo le righe principali; acceso, anche le sotto-righe.
  Vale per il calcolo di quella colonna
- **[Progetto]** **Database alla versione 45**: `database_rows.parentRowId`
  e `subRowsCollapsed`. L'albero lo fa `rowTree` (`SubRows.kt`): segue
  l'ordine della vista (filtro, ricerca, ordinamento, gruppi); una
  sotto-riga la cui riga non si vede (filtrata o in un altro gruppo) sta
  in cima, invece di sparire. Copiando un database le sotto-righe restano
  sotto le righe copiate. "Include sub rows" sta con gli altri calcoli
  della vista ("subrows:colonna"). Test in `SubRowsTest` e `MigrationTest`
  44→45, col file `v44.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 45
  senza errori) sulla pagina di prova "Titolo tre H3": "Create sub row" su
  "1" fa nascere la riga sotto, rientrata, col cursore e la tastiera in
  maiuscolo; scritto "Sotto". La freccia su "1" la chiude e la riapre. In
  fondo a Text, "NOT EMPTY" resta 100% (la sotto-riga, con Text vuoto, non
  conta); acceso "Include sub rows" diventa 75%. Il messaggio per
  cancellare "1" avvisa delle sotto-righe (annullato). Poi la sotto-riga
  cancellata e l'interruttore spento: il database è uguale alla copia,
  tranne la data di ultima modifica della vista collegata

### 🆕 v0.114 — 29/09/2026 · 13:35 — Tabelle: "Move up" e "Move down" tenendo premuto il nome di una riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 254 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Se tengo premuto su una cella della colonna Name, fai sì che ci sia
> un'opzione che mi consenti di spostare l'intera riga sopra o sotto»

- **[Nuova funzionalità]** Nelle azioni che si aprono tenendo premuto il
  nome di una riga della tabella, in cima, "**Move up**" e "**Move
  down**": la riga passa sopra quella che le sta sopra, o sotto quella che
  le sta sotto, **come la si vede** — con un filtro o una ricerca salta le
  righe nascoste, dentro un gruppo o un sottogruppo resta fra le righe di
  quel gruppo. La finestra resta aperta per spostarla ancora; in cima
  "Move up" è grigio, in fondo "Move down". Anche nel database semplice
- **Con un ordinamento** le due voci sono grigie, e sotto c'è scritto
  perché: le righe stanno dove dice l'ordinamento, spostarle a mano non si
  vedrebbe. Grigie anche col database bloccato
- Spostare una riga non cambia il suo "Last edited". L'ordine è quello del
  database, lo stesso in tutte le sue viste
- **[Progetto]** `DatabaseRepository.moveRowNextTo` (subito prima o subito
  dopo il vicino, poi le righe si rinumerano da 0 così non ci sono pari
  merito), `DatabaseDao.setRowOrderIndex`; la tabella dice in quale gruppo
  si è toccata la riga (`RowPlace`). Test in `RowMoveTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): nella vista "Videogiochi", ordinata per nome, le due
  voci sono grigie con la spiegazione. In una vista "Prova" creata apposta
  senza ordinamento: "2" con "Move down" finisce sotto "6" (la finestra
  resta aperta, "Last edited" non cambia) e con "Move up" torna su; sulla
  prima riga "Move up" è grigio. Raggruppata per Select, "Esempio nuovo"
  nel gruppo PS5 sale sopra "2" saltando le sei righe degli altri gruppi
  che nel database stanno in mezzo. Poi l'ordine rimesso com'era con "Move
  down" (in fondo la voce diventa grigia) e la vista tolta: il database è
  identico alla copia

### 🆕 v0.113 — 29/09/2026 · 13:26 — Tabelle: la larghezza delle colonne, col segnalino o adattata al contenuto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 250 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Permettimi di allargare o rimpicciolire la larghezza delle colonne,
> magari con un'opzione che mi fa attivare un segnalino che viene fuori
> tra la fine della colonna che voglio allargare/ rimpicciolire e l'inizo
> di quella a destra, come si fain Excel che mi permette di spostare verso
> destra per allargare la colonna o verso sinstra per rimpicciolirla.
> Mettimi anche un'opzione attivabile e disattivabile quando tengo premuto
> su una proprietà che mi consente di allargare fino a che nella colonna
> si vede tutto quello che c'è scritto, classica cosa che (anche se si fa
> manualmente) fa Excel quando faccio doppio click sul bordino di una
> colonna»

- **[Nuova funzionalità]** Tenendo premuta l'intestazione di una colonna
  (anche **Name**) c'è "**Resize column**": compare un **segnalino** blu
  sul bordo fra quella colonna e la successiva, e la linea del bordo si
  colora dall'intestazione all'ultima riga. Trascinato **a destra allarga**
  la colonna, **a sinistra la stringe**, dal vivo; lasciato, la misura
  resta. Il segnalino resta lì per altri ritocchi e se ne va toccando la
  tabella da un'altra parte. Vicino alla larghezza di partenza (150, Name
  200) si aggancia lì: si torna precisi a com'era
- **[Nuova funzionalità]** Nello stesso menu "**Fit to content**", con
  l'interruttore: acceso, la colonna è **larga quanto serve perché si legga
  tutto**, su una riga sola: l'intestazione, ogni cella delle righe mostrate
  (testo, tag, date, numeri, colori…) e il calcolo in fondo. Resta acceso:
  scrivendo, la colonna segue. Spento, torna la larghezza di prima;
  trascinare il segnalino lo spegne
- Le larghezze sono **di ogni vista**, come su Notion: una vista collegata
  o una vista in più hanno le loro. Con "Lock view" le due voci sono grigie
- **[Progetto]** **Database alla versione 44**: `pages.columnWidths`
  (JSON, dp per colonna; Name è "__name__") e `pages.fitColumns`. Vedi
  `ColumnWidths.kt`; nella tabella le larghezze le leggono intestazioni,
  celle e calcoli da `LocalTableWidths`. Copiando un database le larghezze
  passano alle colonne nuove. Test in `ColumnWidthTest` (il primo giro ha
  trovato un errore vero: salvare una larghezza avrebbe chiuso l'app) e
  `MigrationTest` 43→44, col file `v43.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 44
  senza errori) sulla pagina di prova "Titolo tre H3", vista collegata
  "Videogiochi": "Resize column" su Select fa comparire il segnalino fra
  Select e Text con la linea blu su tutte le righe; trascinato a sinistra
  la colonna si stringe (salvata a 78) e il segnalino la segue; toccando
  l'intestazione Name se ne va. "Fit to content" su Text la adatta a
  "Esempio 1" e, dopo la correzione, anche a "NOT EMPTY 100%" del calcolo,
  che prima si tagliava; su Name la stringe a "Esempio nuovo" + OPEN.
  Trascinando Select di nuovo a destra si è agganciata a 150. Poi tutto
  spento: il database è uguale alla copia, tranne la data di ultima
  modifica della vista collegata

### 🆕 v0.112 — 29/09/2026 · 13:02 — La maiuscola automatica, ovunque si scriva, solo a campo vuoto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 240 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Quando tocco una cella e premo su Create tag nelle colonne Select, mi fa
> scrivere in minuscolo. Fai sì che io possa scrivere automaticamente
> scriverci in maiuscolo. Anzi, una cosa, in tutte le situazioni in tutta
> l'app che proccano l'attivazione della tastiera (quindi ovunque io
> scriva), metti la possibilità che in automatico io debba scrivere in
> maiuscolo se la cella è vuota. Se già c'è scritto qualcosa, anche uno
> spazio vuoto che io manualmente ho scritto o qualsiasi cosa, allora no.
> Stessa cosa anche quando rinomimo il nome do una proprietà di una
> colonna»

- **[Nuova funzionalità]** **In ogni campo di testo dell'app** (38: "Create
  a tag", il nome delle proprietà, celle, titoli, nomi, ricerche, il testo
  delle pagine…) **a campo vuoto la tastiera parte in maiuscolo**; se c'è
  già qualcosa, anche solo uno spazio, no — nemmeno dopo ". ", dove la
  tastiera di solito la rimette. Nel testo di una pagina conta la riga:
  andando a capo, la riga nuova parte in maiuscolo. Restano come sono i
  campi di numeri, password, email, indirizzi e codici colore, e quello
  che chiede "DELETE" tutto maiuscolo
- **[Progetto]** `AutoCaps.kt`: la tastiera legge la richiesta di
  maiuscola solo quando parte, quindi la si cambia mentre si scrive e la
  si riavvia, **solo nei punti in cui lei metterebbe la maiuscola** (riga
  vuota, dopo uno spazio o un a capo): lì non c'è una parola a metà da
  perdere. Ogni campo sta dentro un `AutoCaps { }`: finestre, fogli e menu
  hanno ognuno la sua tastiera, e un'impostazione sola per la schermata
  non li raggiungerebbe. `AutoCapsTest` controlla la regola e che **ogni**
  campo dell'app sia avvolto: uno nuovo lasciato fuori fa fallire i test
- Verificato sul telefono, leggendo dal sistema cosa l'app chiede alla
  tastiera (maiuscola sì/no) e guardando la tastiera Samsung:
  "Create a tag" vuoto → maiuscolo; uno spazio → minuscolo; cancellato →
  di nuovo maiuscolo; "Ab. " → minuscolo (poi svuotato e chiuso senza
  creare nessun tag). Nome della proprietà "Select": pieno → minuscolo,
  svuotato → maiuscolo, poi "Cancel" (il nome è rimasto "Select"). Nella
  pagina di prova: riga vuota → maiuscolo, "Ab. " → minuscolo, a capo →
  maiuscolo. Poi tutto cancellato: il database è uguale a prima, tranne
  2 voci di "Updates" della pagina di prova e il numero d'ordine interno
  delle 16 righe vuote in fondo, spostato di uno (l'ordine che si vede è
  lo stesso). Le lettere sono state scritte via adb, che non passa dalle
  parole "in composizione" della tastiera: com'è quando si scrive col
  dito lo si vede usandola

### 🆕 v0.111 — 29/09/2026 · 12:48 — Le viste di un database: il "+" accanto a "Table" e l'elenco delle viste
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 230 test
passati; commit solo in locale)*

L'utente, parola per parola (con due foto di Notion: il "+" accanto alla
scheda "Table" e la finestra "New view"):
> «A destra del pulsante Table (o sulla vista usata in quel momento) in
> ogni database che abbia come default la vista Table (o sulla vista usata
> in quel momento) mettimi un + che mi consente di aggiungere
> visualizzazioni al database. Al posto di farmi vedere le varie
> visualizzazioni disponibili, quando tocco su Table (o sulla vista usata
> in quel momento) , mi fa vedere la lista di tutte le visualizzazioni
> create da me. Tanto la modifica della vista in quel momento è già
> presente in Settings (del database) e in Layout come prima opzione»

- **[Nuova funzionalità]** **Un database può avere più viste**, come su
  Notion: stesse pagine, ognuna con layout, filtro, ordinamento, gruppi,
  proprietà nascoste e grafico suoi. Vale per ogni database, dentro una
  pagina e a schermo intero, e per le viste collegate
- **[Nuova funzionalità]** **"+" subito a destra del nome della vista**:
  apre "**New view**" — il nome (vuoto = quello del layout) e i sette
  layout, gli stessi riquadri di "Layout" nelle impostazioni; "Done" crea
  la vista e la mette davanti. Parte da zero, come su Notion: niente
  filtro né ordinamento, tutte le proprietà in vista. Scelto "Chart", si
  apre subito la scelta del grafico
- **[Nuova funzionalità]** **Toccando il nome della vista** non si apre più
  la scelta del layout ma l'**elenco delle viste** ("Views"): la spunta su
  quella che si vede, un tocco per passare a un'altra, "Add view" in fondo.
  Dai tre puntini di ogni vista: "**Rename**" (anche quella di partenza) e
  "**Delete view**" (solo quelle aggiunte, con una conferma: le pagine
  restano nel database). Il layout della vista di adesso si cambia da
  Settings → Layout, come prima. La vista scelta resta scelta anche
  chiudendo l'app
- Col database bloccato o con "Lock view" le viste si guardano e si
  scelgono, ma il "+", Rename e Delete view sono spenti
- **[Progetto]** Per far stare il "+" dentro una pagina, i pulsanti della
  barra della vista sono passati da 40 a 36 dp e il riquadro del nome è
  un po' più stretto: prima "Table" diventava "Tab…". Un nome lungo si
  accorcia coi puntini senza spingere fuori le icone di destra
- **[Progetto]** **Database alla versione 43**: `pages.viewHostId`
  (la vista in più è una pagina fatta come una vista collegata, legata al
  database su cui è nata), `pages.viewName` e `pages.activeViewId`.
  Cancellando per sempre un database se ne vanno anche le sue viste;
  copiandolo, la copia ha solo la sua (le viste in più non si copiano).
  `DatabaseContent` ora sceglie la vista e disegna `DatabaseViewBody`;
  titolo, icona, preferito e "Show title" restano quelli del database.
  Test in `DatabaseViewsTest` e `MigrationTest` 42→43, col file `v42.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 43
  senza errori, 26 pagine) sulla pagina di prova "Titolo tre H3", vista
  collegata "Videogiochi" (Table, filtro PS5): il "+" accanto a "Table";
  "Prova vista" in Board, subito davanti con tutte le righe (anche le PC);
  l'elenco con la spunta; tornando a "Table" filtro e ordinamento erano
  intatti; rinominata in "Rinominata" e tolta; una vista Chart apre la
  scelta del grafico, e a schermo intero si vede la stessa vista. Per la
  vista di partenza c'è solo "Rename". Alla fine il database è identico
  alla copia di prima

### 🔧 v0.110.2 — 29/09/2026 · 08:38 — Divisori: il backspace sotto li toglie, e la riga spostata si illumina
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 219 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Sotto un divider, non posso cancellare nulla. Se sono sopra un diveder,
> e sposto verso il basso, non solo non mi sposta verso il basso, ma il
> divider cancella una riga e si sposta da sola verso l'alto»

- **[Bug fix]** Il backspace a inizio della riga subito sotto un divisore
  non faceva niente (riprodotto sul telefono: nel database non cambiava
  nulla). Ora **toglie il divisore**, come una riga orizzontale in un
  documento: la riga resta col suo testo e il cursore all'inizio, e un
  altro backspace la unisce a quella di sopra. Sotto un database, una
  tabella o un collegamento a una pagina resta com'era: lì dentro c'è roba
  da non buttare
- **[Bug fix]** Spostare con ↓ una riga oltre un divisore la spostava
  davvero (controllato nel database, anche su My Space: nessuna riga
  sparita), ma con le righe vuote non si vedeva: fra cinque righe vuote i
  primi tocchi sembravano non fare niente, e passando il divisore
  sembrava il divisore a salire mangiandosi una riga. Ora **la riga
  spostata si illumina** di azzurro per un attimo (0,7 s), vuota o no:
  si vede dov'è andata
- **[Progetto]** `PageEditorViewModel.removeDividerAbove` (da
  `mergeIntoIslandAbove`), `movedBlock` e il velo disegnato dal campo di
  testo condiviso (`MOVE_FLASH_MS`); test in `DividerBackspaceTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): "Sopra", divisore, "Sotto"; le lettere si cancellavano,
  a inizio riga il backspace no. Dopo: il divisore se ne va, il cursore
  resta sulla riga (una "K" scritta finisce lì) e la tastiera non si
  chiude. Registrato lo schermo spostando una riga vuota oltre il
  divisore: dal fotogramma dopo il tocco la riga è sotto, col cursore,
  in una fascia azzurra che svanisce; la pagina non salta. Poi la pagina
  rimessa com'era e il database confrontato con la copia: uguale, tranne
  3 voci di "Updates" della pagina di prova

### 🔧 v0.110.1 — 29/09/2026 · 08:21 — Col gesto indietro il cursore si spegne subito
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 216 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Quando c'è il text cursor attivato da qualsiasi parte e faccio la gesture
> per andare indietro, ci mette almeno un secondo per sparire»

- **[Bug fix]** Il gesto indietro con la tastiera aperta se lo prende la
  tastiera, e l'app spegneva il cursore quando se ne accorgeva: a tastiera
  **scesa del tutto** e poi dopo altri 450 ms (250 nel database a tutta
  pagina), l'attesa messa per la tastiera Samsung che sparisce e riappare
  nei cambi di campo. Misurato sul telefono con un registro dei tempi:
  la tastiera ci mette 290 ms a scendere, e il cursore si spegneva **750 ms**
  dopo che aveva cominciato. Ora, se il cursore è fermo nello stesso campo
  da più di 600 ms, la tastiera che **comincia** a scendere
  (`imeAnimationTarget`) vuol dire che l'ha chiusa l'utente: il cursore si
  spegne subito, insieme a lei. Subito dopo un cambio di campo, o con un
  cambio chiesto dall'app in viaggio, resta l'attesa di prima
- **[Progetto]** `KeyboardClose.kt`: `ClearFocusWhenKeyboardCloses`, usato
  dall'editor e dal database a tutta pagina, e `KeyboardCloseWatch`, che
  sa quando il cursore ha cambiato campo: lo segnano i campi di scrittura
  (`markFocusChanges`: titolo, campo di parcheggio, celle delle tabelle,
  titolo, celle e ricerca dei database) e l'editor a ogni cambio di riga.
  Contare invece i tocchi sullo schermo non andava: con la navigazione a
  gesti lo scorrimento dal bordo arriva all'app come un tocco
- Verificato sul telefono sulla pagina di prova "Titolo tre H3": dopo, il
  cursore si spegne ancora prima che la tastiera cominci a muoversi (prima
  750 ms dopo); lo stesso da una cella del database; titolo → cella "1" →
  cella "2" senza che la tastiera si chiuda; "/divider" col cursore che
  passa alla riga sotto e la tastiera sempre aperta. Poi le prove annullate
  e la pagina uguale a prima

### 🆕 v0.110 — 29/09/2026 · 08:07 — Sottogruppi: "Hide empty sub groups", separato dai gruppi
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 216 test
passati; commit solo in locale)*

L'utente, parola per parola, dopo aver descritto con un esempio i generi
come gruppi e le piattaforme come sottogruppi (con le piattaforme vuote
mostrate), alla proposta di separare l'interruttore:
> «Sì, separalo in due interruttori»

- **[Nuova funzionalità]** Sotto "Hide empty groups" c'è "**Hide empty sub
  groups**", solo quando i sottogruppi ci sono: nasconde (o mostra) i
  sottogruppi vuoti, a prescindere dai gruppi. Così sotto ogni genere si
  vedono tutte le piattaforme, anche quelle senza giochi (con lo 0 e il
  loro "New page"), senza per questo mostrare i generi vuoti
- **[Progetto]** **Database alla versione 42**: `pages.hideEmptySubGroups`.
  La migrazione gli dà il valore di "Hide empty groups", che fino a qui
  valeva per tutti e due: nessuna tabella cambia aspetto da sola. Test in
  `SubGroupTest` e `MigrationTest` 41→42, col file `v41.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 42,
  le 26 pagine tutte lì, e in tutte il nuovo interruttore uguale a quello
  dei gruppi) sulla vista "Videogiochi" di "Titolo tre H3": Group: Text,
  Sub groups: Select, e i due interruttori uno sotto l'altro; spento solo
  quello dei sottogruppi, sotto "Esempio 1" ci sono PS5 (1) e poi PC,
  SWITCH e XBOX con 0. Poi la vista rimessa com'era e il database
  confrontato con la copia: uguale, tranne l'ora di ultima modifica della
  vista

### 🆕 v0.109 — 29/09/2026 · 08:00 — Tabelle: i sottogruppi, gruppi dentro i gruppi
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 214 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Consentimi di creare i Sub groups: appena creo il Grouping, apparirà come
> quarta opzione nelle impostazioni delle pagine proprio Sub groups, che mi
> consentirà di creare gruppi nei gruppi»

- **[Nuova funzionalità]** Nelle impostazioni di una vista a tabella, appena
  "Group" ha una proprietà, compare come **quarta voce "Sub groups"**
  (dopo Layout, Filter e Group). Si sceglie None o una proprietà, le stesse
  che si possono usare per i gruppi tranne quella che fa già i gruppi.
  Dentro ogni gruppo le sue righe si dividono nei sottogruppi, un po'
  rientrati: ognuno si chiude e si riapre toccandone l'intestazione, e ha
  le sue intestazioni delle colonne, i suoi calcoli e il suo "New page".
  Una pagina creata dentro un sottogruppo nasce coi valori del gruppo e
  del sottogruppo. "Hide empty groups" vale per tutti e due i livelli
- Togliendo il raggruppamento (Group: None) se ne vanno anche i
  sottogruppi; raggruppando per la proprietà dei sottogruppi, anche
- Solo nella tabella: la bacheca ha il suo "Group by", che fa le colonne,
  e lì i sottogruppi non ci sono
- **[Progetto]** **Database alla versione 41**: `pages.tableSubGroupColumnId`
  (copiato anche da "Duplicate"). `DatabaseViewModel.tableSubGroupColumn`,
  `setTableSubGroupColumn`, `addRowInSubGroup`; in `DatabaseViewScreen.kt`
  `GroupBody` fa il corpo di un gruppo e di un sottogruppo, e
  `buildRowGroups` divide anche le sole righe di un gruppo. Test in
  `SubGroupTest` e `MigrationTest` 40→41, col file `v40.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 41, le
  26 pagine tutte lì) sulla vista collegata "Videogiochi" della pagina di
  prova "Titolo tre H3": senza gruppi niente "Sub groups"; Group: Select e
  la voce compare quarta; nella sua scelta "Select" non c'è; Sub groups:
  Text, e dentro il gruppo "PS5" (3) i sottogruppi "Esempio 1", "Esempio 2"
  e "Sdrogo", ognuno con la sua tabella; "Esempio 1" si chiude da solo.
  Poi Group: None e il database confrontato con la copia: uguale, tranne
  l'ora di ultima modifica della vista. "New page" dentro un sottogruppo
  non è stato provato sul telefono, per non aggiungere righe al database
  "Videogiochi" (c'è il test)

### 🆕 v0.108 — 29/09/2026 · 07:48 — Sei puntini: "Add above" e "Add below"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 209 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Premendo i sei puntini (tre in due colonne verticali) di ogni campo
> (campo testo, pagina, database, eccetera), aggiungi due voci, ossia "Add
> above" e "Add below" per aggiungere sopra o sotto una riga»

- **[Nuova funzionalità]** Nel menu dei sei puntini, **in cima**, per ogni
  blocco (riga di testo, pagina, database, vista collegata): "**Add
  above**" e "**Add below**". Aggiungono una riga vuota sopra o sotto il
  blocco, allo stesso livello (dentro un toggle resta dentro) e col suo
  rientro, e ci mettono il cursore con la tastiera aperta
- Il posto si legge dal database in fila dopo le scritture di prima, come
  per l'Invio; sulla riga vuota di una pagina nuova quella si salva prima
- **[Progetto]** `PageRepository.insertLineNextTo`,
  `PageEditorViewModel.addLineNextTo`, `AddLineGroup` in
  `BlockActionsSheet.kt`; test in `AddLineTest`
- Verificato sul telefono sulla pagina di prova "Titolo tre H3" (copia del
  database prima): dalla barra, su una riga "Riga", "Add above" e "Add
  below" (le lettere scritte subito finiscono nelle righe nuove); dai sei
  puntini della vista "Videogiochi", le due voci in cima e una riga nuova
  sopra e sotto il database, col cursore dentro. Poi la pagina rimessa
  com'era e il database confrontato con la copia: uguale, tranne 10 voci
  di "Updates" di "Titolo tre H3" con le lettere delle prove

### 🆕 v0.107 — 29/09/2026 · 00:56 — Colonne Checkbox: "Activate all" e "Deactivate all"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 204 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ho provato, adesso funziona.
> Tenendo premuto sulla proprietà Checkbox, ci sono due opzioni ad essa
> esclusive che mi consentono di attivare o disattivare le checkbox di tutte
> le celle, chiamate "Activate all" e "Deactivate all"»

- **[Nuova funzionalità]** Tenendo premuta l'intestazione di una colonna
  **Checkbox**, sotto "Change type" ci sono "**Activate all**" e
  "**Deactivate all**": spuntano o tolgono la spunta a tutte le caselle
  della colonna, sulle righe che la vista mostra (col filtro o la ricerca,
  quelle rimaste). Solo per le caselle: le altre colonne non le hanno. Con
  il database bloccato restano grigie, come le celle
- Tutto in una scrittura sola, e le caselle che sono già così non si
  toccano: la loro riga non risulta modificata adesso ("Last edited time")
- **[Progetto]** `DatabaseRepository.setAllChecked`,
  `DatabaseViewModel.setAllChecked`; test in `CheckAllTest`
- Verificato sul telefono su un database di prova creato nella pagina
  "Titolo tre H3" (una colonna Checkbox, tre righe, quella in mezzo
  spuntata): nel menu le due voci sotto "Change type"; "Activate all" le
  spunta tutte e tre, e la riga già spuntata tiene la sua ora; "Deactivate
  all" le toglie tutte. Poi il database di prova messo nel cestino e la
  pagina rimessa com'era (confrontata con la copia fatta prima)

### 🔧 v0.106.1 — 29/09/2026 · 00:45 — Le frecce della barra spostano davvero la riga, e il cursore la segue
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 201 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Se io utilizzo il pulsante Sposta sopra nella toolbar, sembra che
> cancelli una riga in basso. Adesso sto spostando una riga verso l'altro
> sopra il database e sembra che non sposta niente. Cerca di fare tutte le
> prove necessarie per fixare questi problemi»

e poi, ancora con la versione di prima sul telefono:
> «Adesso le due freccette che permettono alle righe di essere spostate non
> spostano nulla, proprio niente, né in basso né in alto»

- **[Bug fix]** Le frecce su e giù scambiavano due righe **intere**, prese
  dalla copia della schermata, con due salvataggi separati: se il testo
  appena scritto o la riga appena nata con l'Invio non erano ancora nel
  database, lo scambio rimetteva il testo e le posizioni di prima. E il
  campo di testo, se aveva una sua scrittura in viaggio, ignorava il nuovo
  ordine: restava con quello vecchio, e la lettera dopo finiva nella riga
  sbagliata, cancellandone il testo. Ora lo spostamento aspetta in fila le
  scritture di prima e cambia nel database **solo la posizione**
  (`PageRepository.moveBlock`, `BlockDao.setOrderIndex`); il campo prende
  sempre il nuovo ordine, come per Annulla
- **[Bug fix]** **Il cursore segue la riga spostata**, nello stesso punto.
  Prima restava alla stessa altezza, cioè sulla riga scambiata: su righe
  vuote non si vedeva muoversi niente, e sopra un database la riga saliva
  ma il cursore restava sotto, così il tocco seguente spostava un'altra
  riga. Se la riga finisce in cima allo schermo, la pagina lascia sopra lo
  spazio dei pulsanti sospesi: prima ci finiva dietro
- **[Progetto]** Test in `MoveBlockTest`; due di loro (il cursore sopra il
  database, due righe con la stessa posizione) falliscono col codice di
  prima
- Verificato sul telefono (copia del database prima) sulla pagina di prova
  "Titolo tre H3", col database "Videogiochi" in cima: scritte "Uno",
  "Due", "Tre"; ↑ porta "Tre" sopra "Due" col cursore; altri due ↑ la
  portano sopra il database, e una lettera scritta subito finisce in lei;
  ↓ la riporta sotto; una lettera e subito ↑, un Invio e subito ↑, tre ↓ di
  fila veloci: nessuna riga persa, nessuna posizione doppia, il testo
  sempre nella riga giusta; Annulla rimette la riga dov'era. Poi la pagina
  rimessa com'era (il database e 22 righe vuote) e il database confrontato
  con la copia: uguale, tranne 11 voci di "Updates" di "Titolo tre H3" con
  le lettere delle prove

### 🆕 v0.106 — 28/09/2026 · 20:48 — Database: la vista Chart, con barre, linea e ciambella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 196 test
passati; commit solo in locale)*

L'utente, parola per parola, con sette foto di Notion:
> «Ho provato, ora scorre bene. Nuova vista dei database, la vista Chart:
> di base appariranno le barre verticali, ma quando tocco su Chart nelle
> opzioni del database, sotto Layout ci sarà scritto "Chart" e mi si aprirà
> un'altra piccola finestra per scegliere le quattro visualizzazioni, ossia
> Vertical, Horizontal, Line e Donut (vedi le foto). Mettici poi le varie
> opzioni che vedi sempre in foto.»

- **[Nuova funzionalità]** **"Chart" fra i layout dei database**, dopo
  Gallery (e "Chart view" nel menu "/"). Scelto, si apre la finestrella
  "**Chart type**" con i quattro grafici: **Vertical**, **Horizontal**,
  **Line** e **Donut**; di base sono le barre verticali, una per pagina,
  alte quanto il conteggio. Nelle impostazioni, sotto "Layout: Chart", la
  voce "Chart type" riapre la stessa finestrella
- **[Nuova funzionalità]** **Le voci delle foto**, sotto Layout:
  - **X axis**: *What to show* (Name, cioè una barra per pagina, oppure una
    proprietà: i tag contano una volta per tag, le caselle Checked e
    Unchecked, le date un giorno per gruppo nel formato della colonna, i
    vuoti tutti insieme in "Empty"); *Sort by* (Name A → Z, Z → A, e il
    valore dal più basso o dal più alto); *Omit zero values*
  - **Y axis**: *What to show* (Count, oppure una colonna Number con
    Values, Sum, Average, Min, Max, Median o Distinct values); *Decimal
    places* (Default, 1, 1.0, 1.00…); *Cumulative*; *Range* (minimo e
    massimo dell'asse, vuoti = li sceglie il grafico); *Reference line*
    (linee arancioni tratteggiate, col loro nome)
  - con **Horizontal** gli assi si scambiano; con **Donut** c'è "**Data**"
    (What to show, Slice by, Sort by), come nella foto: al centro il
    totale ("186 Total Number"), accanto alle fette "81 (44%)", sotto la
    legenda
  - **Style**: *Color* (Auto, cioè un colore per gruppo, o uno dei dieci);
    *More style options*: Height, Grid lines, Data labels, Smooth line,
    Show legend, Show total, ognuna solo dove serve
- Come su Notion, **ogni gruppo tiene il suo colore** anche cambiando
  l'ordine o togliendo gli zeri (nella foto la "3" resta verde anche per
  prima); la ciambella con un colore solo usa sue sfumature, se no le
  fette non si distinguerebbero. L'asse lascia un po' d'aria sopra (400
  arriva a 480, come nella foto). Con tanti gruppi le barre scorrono di
  lato
- **Non fatte**, delle voci nelle foto: il quinto tipo "#" (la richiesta
  era di quattro), l'ordine "Advanced", e *Source*, *Save chart as…*,
  *Copy link to view*, *Manage data sources*, *Learn about charts*, che su
  Notion servono al sito e ai link; Filter e il blocco della vista ci
  sono già, uguali per tutti i layout
- **[Progetto]** **Database alla versione 40**: `pages.chartSettings`, le
  impostazioni in JSON; tutte di base non si salvano (`null`). I conti in
  `data/entity/Chart.kt` (`chartPoints`, `chartAxis`, `chartTicks`,
  `compactNumber`), il disegno in `DatabaseChart.kt`, le impostazioni in
  `ChartSettingsUi.kt`, le scritte in `ChartStrings`. Test in `ChartTest`
  e `MigrationTest` 39→40, col file `v39.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 40,
  le 19 pagine tutte lì) su "SuduS al contrario": Layout → Chart apre la
  finestrella; barre da 1; col Number 81, 25, 9, 71 sull'asse 0–100;
  Horizontal, Line con l'area sotto e la legenda "Number", Donut; ordine
  dal più alto; colore Green; senza totale e senza etichette, Large;
  Cumulative (81, 152, 177, 186, asse 0–240); due decimali ("0,00");
  Range fino a 200; linea "Media" a 100; asse X per Date (tre gruppi con
  "Empty"); Smooth line; anche aperto a tutta pagina. Corretti durante la
  prova: i numeri tagliati dalla griglia e dalla linea (ora hanno attorno
  un bordino del colore dello sfondo), i colori che seguivano il posto,
  l'asse fino a 300 per un massimo di 186, "Decimal places" che diceva
  "2" invece di "1.00". Poi tutto rimesso com'era dall'app (Table,
  impostazioni del grafico di nuovo `null`): il database è uguale alla
  copia, tranne l'ora di ultima modifica della pagina "SuduS al
  contrario". "1 line" al singolare non è stato riprovato sul telefono

### 🔧 v0.105.2 — 28/09/2026 · 20:05 — Lo scorrimento non si ferma più un istante prima di partire
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ho provato, adesso lo scorrimento funziona, anche se per un millisecondo
> si blocca prima di farmi scorrere»

- **[Bug fix]** Risalendo dal fondo della pagina, il primo fotogramma
  dello scorrimento durava 29 millisecondi invece di 8: la lista della
  pagina rifaceva da capo il database di sopra ("SuduS al contrario"),
  che aveva smontato quando era uscito dallo schermo. Poco dopo c'era un
  altro fotogramma da 35 millisecondi, per le righe di testo sopra. Ora
  ogni voce della pagina, una volta fatta, resta pronta anche fuori dallo
  schermo finché si resta sulla pagina (`KeepComposed`, con
  `LocalPinnableContainer`, lo stesso modo in cui la lista tiene vivo un
  campo col cursore); quelle mai viste si fanno quando arrivano, come
  prima. Una finestra per preparare in anticipo le voci vicine ci sarebbe
  solo in una versione più nuova di Compose (qui la 1.6.8), da aggiornare
  a parte
- Verificato sul telefono misurando i fotogrammi (`dumpsys gfxinfo`) con
  lo stesso scorrimento dal fondo della pagina principale, partito da una
  cella di "Videogiochi". **Prima**: il primo fotogramma 29 ms (27 di
  preparazione), uno da 35 ms a metà corsa. **Dopo**: il primo 14 ms (6
  di preparazione), nessuno sopra i 16 ms su 84. La scrittura funziona
  come prima: toccata "Primo punto", cursore in fondo e tastiera aperta

### 🔧 v0.105.1 — 28/09/2026 · 19:57 — Risalendo una pagina, i database non fanno più saltare tutto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Se io sto in fondo, e scorro verso il basso per andare in alto, ma lo
> scorrimento lo faccio toccando le celle del database Videogiochi, accade
> che fa un grande scatto in alto e mi skippa direttamente tutto il
> database. Usa anche il database sul menu per controllare questo bug.»

- **[Bug fix]** Risalendo una pagina con dei database dentro, quando il
  database di sopra rientrava nello schermo la pagina **saltava in alto**
  di colpo e lo scavalcava tutto. Non dipendeva dalle celle: capitava anche
  partendo dal testo sotto. Ogni database dentro una pagina passa dal
  cancello della password (`LockGate`), che finché non aveva chiesto al
  database se la pagina è bloccata **non disegnava niente**; un database
  uscito dallo schermo veniva smontato, e al rientro per un attimo era
  alto zero, poi alto quanto la sua tabella. La pagina tiene fermo il suo
  bordo di sopra, e l'allungamento spingeva giù tutto il resto. Ora il
  cancello si ricorda l'ultima risposta per pagina, finché le protezioni
  non cambiano (`AppLock.protectionVersion`), e il database torna subito
  alto com'è; la domanda si rifà comunque ogni volta
- Verificato sul telefono registrando lo schermo sulla pagina principale,
  in fondo, con uno scorrimento verso il basso di mezzo schermo partito da
  una cella Select di "Videogiochi". **Prima**: per quattro fotogrammi
  quasi fermo, poi un salto di circa il doppio della strada del dito, fino
  a "SuduS al contrario" e oltre; lo stesso partendo dal testo sotto la
  tabella. **Dopo**: la tabella scende piano col dito, e da sopra compare
  un po' alla volta il fondo di "SuduS al contrario"

### 🆕 v0.105 — 28/09/2026 · 19:49 — Pomodoro: la durata dello studio dal riquadro grande, "Study sound" e "Break sound"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 185 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Nel Pomodoro, togli la possibilità di modificare il tempo della sessione
> di studio. Già la modifico nel riquadro principale. Nelle impostazioni
> lascia solo la lunghezza della pausa e la selezione del suono.
> Aggiungimi, però, anche la selezione del suono per quando riinizia la
> sessione di studio. Mi scriverai quindi "Study sound" e "Break sound",
> con il primo che è un suono che mi dice quando riinizia la sessione di
> studio e il secondo che è un suono che mi dice quando inizia la pausa»

- **[Rimosso]** Dall'ingranaggio del Pomodoro la durata della sessione di
  studio. Restano la durata della pausa e i suoni
- **[Nuova funzionalità]** **Il tempo scritto nel riquadro grande durante
  lo studio è la durata della sessione**, anche per le sessioni dopo e per
  "da capo", secondi compresi. Senza, tolta la sua voce dall'ingranaggio,
  le sessioni seguenti sarebbero tornate alla durata vecchia. Durante la
  pausa il tempo scritto vale solo per quella pausa
- **[Nuova funzionalità]** Due suoni al posto di uno: **"Study sound"**,
  quando ricomincia la sessione di studio, e **"Break sound"**, quando
  comincia la pausa, ognuno scelto fra le suonerie del telefono e ognuno
  col suo "usa il suono predefinito". Il suono unico di prima diventa il
  "Break sound" (si salva ancora col suo nome di allora, `soundUri`)
- **[Progetto]** `PomodoroWidget`: `sessionMs`, `breakSoundUri` /
  `breakSoundName` (salvati come `soundUri` / `soundName`),
  `studySoundUri` / `studySoundName`, `soundFor`, `withTimeWritten`.
  `WidgetStore.pomodoroSetBreakLength` e `pomodoroSetSounds`. Test in
  `WidgetsTest`, anche un pomodoro salvato prima che si rilegge col suo
  suono come "Break sound"
- Verificato sul telefono: l'ingranaggio ha "Break length", "Study sound"
  e "Break sound" (tutti e due "Default"), niente durata dello studio;
  "Study sound" apre l'elenco delle suonerie col suo nome in cima (scelto
  il selettore di Samsung con "Just once"), e tornando indietro non è
  cambiato niente. Chiuso con Cancel: il pomodoro è rimasto com'era (1
  minuto di studio, 5 di pausa, nessun suono scelto)

### 🆕 v0.104 — 28/09/2026 · 19:40 — Nei calendari per scegliere la data, solo i giorni del mese
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, appena
ricollegato; 182 test passati; commit solo in locale)*

L'utente, parola per parola:
> «Nei calendari di selezione della data, toglimi i giorni in grigio del
> mese precedente in alto e i giorni del mese successivo in basso. Mi
> confondono troppo se uso il Repeat e vedo troppi pallini»

- **[Rimosso]** Nel calendario che si apre per scegliere una data (le
  date dei database, con Repeat, e le notifiche: è lo stesso,
  `SamsungCalendar`) **i giorni del mese prima e del mese dopo non si
  vedono più**: al loro posto le caselle restano vuote, senza numero, senza
  pallino della ripetizione e senza tocco. Le settimane restano sei, così
  il calendario non cambia altezza passando da un mese all'altro
- Verificato sul telefono: aperta la data "2025年12月08日" della riga
  "Esempio" di "SuduS al contrario". Dicembre 2025 comincia lunedì 1 con
  la domenica prima vuota, e dopo il 31 le caselle sono vuote; col ">"
  gennaio 2026 comincia giovedì 1, prima niente. Chiuso con Cancel: le date
  nel database sono rimaste quelle di prima

### 🆕 v0.103 — 28/09/2026 · 19:38 — Notebooks in cima alla barra laterale
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 182 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ho provato, lo scatto non c'è più. Mettimi Notebooks sopra tutto lo
> spazio degli Widget»

- **[Nuova funzionalità]** Il riquadro **Notebooks** coi suoi quaderni
  sta ora **in cima alla barra laterale**, sopra i widget; sotto di lui
  resta la linea chiesta il 26/09. Fra i widget e "Main menu" c'è adesso
  la stessa linea: prima a separarli c'erano i quaderni
- Verificato sul telefono: aperta la barra, in cima Notebooks, la linea,
  poi "Widgets" coi fusi orari e gli altri; in fondo ai widget "Add
  widget", la linea, "Main menu"

### 🔧 v0.102.2 — 28/09/2026 · 19:33 — Invio in fretta: niente righe in più, e i font non scattano
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 182 test
passati; commit solo in locale il 28/09/2026)*

L'utente, parola per parola:
> «C'è un bug grafico stranissimo nella home: quando seleziono con il text
> cursor la primissima riga vuota, e faccio invio velocemente, è come se i
> due blocchi dove c'è testo con font diverso e testo sia sub che
> superscripted scattassero, come se si ingrandissero e diminuissero di
> scatto. Riesci a vederlo?»

- **[Bug fix]** **Invio premuti in fretta creavano una riga in più.** Il
  secondo Invio arrivava prima che il database avesse scritto il primo:
  il campo aveva una riga più dei blocchi, `updateRun` tornava a
  confrontare coi testi salvati e da lì sembravano nate due righe. Tre
  Invio facevano quattro righe, e da quel momento campo e blocchi non si
  corrispondevano più: "Paragrafo di prova…" restava senza i suoi font e
  "acqua H₂O energia mc²" senza pedice e apice, finché non si riapriva la
  pagina. Ora ogni modifica aspetta la precedente e rilegge i blocchi dal
  database (`structuralEdits`), dove la precedente c'è già; delle righe
  che c'erano si riscrive solo il testo, così un cambio di tipo arrivato
  un istante prima (il menu "/") non viene riportato indietro
- **[Bug fix]** **Lo "scatto"** anche con un Invio solo: per un
  fotogramma, finché il database non scriveva, ogni riga prendeva font,
  colori, pedici e apici dal blocco della riga accanto, e le righe
  formattate si vedevano cambiare e tornare. Ora righe e blocchi si
  abbinano contando dal fondo e dall'inizio (`blocksOfLines`), come già
  facevano i pallini degli elenchi — e ora anche col backspace e con più
  righe di differenza; la riga divisa in due o le due righe unite tengono
  la formattazione prevista
- **[Progetto]** `updateRun` → `runDiff` e `writeRunChange`;
  `PageRepository.getBlocksOnce`, `applyBlockChanges` con `ensured` e
  `texts`. Test in `FastEnterTest` (con la versione di prima il test dei
  tre Invio falliva: quattro righe) e `LineBlocksTest`
- Verificato sul telefono registrando lo schermo sulla pagina principale,
  cursore sulla prima riga vuota e tre Invio di fila col tasto della
  tastiera. **Prima**: nei fotogrammi dopo il terzo Invio "Paragrafo di
  prova…" senza font e "acqua H2O energia mc2" senza pedice e apice fino
  alla fine della registrazione, e nel database quattro righe nuove invece
  di tre. **Dopo**: in tutti i 23 fotogrammi le due righe restano
  formattate, e nel database le righe nuove sono esattamente tre. Le
  dieci righe vuote aggiunte dalle prove in cima alla pagina principale
  non ho fatto in tempo a toglierle (il telefono si è scollegato); al
  28/09/2026, ricollegato, non c'erano più: in cima ci sono quattro righe
  vuote, e il resto della pagina, formattazione compresa, è intatto

### 🔧 v0.102.1 — 26/09/2026 · 18:31 — "To-do list" dal "+" sulla prima riga di una pagina nuova
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 174 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Non mi fa mettere le checkbox nei template»

- **[Bug fix]** In un template appena creato — e in ogni pagina nuova,
  comprese quelle delle righe dei database — **scegliere un tipo dal "+"
  sulla prima riga, prima di scriverci, non faceva niente**: "To-do list"
  non metteva la casella, e quello che si scriveva dopo restava testo
  normale. La prima riga di una pagina senza righe si vede ma nel database
  non c'è finché non ci si scrive (vedi `placeholder`), e il cambio di tipo
  scrive **una colonna sola** di una riga che deve già esserci: non la
  trovava e si perdeva. Con "/" funzionava, perché la barra scritta salvava
  prima la riga. Ora le modifiche di una colonna sola (tipo, testo,
  rientro, numero di partenza, notifica, tabella, vista collegata)
  passano da `blocksDb`, che prima salva quella riga **solo se manca**:
  se intanto è stata scritta, resta com'è
- **[Progetto]** `BlockDao.insertIfAbsent`, `PageRepository.saveBlockIfAbsent`.
  Test in `EmptyPageTest`
- Verificato sul telefono: riprodotto prima della correzione (template
  nuovo, "+" → To-do list, "ciao": nel database un paragrafo "ciao").
  Dopo: la casella compare subito, "ciao", Invio, "due" → due caselle nel
  database. I template di prova sono nel cestino; "SaaS" e "SuduS"
  dell'utente non sono stati toccati

### 🆕 v0.102 — 26/09/2026 · 18:21 — I template delle pagine, e le proprietà in cima alla pagina di una riga
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 173 test
passati; commit solo in locale)*

L'utente, parola per parola, con tre foto di Notion (le voci dei template
in una pagina nuova, la pagina "2" con le caselle 1…10 del template
"SaaS", le proprietà Date, Number, Select e "Add a property" sotto il
titolo):
> «Due feature che riguardano le pagine contenuti nei database complessi:
> 1) come fa Notion, implementa i templates; creami innanzitutto una voce
> nella Sidebar, sotto Startup window, chiamata Templates, dove ci sarà la
> lista dei template che creerò. Notion concede di creare un template per
> le pagine che mi consentono di risparmiare tempo se in più pagina voglio
> scrivere principalmente una base uguale per tutte (se per esempio voglio
> creare un template con tutte to-do list, con nomi specifici, che mi
> consente di non scrivere per ogni pagina svariate check-box uguali).
> Appena creo una pagina, verrà visualizzato quello che vedi nella prima
> foto, ossia delle voci in un grigio visibile per differenziarlo dal
> colore classico di tutto il resto (vediamo poi se mi piace come lo
> farai) che scomparirà se premo su un punto a caso della pagina (dato che
> significa che non mi serve più quello che c'è scritto). Le voci saranno
> '+ New template' che mi consentirà di creare anche qui dentro un
> template (stessa cosa nella sua sezione privata nella sidebar), e 'Add
> existing template', che mi aprirà la lista dei template che ho creato e
> che mi consentirà di scrivere esattamente tutto quello che c'è scritto
> in quel template al fine di, come ti ho detto prima, farmi risparmiare
> tempo. Un esempio che vedi nella prima foto, è un template che ho
> rinominato SaaS che, una volta che lo tocco, mi farà apparire tutte le
> checkbox perché creandolo ho scritto tutte quelle checkbox precise con
> le scritte (come vedi nella seconda foto). Ovviamente, fai sì che i
> template che creo nella voce Templates nella sidebar e i template che
> creo direttamente in una pagina vengano sincronizzati, anche per altri
> database;
> 2) sempre su Notion, sia su PC che su telefono, quando apro una pagina in
> un database, mi fa vede tutte le proprietà collegate ed esistenti a
> questa pagina (come vedi nella terza foto). Aggiungi questa feature
> anche qui, con un divider che dividerà le proprietà e il campo di testo
> su cui scrivere. Ovviamente, sarò in grado di modificare anche dentro un
> pagina le sue proprietà, ed ovviamente fai sì che tutto venga
> sincronizzato. Fammi anche vedere le proprietà nascoste che posso
> comunque modificare»

- **[Nuova funzionalità]** **"Templates" nella barra laterale**, sotto
  "Startup window": l'elenco dei template, "New template" in cima per
  crearne uno, e i tre puntini di ciascuno per mandarlo nel cestino (da cui
  torna fra i template, non nel menu principale). Un template **si scrive
  come una pagina qualsiasi** — caselle, elenchi, titoli, toggle, tabelle,
  sottopagine — con in cima, in grigio, "You're editing a template"; senza
  nome si chiama "Untitled template". Nei tre puntini di un template non ci
  sono "Duplicate" e "Move to", che lo farebbero diventare una pagina del
  menu principale
- **[Nuova funzionalità]** **Le voci grigie di una pagina nuova di un
  database**: aprendo la pagina di una riga ancora vuota, sotto le
  proprietà compaiono "Tap anywhere to continue with an empty page, or pick
  a template" e le due voci **"+ New template"** (crea un template e lo
  apre, da scrivere) e **"Add existing template"** (l'elenco dei template:
  toccato uno, tutto quello che c'è scritto dentro finisce nella pagina e
  le voci se ne vanno). Sono grigie come il resto dei suggerimenti
  dell'app. **Un tocco in un punto qualsiasi della pagina** le fa sparire;
  riaprendo la pagina ancora vuota tornano, come su Notion. Il template
  prende il posto delle righe vuote della pagina (in una pagina già scritta
  andrebbe in fondo); titolo, proprietà, icona e copertina della pagina non
  cambiano; Annulla lo toglie. I template sono **gli stessi per tutti i
  database**, sia quelli creati dalla barra laterale sia quelli creati da
  una pagina
- **[Nuova funzionalità]** **Le proprietà in cima alla pagina di una
  riga**, come nella terza foto: fra il titolo e il testo, una per riga,
  con l'icona del tipo e il nome, **ciascuna modificabile lì** come nella
  sua cella (tag, date con ripetizioni e notifiche, numeri, caselle,
  colori, testo…); sotto, "**Add a property**" per aggiungerne una al
  database, e una linea che le separa dal testo. **Ci sono anche quelle
  nascoste nella tabella**, con accanto l'occhio sbarrato. Tutto
  sincronizzato: cambiata qui, la proprietà cambia nella tabella e nelle
  altre viste, e al contrario. Con la pagina o il database bloccati si
  leggono soltanto. I database semplici non c'entrano: le loro righe non
  hanno pagine
- **[Progetto]** **Database alla versione 39**: `pages.isTemplate`. I
  template non compaiono nell'albero, nella ricerca, fra le pagine da cui
  partire e fra le destinazioni di "Move to". `PageRepository`:
  `observeTemplates`, `createTemplate`, `applyTemplate` (copia blocchi,
  figli, celle delle tabelle e sottopagine, come "Duplicate"),
  `observeRowOfPage`. `RowPageProperties` (in `DatabaseViewScreen.kt`, con
  le celle della tabella) usa un `DatabaseViewModel` caricato
  `light`, senza le anteprime della galleria che cambierebbero a ogni
  tasto. `TemplatesScreen.kt` ha l'elenco, `TemplatePicker` e
  `TemplateChooserSheet`; le scritte stanno in `TemplateStrings`. Test in
  `TemplateTest` e `MigrationTest` 38→39, col file `v38.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 39, le
  10 pagine tutte lì): Templates sotto Startup window, vuoto; "New
  template" → "SaaS" con le caselle 1, 2, 3; nei suoi tre puntini niente
  Duplicate e Move to. Nel database "Untitled", pagina "2": Select PS5,
  Text, "Add a property", la linea, e le voci grigie; "Add existing
  template" → SaaS → le tre caselle nella pagina, le voci sparite. Select
  cambiato in PC dalla pagina → PC nella tabella; rimesso PS5 dalla
  tabella → PS5 nella pagina. "Text" nascosta nella tabella → nella pagina
  c'è, con l'occhio sbarrato. Un tocco nel vuoto fa sparire le voci; "Add a
  property" apre "New property" (annullato); "+ New template" dalla pagina
  "4" apre un template nuovo, e tornando le voci sono ancora lì con due
  template nell'elenco; la pagina "1", che ha del testo, non le mostra.
  Alla fine rimesso tutto com'era: Annulla ha tolto le caselle dalla pagina
  "2" (stesso blocco di prima), "Text" di nuovo visibile, PS5 al suo posto,
  e i due template di prova nel cestino. Il confronto col database di prima
  ha trovato **un mio errore**, corretto: un tocco finito su una cella di
  "SuduS al contrario" e uno scorrimento sopra la tastiera avevano
  aggiunto "⁵" al nome "Riga semplice"; tolto, e al secondo confronto i
  dati erano identici a prima (a parte i due template di prova)

### 🔧 v0.101.1 — 26/09/2026 · 17:49 — L'Invio a inizio della prima riga non chiude più la tastiera
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 168 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Prima dimmi se il telefono è collegato. Poi, piccolo bug: se con il
> text cursor mi trovo sul primissimo blocco di un campo di testo su
> questo blocco c'è qualcosa scritto e vado a capo, la tastiera
> scompare. Questa cosa accade solo se mi trovo al primo blocco e solo
> se c'è qualcosa scritto.»

- **[Bug fix]** **Invio col cursore all'inizio della prima riga** di un
  gruppo di righe di testo (in cima alla pagina, o subito sotto un
  toggle, un divisore, un link…): la tastiera spariva. Il campo che
  tiene insieme le righe era riconosciuto dal suo primo blocco, e la
  riga vuota nuova nasce proprio sopra di lui: con un primo blocco
  diverso il campo veniva rifatto da capo e perdeva il cursore. Solo
  sulla prima riga e solo con del testo perché su una riga vuota, o su
  una riga più sotto, la riga nuova non diventa la prima del gruppo.
  Ora il campo è riconosciuto da quello che gli sta sopra (l'isola che
  lo precede, o l'inizio della pagina), che andando a capo non cambia
- **[Bug fix]** Nello stesso caso, sotto un'isola **la riga vuota finiva
  in cima alla pagina**, sopra l'isola, invece che subito sopra la riga:
  le si dava il posto zero della pagina invece di quello della riga.
  Non si vedeva ancora perché il campo si chiudeva prima
- **[Progetto]** `groupIntoRenderItems` dà la chiave ai gruppi di righe
  (`RenderItem.Run.key`), e `MergedTextRunField` la riceve invece di
  ricavarla dal primo blocco; in `updateRun` le righe nuove senza un
  blocco prima vanno al posto della prima riga. Test in `RunKeyTest` e
  `EnterAtFirstLineTest`
- Verificato sul telefono (Galaxy S25 Ultra, collegato col debug senza
  fili), sulla pagina "Titolo tre H3", che ha solo righe vuote: scritto
  "prova" sulla prima riga, cursore all'inizio, Invio col tasto della
  tastiera. **Prima** la tastiera si chiudeva (riprodotto), **dopo**
  resta aperta, "prova" scende di una riga e la lettera scritta subito
  dopo va davanti a "prova". Lo stesso con un divisore sopra: la riga
  vuota nasce fra il divisore e "prova", la tastiera resta. (In fondo
  alla riga l'Invio andava già bene: provato prima della correzione,
  non di nuovo dopo.) Alla fine
  tolti il testo e il divisore di prova: la pagina ha di nuovo le sue
  23 righe vuote

### 🆕 v0.101 — 26/09/2026 · 13:53 — "Date format" e "Time format" nella proprietà Date, e via da Settings
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 164 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «In Edit property della proprietà Date, aggiungimi due voci, ossia Date
> format (prima foto) e Time format (seconda foto). In Date format
> dell'Edit property della proprietà Date, mettimi tutto quello che c'è in
> Date format in Settings. Quest'ultimo poi, una volta aver messo tutto
> lì, toglimelo. È ridondante»

e subito dopo, con le due foto di Notion:
> «Perdonami, eccole»

- **[Nuova funzionalità]** **Date format** in Edit property di una
  colonna Date: gli stessi nove formati che c'erano in Settings (Full
  date, Short date, Month.Day.Year, Month/Day/Year, Day.Month.Year,
  Day/Month/Year, Year.Month.Day, Year/Month/Day, Chinese date), ognuno
  con la data di oggi scritta così e la spunta su quello attuale. "Relative"
  della foto di Notion non c'è: la richiesta era la lista di Settings
- **[Nuova funzionalità]** **Time format** accanto: **Hidden**, **12
  hour**, **24 hour**, come nella seconda foto. 12 hour scrive "7:43 PM",
  24 hour "19:43", Hidden toglie l'ora dalla cella (resta salvata, e la
  notifica pure). Vale per le celle, le liste, i calcoli (Earliest,
  Latest…) e il riassunto del filtro, in tutte le viste della colonna;
  ogni colonna Date ha il suo
- **[Rimosso]** **Date format da Settings**. Le colonne Date che non ne
  hanno ancora scelto uno (e quelle nuove) continuano col formato scelto
  lì l'ultima volta, e così la cronologia, i preferiti, il cestino e la
  data dei quaderni, che non hanno una proprietà da cui prenderlo
- **[Progetto]** **Database alla versione 38**:
  `database_columns.dateFormat` (il nome di `DateStyle`; vuoto = quello
  di prima) e `database_columns.timeFormat` (H12, HIDDEN; vuoto = 24
  ore), letti con `dateSettings()`. `Formats.dateTimeFormatter(style,
  time)` e `Formats.dateStyleName`. Test in `MigrationTest` 37→38 (le
  date restano come prima, ora a 24 ore), col file `v37.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 38,
  le 10 pagine tutte lì), su "SuduS al contrario": Edit property di Date
  → Date format "Chinese date", Time format "24 hour"; la lista dei nove
  formati con gli esempi di oggi e la spunta su Chinese date; Time format
  con Hidden, 12 hour, 24 hour. Day/Month/Year e 12 hour → "25/09/2026
  7:43 PM", "08/12/2025", e il calcolo Earliest "08/12/2025"; Hidden →
  "25/09/2026". In Settings, dopo Number format viene subito Language.
  Alla fine rimesso Chinese date e 24 hour: le celle sono tornate
  "2026年09月25日 19:43" e "2025年12月08日" come prima

### 🆕 v0.100 — 26/09/2026 · 13:40 — A sinistra, al centro o a destra: le celle e i calcoli
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 163 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Due piccole feature:
> 1) dammi la possibilità di allineare a sinistra, a destra o perfettamente
> al centro tutto quello che viene fuori dal Calculate, tenendo premuto su
> ogni cella del Calculate;
> 2) dammi la possibilità di allineare a sinistra, perfettamente al centro
> (queste già ci sono) o a destra tutto quello che viene scritto in una
> cella, che sia Select, Date, Number, o tutto il resto, sempre tenendo
> premuto su ogni cella della Property, ma MAI la scritta delle Property:
> quelle sempre a sinistra, lasciale così come sono.»

- **[Nuova funzionalità]** **Le celle di ogni colonna a sinistra, al centro
  o a destra**: tenendo premuta l'intestazione, tre voci — **Align left**,
  **Center**, **Align right** — con la spunta su quella attuale, per
  **ogni tipo** (prima si centravano solo le caselle). Si spostano il
  testo, i numeri (anche barra e anello: l'anello al centro o a destra
  segue la colonna, a sinistra comanda la sua "Position"), i tag, le date,
  il testo delle celle colorate, le caselle, le date di creazione e
  modifica. **Le intestazioni restano sempre a sinistra.** Vale in tutte
  le viste che mostrano la colonna; la colonna del nome non cambia
- **[Nuova funzionalità]** **Il risultato di ogni calcolo** sotto la
  tabella a sinistra, al centro o a destra, **tenendolo premuto** (toccato
  si cambia il calcolo, come prima). Di suo a destra, come su Notion. Vale
  per quella vista, come i calcoli
- **[Progetto]** **Database alla versione 37**: `database_columns.contentAlignment`
  (LEFT, CENTER, RIGHT; vuoto = come prima, al centro solo le caselle
  centrate col vecchio interruttore, vedi `cellAlignment`). Gli
  allineamenti dei calcoli stanno nella stessa stringa dei calcoli della
  vista, con le chiavi "align:colonna" (`parseCalculationAlignments`),
  senza colonne nuove. Il contenuto della cella sa dove stare da
  `LocalCellAlignment`; `setColumnCentered` non serve più. Test in
  `NumberFormatTest` e `MigrationTest` 36→37 (una casella centrata prima
  resta al centro), col file `v36.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 37, le
  10 pagine tutte lì), su "SuduS al contrario", dove intanto l'utente aveva
  scritto dei numeri (25, 81, 9, 71) e messo "Sum": tenuta premuta
  l'intestazione di Number → Align left spuntato, Center, Align right →
  Align right → i numeri a destra; Date → Center → i trattini al centro (la
  data della prima riga ha l'ora e riempie la cella, quindi non ha spazio
  per spostarsi); "SUM 186" tenuto premuto → Align left → a sinistra. Alla
  fine rimesso com'era: Number e Date a sinistra, "SUM 186" a destra, il
  calcolo dell'utente al suo posto (nel database la stringa dei calcoli è
  tornata identica)

### 🆕 v0.99 — 26/09/2026 · 13:24 — La proprietà Number: formato, cifre, barra e anello; "Calculate" sotto la tabella
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne quello
elencato in fondo; 161 test passati; commit solo in locale)*

L'utente, parola per parola (con quattordici foto di Notion: il menu della
proprietà, "Edit property" con Number format, Decimal places e "Show as",
la lista dei formati, le cifre decimali, Bar e Ring con le loro celle, e i
menu di "Calculate"):
> «Tante nuove mini feature nella proprietà Number dei database:
> 1) quando tengo premuto sulla proprietà Number, mi appariranno le
> classiche opzioni, tra cui Edit property. Quello che c'era qui dentro,
> ossia le opzioni per cambiare tipo di proprietà, mettilo in un'altra
> nuova opzione chiamata Change Type;
> 2) dentro Edit property invece ci metterai tre voci come vedi nella prima
> foto: Number format, Decimal places, qui ci metti un divider con il
> titolo Show as e tre piccoli riquadri uguali alla foto, Number (con il
> numero 42), Bar (con una barra riempita a metà) e Ring (con un anello
> riempito del 40%);
> 3) quando tocco Number format, apparirà una lista completa di tutte le
> tipologie di numeri inseribili come vedi nella seconda foto: qui metti
> Number, Percent, un divider che farà iniziare tutte le valute con il
> rispettivo simbolo della valuta, ossia US Dollar (USD), Euro (EUR), Pound
> (GBR), Swiss Franc (CHF), Yuan (CNY), Yen (JPY), Won (KRW), Australian
> Dollar (AUD), Canadian Dollar (CAD), Singapore Dollar (SGD), Ruble (RUB),
> Rupee (INR), Hong Kong Dollar (HKD), New Zealand Dollar (NZD),  Swedish
> Krone (SEK), Norwegian Krone (NOK), Mexican Peso (MXN), Taiwan Dollar
> (TWD), Danish Krone (DKK) e Złoty (PLN). Tutto il resto che vedi nella
> seconda foto non lo mettere e rispetta per favore l'ordine che ho usato
> qui nel prompt;
> 4) in Decimal places, metterai una lista che va da 0 a 5 come vedi nella
> terza foto, e questi rappresenteranno il numero dei numeri che possono
> essere aggiunti a destra della virgola di un numero decimale. Se scrivo
> 45.76, ma i Decimal places sono solo uno, il 6 non me lo visualizza ma
> rimane comunque nascosto, non me lo cancellare. Se scrivo 45.76, ma i
> Decimal places sono tre, mi visualizzerà 45.760;
> 5) se è attivo number, sotto non ci saranno altre opzioni. Se attivo Bar,
> invece, mi visualizzerà nelle celle del database una barra orizzontale
> di riempimento che rappresenta il numero (come hai fatto con il widget
> Life progress bar). Se è attivo Bar, sotto appariranno due opzioni come
> vedi nella quarta foto, ossia Color (con la selezione del colore della
> barra riempita, sempre con la classica palette colori che già hai messo
> altrove) e Show number. Divide by non mi interessa; nelle celle vedrò un
> qualcosa simile che vedi nella quinta foto;
> 6) il Ring è un anello che entra nella cella (concedimi di scegliere se
> metterlo più a sinistra o al centro) come vedi nella sesta foto; stesse
> opzioni di Bar (ossia Color e Show number) e verrà qualcosa fuori come
> vedi nella settima foto;
> 7) ogni volta che tengo premuto su una proprietà mi metterai una voce
> chiamata Calculate, come vedi nell'ottava foto: come vedi in tutte le
> foto successive alla nona compresa, queste opzioni mi consentiranno di
> far apparire una barra attaccata sotto a quel database con dei valori di
> riassunto, diciamo. Questi valori sono: None (per disattivare tutto e la
> barra compresa), Count (con Count all, Count values, Count unique
> values, Count empty e Count not empty) e Percent (con Percent empty e
> Percent not empty). Queste due voci sono universali per tutte le
> proprietà. Alla proprietà Date aggiungi la voce Date (con Earliest date,
> Latest date e Range date). Alla proprietà Number aggiungi la voce More
> option (con Sum, Average, Median, Min, Max, Range). Alla proprietà
> Checkbox, modifica le voci di Count in Count all, Checked e Unchecked.»

- **[Nuova funzionalità]** **"Change type"** nel menu che si apre tenendo
  premuta l'intestazione di una colonna, di **qualunque** tipo: lì c'è la
  lista dei tipi con la ricerca. **"Edit property"** (e il tocco
  sull'intestazione) ha il nome e, per una colonna Number, le impostazioni
  qui sotto; il tipo non c'è più
- **[Nuova funzionalità]** **Edit property di una colonna Number**, come
  nella prima foto: **Number format**, **Decimal places**, una riga, "Show
  as" e i tre riquadri **Number** (42), **Bar** (piena a metà) e **Ring**
  (pieno al 40%); quello scelto è bordato di blu. Si salva con "Save"; vale
  in tutte le viste che mostrano la colonna
- **[Nuova funzionalità]** **Number format**: Number e Percent, una riga,
  poi le venti valute **nell'ordine chiesto**, ognuna col suo segno ($, €,
  £, CHF, ¥, ₩, A$, CA$, S$, ₽, ₹, HK$, NZ$, kr, MX$, NT$, kr., zł), con un
  campo per cercarle. Il segno va davanti ("€25,00") o dopo per ₽, kr e zł
  ("100 kr"). "Pound" è **GBP**: nella richiesta c'era "GBR", che è il
  codice del paese. **Percent** aggiunge il segno % al numero scritto: 45
  si legge 45% (non si moltiplica per 100)
- **[Nuova funzionalità]** **Decimal places**: Default (come l'ha scritto
  l'utente) e da 0 a 5. Le cifre in più **si nascondono senza
  arrotondare** e senza essere cancellate: 45.76 con una cifra si legge
  45.7, con tre 45.760; il valore salvato resta 45.76. Scrivendo nella
  cella si vede il numero com'è, senza formato
- **[Nuova funzionalità]** **Bar e Ring nelle celle**: una barra orizzontale
  o un anello pieni per il numero diviso 100 (72 → quasi tre quarti; "Divide
  by" non c'è, come chiesto), col numero accanto se **Show number** è
  acceso. **Color** apre la tavolozza di sempre (spettro, grigi, RGB, Hex);
  di suo è verde. Per l'anello, **Position**: a sinistra o al centro della
  cella. Nell'elenco e nelle schede il numero si legge col suo formato
- **[Nuova funzionalità]** **"Calculate"**, tenendo premuta l'intestazione di
  qualunque colonna **e del nome**: None, **Count** (Count all, Count
  values, Count unique values, Count empty, Count not empty; per le caselle
  Count all, Checked, Unchecked), **Percent** (Percent empty, Percent not
  empty), per le date **Date** (Earliest date, Latest date, Date range),
  per i numeri **More options** (Sum, Average, Median, Min, Max, Range). I
  gruppi si aprono toccandoli. Il calcolo compare in una **riga sotto la
  tabella**, sotto la sua colonna ("SUM €25,00", "COUNT 4"), sulle righe
  mostrate; in una tabella a gruppi, sotto ogni gruppo. Toccandolo si
  cambia; con None su tutte le colonne la riga sparisce. I calcoli sono
  **della vista**, come su Notion: una vista collegata ha i suoi
- **[Progetto]** **Database alla versione 36**: in `database_columns`
  `numberFormat`, `decimalPlaces`, `numberDisplay`, `numberColor`,
  `showNumber`, `ringCentered`; in `pages` `viewCalculations`
  ("colonna=CALCOLO;…"). Formati e calcoli in `NumberFormat.kt` (letti per
  nome: uno sconosciuto torna al valore di sempre invece di far chiudere
  l'app), la parte grafica in `NumberProperty.kt`. Test `NumberFormatTest`
  (8, anche coi numeri delle foto: somma 309, media 38,63, mediana 30,5) e
  `MigrationTest` 35→36 col file `v35.sql`
- Verificato sul telefono (copia del database prima; migrazione alla 36, le
  10 pagine tutte lì), sulla colonna "Number" di "SuduS al contrario" (25
  in "Riga semplice"): tenuta premuta → Move, Edit property, **Change
  type**, Hide, **Calculate**; Edit property come nella foto; Number format
  → la lista con i segni, Number e Percent sopra la riga → Euro; Decimal
  places → 2; Bar → Color e Show number → "Save" → **"€25,00" e la barra
  verde piena a un quarto**; Ring + Center → "€25,00" e l'anello in mezzo
  alla cella; Calculate → More options → Sum → **"SUM €25,00"** sotto la
  colonna; sul nome Count all → **"COUNT 4"**; toccando la cella si scrive
  "25", con "Done" torna "€25,00"; Change type → solo i tipi. Alla fine
  tutto rimesso com'era (Number, Default, Number, a sinistra, nessun
  calcolo: la riga dei calcoli sparisce), controllato nel database
- **Non provato sul telefono**: le cifre che si nascondono (45.76 → 45.7;
  lo prova il test, sul telefono avrebbe voluto dire cambiare un numero
  dell'utente), il cambio di colore della barra, i calcoli delle date e
  delle caselle e quelli sotto i gruppi

### 🔧 v0.98.1 — 26/09/2026 · 12:51 — Il nome di una riga: il cursore in fondo, e la tastiera che resta con l'ordinamento
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Ci sono due piccoli bug nei database:
> 1) quando tocco su una cella per modificare il nome nella colonna Name,
> il text cursor mi va a sinistra del titolo, ma io lo vorrei a destra per
> modificare o cancellare il titolo;
> 2) quando c'è un sort attivato, se cancello il titolo di una pagina, la
> tastiera mi scompare.»

- **[Bug fix]** **Il cursore in fondo al nome.** Il campo del nome si
  apriva col cursore all'inizio (un campo di solo testo parte da lì); ora
  parte in fondo, pronto per continuare o cancellare
- **[Bug fix]** **Con un ordinamento, cancellando il nome la tastiera
  spariva.** Le righe della tabella non avevano il loro id: con l'ordine
  per nome, un nome vuoto va in fondo, la riga cambiava posto e il campo
  in cui si scriveva finiva su un'altra riga, che lo chiudeva. Ora ogni
  riga ha il suo id (`key(row.id)`), anche nei gruppi. In più, **mentre si
  scrive il nome l'ordine resta fermo** (`DatabaseViewModel.startTitleEdit`
  / `endTitleEdit`): la riga non salta a ogni lettera, e va al suo posto
  quando si preme "Done" o si esce dal campo
- **[Bug fix]** (trovato provando) **Il nome di una riga si riscriveva una
  volta sola**: la seconda volta la cella si apriva e si richiudeva
  nell'istante, perché si ricordava di aver già avuto il fuoco la volta
  prima. Ora si riparte da capo a ogni tocco
- Verificato sul telefono, sul database ordinato per nome dell'utente
  (righe "1", "2", "4", "5" e una senza nome), e su "Esempio" dell'altro:
  toccando "Esempio" il cursore sta dopo la "o"; "2" → cancellato → la
  tastiera resta, la riga resta seconda (vuota); "2" riscritto e "Done";
  di nuovo "2" (la seconda volta si apre) → "9" → resta seconda mentre si
  scrive → "Done" → va dopo "5"; poi "9" → "2" → resta quarta mentre si
  scrive → "Done" → torna seconda. I nomi sono di nuovo quelli di prima
- Notato: in un tentativo le coordinate erano vecchie (la pagina si era
  spostata chiudendo la tastiera) e un tocco è finito sulla riga "Riga
  dentro il toggle"; controllato col confronto sulla copia di stamattina:
  i 28 blocchi di My Space sono identici, niente cambiato

### 🆕 v0.98 — 26/09/2026 · 12:39 — La pagina di un quaderno, e un divisore sotto i quaderni
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Altre due cose: mettimi un divider sotto il riquadro dei Notebooks, così
> non mi confondo con il pulsante del Main menu. Poi, quando apro la
> sidebar e tocco su Notebook, indirizzami direttamente in una pagina a
> parte, con tutta la lista delle Section e delle Section group. Sulla
> sidebar, ovviamente, fammi funzionare tutto quanto normalmente, così
> invece che ricercare sezione per sezione, posso toccare le freccette a
> sinistra per aprire la gerarchia di tutto il Notebook»

- **[Nuova funzionalità]** **Un divisore sotto i quaderni**, come quello
  sotto "Main menu": l'ultima riga dei quaderni non si confonde più con
  "Main menu"
- **[Nuova funzionalità]** **Nella barra, il nome di un quaderno apre la sua
  pagina, la freccetta a sinistra apre e chiude l'albero** lì dov'è. La
  freccetta ora è un pulsante a sé, un po' più largo, per il dito. I gruppi
  si aprono e chiudono come prima
- **[Nuova funzionalità]** **La pagina di un quaderno**: in cima il quaderno
  (icona grande e nome), sotto **tutte** le sezioni e i gruppi, rientrati
  come nell'albero, ogni sezione con la sua linguetta, il lucchetto se è
  protetta e **quante pagine ha**, in fila sul bordo destro. Toccando una
  sezione la si apre (e "indietro" torna qui); tenendola premuta, il suo
  menu; la freccetta di un gruppo lo chiude **solo in questa pagina** (la
  barra resta com'è), e lo ricorda anche tornando da una sezione. In fondo
  "+ New section" e "+ New section group"; i tre puntini aprono il menu del
  quaderno, ed eliminandolo si torna indietro. Nella barra, il quaderno
  aperto è evidenziato
- **[Progetto]** La rotta `notebook/{notebookId}` e `NotebookScreen`; il
  conteggio delle pagine per sezione (`NotebookDao.observePageCounts`) si
  legge solo mentre la pagina è aperta, perché la tabella delle pagine
  cambia a ogni tasto
- **[Bug fix]** (trovato provando) I numeri delle pagine si spostavano a
  destra a ogni rientro invece di stare in colonna
- Verificato sul telefono, coi quaderni dell'utente ("My Notebook", con
  due gruppi uno dentro l'altro, e "My Notebook 2"): il divisore sopra
  "Main menu"; la freccetta di "My Notebook" lo chiude e lo riapre (lasciato
  aperto com'era); il nome apre la pagina con tutte e cinque le sezioni e i
  due gruppi, "0 pages" in colonna; "New Section Group 2" chiuso nella
  pagina e ancora aperto nel database; "New Section 3" → la sezione, con
  "My Notebook › New Section Group 1" → indietro → di nuovo la pagina del
  quaderno, col gruppo ancora chiuso. Nessun dato dell'utente toccato

### 🆕 v0.97 — 26/09/2026 · 09:06 — Le ripetizioni: senza toccare il giorno, i pallini arancioni, fino al 2100
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 152 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Quando vado su un database, creo una pagina, gli dico che voglio
> ripeterlo ogni, per esempio lunedì, ogni ultima domenica del mese, o
> altro, non me li fa visualizzare sul calendario quando seleziono la data
> e neanche sulla vista Calendar del database. Peraltro, nel calendario di
> selezione della data e della ripetizione, vorrei che ci fossero dei
> pallini arancioni (stesso usato quando seleziono un giorno) per
> rispettare la mia condizione del repeat. Quindi, se io per esempio
> voglio ripetere l'evento ogni sabato, su quel calendario di selezione
> della data, ogni sabato d'ora in poi avrà il pallino arancione. Fai sì
> che se uso la funzione Repeat non devo selezionare il giorno sul
> calendario, perché tanto gliel'ho detto già io di ripeterlo in base a
> ciò che desidero io. Metti anche un'altra opzione che mi consente di
> ripetere quell'evento 1) o da oggi (il giorno corrente) fino al 2100 2)
> oppure da un giorno preciso che decido io fino al 2100»

- **[Bug fix]** **La ripetizione senza un giorno toccato andava persa.**
  Scegliendo solo "Repeat" senza toccare un giorno sul calendario, "Done"
  trovava la data vuota e **svuotava la cella**: la regola non arrivava
  mai al database, e per questo non si vedeva né sul calendario né nella
  vista Calendar (che le ripetizioni le sa già disegnare). Sul telefono la
  riga "Esempio" dell'utente aveva infatti la data vuota. Ora, con una
  ripetizione, **il giorno non va toccato**: si parte da oggi
- **[Nuova funzionalità]** **I pallini arancioni della ripetizione** sul
  calendario della data, gli stessi del giorno scelto: con "ogni sabato",
  tutti i sabati **da quel giorno in avanti** (quelli prima no). Il giorno
  da cui parte, se non è uno di quelli, ha solo il contorno arancione
- **[Nuova funzionalità]** **Da quando**: sotto "Repeat", quando c'è una
  ripetizione, la riga **"Starts"** ("Today" o la data). Toccandola:
  **"From today until 2100"** oppure **"From a day I choose until 2100"**
  (col selettore della data). Tutte e due fanno finire la ripetizione il
  31/12/2100, che si legge nel riassunto ("Weekly on Saturday, until
  2100年12月31日")
- **[Nuova funzionalità]** **"L'ultima domenica del mese" senza partire da
  un'ultima domenica.** In "Custom…" con "month": "On [giorno]" col numero
  da scrivere, oppure "The first / second / third / fourth / last
  [giorno]" da un menu, col giorno della settimana scelto dai pallini sotto.
  Prima si poteva solo "come il giorno toccato sul calendario"
- **[Nuova funzionalità]** Nella cella, accanto alla data, le **frecce in
  cerchio** di una data che si ripete (come la campanella della notifica)
- **[Progetto]** `Recurrence` ha tre campi in più, `monthDay`, `nthWeek`
  (1–4 o `LAST_WEEK`) e `nthWeekday`, salvati nella regola (`MDAY`,
  `NTH`, `NWD`): null vuol dire "dalla data di partenza", quindi le regole
  scritte prima si leggono come prima. Niente migrazione: la regola sta
  già nel testo della cella. Anche il modo rapido "Monthly on the last
  Saturday" scrive settimana e giorno nella regola, così cambiando poi il
  giorno di partenza resta l'ultimo sabato. "Ogni giorno" salta dritto al
  periodo mostrato: dal 2026 al 2100 sono ventisettemila giorni, oltre il
  tetto di sicurezza del calcolo. Test `RecurrenceTest` (5)
- Verificato sul telefono, sulla riga "Esempio" del primo database di My
  Space (data vuota): Repeat → "Weekly on Saturday" senza toccare giorni →
  pallini su 26 settembre, 3 e 10 ottobre, non sui sabati passati; "Starts
  Today". "Starts" → "From a day I choose until 2100" → 5 ottobre →
  contorno sul 5, pallini da sabato 10; il riassunto "…, until
  2100年12月31日". "Custom…" → month → "The last Sunday" (menu e pallino
  della domenica) → pallino sul 25 ottobre. "Done" → salvato
  `FREQ=MONTHLY;…;NTH=5;NWD=SUNDAY;UNTIL=…`, la cella con la data e le
  frecce. Vista Calendar (messa per la prova e poi rimessa Table):
  "Esempio" il 25 ottobre. Alla fine la data di "Esempio" è stata
  cancellata, com'era prima
- Da sapere: la cella mostra il giorno da cui la ripetizione parte, non la
  prossima volta in cui capita

### 🆕 v0.96 — 26/09/2026 · 08:53 — I quaderni, come in OneNote
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne
quello che è elencato in fondo; 147 test passati; commit solo in locale)*

L'utente, parola per parola (con tre foto della colonna di sinistra di
OneNote: l'albero dei quaderni, il menu di una sezione col tasto destro, e
una pagina aperta con titolo, data e ora):
> «Creami la sezione Notebooks (che in realtà hai già creato, ma attivala)
> nella sidebar (ti mando una foto così capisci meglio come sarà
> strutturata la gerarchia). Il concetto sarà lo stesso identico di come
> funziona OneNote, ma sarà un qualcosa esclusivo a sé stesso, che non avrà
> niente a che fare con tutto ciò che inizia dal menu principale e che non
> avrà alcuna interazione con tutto ciò che sta nel sistema gerarchico del
> menu principale. Workspace e Notebooks funzioneranno da sole, l'una
> libera dall'altra. La "gerarchia" funziona in pratica che creo vari
> quaderni (con un'icona che se vuoi ti metto io, altrimenti le classiche
> icone da Notebook prese da Flaticon) quelli che vedi in alto con l'icona
> del quaderno blu, con sezioni o sezioni da gruppo (che hanno lo stesso
> concetto dei toggle di Notion) e dentro le sezioni posso creare delle
> pagine a scrolling infinito (ovviamente infinito da creare, di base la
> grandezza di una pagina è solo quello che si vede, poi se scrivo tante
> cose, non avrò limiti finché scrivo)
> (non far caso a Copilot Notebook, non mi interessa)»

- **[Nuova funzionalità]** **La voce "Notebooks" della barra laterale è
  accesa**: è la fascia di prima (un altro colore, sotto i widget), con la
  freccetta per aprirla e chiuderla — la scelta si ricorda — e il **+** per
  un quaderno nuovo. Sotto, **l'albero di OneNote**: i quaderni col loro
  quaderno disegnato e la freccetta; dentro, le **sezioni** con la
  linguetta colorata e i **gruppi di sezioni**, che si aprono e si
  chiudono come i toggle e possono contenere altre sezioni e altri gruppi
  (come "Languages" → "Deutsch" → "General" nella foto). In fondo a ogni
  quaderno aperto, "+ New section". La sezione aperta è evidenziata
- **[Nuova funzionalità]** **Un quaderno nuovo**: nome e colore (i dodici
  di OneNote), e nasce con la sua prima sezione "New Section 1", come in
  OneNote. **L'icona**: il quaderno disegnato nel colore scelto, oppure
  **un'immagine a scelta dalla galleria** — per esempio un'icona scaricata
  da Flaticon — da "Notebook icon" nel suo menu. Scaricarle da Flaticon
  non lo posso fare io: l'app non va in rete (e le icone hanno la loro
  licenza); se l'utente me le manda, le metto fra quelle pronte
- **[Nuova funzionalità]** **Tenendo premuta una voce** si apre il suo menu,
  le voci del tasto destro di OneNote che hanno senso qui:
  - quaderno: Rename, Notebook color, Notebook icon, New section, New
    section group, Move up, Move down, Delete
  - sezione: Rename, Section color, **Password protect this section**
    (con la password dell'app o l'impronta, come le pagine), New section,
    New section group (nascono accanto a lei), Move up, Move down, Delete
  - gruppo: Rename, New section e New section group (nascono dentro),
    Move up, Move down, Delete
  Il nome nuovo si propone già scritto e selezionato ("New Section 2",
  "New Section Group 1"…). Una sezione nuova prende il colore dopo quello
  dell'ultima, e si apre subito
- **[Nuova funzionalità]** **Una sezione aperta**: in alto dove sta
  ("Prova quaderni › Languages"), il nome con la linguetta, **"Add page"**
  come in OneNote, e le pagine nel loro ordine, col giorno in cui sono
  nate. Toccando una pagina la si apre; tenendola premuta, Move up, Move
  down, Delete page. I tre puntini aprono il menu della sezione. Su un
  telefono le tre colonne di OneNote non ci stanno: la colonna delle
  sezioni è la barra laterale, quella delle pagine è questa schermata
- **[Nuova funzionalità]** **Le pagine dei quaderni** usano lo stesso
  editor delle altre, quindi **scorrono senza fine**: all'inizio c'è solo
  quello che si vede, e si allungano man mano che si scrive. In cima,
  **come in OneNote**: il titolo ("Untitled page" in grigio finché è
  vuoto), una riga sottile e sotto **il giorno, la data e l'ora** in cui
  la pagina è nata (nel formato di data scelto nelle impostazioni). Niente
  icona né copertina. Si disegna, si blocca, si protegge con la password,
  si guarda la cronologia come nelle altre pagine
- **[Nuova funzionalità]** **Due mondi separati**, come chiesto. Le pagine
  dei quaderni non compaiono mai nel menu principale: né nell'albero delle
  pagine, né nella ricerca, né fra le pagine da cui far partire l'app, né
  fra i database da mostrare in una vista collegata. E dal quaderno non si
  arriva al menu principale: nei menu "/" e "+" delle pagine dei quaderni
  non ci sono Page, Link to page e i database; nei tre puntini non ci sono
  preferiti, ricerca, Duplicate e Move to. **Eliminare** (una pagina, una
  sezione, un gruppo, un quaderno) è **per sempre**, perché il cestino è
  del menu principale: prima lo chiede, dicendo quante pagine se ne vanno.
  Una sezione protetta, o un gruppo o un quaderno che ne contiene una,
  chiede prima impronta o password
- **[Progetto]** **Database alla versione 35**: le tabelle `notebooks` e
  `notebook_sections` (sezioni e gruppi insieme, perché formano un unico
  albero con un ordine solo), e `pages.notebookSectionId`. Le pagine sono
  pagine vere, così hanno tutto l'editor gratis; `orderIndex` è il loro
  ordine nella sezione. `NotebookRepository`, `NotebooksViewModel`,
  `NotebookSectionViewModel`, `Notebooks.kt` (barra, menu, schermata della
  sezione), `NotebookStrings` nelle otto lingue. `lockingPageOf` ora sa
  delle sezioni protette; "Remove password" nelle impostazioni conta anche
  quelle. Test `NotebookRepositoryTest` (5) e `MigrationTest` 34→35, col
  file `v34.sql`
- **Fuori, per ora** (dal menu di OneNote nella foto): Export, Move or
  Copy, Merge into Another Section, Copy Link to Section (e Copilot, che
  non interessa). Neanche l'ordinamento delle pagine, le sottopagine
  rientrate e un cestino dei quaderni. Si aggiungono se servono
- Verificato sul telefono (con un quaderno di prova, poi eliminato): prima
  di installare, copia del database del telefono; migrazione alla 35 sul
  posto (la colonna nuova in fondo a `pages`), le 10 pagine dell'utente
  tutte lì. "Notebooks" acceso nella barra, "+ New notebook" con nome
  selezionato e colori → "Prova quaderni" col quaderno blu e "New Section
  1"; tenuto premuto → il menu del quaderno → New section group
  "Languages" → dentro, New section "Deutsch" (rossa) → **si apre da
  sola**, con "Prova quaderni › Languages"; "Add page" → la pagina con
  "Untitled page", la riga e "Saturday, 2026年09月26日 08:47"; titolo
  "Esempio" e una riga di testo; nel menu "/" niente Page, e "/data" →
  "Nothing with that name"; i tre puntini con Draw, Delete page, Lock page,
  Lock with password, Updates, 2 words. Indietro → "Esempio" nell'elenco.
  Nella barra l'albero come nella foto, "Deutsch" evidenziata. "Password
  protect this section" → la sezione si chiude subito (lucchetto accanto
  al nome, "This section is locked", l'impronta chiesta da sola; annullata,
  non l'ho aperta). Delete del quaderno → "Everything inside goes with it,
  1 page included" → eliminato: nel database zero quaderni, zero sezioni,
  zero pagine di quaderni, le 10 pagine dell'utente intatte
- **[Bug fix]** (trovato provando) Il blu del quaderno nuovo non era uno
  dei dodici colori, e nella scelta nessuno aveva la spunta: ora è il
  primo. Verificato: la spunta sul blu
- **Non provato sul telefono**: l'impronta chiesta prima di eliminare una
  sezione protetta (per chiudere la prova sarebbe servita l'impronta
  dell'utente), l'immagine come icona di un quaderno, i colori cambiati
  dopo, Rename e Move up/down dai menu (su e giù li prova il test)

### 🔧 v0.95.1 — 26/09/2026 · 08:27 — La freccetta del toggle, un pelino più su
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; commit solo
in locale)*

L'utente, parola per parola:
> «Ho controllato, il lucchetto c'è.
>
> Piccolo buggettino: se vedi in fondo, dopo il secondo database a vista
> Gallery, c'è un toggle. La freccetta è ancora storta, alzamela di un
> pelino»

- **[Bug fix]** **Il triangolino del toggle stava un po' basso** rispetto al
  titolo. Il triangolo è in mezzo al suo quadrato di 24dp e la riga del
  titolo è in mezzo ai suoi 24, ma le lettere non stanno in mezzo alla loro
  riga: stanno più su, perché sotto c'è il posto delle gambe di g, p e q.
  Misurato sul telefono (in pixel, sulla schermata): il triangolo aveva il
  centro 5 pixel sotto il centro delle minuscole e 9 sotto quello delle
  maiuscole. Ora sale di 2dp (`TOGGLE_ARROW_LIFT`) e il suo centro sta fra
  i due, 2 pixel sopra le minuscole e 2 sotto le maiuscole. Vale per ogni
  toggle, chiuso e aperto
- Verificato sul telefono, sul toggle "Riga dentro il toggle" in fondo a My
  Space: prima 2422–2455 (in pixel), dopo 2415–2448, con la "R" del titolo
  a 2414–2445

### 🆕 v0.95 — 26/09/2026 · 08:20 — Il lucchetto accanto ai tre puntini
*(Claude Code sul PC; verificato sul telefono lo stesso giorno
dall'utente: «Ho controllato, il lucchetto c'è»; 141 test passati; commit
solo in locale)*

L'utente, parola per parola:
> «Ho provato, funziona. Mi aggiungi un'icona uguale identica a quella dei
> tre puntini per ogni pagina ma di un lucchetto chiuso alla sinnistra di
> quella dei tre puntini per descrivere quella come protetta da password?»

- **[Nuova funzionalità]** Su una pagina bloccata con la password, **alla
  sinistra dei tre puntini** c'è un secondo pulsante **identico** — lo
  stesso cerchio grigio semitrasparente, la stessa grandezza — con **un
  lucchetto chiuso**. Toccato, dice "Protected by a password"
  ("Protezione con password attiva"). Anche sui database aperti a schermo
  intero, accanto ai loro tre puntini (lì senza cerchio, come i tre
  puntini). Compare solo sulla pagina bloccata lei stessa, come
  l'interruttore "Lock with password" nel menu: le sue sottopagine no
- **Verificato sul telefono, dalla voce di prima**: il database che
  l'utente ha bloccato ("Untitled", dentro "Titolo tre H3") **è diventato
  un collegamento** col lucchetto accanto al nome — la conversione, fin qui
  provata solo dal test, funziona anche sul telefono
- Il lucchetto accanto ai tre puntini si vede solo con la pagina aperta,
  e le pagine bloccate si aprono con l'impronta o la password dell'utente:
  **l'ha guardato l'utente**, aprendo "Untitled" in "Titolo tre H3"

### 🔧 v0.94.1 — 26/09/2026 · 08:16 — La scheda della galleria di una pagina bloccata
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 141 test
passati; commit solo in locale)*

L'utente, parola per parola:
> «Funziona tutto, a quanto pare. Una cosa: nel secondo database, quello in
> vista Gallery, fai sì che le pagine bloccate facciano vedere uno sfondo di
> default con un lucchetto bianco. Adesso, ho bloccato una pagina con una
> cover, ma la cover si vede. Vorrei evitare questo, ecco»

E subito dopo:
> «Ovviamente, perdonami, questo deve vale per tutti i database, non solo
> per questo che ti ho appena menzionato»

- **[Bug fix]** Nella vista Gallery **di qualunque database** (anche dentro
  una pagina, anche una vista collegata), la scheda di una pagina bloccata
  con la password mostrava la sua copertina. Ora al posto dell'anteprima
  c'è **uno sfondo grigio scuro sfumato, sempre uguale, con un lucchetto
  bianco** in un cerchio; il lucchetto cresce con la dimensione della
  scheda. Vale per "Page cover" e per "Page content"; con "None" la scheda
  non ha anteprima e resta com'è. Tenendo premuta la scheda non c'è più
  "Reposition cover", che avrebbe fatto rivedere la copertina
- **[Bug fix]** (trovato lavorandoci) La lente accanto a Sort cercava anche
  dentro il testo delle pagine bloccate, e trovandolo illuminava la riga:
  ora le salta, come la ricerca generale
- **[Progetto]** La copertina e il testo di una pagina bloccata non escono
  nemmeno dal database (`observeRowCovers`, e le tre query della lente);
  le righe bloccate arrivano alla galleria da `observeLockedRowIds`, seguite
  solo mentre la galleria ha un'anteprima. Lo sfondo è `LockedCardPreview`,
  scuro anche col tema chiaro perché il lucchetto resti bianco. Test in
  `PageLockTest`
- Verificato sul telefono, nel database a galleria di My Space: la scheda
  "UntitledZeta" (bloccata dall'utente, con una copertina) mostra lo sfondo
  col lucchetto, "Gioco uno" accanto la sua copertina; tenuta premuta,
  "UntitledZeta" ha Icon, Edit property e Delete, senza "Reposition cover",
  che invece "Gioco uno" ha ancora

### 🆕 v0.94 — 26/09/2026 · 08:04 — Bloccare pagine e database con la password o l'impronta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne il
database che diventa un collegamento, vedi in fondo; 140 test passati;
commit solo in locale)*

Richiesta dell'utente, parola per parola:
> «Possibilità di bloccare pagine e database con una password o con
> l'impronta (nelle opzioni dell'icona dei tre puntini in alto a destra).
> Se blocco un database, fai sì che diventi una pagina come se avessi
> usato il Turn into page, e poi mi chiede la password o l'impronta se
> voglio aprirla»

- **[Nuova funzionalità]** **"Lock with password"** nei tre puntini di ogni
  pagina e di ogni database, sotto "Lock page", con l'icona dell'impronta e
  l'interruttore. Non c'è in My Space: bloccarla chiuderebbe l'app intera.
  **La prima volta si sceglie la password** (almeno 4 caratteri, scritta
  due volte) e se aprire anche con l'impronta; dopo, bloccare è immediato.
  Bloccata, la pagina si richiude subito: al suo posto un lucchetto, e
  **la richiesta dell'impronta parte da sola**; "Use password" passa alla
  password. Con la pagina si chiude **tutto quello che ha dentro**:
  sottopagine, database, pagine delle righe. Nella barra laterale non
  mostra più cosa contiene, la ricerca ne trova solo il titolo (senza il
  testo), e il suo collegamento ha accanto un piccolo lucchetto. Aperta,
  resta aperta finché ci si muove al suo interno; andando altrove, o
  uscendo dall'app per più di 30 secondi, si richiude. Togliere il blocco
  (l'interruttore spento) chiede di nuovo impronta o password
- **[Nuova funzionalità]** **Un database bloccato, dentro una pagina,
  diventa un collegamento**, come con "Turn into page", e aprirlo chiede
  impronta o password. Si chiudono anche le sue righe e le sue viste
  collegate: una vista collegata dentro una pagina aperta mostra, al posto
  della tabella, la richiesta di sbloccarlo
- **[Nuova funzionalità]** In Settings, sotto "Time zone", **"Password for
  locked pages"** (Set / Not set): l'impronta sì o no, **"Change
  password"** (prima si conferma che si è noi) e **"Remove password"**,
  che funziona solo quando nessuna pagina è più bloccata — altrimenti
  resterebbero chiuse per sempre. Dalla finestra della password,
  **"Forgot the password?"** chiede il blocco schermo del telefono (PIN,
  sequenza o impronta) e poi una password nuova
- **[Progetto]** **Database alla versione 34**: `pages.isProtected`. La
  password non si salva: si tiene solo un hash PBKDF2 col suo sale, nelle
  preferenze dell'app (`AppLock`, `PasswordHash`). Il cancello `LockGate`
  sta davanti a ogni pagina e database a schermo intero (`AppNavHost`) e
  davanti ai database dentro le pagine; `lockingPageOf` risale
  collegamenti, righe e viste collegate per trovare chi blocca cosa. La
  ricerca e la barra laterale filtrano in `PageRepository`; le anteprime
  delle schede della galleria saltano le pagine bloccate. L'impronta è il
  `BiometricPrompt` di sistema (permesso `USE_BIOMETRIC`). Test
  `PageLockTest` e `MigrationTest` (33→34, col file `v33.sql`)
- **[Bug fix]** (trovato provando) Tornando indietro da una pagina bloccata
  appena aperta, la richiesta dell'impronta ripartiva dalla schermata che
  se ne stava andando: ora parte solo quando la schermata è davvero
  davanti
- Verificato sul telefono: la prima volta la finestra per scegliere la
  password → pagina bloccata, col lucchetto; **l'impronta l'ha provata
  l'utente**: «L'ho fatto io, ed ho sbloccato la pagina con l'impronta».
  Password sbagliata → "Wrong password"; quella giusta → la pagina si
  apre; indietro → nessuna richiesta; riaperta → la richiesta torna; il
  lucchetto accanto al collegamento in My Space; togliere il blocco dal
  menu chiede di confermare. Alla fine Settings → "Remove password" →
  "Password removed", "Not set": **la password di prova non c'è più**, e
  la prima volta che l'utente blocca qualcosa sceglie la sua. **Il
  database che diventa un collegamento non è stato provato sul
  telefono**: lo prova il test
- Notato: i disegni della pagina "Titolo tre H3" erano spariti già prima
  dell'installazione; li aveva cancellati l'utente («L'ho fatto io»), non
  è un errore

### 🆕 v0.93 — 26/09/2026 · 00:23 — Svuotare la cache, cancellare tutto il contenuto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne la
cancellazione vera, vedi in fondo; 133 test passati; commit solo in
locale)*

L'utente, parola per parola:
> «Ho provato la Color cell sulla galleria, funziona.
>
> Altra feature: dammi la possibilità, mettendo due voci in Settings, sotto
> Time Zones, che mi consentono di cancellare tutta la cache dell'app e
> tutto il contenuto»

- **[Nuova funzionalità]** In Settings, **sotto "Time zone"** e separate da
  una riga, due voci:
  - **"Clear cache"**, con accanto quanto occupa: toglie i soli file
    temporanei, che l'app rifà quando servono. Chiede conferma, poi dice
    "Cache cleared". Pagine, database, disegni e immagini non si toccano
  - **"Delete all content"** ("Pagine, database, disegni, immagini e
    cestino"): cancella ogni pagina, database, riga, disegno, la
    cronologia delle modifiche, il cestino e le immagini delle pagine.
    Restano le impostazioni e i widget della barra. **Non si può
    annullare**, quindi il pulsante rosso "Delete everything" si accende
    solo dopo aver **scritto la parola** che la finestra chiede ("DELETE"
    in inglese, "ELIMINA" in italiano). Fatto, My Space rinasce vuota e
    **l'app riparte da capo**: le schermate aperte avevano ancora in mano
    il contenuto di prima, e un loro Annulla l'avrebbe rimesso
- **[Progetto]** `AppCleanup`: la cache è `cacheDir` (e quella esterna);
  il contenuto si cancella con `clearAllTables()` di Room, poi la pagina
  principale si ricrea subito (il resto dell'app dà per scontato che ci
  sia), si svuota la cartella delle immagini (`PageImageStore.deleteAll`)
  e si rimette la sveglia delle notifiche. Test `AppCleanupTest`, con
  Robolectric, su un database e dei file di prova
- Verificato sul telefono: le due voci sotto "Time zone"; "Clear cache" (la
  cache era già vuota, 0 B) → conferma → "Cache cleared". "Delete all
  content" → la finestra con l'avviso e la parola da scrivere, "Delete
  everything" spento finché la parola non c'è → annullata: le 9 pagine
  dell'utente ci sono tutte. **La cancellazione vera non è stata fatta sul
  telefono** (avrebbe cancellato le note dell'utente): la prova il test
- Notato: in un primo tentativo di arrivare alle impostazioni il menu
  laterale non si era aperto, e un tocco è finito sull'icona delle
  impostazioni del database a galleria di My Space; nessuna scelta toccata

### 🆕 v0.92 — 26/09/2026 · 00:14 — La proprietà "Color cell"
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, tranne le
targhette della bacheca e della galleria; 131 test passati; commit solo in
locale)*

Richiesta dell'utente, parola per parola:
> «Nuova feature: ai database (semplici e complessi) aggiungi una
> proprietà, chiamata Color cell, che mi consente letteralmente di riempire
> la cella con un colore a mia scelta. Dammi anche la possibilità di
> scriverci sopra del testo (possibilità, non obbligo)»

- **[Nuova funzionalità]** **"Color cell"** fra i tipi di proprietà (col
  secchiello), nei database normali e in quelli semplici. Nella tabella il
  colore **riempie tutta la cella**; il testo, se c'è, sta sopra, **nero o
  bianco** a seconda di cosa si legge meglio su quel colore. Toccando la
  cella si apre la sua finestra: com'è, il **testo (facoltativo)** e il
  **colore**, che si sceglie col selettore del pennello (spettro, grigi,
  RGB, Hex; "No color" lo toglie). Si salva con "Done". Nella scheda di una
  riga è un riquadro colorato; nelle schede della bacheca e della galleria
  e nell'elenco, una targhetta del suo colore. Col database bloccato si
  guarda e basta
- **[Progetto]** Il valore sta nella cella come gli altri, in una stringa:
  `"#RRGGBB|testo"`, solo il colore o solo il testo (`ColorCellValue.kt`).
  Nessuna migrazione: il tipo di una colonna è già salvato come testo.
  L'ordinamento e la ricerca guardano il testo, non il codice del colore;
  la tabella non si raggruppa per una cella colorata (il gruppo si
  chiamerebbe "#E03E3E"). Cambiando il tipo della colonna i valori restano
  quelli, come per gli altri tipi. Test `ColorCellValueTest`
- **[Bug fix]** (trovato provando) Sul verde acceso scelto nella prova
  (#12F20D) il testo veniva bianco e non si leggeva: la soglia fra nero e
  bianco è scesa a metà
- Verificato sul telefono, nella tabella "Untitled" di My Space: "Color
  cell" nell'elenco dei tipi → colonna nuova, cella "—"; toccata → la
  finestra; colore dal selettore (#12F20D) → anteprima verde; testo
  "Fatto" → "Done" → la cella tutta verde con "Fatto" (salvato
  "#12F20D|Fatto"); la scheda della riga → riquadro verde con "Fatto". Poi
  la proprietà di prova tolta ("Delete property"): la tabella è com'era

### 🆕 v0.91 — 26/09/2026 · 00:03 — Via il cestino accanto ai collegamenti alle pagine
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 126 test
passati; commit solo in locale)*

Richiesta dell'utente, parola per parola:
> «Toglimi l'icona del cestino a destra delle pagine nei campi di testo. Mi
> basta tenerci premuto se voglio eliminarla»

- **[Rimosso]** Il cestino a destra di ogni collegamento a una pagina
  dentro il testo (e la sua scritta "Remove link"). Per eliminare si tiene
  premuto il collegamento: nel menu del blocco, in fondo, c'è "Move to
  trash". Su una pagina bloccata il menu non si apre, come prima
- Verificato sul telefono, su "Titolo tre H3" in My Space: il cestino non
  c'è più; tenendo premuto si apre il menu con "Move to trash" (richiuso
  senza toccare niente)

### 🆕 v0.90 — 25/09/2026 · 23:57 — Il dito disegna, due dita scorrono, il palmo non conta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, le due dita
dall'utente il 26/09/2026, vedi in fondo; 126 test passati; commit solo in
locale)*

L'utente, parola per parola:
> «Ma è normale che con il dito non disegna? È un bug?»

Non era un bug: la prima volta che si usava la S Pen il dito smetteva di
disegnare e scorreva la pagina, per il palmo. Lo diceva un avviso di pochi
secondi, e la mano per riaccenderlo stava in fondo alla barra, fuori dallo
schermo. Proposto di spostare la mano, o di non spegnere più il dito da
solo. Risposta, parola per parola:
> «Sì. Quando disegno lo scorrimento lo devi mettere solo con due dita»

- **[Nuova funzionalità]** **Mentre si disegna, un dito disegna e la pagina
  scorre solo con due dita**, sempre. Con la mano spenta un dito non fa
  niente (non scorre più la pagina)
- **[Nuova funzionalità]** **La mano sta accanto a ✓, Annulla e Ripeti**,
  sempre in vista, blu quando il dito disegna
- **[Rimosso]** Lo spegnimento automatico del dito al primo uso della S Pen,
  e il suo avviso. La scelta salvata ha un nome nuovo (`fingerDrawing`):
  quella spenta da sola sul telefono dell'utente si è dimenticata, e il dito
  è tornato a disegnare
- **[Nuova funzionalità]** **Il palmo non conta**, al posto dello
  spegnimento: mentre la S Pen è vicina allo schermo (anche solo sospesa) o
  lo tocca, e per 0,6 secondi dopo, i tocchi della mano si ignorano; e se la
  mano ha cominciato un segno prima che la penna tocchi, quel segno si butta
  e disegna la penna
- Verificato sul telefono, su "Titolo tre H3" (dove ci sono i disegni
  dell'utente, lasciati com'erano): la mano accanto a Ripeti, blu; un dito
  → disegna; S Pen (simulata) e subito dopo un dito → la linea del dito non
  c'è; un dito un secondo dopo → disegna. Le tre linee di prova tolte con
  Annulla (restano i 7 tratti dell'utente). **Lo scorrimento con due dita
  l'ha provato l'utente il 26/09/2026: "funziona"**. Da provare a mano
  resta la penna sospesa col palmo appoggiato
- Notato: la S Pen simulata da adb aveva lasciato accesa l'icona mobile
  della S Pen di Samsung, come se la penna fosse fuori dal foro. L'utente
  l'ha estratta e rimessa, e l'icona è sparita. Da qui in poi le prove di
  disegno si fanno col dito, e quello che vuole la penna vera lo prova
  l'utente

### 🆕 v0.89 — 25/09/2026 · 23:49 — Il colore delle penne col selettore del pennello
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 126 test
passati; commit solo in locale)*

Richiesta dell'utente, parola per parola:
> «Il colore di quando disegno fammelo scegliere con la stessa palette
> colori e spettro dei grigi del pennello sulla Toolbar»

- **[Nuova funzionalità]** Nella finestra di una penna, sotto "Colors", c'è
  il colore in uso (il tondo e il codice, "#2383E2"); toccandolo si apre
  **il selettore del pennello**, lo stesso della barra del testo: il
  riquadro di tinta e saturazione, la striscia dei grigi dal bianco al
  nero, Rosso/Verde/Blu ed Hex. Si apre sul colore della penna. Al posto di
  "No color" c'è **"Text color"**, che rimette il colore del testo (quello
  che segue il tema); sotto l'anteprima c'è il nome della penna invece di
  "Text color"
- **[Rimosso]** I venti tondi dei colori fissi della finestra della penna
- **[Progetto]** `ColorPickerSheet` prende due scritte da fuori
  (`previewLabel`, `noColorLabel`); il pennello e le barre dei widget non
  cambiano. Il colore della penna passa al selettore come "#RRGGBB" e torna
  pieno (`inkHex`, `hexToInkColor`, test `InkColorTest`)
- Verificato sul telefono: penna blu → "Colors" → il selettore del
  pennello su #2383E2 (35, 131, 226), con "Text color" e "Apply"; "Apply"
  sullo stesso colore → la penna resta #2383E2. Le altre penne sono come le
  aveva lasciate l'utente
- Notato: l'utente aveva già disegnato su "Titolo tre H3" con la S Pen vera
  (scarabocchi bianchi e rosa, con la prima penna fatta rosa e più
  spessa): **la pressione funziona**, il tratto si allarga e si stringe.
  Lasciati com'erano. Nella prova è rimasta in mano la penna blu

### 🆕 v0.88 — 25/09/2026 · 23:42 — Disegnare a mano sulle pagine, anche con la S Pen
*(Claude Code sul PC; verificato sul telefono lo stesso giorno, con la S Pen
simulata da adb, vedi in fondo; 123 test passati; commit solo in locale)*

Richiesta dell'utente, parola per parola, con una foto della barra "Drawing
Tools" di OneNote (una gomma aperta sul menu Thickness / Types — Stroke
Eraser, Point Eraser — e sei penne di colori diversi, l'ultima gialla un
evidenziatore):
> «Dammi la possibilità di disegnare su una pagina toccando l'icona di una
> matita che metterai dentro i tre puntini di ogni pagina. Non preoccuparti
> se disegno sopra del testo normale. Se voglio disegnare qualcosa, metterò
> dei divider sopra e sotto come dei "confini". Ovviamente, ho anche la
> scelta di disegnare sopra qualsiasi cosa (iniziando dalla fine della
> copertina NON compresa in giù), tipo sopra un database se voglio cerchiare
> qualcosa. Mettimi le opzioni che vedi in foto, con la gomma che cancella
> solo quello che la gomma trova oppure il tratto completo che viene toccato
> (stroke e point come in Onenote), con la verie tipologie di penne,
> pennarelli e l'evidenziatore. Per tutti quanti metti anche il thickness e
> i colori (tranne per la gomma ovviamente). Dimmi se riesci a far
> funzionare la mia SPen»

- **[Nuova funzionalità]** **"Draw"**, con la matita, nei tre puntini di
  ogni pagina di testo (anche le pagine delle righe; non nei database a
  schermo intero). Si disegna **su tutta la pagina dalla fine della
  copertina in giù**, copertina esclusa: sopra il testo, il titolo, un
  database. In fondo c'è la barra del disegno al posto di quella del testo:
  **✓** per finire (anche il gesto indietro), **Annulla** e **Ripeti** (solo
  dei disegni, separati da quelli del testo), la **gomma** e sei penne come
  nella foto, e la **mano** per disegnare col dito. Mentre si disegna il
  testo si legge e basta; finito, i disegni restano e si vedono sempre
- **[Nuova funzionalità]** **Le penne**: penna, matita, pennarello ed
  evidenziatore. Di partenza: penna del colore del testo (bianca sul tema
  scuro, nera sul chiaro), penna blu, penna rossa, matita grigia,
  pennarello verde, evidenziatore giallo. Toccando lo strumento già in mano
  (o tenendolo premuto) si apre la sua finestra, come nella foto:
  **Thickness** (cinque spessori, e "−" "+" per i passi in mezzo), **Types**
  (quale delle quattro è) e **Colors** (dal 25/09/2026 col selettore del
  pennello; prima venti colori fissi). La penna si allarga premendo di più con la S Pen; la matita
  è un po' trasparente; l'evidenziatore è trasparente, a punta piatta, e
  sta sotto gli altri tratti. Le scelte si ricordano
- **[Nuova funzionalità]** **La gomma**, con **Thickness** e **Types**:
  **Stroke Eraser** toglie tutto il tratto toccato, **Point Eraser** solo
  quello che passa sotto la gomma (il tratto si spezza). Mentre si cancella
  si vede il cerchio della gomma. Una passata è un Annulla solo
- **[Nuova funzionalità]** **La S Pen.** Sì, funziona: il telefono è un
  Galaxy S25 Ultra (SM-S938B) e la S Pen arriva all'app come penna vera, con
  la **pressione**. La prima volta che la si usa il dito smette di
  disegnare e **scorre la pagina** (un avviso lo dice): così il palmo
  appoggiato non lascia segni. La mano nella barra lo fa tornare a
  disegnare; col dito che disegna, **due dita** scorrono. Col **tasto
  laterale** premuto la S Pen **cancella**, come su Samsung Notes (la
  gomma scelta nella barra). Quello che non c'è: i gesti a distanza (Air
  actions), che la S Pen del S25 Ultra non ha più
- **[Progetto]** **I disegni seguono quello su cui li si è fatti.** Ogni
  tratto è attaccato alla voce della pagina dove è cominciato — il gruppo
  di righe fra due divisori, un database, un toggle, o la testata col
  titolo — con le coordinate contate dal suo angolo (`InkStrokeEntity`,
  `InkGeometry.kt`). Se sopra si aggiungono righe o un divisore, il disegno
  scende con le sue righe. Cancellato quello su cui era fatto (il database
  che si era cerchiato), il disegno non si vede ma resta: annullando la
  cancellazione torna. Duplicando la pagina i disegni vengono con la copia
- **[Progetto]** **Database alla versione 33**: la tabella `ink_strokes`,
  un tratto per riga (i punti in una stringa compatta, "x,y,pressione" in
  dp), cancellata con la sua pagina. Test `MigrationTest.from32…` su un
  file alla 32 (`schema/v32.sql`); 8 test nuovi dei conti
  (`InkGeometryTest`: salvataggio dei punti, le due gomme, gli ancoraggi,
  gli spessori). Gli strumenti stanno nelle preferenze (`InkPrefs`), non
  nel database, come le impostazioni
- Verificato sul telefono, su "Titolo tre H3", con la S Pen simulata da
  adb (`input stylus`): "Draw" nei tre puntini → barra; due tratti →
  salvati, attaccati alla riga sotto il titolo, e l'avviso "S Pen
  rilevata"; menu della penna come nella foto; Stroke Eraser su una linea →
  sparita, Annulla → tornata; Point Eraser in mezzo all'altra → spezzata in
  due; matita, pennarello ed evidenziatore sul titolo (attaccato alla
  testata); il dito, spento, non disegna; **divisore messo sopra le righe
  disegnate → i tratti sono scesi con le righe**, l'evidenziatore rimasto
  sul titolo; un tratto è rimasto dopo aver ucciso e riaperto l'app. Poi
  tutto rimesso com'era: testo, disegni (zero) e strumenti di partenza.
  **Da provare con la S Pen vera**: la pressione (adb la dà sempre piena),
  il tasto laterale, le due dita
- Notato: le righe-notifica di prova in "Titolo tre H3" non c'erano più.
  Dalla cronologia della pagina le ha svuotate l'utente alle 23:16–23:17,
  prima dell'aggiornamento: non è stata la migrazione

### 🆕 v0.87 — 25/09/2026 · 23:12 — Le notifiche: "/notification", le date dei database, timer e pomodoro
*(Claude Code sul PC; verificato sul telefono lo stesso giorno,
tranne la riaccensione vera, vedi in fondo; 114 test passati; commit solo in
locale)*

Richiesta dell'utente, parola per parola:
> «Implementa una nuova e grossa feature: dammi la possibilità tramite il
> comando /notification di creare un qualcosa che mi dia una notifica se
> io la imposto per esempio tramite un comando (magari scrivo
> /notification 22.10.2026 17:41), stessa cosa per le date delle pagine
> su un database (che deve essere precisamente una notifica impostata, non
> una semplice data che scrivo su un database, magari quando inserisco una
> data, sotto a Include time mettimi un'opzione chiamata Include
> notification, che mi manda una notifica sia se è una singola data
> precisa con giorno mese anno ora e minuti e sia se è un evento con
> inizio a fine, quindi se è una pagina che ha come data dal 23 ottobre
> 2026 fino al 17 dicembre 2026, in entrambi i giorni riceverò una
> notifica). Se non inserisco ora e minuti, ma decido di farmi mandare una
> notifica, ora e minuti di default saranno sempre alle 9:00 di mattina.
> Quello che verrà visualizzato sulla notifica sarà: 1) o il titolo della
> pagina appartenente alla data e ora della notifica che io ho importato;
> 2) oppure quello che scrivo su un qualsiasi campo di testo scritto da
> parentesi. Un esempio del primo caso: … database con i nomi degli album
> … Revive + delle IVE … La notifica mi verrà fuori alle 9:00 (se non
> metto io un'ora specifica) del 23 febbraio 2026 con scritto Revive +. Un
> esempio del secondo caso: /notification 22.10.2026 19:41 Buttare
> l'immondizia. La notifica mi farà vedere Buttare l'immondizia. Una mini
> feature da implementare è anche una notifica quando finisce un timer, e
> quando finisce la sessione di studio e di break del pomodoro.»

- **[Nuova funzionalità]** **La riga "/notification".** Si scrive
  `/notification 22.10.2026 19:41 Buttare l'immondizia` e si preme Invio:
  il comando sparisce e resta una riga con la **campanella**, il testo
  "Buttare l'immondizia" e in fondo, in un riquadro blu, **quando suona**;
  la scrittura continua nella riga nuova sotto. Toccando il riquadro si
  sceglie il giorno e poi l'ora, con le finestre di Samsung. La data si
  può scrivere anche `22/10/2026`, `22-10-26`, `2026-10-22` o senza
  l'anno (`22.10`: quest'anno, o il prossimo se è già passata); l'ora
  anche `19.41`. **Senza ora, alle 9:00**; con la sola ora, oggi (o domani
  se è già passata); con niente, le prossime 9:00. Il testo fra parentesi
  perde le parentesi. Vale anche `/notifica`. Senza testo, la notifica
  mostra il **nome della pagina** (la riga vuota lo suggerisce in grigio).
  C'è anche la voce **"Notification"** nel menu "/" e in "Turn into":
  lascia il testo com'è, mette le prossime 9:00 e apre subito la scelta
  del giorno. Backspace a inizio riga la fa tornare testo, come una
  casella da spuntare. Una notifica già passata si fa grigia
- **[Nuova funzionalità]** **"Include notification"** nella finestra della
  data dei database, sotto "Include time" (e sotto le sue ore): la data
  manda una notifica col **nome della pagina della riga** ("Revive +") e
  sotto il nome del database. All'ora della data, o **alle 9:00** se l'ora
  non c'è; con una data di fine, **il primo giorno e l'ultimo** (sotto:
  "Album · Inizio", "Album · Fine"); una data che si ripete suona a ogni
  ripetizione. Sotto l'interruttore una riga dice quando suonerà. Nella
  cella, accanto alla data, una campanellina
- **[Nuova funzionalità]** **La fine di un timer e di ogni fase del
  pomodoro** mandano una notifica ("Tempo scaduto", sotto "Timer · 10 m";
  alla fine della sessione il suo nome e "È ora della pausa · 5 min"; alla
  fine della pausa "Pausa finita" e di nuovo la sessione),
  e adesso suonano **anche ad app chiusa**
- **[Nuova funzionalità]** Toccando una notifica si apre **la sua
  pagina**: quella della riga "/notification", o quella della riga del
  database (se la riga non può avere una pagina, il database)
- **[Nuova funzionalità]** Il **permesso delle notifiche** (Android 13 e
  dopo) si chiede **quando serve**, non all'avvio: creando una notifica,
  accendendo "Include notification", facendo partire un timer o un
  pomodoro. Nelle impostazioni, al posto di "Le notifiche non ci sono
  ancora", una riga dice da dove arrivano. **"Turn off notifications" e
  "Turn off for a while" valgono per tutte**; il suono è quello scelto in
  "Notification sound" (o quello del pomodoro), una volta sola
- **[Progetto]** Come funziona (`data/notify/`): **una sola sveglia di
  Android** (`NoteAlarms`, `AlarmManager` esatta, che suona anche col
  telefono in standby) messa alla prima cosa che deve suonare, fra
  promemoria e fine di timer e pomodori. Quando suona, `AlarmReceiver` —
  anche ad app chiusa, Android fa partire l'app solo per questo — manda
  quello che è arrivato **dall'ultima volta a adesso** (`deliveredUpTo`),
  così niente arriva due volte, e una notifica persa a telefono spento
  arriva alla riaccensione se non sono passate più di 12 ore; poi rimette
  la sveglia alla cosa dopo. Dopo un riavvio, un aggiornamento o un cambio
  d'ora la rimette `RescheduleReceiver`. Una sveglia messa per una cosa
  poi cancellata non fa danni: quando suona si rilegge il database. I
  canali "Promemoria" e "Timer e pomodoro" non hanno suono proprio: lo fa
  l'app (`WidgetStore.playOnce`), così il suono scelto nelle impostazioni
  vale subito. Il calcolo di cosa suona e quando (`Reminders.kt`) e il
  comando (`NotificationCommand.kt`) sono senza Android, con 18 test
  (`NotificationsTest`, sugli esempi della richiesta)
- **[Progetto]** Permessi nuovi nel manifest: `POST_NOTIFICATIONS`,
  `USE_EXACT_ALARM` (Android 13 e dopo; `SCHEDULE_EXACT_ALARM` fino al
  12), `RECEIVE_BOOT_COMPLETED`. Ancora nessun permesso di rete
- **[Progetto]** **Database alla versione 32**: `blocks.notifyAt`, l'ora
  delle righe "/notification", aggiunta con `ALTER TABLE` (colonna vuota
  per tutte le righe di prima). Test `MigrationTest.from31…` su un file
  alla 31 (`schema/v31.sql`). Il segno di "Include notification" sta
  invece nel valore della cella data, in fondo (`…|n`): i valori scritti
  prima restano validi
- Verificato sul telefono: `/notification 19:36 Prova notifica` + Invio →
  riga con campanella e "25.09.2026 19:36", e alla prima notifica Android
  ha chiesto il permesso. **Ad app chiusa** (processo ucciso):
  `/notification 19:41 Test app chiusa` → alle 19:41 Android ha fatto
  ripartire l'app e la notifica "Test app chiusa", sotto "Titolo tre H3",
  è arrivata. L'utente ha provato "Include notification" su "Riga
  semplice" alle 19:35, ad app chiusa: "funziona perfettamente"

Poi l'utente, parola per parola:
> «Se l'app è chiusa, non mi manda la notifica se la creo su un campo
> testo. Se la creo su un databse, funziona perfettamente»

- **[Bug fix]** Non era l'app chiusa: nel database del telefono e nelle
  sveglie di Android si vedeva che la sua riga era per **domani**. Aveva
  scritto `/notification` senza ora alle 19:32, quindi le prossime 9:00,
  cioè domani; poi, toccando il riquadro, aveva cambiato solo l'ora
  (19:34), e il giorno era rimasto il 26. Il riquadro diceva
  "26.09.2026 19:34" e non se n'era accorto. Ora **oggi e domani si
  scrivono per nome** ("Oggi 19:41", "Domani 09:00"), e se l'ora scelta è
  già passata un messaggio dice che la notifica non suonerà. La prova ad
  app chiusa qui sopra è stata fatta proprio per escludere l'altra causa

Poi l'utente, parola per parola:
> «Ho riattivato il debug wireless, prova tu»

e, durante le prove:
> «Mettila alle 22:57, per non perdere tempo»

(erano già le 22:56:36: messa alle 22:58)

- Verificato sul telefono, ad app chiusa (processo ucciso) in tutti i
  casi: **riavvio simulato** — app fermata del tutto, che toglie le sue
  sveglie come un riavvio, poi `RescheduleReceiver` chiamato a mano: la
  sveglia delle 22:59 è tornata, e la notifica è arrivata; **tocco sulla
  notifica** "Tocca per aprire" delle 22:58 → l'app si apre direttamente su
  "Titolo tre H3" (non su My Space, la pagina d'avvio); **pomodoro** da
  1 minuto → alle 23:01 "Session · Time for a break · 5 min"; **timer** di
  14 secondi aggiunto col "+" → "Time's up · Timer · 14 s". Poi tutto
  rimesso com'era: pomodoro fermo su 01:00, timer di prova eliminato col
  cestino, il primo timer riportato alle sue rotelle (00:21:02), il secondo
  mai toccato (00:14:00). Il riavvio vero del telefono non è stato provato:
  spegnerebbe il debug wireless
- **[Bug fix]** (trovato provando) **La riga-notifica restava blu dopo aver
  suonato**, finché la pagina non si ridisegnava per altro: l'ora si
  guardava solo al disegno. Ora campanella e riquadro si fanno grigi da
  soli nel momento in cui la notifica suona (provato: "Diventa grigia"
  alle 23:12, a pagina aperta senza toccare niente)
- **[Bug fix]** (trovato provando, c'era già prima) **La barra laterale
  senza pagine.** Se Android chiudeva l'app con la barra aperta, alla
  riapertura la barra tornava aperta da sola, ma senza passare dal
  pulsante che legge l'albero: sotto "Pages" non c'era niente, finché non
  la si chiudeva e riapriva. Ora l'albero si legge anche in quel caso
  (provato: barra aperta, app uccisa, riaperta → My Space, Titolo tre H3 e
  i due database al loro posto)
- Notato: le prove hanno lasciato in "Titolo tre H3" quattro righe-notifica
  già suonate ("Test app chiusa", due "Tocca per aprire", "Diventa
  grigia"); lasciate lì, sono sulla pagina di prova dell'utente

### 🆕 v0.86 — 25/09/2026 · 18:46 — I selettori di data e ora come quelli di Samsung
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 95 test
passati; commit solo in locale)*

Richiesta dell'utente, parola per parola, con tre foto dei Promemoria di
Samsung (il mese di settembre 2026 col 25 cerchiato d'arancio; le rotelle
SEP · 25 · 2026; le rotelle 19 : 00 di "Set time"):
> «Nei database, quando creo la proprietà Date, si crea una cella con un -
> . Quando lo tocco si apre il menu del calendario, ma non mi piace tanto
> come sono il selettore della data e quello dell'ora. Fallo come nelle
> foto che ti mando: per selezionare la data, si vede il mese corrente,
> con le due freccette che vedi in alto a destra e in alto a sinistra per
> cambiare mese, mettimi anche lo scorrimento orizzontale), se premo il
> mese e l'anno corrente mi si apre un qualcosa simile alla seconda foto,
> con giorno, mese e anno a scorrimento verticale. Per quanto riguarda
> l'ora, si apre la finestra simile alla terza foto con ore e minuti a
> scorrimento verticale. Non far caso che nella foto vedi 55, 00, 05. Fai
> gli intervalli ad ogni secondo»

- **[Nuova funzionalità]** **Il mese come quello di Samsung**
  (`SamsungCalendar` in `SamsungPickers.kt`), al posto del calendario di
  Material: in alto "Settembre 2026 ▾" con le frecce ‹ › ai lati, e il
  mese si cambia anche **trascinando in orizzontale**. Sei settimane a
  griglia, le domeniche in rosso, i giorni degli altri mesi spenti (ma si
  possono toccare), oggi in arancio, il giorno scelto in un **cerchio
  arancio**. Toccando il mese si passa alle **rotelle** di mese, giorno e
  anno (▴), nell'ordine in cui si scrive la data dove si è (in Italia
  giorno, mese, anno); il giorno si adatta al mese (dal 31 a febbraio si va
  al 28); ritoccando il mese si torna alla griglia, sul mese scelto. Anni
  dal 1900 al 2100
- **[Nuova funzionalità]** **"Set time" a rotelle** (`SamsungTimeDialog`):
  ore e minuti, sulle ventiquattro ore, **un minuto alla volta** — la
  richiesta "intervalli ad ogni secondo" letta come "ogni singolo valore"
  (la foto andava di cinque in cinque, e l'utente ha chiesto ore e minuti,
  non secondi). In fondo **Cancel | Done**, come nella foto
- **[Nuova funzionalità]** **"End date"**: con il calendario nuovo toccare
  un altro giorno **sposta** la data (prima, col calendario di Material, un
  secondo tocco faceva un periodo). Il periodo si fa con l'interruttore
  "End date", come su Notion: acceso, il primo tocco è l'inizio e il
  secondo la fine, con una fascia arancio chiaro fra i due
- **[Nuova funzionalità]** Lo stesso calendario, in una finestra "Set date"
  con Cancel | Done (`SamsungDateDialog`), anche dove si sceglie un giorno
  solo: la fine di una ripetizione, il filtro per data, le date proprie del
  widget Life progress
- **[Progetto]** Le rotelle sono **una sola** (`WheelPicker`), la stessa del
  timer della barra laterale (lì color pesca, qui in bianco); fanno anche le
  rotelle che si fermano ai capi (gli anni). Tolti il calendario e l'ora di
  Material (`DateRangePicker`, `DatePickerDialog`, `TimeInput`) e le loro
  conversioni dei millisecondi in UTC, che non servono più
- **[Bug fix]** (trovato provando) La finestra di "Set time" finiva sotto la
  barra di navigazione, con Cancel e Done tagliati: dentro la finestra c'era
  un riquadro a tutto schermo con la scheda in fondo, e la finestra non sa
  dov'è la barra. Ora è la finestra stessa a stare in fondo, alta quanto la
  scheda
- Verificato sul telefono, sulla cella Date di "Riga semplice": il mese di
  settembre come nella foto, tocco sul 25 → cerchio arancio, trascinamento
  → ottobre, tocco sul mese → rotelle SEP · 25 · 2026, rotella del mese su
  OCT → ritorno alla griglia su ottobre col 25 scelto; Include time →
  Start time → "Set time" 09 : 00, minuti girati di tre → 09:03 nella riga.
  Poi Cancel: nella cella non è stato salvato niente
- Notato: in cima a "My Space" ci sono sei righe vuote prima di "Titolo
  uno H1", nate dalle prove dell'utente delle 17:45; lasciate com'erano

### 🆕 v0.85 — 25/09/2026 · 18:18 — Il cestino per i timer aggiunti
*(Claude Code sul PC;
verificato sul telefono lo stesso giorno; 95 test passati; commit solo in
locale)*

Richiesta dell'utente, parola per parola:
> «Quando metto un nuovo timer con il +, sia quando finisce sia quando lo
> resetta rimane lì fermo. Metti un'opzione che mi consente di rimuovere
> ogni timer aggiuntivo rispetto al primo. Fai sì che il sistema riconosca
> quando c'è un secondo timer attivato, e dal secondo timer in poi dammi
> la possibilità di eliminare i successivi al primo»

- **[Nuova funzionalità]** **Il cestino dei timer aggiunti.** Nel widget
  Timer, dalla seconda pagina in poi, in alto a destra c'è un **cestino**
  che elimina quel timer, in qualunque stato: sulle rotelle, in corsa, in
  pausa, finito. Sulla prima pagina non c'è: **il primo timer non si
  elimina mai**, e la regola sta anche nel salvataggio
  (`TimerWidget.withoutTimer`), non solo nella schermata. Se il timer
  eliminato correva, la sua sveglia se ne va con lui. Test nuovo
  `WidgetsTest.onlyTheTimersAddedAfterTheFirstCanBeDeleted`
- **[Rimosso]** Il **"Cancel"** sulle rotelle, che c'era con più di un
  timer: eliminava il timer, ma non si capiva — e valeva anche per il
  primo. Ora sulle rotelle c'è solo Start, e per eliminare il cestino
- Verificato sul telefono, coi due timer dell'utente (10:02 e 14:00, sulle
  rotelle): sulla prima pagina solo Start, sulla seconda il cestino; fatto
  partire il secondo, aggiunto un terzo col "+", fatto partire (9:58, con
  cestino, ↻ e +) ed eliminato col cestino → due timer; il secondo
  riportato alle rotelle con ↻ — i due timer dell'utente sono com'erano

### 🆕 v0.84 — 25/09/2026 · 18:04 — Quattro bug, il link delle immagini tolto, la pagina vuota
*(Claude Code sul PC; tutto verificato sul telefono lo stesso giorno; 94
test passati; commit solo in locale, **non** su GitHub finché l'utente non
lo dice)*

Richiesta dell'utente, parola per parola:
> «BUGS:
> 1) Se metto un'icona su una pagina tramite il link, incollo il link, e
> poi però non posso fare più nulla. Non posso scaricare ed usare
> quell'icona dal link, nulla. Di solito, c'è un pulsante tipo upload, che
> consente di scaricare temporaneamente oppure direttamente sul telefono
> per poi incollarla sullo spazio dell'icona dedicata alla pagina
> 2) Se io ho più di due blocchi vuoti, partendo dall'alto, mi capita che
> se io mi trovo al  secondo blocco dall'alto, e cancello, mi va al primo
> blocco (come di consueto) ma non sono sicuro perché ma si bugga facendo
> scomparire la tastiera
> 3) Nella selezione del colore esadecimale delle barre del widget Life
> progress bar, non escono le opzioni classiche del telefono quando
> seleziono del testo e tengo premuto, quindi copia, incolla, select all e
> il resto. Questo problema c'è anche in tutte le selezioni del colore in
> generale? Fixalo per favore
> 4) Vedi sotto il database aperto in Gallery? C'è un toggle. Quello che
> c'è dentro è storto (come la freccetta del toggle) e dentro le Numbered
> list non vengono attivate»

Poi, dopo un'interruzione di corrente: «Sì, continua pure e sistema anche
quel bug» (quello della pagina vuota, trovato da Claude durante le prove);
e, visto il pulsante "Download": «Non voglio appesantirti troppo il
lavoro, quindi togliamo la funziona link, useremo solo Upload
direttamente dal telefono. Ma vorrei chiederti una cosa: è normale che se
scarico un'icona .png 512x512 da Flaticon e la uploado, il .png si
"annulla" e vedo lo sfondo bianco? È normale o puoi fare qualcosa in modo
che lo sfondo del .png sia trasparente (proprio per rispettare quello che
i .png fanno) ?»

- **[Rimosso]** **Il link delle immagini** (icona e copertina). Prima c'era
  una pastiglia "Link" accanto a "Upload", spenta finché il campo era
  vuoto, che scaricava l'immagine: sembrava un'etichetta. Per qualche ora
  è diventata un pulsante "Download" accanto al campo, col tasto Invio e i
  motivi dell'errore spiegati (un sito che rifiuta, un link che non è
  un'immagine, niente rete); provandolo sul telefono il download è
  risultato lentissimo (oltre 20 secondi anche per una pagina minuscola,
  0,1 s dal PC: probabilmente la rete del telefono), e l'utente ha chiesto
  di togliere il link. Tolto tutto: il campo, il pulsante, lo scaricamento
  (`PageImageStore.saveFromUrl`) e le sue scritte. La finestrella ha solo
  **Upload** (e Reposition / Remove quando c'è un'immagine)
- **[Rimosso]** **Il permesso di andare in rete** (`INTERNET` nel
  manifest): serviva solo al link. I font cinesi e giapponesi li scarica
  Google Play Services col permesso suo. Da ora l'app non si collega a
  niente
- **[Bug fix]** Scegliendo dalla galleria un file che non è un'immagine,
  la copia fallita restava nella cartella dell'app: si cancellava un file
  che aveva per nome il messaggio d'errore (`PageImageStore.saveFromGallery`)
- **Il PNG con lo sfondo bianco non era un bug dell'app.** Il file scelto
  era `Pictures/Telegram/IMG_20260925_143037_979.jpg`, un **JPEG**
  512×512 di 10.716 byte — esattamente quello copiato dall'app: Telegram,
  mandandolo come foto, l'aveva compresso in JPEG, che la trasparenza non
  ce l'ha. Mandato come file è rimasto trasparente («Ok, funziona»). Vedi
  "Limiti noti"
- **[Bug fix]** **Backspace fra righe vuote: la tastiera spariva** (bug 2).
  Con tante righe vuote, il backspace a inizio della seconda toglieva la
  **prima** riga invece della seconda: per il testo è uguale, ma il campo
  condiviso è legato al primo blocco del suo gruppo (la chiave nella
  lista), e tolto quello rinasceva da capo senza cursore. Il confronto fra
  prima e dopo, con righe tutte uguali, attribuiva la riga sparita alla
  prima; ora, quando le righe calano, la riga del cursore è sempre fra
  quelle riscritte, e si toglie la successiva (`updateRun`). Test nuovi in
  `EmptyLinesBackspaceTest`. Verificato: quattro righe vuote, backspace
  dalla seconda → tastiera aperta, "x" scritta sulla prima
- **[Bug fix]** **Copia / Incolla / Select all nelle finestrelle che salgono
  dal basso** (bug 3). Il `ModalBottomSheet` di Material 1.2 si apre in una
  finestra aggiunta a mano, che non sa mostrare la barrettina del telefono:
  la parola si selezionava e non compariva niente. Non solo nel colore
  delle barre del Life progress bar: in **tutte** le finestrelle — il
  selettore di colore, il link dell'immagine, la ricerca dei fusi, le
  proprietà del database. Ora tutte passano da `NoteBottomSheet`
  (`SheetTextToolbar.kt`), che dentro usa una barrettina disegnata
  dall'app (`PopupTextToolbar`) con le voci e le scritte di Android.
  Trovata anche una trappola: la prima versione teneva lo stato "aperta"
  osservabile, il campo di testo lo leggeva mentre disegnava le maniglie e
  la richiudeva — aperta e chiusa quattro volte in 30 ms, poi più niente
  (visto nel registro). Verificato nel selettore di colore: Cut / Copy /
  Select all su "#FF0000", Copy → compare Paste, Select all seleziona tutto
- **[Bug fix]** **Il toggle storto** (bug 4). Le righe dentro un toggle
  hanno ciascuna un campo suo, e Compose toglieva lo spazio sopra la prima
  riga: il testo stava ~3dp più su della freccetta e dei numeri, che si
  scrivono a parte (i numeri avevano in più 2dp di margine sopra). Ora la
  riga è fatta come quella del testo condiviso (24, testo in mezzo), senza
  i 2dp, e le righe dentro un toggle stanno al passo di quelle fuori (niente
  4dp sopra e sotto). Il quadratino delle caselle non va più alzato a mano
  (via `CHECKBOX_BASELINE_LIFT`). Verificato: freccetta centrata sul
  titolo, "1." sulla riga di "A", stesso passo dell'elenco fuori
- **[Bug fix]** **"1." dentro un toggle non faceva l'elenco numerato**
  (bug 4): la regola c'era solo nel testo condiviso. Ora anche nelle righe
  dentro un toggle (`startNumberedListInBlock`). Verificato: Invio sulla
  riga vuota, "1." → elenco da 1, "Nuovo" scritto dentro; poi annullato
- **[Bug fix]** **La pagina vuota: il primo Invio perdeva il cursore**
  (trovato da Claude provando il bug 2, sistemato su richiesta). Una
  pagina senza righe ne mostra una vuota che nel database non c'è; il
  primo Invio salvava solo la riga nuova, quella vuota spariva e con lei
  il campo: tastiera aperta, barra sparita, e quello che si scriveva non
  andava da nessuna parte. Per trovarlo è servito far stampare al
  database ogni sua scrittura: al tocco sulla pagina vuota non ne partiva
  nessuna. Ora la riga vuota ha lo stesso id finché si resta sulla pagina
  (prima cambiava a ogni lettura del database) e viene salvata **insieme**
  alla prima modifica, nella stessa transazione (`placeholder`,
  `unsavedPlaceholderIn`). Test nuovi in `EmptyPageTest`. Verificato:
  pagina "Untitled" svuotata, tocco sotto il titolo, Invio, "Riga" → barra
  al suo posto, "Riga" sulla seconda riga, nel database le due righe con la
  prima salvata; poi annullato
- Notato: il toggle "Riga dentro il toggle" di "My Space" alle 17:45 è
  stato modificato dall'utente (righe "A" rifatte, toggle chiuso); lasciato
  com'è

### 🆕 v0.83 — 25/09/2026 · 13:31 — Divisore sotto le pagine, menu dei font non più coperto
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 89 test
passati; commit solo in locale)*

Richiesta dell'utente, parola per parola (dopo aver letto del menu
coperto, nella voce qui sotto):
> «Sì, sistemalo per favore. Ed un'altra cosa: sotto Pages, ossia la
> sezione con la visualizzazione gerarchica del Workspace nella sidebar,
> mettimici un divider, così non si confonde con Search»

- **[Nuova funzionalità]** **Un divisore sotto l'albero delle pagine**
  nella barra laterale, uguale a quello sotto "Main menu": l'ultima
  pagina dell'albero e "Search" non sembrano più la stessa lista
  (`Sidebar.kt`). Verificato sul telefono: il divisore sta fra l'ultimo
  "Untitled" e "Search"
- **[Bug fix]** **Il menu dei font era coperto dalla barrettina Copy /
  Cut / Select all** del telefono, quando c'era del testo selezionato:
  copriva Garamond e Arial. Il menu non prende il fuoco apposta (così la
  tastiera e la barra Aa restano), quindi il campo restava a fuoco con la
  selezione e il telefono lasciava la barrettina sopra il menu. Ora
  aprendo il menu la barrettina si chiude (`LocalTextToolbar.hide()` in
  `FontPickerChip`); la selezione resta, e la barrettina torna dopo la
  scelta. Verificato: selezionato "sotto", aperto il menu — tutto
  visibile, Garamond compreso — scelto Garamond → solo "sotto" in
  Garamond, ancora selezionato; poi annullato con ↶
- Notato durante la prova: nella pagina "My Space" la parola "Paragrafo"
  è in **Futura**. Non l'ha messa Claude: alle 13:21 era nel font
  normale, alle 13:27, appena reinstallata l'app e prima di toccare la
  pagina, era già in Futura. **Confermato dall'utente**: «Sì, l'ho
  provato io, tranquillo». Lasciata com'è

### 🆕 v0.82 — 25/09/2026 · 13:23 — Font e dimensione sul testo selezionato
*(Claude Code
sul PC; verificato sul telefono lo stesso giorno; schema alla versione
**31**; 89 test passati; commit solo in locale, **non** su GitHub finché
l'utente non lo dice)*

Richiesta dell'utente, parola per parola:
> «In Settings, ti ho già detto in passato di mettermi Font e Font size.
> Toglili: non mi va di farti scervellare solo per farti mettere font e
> size diversi tra tutto il Workspace. Però ho notato una cosa:
> selezionando una parte di un testo e cambiando font e size mi modifica
> tutto quanto quello che c'è in una pagina. La mia idea era quella di
> selezionare del testo e modificare il font di quel testo. Poi
> selezionarne altro e modificare quel testo con un altro font. Puoi
> farlo?»

- **[Rimosso]** **"Font" e "Font size" nelle impostazioni.** Erano due
  voci grigie, messe lì per un futuro font di tutto il Workspace: tolte,
  con le loro scritte (`Strings.font`, `Strings.fontSize`)
- **[Nuova funzionalità]** **Font e dimensione valgono solo per il testo
  selezionato.** Prima le due voci della barra Aa cambiavano **tutta la
  pagina**, qualunque cosa fosse selezionata (non era un bug: era fatta
  così il 24/09, "per ogni pagina"). Ora si comportano come il colore:
  si seleziona un pezzo di testo, si sceglie il font o si scrive la
  dimensione, e cambia **solo quel pezzo**; si seleziona un altro pezzo
  e gli si dà un altro font. Le due voci mostrano il font e la
  dimensione del testo selezionato (o di quello subito prima del
  cursore). Toccandole senza niente di selezionato compare **"Select some
  text first"**. Il font e la dimensione stanno nel testo stesso, come
  grassetto e colore (`RichTextSpan.font`, `RichTextSpan.size`;
  `setFontInRange`, `setSizeInRange`, `spanAt`;
  `PageEditorViewModel.applyFontToCapturedSelection`,
  `applySizeToCapturedSelection`, `selectionTypography`; a schermo
  `fontStyleOf`), e si annullano con ↶ come ogni altra modifica. Il 16
  di partenza non si scrive nel testo. Il testo scritto subito dopo un
  pezzo in un altro font ne prende il font, come il grassetto. Non
  c'è più niente "di pagina": via `PageTypography`, `LocalPageTypography`,
  `setPageFont`, `setPageFontSize`. Le celle delle tabelle semplici e il
  titolo della pagina, che prendevano il font della pagina, tornano al
  font di sistema (sono testo senza formattazione)
- **[Nuova funzionalità]** **Le righe si allungano per il testo grande.**
  Con una dimensione più grande di 16 in un gruppo di righe, **tutte**
  le righe di quel gruppo diventano più alte, quanto serve al pezzo più
  grande; perché non solo la sua, vedi "Limiti noti"
- **[Progetto]** Migrazione **30→31** (`MIGRATION_30_31`): le pagine che
  avevano un font o una dimensione "di pagina" li danno a ogni pezzo di
  testo dei loro blocchi che non ne ha già uno suo — così si vedono come
  prima — e poi `pageFont` e `pageFontSize` si svuotano in tutte le
  pagine. Le due colonne restano (toglierle vorrebbe dire ricostruire la
  tabella) ma non le scrive più nessuno. `MigrationTest` prova anche
  dalla 30 (schema in `test/resources/schema/v30.sql`, generato dal
  codice di Room al commit `a68a44c`, con una pagina in Garamond a 20 e i
  suoi blocchi); test nuovi in `RichTextFontTest`. Verificato sul
  telefono: backup del database, `user_version` 31, nessun crash
- Verificato sul telefono, nella pagina "My Space": selezionato "prova"
  → Consolas, **solo quella parola**; selezionato "titolo" → le voci
  dicono "Default" e "16", scelto Georgia e poi 28 → solo "titolo",
  grande, e le righe del gruppo più larghe; nel database
  `{"text":"prova","font":"CONSOLAS"}` e
  `{"text":"titolo","font":"GEORGIA","size":28}`; ↶ riporta "titolo" a 16
  e le righe al passo di prima; col solo cursore "Select some text
  first"; le impostazioni senza Font e Font size. "prova" in Consolas e
  "titolo" in Georgia sono rimasti nella pagina, per farli vedere
- Notato durante la prova (c'era già): con una parola selezionata, il
  menu dei font si apriva **sotto** la barrettina di sistema Copy / Cut /
  Select all, che copriva Garamond e Arial. **Corretto subito dopo**, vedi
  la voce qui sopra

### 🆕 v0.81 — 25/09/2026 · 12:59 — Timer: via i tempi pronti da 10, 15 e 30 minuti
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

L'utente, parola per parola:
> «Mi sono dimenticato di dirti che i tre timer di preset (ossia 10
> minuti, 15 minuti e 30 minuti) di Samsung non li volevo. Toglimeli per
> favore.»

- **[Rimosso]** I tempi pronti 00:10:00, 00:15:00 e 00:30:00 sotto le
  rotelle del Timer, quelli della seconda foto (v0.80): resta solo
  **Start**

### 🆕 v0.80 — 25/09/2026 · 12:56 — Cronometro e Timer fra i widget, come quelli dell'orologio Samsung
*(Claude Code sul PC; verificato sul telefono lo stesso giorno; 80 test
passati)*

- **[Nuova funzionalità]** **Cronometro e Timer fra i widget.** Richiesta
  dell'utente, parola per parola, con quattro foto dell'orologio Samsung
  (il cronometro fermo a 00:32.52 con tre giri e in corsa a 00:10.84; le
  rotelle del timer a 00:10:00 coi tempi pronti; un timer a 9:55 con
  sopra "Add timer"): «Mettimi tra i widget Lo Stopwatch (prima e quarta
  foto) e il Timer (seconda e terza foto). Saranno uguali a quelli di
  Samsung (ovviamente i riquadri dei widget saranno piccoli e
  rispetteranno la grande della sidebar). Nel caso dello Stopwatch, ci
  saranno all'inizio due pulsanti, Lap e Start, faccio Start e parte lo
  Stopwatch, il tempo scorre, Start sarà diventato Stop interrompendo
  temporaneamente il tempo, che poi diventerà Resume per farlo continuare
  a dov'era prima ed aggiungerà al posto di Lap Reset per resettare lo
  Stopwatch; Lap servirà per vedere i giri (come nei circuiti), e sotto
  apparirà una lista di giri come vedi nelle foto, con Lap, Lap times e
  Overall times. Nel caso del Timer, il widget mi consentirà di scorrere
  verticalmente da 0 a 99 ore, da 0 a 59 minuti e da 0 a 59 secondi,
  toccherò Start, e in alto a destra avrò un'iconcina che mi consente di
  resettare il timer e un'altra iconcina che mi consente di impostazione
  contemporaneamente un altro timer (i vari timer saranno visualizzabili
  tramite scorrimento orizzontale)». E: «Per il momento no, non mandarlo
  su Github, te lo dirò io quando sarà il momento».
  - **Stopwatch** (`StopwatchWidget`, `StopwatchView` in
    `SidebarTimers.kt`): il quadrante a sessanta tacche coi centesimi al
    centro in color pesca, la lancetta **blu** per i secondi del tempo
    totale e, dopo il primo giro, quella **bianca** per il giro in corso
    (è così nelle foto: a 00:32.52 la bianca segna i 24,76 s del quarto
    giro). Pulsanti come chiesto: **Lap** (spento) e **Start**; in corsa
    **Lap** e **Stop** (rosso scuro); fermo **Reset** e **Resume**. Sotto,
    la lista **Lap / Lap times / Overall time**, dal giro più recente, col
    **più veloce in arancio e il più lento in rosso** come nella foto (da
    tre giri in su: con due sarebbero colorati tutti e due — scelta mia,
    da confermare). Si conta sull'orologio come il pomodoro: il tempo va
    avanti anche a barra o app chiusa
  - **Timer** (`TimerWidget`, `CountdownTimer`, `TimerView`): tre
    **rotelle** che girano senza fine come su Samsung (ore 0-99, minuti e
    secondi 0-59), ognuna si ferma su un numero intero e il tempo si salva
    subito; **Start**. *(I tempi pronti 00:10:00, 00:15:00 e 00:30:00
    della seconda foto c'erano in questa versione e sono stati tolti
    subito dopo, su richiesta dell'utente: vedi v0.81)*
    Partito: il quadrante con le **tacche arancio per il tempo che resta**,
    dentro il tempo impostato ("10 m"), il conto alla rovescia ("9:55") e
    la **campanella con l'ora in cui finisce**; sotto **Pause** / **Resume**.
    In alto a destra **↻** (torna alle rotelle) e **+** (un altro timer, che
    corre insieme agli altri): la pagina nuova si apre da sola con
    **Cancel / Start**, come l'"Add timer" della terza foto, e i timer si
    **sfogliano in orizzontale** coi pallini sotto. Finito, **suona** (il
    suono delle notifiche dell'app, una volta sola, anche a barra chiusa)
    e mostra **0:00 in rosso** con **Reset**
  - Verificato sul telefono: cronometro Start → tre Lap → Stop (00:10.28,
    giri 01 bianco, 02 rosso, 03 arancio), Resume da 10.28 a 11.83, Reset;
    timer con le rotelle portate a 00:07:02 e poi a 00:00:05 trascinando,
    Start → 0:03 con le tacche e la fine alle 08:57, a zero il suono
    (08:57:05-08:57:08) e 0:00 rosso; "+" → secondo timer con Cancel/Start
    e i pallini, partito, **lo scorrimento orizzontale sfoglia i timer
    senza chiudere la barra**, ↻ e Cancel lo tolgono. Test nuovi in
    `WidgetsTest` (cronometro, giri e colori coi tempi delle foto, stati
    del timer, rotelle, testi): 80 test passati

### 🆕 v0.79 — 25/09/2026 · 08:34 — Fusi orari: l'ora a destra di ogni fuso nella finestra di scelta
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

- **[Nuova funzionalità]** **L'ora nella scelta del fuso di "Add to the
  list".** Richiesta dell'utente, parola per parola: «Nella lista degli
  orologi, non quando faccio "Add clock" ma "Add to the list", aggiungimi
  a destra anche l'ora. Mi serve saperla in aggiunta del fuso orario
  appartenente (ovviamente si deve aggiornare man mano che il tempo reale
  scorre)». Letta così: nell'elenco del widget l'ora c'era già, mancava
  nella finestra di scelta. Ora lì, a destra di ogni fuso (sotto il nome
  resta "Regione · GMT±x"), c'è **l'ora che è in quel posto adesso**, che
  avanza mentre la finestra resta aperta. Subito dopo l'utente: «Me la
  metti anche in Add clock per favore?» — ora c'è in tutte e tre le
  strade che aprono la finestra: "Add to the list", "Add clock" e "Change
  time zone" di un orologio (`ZonePickerSheet`). Verificato: Midway 19:33
  → 19:34 allo scatto del minuto, Marquesas (GMT-9:30) 21:03 → 21:04; in
  "Add clock" 19:36 alle 08:36 italiane

### 🔧 v0.78.1 — 25/09/2026 · 08:29 — Pomodoro: la cifra cambia a ogni secondo da quando si preme ▶
*(Claude Code sul PC; verificato sul telefono lo stesso giorno)*

- **[Bug fix]** **…e partiva con un secondo di ritardo.** Segnalato
  dall'utente dopo la correzione del secondo in più (v0.78): «non si vede il secondo in
  più che ti dicevo, ma comunque il pomodoro ci mette un secondo in più a
  partire rispetto a quando premo il pulsante play». Il widget si
  ridisegnava allo scoccare dei secondi **dell'orologio**, e il timer
  parte a metà di un secondo qualunque: il primo "00:59" arrivava fino a
  quasi due secondi dopo ▶. Ora il pomodoro ha un'ora sua, rinfrescata
  **quando cambia la cifra del timer** (`rememberTimerNow`,
  `msUntilTimerTicks`); gli orologi restano sui secondi dell'orologio.
  Test nuovo `WidgetsTest.theTimerRedrawsExactlyWhenItsDigitChanges`.
  Verificato con screenshot a tempo dal telefono: "01:00" fino a 0,76 s
  dopo il tocco, "00:59" a 1,13 s, "00:58" a 2,16 s, "00:57" a 3,06 s

### 🆕 v0.78 — 25/09/2026 · 08:25 — Le prove dell'utente: "Reposition cover" nelle schede, il suono del pomodoro, SimSun al posto di MS YaHei
*(Claude Code sul PC; tutto verificato sul telefono lo stesso giorno;
schema alla versione **30**)*

Richieste dell'utente, parola per parola (dopo le prove della sera
prima: copertine, rotazione, pomodoro, font offline, voce 14):
> «1. Funziona. Dammi per favore la possibilità di risposizione (come
> faccio nelle pagine in maniera classica) le immagini delle pagine in
> visualizzazione Gallery dei database
> 2. Sì, si vedono le cinque pagine in Gallery
> 3. Sì, si sente il classico Ringtone di quando mi arrivano le
> notifiche. Dammi la possibilità di cambiarlo tramite i ringtone
> standard del telefono (se puoi). Piccolo bug: quando metto un minuto,
> si vede uno scatto che fa vedere che si è aggiunto un secondo
> 4. Sì, sembra funzionare. Ma, altro bug, Ms Yahei non cambia font,
> rimane sempre quello di default
> 5. Fai come Notion: piccole, medie e grandi. Di base medie (su
> telefono, poi su pc vedremo in futuro)»

Alle tre domande fatte subito dopo ha risposto: inquadratura **solo
della scheda** (non la copertina della pagina); suono **solo del
pomodoro**; MS YaHei: «Toglilo. Mi ci puoi mettere SimSum?». E durante la
prova: «La suoneria sta continuando a suonare all'infinito».

- **[Nuova funzionalità]** **"Reposition cover" nelle schede della
  galleria.** Tenendo premuta una scheda che mostra la copertina, nelle
  azioni c'è "Reposition cover": la copertina **dentro la scheda** si
  trascina e si pizzica (fino a 4×, come nella pagina), mai oltre i bordi;
  ✓ salva, ✕ lascia com'era. Mentre si sposta la scheda ha un bordo e non
  si apre toccandola. L'inquadratura è **solo della scheda**: la copertina
  in cima alla pagina resta com'è, e viceversa. Finché non se ne sceglie
  una, la scheda usa quella della pagina come prima; una copertina nuova
  le azzera tutte e due. La voce c'è solo in galleria con "Page cover",
  una copertina da spostare e il contenuto non bloccato
  (`GalleryCard`, `CardCoverMoveControls`, `DatabaseViewModel.setRowCardCover`;
  colonne `cardCoverScale`, `cardCoverOffsetX`, `cardCoverOffsetY` in
  `pages`). Verificato: trascinata la copertina di "Gioco uno", salvata
  nella sola scheda (`cardCoverOffsetY` 0,085, la pagina a 0)
- **[Nuova funzionalità]** **Il suono del pomodoro fra le suonerie del
  telefono.** Nell'ingranaggio c'è "Sound": apre l'elenco di sistema dei
  suoni (con l'anteprima), si sceglie, e con "Fatto" vale **per quel
  pomodoro**; "Use default sound" torna al suono delle notifiche dell'app,
  che resta quello di partenza (`PomodoroWidget.soundUri`, `soundName`).
  Sul S25 Ultra il telefono chiede con quale app sceglierlo (SecSoundPicker
  o Zedge, se installato). Verificato con "Asteroid"
- **[Bug fix]** **Un suono che non finiva più.** Le suonerie del telefono
  (le "Galaxy" di Samsung) sono fatte per ripetersi finché non si
  risponde — lo chiede il file stesso, con l'etichetta `ANDROID_LOOP` — e
  suonate come avviso andavano avanti all'infinito (fermate chiudendo
  l'app). Ora ogni suono dell'app (fine fase del pomodoro, e "Play" nelle
  impostazioni del suono) suona **una volta sola** (`isLooping = false`,
  da Android 9) e comunque **si ferma dopo 8 secondi** (`WidgetStore.playOnce`).
  Verificato: partito alle 08:22:58, fermato e rilasciato alle 08:23:06
- **[Bug fix]** **Il pomodoro appena avviato mostrava un secondo in
  più** ("01:01" per un attimo, con un minuto). L'ora che la barra
  laterale passa al widget si rinfresca allo scoccare di ogni secondo, e
  subito dopo ▶ era indietro di quasi un secondo rispetto a quando il
  timer era partito. Ora il tempo mostrato non supera mai quello di
  partenza (`PomodoroWidget.remainingAt`). Test nuovo
  `WidgetsTest.aJustStartedPomodoroNeverShowsMoreThanItStartedWith`.
  Verificato: subito dopo ▶ "01:00", e un minuto dopo la pausa in verde
- **[Rimosso]** **MS YaHei.** Non era rotto: il suo sosia libero, Noto
  Sans SC, è **lo stesso disegno** che Android usa già per il cinese, e
  sul telefono non si distingueva dal font di sistema. Tolto su richiesta
  dell'utente; le pagine che lo usavano tornano al font di sistema
  (migrazione 29→30)
- **[Nuova funzionalità]** **SimSun** al posto di MS YaHei, nel gruppo
  cinese: un Song dai tratti sottili, reso con **Noto Serif SC nel peso
  Light** — lo stesso font del Songti ma più leggero, che a SimSun somiglia
  e dal Songti si distingue. Si scarica come gli altri cinesi. Verificato
  su "我们都是意大利人": Default, Songti e SimSun sono tre disegni diversi
- **[Progetto]** Migrazione **29→30** (`MIGRATION_29_30`): le pagine con
  `pageFont = 'MS_YAHEI'` tornano a NULL — deve succedere prima di
  qualunque lettura, perché il nome non esiste più nell'enum — e tre
  colonne vuote per l'inquadratura delle schede. `MigrationTest` ora prova
  sia dalla 28 sia dalla 29 (schema in `test/resources/schema/v29.sql`,
  generato dal codice di Room al commit `b53a7bf`, con una pagina in MS
  YaHei e una in Songti); `AppDatabase.resetForTests` perché i due test
  non si pestino i piedi. Verificato sul telefono: `user_version` 30,
  colonne in fondo al `CREATE TABLE`, nessun crash
- **[Progetto]** Voce 14 confermata dall'utente: le schede piccole, medie
  e grandi come su Notion, **medie** di partenza — è già così

### 🔧 v0.77.1 — 24/09/2026 · 23:17 — Ripresa sul PC del 24/09/2026 — il lavoro cloud provato sul telefono
*(Claude Code sul PC, S25 Ultra in debug wireless, build
firmata con la chiave di debug del PC; esito voce per voce in "Diario
delle sessioni cloud" → "Da verificare sul telefono")*
- **[Progetto]** Clonato il ramo `claude/funny-ramanujan-uuwahs` (la
  pull request #1 non era ancora unita) in `C:\Users\Utente\MyNotesApp`.
  Sul PC mancava Git: installato Git for Windows 2.55 con `winget`. Il
  progetto non ha `gradlew`: compilato col Gradle 8.7 già in cache
  (`%USERPROFILE%\.gradle\wrapper\dists\gradle-8.7-bin`) e la JDK 21
  (`%USERPROFILE%\.jdks\jdk-21.0.12.1+1`), con un `local.properties`
  (ignorato da Git) che dice dov'è l'SDK. `assembleDebug` riuscito, solo
  avvisi; `testDebugUnitTest` 70 su 70. **Android Studio non è stato
  aperto**: la sincronizzazione lì resta da vedere
- **[Progetto]** **Le migrazioni 24→29 provate su un telefono vero.**
  notE non era installata (nessun database da salvare), quindi: build di
  `4a35c64` (schema 24) installata con la chiave del PC, note di prova
  scritte a mano (H1, H2, H3, paragrafo, elenco, toggle con una riga
  dentro, database con una riga e del testo nella sua pagina), database
  copiato, poi build nuova installata **sopra** (`adb install -r`, nessuna
  disinstallazione chiesta). Esito: `user_version` 29; le sette colonne
  nuove in fondo al `CREATE TABLE` di `pages`, senza apici inversi (la
  tabella non è stata rifatta); i tre titoli diventati `PARAGRAPH` col
  testo intatto; tutto il resto identico; `integrity_check` e
  `foreign_key_check` puliti; nessun errore in `logcat`
- **[Bug fix]** **Barra Aa: la voce della dimensione era vuota** — solo
  la freccetta, niente "16" (lavoro cloud). `BarValueChip` accorcia il
  testo coi puntini con `weight(1f, fill = false)`, che vuole una
  larghezza massima: la barra scorre in orizzontale, e senza limite
  Compose dava al testo zero pixel. La voce del font l'aveva
  (`FONT_CHIP_MAX_WIDTH`), quella della dimensione no: ora ha
  `SIZE_CHIP_MAX_WIDTH` (64 dp). Verificato: "16", e 32, 72, 5
- **[Bug fix]** **Annulla faceva chiudere l'app** in qualunque pagina con
  un toggle non vuoto che non fosse il primo blocco
  (`SQLiteConstraintException: FOREIGN KEY constraint failed` in
  `replaceAllBlocks`). **C'era già prima del cloud.** La foto di Annulla
  arriva ordinata solo per `orderIndex`, che per i figli riparte da 0: il
  figlio veniva reinserito prima del suo toggle, e la chiave esterna verso
  il genitore lo rifiutava. I dati non si perdevano (tutto in una
  transazione), ma l'app si chiudeva. Ora si reinserisce con
  `inDocumentOrder`, genitori prima dei figli. Test nuovo
  `PageRepositoryTest.undoSnapshotPutsBackAToggleWhoseChildComesFirstInOrderIndex`:
  senza la correzione fallisce con lo stesso errore del telefono (787),
  con la correzione passa. Verificato sul telefono
- **[Bug fix]** **Dopo "Duplicate" dal menu del blocco il testo restava
  indietro, e la lettera successiva cancellava la copia.** Il campo
  unito mostrava le righe di prima (una sola copia) coi segni degli
  elenchi scalati di una riga; scrivendo una lettera, `updateRun`
  confrontava sette righe con otto blocchi e **cancellava il blocco della
  copia** (visto nel database). Causa: in `MergedTextRunField` il ramo
  normale di `onValueChange` impostava `pendingText` anche quando si
  spostava solo il cursore o si selezionava; senza nessun salvataggio in
  viaggio `pendingText` non si azzerava più, e il campo scambiava per
  "database indietro" ogni cambiamento arrivato da fuori. Prima del cloud
  gli unici erano Annulla e Ripristina, che lo azzerano; il menu del
  blocco è nuovo. Ora `pendingText` si imposta solo se il testo è
  cambiato davvero. Verificato: stessa sequenza, due righe subito, segni
  giusti, la lettera va nel blocco giusto e la copia resta
- **[Bug]** *(non corretto, c'era già in `4a35c64`)* **La riga fantasma**:
  trasformando dal menu "/" l'**ultima** riga di un gruppo di testo in
  un'isola (toggle, database, pagina, tabella), nel campo resta una riga
  in più col testo che aveva (vuota se era vuota). `applySlashEntry`
  imposta `pendingText` legittimamente (ha appena tolto "/comando"), poi
  la riga esce dal gruppo; Room consegna solo lo stato finale, diverso da
  `pendingText`, e il campo lo scambia per "database indietro". Non perde
  niente: scrivendoci nasce un blocco nuovo, e riaprendo la pagina
  sparisce. Da correggere con calma: è il punto più delicato dell'app
- **[Bug]** *(non corretto, c'era già)* Creando un database o una pagina
  dal menu "/", il testo del comando ("/table", "/gallery") **resta
  salvato** nel blocco del collegamento: `convertToDatabaseLink(block)`
  salva la copia del blocco presa prima che il comando fosse tolto. Non
  si vede (il collegamento mostra il nome della pagina)
- **[Bug]** *(non corretto, lavoro cloud, solo aspetto)* Sotto
  l'orologio del widget "Fusi orari" si legge "Thu 24 Sep • GMT+02:00"
  invece della sigla "CEST" che la voce prometteva: su questo telefono il
  nome breve del fuso, con la lingua dell'app in inglese, esce come
  scostamento
- **[Progetto]** Come si è provato: `adb` e `uiautomator dump` per
  toccare gli elementi per nome, uno screenshot fra un passo e l'altro, e
  il database riletto dal PC dopo ogni operazione che scrive (`.db` e
  `-wal`, senza `-shm`). Due trappole viste: il **debug wireless si
  spegne da solo** ogni 10-15 minuti (meglio il cavo); e **uno
  scorrimento fatto con la tastiera aperta parte sulla tastiera
  Samsung** e scrive caratteri — un "⁵" finito in un paragrafo di prova,
  tolto a mano. Il tasto Indietro con la tastiera già chiusa esce
  dall'app

### 🆕 v0.77 — 24/09/2026 · 15:52 — Pedice e apice nella barra Aa
*(verificato in parte sul telefono
il 24/09/2026: voce 30)* *(sessione cloud del 24/09/2026,
richiesta 39: compilato, 70 test passati — 7 nuovi —, nessuna
migrazione, **non provato sul telefono**)*
- **[Nuova funzionalità]** Nella barra **Aa**, subito dopo S, due
  pulsanti: **x₂ pedice** (una x col 2 piccolo in basso a destra) e
  **x² apice** (il 2 in alto a destra), come chiesto. Come B, I, U, S
  agiscono sul testo selezionato, anche a cavallo di più righe, e un
  secondo tocco toglie. **Si escludono**: dare l'apice a un pedice lo
  trasforma, come in Word (`withSubscript`, `withSuperscript`)
- **[Nuova funzionalità]** Il testo in pedice o apice è **a sette decimi
  del corpo** della pagina (cresce con lui) e sta sotto o sopra la riga;
  le altre formattazioni — grassetto, colore, spoiler — restano. È solo
  aspetto: le lettere sono dove erano, quindi cursore, selezione,
  ricerca e conteggio del testo non cambiano (`scriptStyleOf`, usato sia
  dal testo unito sia dalle isole)
- **[Progetto]** Nel testo salvato sono due chiavi nuove,
  `subscript` e `superscript`, scritte solo quando sono accese: il testo
  di prima si legge com'era e, senza pedici, si salva identico. Niente
  migrazione (è JSON dentro una colonna che c'era già)
- **[Bug fix]** *(trovati col disegno di prova, prima del telefono)*
  Nei pulsanti, il 2 dell'apice spingeva giù la x, che non stava più
  all'altezza di B, U, S; poi, messo il 2 in un angolo, restava a metà
  in tutti e due i pulsanti perché si portava dietro l'altezza di riga
  del testo normale. E nel testo il pedice, con lo spostamento standard,
  scendeva sotto le gambe di g e p: ora scende meno dell'apice
- **[Progetto]** Test: `RichTextScriptTest` (6: solo sulla selezione, uno
  sostituisce l'altro, secondo tocco toglie, le altre formattazioni
  restano, si continua a scrivere in apice, il testo salvato prima si
  legge e si risalva uguale) e `ScriptRenderTest` (i pulsanti e un testo
  con H₂O e mc², in `app/build/widget-screenshots/scripts.png`). Testi
  nuovi nelle otto lingue (`EditorStrings.subscript`, `superscript`, che
  chi usa il lettore di schermo sente sui due pulsanti)

### 🆕 v0.76 — 24/09/2026 · 15:21 — Widget nella barra laterale — fusi orari, avanzamento, contatore, pomodoro
*(verificato in parte sul telefono il 24/09/2026: voce 29)*
*(sessione cloud del 24/09/2026, richiesta 37: compilato, 63
test passati — 12 nuovi —, nessuna migrazione, **non provato sul
telefono**)*
- **[Nuova funzionalità]** **La sezione "Widget" in cima alla barra
  laterale non è più uno spazio vuoto** (`WIDGETS_SPACE`, tolto): i widget
  si aggiungono da **"+ Aggiungi widget"** in fondo alla sezione, arrivano
  in fondo, ognuno nel suo riquadro arrotondato col nome e i **tre
  puntini** (Sposta su, Sposta giù, Rimuovi con la conferma; sul
  contatore anche Rinomina). Di ognuno se ne possono mettere quanti se ne
  vuole (`SidebarWidgets.kt`, `WidgetsSection`)
- **[Nuova funzionalità]** **L'interruttore**: una freccetta accanto al
  titolo "Widget", su per chiudere e giù per riaprire, **nasconde tutta
  la sezione** e lascia solo il titolo. La scelta **resta** chiudendo la
  barra e l'app, come chiesto
- **[Nuova funzionalità]** **Fusi orari**: **orologi** due per riga, col
  nome della città sopra; l'analogico come nella foto dell'utente —
  tacche, lancette di ore, minuti e secondi, e dentro l'ora scritta
  ("16:05:09") col giorno nella lingua dell'app e la sigla del fuso
  ("gio 24 set • CEST"); il digitale con l'ora grande. **Tenendo premuto
  un orologio**: Analogico / Digitale, Cambia fuso orario, Rimuovi. Il
  primo orologio nasce sul fuso dell'app (Settings)
- **[Nuova funzionalità]** **L'elenco dei fusi**, sotto gli orologi:
  città, ora, sole o luna (giorno dalle 6 alle 18). **Sempre da ovest a
  est**, come chiesto: si ordina a ogni secondo sulla distanza da
  Greenwich **di adesso**, perché l'ora legale sposta i fusi (New York e
  Londra non sono sempre a cinque ore); a parità, per nome
  (`sortedWestToEast`). Tenendo premuta una riga, la si toglie
- **[Nuova funzionalità]** **La scelta del fuso**: tutti i fusi con un
  nome di città (fuori i doppioni vecchi come "US/Pacific" o
  "Etc/GMT+5"), anche loro da ovest a est, con "Regione · GMT+5:30"
  sotto, e la ricerca per città, regione o "GMT+2" (`ZonePickerSheet`)
- **[Nuova funzionalità]** **Avanzamento** (la "Life Progress Bar"):
  **Anno, Mese, Settimana, Giorno** già dentro, e **"Aggiungi una data"**
  per le proprie — nome, Da, A, con il calendario dei database; A prima
  di Da non si accetta. Barra con la cornice, pieno quanto è passato,
  "Anno: 73%" accanto nello stesso colore; **tenendo premuta una barra**
  si cambia **il colore** (la tavolozza del testo, senza il selettore
  testo/sfondo, che qui non vuol dire niente: `ColorPickerSheet` ha ora
  `showTargetSwitch`) e, per le date proprie, Modifica ed Elimina. Le
  barre partono **verdi** come nella foto. La settimana comincia il
  lunedì; una data si conta dalla mezzanotte del primo giorno alla
  mezzanotte dell'ultimo, e da lì è al 100%; tutto nel fuso dell'app
  (`fractionAt`)
- **[Nuova funzionalità]** **Contatore**: − e + quadrati ai lati del
  numero, sotto la freccetta che gira per azzerare, come nella foto. Un
  nome facoltativo sopra al numero (Rinomina, dai tre puntini)
- **[Nuova funzionalità]** **Pomodoro**: il riquadro arrotondato col
  **tempo grande** ("25:00"; toccandolo lo si scrive a mano, minuti e
  secondi), sotto **il nome della sessione** (toccandolo lo si cambia; in
  pausa c'è scritto "Pausa") e tre pulsanti tondi: **▶** (che diventa ⏸),
  **da capo** (fermo, sulla sessione, alla durata piena), e
  **l'ingranaggio** con **Durata della sessione** e **Durata della
  pausa**, − e + da 1 a 180 minuti. Il colore dice la fase: arancio la
  sessione, verde la pausa. Finita una fase **suona** e comincia l'altra
  da sola, come i timer "25 + 5" della foto
- **[Progetto]** Com'è fatto il pomodoro: **si conta sull'orologio, non a
  colpi di secondo**. Quando corre si salva *quando* finirà la fase
  (`endsAt`), e il tempo che manca si ricava da lì; così resta giusto con
  la barra chiusa, con l'app in secondo piano o chiusa e riaperta.
  Un'attesa di `WidgetStore`, che vive quanto l'app e non quanto la
  barra, dorme fino alla fine della fase e allora fa avanzare e suonare.
  Riaprendo l'app dopo ore, le fasi passate si recuperano tutte, contando
  dal momento esatto in cui ognuna è finita, e **senza suonare** per il
  passato (`advancedTo`). Il suono è quello scelto per le notifiche nelle
  impostazioni (o quello del telefono), e tace se le notifiche sono messe
  a tacere
- **[Progetto]** Dove stanno: **nelle preferenze del telefono**, in un
  file a parte (`widgets`), in JSON, e non nel database delle note: sono
  attrezzi, non contenuto — nel database finirebbero dentro ogni backup e
  ogni copia di pagina. Quindi **nessuna migrazione**, e un widget rotto
  non può toccare le note. `WidgetStore` è osservabile come
  `AppSettings`: la barra si ridisegna da sola a ogni modifica. Orologi e
  barre si aggiornano ogni secondo **solo a barra aperta** (chiusa, resta
  disegnata dietro alla pagina, e ridisegnarla per niente consumerebbe
  batteria)
- **[Progetto]** **Il disegno dei widget si guarda anche in cloud**:
  `WidgetsRenderTest` disegna la sezione (tutti e quattro i widget, e la
  sezione chiusa) con Robolectric e la salva come immagine in
  `app/build/widget-screenshots/`. Servono, solo per i test, la libreria
  `ui-test-junit4` di Compose e `unitTests.isIncludeAndroidResources`
  in `app/build.gradle.kts`. `captureToImage` di Compose sotto
  Robolectric non finisce mai: si disegna a mano la finestra su una
  bitmap
- **[Bug fix]** *(trovato con quel disegno, prima di arrivare al
  telefono)* **Il contenuto di ogni widget era disegnato tutto nello
  stesso punto**, righe una sopra l'altra: stava in un `Box` invece che
  in una `Column`. E **le barre dell'avanzamento erano lunghe ognuna a
  modo suo**, perché l'etichetta accanto ("Anno", "Settimana") si
  allargava e si stringeva; ora ha una larghezza fissa
- **[Progetto]** Test: `WidgetsTest` (11: anno, mese, settimana dal
  lunedì, giorno, date proprie, ovest-est anche con l'ora legale, nomi e
  "GMT+5:30", il pomodoro che avanza, recupera più fasi, resta fermo in
  pausa, e il salvataggio in JSON andata e ritorno) e `WidgetsRenderTest`
  (1). Testi nuovi nelle otto lingue (`WidgetStrings`).
  `DayPickerDialog` e i suoi due aiuti in `DatabaseViewScreen.kt` sono
  ora `internal`, per usarli anche qui
- **[Progetto]** README: corrette due affermazioni rimaste indietro nei
  Limiti noti — "la ricerca nei database non esiste" (c'è dalla
  richiesta 35) e "Linked view of data source" fra le voci grigie dei
  menu "+" e "/" (costruita alla richiesta 36). Barrate, non cancellate

### 🆕 v0.75 — 24/09/2026 · 14:56 — Viste collegate — lo stesso database in un'altra pagina
*(verificato in parte sul telefono il 24/09/2026: voce 28)* *(sessione
cloud del 24/09/2026, richiesta 36: compilato, 51 test passati — 9
nuovi, compresa la migrazione 28→29 aperta da Room —, **schema alla
versione 29**, **non provato sul telefono**)*
- **[Nuova funzionalità]** **"Linked view of data source"** nei menu "/"
  e "+" (finora grigia) e in "Turn into": si sceglie un database da un
  elenco con la ricerca in cima — tutti i database dell'app, ognuno con
  sotto la pagina in cui sta, come "Existing data sources" di Notion
  (`LinkedSourcePickerSheet`) — e nella pagina compare **lo stesso
  database**: stesse righe, proprietà, pagine e quello che c'è scritto
  dentro. Non è una copia: scrivere da una parte è scrivere dall'altra
- **[Nuova funzionalità]** **La vista nasce identica** all'originale —
  vista, filtro, ordinamento, raggruppamento, galleria, calendario,
  proprietà nascoste — e da lì ha **impostazioni sue**: nella pagina
  "PS5" si filtra Piattaforma = PS5 e nel database principale non cambia
  niente. Anche **le proprietà nascoste** sono della vista
  (`PageEntity.viewHiddenColumnIds`)
- **[Nuova funzionalità]** **Il titolo è "↗ Nome del database"**, quello
  vivo dell'origine, e **toccandolo si apre il database principale** a
  schermo intero, come chiesto (`LinkedViewTitle`). Icona, nome, blocco
  del contenuto e "database semplice" sono quelli dell'origine
- **[Nuova funzionalità]** **Una riga nuova nasce col valore del filtro**,
  come su Notion — nella vista PS5, "Nuova pagina" crea un gioco PS5 nel
  database principale. Vale per ogni database filtrato, non solo per le
  viste: prima una riga creata con un filtro attivo spariva appena nata.
  Per le selezioni (il primo valore scelto) e per le caselle filtrate
  sulle spuntate (`fillFilterValue`)
- **[Nuova funzionalità]** Menu del blocco su una vista: le voci del
  database (semplice / complesso, Lock database, Rename, Edit icon)
  agiscono **sull'origine**, quelle della pagina (Duplicate, Move to,
  Move to trash, preferiti) **sulla vista**; niente "Turn into page".
  **Togliere la vista** (Delete nelle sue impostazioni) lo dice con una
  finestra sua: se ne va solo la vista, il database resta
- **[Nuova funzionalità]** Se il database di origine è cancellato o nel
  cestino, al posto della vista una riga lo dice, coi sei puntini per
  toglierla (`LinkedSourceMissing`); ripristinandolo, la vista torna
- **[Nuova funzionalità]** Le viste **non compaiono nella barra laterale
  né nella ricerca generale**: sono un altro modo di guardare un database
  che lì c'è già, e lo avrebbero ripetuto. Una pagina con solo viste non
  ha il triangolino dei figli (`BlockDao.countLinksInPage`)
- **[Progetto]** Com'è fatta. Una vista collegata è una pagina-database
  con `sourceDatabaseId` (l'origine) e **nessun dato suo**: il ViewModel
  del database tiene separate **la pagina della vista** (`page`, le
  impostazioni) e **quella dei dati** (`dataPage`, l'origine), e legge
  righe, colonne, celle, icone, copertine e testo per la ricerca dalla
  seconda. Per un database vero sono la stessa pagina, e non cambia
  niente. **Migrazione 28→29**: due colonne `TEXT` in `pages`, vuote per
  tutto quello che c'era
- **[Progetto]** **Attenzione trovata scrivendolo**: nello stato della
  vista le colonne hanno il "nascosto" della vista; ogni funzione che
  salva una colonna partendo da lì — rinominarla, cambiarle le opzioni,
  spostarla, centrarla — avrebbe scritto quel "nascosto" **dentro il
  database di origine**, dove l'avrebbero visto tutti. Ora si parte
  sempre dalle colonne vere (`rawColumns`); controprova fatta: tolta la
  protezione, il test lo coglie
- **[Bug fix]** *(trovato scrivendo questo)* **Una riga nuova in un
  database filtrato poteva prendere il numero d'ordine di una riga
  nascosta**: il numero si calcolava sulle righe visibili, già filtrate
  (o, da oggi, cercate). Due righe con lo stesso numero non hanno un
  ordine sicuro fra loro. Ora si conta su tutte (`lastRowOrderIndex`)
- **[Progetto]** Test: `MigrationTest` (un file alla versione 28 con lo
  schema di allora, aperto da `AppDatabase.getInstance` come fa l'app:
  Room esegue la migrazione e controlla lo schema; controprova fatta —
  senza la migrazione l'apertura fallisce), `LinkedViewModelTest` (4:
  righe dell'origine col filtro della vista, proprietà nascosta solo lì,
  riga nuova nell'origine col valore del filtro, origine nel cestino), e
  4 prove nuove in `PageRepositoryTest`. Testi nuovi nelle otto lingue
  (`DbStrings.linkedViewTitle`, `linkedViewHint`, `openSourceDatabase`,
  `linkedSourceMissing`, `noDatabasesToLink`, `removeLinkedViewTitle`,
  `removeLinkedViewText`)

### 🆕 v0.74 — 24/09/2026 · 14:35 — Ricerca dentro un database — la lente accanto a Sort
*(verificato in parte sul telefono il 24/09/2026: voce 27)* *(sessione
cloud del 24/09/2026, richiesta 35: compilato, 42 test passati — 10
nuovi —, nessuna migrazione, **non provato sul telefono**)*
- **[Nuova funzionalità]** Nella riga degli strumenti di ogni database,
  **subito dopo Sort**, la **lente**. Toccandola, lì dov'era la riga
  compare **la barra della ricerca**, col cursore già dentro: si scrive
  subito, come su Notion. La **X** la chiude, cancella la parola e
  rimette la riga degli strumenti (`DatabaseSearchBar`). Si può cercare
  anche con "Lock view": cercare non cambia com'è fatto il database
- **[Nuova funzionalità]** **Cosa si cerca**, a ogni lettera, senza
  distinguere maiuscole e minuscole: il **nome** delle righe; il valore
  delle **proprietà** che si leggono come sono scritte — testo, numero,
  selezione, selezione multipla (valore per valore), URL, email,
  telefono; e **tutto quello che c'è dentro la pagina** della riga — il
  testo dei blocchi (anche nei toggle chiusi), le celle delle tabelle, i
  nomi delle sottopagine. Date, caselle e date automatiche no: il loro
  valore salvato ("2026-09-24", "true") non è quello che si legge, e
  "true" troverebbe ogni casella spuntata
- **[Nuova funzionalità]** **Cosa si illumina**, come chiesto: se la
  parola è **nel nome**, si illuminano di giallo **le sue lettere**;
  se è **dentro** (una proprietà, la pagina), si illumina di un giallo
  più leggero **tutta la riga** — o la scheda, o la barra. Giallo e non
  l'azzurro dell'app, che vuol dire "selezionato". In **tutte e sei le
  viste**: tabella, bacheca, elenco, galleria, calendario, linea del tempo
  (`highlightedTitle`, `Modifier.foundInside`, `LocalRowSearch`)
- **[Nuova funzionalità]** **Le righe senza la parola si nascondono**
  mentre si cerca, come su Notion; senza nessuna riga trovata, sotto la
  barra c'è "Nessun risultato". La ricerca è un filtro in più sopra a
  quello delle impostazioni, e sta nel ViewModel del database
  (`searchRows`, dentro lo stesso calcolo del filtro e dell'ordinamento):
  per questo vale in ogni vista senza che nessuna debba saperlo, e
  aprendo una pagina trovata e tornando indietro la ricerca è ancora lì
  (senza riaprire la tastiera)
- **[Nuova funzionalità]** **Una riga creata mentre si cerca resta
  visibile** fino alla chiusura della ricerca: è vuota, non ha la
  parola, e sparendo subito avrebbe fatto sembrare rotto "Nuova pagina"
- **[Progetto]** **Veloce anche con tanto testo**: il testo delle pagine
  delle righe si legge dal database **una volta**, all'apertura della
  ricerca e al ritorno sulla schermata (tre query:
  `DatabaseDao.getRowPageBlockTexts`, `getRowPageTableTexts`,
  `getRowPageLinkTitles`; `DatabaseRepository.searchIndex`); ogni lettera
  scritta poi cerca in memoria. Il nome e le proprietà invece sono quelli
  vivi della tabella
- **[Progetto]** Test: `DatabaseSearchTest` (9 prove su `searchRows`:
  nome, proprietà, selezione multipla, pagina, date e caselle escluse,
  spazi, accenti, riga nuova) e una prova in più in
  `PageRepositoryTest` sull'indice, su un database vero (testo diviso in
  più pezzi con grassetto e spoiler, tabella, sottopagina, riga mai
  aperta). Testi nuovi nelle otto lingue (`DbStrings.searchInDatabase`,
  `closeSearch`)

### 🆕 v0.73 — 24/09/2026 · 14:24 — Annulla e Ripristina anche per Move to, Duplicate e Move to trash
*(verificato in parte sul telefono il 24/09/2026: voci 25 e 26)*
*(sessione cloud del 24/09/2026, richiesta 34: compilato, 32 test passati
— 16 nuovi, sul database vero —, nessuna migrazione, **non provato sul
telefono**)*
- **[Nuova funzionalità]** **Annulla disfa anche "Move to", "Duplicate"
  e "Move to trash", e Ripristina li rifà.** Prima Annulla conosceva solo
  le foto dei blocchi della pagina aperta, e queste tre toccano altre
  pagine:
  - **Move to** → la pagina torna **esattamente dov'era**: stessa pagina,
    stesso punto fra le righe, dentro lo stesso toggle. Un database
    torna **aperto dentro la pagina**, se lo era. La pagina di una riga
    torna nella sua riga **con i valori delle proprietà**, che lo
    spostamento aveva tolto
  - **Duplicate** → la copia va **nel Cestino**, come su Notion quando si
    toglie una pagina: se nel frattempo ci si è scritto dentro, niente è
    perso. Se la copia era una riga nuova di un database, la riga si
    toglie. Ripristina la tira fuori dal Cestino e la rimette dov'era
  - **Move to trash** → la pagina esce dal Cestino e torna al suo posto
    (fatto anche questo: stesso meccanismo, e la voce stava nello stesso
    elenco di Limiti noti)
- **[Nuova funzionalità]** **Dove si annulla.** Dal menu del blocco, nella
  pagina stessa. Dai tre puntini la schermata se ne va — si torna
  indietro, o si entra nella copia — e l'Annulla passa alla **prossima
  pagina che compare** (`PendingPageUndo`, preso in
  `adoptPendingPageChange` quando la pagina si apre o ci si torna):
  tornati indietro dopo un Move to, lì Annulla rimette la pagina dov'era;
  dentro la copia dopo un Duplicate, Annulla la manda nel Cestino e
  **torna indietro da solo** (restare davanti a una pagina nel Cestino
  non avrebbe senso)
- **[Progetto]** Com'è fatto. La cronologia della pagina ha **due tipi di
  passo**: le foto dei blocchi, come prima (`BlocksStep`), e i
  cambiamenti alle pagine (`PageStep`), che si portano dietro quello che
  serve a tornare indietro — i collegamenti com'erano, la riga e le sue
  celle (`PageRepository.PageChange`: `Moved`, `Duplicated`, `Trashed`;
  `undoPageChange`, `redoPageChange`). I collegamenti tornano al loro
  posto facendo scalare di uno quelli che seguono; se il toggle in cui
  stavano non c'è più vanno in fondo alla pagina, e se la pagina non si
  potesse rimettere da nessuna parte va in fondo al menu principale
  invece di restare irraggiungibile (`putLinksBack`). Annulla e
  Ripristina ora si eseguono **uno alla volta** (`historyLock`): un passo
  di pagina lavora sul database per un attimo, e due tocchi veloci non
  devono sovrapporsi. Se la pagina nel frattempo è stata cancellata per
  sempre (Cestino svuotato), il passo si salta
- **[Progetto]** **Test su un database vero, senza telefono.** Aggiunti
  Robolectric 4.13 e `androidx.test:core-ktx` **solo per i test**
  (l'APK non cambia) e `PageRepositoryTest`: 16 prove che girano su Room
  e SQLite veri, in memoria, con le chiavi esterne accese come nell'app —
  spostare (in fondo; nella stessa pagina; un database arriva come
  collegamento; dentro una propria sottopagina rifiutato; la pagina di
  una riga esce e con Annulla torna con le sue proprietà; dentro una riga
  mai aperta), duplicare e buttare con Annulla e Ripristina (anche la
  pagina di una riga, e una pagina cancellata per sempre nel frattempo),
  le foto di Annulla (tabelle, collegamento a un database cancellato, a
  una pagina spostata), "Duplicate" di un toggle con sottopagina, il
  toggle che lascia uscire le sue righe, "Turn into simple database"
- **[Progetto]** **Controprova sui due difetti "trovati leggendo il
  codice"**: tolta per un momento ciascuna correzione, il suo test
  fallisce. Senza `withoutStaleLinks` Annulla dopo aver eliminato un
  database finisce in `SQLiteConstraintException: FOREIGN KEY constraint
  failed (code 787)` — è la chiusura dell'app; senza rimettere le celle,
  la tabella resta vuota. Non erano ipotesi. Le correzioni sono al loro
  posto, e i test passano

### 🆕 v0.72 — 24/09/2026 · 14:04 — Move to — l'albero con la ricerca
*(verificato in parte sul
telefono il 24/09/2026: voce 24)* *(sessione cloud del 24/09/2026,
richiesta 33: compilato, test passati, nessuna migrazione, **non
provato sul telefono**)*
- **[Nuova funzionalità]** **"Move to" apre l'albero della barra
  laterale con la ricerca in cima** — lo stesso di "Duplicate → Dentro
  una pagina" (`DestinationPickerSheet`, ora condiviso, e
  `MovePagePicker`) — dai tre puntini di una pagina, dai tre puntini di
  un database a schermo intero e dal menu del blocco. Prima era un
  elenco piatto, in ordine alfabetico, delle sole pagine di testo
- **[Nuova funzionalità]** Si può scegliere **anche la pagina di un
  database**: i database si aprono come cartelle, e si sceglie una delle
  loro righe — anche una mai aperta, la cui pagina nasce in quel
  momento. Il database stesso no: il suo contenuto sono righe, non
  blocchi. Prima le pagine delle righe erano escluse
- **[Nuova funzionalità]** **Sempre in fondo** alla pagina scelta, come
  chiesto: anche scegliendo la pagina in cui sta già (prima lì non
  succedeva niente). Un avviso dice "Spostata in …"; dal menu del blocco
  si resta nella pagina, dai tre puntini si torna indietro come prima
- **[Nuova funzionalità]** **Un database spostato arriva sempre come
  collegamento a pagina**, anche se prima era aperto dentro la pagina:
  per riaprirlo lì c'è "Turn into database" (tenendo premuto il
  collegamento, o dai tre puntini del database). Prima si ricreava
  sempre aperto. La riga di spiegazione in cima all'albero lo dice
- **[Nuova funzionalità]** **"Move to" sulla pagina principale c'è ma è
  grigia** (prima non c'era proprio). L'utente ha lasciato la scelta fra
  la voce grigia e un messaggio "The main menu cannot be moved": messa
  la grigia, perché è già il modo in cui l'app dice "non qui" (Import ed
  Export nello stesso menu) e non costa un tocco a vuoto. Cambiarla col
  messaggio è una riga
- **[Bug fix]** *(trovato scrivendo questo)* **Si poteva spostare una
  pagina dentro una sua sottopagina**: il vecchio elenco offriva tutte
  le pagine tranne quella stessa. Il risultato era un giro chiuso — la
  pagina dentro la sottopagina, la sottopagina dentro la pagina — e
  nessuna delle due si raggiungeva più dal menu principale. Ora la
  pagina e tutto quello che ha dentro, giù fino in fondo, si vedono
  grigie e non si aprono, e cercando non compaiono
  (`PageRepository.moveExclusions`); il repository rifiuta comunque lo
  spostamento se arriva da un'altra strada
- **[Bug fix]** *(trovato scrivendo questo)* **La pagina di una riga
  spostata restava anche nel database**, cioè in due posti. Ora esce dal
  database, come su Notion: la riga si toglie con le sue proprietà, e la
  pagina diventa una pagina come le altre (`isRowPage` spento,
  `PageDao.setRowPage`). La riga di spiegazione lo dice prima di scegliere
- **[Bug fix]** *(trovato scrivendo questo; **confermato dai test** della
  richiesta 34)* **Annulla dopo aver
  eliminato un database dentro la pagina faceva chiudere l'app.**
  L'eliminazione (`deleteDatabaseBlock`) scatta una foto per Annulla e
  poi cancella il database; Annulla rimetteva il blocco, che puntava a
  una pagina non più esistente, e il database lo rifiutava (chiave
  esterna) — un errore dentro una coroutine senza nessuno che lo
  raccolga. Ora, a ogni Annulla e Ripristina, i collegamenti della foto
  a pagine **cancellate, nel cestino o spostate in un'altra pagina** non
  tornano (`PageRepository.withoutStaleLinks`, dentro
  `replaceAllBlocks`). Vale per tutte le strade — tre puntini della
  sottopagina, menu del blocco, cestino — e sostituisce `forgetLinksTo`
  della voce precedente, che copriva solo il menu del blocco
- **[Rimosso]** La vecchia finestra di "Move to" (`MoveToSheet`), la
  query delle sue destinazioni (`PageDao.getMoveDestinations`,
  `PageRepository.moveDestinations`), le funzioni che la caricavano nei
  due ViewModel e il testo "Non c'è un'altra pagina in cui spostarla"
  (`Strings.noOtherPage`): con l'albero c'è sempre almeno il menu
  principale
- **[Progetto]** `TreeRow` ha lo stato spento (`enabled`), grigio e senza
  triangolino. Testi nuovi nelle otto lingue (`Strings.moveHint`,
  `moveDatabaseHint`, `moveRowPageHint`, `movedInto`)

### 🆕 v0.71 — 24/09/2026 · 13:52 — Il menu del blocco — i sei puntini
*(verificato in parte sul
telefono il 24/09/2026: voci 22 e 23; Duplicate aveva un bug, corretto —
vedi "Ripresa sul PC")* *(sessione cloud del 24/09/2026,
richieste 31-32: compilato, test passati, nessuna migrazione — schema
sempre alla 28 —, **non provato sul telefono**)*
- **[Nuova funzionalità]** Nella barra sopra la tastiera, **prima del
  rientro a sinistra**, un'icona coi **sei puntini** (due colonne da tre,
  `Icons.Filled.DragIndicator`, la stessa di Notion): apre il **menu
  della riga in cui sta il cursore**. Il cursore si spegne prima di
  aprirlo: la tastiera sparirebbe comunque sotto la finestra, e una riga
  che "Turn into" fa sparire non deve morire col fuoco dentro — il modo
  in cui l'app si chiudeva (vedi più giù in Cronologia "La tastiera non
  sparisce più", e la causa di fondo lì sotto). La finestra
  (`BlockActionsHost`, file nuovo `BlockActionsSheet.kt`) sta a livello
  di schermata, come il pennello e la dimensione del testo, perché la
  barra sparisce con la tastiera
- **[Nuova funzionalità]** **Per una riga di testo** (paragrafo, elenchi,
  casella, toggle, righe dentro un toggle): **Turn into, Color,
  Duplicate**. Edit icon, Add to favorites, Move to e Move to trash ci
  sono solo per le pagine, come chiesto (richiesta 32). **Rename**
  anche: una riga di testo non ha un nome da cambiare, il suo nome è il
  testo stesso — scelta da confermare con l'utente
  - **Turn into**: le **stesse voci dei menu "/" e "+"**, nello stesso
    ordine (`SLASH_ENTRIES`), grigie quelle non costruite, con la
    **spunta sul tipo attuale**. Rispetto al "/" due differenze, che
    vengono dal punto da cui si parte: il testo della riga **non si
    perde** — diventa il nome della pagina o del database, la prima
    cella della tabella (senza grassetti e colori: le celle sono testo
    semplice), la riga sotto il divisore — e **si resta nella pagina**
    invece di entrare in quella nuova, come su Notion. Dal "/" la riga
    è vuota e non serviva: lì non cambia niente
    (`convertToPageLink`/`convertToDatabaseLink` con `title`,
    `convertToDividerAndFocusNext` con `keepText`,
    `convertToTableKeepingText`)
  - **Color**: la finestra del pennello, ma il colore — del testo o
    dietro al testo — va su **tutta la riga**, senza doverla selezionare
    (`colorWholeBlock`). Parte dal colore che la riga ha già
  - **Duplicate**: la copia nasce **subito sotto**, con tipo, testo,
    colori, rientro e spunta. Un toggle si copia **coi suoi figli**, una
    tabella **con le sue celle**, e le sottopagine o i database dentro
    un toggle si copiano anche loro, come fa "Duplicate" di una pagina:
    due collegamenti alla stessa sottopagina non sarebbero una copia
    (`PageRepository.duplicateBlock`)
- **[Nuova funzionalità]** **Tenendo premuto il collegamento a una
  pagina** nel testo (dove il cursore non entra) si apre il menu **della
  pagina**: Turn into (grigio: trasformare una pagina in altro non è
  ancora costruito), **Color** (il colore del nome, solo in quel
  collegamento, tenuto in uno span vuoto del blocco), **Edit icon** (la
  stessa finestra Upload / Link / Remove), **Add to favorites** /
  **Remove from favorites**, **Rename** (una finestrella col nome già
  tutto selezionato; cambia solo il titolo, `PageDao.setTitle`, e il
  nome della riga se è la pagina di una riga), **Duplicate** (la stessa
  scelta del posto dei tre puntini; "accanto all'originale" vuol dire
  **sotto il collegamento toccato**, anche se la pagina è collegata in
  due punti), **Move to**, **Move to trash** (con la stessa conferma dei
  tre puntini). Si resta sempre nella pagina: un avviso dice dove è
  finita la copia quando non è qui. Il tocco normale apre la pagina come
  prima (`combinedClickable`)
- **[Nuova funzionalità]** **Per un database**, in cima: **Turn into
  page**, **Turn into simple database**, **Turn into complex database**,
  **Lock database** (interruttore), **Open as page**; sotto le voci
  della pagina, con Turn into e Color grigi. Ci si arriva dai **sei
  puntini** che ora stanno all'inizio della riga degli strumenti di un
  database dentro la pagina — prima del riquadro della vista, perché
  il nome si può nascondere e i puntini devono esserci sempre — o
  tenendo premuto un collegamento a un database
  - **Turn into page** fa quello che fa la voce delle impostazioni del
    database: il blocco diventa un collegamento e il database si apre a
    schermo intero. Se il database è **già** un collegamento, al suo
    posto c'è **Turn into database**, che rimette dentro la pagina
    **quel** collegamento (`turnPageLinkIntoDatabase`; la voce dei tre
    puntini li rimette dentro tutti)
  - **Turn into simple database**: le pagine delle righe si cancellano
    **per sempre**, con quello che contengono — non nel cestino: in un
    database semplice una pagina di riga non avrebbe più una riga a cui
    tornare —; nomi e proprietà delle righe restano
    (`turnIntoSimpleDatabase`). L'utente ha detto che ci sta attento
    lui; comunque, **se almeno una pagina di riga ha qualcosa dentro**
    (testo, un blocco che non sia una riga vuota, un'icona, una
    copertina) una conferma dice **quante sono**. Se non c'è niente da
    perdere si fa e basta (`countRowPagesWithContent`)
  - **Turn into complex database**: le righe tornano pagine. Ognuna
    nasce la prima volta che la si apre, come in ogni database
    (`ensureRowPage`), e compaiono subito nella barra laterale
  - Le due voci ci sono sempre: quella che il database è già si vede
    grigia con la spunta
- **[Bug fix]** *(trovato leggendo il codice per il Duplicate;
  **confermato dai test** della richiesta 34)* **Annulla
  e Ripristina svuotavano tutte le tabelle della pagina.** Rimettono la
  pagina com'era riscrivendo tutti i blocchi da capo
  (`replaceAllBlocks`: cancella e reinserisce), e le celle delle tabelle
  sono legate al loro blocco con `ON DELETE CASCADE`, con le chiavi
  esterne accese (`PRAGMA foreign_keys = ON` nel codice generato da
  Room): cancellando la tabella il database portava via il suo testo.
  Bastava annullare una lettera scritta tre righe più su. Ora le celle
  si mettono da parte e si rimettono, tutto in una transazione (e
  `TableCellDao.getCellsForPageOnce`). Il testo delle celle resta quello
  di adesso: Annulla non lo segue, come già scritto nel codice
- **[Bug fix]** *(trovato scrivendo "Turn into")* **Un toggle con delle
  righe dentro, cambiato in un altro tipo dal "+"**, lasciava le sue
  righe appese a un blocco che non le mostra: **sparivano alla vista**
  (nel database c'erano ancora). Ora escono subito sotto, allo stesso
  livello e nello stesso ordine, come quando il toggle si toglie col
  backspace — per ogni trasformazione: tipo, pagina, database, tabella,
  divisore (`PageRepository.unnestChildren`, estratta da
  `unnestAndConvert`)
- **[Nuova funzionalità]** Il collegamento a una pagina **segue la
  pagina nel tempo** (`observePageInfo` al posto di `getPageInfo`):
  prima nome e icona si leggevano quando la riga compariva e restavano
  quelli; con Rename ed Edit icon dal menu del blocco devono cambiare
  subito
- **[Progetto]** **Annulla non fa ricomparire il collegamento a una
  pagina spostata o buttata** dal menu del blocco: le foto di Annulla
  sono tutti i blocchi della pagina, e quelle scattate prima avevano
  ancora il collegamento. Viene tolto da tutte (`forgetLinksTo`).
  *Superato* nella voce successiva ("Move to — l'albero con la
  ricerca"): `forgetLinksTo` è stata tolta, e al suo posto un controllo
  fatto a ogni Annulla vale per tutte le strade, non solo per il menu
  del blocco (`withoutStaleLinks`)
- **[Progetto]** `SLASH_ENTRIES`, `SlashEntry`, `SlashAction`,
  `SlashCategory` (in `PageEditorScreen.kt`) e `OptionsGroup`,
  `OptionRow` (in `PageOptionsSheet.kt`) da privati a `internal`, per
  usarli nel file nuovo. `PageRepository.duplicatePage` ha un parametro
  in più, `besideLinkBlockId`, messo **prima** di `copyImage` perché le
  chiamate che passano la copia delle immagini come ultima lambda
  continuino a funzionare. Testi nuovi nelle otto lingue
  (`EditorStrings.blockOptions`, `turnInto`, `color`, `editIcon`,
  `addToFavorites`, `removeFromFavorites`, `rename`,
  `turnIntoSimpleDatabase`, `turnIntoComplexDatabase`, `lockDatabase`,
  `openAsPage`, `simpleDatabaseLossTitle`, `simpleDatabaseLossText`)

### 🆕 v0.70 — 24/09/2026 · 13:25 — Database semplice — le righe sono solo testo
*(verificato in parte
sul telefono il 24/09/2026: voci 21 e 22)* *(sessione cloud del
24/09/2026, richiesta 30: compilato, test passati, migrazione 27→28
simulata, **non provato sul telefono**)*
- **[Nuova funzionalità]** **"Simple database"** nei menu "/" e "+"
  (finora grigia) crea un **database semplice**: un database vero, con
  tutte le viste e le impostazioni degli altri — filtri, ordinamento,
  raggruppamento, proprietà, Lock view — ma in cui **le righe sono solo
  testo e non diventano mai pagine**. L'utente lo voleva da sempre, e
  nessuna app lo ha. Nasce come tabella, come "Database - Inline"; la
  vista si cambia dalle impostazioni. Salvato in
  `PageEntity.isSimpleDatabase` (**migrazione 27→28**, 0 per tutti i
  database che c'erano: restano normali)
- **[Nuova funzionalità]** **Toccando una riga si apre la sua scheda**
  invece di una pagina, in tutte e sei le viste (tabella dal tocco
  prolungato, bacheca, elenco, galleria, calendario, linea del tempo):
  in cima **il nome da scrivere**, subito sotto **tutte le proprietà**
  modificabili sul posto, poi Delete e le date di creazione e modifica
  (`RowActionsSheet` con `simple = true`, `SimpleRowNameField`). Nella
  tabella il nome si scrive toccandolo, come sempre
- **[Nuova funzionalità]** Tolto tutto quello che nei database normali
  riguarda le pagine delle righe: **niente OPEN** nella cella del nome,
  **niente voce "Icon"** nella scheda, **niente "Card preview"** nella
  galleria (copertina e testo sono della pagina della riga: le schede
  hanno nome e proprietà), e i pulsanti dicono **"Nuova riga"** invece di
  "Nuova pagina". In fondo alle impostazioni una riga dice che il
  database è semplice e che cosa vuol dire
- **[Nuova funzionalità]** **Nessuna strada porta a una pagina di
  riga**: il divieto sta in `PageRepository.ensureRowPage`, l'unico punto
  da cui una pagina di riga nasce, che per un database semplice rifiuta.
  E in più: nella **barra laterale** il database semplice non ha figli
  (`treeChildren`, `nodeFor`); nella **ricerca** una riga trovata porta
  **al database** e dice in quale sta; il **Duplicate** non offre le sue
  righe come posto in cui copiare
- **[Bug fix]** *(trovato scrivendo questo)* La lista dei risultati della
  ricerca usava come chiave l'identificativo della pagina: un risultato
  di riga che porta al database, e due righe dello stesso database,
  avrebbero avuto la stessa chiave, cosa che una `LazyColumn` non
  perdona — l'app si chiude. Ora la chiave è prima la riga, poi la
  pagina (`SearchScreen`)
- **[Progetto]** `RowPropertiesList` è stata estratta dalla finestra
  delle azioni di una riga, per usarla sia in "Edit property" sia nella
  scheda della riga semplice. Testi nuovi nelle otto lingue
  (`DbStrings.newRow`, `row`, `simpleDatabaseNote`)

### 🆕 v0.69 — 24/09/2026 · 13:12 — Menu "+" — le stesse voci del menu "/", nello stesso ordine
*(verificato in parte sul telefono il 24/09/2026: voce 20)*
*(sessione cloud del 24/09/2026, richiesta 29: compilato, test passati,
**non provato sul telefono**)*
- **[Nuova funzionalità]** Il menu "+" della barra **legge lo stesso
  catalogo del menu "/"** (`SLASH_ENTRIES`): stesse voci, stesse
  icone, **stesso ordine**, comprese le voci non ancora costruite, che
  ci sono e sono grigie. Prima il "+" aveva un elenco suo, fermo alle
  nove voci dei primi tempi, mentre il "/" cresceva. Ora una voce
  aggiunta al catalogo compare in tutti e due
- **[Nuova funzionalità]** **Due colonne riempite riga per riga**, come
  chiesto: la prima voce del "/" è la prima a sinistra, la seconda la
  prima a destra, e così via. Oggi (27 voci, l'ultima riga ne ha una):

  | sinistra | destra |
  |---|---|
  | Text | Bulleted list |
  | Numbered list | To-do list |
  | Toggle list | Page |
  | *Callout* | *Quote* |
  | Table | Divider |
  | *Link to page* | *Image* |
  | *Video* | *Audio* |
  | *Code* | *File* |
  | *Web bookmark* | Table view |
  | Board view | Gallery view |
  | List view | Calendar view |
  | Timeline view | Database - Inline |
  | *Database - Full page* | Simple database |
  | *Linked view of data source* | |

  (in corsivo le grigie; "Simple database" era grigia fino alla
  richiesta 30). Senza i titoli delle famiglie che ha il "/"
  (Basic blocks, Media, Database): su due colonne riga per riga
  spezzerebbero l'ordine chiesto, lasciando buchi a fine famiglia. Il
  menu scorre, perché le righe sono più di quante ne stiano sopra la
  tastiera. Le icone sono quelle del "/" (prima il "+" aveva le sue,
  con le lettere "T", "H1"...)
- **[Progetto]** **Una sola funzione applica la voce scelta**, da tutti
  e due i menu (`applyInsertAction` in `PageEditorScreen`): erano due
  copie quasi uguali e si stavano già separando — il "+" creava un
  database sempre come tabella, mentre dal "/" "Calendar view" dava un
  calendario. Ora dal "+" ci sono anche tutte le viste di database
  (Table, Board, Gallery, List, Calendar, Timeline view), che prima
  aveva solo il "/". Il comportamento di ogni voce è quello che aveva
  nel "/"

### 🆕 v0.68 — 24/09/2026 · 13:06 — Pagine — la dimensione si scrive a mano, e via i titoli (Heading)
*(verificato sul telefono il 24/09/2026: voci 18 e 19, e la migrazione
26→27 su titoli veri)* *(sessione cloud del 24/09/2026, richiesta 28: compilato, test passati,
migrazione 26→27 provata su un database di prova, **non provato sul
telefono**)*
- **[Nuova funzionalità]** **La dimensione del testo si scrive a
  mano**: toccando il numero nella barra Aa si apre una finestrella
  (`FontSizeDialog`) con una piccola casella, il numero in uso già
  selezionato (scrivendo lo si sostituisce) e **la tastiera dei
  numeri**. Accetta solo cifre, al massimo due; "Fatto" — e il tasto
  Fatto della tastiera — funziona solo fra 5 e 72, e sotto la casella
  "Da 5 a 72" diventa rosso se si esce. Sostituisce l'elenco di numeri
  della prima versione, come chiesto dall'utente
- **La finestra e non una casella dentro la barra**, di proposito:
  mentre si scrive il numero la tastiera passa alla casella, nessuna
  riga della pagina ha più il cursore, e la barra Aa — che esiste solo
  quando una riga ce l'ha — se ne va. Una casella nella barra sparirebbe
  a metà numero; per lo stesso motivo la finestra è accesa da uno stato
  della schermata (`showFontSizeDialog`), come quella del pennello, e
  non nasce dentro la barra. Chiusa la finestra, **il cursore non torna
  da solo** nella pagina: si tocca dove si vuole riprendere. Rimetterlo
  a comando vorrebbe dire spostare il fuoco mentre la tastiera passa da
  numeri a lettere, il passaggio che con la tastiera Samsung dà più
  problemi
- **[Rimosso]** **I titoli — Heading 1, 2 e 3 — dall'app intera**, su
  richiesta dell'utente: con la dimensione scelta per pagina, dei titoli
  a misura fissa non se ne faceva più niente (e i "Toggle Heading" che
  stava per chiedere non servono più). Tolti dal menu "+" e dal menu
  "/", dal tipo di blocco (`BlockType`: i tre valori non esistono più),
  dal campo unito (`FLOWING_TYPES`, `isFlowingTextType`), dalla sua
  trasformazione visiva, dalle isole, dall'anteprima della Gallery e
  dai testi tradotti. Nessuna scorciatoia di scrittura li creava
- **[Rimosso]** **Quelli già scritti sono diventati testo normale**
  (**migrazione 26→27**: `UPDATE blocks SET type = 'PARAGRAPH'`). Fra le
  tre possibilità proposte — testo in grassetto, testo normale,
  lasciarli come sono — l'utente ha scelto il testo normale. Il testo
  non si perde, e la formattazione messa a mano (grassetto, colori)
  resta. Provata su un database di prova: tre titoli con grassetto e
  colore diventano paragrafi identici, gli altri blocchi non si toccano,
  e nessun tipo rimasto nel database è sconosciuto all'app — che è
  quello che conta, perché un nome di tipo che l'enum non conosce più fa
  chiudere l'app alla lettura. Lo schema delle tabelle non cambia
- **"Predefinito"** in cima all'elenco dei font resta, confermato
  dall'utente

### 🆕 v0.67 — 24/09/2026 · 12:55 — Pagine — font e dimensione del testo (barra Aa)
*(verificato sul
telefono il 24/09/2026: voci 17 e 18; la voce della dimensione era
vuota, corretto — vedi "Ripresa sul PC")* *(sessione cloud
del 24/09/2026, richiesta 27: compilato, migrazione 25→26 simulata,
**non provato sul telefono**)*
- **[Nuova funzionalità]** Nella barra Aa, **prima di B**, due voci
  come su OneNote: **il font**, col nome di quello in uso ("Calibri", o
  "Predefinito" se la pagina non ne ha scelto uno), e **la dimensione**,
  col numero in uso ("16"). Toccandole si apre l'elenco per cambiarle.
  Valgono per **tutta la pagina**, come chiesto ("selezionando
  automaticamente tutto quanto"), e sono salvate per pagina
  (`PageEntity.pageFont` e `pageFontSize`, **migrazione 25→26**; null
  vale "come prima", e il 16 si salva come null). Con la pagina bloccata
  le due voci non ci sono
- **[Nuova funzionalità]** **I font, e chi li disegna davvero.** Quelli
  chiesti sono commerciali e non si possono mettere nell'app: al loro
  posto ci sono sosia con licenza libera (SIL Open Font License), scelti
  e approvati con l'utente. **Nella barra si legge il nome originale;
  nell'elenco il nome originale col sosia fra parentesi** — scelta
  dell'utente. L'elenco, nell'ordine chiesto, con un divisore fra i
  gruppi e "Predefinito" in cima per tornare al font di sistema:

  | Nome | Sosia | Da dove arriva |
  |---|---|---|
  | Helvetica | Inter | dentro l'app |
  | Garamond | EB Garamond | dentro l'app |
  | Arial | Arimo (stesse misure) | dentro l'app |
  | Verdana | Noto Sans (il più vicino) | dentro l'app |
  | Georgia | Gelasio (stesse misure) | dentro l'app |
  | Calibri | Carlito (stesse misure) | dentro l'app |
  | Futura | Jost | dentro l'app |
  | Times New Roman | Tinos (stesse misure) | dentro l'app |
  | Cambria | Caladea (stesse misure) | dentro l'app |
  | Consolas | Inconsolata (niente corsivo vero) | dentro l'app |
  | Songti | Noto Serif SC | scaricato |
  | Kaiti | Ma Shan Zheng (niente grassetto vero) | scaricato |
  | ~~MS YaHei~~ SimSun *(dal 25/09/2026)* | ~~Noto Sans SC~~ Noto Serif SC nel peso Light | scaricato |
  | Mincho | Noto Serif JP | scaricato |
  | Gothic | Noto Sans JP | scaricato |
  | Kaisei | Kaisei Opti (è proprio lui, già libero) | scaricato |

  **Lishu tolto**: un Lishu libero non esiste, e l'utente ha scelto di
  toglierlo invece di sostituirlo con un'altra calligrafia
- **[Nuova funzionalità]** **Da dove arrivano**, scelta dell'utente
  ("Misto"): i dieci occidentali stanno **dentro l'app** — 38 file TTF
  in `app/src/main/res/font` (normale, grassetto, corsivo, grassetto
  corsivo), circa 12 MB, scaricati da Google Fonts il 24/09/2026 — e
  funzionano sempre, anche offline. I sei cinesi e giapponesi pesano
  10-20 MB l'uno e **si scaricano da Google Play Services** la prima
  volta che una pagina li usa, poi restano sul telefono. L'APK di debug
  è passato da 17,8 a 23,9 MB
- **[Nuova funzionalità]** **Il corpo scala tutto in proporzione**, non
  solo il testo normale (`PageTypography`, `ui/theme/PageFonts.kt`): i
  titoli H1-H3 (28/22/18 a corpo 16; tolti poco dopo, richiesta 28),
  l'altezza delle righe del campo
  unito (24, o 27 con le caselle), i segni degli elenchi. A 16 ogni
  misura resta esattamente quella di prima. Le misure passano da un
  `CompositionLocal` (`LocalPageTypography`) e arrivano al campo unito
  (dal suo `textStyle`, l'unico posto dove il README dice che le misure
  si toccano senza rischi), alla sua trasformazione visiva (solo
  `fontSize` negli stili, nessun carattere in più: la traduzione delle
  posizioni non cambia), alle isole, ai toggle, alle celle delle tabelle
  e ai collegamenti a pagine. Il titolo della pagina prende **solo il
  font**. Restano fissi i quadratini delle caselle e le frecce dei
  toggle, e i database dentro la pagina
- **[Nuova funzionalità]** **Gli elenchi non chiudono la tastiera**
  (`PopupProperties(focusable = false)`): un menu che prende il fuoco
  chiude la tastiera, con lei se ne va la barra — ci è appoggiata sopra
  — e con la barra la voce da cui il menu è partito. Così si sceglie e
  si continua a scrivere. ~~La dimensione si sceglie da un elenco con
  tutti i numeri da 5 a 72~~ — **superato dalla richiesta 28**: la
  dimensione si scrive a mano in una finestrella, vedi la voce qui
  sotto
- **[Nuova funzionalità]** Nell'elenco dei font i nomi occidentali sono
  scritti **ognuno col suo font**, come nei menu di Word; quelli cinesi
  e giapponesi no, per non scaricarli tutti solo aprendo il menu
- **[Progetto]** Nuova dipendenza `androidx.compose.ui:ui-text-google-fonts`
  (versione dal BOM di Compose). I certificati del fornitore dei font
  sono in `res/values/font_certs.xml`, copiati dall'esempio ufficiale
  Android "DownloadableFonts" (licenza Apache 2.0). Gli avvisi di
  copyright e il testo della licenza dei dieci font inclusi sono in
  `app/src/main/assets/font_licenses/OFL.txt`, come chiede la licenza.
  Testi nuovi nelle otto lingue (`EditorStrings.defaultFont`, `font`,
  `fontSize`). Primo uso di `R` nel codice (`R.font`, `R.array`)

### 🆕 v0.66 — 24/09/2026 · 12:32 — Pagine — il conteggio del testo ("X words")
*(verificato sul
telefono il 24/09/2026: voce 16)* *(sessione cloud del
24/09/2026, richiesta 25: compilato e con i test automatici passati,
**non provato sul telefono**)*
- **[Nuova funzionalità]** In fondo al menu dei tre puntini di una
  pagina, dopo "Updates", c'è **"X words"** (col numero nel formato
  scelto nelle impostazioni: 2.652 o 2,652). **Toccandola si apre lì
  sotto** l'elenco completo, e ritoccandola si richiude. L'utente
  lasciava scegliere fra una finestra a parte e l'apertura sul posto:
  scelta la seconda, perché una finestra sopra il menu, su un telefono,
  copre proprio il menu da cui si arriva, e per richiuderla serve un
  tocco in più. Da far confermare all'utente
- **[Nuova funzionalità]** Le voci, nell'ordine chiesto: **Words,
  Letters, Numbers, Letters and numbers, Characters (no spaces),
  Characters (with spaces), Lines, Japanese characters, Chinese
  characters, Korean characters** (in italiano: Parole, Lettere,
  Numeri, Lettere e numeri, Caratteri senza e con spazi, Righe,
  Caratteri giapponesi, Caratteri cinesi, Lettere coreane — la parola
  dell'utente). Tutte e dieci sempre, anche a zero, come in Word
- **[Nuova funzionalità]** *(richiesta 26)* **"Lettere e numeri"** è
  una voce a sé, voluta dall'utente dopo aver visto come si contano i
  caratteri: la somma di lettere e cifre e **nient'altro**
  (`TextStats.lettersAndDigits`), mentre **i caratteri contano tutto**
  — lettere, numeri e ogni altro segno: parentesi, punti, virgole,
  asterischi... Così "hin(...) = 3x!" fa 5 lettere e numeri e 12
  caratteri senza spazi. Le due voci sui caratteri (senza e con spazi)
  restano come nella richiesta originale
- **[Nuova funzionalità]** **Le regole sono quelle della finestra
  "Word Count" di Word**, che l'utente ha mostrato come modello
  (`data/TextStats.kt`, `textStatsOf`):
  - una **parola** è quello che sta fra due spazi, anche "=" o "[]";
    in cinese e giapponese **ogni carattere è una parola**, perché
    quelle lingue non separano le parole con gli spazi; il coreano sì,
    e si conta come l'italiano;
  - le **lettere** sono quelle degli alfabeti (latino con gli accenti,
    greco, cirillico...), senza cinese, giapponese e coreano, che hanno
    le loro voci; i **numeri** sono le cifre, una per una ("2026" fa
    4), perché l'utente li descriveva come una parte dei caratteri;
  - i **caratteri** sono tutti i segni scritti, punteggiatura e simboli
    compresi, come in Word. L'utente li aveva descritti come "lettere e
    numeri insieme", ma così sarebbero stati solo la somma delle due
    voci sopra: **scelta da far confermare**. Con gli spazi si contano
    anche gli spazi (compresi quello "che non va a capo" e quello largo
    delle lingue asiatiche), non gli a-capo;
  - **righe** = blocchi con del testo, come ha chiesto l'utente ("in
    realtà quanti blocchi"): i blocchi vuoti non contano, una tabella
    conta una volta;
  - **giapponese o cinese**: gli ideogrammi sono gli stessi, quindi si
    guarda la riga — se ha almeno un hiragana o katakana, i suoi
    ideogrammi sono giapponesi, altrimenti cinesi. Il trattino lungo
    del katakana (ー) conta come giapponese;
  - si conta per **punti di codice** e non per `Char`, così un'emoji o
    un ideogramma raro è un carattere solo e non due
- **[Nuova funzionalità]** **Cosa si conta** (`PageRepository.textStats`):
  il testo di paragrafi, titoli, elenchi, caselle e toggle, **anche
  quello dentro i toggle chiusi**, e le celle delle tabelle semplici
  (solo quelle che si vedono: rimpicciolita una tabella, le celle
  rimaste fuori non contano). **Non si contano**, come chiesto, i
  database e tutto quello che l'utente non ha scritto lì: righe e celle
  dei database, i nomi delle pagine collegate, e **il titolo della
  pagina** — spesso messo dall'app ("Senza titolo", "Untitled"). Da far
  confermare all'utente
- **[Nuova funzionalità]** Per un **database** aperto a schermo intero
  la voce non c'è: il suo contenuto sono righe, e le righe non si
  contano. Il menu dei tre puntini ora **scorre**, perché aperto il
  conteggio le voci sono più di quante stiano su uno schermo basso
- **[Progetto]** **Primi test automatici del progetto**:
  `app/src/test/java/com/gabriele/notionlocal/data/TextStatsTest.kt`,
  sedici casi — italiano con accenti, liste di vocaboli con "[]" e
  "=", cifre, lettere e numeri separati dai simboli, giapponese con e
  senza kana, katakana col trattino lungo, cinese, coreano, lingue
  mescolate, spazi speciali, emoji, a-capo, testo vuoto. Girano senza
  telefono: `gradle testDebugUnitTest`

### 🆕 v0.65 — 24/09/2026 · 12:13 — Colonne — spostarle tenendo premuta l'intestazione
*(verificato in
parte sul telefono il 24/09/2026: voce 15)* *(trovata già
scritta nel codice arrivato dal PC il 24/09/2026, ma mancava in questo
README: documentata nella sessione cloud dello stesso giorno. **Da
verificare sul telefono**: non risulta che sia mai stata provata)*
- **[Nuova funzionalità]** Tenendo premuta l'intestazione di una
  colonna della tabella, il menu ha nell'ordine: **Move to left**,
  **Move to right**, **Edit property**, *Center / Align left* (solo per
  le colonne a caselle) e **Hide**. È la richiesta dell'utente, che
  nella sessione cloud l'ha ripetuta parola per parola (Diario,
  richiesta 20) perché l'aveva appena chiesta a Code sul PC
- **[Nuova funzionalità]** *(sessione cloud del 24/09/2026, richiesta
  22)* **Le due voci per spostare sono passate in cima al menu**, sopra
  Edit property. Nel codice arrivato dal PC stavano sotto, come diceva
  la prima richiesta; l'utente, non avendole mai viste sul telefono
  (l'app installata era più vecchia del codice), ha chiesto di metterle
  sopra. Spostare è la cosa per cui si tiene premuta un'intestazione —
  per modificarla basta toccarla — e in cima si trova senza leggere il
  menu. Cambiato solo l'ordine delle voci in `PropertyHeaderCell`
- **[Rimosso]** *(sessione cloud del 24/09/2026, richiesta 23)* **"Move
  left" e "Move right" dalla finestra della proprietà** (quella di Edit
  property, `PropertySheet`): lì resta solo "Delete property". Erano un
  doppione del menu dell'intestazione, e per di più comparivano una
  alla volta a seconda della colonna — sulla prima dopo Name c'era solo
  "Move right", ed è quella che l'utente ha chiesto di togliere; tolte
  tutte e due, perché "Move left" è la stessa cosa sulle altre colonne.
  Tolti anche i parametri `canMoveLeft`, `canMoveRight` e `onMove` di
  `PropertySheet`. **Conseguenza**: una colonna nascosta non si può più
  spostare senza rimostrarla (vedi Limiti noti). Il ramo di
  `DatabaseTableState.neighbourForMove` che muoveva le colonne nascoste
  fra tutte resta nel codice ma oggi nessuno lo raggiunge
- **[Nuova funzionalità]** **"Move to left" è grigia sulla prima
  colonna dopo Name**, qualunque essa sia: Name è il titolo delle
  pagine, non una proprietà, e resta sempre la prima. **"Move to
  right" è grigia sull'ultima**, e con **"Lock view"** lo sono tutte e
  due. Grigie e non nascoste: una voce che sparisce e ricompare farebbe
  cambiare posto alle altre sotto il dito (`PropertyHeaderCell`,
  `canMoveLeft = !viewLocked && index > 0`)
- **[Nuova funzionalità]** Si conta **fra le colonne che si vedono**
  (`DatabaseTableState.neighbourForMove`): scambiarsi con una colonna
  nascosta in mezzo non sposterebbe niente di visibile. Lo spostamento
  rinumera tutte le colonne in un salvataggio solo
  (`DatabaseViewModel.moveColumn` → `DatabaseRepository.updateColumns`,
  una transazione), così la tabella si ridisegna una volta. Con la
  tabella raggruppata l'intestazione si ripete sopra ogni gruppo, con
  lo stesso menu. Le regole sono le stesse di Move left / Move right
  nella finestra della proprietà

### 🆕 v0.64 — 24/09/2026 · 11:50 — Database — vista Gallery
*(verificato in parte sul telefono il
24/09/2026: voci 3-14; mancano copertine, rotazione, fluidità)*
*(sessione cloud del 24/09/2026:
**compilata** in cloud — `assembleDebug` riuscito, query controllate da
Room, migrazione simulata su SQLite, vedi Diario — **ma NON provata sul
telefono**)*
- **[Nuova funzionalità]** **Gallery**, la sesta vista: le pagine del
  database come **schede in griglia**. In alto l'anteprima, sotto il
  nome (con l'icona della pagina, se c'è) e le proprietà che si vedono
  e hanno un valore, una per riga come sulle schede della bacheca. Un
  tocco apre la pagina, il tocco prolungato le azioni della riga, come
  nelle altre viste. Filtro e ordinamento valgono anche qui. Si sceglie
  da Settings → Layout (sesto riquadro, icona a griglia) o si crea dal
  menu "/" con **"Gallery view"**, che finora c'era ma era spenta
- **[Nuova funzionalità]** **Card preview**, nelle impostazioni della
  vista, con le voci di Notion che qui si possono fare: **None**,
  **Page cover**, **Page content**. Salvata per ogni database
  (`PageEntity.galleryCardPreview`, **migrazione 24→25**). Finché non
  si sceglie vale **Page cover** (`GALLERY_DEFAULT_PREVIEW`): la
  galleria aspettava le copertine proprio perché è la vista che si
  riconosce dalle immagini. La quarta voce di Notion, l'anteprima da una
  proprietà "Files & media", manca perché quel tipo di proprietà non
  esiste nell'app
- **[Nuova funzionalità]** **Card size**: **Small / Medium / Large**,
  salvata per ogni database (`PageEntity.galleryCardSize`, stessa
  migrazione), media finché non si sceglie. Le schede non hanno una
  larghezza fissa ma **una minima** (100, 150 e 280 punti): in una riga
  ne entrano quante ci stanno e si allargano insieme a riempirla. Su un
  telefono in verticale sono tre, due o una per riga; girandolo ne
  entrano di più invece di diventare enormi (`galleryColumnsFor`)
- **[Nuova funzionalità]** Con **Page cover** la scheda mostra la
  copertina della pagina **con l'inquadratura scelta in
  "Reposition"**. La scheda ha un'altra forma della striscia della
  pagina, e la stessa inquadratura avrebbe potuto portare l'immagine
  oltre il bordo e scoprire un angolo vuoto: `CoverImage` (ora
  `internal`, per usarla da qui) ha un parametro nuovo, `clampOffset`,
  che ferma lo spostamento dove l'immagine smette di coprire. Nella
  pagina resta spento e lì non cambia niente
- **[Nuova funzionalità]** Con **Page content** la scheda mostra
  **l'inizio del testo della pagina**, in piccolo (8, 10 o 12 punti
  secondo la dimensione della scheda), scritto come nella pagina: i
  titoli più grandi e in grassetto, gli elenchi con **lo stesso segno
  della pagina** (`bulletMarkerFor` e `numberedMarkerFor`, ora
  `internal`), numerati contando come `numberedListOrdinal` — un
  contatore per livello, `numberStartsAt` rispettato — le caselle con
  ☐ e ☑ (quelle spuntate grigie e barrate), i toggle con ▸. Si leggono
  al massimo 12 blocchi di primo livello per pagina
  (`GALLERY_PREVIEW_MAX_LINES`), saltando quelli senza testo (tabelle,
  divisori, collegamenti) e le righe vuote in testa e in coda; quello
  che non ci sta lo taglia il bordo della scheda
- **[Nuova funzionalità]** **Gli spoiler restano coperti
  nell'anteprima**: ogni lettera coperta diventa `▒`, gli spazi
  restano. Senza, un testo nascosto nella pagina si sarebbe letto in
  chiaro nella scheda — che è proprio il colpo d'occhio da cui lo
  spoiler protegge
- **[Nuova funzionalità]** Le schede di una stessa riga sono **alte
  uguali** (`IntrinsicSize.Max`), e l'ultima riga, se non è piena,
  tiene le schede larghe come le altre. L'anteprima occupa il suo posto
  anche quando è vuota (pagina senza copertina o senza testo), così le
  schede restano allineate
- **[Nuova funzionalità]** "Card preview" e "Card size" compaiono nelle
  impostazioni **solo con la galleria**, subito sotto Filter come le
  voci delle altre viste, e **"Lock view" le spegne**. Scelta una voce
  la finestra resta aperta e il segno ✓ si sposta, come per "Date
  property" e "Group by": la galleria dietro cambia subito
- **[Nuova funzionalità]** Testi nuovi nelle otto lingue dell'app
  (`DbStrings.layoutName`, `galleryPreviewName`, `gallerySizeName`,
  `cardPreview`, `cardSize`). In italiano la dimensione è al femminile
  perché si parla della *scheda*
- **[Progetto]** Copertine e testo delle pagine si leggono **solo
  mentre la galleria li mostra** (`DatabaseViewModel.load`,
  `flatMapLatest` sulla pagina). Il testo soprattutto: la query guarda
  la tabella dei blocchi, che cambia a ogni tasto battuto in
  **qualunque** pagina, e il ViewModel di un database resta vivo anche
  quando è rimasto indietro nella pila di navigazione. Seguirla sempre
  avrebbe voluto dire rifare la query a ogni lettera scritta altrove.
  Due query nuove in `DatabaseDao`: `observeRowCovers` e
  `observeRowPreviewBlocks`; il taglio ai primi blocchi lo fa
  `DatabaseRepository.observeRowContentPreviews`, perché in SQL
  richiederebbe funzioni che su Android arrivano solo dalla versione 11
- **Scelte da far confermare all'utente**: la copertina come anteprima
  di partenza, le larghezze minime delle schede e il rapporto 1,6 : 1
  della parte alta (`GALLERY_PREVIEW_RATIO`)

### 🆕 v0.63 — 23/09/2026 · 15:41 — Duplicate: si sceglie dove va la copia, e la copia è davvero una copia
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di duplicare una pagina dal menu dei tre puntini. Metti anche un'opzione per decidere dove metterla, se crearla come pagina e duplicarla in fondo al menu principale oppure in una pagina a scelta (anche all'interno di ogni pagina di tutti i database dell'app)»

- **[Nuova funzionalità]** "Duplicate" nel menu dei tre puntini (pagine e
  database a schermo intero) apre una finestra che chiede **dove mettere
  la copia**, con tre scelte: **Next to the original** (subito sotto
  l'originale; per la pagina di una riga vuol dire **una riga nuova
  dello stesso database**, subito sotto, con tutte le sue proprietà),
  **End of the main menu** (in fondo alla pagina principale) e **Inside
  a page** (`DuplicateSheet.kt`)
- **Inside a page** apre l'albero della barra laterale: si tocca la
  pagina in cui metterla e la copia finisce in fondo. I database si
  aprono come cartelle e mostrano le loro righe, che si possono
  scegliere tutte, **anche quelle mai aperte** (la loro pagina nasce in
  quel momento, `DuplicateTarget.IntoRow`). I database stessi non si
  scelgono: dentro hanno righe, non blocchi. In alto c'è una ricerca per
  titolo, e sotto ogni risultato si legge dove sta la pagina ("In A",
  "In Untitled"): dopo un Duplicate i nomi uguali sono sempre due. Le
  pagine che non stanno da nessuna parte non compaiono, perché una copia
  messa lì dentro sparirebbe con loro
- Fatta la copia **si apre**, e se non è accanto all'originale un avviso
  dice dov'è finita ("Copy placed in “B”"). Il titolo prende " (copy)"
  nella lingua dell'app; le pagine dentro di lei tengono il loro nome
- **[Bug fix]** La copia **non era una copia**. Puntava alle stesse
  sottopagine e, per un database, alle stesse pagine delle righe:
  scrivere in una sottopagina della copia cambiava anche l'originale,
  rinominare la pagina di una riga rinominava la riga in tutti e due i
  database, buttarne una la toglieva da entrambi. Ora si copia **tutto,
  giù fino in fondo**, come su Notion (`PageRepository.copyPageDeep`);
  una pagina richiamata due volte si copia una volta sola, e un
  collegamento all'indietro non fa girare la copia all'infinito
- **[Bug fix]** Duplicare **la pagina di una riga** creava una pagina che
  non stava da nessuna parte: non c'era un collegamento da mettere
  accanto, e la copia si trovava solo cercandola. Nel database del
  telefono ce n'è una rimasta così, **"A (copia)"**, di una prova di
  prima. Ora accanto all'originale diventa una riga nuova, e altrove una
  pagina normale
- **[Bug fix]** Icona e copertina della copia **condividevano il file**
  con l'originale: cambiando l'immagine di una, o cancellandola per
  sempre, il file spariva e l'altra restava senza. Ora ogni copia ha il
  suo file (`PageImageStore.copy`)
- **[Bug fix]** La copia perdeva il testo delle **tabelle semplici**
  (vive in una tabella a parte, per blocco) e, in un database, puntava
  ancora alle colonne dell'originale per raggruppamento, calendario,
  ordine e filtro, che quindi sparivano. I figli dei toggle si salvano
  dopo il loro toggle, come chiede il vincolo sul genitore
- **[Bug fix]** Il testo scritto in una **tabella semplice spariva
  riaprendo la pagina** (nel database c'era ancora): ogni casella teneva
  il suo testo in uno stato nato col primo valore visto, e al primo
  disegno le celle non erano ancora arrivate. Ora le caselle rinascono
  una volta quando arrivano (`TableBlockContent`)
- **Provato sul telefono** con una pagina di prova (toggle con due figli,
  tabella 2×2, sottopagina con copertina, icona): copia della riga A
  accanto all'originale, copia dentro la riga B mai aperta scelta dalla
  ricerca, copia del database intero in fondo al menu principale. Nel
  database: nessuna pagina richiamata da due posti, nessun file immagine
  condiviso, figli dei toggle nella pagina giusta, celle copiate,
  `PRAGMA foreign_key_check` vuoto, nessun crash. Il testo nella tabella
  resta dopo essere usciti e rientrati. Poi il database del telefono è
  tornato com'era prima delle prove

### 🆕 v0.62 — 23/09/2026 · 15:06 — Icone per le pagine dei database
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di aggiungere e rimuovere icone alle pagine contenute in un database»

- **[Nuova funzionalità]** Le pagine contenute in un database (le righe)
  hanno la loro icona, e **si vede nel database accanto al nome**: nella
  tabella, nell'elenco, nelle schede della bacheca, nelle barre del
  calendario e della linea temporale, e fra le righe senza data. Una riga
  senza icona parte dal margine, senza segnaposto
- **Si mette e si toglie in due modi**: tenendo premuto sul nome della riga,
  dalla voce **Icon** in cima alle azioni (dice "Change" o "None"), oppure
  dentro la pagina, toccando l'icona. È la stessa finestra di pagine e
  database: Upload, Link, Remove. Se la riga non era mai stata aperta come
  pagina, la pagina nasce in quel momento: l'icona sta nella pagina. Con il
  database bloccato o nel cestino la voce è spenta
- **La pagina di una riga nasce senza icona**, come su Notion: al posto del
  foglietto 📄 fisso c'è **"Add icon"** accanto a "Add cover". Prima
  "Remove" toglieva l'immagine ma lasciava il 📄, quindi un'icona tolta non
  si poteva davvero togliere. Le altre pagine tengono la loro emoji
- Le icone arrivano da una query che segue insieme righe e pagine
  (`DatabaseDao.observeRowIcons`): cambiata dentro la pagina, l'icona si
  aggiorna nel database senza riaprirlo. Il salvataggio scrive solo la
  colonna dell'icona (`PageDao.setIconImage`), e l'immagine di prima viene
  tolta dalla cartella
- **Provato sul telefono** sulla riga "A" della tabella, nei due sensi:
  messa dal menu della riga e tolta dentro la pagina, poi messa da "Add
  icon" e tolta dal menu della riga. Ogni volta l'icona compare e sparisce
  sia nella tabella sia in testa alla pagina, il menu passa da "None" a
  "Change", il file dell'immagine nasce in `page-images` e viene cancellato
  al Remove, e nel database `iconImage` torna vuoto. Nessun crash. Le altre
  viste (bacheca, elenco, calendario, linea temporale) usano lo stesso
  pezzo ma non le ho aperte

### 🆕 v0.61 — 23/09/2026 · 14:14 — La barra laterale: preferiti, albero, ricerca, cestino, avvio e impostazioni
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Preparati perché questo è bello grosso.
> Creami una Sidebar che si apre toccando un'icona che creerai di tre lineette orizzontali in alto a sinistra dove posso trovare (se scrivo qualche voce in italiano, traducila in inglese come visualizzazione nella Sidebar:
>
> 1. Widget: creami il titolo con un pochino di spazio, poi la implementerai in futuro;
> 2. Notebook: poi ti dirò in cosa consiste, per il momento mettila grigia. Mettila separata dalle altre voci con un background colorato diverso dal colore classico della sidebar, che anch'essa sarà un pochino più scura rispetto a tutte le pagine del Workspace;
> 3. Main menu: (con un'icona di una casetta) per andare al menu principale (qui sotto mettici un divider semplice);
> 4. Favorite pages: una pagina che si apre che fa vedere una lista di tutte le pagine preferite (che hai già implementato) a visualizzazione una sotto l'altra, in ordine di aggiunta come preferita con anche data e ora di aggiunta ai preferiti (dammi la possibilità di ordinare le pagine preferite alfabeticamente oppure in data di aggiunta ai preferiti, entrambe crescente o decrescente)
> 5. visualizzazione veloce gerarchica delle pagine, sia come fa Notion sia come fanno gli explorer di Windows e Mac, così potrò navigare velocemente per le pagine delle sottopagine di ogni database, ed ovviamente quando premo una pagina vado direttamente lì, saltando tutti i passaggi per arrivarci attraverso tutte le sopra-pagine ad essa. Coma pagina iniziale mettimi ovviamente la pagina del menu principale (ovviamente qui dovranno essere viste anche tutte le icone di appartenenza), con un triangolino ad ogni pagina come vedi nella foto.
> 6. Search: tasto di ricerca globale in tutta l'app con queste voci (rendi attive e lavora sopra la voce 1 ed 6, le altre falle grigie, per il momento non mi servono):
>    1. Cerca nel contenuto
>    2. Cerca nei titoli
>    3. Cerca nei tag
>    4. Cerca negli allegati
>    5. Cerca nelle note archiviate
>    6. Cerca nel cestino
>    7. Case sensitive
>    8. Cerca parole intere
>    9. Indicizzazione automatica
>    10. Ricerca OCR nelle immagini/PDF
> 7. Startup window: mi consente di scegliere come prima pagina da vedere appena apro l'app se è un'ultima pagina visitata, la home, oppure una pagina specifica;
> 8. Backup (questa falla grigia)
> 9. Notifications (poi la implementerai meglio in futuro, per ora falla grigia)
> 10. Trash, dove qui troverò una lista delle pagine cancellate tramite l'opzione Move to trash. Dammi la possibilità di aprirle e in alto ci sarà una barra orizzontale con due opzioni: ripristina (che sarà messa in fondo al menu principale), oppure cancella definitivamente, con conservazione delle pagine eliminate per 30 giorni, oppure svuota automaticamente il cestino;
> 11. Import, che mi consente di inserire a fine pagina principale tutto quello che importo, che può essere contenuto preso da un .csv, da un .pdf, da un Text & Markdown, da un .html, da un file Word, da un file Excel. Per il momento metti la scritta in grigio, poi questa la completerai quando ti chiederò in futuro di farmi l'app Windows e Mac, ora è troppo presto per implementare tutto
> 12. Export: uguale ad import, falla grigia
> 13. Connections: una pagina che mi farà vedere su quale dispositivo questo "account" è collegato, ma comunque uguale ad Import, metti la scritta in grigio
> 14. Settings:
>    1. Font: mettilo grigio
>    2. Font size: mettilo grigio
>    3. Theme (Dark, Light o System)
>    4. App sounds: la possibilità di selezionare manualmente tramite l'explorer del telefono una suoneria come suono di ogni notifica, opzione per disattivare le notifiche per sempre, e un'opzione per fare la stessa cosa per un periodo di tempo
>    5. Animations: Attive / ridotte / disattivate
>    6. Number format (1.000.000 o 1,000,000)
>    7. Date format (Full date (September 20, 2026), Short date (Sep 20), Month.Day.Year (09.20.2026), Month/Day/Year (09/20/2026), Day.Month.Year (20.09.2026), Day/Month/Year (20/09/2026), Year.Month.Day (2026.09.20), Year/Month/Day (2026/09/20), Chinese date (2026年09月20日)
>    8. Language (con Inglese, Italiano, Tedesco, Francese, Spagnolo, Cinese, Coreano, Giapponese, che mi traduce solo le voci dell'app standard, quindi per esempio le traduzioni delle opzioni quando faccio / o quelle in +, le voci dei databse quindi la traduzione di Name, Date, Number, eccetera, non quello che io scrivo o creo manualmente)
>    9. Time zone (quello principale sarà Berlino, che rispetta ovviamente ora solare e legale)»

Poi:
> «Ah una cosa: come hai fatto con l'icona delle impostazioni della pagina, l'icona della sidebar falla uguale a quella, ossia anche sopra la copertina, con un pallino in background e che faccia notare le tre lineette»

- **[Nuova funzionalità]** **Barra laterale** che scorre da sinistra sopra la
  pagina, aperta dal pulsante con le tre lineette: stesso cerchio grigio
  semitrasparente dei tre puntini, sospeso sopra la copertina, e accanto
  a indietro nelle sottopagine (`FloatingPageButton`). Nel database a
  schermo intero sta nella barra in alto, accanto a indietro. Si chiude
  toccando fuori, col gesto indietro o trascinandola; **non si apre
  trascinando dal bordo** perché da lì parte il gesto indietro del
  telefono. È un filo più scura delle pagine (`SidebarBackground`)
- **L'ordine delle voci è quello chiesto**: Widgets (titolo e spazio, da
  riempire — riempito il 24/09/2026, vedi "Widget nella barra laterale"), Notebook grigio in una fascia di un altro colore
  (`NotebookBand`), Main menu con la casetta e sotto un divisore, Favorite
  pages, l'albero delle pagine, Search, Startup window, Backup e
  Notifications grigie, Trash, Import/Export/Connections grigie, Settings.
  Tutte le voci della barra sono tradotte (vedi sotto)
- **[Nuova funzionalità]** **L'albero delle pagine**, come Notion ed Esplora
  file: parte dal menu principale (aperto), ogni pagina ha la sua icona e
  il triangolino **solo se dentro c'è qualcosa**. Toccando il triangolino si
  apre e chiude; toccando il nome si va **dritti alla pagina**, senza
  passare da quelle che la contengono, e sotto resta solo il menu
  principale, così "indietro" riporta lì. La pagina aperta è evidenziata
- L'albero **non sta in una colonna "genitore"**: vive nei collegamenti
  dentro le pagine, nell'ordine del testo (anche dentro i toggle), e per i
  database nelle righe. Le righe mai aperte come pagina ci sono lo stesso:
  la loro pagina nasce al primo tocco, come col pulsante OPEN. Si carica
  un ramo alla volta e si ricarica a ogni apertura della barra. Un
  collegamento all'indietro (una pagina che richiama una delle sue
  antenate) si mostra ma non si riapre, altrimenti la fila non finirebbe
- **[Nuova funzionalità]** **Pagine preferite**: l'elenco, una sotto
  l'altra, con data e ora di aggiunta. Si ordina per data di aggiunta o per
  nome, crescente o decrescente, e la scelta resta salvata. Nuova colonna
  `favoritedAt` (**migrazione 23→24**); i preferiti che c'erano già non
  hanno una data vera e lo dicono ("Date added unknown") invece di
  inventarla. Le pagine nel cestino non compaiono fra i preferiti
- **[Nuova funzionalità]** **Search** rifatta: cerca nei titoli e, con
  "Search in content" (acceso di partenza), nel testo dei blocchi, nelle
  tabelle semplici e nei nomi delle righe dei database. Ogni risultato
  mostra **il pezzo di frase con la parola evidenziata**. "Search in trash"
  include le pagine buttate, segnate "In trash". Le altre otto opzioni ci
  sono, grigie; "Search in titles" è grigia **ma accesa**, perché nei
  titoli si cerca sempre
- **[Nuova funzionalità]** **Trash**: le pagine buttate, con data e giorni
  che restano. Aperta, una pagina del cestino è **in sola lettura** e ha in
  cima una barra con **Restore** — torna **in fondo al menu principale**, o
  alla sua riga se era la pagina di una riga — e **Delete permanently**,
  con conferma. In alto a destra "Empty trash". Due regole: **tenere le
  pagine 30 giorni**, oppure **svuotare automaticamente**, cioè cancellarle
  al prossimo avvio dell'app. La pulizia avviene all'avvio
- La cancellazione definitiva porta via anche **le pagine contenute** (i
  collegamenti dentro, le righe se è un database, e così via in giù), ma
  **solo quelle richiamate soltanto da lì**: una sottopagina collegata
  anche da un'altra parte resta. Con le pagine se ne vanno i file di icone
  e copertine
- La conferma di "Move to trash" ora dice che la pagina va nel cestino e
  **quando se ne andrà** secondo la regola scelta (prima diceva che un
  cestino non c'era)
- **[Nuova funzionalità]** **Startup window**: l'app parte dall'ultima pagina
  aperta, dal menu principale o da una pagina scelta da un elenco. Se
  quella pagina non c'è più o è nel cestino si parte dal menu principale.
  Una rotazione o un cambio di tema non rifanno il salto
- **[Nuova funzionalità]** **Settings**, salvate sul telefono
  (`AppSettings`, `SharedPreferences`, non nel database: sono scelte del
  telefono, non contenuto):
  - **Font** e **Font size** grigie
  - **Theme**: Dark, Light, System. I grigi scritti a mano in circa 150
    punti ora **scelgono da soli la versione chiara o scura**
    (`Color.kt`): i nomi sono rimasti quelli di prima, e in chiaro
    `NotionWhite` è il colore del testo. Le icone della barra del telefono
    diventano scure sul tema chiaro
  - **App sounds**: suono delle notifiche scelto **con l'esplora file del
    telefono** (il permesso di leggerlo resta anche dopo un riavvio), o
    quello predefinito, con un tasto per ascoltarlo; notifiche spente per
    sempre, o per 1 ora / 8 ore / 1 giorno / 1 settimana. **Le notifiche
    non esistono ancora**: le scelte si salvano e varranno quando ci
    saranno, e la pagina lo dice
  - **Animations**: On, Reduced, Off. Valgono per l'apertura della barra
    laterale e per le dissolvenze fra una schermata e l'altra (piene 700
    ms come prima, ridotte 120, nessuna)
  - **Number format**: 1.000.000 o 1,000,000. Le celle numeriche lo
    mostrano quando non ci si scrive; **mentre si scrive il numero resta
    grezzo**, altrimenti i separatori si sposterebbero sotto le dita. Il
    valore salvato non cambia mai
  - **Date format**: i nove formati chiesti. "Full date" la scrive ogni
    lingua a modo suo ("September 23, 2026", "23 settembre 2026"). Vale
    per le celle dei database, la cronologia, preferiti e cestino
  - **Language**: English, Italiano, Deutsch, Français, Español, 中文, 한국어,
    日本語, ognuna scritta nella sua lingua. Traduce **le voci dell'app**:
    barra laterale, menu "/" e "+", barra degli strumenti, menu ⋯,
    database (tipi di proprietà, viste, filtri, ordinamento, tag, date che
    si ripetono, impostazioni), selettore colori, cronologia, finestre di
    conferma. Nel menu "/" si cerca sia col nome tradotto sia con quello
    inglese
  - **Time zone**: Berlino di partenza, che segue l'ora legale; si cerca
    fra tutti i fusi "Regione/Città", con lo scarto da UTC di adesso
- **Le traduzioni** stanno in `ui/i18n/` (`Strings`, `EditorStrings`,
  `DbStrings`): **ogni voce con le sue otto traduzioni nella stessa riga**,
  così una voce nuova si scrive una volta sola e una lingua dimenticata
  non compila. **I nomi delle proprietà**: una proprietà lasciata senza
  nome prende quello inglese del tipo ("Number", "Date") e **si mostra
  tradotto**; un nome scritto dall'utente resta com'è. Le proprietà nuove
  senza nome continuano a salvare il nome inglese, così seguono sempre la
  lingua
- **Limiti dichiarati**: le parole degli elenchi numerati "a parole"
  ("One", "First") restano inglesi, sono uno stile di numerazione; gli
  errori dello scaricamento delle immagini ora sono generici (prima erano
  in italiano e dettagliati); le **date senza ora** sono salvate alla
  mezzanotte del fuso in cui sono nate, quindi scegliendo un fuso molto a
  ovest di Berlino quelle vecchie potrebbero comparire un giorno prima;
  le animazioni dentro le pagine (scorrimenti, finestre) non seguono
  ancora l'impostazione; le pagine che **nessun collegamento richiama**
  non compaiono nell'albero (sul telefono ce ne sono due, un "Esempio" e
  un "Untitled": si ritrovano con la ricerca)
- **Verificato sul telefono**: barra laterale aperta e chiusa; albero con
  le righe del database, tocco su una riga → pagina aperta direttamente;
  preferito aggiunto → in elenco con data e ora; ricerca → pagina trovata
  per contenuto con la parola evidenziata, pannello delle opzioni; pagina
  buttata → nel cestino con "30 days left", aperta con la barra, Restore;
  italiano → impostazioni, barra laterale, menu "/" e intestazioni del
  database tradotti; tema chiaro su pagina, barra laterale e impostazioni;
  numeri col separatore delle migliaia; "Last visited page" → riavviata
  l'app, riparte dal database aperto per ultimo. Nessun crash. Prove
  ripulite e impostazioni rimesse come le aveva l'utente

### 🆕 v0.60 — 23/09/2026 · 09:28 — Il menu "/" filtra davvero
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Se scrivo /d , non mi appare nella selezione del comando divider»

- **[Bug fix]** **Scrivendo "/d" il menu spariva alla "d".** Il menu "/"
  si chiudeva a ogni tocco fuori da sé, e per Android anche **la
  tastiera è "fuori"**: è un'altra finestra. Quindi la prima lettera
  battuta dopo la barra lo chiudeva, e il filtro non si è mai visto
  lavorare. Nelle prove era sfuggito perché barra e lettere arrivavano da
  `adb input text`, che non tocca lo schermo; battendo la "d" sul tasto
  vero il menu è sparito subito. Tolta la chiusura al tocco fuori
  (`dismissOnClickOutside = false`), come per il menu del "+": si chiude
  lo stesso scegliendo una voce, togliendo la barra, portando il cursore
  prima di lei o su un'altra riga, con uno spazio, o perdendo il fuoco
- **[Nuova funzionalità]** **Con un filtro, prima le voci che somigliano
  di più.** Il filtro teneva ogni voce che *contenesse* le lettere,
  nell'ordine del catalogo: con "/d" erano sedici, e "Divider" veniva
  settima, sotto il bordo del riquadro. Ora vince chi **comincia** così,
  poi chi ha una **parola** che comincia così ("/list" trova "Bulleted
  list", "/do" trova "To-do list"), e solo dopo chi le contiene; a parità,
  le voci che funzionano davanti a quelle ancora spente. Con un filtro le
  famiglie (Basic blocks, Media, Database) non si separano più, altrimenti
  con "/data" le voci di base passerebbero comunque davanti. A ogni
  lettera la lista ricomincia dall'alto
- **[Bug fix]** **Scegliendo Divider la pagina scivolava fino al database
  e la tastiera accennava a chiudersi.** Ogni gruppo di righe di testo è
  una casella sola, riconosciuta dalla sua prima riga: se la prima riga
  diventa un divisore, la casella cambia identità e viene distrutta col
  cursore dentro. Per un attimo il fuoco non era di nessuno — nel video:
  barra sparita, tastiera che scende e risale, pagina che scorre. Adesso,
  come già per il toggle, il fuoco si posa prima sul campo invisibile e
  da lì passa alla riga nuova sotto il divisore. Vale anche dal "+"
- **[Bug fix]** **La barra resta mentre il cursore cambia riga.** Nei
  passaggi dal campo invisibile (divisore, toggle, casella che torna
  testo) nessuna riga ha il fuoco per un centinaio di millisecondi, e la
  barra spariva e tornava. Ora, perso il fuoco a tastiera aperta, aspetta
  tre decimi di secondo prima di andarsene (`BAR_HANDOFF_GRACE_MS`); a
  tastiera chiusa se ne va subito come prima. Il prezzo: passando al
  titolo della pagina la barra resta un attimo in più
- **[Bug fix]** **Il divisore si portava dietro il comando.** Veniva
  salvato per intero dalla copia della riga che aveva in mano la
  schermata, che aveva ancora dentro "/d": invisibile, ma nel database.
  Stesso difetto della "/" nei toggle, stesso rimedio: si scrive solo il
  tipo (`turnIntoDividerWithLineBelow`)
- **Verificato sul telefono** battendo sui tasti veri: "/" poi "d" → il
  menu resta aperto, **Divider in cima**; con la "D" maiuscola che la
  tastiera mette da sola a inizio riga, uguale. Toccato Divider su una
  riga in fondo allo schermo, registrando: barra e tastiera ferme in tutti
  i 36 fotogrammi, cursore sulla riga nuova, nessuna chiusura della
  tastiera nel registro, divisore **senza testo** nel database. Dopo le
  prove la pagina è stata rimessa com'era dalla copia fatta prima
- **Da decidere, non toccato**: scegliendo Divider su una riga che ha già
  del testo, **tutta la riga** diventa divisore e il testo resta nascosto
  dentro. Su Notion il divisore va invece sotto e il testo resta

### 🔧 v0.59.1 — 23/09/2026 · 09:12 — Gli scatti dentro i toggle
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Le lettere sotto i toggle e le stesse create scattano, scompaiono e ricompaiono. Quando cancello qualcosa di un blocco dentro un toggle succede che, riusando backspace e quindi andando sopra, la toolbar sopra la tastiera scatta di un po' di più rispetto ai soliti scattini. Faccio anche caso che il text cursor e la lettera appena passata scattano quando vado poi a capo sotto»

- **[Bug fix]** **Andando a capo dentro un toggle la riga appena scritta
  spariva per un attimo e il cursore scendeva di colpo.** Le righe dentro
  un toggle sono campi separati, e l'Invio mette la riga nuova **sopra**
  col testo prima del cursore (è quello che tiene ferma la tastiera):
  il campo col cursore passava subito al testo dopo il cursore, ma la
  riga di sopra arrivava solo quando il database l'aveva scritta e
  rimandata indietro. Misurato: 29 ms, e nel video dello schermo c'è un
  fotogramma in cui la lettera appena battuta **non c'è più da nessuna
  parte**, e in quello dopo ricompare una riga più su
- Ora la riga nuova entra nella lista in memoria **nello stesso istante**
  in cui il campo cambia testo (`showBeforeSaved`), e il database la
  conferma dopo. Rifatto il video: da "CB" a "C / B" in un fotogramma,
  senza passaggi intermedi
- **Non basta aggiungerla una volta**: una lettura del database partita
  un attimo prima dell'Invio può arrivare dopo, senza la riga, e
  toglierla di nuovo — e se quella riga ha già il cursore, togliere la
  sua casella di scrittura fa chiudere l'app. La modifica resta quindi
  in sospeso e **si riapplica a ogni lettura che non contiene ancora la
  riga nuova**; la prima che la contiene la chiude. Se il database
  rinuncia a scriverla (il blocco nel frattempo non c'è più) la riga
  viene tolta, e annulla/ripristina buttano via quelle in sospeso
- Stesso trattamento per l'Invio nel **titolo** del toggle e per il
  segnaposto *Empty toggle*: la riga nuova si vede subito. Il cursore ci
  arriva invece **come prima**, dopo che il database l'ha scritta:
  mandarcelo prima vorrebbe dire scrivere lettere in una riga che nel
  database ancora non esiste, e perderle
- **[Bug fix]** **Salendo col backspace da una riga vuota la barra sopra
  la tastiera spariva e tornava.** Il fuoco passava dal campo invisibile,
  la riga veniva cancellata e 60 ms dopo il cursore andava su quella di
  sopra: per tutto quel tempo nessuna riga aveva il fuoco, e la barra si
  vede solo se ce n'è una. Nel video: **quattro fotogrammi, cento
  millisecondi, senza barra**, con la tastiera sempre aperta
- Ora il cursore passa **direttamente** da questa riga a quella di sopra
  (nel registro: uno perde il fuoco e l'altro lo prende nello stesso
  millisecondo), e la riga si cancella **dopo**, quando non ha più il
  cursore. Il cursore va in fondo alla riga di sopra: è messo prima di
  prendere il fuoco, altrimenti lo si vedrebbe saltare
- La tastiera Samsung manda lo stesso backspace **due o tre volte** a due
  millisecondi di distanza (con la tastiera vera, non solo con `adb`):
  la riga in uscita si ricorda, e i doppioni non la tolgono una seconda
  volta
- **[Bug fix] mio, preso prima di consegnare**: nel primo giro la riga
  vuota **non veniva mai cancellata**. `withTimeoutOrNull` restituisce
  `null` sia quando scade il tempo sia quando il blocco restituisce
  `null` — ed è proprio quello che diventa la richiesta di fuoco appena
  servita. Visto rileggendo il database dopo la prova, non dallo
  schermo, dove il cursore saliva correttamente
- **Verificato sul telefono**, registrando lo schermo e guardando i
  fotogrammi uno per uno: Invio in mezzo a una riga dentro il toggle →
  nessun fotogramma con la riga mancante; backspace su una riga vuota →
  barra presente in tutti i fotogrammi, riga sparita dal database,
  lettera battuta dopo finita in fondo alla riga di sopra; Invio nel
  titolo → riga nuova visibile subito, backspace → riga tolta, cursore
  in fondo al titolo. Dopo le prove la pagina è stata rimessa com'era
  dalla copia del database fatta prima

### 🆕 v0.59 — 23/09/2026 · 00:49 — Icone anche sui database
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di aggiungere e rimuovere icone ai database, essendo ora delle pagine convertibili»

- **[Nuova funzionalità]** Un database può avere la sua icona, prima del
  nome: più piccola quando sta dentro una pagina, grande quanto quella
  delle pagine quando è aperto a schermo intero. Per metterla c'è la
  voce **Icon** nelle impostazioni del database; quando c'è, si cambia
  o si toglie anche toccandola
- **Stessa finestra delle pagine** (Upload / Link / Remove), non una
  copia: `PageImageSheet` e `PageImage` sono stati resi condivisibili
  fra le due schermate. Togliendo o sostituendo l'icona, il file vecchio
  viene cancellato come per le pagine
- **"Nessuna icona" vuol dire proprio niente accanto al nome.** Le
  pagine hanno un'emoji di riserva (📄) scritta nel campo `icon`; per un
  database quella non si usa, altrimenti ogni database avrebbe avuto un
  foglietto davanti senza che nessuno l'avesse chiesto. Conta solo
  `iconImage`, e senza icona il nome parte dal margine come prima
- Il **collegamento** a un database trasformato in pagina mostra la sua
  icona, se c'è; se non c'è, il segno della tabella invece del 📄
- Con la pagina bloccata ("Lock page") l'icona non si cambia: fa parte
  del contenuto
- **Verificato sul telefono** con una copia di un'immagine già presente
  (per non toccare la copertina): icona visibile accanto al nome,
  toccata → la finestra offre anche Remove, Remove → icona sparita e
  **file della copia cancellato**, copertina intatta

### 🆕 v0.58 — 23/09/2026 · 00:37 — La copertina sale fin sotto la barra del telefono
*(Claude Code sul PC)*

L'utente, parola per parola:
> «C'è quello spazio inutile tra la copertina e la barra del telefono dove ci sono ora, icona del WiFi e batteria. Sposta tutto fino a sotto il pixel dove finisce la barra. I tre puntini mettimeli tranquillamente sopra la copertina, magari con un cerchietto con colore background grigio semi chiaro. Poi ti dico se mi piace»

- **[Nuova funzionalità]** Tolta la barra in alto delle pagine: occupava
  una fascia vuota fra l'orologio e la copertina solo per ospitare due
  icone. Ora la copertina comincia **subito sotto l'ultimo pixel della
  barra del telefono**, e indietro e tre puntini stanno **sospesi sopra**
  in un cerchietto grigio semitrasparente (`FloatingPageButton`,
  `FloatingButtonBackground`)
- **Semitrasparente e non pieno**, di proposito: pieno sarebbe una toppa
  sulla copertina, del tutto trasparente sparirebbe sulle copertine
  chiare. Così si legge su qualunque immagine e la lascia intravedere
- **Senza copertina** in cima resta la fascia dei pulsanti
  (`FLOATING_BAR_CLEARANCE`), altrimenti il pulsante indietro finirebbe
  sopra l'icona della pagina e su "Add cover"
- Ognuno dei due pulsanti è grande **solo quanto il suo cerchio**:
  intorno la pagina si tocca come prima
- Il database aperto a schermo intero ha ancora la barra classica: lì
  non c'è una copertina da far salire

### 🆕 v0.57 — 23/09/2026 · 00:29 — Turn into page / Turn into database
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Mettimi nelle impostazioni dei database un'opzione chiamata 'Turn into page' che mi consente di convertire quel database in una pagina, come se la potessi visualizzare direttamente in full page. Per rimetterla in visualizzazione classica come Database, mi metterai un'opzione nelle impostazioni della pagina con l'icona dei tre puntini chiamata 'Turn into database' che non solo mi farà ritornare alla schermata di prima, ma anche che me la rifarà vedere come prima in un database aperto.»

- **[Nuova funzionalità]** Nelle impostazioni di un database dentro una
  pagina c'è **Turn into page**: il database smette di essere mostrato
  lì e diventa un collegamento (📄 col suo nome) che lo apre a schermo
  intero. Toccata la voce, il database **si apre subito** a schermo
  intero, che è come lo si vedrà da lì in poi
- **[Nuova funzionalità]** Nel menu dei tre puntini del database a
  schermo intero c'è **Turn into database**: torna a vedersi dentro la
  pagina che lo richiama, e si **torna indietro** su quella pagina, dove
  lo si ritrova aperto come prima
- **Il database non viene toccato**, in nessuno dei due versi: righe,
  colonne, vista, ordinamento e filtro restano identici. Cambia solo
  **il blocco che lo mostra** nella pagina — `DATABASE_LINK` diventa
  `PAGE_LINK` e viceversa — scritto con una query sola sulla colonna del
  tipo, senza riscrivere nient'altro
- **Turn into database compare solo quando serve**: se il database è
  richiamato come pagina da qualche parte. Aperto a schermo intero
  dall'icona ↗ è ancora dentro la sua pagina, e lì la voce non c'è.
  Si ricontrolla a ogni apertura del menu, perché nel frattempo può
  essere cambiato dall'altra schermata
- **[Bug fix]** Un collegamento a pagina che punta a un database lo
  apre **come database**. Prima contava solo il tipo del blocco, e un
  collegamento a pagina apriva sempre l'editor di testo: su un database
  avrebbe mostrato un foglio vuoto al posto delle righe
- **Verificato sul telefono** sul database della pagina principale:
  Turn into page → aperto a schermo intero; indietro → nella pagina è
  un collegamento; toccato → si apre come database; Turn into database
  → di nuovo nella pagina principale, aperto, con le stesse righe; e
  aperto dall'icona ↗ la voce non compare

### 🆕 v0.56 — 23/09/2026 · 00:18 — I toggle, rifatti
*(Claude Code sul PC)*

L'utente, parola per parola:
> «La toggle è parecchio buggata: se la apro mi viene fuori con una freccetta verso l'alto e con un / scritto. La freccetta dei toggle deve essere verso destra se il toggle è chiuso, e verso il basso se è aperto (come vedi nelle foto. Adesso non posso neanche scriver dentro perché è tutto ultra buggato»

Poi:
> «Non posso cancellare un toggle»

- **[Bug fix]** **Aprire o chiudere un toggle cancellava tutto quello
  che conteneva.** Il salvataggio dei blocchi usava
  `@Insert(onConflict = REPLACE)`, e in SQLite `REPLACE` non aggiorna:
  **cancella la riga e ne inserisce una nuova**. La cancellazione fa
  scattare il vincolo a cascata verso i figli (`parentBlockId`), quindi
  ogni salvataggio di un blocco con dei figli li distruggeva. Aprire o
  chiudere un toggle lo salva; anche spostarlo su o giù e cambiargli
  tipo. Visto nel database: cinque righe dentro un toggle, un tocco
  sulla freccia, zero righe. Ora è `@Upsert`, che inserisce se il blocco
  non c'è e **aggiorna al suo posto** se c'è: la riga non viene mai
  cancellata e i figli restano. Le altre `REPLACE` del progetto sono
  state controllate: creano sempre righe con id nuovi, quindi non
  toccano mai una riga esistente
- **[Bug fix]** **La "/" che restava dentro ogni toggle creato dal menu
  "/".** Si scrive `/toggle`, il campo toglie comando e barra e salva;
  subito dopo il cambio di tipo salvava **il blocco intero** con la
  copia che la UI aveva in mano prima — e riscriveva il testo vecchio,
  barra compresa. Ora `updateBlockType` scrive **solo la colonna del
  tipo**, come già facevano le altre modifiche di un campo solo
- **[Bug fix]** **La freccia**: da chiuso un triangolino verso destra,
  da aperto verso il basso, come su Notion. Prima erano le frecce a
  parentesi di Material, e da aperto puntava in su
- **[Bug fix]** **In un toggle nuovo non si poteva scrivere.** Aperto e
  vuoto non mostrava niente, e il pulsante "+ Add inside" compariva
  solo se c'era già almeno un figlio. Ora compare la riga grigia
  *Empty toggle. Tap to add a block inside.*; con dei figli non c'è più
  nessun pulsante in più, si aggiunge con l'Invio come su Notion
- **[Bug fix]** **L'Invio nel titolo faceva uscire il titolo dal
  toggle.** Veniva trattato come un paragrafo: la riga nuova nasceva
  sopra col testo prima del cursore — cioè col titolo — e il toggle
  restava vuoto sotto. Ora (`enterInToggle`) a toggle **aperto** la riga
  nuova è il **primo figlio**; a toggle **chiuso** è un **altro toggle**
  subito sotto, perché dentro non si vedrebbe
- **[Bug fix]** **Dopo un Invio dentro il toggle le lettere finivano
  nella riga sbagliata**: "1", Invio, "2" dava "12" nella riga di
  sopra. Le righe dentro un toggle stavano in una colonna **senza
  `key`**, quindi Compose le riconosceva dalla posizione: la riga nuova
  nasce sopra (è quello che tiene ferma la tastiera, vedi
  `splitBlockAt`), la riga col cursore scendeva di un posto ma il suo
  campo restava dov'era e si ritrovava a mostrare la riga nuova. Trovato
  col registro `NOTE`, che mostrava la lettera arrivare a un campo che
  non aveva il fuoco. Il testo di primo livello non aveva il problema
  perché la lista che lo contiene le chiavi le ha già
- **[Nuova funzionalità]** **Backspace su una riga vuota dentro un
  toggle la toglie** e porta il cursore su quella sopra, o sul titolo
  se era la prima (`removeEmptyNestedLine`). Prima non succedeva niente
  e una riga vuota creata per sbaglio restava lì
- **[Nuova funzionalità]** Creato un toggle dal menu "/" o dal "+", **il
  cursore va sul suo titolo** e la tastiera resta su: prima si chiudeva
  e bisognava ritoccare la riga. Il fuoco si posa per un istante sul
  campo invisibile, perché il campo condiviso in cui si stava scrivendo
  — se quella era la sua unica riga — muore col cursore dentro
- **[Bug fix]** Le righe dentro un toggle sono **allineate sotto il testo
  del titolo** (`NESTED_INDENT` = freccia + stacco). Prima il rientro
  era un 20 fisso, quattro punti più corto, e stavano storte a sinistra
- **[Nuova funzionalità]** **Un toggle si toglie col backspace**, come
  su Notion: all'inizio del titolo **torna testo normale**, col suo
  titolo, e le righe che conteneva **escono e restano subito sotto**,
  allo stesso livello (`unmakeToggle`, `unnestAndConvert`). Un secondo
  backspace lo unisce alla riga sopra. Prima non succedeva niente, e un
  toggle col titolo scritto non si poteva togliere in nessun modo: il
  cestino della barra compare solo sulle righe vuote. Le righe interne
  non si cancellano di proposito — un backspace che si porta via mezza
  pagina nascosta in un toggle chiuso sarebbe il modo peggiore di
  perdere del testo
- Il cursore resta **a inizio riga** dopo la trasformazione: il campo
  condiviso, quando riceve il fuoco, ora rispetta anche la posizione
  richiesta (`pendingCaret`) invece di mettersi sempre in fondo. In
  fondo, il backspace successivo avrebbe mangiato l'ultima lettera del
  titolo invece di unire la riga a quella sopra
- **[Bug fix]** **Un toggle nuovo nasce aperto.** Lo stato aperto/chiuso
  sta nel blocco e ci restava anche cambiando tipo: una riga che era
  stata un toggle chiuso, rifatta toggle, rinasceva chiusa, e l'Invio
  nel titolo creava un secondo toggle sotto invece di scendere dentro
- **[Bug fix]** Aprire/chiudere un toggle scrive **solo quella colonna**
  (`setExpanded`) e parte dalla versione più recente del blocco: prima
  salvava la copia che la freccia aveva in mano, che poteva avere un
  titolo più vecchio di quello appena scritto
- **[Bug fix]** Il cestino della barra sulle righe vuote ora posa prima
  il fuoco sul campo invisibile: la riga che sparisce aveva il cursore
  dentro, che è il modo in cui l'app si chiudeva
- **Un dubbio aperto, dichiarato**: nel primo tentativo, trasformando in
  testo un toggle **vuoto**, è sparita anche la riga vuota subito sotto.
  Rifatto col registro acceso, non è successo; il registro mostra però
  che il backspace simulato con `adb` arriva al campo **due volte**, che
  è il sospetto principale. Se capita con la tastiera vera, è da
  riprendere. *Aggiornamento del 23/09/2026*: premendo il tasto della
  tastiera Samsung vera il backspace arriva **due o tre volte** anche
  lì; per la riga vuota dentro un toggle ora i doppioni sono scartati
  (vedi "Gli scatti dentro i toggle")
- **Limiti dichiarati**: il backspace a inizio di una riga **con del
  testo** dentro un toggle non la fonde con quella sopra (sono due campi
  separati, è un lavoro a parte); le righe dentro un toggle restano
  campi a sé, non il testo condiviso, quindi non si selezionano due
  righe insieme
- **Verificato sul telefono**: `/toggle` → toggle senza "/", cursore
  sul titolo; titolo, Invio → prima riga dentro, allineata; `412`,
  Invio, `5`, Invio, `6` → tre righe separate; backspace su riga vuota
  → sparisce, cursore in fondo a quella sopra; due righe dentro,
  chiuso e riaperto → **entrambe ancora nel database**

### 🆕 v0.55 — 22/09/2026 · 19:36 — Come si chiude il menu del "+"
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando premo sul + della toolbar, fai sì che sia effettuando la gesture per andare indietro mi tolga la finestra»

Poi:
> «Se faccio la gesture per andare indietro quando ho la finestra delle opzioni del + aperta, prima mi chiude la tastiera, poi la finestra. Fai sì che si chiuda prima la finestra»

Poi:
> «Ora se apro la finestra delle opzioni + mi si chiude la tastiera»

Poi:
> «Eh ma così si chiudono tutte e due, toolbar compresa. Facciamo così, cancella quello che ti ho detto sulla finestra, e fai sì che si chiuda o ritoccando il + oppure toccando su una parte vuota della pagina. Una chicca in più: se riesci, fai sì che il text cursor non venga modificato se tocco su una parte vuota dove c'è la possibilità di scrivere, perché purtroppo succede questo»

Poi:
> «Se tocco da qualsiasi parte della pagina, che non sia questa finestra di cui stiamo parlando, la finestra non scompare, ma il cambio del text cursor avviene comunque»

- **[Nuova funzionalità]** Due modi, e sono quelli che vengono da sé:
  **ritoccare il "+"**, che fa da interruttore, oppure **toccare una
  parte vuota della pagina**. La tastiera resta dov'è in entrambi i
  casi
- **[Bug fix]** **Toccare la pagina non sposta più il cursore.** Il
  menu si chiudeva già da solo al primo tocco fuori, ma quel tocco
  arrivava **anche alla pagina sotto**, che portava il cursore altrove:
  si tornava a scrivere e ci si ritrovava da un'altra parte
- Per risolverlo non bastava ignorare il tocco "mentre il menu è
  aperto": fra la chiusura automatica e il tocco sulla pagina **non era
  detto chi arrivasse prima**, quindi a volte il menu risultava già
  chiuso e il cursore si spostava lo stesso. La chiusura automatica è
  stata tolta (`dismissOnClickOutside = false`) e la fa la pagina:
  decidendo noi, l'ordine è sempre lo stesso
- **E non bastava nemmeno intercettare il tocco dalla lista**: quel
  gestore vede solo quello che i figli non hanno già preso, quindi
  toccando **una riga di testo** il tocco se lo prendeva il campo di
  scrittura — cursore spostato, menu ancora aperto. Serve un **velo
  invisibile** steso sopra la pagina mentre il menu è aperto, che
  intercetta prima di chiunque: chiude il menu e non lascia passare
  altro. Copre solo la pagina e non la barra, così il "+" resta un
  interruttore vero
- `focusable = false` sul menu **resta**: un menu che prende il fuoco fa
  chiudere la tastiera, e qui la tastiera deve restare dov'è
- **Strade scartate, provate e tolte il 22/09/2026**: (1) far chiudere
  il menu al **gesto indietro** — a tastiera aperta quel gesto lo
  prende la tastiera e all'app non arriva affatto, quindi ci volevano
  due gesti; (2) chiudere la tastiera all'apertura del menu per
  aggirare il punto (1) — funzionava, ma vedersi cadere la tastiera
  sotto le dita ogni volta che si apre un elenco è peggio del problema
  che risolveva; (3) chiudere menu e tastiera **insieme** col gesto
  indietro — un gesto solo, ma portava via anche la barra, che non si
  era chiesto di chiudere
- **Verificato sul telefono** con la prova che non lascia dubbi:
  scritto `Prova` col cursore in fondo, aperto il "+", toccata la
  **riga di testo** (non il vuoto), e poi battuta una lettera. Se il
  cursore si fosse spostato sarebbe finita all'inizio; è finita in
  fondo — `ProvaXY` — e il menu si era chiuso

### 🆕 v0.54 — 22/09/2026 · 19:06 — Colore del testo e colore dietro al testo (il pennello)
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di cambiare colore al testo o al background dietro al testo, mettendo un'icona di un pennello e con la palette dei colori dei pittori nella toolbar a destra di Aa. La selezione dei colori fammela tramite una palette colori (che vedi nella foto) con gli esadecimali (che posso scrivere prendendo il codice da internet io manualmente, non dall'app, oppure muovere un pallino dentro il grafico per scegliere in quel modo il colore) e con la possibilità di cambiare il colore da chiaro più vicino al bianco o scuro più vicino al nero e con un grafico con lo spettro dei colori (vedi la foto)»

Poi:
> «Posso chiederti se puoi mette le voci in inglese? Quando apro il pennello sono tutte in italiano. Ricordati questa cosa per il futuro anche per ogni opzione e voci delle opzioni che ti chiedo di creare»

Poi:
> «Non mi fa muovere il pallino del colore del background»

- **[Nuova funzionalità]** Un **pennello nella barra, accanto ad Aa**,
  apre un selettore di colore da cui si tinge il testo selezionato o la
  fascia dietro di lui. Due riquadri in cima, *Testo* e *Sfondo*, per
  passare dall'uno all'altro senza chiudere niente
- **Accanto ad Aa e non dentro**, di proposito: il colore non è un
  interruttore come grassetto e corsivo, apre una finestra sua, e
  metterlo in fila con quelli avrebbe promesso un comportamento che non
  ha
- **[Nuova funzionalità]** Il selettore è quello di Windows, che è
  quello chiesto guardando: riquadro grande con lo spettro (tinta da
  sinistra a destra, saturazione dall'alto in basso), striscia della
  luminosità a destra dal bianco al nero, i tre riquadri
  Rosso/Verde/Blu e il riquadro **Hex**, dove il codice si scrive o si
  incolla a mano — è il modo in cui i colori si copiano da fuori
- **Una sola verità: tinta, saturazione, luminosità.** I tre modi di
  scegliere sono sempre d'accordo perché leggono e scrivono la stessa
  terna. Tenere invece RGB come verità vorrebbe dire ricalcolare la
  posizione del pallino a ogni giro, e **sui grigi la tinta non
  esiste**: il pallino schizzerebbe a sinistra da solo ogni volta che
  si passa per un grigio
- **Mentre si scrive nel riquadro Hex il colore si muove solo quando
  quello che c'è scritto è un colore vero**: a metà di `#FF00` non c'è
  ancora niente da mostrare, e saltare a un colore a caso a ogni
  lettera sarebbe stato peggio che non muoversi
- **[Bug fix]** **La selezione si mette da parte prima di aprire la
  finestra.** Il pannello che sale dal basso toglie il fuoco al campo
  di scrittura, e un campo che perde il fuoco **chiude la selezione**
  riducendola a un cursore: al momento di applicare il colore non
  c'era più niente da colorare, e non succedeva nulla. Visto sul
  telefono al primo tentativo. Ora i campi segnalano la selezione man
  mano (`reportSelection`) e il pennello ne fa una copia nell'istante
  in cui lo si tocca. I comandi della barra Aa non hanno mai avuto
  questo problema perché non aprono niente
- **[Bug fix]** I blocchi isola si riallineavano al database **solo
  quando cambiava il testo**, non la formattazione: il colore veniva
  salvato ma la riga restava com'era fino alla battuta successiva
- **[Bug fix]** **Passando a "Background" il pallino non si muoveva
  più.** Lo stato del colore era legato anche al bersaglio
  (`remember(initialHex, background)`), quindi toccando il riquadro
  "Background" `remember` fabbricava uno stato **nuovo** — mentre i
  gestori dei tocchi, che una `pointerInput` si tiene congelati dalla
  prima composizione, continuavano a scrivere in quello vecchio, ormai
  buttato via. Da fuori sembrava che il riquadro dei colori avesse
  smesso di rispondere. Due correzioni insieme: lo stato si crea **una
  volta sola** all'apertura, e le lambda dei tocchi si tengono fresche
  con `rememberUpdatedState`. Cambiando bersaglio il colore adesso
  **resta quello che è**, che è anche più comodo — di solito si vuole
  lo stesso colore, o una sua correzione
- **[Bug fix]** Il pallino si tiene **dentro i bordi** del riquadro:
  sul rosso pieno cadeva nell'angolo in alto a sinistra e se ne vedeva
  un quarto, cioè sembrava non esserci
- **Nel testo il colore sta sotto lo spoiler**: coperto, il velo deve
  vincere su tutto, altrimenti si leggerebbe il testo colorato
  attraverso
- **Un esadecimale che non si riesce a leggere viene ignorato** invece
  di far saltare il disegno: quei codici li scrive l'utente a mano, e
  una lettera di troppo non deve far sparire il testo
- **Nessuna migrazione**: `color` e `background` sono due campi con
  valore predefinito dentro il JSON del testo, come `spoiler`
- **Verificato sul telefono**: parola selezionata, pennello, colore
  preso dallo spettro, `Applica` — testo verde `#26D931` a schermo e
  nel database; poi lo stesso su *Sfondo*, e `Nessun colore` che toglie
  solo quello lasciando il testo colorato

### 🆕 v0.53 — 22/09/2026 · 18:36 — Il menu dei tre puntini di una pagina, e Updates
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Togli dappertutto, menu principale e tutte le pagine, l'icona della lente d'ingrandimento in alto a destra della feature search. Mettimi in ogni pagina un'icona (nella stessa posizione di dove stava la lente d'ingrandimento) in alto a destra rappresentata da tre puntini orizzontali che rappresentano le opzioni di quella pagina (anche di quella principale). Mettimi poi dentro tutte le opzioni che ti dico (tra quelle che vedi in foto): Favorite, Search, Duplicate, Move to, Move to trash, Lock view, Lock page, Import, Export, Updates. Quest'ultima mi consentirà di aprire una nuova finestra con tutte le modifiche fatte alla pagina, con data, ora e parole modificate, per esempio così:
> 22.10.2026 19:41= Esmepoi (con lo strikethrough) -> Esempio
> Import ed Export mettile in grigio, poi in futuro le implementerai.»

- **[Nuova funzionalità]** La **lente della ricerca sparisce** dalla
  barra in alto, su ogni pagina e sulla principale, e al suo posto —
  stessa posizione — ci sono i **tre puntini**. Dentro: Favorite,
  Search, Duplicate, Move to, Move to trash, Lock view, Lock page,
  Import, Export, Updates. Le cose che si fanno a una pagina intera
  stanno in un posto solo invece di essere sparse fra la barra e le
  impostazioni
- **[Nuova funzionalità]** Lo stesso menu c'è anche sul **database
  aperto a schermo intero**: un database lì è una pagina come le altre.
  È un file a sé (`PageOptionsSheet.kt`) proprio perché lo aprono due
  schermate diverse, e due copie di un menu di comandi si separano al
  primo cambiamento
- **Import ed Export sono spente**, come chiesto: si vedono per dire
  che arriveranno. Le voci che invece **non hanno senso** su una certa
  pagina non ci sono affatto — sulla principale niente Favorite,
  Duplicate, Move to o Move to trash, e "Lock view" solo sui database
  (*dal 24/09/2026 Move to sulla principale c'è, grigia*: richiesta 33).
  Sono due cose diverse: spenta vuol dire "non ancora", assente vuol
  dire "non qui"
- **Duplicate** copia blocchi e, per un database, colonne righe e
  celle, e mette la copia **accanto all'originale**. Gli id di blocchi
  e colonne vengono tradotti mentre si copia, altrimenti i figli di un
  toggle e le celle resterebbero appesi all'originale. Le **sottopagine
  non si duplicano**: la copia rimanda alle stesse, perché duplicare a
  cascata mezzo archivio non è quasi mai quello che si vuole.
  *Superato*: vedi "Duplicate: si sceglie dove va la copia" più in alto
  — rimandare alle stesse sottopagine voleva dire che la copia non era
  una copia
- **Move to**: qui l'albero di navigazione non è un campo della pagina
  ma **i blocchi che la richiamano**, quindi spostare vuol dire togliere
  quel collegamento e crearne uno in fondo alla pagina scelta. Fra le
  destinazioni non compaiono i database (il loro contenuto sono righe,
  non blocchi) né le pagine nel cestino.
  *Superato* (24/09/2026, richiesta 33): la scelta ora è l'albero con la
  ricerca, comprese le pagine dei database; vedi "Move to — l'albero con
  la ricerca" più in alto
- **Move to trash** non cancella niente: segna `trashedAt` e toglie i
  collegamenti che la mostravano. Il contenuto resta tutto. **Non c'è
  ancora una schermata del cestino** — finché non c'è, una pagina
  buttata si ritrova solo dal database, e la finestra di conferma lo
  dice invece di far finta di niente
- **Lock page** rende il contenuto **di sola lettura**, non spento:
  `readOnly` e non `enabled = false`, così il testo si seleziona ancora
  e si copia. Vale per titolo, corpo, nomi delle righe e celle, e fa
  sparire la barra sopra la tastiera e il pulsante "New page" — sono
  tutte cose che modificano
- **Lock view** è un'altra cosa e vale solo sui database: le righe si
  modificano ancora, **l'impaginazione no** (vista, ordine, filtro,
  raggruppamento, colonne). I comandi restano al loro posto **spenti**
  invece di sparire: un pulsante che scompare fa pensare a un guasto,
  uno spento dice che è stato messo a riposo apposta. Cancellare invece
  resta possibile — il lucchetto protegge come è messo insieme il
  database, non la sua esistenza
- **[Nuova funzionalità]** Una schermata che elenca cosa è stato
  cambiato nella pagina, dalla più recente, nella forma chiesta:
  `22.09.2026 18:34` e sotto il testo di prima **sbarrato**, una
  freccia, il testo di adesso
- **Una voce per sessione di scrittura, non per tasto premuto.** È il
  punto delicato: registrando ogni battuta, "Esmepoi → Esempio"
  diventerebbe sette righe illeggibili. Una sessione dura finché si
  scrive nello stesso blocco e si chiude quando il cursore va altrove,
  quando cambia la struttura della pagina o quando si esce. È **lo
  stesso confine che l'Annulla usa già**, riusato invece di
  inventarne un secondo che prima o poi si sarebbe disallineato
- **[Bug fix]** Il solo spostare il cursore, senza scrivere, non
  chiudeva la sessione: la modifica restava in sospeso fino alla
  successiva e la cronologia aveva sempre un pezzo di ritardo. Ora la
  chiude la schermata quando cambia la riga a fuoco, e il titolo la
  chiude quando perde il fuoco
- **Il "dopo" si legge dopo una pausa** (`EDIT_SETTLE_MS`, 400): quello
  che si è appena scritto viaggia verso il database e torna indietro, e
  leggendolo subito la cronologia racconterebbe una correzione troncata
  all'ultima lettera
- **E su uno scope che non muore con la schermata**
  (`PageRepository.recordEditAfterSettling`): l'ultima sessione si
  chiude proprio mentre si esce dalla pagina, e lo scope del ViewModel
  a quel punto è già annullato — quella riga, che è proprio quella che
  si vuole ritrovare, non sarebbe stata scritta mai
- **Il titolo entra nella cronologia** con un id di blocco riservato
  (`PageEditEntity.TITLE_BLOCK_ID`): non è un blocco ma si modifica
  come loro, e dargli un id evita di trattare un `null` come caso a sé
  dappertutto
- **`blockId` non ha un vincolo verso i blocchi**, di proposito: la
  cronologia deve sopravvivere al blocco che racconta, altrimenti
  cancellare un paragrafo cancellerebbe anche la prova che c'era
- **Limite dichiarato**: si registrano il testo dei blocchi e il
  titolo. Le **celle di un database no** — vivono in tabelle separate e
  passano da un'altra strada, che andrà agganciata a parte
- **Verificato sul telefono**: migrazione a 23 con le pagine intatte e
  la tabella nuova creata; scritto `Esmepoi`, corretto in `Esempio`,
  spostato il cursore, e in "Updates" è comparsa **una riga sola** con
  la data giusta e la parola vecchia sbarrata

### 🆕 v0.52 — 22/09/2026 · 15:19 — Preferiti, la base
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Devo farti i complimenti: senza avertelo chiesto hai messo una piccola ma ultra importante feature, ossia quello di toccare con il dito la parola per scoprirla. Bravissimo.
> Ora, nuova feature: dammi la possibilità di mettere come preferita una pagina (ovviamente non quella del menu principale )o un database. Per il momento, crea un archivio nascosto che più in là ti dirò di mettere in una sidebar che sempre in futuro ti dirò di implementare (ora non mi serve vedere la lista delle pagine preferite, ma solo per creare la base)»

- **[Nuova funzionalità]** Una pagina o un database si possono mettere
  fra i preferiti. **Per ora non c'è niente che li mostri**: è
  deliberato, serve la base su cui poggerà la barra laterale che verrà
- **Una colonna sola per pagine e database** (`pages.isFavorite`, con
  la migrazione 21→22): qui un database **è** una pagina con
  `isDatabase = 1`, quindi metterlo fra i preferiti è esattamente la
  stessa operazione. Due elenchi separati sarebbero stati due posti da
  tenere in fila e due modi di sbagliare
- **La pagina principale non si può mettere fra i preferiti**: è quella
  da cui si parte sempre, e un collegamento a lei non porterebbe da
  nessuna parte. La stellina lì non compare, e il divieto è ribadito in
  `PageRepository.setFavorite` — una regola che vive in un posto solo
  prima o poi viene aggirata da una strada nuova
- **Due posti diversi, e il motivo**: per le pagine è una **stellina
  nella barra in alto**, perché lassù di impostazioni non ce n'è
  nessun'altra; per i database è un interruttore dentro **Settings**,
  perché quella finestra è l'unica che si raggiunge sia col database
  dentro una pagina sia aperto a schermo intero, quindi è anche
  l'unico punto dove non va messo due volte
- **Ci stanno anche le pagine-riga**, al contrario dell'elenco della
  Home: là l'esclusione serve a non riempire la Home di pagine che
  nessuno ha creato apposta, qui invece nell'elenco finisce solo quello
  che è stato scelto uno per uno
- Si scrive **solo quella colonna** (`PageDao.setFavorite`), non tutta
  la riga: la stessa pagina può stare aperta in due schermate insieme,
  e salvarla intera da qui rimetterebbe indietro un titolo cambiato
  nell'altra
- **Verificato sul telefono**: migrazione a 22 eseguita con le cinque
  pagine intatte; stellina accesa e spenta su una pagina e
  l'interruttore acceso e spento su un database, controllando ogni
  volta il valore nel database

### 🔧 v0.51.1 — 22/09/2026 · 14:52 — La formattazione sopravvive alla scrittura
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Funziona ma una piccola cosa: scrivo le due lineette, scrivo la parola, scrivo le terza lineetta, scrivo la quarta e si censura subito, ma appena faccio spazio, backspace dopo lo spazio, o scrivo qualcosa, si rivede qualsiasi parola con lo spoiler per un attimo, solito scattino che scompare e riappare»

- **[Bug fix]** **La parola censurata non riappare più a ogni tasto.**
  Gli span salvati sono sempre un giro indietro rispetto al campo — il
  campo ha già la lettera appena battuta, il database ce l'avrà fra
  qualche decina di millisecondi — e in quel fotogramma la
  trasformazione rinunciava a disegnare ogni stile. Per grassetto e
  corsivo non si notava; uno spoiler che per un istante non viene
  coperto invece **si legge**, ed era lo scatto che si vedeva scrivendo
  uno spazio o cancellando. Adesso, invece di rinunciare, la stessa
  modifica si applica agli span con `applyTextEdit`, che restituisce
  span corrispondenti **esattamente** alla riga mostrata: le posizioni
  non possono sfasarsi
- **Limite dichiarato**: quando il campo ha un numero di righe diverso
  dai blocchi — cioè nell'istante in cui una riga nasce o sparisce — si
  continua a non disegnare niente. Lì la corrispondenza riga-blocco non
  è affidabile, e uno stile preso dal blocco sbagliato sarebbe peggio
  di nessuno stile
- **[Bug fix]** Nel testo scorrevole ogni riga toccata veniva riscritta
  come **un unico pezzo senza formato**: bastava aggiungere una lettera
  in fondo a un paragrafo perché il grassetto di una parola lontana
  sparisse. Era un difetto vecchio, che non si notava perché quasi
  sempre si formatta alla fine; con gli spoiler sarebbe stato evidente
  al primo tentativo — si copre una parola, si continua a scrivere e il
  velo cade da solo
- Ora la zona modificata si tratta come **un testo solo**, a-capo
  compresi: la modifica si applica agli span con `applyTextEdit`, che
  tocca soltanto il pezzo davvero cambiato, e poi si rimette in righe
  (`splitLines`/`joinLines`). Vale anche a cavallo di più righe:
  andando a capo in mezzo a una frase, la metà che scende **tiene il
  suo formato**
- **Gli span attaccati con la stessa formattazione si fondono**
  (`merged`). Applicando solo il tratto cambiato, ogni tasto battuto
  lasciava il suo pezzetto separato e il JSON di un paragrafo cresceva
  di una voce per lettera scritta: visto davvero sul telefono, una
  frase di dieci caratteri era dieci span
- **Rete di sicurezza**: se il risultato non corrisponde più alla riga
  che deve descrivere, si scrive la riga in chiaro. Meglio perdere un
  grassetto che scrivere nel database un testo diverso da quello che
  l'utente vede — è già successo una volta, e quella volta è costata
  dei blocchi svuotati
- **[Bug fix]** Nei blocchi isola (toggle) la barra **Aa** formattava
  **una lettera prima** di quella selezionata: le posizioni della
  selezione contano anche l'a-capo nascosto in testa, gli span no

### 🆕 v0.51 — 22/09/2026 · 14:45 — Testo coperto (spoiler)
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ottimo lavoro. Mettimi la possibilità di mettere il filtro Spoiler ad un campo di testo (come fanno Discord e Telegram) scrivendo, per esempio, due linee verticali a sinistra e a destra del testo da nascondere (quindi tipo ||Spoiler||). Questa cosa mettimela anche come creabile nella categoria di testo Aa»

Poi:
> «Appena scrivo la seconda linea verticale dopo la parola, la parola stessa si vede con un background grigio. Quello che vorrei è che appena scrivo la quarta e ultima lineetta verticale è che si faccia subito la "censura spoiler"»

- **[Nuova funzionalità]** Come su Discord e Telegram: si scrive
  `||qualcosa||` e le quattro barre spariscono — sono un comando, non
  roba scritta — lasciando al loro posto una **fascia grigia** che
  copre le lettere. La stessa cosa si fa dalla barra **Aa**, con
  l'occhio sbarrato, su qualsiasi pezzo di testo selezionato
- **[Nuova funzionalità]** Si scopre **toccandolo**: il pezzo coperto
  torna leggibile quando il cursore, o la selezione, ci sta **dentro**,
  e si ricopre appena il cursore se ne va o il campo perde il fuoco. Non
  serviva nient'altro: in un editor un tocco è già un cursore che si
  posa lì
- **Dentro, non sui bordi**, e il motivo è il momento in cui lo spoiler
  nasce: finito di scrivere `||parola||` il cursore resta appoggiato
  **subito dopo l'ultima lettera**, quindi contando anche il bordo lo
  spoiler veniva al mondo già scoperto — si vedeva la parola con la sua
  fascia chiara invece della censura. Contando solo l'interno si copre
  nell'istante in cui si scrive la quarta barra, ed è quello che ci si
  aspetta. Per leggerlo non cambia niente: un dito cade dentro la
  parola, non sul suo bordo
- **Scelta**: da coperte le lettere si disegnano **trasparenti**, non
  tolte. Restano dov'erano, quindi la riga è larga uguale, va a capo
  dove andava e il cursore ci si muove dentro normalmente; sopra passa
  la fascia. Togliere davvero il testo avrebbe voluto dire rifare tutta
  la traduzione delle posizioni, che è la parte più delicata di questo
  editor
- **Scelta**: la chiusura si lega all'**apertura più vicina**, così
  `||a|| e ||b||` fa due spoiler separati invece di coprire anche la
  "e" in mezzo. In mezzo alle barre non ci può stare né un'altra barra
  né un a-capo
- **[Nuova funzionalità]** Uno spoiler **non si allunga scrivendoci
  dopo**. Per tutto il resto (grassetto, corsivo...) continuare a
  scrivere continua la formattazione, che è quello che fanno tutti gli
  editor; qui no, perché il cursore resta proprio lì attaccato e la
  parola dopo sarebbe diventata invisibile senza che nessuno l'avesse
  chiesto. Scrivendo **dentro**, invece, si allarga
- **Nessuna migrazione**: `spoiler` è un campo con valore predefinito
  dentro il JSON del testo, e il testo già salvato — che quella chiave
  non ce l'ha — si rilegge così com'è

### 🆕 v0.50 — 22/09/2026 · 14:27 — Gli elenchi numerati cambiano modo di contare a ogni rientro
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ottimo lavoro come sempre (poi sappi che ad un certo punto controllerò tutto quanto sin dall'inizio e poi in futuro ti dirò).
> Nuova feature: fai la stessa identica cosa ma quando faccio una Numbered list, e gli elementi sotto li indento a destra, fai sì che gli elementi sotto non siano sempre dei numeri ma fai come fa OneNote nella foto (i numeri 111, 222, 333, eccetera sono solo degli esempi). Ovviamente, se alcuni elementi sotto li indento a sinistra, fai sì che alla stessa posizione della colonna di ciò che indento a sinistra (in base a quello che c'è sopra sulla stessa linea non visibile) venga rispettato l'ordine.
> I simboli in ordine saranno: numeri con un punto, numeri romani con punti, numeri con parentesi, numeri romani con parentesi, lettere con numeri, One Two Three eccetera, lettere con parentesi, First Second Third eccetera, e poi ricomincia con i numeri con un punto (tutto come vedi nella foto, ma rispetta l'ordine che ti ho scritto qui, non quello della foto, che ti serve solo per vedere come si scrivono)»

- **[Nuova funzionalità]** Stessa idea dei puntati, otto modi in fila
  (`NUMBERED_STYLES`): `1.`, `i.`, `1)`, `i)`, `a.`, `One.`, `a)`,
  `First.` — e poi si ricomincia da `1.`. Verificato sul telefono
  livello per livello, dallo zero all'otto: al nono rientro torna `1.`
- **Assunto dichiarato**: "lettere con numeri" è stato inteso come
  **lettere col punto** (`a.` `b.` `c.`), in parallelo a "lettere con
  parentesi" (`a)`). Se si intendeva altro è una riga sola da cambiare
- **[Bug fix]** **Ogni livello ha il suo contatore che riparte da uno**
  (`numberedListOrdinal`). Prima il numero veniva composto col ramo
  intero — `1.1.1` — e a quel punto i modi di scrivere delle colonne
  più interne non avevano senso: `i.` non può dire "uno punto uno".
  Ora il conteggio si tiene in una pila di contatori, uno per livello,
  e tornando indietro si riprende da dove quella colonna era rimasta
- **Scelte dentro i modi di scrivere**: i numeri romani tornano a
  essere **cifre oltre il 3999** (`romanNumeral`), che è meglio di una
  riga di `m`; le lettere proseguono **come le colonne di un foglio di
  calcolo** (`letterOrdinal`, `z` → `aa`) invece di fermarsi alla zeta;
  le parole (`One`, `First`) si fermano a **venti** e oltre tornano
  cifre, perché "Thirtyseventh" si mangerebbe mezza riga
- **[Nuova funzionalità]** Anche i numeri **dentro un toggle** passano
  dalla stessa funzione: allo stesso rientro un numero dentro un toggle
  e uno fuori devono essere scritti uguali
- **[Bug fix]** La riga appena creata con l'Invio prende il numero
  **previsto** (`carriedOrdinal + 1`) invece di nessun numero: è lo
  stesso rimedio dei pallini, e senza si vedeva il segno comparire in
  ritardo

### 🆕 v0.49 — 22/09/2026 · 14:09 — Gli elenchi puntati cambiano segno a ogni rientro
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Grazie del complimento della copertina (spero che non mi avresti detto la stessa cosa se fosse stata dell'Inter ahahahaha, scherzo).
> Nuova feature: quando faccio una Bulleted list, e gli elementi sotto li indento a destra, fai sì che gli elementi sotto non siano sempre dei pallini ma fai come fa OneNote nella foto (i numeri 111, 222, 333, eccetera sono solo degli esempi). Ovviamente, se alcuni elementi sotto li indento a sinistra, fai sì che alla stessa posizione della colonna di ciò che indento a sinistra (in base a quello che c'è sopra sulla stessa linea non visibile) venga rispettato l'ordine.
> La sequenza sarà: pallini pieni, pallini vuoti, quadratini pieni, quadratini vuoti, rombi pieni, rombi vuoti triangolini verso destra pieni, triangolini verso destra vuoti, e poi ricomincia dai pallini pieni. Tutte queste icone fammele sufficientemente piccole in modo anche che non mi confondo con i quadratini vuoti rispetto alle checkbox vere e proprie, ma non troppo»

Poi:
> «Oddio, perdonami se sono pignolo, ma sono un po' troppo piccoli così»

Poi:
> «Non sempre, ma al 90% delle situazioni, vedo che quando vado a capo quando sto su un bullet, prima fa un scattino il text cursor all'inzio del blocco, e poi ritorna al punto esatto dove si trova il bullet. I simbolini falli ancora un pochino più grandi, ovviamente centrati per bene rispetto all'altezza complessiva del blocco»

Poi:
> «Adesso ti chiedo per favore che i simbolini non me li centri perfettamente come ti ho chiesto poco fa, ma me li sposti un pelino più sotto. Perdonami. Ti dico poi se mi va bene»

Poi:
> «Mi sono fatto io di funghi allucinogeni (scherzo) oppure alcuni bullet sono stortini? Tipo i rombini, i quadratini, il paragraph tra alcuni bullet non è uguale per tutti»

- **[Nuova funzionalità]** Come su OneNote: `•` pieno, `◦` vuoto, `▪`
  quadratino pieno, `▫` quadratino vuoto, `◆` rombo pieno, `◇` rombo
  vuoto, `▸` triangolino pieno, `▹` triangolino vuoto — e poi si
  ricomincia
- **[Nuova funzionalità]** Il segno dipende **solo dal livello di
  rientro**, non da cosa c'era prima. È quello che serve per riportare
  indietro una riga: ritrova da sola il segno della colonna in cui
  arriva, qualunque strada abbia fatto. Con un contatore "prosegui da
  dove eri", due righe alla stessa altezza potrebbero finire con segni
  diversi
- **[Nuova funzionalità]** Ognuno ha **il suo corpo**
  (`BULLET_MARKER_SIZES`): questi caratteri sono disegnati molto
  diversi fra loro — un pallino occupa meno di un terzo del suo spazio,
  un rombo quasi due terzi — e con un'unica dimensione per tutti i
  pallini venivano minuscoli e i rombi delle macchie. Così si vedono
  della stessa misura
- **[Bug fix]** **L'altezza della riga del testo scorrevole è sempre
  fissata** (`RUN_LINE_HEIGHT`, 24). Lasciandola decidere al carattere,
  ogni riga diventava alta quanto il segno più grande che conteneva: le
  righe coi pallini (scritti a 20) erano più alte di quelle coi rombi
  (scritti a 13), e l'elenco veniva a passo irregolare. Sembravano
  storti i segni, ma a essere storte erano le righe. Misurato sulla
  stessa lista di diciassette punti: prima il passo fra una riga e
  l'altra andava da 34 a 48 punti, dopo è 43-44 su tutte
- **[Nuova funzionalità]** I segni scendono di un filo sotto la riga di
  base (`BULLET_MARKER_DROP`): appoggiati esattamente sulla riga di base
  sembravano un po' troppo in alto rispetto al testo. È una **frazione
  del corpo del segno**, non una misura fissa, così i pallini (scritti
  grandi) e i rombi (scritti piccoli) scendono in proporzione e restano
  allineati fra loro. Non è un centraggio calcolato: è un ritocco
  deciso guardando lo schermo
- **Vincolo**: nessun segno supera i **20**. Il limite non è la misura
  del testo (16) ma **l'altezza della riga**: il testo scorrevole sta su
  righe da 24 e un carattere da 16 ne occupa in altezza meno di 19,
  quindi c'è margine. Dentro quei 24 la riga non si allarga; oltre, si
  allargherebbero solo le righe col segno più grande e l'elenco
  verrebbe a scalini
- **[Bug fix]** **Il cursore non scatta più a inizio riga andando a
  capo.** Subito dopo un Invio il campo ha una riga in più del
  database, e da quel punto in giù righe e blocchi non si
  corrispondevano: chiedendo "il blocco numero i" si otteneva quello
  della riga *successiva*. Se era un paragrafo — il caso normale, un
  elenco finisce quasi sempre con una riga vuota sotto — la riga nuova
  veniva disegnata **senza pallino**, e il cursore si vedeva scattare a
  sinistra per poi tornare al suo posto appena il database arrivava.
  Ora il punto d'inserimento si trova **dalla fine** (le righe in fondo
  che combaciano ancora coi loro blocchi dicono quante ne vengono dopo
  quella nuova) e da lì in giù la corrispondenza viene spostata di uno.
  Cercarlo dall'inizio non funziona: andando a capo cambia testo anche
  la riga divisa. Verificato catturando il **primo fotogramma** dopo
  l'Invio: il pallino c'è già e il cursore è al suo posto
- **[Nuova funzionalità]** Il tetto ai rientri sale **da 5 a 10**: con
  cinque livelli il giro di otto non si sarebbe mai potuto vedere né
  finire né ricominciare

### 🔧 v0.48.1 — 22/09/2026 · 13:54 — Copertina e titolo scorrono con la pagina
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Woah, attenzione: se scorro la pagina del menu principale, tutto lo spazio che va dal titolo compreso in sopra compresa la copertina rimane bloccato. Prima funzionava bene.
> Vedi se è collegato»

- **[Bug fix]** Stavano fuori dalla lista che scorre, quindi restavano
  inchiodati in cima mentre il testo scorreva sotto. Con una copertina
  alta si mangiavano mezza schermata che non si poteva recuperare. Ora
  sono la **prima voce della lista** e se ne vanno scorrendo, come su
  Notion. Non era una regressione recente: sono sempre stati lì fuori,
  si nota da quando c'è una copertina
- **[Bug fix]** Sono **una voce sola** di proposito: gli indici della
  lista servono anche a portare in vista la riga che prende il cursore,
  e uno scarto fisso (`HEADER_ITEM_COUNT`) è molto più facile da tenere
  giusto di uno variabile
- **Verificato**: il titolo si scrive ancora, e **messo a fuoco e poi
  fatto scorrere via sei volte di fila non chiude l'app** — era il
  rischio vero, perché ora anche quel campo di testo può essere
  distrutto dallo scorrimento come gli altri dentro la lista

### 🆕 v0.48 — 22/09/2026 · 13:29 — La barra sopra la tastiera, rifatta
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Per migliorare in maniera definitiva questa toolbar, fammela con un colore del background diversa da quello dell'app, un nero più tendente al grigio scuro diverso da quello della tastiera (ti mando la foto così ti rendi conto). E soprattutto, rendimela più bassa (rispetta ovviamente l'altezza di tutte le icone presenti nella toolbar) e attaccata alla tastiera»

Poi:
> «Quando eseguo la gesture per andare indietro e far scomparire la tastiera, la toolbar ci mette qualche millisecondo in più a scomparire. Riusciresti a farlo allo stesso tempo della tastiera?»

- **[Nuova funzionalità]** Ha un **grigio tutto suo**
  (`DarkSurfaceVariant`), più chiaro della pagina e diverso dalla
  tastiera: prima era dello stesso nero della pagina e non si capiva
  dove finisse una e cominciasse l'altra
- **[Nuova funzionalità]** È **più bassa**: i pulsanti passano dai 48 di
  serie a 40 e le icone a 22 (`BAR_BUTTON_SIZE`, `BAR_ICON_SIZE`), e
  sparisce il margine sopra e sotto — l'altezza la danno i pulsanti, e
  ogni punto in più lì è una fascia in più che copre il testo. Tutti i
  pulsanti passano da `BarButton`, così non possono avere misure
  diverse fra loro
- **[Bug fix]** **Se ne va insieme alla tastiera.** Il suo bordo
  inferiore è appoggiato sopra la tastiera, quindi mentre quella scende
  la barra la segue — ma si fermava al bordo dello schermo e restava lì
  finché il sistema non dichiarava la tastiera chiusa: da fuori sembrava
  sparisse qualche istante dopo. Negli ultimi punti della discesa ora la
  si spinge giù di quanto le manca per uscire
  (`slideAway = altezzaBarra - insetTastiera`), così arriva al bordo
  nello stesso momento; quando il sistema poi la smonta è già
  invisibile e non si vede nessuno scatto. Col menu "+" aperto lo
  scorrimento è disattivato, perché lì la barra si vede anche senza
  tastiera
- **[Bug fix]** È **attaccata alla tastiera**: restava sospesa di pochi
  punti perché lo spazio per la barra di navigazione arrivava dallo
  `Scaffold` e quello per la tastiera da `imePadding`, e a tastiera
  aperta **si sommavano**. Ora il fondo prende l'unione dei due
  (`WindowInsets.ime.union(WindowInsets.navigationBars)`): vince la più
  alta, che è sempre quella giusta — a tastiera aperta la barra di
  navigazione le sta dietro e non occupa niente

### 🔧 v0.47.1 — 22/09/2026 · 11:31 — La barra della formattazione non resta più a mezz'aria
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Perfetto, funziona tutto alla grande, ottimo lavoro ancora.
> Adesso vorrei parlarti di un bug, prima di concludere la storia delle view dei databse (ci mancava la Gallery se non erro): se io, quando NON è aperta la tastiera, tocco su una qualsiasi parte di quello spazio cerchiato di rosso nella prima foto, appare il bug che vedi nella seconda foto, ossia, sempre quella dannata toolbar che va troppo in alto alla tastiera e il text cursor (compreso il focus) va sotto in fondo alla pagina»

- **[Bug fix]** È il difetto che tornava da giorni, e la causa non era
  nel codice ma nel **manifesto**: l'attività non dichiarava come
  comportarsi all'apertura della tastiera, e Android sceglieva da sé di
  far **scorrere su tutta la finestra** per tenere in vista il punto in
  cui si scrive — il titolo della pagina usciva dallo schermo in alto.
  Il guaio è che il codice aggiunge *già* lo spazio per la tastiera con
  `imePadding`: i due effetti si sommavano e la barra finiva staccata
  dalla tastiera, di quasi un'altezza di tastiera
- **[Bug fix]** Con `android:windowSoftInputMode="adjustResize"` la
  finestra resta ferma e a fare spazio pensa solo `imePadding`, che sa
  dov'è davvero la tastiera. Verificato sullo stesso gesto che lo
  faceva comparire: il titolo ora **resta al suo posto** e la barra è
  incollata alla tastiera
- **[Bug fix]** **Il tocco a vuoto non manda più il cursore in fondo
  alla pagina.** Il gestore "hai toccato lo spazio vuoto, continua a
  scrivere" copriva **tutta** la lista, quindi prendeva anche lo spazio
  accanto al titolo e i buchi fra una riga e l'altra: toccandoli, il
  cursore saltava all'ultimo blocco in fondo e si apriva la tastiera.
  Ora scatta solo per i tocchi **sotto l'ultimo blocco** — e solo se
  l'ultimo blocco è davvero l'ultimo, non l'ultimo *visibile*.
  Verificato che il gesto utile continui a funzionare

### 🆕 v0.47 — 22/09/2026 · 11:17 — Le caselle da spuntare entrano nel campo di testo condiviso
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Se aggiorni a Compose 1.7? Pensi che si risolverà tutto subito? Il "lavoro più delicato dell'editor e lo faccio con calma nel prossimo giro" lo vuoi fare adesso?
> EDIT: se creo una checkbox, mi si toglie la tastiera, ovviamente io ci voglio scrivere subito;
> EDIT 2: adesso, se cancello le checkbox scatta solamente la toolbar, non più la tastiera
> EDIT 3: se scrivo dopo una checkbox, premo invio, mi cancella TUTTO quello che ho scritto e non va a capo»

Poi:
> «Ok, mi sembra tutto perfetto (per ora), ottimo lavoro. Vorrei solo chiederti una piccola cosa: puoi mettermi un pochino di spazio tra ogni blocco di testo che contiene una checkbox? Dato che già di per sé le checkbox sono piccole, ho paura che per sbaglio tocco un'altra checkbox per quanto sono piccole. Poi ti dico se va bene o no, inizia ad aumentare lo spazio di poco poco alla volta per favore»

Poi:
> «Ancora un pelino, ma vedo che il testo adesso non è perfettamente allineato con le checkbox»

Poi:
> «Adesso, ogni volta che scrivo o cancello qualcosa su una checkbox o su un blocco di testo vuoto, tutto quello che contenuto in un blocco con una checkbox sotto di esso scatta scomparendo e ricomparendo, e per pochissimi millisecondi scatta a destra. Scatta solo ciò che sta in un blocco da checkbox.
> EDIT: ho fatto caso che il text curor ora è troppo grande, più di prima. Mettilo standard»

- **[Nuova funzionalità]** È la cura alla radice di tutta la famiglia di
  difetti delle caselle. Finché ognuna era un campo di testo **a sé**,
  andare a capo spostava il fuoco da un campo all'altro e ogni cambio di
  specie ne distruggeva uno mentre aveva il cursore: da lì la tastiera
  che spariva, la maiuscola che faceva uno scatto, le lettere nella riga
  di prima, il testo cancellato dall'Invio e l'app che si chiudeva.
  Dentro il campo condiviso **andare a capo è solo scrivere un a-capo**,
  come fra due paragrafi: non c'è nessun fuoco da spostare e niente da
  distruggere
- **[Nuova funzionalità]** Il quadratino non può stare *dentro* un campo
  di testo, quindi il testo gli riserva lo spazio con dei caratteri
  invisibili (`CHECKBOX_MARKER`) e il quadratino vero viene disegnato
  **sopra**, alla posizione che il campo stesso dichiara per quel
  carattere (`RunCheckboxOverlay`). Così resta incollato alla sua riga
  anche quando il testo va a capo da solo, e ha una zona di tocco vera
  invece di essere un carattere da centrare col dito
- **[Nuova funzionalità]** L'overlay va messo **dopo** il campo dentro
  il `Box`: in Compose i tocchi li riceve per primo l'ultimo figlio, e
  messo prima il quadratino non rispondeva — il tocco andava al testo
- **[Nuova funzionalità]** Una casella spuntata ha il testo **sbarrato e
  smorto**, come su Notion. L'Invio ne crea un'altra non spuntata, e il
  backspace a inizio riga toglie il quadratino lasciando il testo: sono
  gli stessi due comportamenti che avevano già elenchi puntati e
  numerati, non un ramo a parte
- **Trappola, costata una mezz'ora**: i tipi che vivono nel campo
  condiviso sono elencati **due volte**, in `FLOWING_TYPES` nella
  schermata e in `isFlowingTextType` nel ViewModel. Aggiornando solo il
  primo, il campo mostrava le caselle ma il ViewModel non le riconosceva
  come sue: sullo schermo si scriveva e **nel database non arrivava
  niente**, senza un errore. Se si tocca uno, va toccato l'altro
- **Verificato sul telefono**, misurando gli eventi veri della tastiera:
  spuntare col tocco funziona e si salva; l'Invio crea un'altra casella
  e il testo resta (`AaaaaaaaaaaaaQQ` / `RR` / `SS` nel database, nessun
  indice doppio); il backspace a inizio riga toglie il quadratino;
  creare una casella dal menu "/" lascia la tastiera aperta e si scrive
  subito. In tutte e quattro le prove `ImeTracker` **non ha registrato
  né `onHidden` né `onShown`**, e non c'è stato nessun crash
- **[Nuova funzionalità]** Le righe con una casella **stanno più
  larghe** (`CHECKBOX_LINE_HEIGHT`): il quadratino è piccolo e attaccate
  è facile spuntare quella sbagliata col dito
- **[Bug fix]** **I quadratini non saltano più ad ogni tasto.**
  Scrivendo o cancellando, sparivano e riapparivano spostati di lato per
  un istante. Erano disegnati alle posizioni ricalcolate dal testo
  corrente del campo, mentre le coordinate arrivavano dalla misura del
  fotogramma prima: due fonti che per un attimo non combaciano. Ora il
  marcatore si cerca **dentro il testo che il campo ha davvero
  misurato** (`layoutInput.text`), quindi disegno e coordinate non
  possono più sfasarsi
- **Compromesso dichiarato**: **il cursore di scrittura è alto quanto la
  riga**, e il campo di testo lo disegna così — non si può separare. Più
  spazio fra le caselle vuol dire cursore più alto. A 34 si notava; il
  valore sta a 27, appena sopra il naturale (24): cursore di misura
  normale e un po' d'aria fra una casella e l'altra
- **[Nuova funzionalità]** Il testo si **centra** nella riga alta
  (`lineHeightStyle`). Di suo Compose spartisce lo spazio in più in
  proporzione a quanto le lettere salgono e scendono, e il testo non
  finisce in mezzo: i quadratini, che invece stanno in mezzo,
  risultavano disallineati. Chiedendo il centro, testo e quadratino
  coincidono **per costruzione**, qualunque altezza di riga si scelga —
  meglio di una correzione a occhio da ritoccare ogni volta
- **Resta com'era** la casella dentro un *toggle*: lì è ancora una riga
  a sé (`BlockRow`), perché i figli di un toggle si disegnano per conto
  loro

### 🔧 v0.46.1 — 22/09/2026 · 10:27 — L'Invio dentro una casella non sposta più il fuoco
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Le checkbox normali hanno danno ancora problemi. Se ne creo una, mi fa scrivere e poi vado a capo, la maiuscola attivata fa uno scattino, da minuscola a maiuscola. Mi è anche capitato che andando a capo varie volte e velocemente (capiscimi, lo faccio solo per far sì che quest'app sia efficace al massimo) l'app mi si chiude, non mi dà un messaggio di crash ma si chiude e basta. Inoltre, quando cancello una checkbox, oppure usando backspace andando sopra al testo collegato ad una checkbox, la tastiera scompare.»

Poi:
> «Se cancello una checkbox, la tastiera scompare ancora. Ed ora, se metto una checkbox, scrivo qualcosa, faccio invio, mi cancella quello che c'è scritto e rimane lì»

Poi:
> «Una notifica di Device Care di Android mi dice che notE sta frequentemente crashando. Randomicamente mi ha aperto sia l'app di Claude che quella di Firefox.
> Poi, ritornando alle checkbox: si comportano ancora troppo stranamente. Cosa sta succedendo? Come mai stanno dando così tanti problemi?
> 1) capita troppo spesso che se mi trovo in un blocco vuoto, uso backspace, invece che andarmi al blocco sopra dove si trova la checkbox, mi si chiude l'app
> 2) capita sempre che se tocco su un blocco dove già c'è una checkbox e dove c'è già scritto del testo, invece che a destra (ossia dopo l'ultima lettera/ numero del blocco) il text cursos mi va a sinistra, quindi all'inizio del blocco dopo la checkbox, cosa che non va bene, perché di consueto devo continuare a scrivere
> 3) se cancello una checkbox, capita che tutto sotto (per esempio il database che trovi sotto) scatta, scompare ricompare dopo qualche millisecondo
> 4) se io tocco invio quando mi trovo in un blocco dove c'è una checkbox, mi cancella quello che c'è scritto invece di andarmi a capo»

- **[Bug fix]** Tre difetti diversi delle caselle da spuntare — la
  maiuscola che faceva uno scatto da minuscola a maiuscola, le lettere
  che finivano nella riga di prima andando a capo in fretta, e l'app che
  si chiudeva **senza messaggio** — avevano **una sola causa**, trovata
  col registro: ogni casella è un campo di testo a sé, e andando a capo
  il fuoco si spostava sul campo nuovo. Nel log si vedeva a ogni Invio
  `onStartInputView restarting=true`, cioè **la tastiera che si stacca e
  si riattacca**. In quel momento la maiuscola automatica si rimette a
  zero (di qui lo scatto), i tasti già premuti arrivano al campo
  vecchio, e il campo appena lasciato viene distrutto mentre la tastiera
  gli sta ancora chiedendo dov'è il cursore — che è l'errore
  `LayoutCoordinate ... isAttached` con cui l'app spariva
- **[Bug fix]** La cura è controintuitiva: **la riga nuova nasce sopra,
  non sotto.** Va a capo mettendo il testo *prima* del cursore in un
  blocco nuovo inserito sopra, e lasciando al blocco su cui si sta
  scrivendo — che il fuoco ce l'ha già — il testo *dopo* il cursore.
  Sullo schermo è identico, ma **il fuoco non si muove mai**: niente
  cambio di campo, niente tastiera staccata, niente da distruggere.
  Misurato: in una raffica di sedici a-capo, **zero** cambi di fuoco, e
  le lettere tutte al posto giusto
- **[Bug fix]** La spunta e il "riparti da N" restano alla riga di
  sopra, che è la continuazione di quella che c'era; la riga nuova nasce
  non spuntata, come su Notion
- **[Bug fix]** L'indice del blocco viene **riletto dal database dentro
  la transazione** (`PageRepository.splitBlockAbove`), e le divisioni si
  mettono in fila con un `Mutex`. Prima usavo l'indice della copia in
  mano alla schermata: andando a capo in fretta era di un a-capo fa, e
  **nascevano due blocchi con lo stesso posto** — visto davvero nel
  database, non ipotizzato
- **[Bug fix]** **Il secondo a-capo di fila non veniva più fatto.**
  Effetto collaterale della cura, trovato subito dopo leggendo il
  database: il riconoscimento dell'"eco della tastiera" (vedi
  `SplitEcho`) è per campo, e ora che il campo sopravvive a tutte le
  divisioni restava armato. Scrivendo in fretta due righe che finivano
  con lo stesso testo, il secondo Invio veniva scambiato per un'eco e
  ignorato. Ora la memoria dell'eco si azzera appena si batte un
  carattere vero: un'eco arriva subito dopo la divisione e senza niente
  in mezzo, quindi quella memoria non serve più. Verificato: tre a-capo
  in raffica danno tre righe, `Z` `W` `Q`
- **[Bug fix]** **Un salvataggio di testo non sposta più il blocco.**
  `updateBlockSpans` risalvava l'**entità intera**, quindi rispediva
  anche l'`orderIndex` e la spunta che la schermata aveva in mano: dopo
  un a-capo quella copia è di un istante prima, e il primo carattere
  successivo rimandava il blocco al posto che nel frattempo aveva preso
  la riga nuova. Si vedeva come "scrivo, faccio invio, mi cancella
  quello che ho scritto". Ora scrive **solo la colonna del testo**
  (`BlockDao.setText`)
- **[Bug fix]** **La tastiera non sparisce più** cancellando una casella
  o andando a capo su una casella vuota, e **l'app non si chiude più**.
  Erano i due corni dello stesso problema: quel campo muore per forza (il
  blocco passa da riga a sé a testo scorrevole, che è un'altra
  composable), e distruggerlo mentre ha il fuoco fa chiudere l'app,
  mentre staccare la tastiera con `clearFocus()` la fa sparire e tornare.
  Misurati tutti e due: senza `clearFocus()` venti cicli su venti
  chiudevano l'app con `LayoutCoordinate ... isAttached`; con
  `clearFocus()` `ImeTracker` segnava `onHidden` e poi `onShown` 315 ms
  dopo
- **[Bug fix]** La terza strada è **il posteggio della tastiera**: un
  campo di testo alto un punto e invisibile, fuori dalla lista a
  scorrimento (dentro sparirebbe appena esce dallo schermo), su cui il
  fuoco si posa un istante prima che il campo vero venga distrutto. La
  tastiera resta aperta perché sotto di sé ha sempre un campo, e riparte
  sul blocco nuovo appena esiste. Verificato: gli stessi venti cicli non
  chiudono più l'app, e in una sequenza completa — scrivo, Invio,
  scrivo, Invio, backspace, scrivo — `ImeTracker` **non registra né
  `onHidden` né `onShown`**, cioè la tastiera non si muove mai
- **[Bug fix]** Il posteggio serviva **anche al campo del testo
  scorrevole**, non solo alle isole: riga vuota sotto una casella +
  backspace fonde la riga dentro la casella, la riga sparisce e con lei
  il suo campo. Era il caso più facile da incontrare e chiudeva l'app.
  `backspaceAtLineStartDetailed` ora dice *cosa* ha fatto, così il campo
  sa quando sta per morire. Verificato: dodici cicli, nessun crash
- **La causa di fondo, dichiarata**: ogni casella da spuntare è un campo
  di testo **a sé**, mentre i paragrafi ne condividono uno solo. Un
  blocco che passa da una specie all'altra cambia quindi campo, e un
  campo distrutto mentre ha il fuoco fa chiudere l'app per un difetto di
  Compose 1.6. Ogni punto in cui questo può succedere va protetto a
  mano, e finché l'architettura è questa se ne può sempre scoprire uno
  nuovo. Le due vie d'uscita vere sono **aggiornare Compose** (il
  difetto è corretto dalla 1.7) o **mettere anche le caselle dentro il
  campo condiviso** — che risolverebbe pure la selezione fra caselle,
  già in "Limiti noti"
- **Nota sul misurare**: `dumpsys input_method | grep mInputShown` **non
  basta** per dire se la tastiera sparisce: resta `true` anche mentre si
  sta chiudendo. Aveva fatto dichiarare risolto un difetto che c'era
  ancora. Il segnale onesto sono gli eventi `ImeTracker: onHidden` /
  `onShown` in `logcat`
- **Limite dichiarato**: un blocco con dei figli (un toggle) usa ancora
  la vecchia strada, riga nuova sotto e fuoco che si sposta. Spostando
  l'identità del blocco alla riga di sotto, i suoi figli finirebbero
  sotto la riga sbagliata
- **Nota onesta**: lo scatto della maiuscola e la chiusura improvvisa
  **non sono stati riprodotti a comando** — il primo perché iniettando
  il testo via adb la tastiera non entra in gioco e la maiuscola
  automatica non scatta, la seconda perché non è successa in nessuna
  delle raffiche provate. È stata tolta la causa comune, che invece è
  stata misurata; la conferma vera arriva usando l'app a mano

### 🆕 v0.46 — 22/09/2026 · 09:16 — Filtri su numeri e caselle, e la casella al centro della colonna
*(Claude Code sul PC)*

L'utente, parola per parola:
> «È collegato il telefono? Perdonami, non mi ricordavo che già ci fosse, ma tra i filtri non ci sono né Number né Checkbox. Sul primo, dammi anche la possiblità di fare un range di numeri, tipo da x a y dove in questo range ci possono essere dei numeri correlati a determinate pagine. Sulle Checkbox invece fai sì che mi mostri sia quelle spuntate che quelle non spuntate. Date e Select ci sono. Per quanto riguarda le cose scritte che per sbaglio cancelli, stai tranquillo, non sto ancora usando quest'app e quello che scrivo è solo come test. Anzi, se vuoi usa direttamente quello che ho già scritto io per fare le prove.
> EDIT: bug: se io tocco sulla parte della cella vuota di una checkbox, mi va a capo del database e mi fa il bug della toolbar rialzata di tanto dalla tastiera (vedi foto). Fai sì che se non tocco la checkbox in quella cella, non fa letteralmente nulla. Quando tengo premuto sulla proprietà delle Checkbox, metti anche un'opzione che mi consente di centrare la checkbox al centro perfetto, anche considerando se io aumento o diminuisco la larghezza della colonna»

Poi:
> «Questo è quello che succede (mi ero dimenticato lo screen)»

- **[Nuova funzionalità]** Si filtra su **numeri** (da x a y, estremi
  inclusi), **date**, **Select/MultiSelect** e **caselle da spuntare**.
  Restano fuori testo, indirizzo, mail e telefono: lì la domanda
  sarebbe "contiene queste lettere", cioè una ricerca, che è un'altra
  cosa e ancora non c'è
- **[Nuova funzionalità]** Sui **numeri** basta un estremo: solo "da"
  vuol dire "da lì in su", solo "a" vuol dire "fino a lì". Metà delle
  domande vere sono aperte da un lato ("sopra i 100"), e obbligare a
  scrivere un secondo numero inventato le renderebbe sbagliate. Una
  cella vuota non passa mai: non è zero, è "non c'è un numero"
- **[Nuova funzionalità]** Sulle **caselle** si scelgono *Checked*,
  *Unchecked* o tutte e due. Le celle mai toccate **non esistono nel
  database**: valgono "non spuntata", non "non lo so", altrimenti
  sparirebbero da tutti e due i filtri — il modo peggiore di sbagliare.
  Verificato sul telefono: *Checked* dà A, B, E, G e *Unchecked* dà
  C, F, Esempio, che sono esattamente i complementari letti in SQLite
- **[Bug fix]** **Il tocco sullo spazio vuoto di una cella non fa più
  niente.** Prima attraversava la tabella e arrivava alla pagina che le
  sta dietro, che lo intende come "scrivi qui": toccando accanto a una
  casella da spuntare il cursore finiva **sotto** al database e si
  apriva la tastiera con la barra della formattazione appesa in alto.
  Ora la cella si mangia i tocchi che non colpiscono niente; i figli
  vengono serviti per primi, quindi la casella continua a rispondere
  (verificato: `mInputShown=false` prima e dopo il tocco a vuoto, e lo
  schermo byte per byte identico)
- **[Nuova funzionalità]** Tenendo premuta l'intestazione di una
  colonna **a caselle** compare **"Center"**, che mette il quadratino
  al centro della colonna invece che appoggiato al bordo sinistro; la
  stessa voce diventa poi "Align left". Essendo un allineamento e non
  una misura, resta al centro **qualunque larghezza** abbia la colonna
- **[Nuova funzionalità]** L'opzione c'è solo per le caselle: è l'unico
  contenuto di cella che è un segno solo, dove l'allineamento si nota.
  Un testo o una data centrati renderebbero la colonna illeggibile,
  perché le righe non partirebbero più tutte dallo stesso punto
- Sta in `centerContent` di `database_columns`, **schema alla versione
  21**

### 🆕 v0.45 — 22/09/2026 · 01:46 — I filtri
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Perché hai iniziato a parlare inglese? Non che io non lo capisca, anzi, ma continua in italiano per cortesia. Ora, nuova aggiunta. Dammi la possibilità di filtrare in base a quello che è presente tra le proprietà con un'icona Filter nelle impostazioni e con un'icona a sinistra di quella Sort che fa la stessa cosa: il filtro mi concede di far vedere pagine che corrispondono solo a date precise che io scelgo (se sono di un giorno solo o più giorni), a Select o MultiSelect a mia scelta. Metti anche un'iconcina nella pagina del filtro con un cestino per far sì che io possa cancellare il filtro»

- **[Nuova funzionalità]** Le righe si possono **filtrare**: l'icona a
  sinistra di Sort nella barra della vista, e la voce **"Filter"**
  nelle impostazioni. Non sono due schermate gemelle ma **la stessa
  finestra** aperta da due posti: due copie si sarebbero separate al
  primo cambiamento, ed è il tipo di differenza che si nota solo
  usandole
- **[Nuova funzionalità]** Sulle **date** una pagina passa se i suoi
  giorni **toccano** quelli scelti, non se coincidono: un impegno dal 3
  al 7 compare filtrando il 5, altrimenti filtrare un giorno solo non
  mostrerebbe quasi mai niente. Le **date che si ripetono** contano
  tutte le loro volte, non solo la prima
- **[Nuova funzionalità]** Sui **tag** passa chi ne ha **almeno uno**
  fra quelli scelti, come il "contains any of" di Notion: chiederli
  tutti renderebbe inutile sceglierne più d'uno su una proprietà a
  selezione singola, dove nessuna riga potrebbe mai passare
- **[Nuova funzionalità]** L'icona è **accesa di blu** quando un filtro
  c'è: senza, una tabella con metà delle pagine sembrerebbe una tabella
  che ha perso delle pagine. Il **cestino** in alto a destra nella
  finestra compare solo quando c'è davvero un filtro da buttare
- **[Nuova funzionalità]** Il filtro vale per **tutte le viste**: la
  domanda "fammi vedere solo queste pagine" non cambia senso passando
  alla bacheca o al calendario, e un filtro che si spegne cambiando
  vista sarebbe un filtro di cui non ci si può fidare. Se la proprietà
  su cui si filtrava viene cancellata, il filtro smette di valere
  invece di nascondere tutto
- **[Nuova funzionalità]** Il filtro si salva sulla pagina
  (`filterColumnId` e `filterValue`, **schema alla versione 20**). Il
  valore usa lo stesso formato delle celle, quindi a leggerlo è la
  stessa funzione che legge una data: due copie non possono separarsi.
  Per questo `DateRange`/`parseDateRange` si sono spostate da
  `DatabaseViewScreen` a `data/entity/DateValue.kt`
- **Limiti dichiarati**: **un filtro per volta** su una proprietà sola,
  come l'ordinamento; Notion ne concatena più d'uno con "and"/"or"

### 🆕 v0.44 — 22/09/2026 · 01:39 — Colonne che si nascondono
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di decidere, tenendo premuto sul nome della proprietà di un database), se nascondere quella colonna o meno. Nelle impostazioni di quel database (tre lineette con i pallini) poi, mi metterai una voce chiamata "Property visibility", con una lista delle proprietà nascoste e lì deciderò quali rifar mostrare o no. Ovviamente dentro, mettimi due categorie, ossia 'Shown in table' con le proprietà visibili e 'Hidden in table' con quelle nascoste (come vedi nelle foto)»

- **[Nuova funzionalità]** **Tenendo premuta l'intestazione** di una
  colonna compare un menu con *Edit property* e **Hide**: la colonna
  sparisce dalla tabella, che si stringe di conseguenza
- **[Nuova funzionalità]** Nelle impostazioni della vista c'è
  **"Property visibility"**, col numero di colonne nascoste accanto.
  Dentro, le due liste **"Shown in table"** e **"Hidden in table"**
  come su Notion, ciascuna col suo *Hide all* / *Show all*; toccando
  una riga la si sposta nell'altra lista
- **[Nuova funzionalità]** **Nascosta non vuol dire cancellata**: i
  valori restano nelle celle (verificato leggendo il database prima e
  dopo), e la proprietà continua a comparire negli ordinamenti, nei
  raggruppamenti e nelle azioni di una riga. È solo una colonna in meno
  da scorrere in orizzontale, che su un telefono è il motivo per cui
  serve
- **[Nuova funzionalità]** Nascondere sta **sull'intestazione** e
  rimostrare **nelle impostazioni**, non per simmetria mancata: si
  decide di nascondere guardando la tabella, mentre una colonna
  nascosta non ha più un'intestazione da tenere premuta
- **[Nuova funzionalità]** "Name" non compare in nessuna delle due
  liste: è il titolo della pagina, non una proprietà, e una riga senza
  nome non si saprebbe più aprire
- **[Nuova funzionalità]** Lo stato sta nella colonna `hidden` di
  `database_columns` (**schema alla versione 19**), quindi vale per
  tutte le viste dello stesso database e sopravvive al riavvio
  (verificato: nascosta, app chiusa e riaperta, ancora nascosta)

### 🆕 v0.43 — 22/09/2026 · 01:23 — Le date che si ripetono
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Crea un effettivo sistema di task a ripetizione degli eventi o pagine nella proprietà Date sui database con anche le notifiche, come fa Google Calendar o Google Task, ossia ogni giorno/ settimana..., ogni 3 settimane/ 4 mesi..., ogni ultima domenica/ ogni secondo lunedì... (come vedi nelle foto)»

Poi:
> «Aspetta, le notifiche per il momento non le mettere»

Poi:
> «Mi sono anche dimenticato una cosa, perdonami: se io faccio che un evento si ripete per esempio ogni domenica, fai sì che quella stessa pagina creata sia la stessa (e sincronizzata) per tutte le domeniche successive»

- **[Nuova funzionalità]** Una data può **ripetersi**: dalla finestra
  della data, riga **Repeat**, con le scorciatoie di Google Calendar
  costruite sul giorno scelto — *Daily*, *Weekly on Sunday*, *Monthly
  on the first Sunday*, *Annually on...*, *Every weekday* — e una
  schermata **Custom** per tutto il resto: ogni N giorni/settimane/
  mesi/anni, in quali giorni della settimana, "il 21 del mese" oppure
  "il terzo lunedì", e quando finisce (mai / entro una data / dopo N
  volte)
- **[Nuova funzionalità]** **Una pagina che si ripete resta una pagina
  sola.** Non vengono create copie: la regola sta attaccata alla data,
  e il calendario la trasforma in tante comparse quante ne servono per
  il periodo che si sta guardando. Aprirne una qualsiasi apre quella
  pagina, e modificarla cambia tutte le volte — è la sincronizzazione
  che ci si aspetta, e l'unico modo per poter correggere "tutte le
  domeniche" con una modifica sola. Creare davvero una riga per ogni
  ripetizione riempirebbe il database di copie da correggere a una a una
- **[Nuova funzionalità]** Le occorrenze si calcolano **solo per il
  periodo mostrato**: una regola senza fine genererebbe date
  all'infinito, e di quelle del 2043 non se ne fa niente nessuno
  finché non ci si arriva
- **[Nuova funzionalità]** La riga fissa di ogni pagina nel calendario
  si calcola sulle date **di partenza**, non sulle comparse: quelle
  cambiano da un mese all'altro e farebbero ballare la riga a seconda
  di dove ci si trova
- **[Nuova funzionalità]** "Il terzo lunedì" e **"l'ultima domenica"**
  sono due cose diverse: quando il giorno scelto è l'ultimo del suo
  tipo nel mese la regola diventa "l'ultimo", perché chi dice l'ultima
  domenica intende sempre l'ultima, anche nei mesi che ne hanno cinque.
  Allo stesso modo "ogni mese il 31" cade l'ultimo giorno nei mesi
  corti
- **[Nuova funzionalità]** La regola si salva **accanto alla data**,
  come quarto campo del valore della cella (`RecurrenceCodec`): niente
  colonne nuove, niente migrazione, e le date scritte prima continuano
  a valere "capita una volta sola"
- **Limiti dichiarati**: le ripetizioni si vedono nel **calendario**,
  non ancora nella linea del tempo; e **le notifiche non ci sono** —
  sono un pezzo a sé, con permesso di sistema, allarmi e
  riprogrammazione al riavvio, ed è stato deciso di farle dopo

### 🆕 v0.42 — 22/09/2026 · 00:59 — Il menu "/"
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando scriverò /, mi apparirà in seguito ogni tipologia di testo che ti ho chiesto di creare per il campo di testo, ossia (qui ci scrivi la categoria BASIC BLOCKS) Text, Heading 1, Heading 2, Heading 3, Bulleted list, Numbered list, To-do list, Toggle list, Page, Callout, Quote, Table, Divider, Link to page, (qui ci scrivi la categoria MEDIA) Image, Video, Audio, Code, File, Web bookmark, (qui ci scrivi la categoria DATABASE) Table view, Board view, Gallery view, List view, Calendar view, Timeline View, Database inline, Database full page, Simple database (quello senza pagine che ti dicevo) e Linked view of data source. Le opzioni che ti ho detto di creare nella che ancora non ci sono, mettile comunque nella lista del / . Poi più in la ti chiederò di crearle»

- **[Nuova funzionalità]** Scrivendo **"/"** dentro il testo si apre il
  menu dei blocchi, diviso in **Basic blocks / Media / Database** come
  su Notion. Quello che si scrive dopo la barra **filtra la lista**
  ("/tab" lascia Table e Table view), e la barra col filtro sparisce
  dal testo appena si sceglie: è un comando, non roba scritta
- **[Nuova funzionalità]** **Le voci non ancora costruite ci sono
  lo stesso, spente**: Callout, Quote, Link to page, tutto il gruppo
  Media, Gallery view, Database full page, Simple database, Linked
  view of data source. Dicono cosa c'è e cosa manca senza costringere
  a cercarlo, e quando una verrà costruita basterà cambiarle l'azione.
  **È l'unico posto dell'app dove una voce inerte è meglio di una voce
  assente**: qui si sta scegliendo da un catalogo, non cercando un
  comando che dovrebbe esserci
- **[Nuova funzionalità]** Le voci delle viste database (**Table,
  Board, List, Calendar, Timeline view**) creano il database dentro la
  pagina e gli mettono subito quella vista: "Calendar view" dà un
  calendario, non una tabella da cambiare a mano. Verificato sul
  telefono
- **[Nuova funzionalità]** Il menu **non ruba il fuoco**
  (`focusable = false`): la tastiera resta aperta, perché il modo
  normale di usarlo è continuare a scrivere per filtrare
- **Limite dichiarato**: il menu "/" funziona nel testo scorrevole
  (paragrafi, titoli, elenchi), non dentro una casella da spuntare o
  un toggle, che hanno un campo di testo tutto loro

### 🆕 v0.41 — 22/09/2026 · 00:47 — Il calendario anche senza una proprietà Date
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando creo un database e decido di visualizzarlo in modalità Calendar, tu fammi comunque vedere un calendario anche vuoto (perché giustamente non ci sono ancora eventi con date create) in modalità Month con il mese corrente, come se fosse un calendario che si vede dopo aver messo pagina e data. Poi sarò io a creare una pagina toccando su un giorno del mese. Adesso, se seleziono Calendar dopo aver creato un database, mi dice quello che vedi in foto»

- **[Nuova funzionalità]** Scegliendo la vista Calendario su un
  database appena creato, **il calendario si vede lo stesso**: mese
  corrente, vuoto, con i suoi giorni. Prima al suo posto c'era un
  cartello che spiegava che serviva una proprietà data e offriva di
  crearla — corretto ma inutile, perché un calendario vuoto si capisce
  da solo
- **[Nuova funzionalità]** **La proprietà data la crea il primo
  tocco.** Toccando un giorno nasce lì la pagina, e con lei la
  proprietà "Date" se il database non ne aveva una — che viene anche
  fissata come quella del calendario, così aggiungendone un'altra
  domani la vista non cambia colonna sotto i piedi. Chi tocca un
  giorno ha già detto tutto quello che serve: vuole una pagina, in
  quel giorno. L'impalcatura è un problema nostro
- **[Nuova funzionalità]** Anche "+ New page" in fondo funziona senza
  proprietà data: crea la pagina datata oggi, e la proprietà con lei.
  La linea del tempo invece continua a chiedere la proprietà, perché
  lì una pagina senza date non ha nemmeno una barra da disegnare

### 🆕 v0.40 — 22/09/2026 · 00:39 — Il menu del numero, come su OneNote
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Qualche tempo fa, ti ho chiesto di resettare la lista se tengo premuto su un numero di una Numbered list. Ecco, ora se tengo premuto su quel numero, fai sì che appaiono queste opzioni che vedi in foto per modificare la lista come si fa su OneNote»

- **[Nuova funzionalità]** **Tenendo premuto sul numero** si apre il
  menu della lista, con le stesse quattro voci di OneNote: *Begin a new
  list here* (il conteggio riparte da 1 da lì), *Renumber as
  continuation of previous list* (toglie quel "riparti" e rimette la
  riga in fila), *Remove number* (la riga resta ma come paragrafo) e
  *Change to bullet*. Provate tutte e quattro sul telefono
- **[Nuova funzionalità]** La voce *Renumber as continuation* c'è
  sempre ma è **spenta quando non serve**, cioè quando quella riga non
  fa ripartire niente: toglierla del tutto farebbe ballare il menu fra
  un'apertura e l'altra, e una voce grigia dice anche *perché* non si
  può fare
- **[Cambiamento]** **Un tocco semplice sul numero non fa più
  ripartire il conteggio.** Era comodo da scoprire e scomodo da
  subire: rinumerava tutta la lista per un dito appoggiato male, senza
  chiedere niente. Ora quella è una voce del menu, dove la si sceglie
  apposta

### 🆕 v0.39 — 22/09/2026 · 00:28 — Raggruppare la tabella
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di creare gruppi in base alla proprietà che decido io: come vedi nelle foto (per farti capire al massimo), metti tutte le opzioni che vedi del caso collegate alle proprietà, tipo gruppi di più pagine dello stesso Select o MultiSelect. Sempre come fa Notion»

- **[Nuova funzionalità]** La tabella può **raggruppare le righe in
  base a una proprietà**, come su Notion: ogni gruppo ha la sua
  intestazione col valore e quante pagine contiene, la sua riga di
  intestazione delle colonne e il suo "+ New page". Si sceglie dalle
  impostazioni, voce **Group** (`tableGroupColumnId`, migrazione
  17→18)
- **[Nuova funzionalità]** Si può raggruppare per **quasi tutto**: tag
  singoli e multipli (l'intestazione è il tag colorato), caselle da
  spuntare (Checked/Unchecked), date (un gruppo per giorno), testo e
  numeri. Restano fuori "Creata il" e "Modificata il", che non sono
  celle ma proprietà della riga, e a istante esatto darebbero un
  gruppo per riga
- **[Nuova funzionalità]** Con la **selezione multipla** una riga
  compare in **tutti** i gruppi dei suoi tag: è quello che ci si
  aspetta da un raggruppamento, ed è anche il motivo per cui la
  bacheca non la sostiene — lì una scheda dovrebbe stare in due
  colonne insieme
- **[Nuova funzionalità]** **"+ New page" dentro un gruppo crea la
  pagina già con quel valore.** Verificato sul telefono: creata dentro
  il gruppo "PC", la riga nuova nasce col tag PC e resta lì, invece di
  finire fra quelle senza valore
- **[Nuova funzionalità]** Il gruppo **"senza valore" sta sempre in
  fondo** e si chiama "No <proprietà>": davanti ruberebbe il primo
  sguardo alle righe già smistate. L'interruttore **"Hide empty
  groups"** nasconde i gruppi senza pagine, ed è acceso di suo, come
  su Notion; compare solo quando un raggruppamento c'è
- **[Nuova funzionalità]** I gruppi si **chiudono e riaprono** toccando
  l'intestazione. Quale sia chiuso non si salva nel database: è come
  si sta guardando la tabella adesso, non una proprietà del database
- **[Nuova funzionalità]** Il raggruppamento della tabella è
  un'impostazione **diversa** da "Group by" della bacheca, di
  proposito: la bacheca senza colonne non esiste e quindi ripiega su
  una proprietà qualsiasi, la tabella invece funziona benissimo piatta
  e non deve rimescolarsi da sola. Cambiare vista non tocca l'altra
- **[Nuova funzionalità]** Un tag rimasto nelle celle dopo che
  l'opzione è stata cancellata **non fa sparire le sue righe**: gli si
  dà comunque un gruppo, in fondo a quelli dichiarati

### 🔧 v0.38.2 — 22/09/2026 · 00:03 — Copertine dritte, e la tastiera che non sparisce
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Primo bug: le copertine che metto dalla galleria mia vengono ruotate a 90 gradi verso sinistra.
> Secondo bug: quando cancello la check box, quando ne metto una nuova o quando faccio invio su una check box, la tastiera scompare. Può capitare che ne metto una nuova, mi sposta il cursore al blocco sopra senza che la tastiera scompaia.
> Terzo bug: non posso ancora selezionare tutto il testo tra varie check box.»

- **[Bug fix]** **Le copertine prese dalla galleria non escono più
  coricate.** Le foto scattate col telefono ruotato non vengono salvate
  ruotate: i pixel restano come li ha letti il sensore e dentro al file
  c'è un'etichetta (EXIF) che dice di quanto girarli. La galleria la
  legge, `BitmapFactory` no. Ora `PageImageStore` la legge e applica la
  rotazione alla copia che si guarda — il file non viene toccato.
  Provato fabbricando un JPEG con l'etichetta "ruota di 90" e
  guardandolo dentro l'app
- **[Bug fix]** **La tastiera non sparisce più** creando, cancellando o
  uscendo da una casella da spuntare. Erano due cose insieme: il
  cursore veniva richiesto sul campo nuovo solo `if (!imeVisible)`, ma
  subito dopo un cambio di fuoco la tastiera **risulta ancora aperta
  mentre sta sparendo**, quindi la richiesta veniva saltata e la
  tastiera se ne andava; e l'effetto che spegne il cursore quando la
  tastiera si chiude scattava anche mentre il cursore lo stavamo
  spostando noi. Ora la tastiera si chiede sempre (chiederla aperta non
  fa niente) e quell'effetto si ferma finché c'è una richiesta di fuoco
  in viaggio. Verificato con `dumpsys input_method`: `mInputShown=true`
  dopo l'Invio, dopo l'uscita dalla lista e dopo il backspace che
  toglie la casella

### 🔧 v0.38.1 — 21/09/2026 · 20:49 — L'Invio continua la lista anche partendo da zero
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Se scrivo 0., mi crea la lista da 0, scrivo qualcosa, faccio invio, ma non mi continua la lista come di consueto. EDIT: ho fatto caso che quando si blocca il telefono, lo sblocco, e riapro l'app, si apre la tastiera a caso e la toolbar è ancora troppo sopra la tastiera. Forse è perché stavi scrivendo te? Oppure è ancor buggata?»

Poi:
> «La lista dopo 0 ancora non continua»

- **[Bug fix]** **L'Invio continua la lista anche quando sotto ci sono
  righe vuote.** Scrivendo `0.` (o qualsiasi numero), poi il testo, poi
  Invio, la riga nuova usciva come paragrafo e la lista si fermava. La
  causa non era la velocità: una riga vuota inserita in mezzo ad altre
  righe vuote è **indistinguibile** dalle sue vicine, e il confronto
  attribuiva l'inserimento all'ultima riga del gruppo, facendo
  ereditare il tipo dal blocco sbagliato. Ora il campo dice anche dove
  sta il cursore. Trovato col log dopo una diagnosi sbagliata: la voce
  per esteso, con l'errore incluso, sta in "Strade già tentate"

### 🆕 v0.38 — 21/09/2026 · 20:33 — Caselle da spuntare: cancellarle e uscirne
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Se mi trovo sotto una check box, ed uso backspace per cancellare tutto quello che c'è sopra, non mi fa cancellare niente. Come se si trovasse completamente in alto, ma in realtà sopra c'è ancora roba. EDIT: le check box non si riescono a cancellare, ed ovviamente tutto quello che c'è sopra. EDIT 2: il contenuto delle check box non si può selezionare come se fosse testo. È lo stesso problema di qualche giorno fa, quando ti dicevo che la selezione del testo quando tengo premuto viene fatta solo su un blocco, e sopra o sotto si bloccava. Con le check box accade la stessa cosa. EDIT 3: il testo non è ancora perfettamente allineato al centro della check box. EDIT 4: se premo invio quando sto su una check box, crash l'app»

- **[Bug fix]** **L'app non si chiude più premendo Invio su una casella
  vuota.** Quella casella diventa un paragrafo (giusto, come Notion),
  ma così facendo cambia specie: da riga a sé a testo scorrevole, che
  per la UI è un posto diverso, e il campo di prima viene distrutto. Se
  succede mentre la tastiera è ancora attaccata a quel campo, lei
  continua a chiedergli dov'è il cursore e Compose 1.6 va in crash
  (`LayoutCoordinate operations are only valid when isAttached is
  true`). Ora il fuoco viene tolto **prima** che la riga sparisca, e
  rimesso subito dopo sul campo nuovo. Riprodotto in modo
  deterministico prima della correzione e riprovato quattro volte dopo
- **[Nuova funzionalità]** **Backspace a inizio casella toglie la
  casella** e lascia il testo, che diventa una riga normale — lo stesso
  gradino che il testo scorrevole fa già per gli elenchi. Da lì in poi
  la riga si fonde con quella sopra come tutte le altre: prima le
  caselle non si riuscivano a cancellare in nessun modo dalla
  tastiera
- **[Nuova funzionalità]** **Backspace a inizio della riga sotto una
  casella la fonde dentro la casella.** Prima lì non succedeva niente:
  sembrava di essere in cima alla pagina mentre sopra c'era ancora
  roba. Le due cose insieme fanno sì che si possa cancellare
  all'indietro attraversando le caselle, senza trovare muri
- **[Nuova funzionalità]** Per accorgersi del backspace a inizio riga,
  anche il campo delle caselle ha ora **l'a-capo nascosto in testa**
  (`RUN_LEAD`), lo stesso meccanismo del campo unito: la tastiera non
  manda eventi tasto intercettabili, ma se il backspace si mangia
  quell'a-capo il testo cambia, e un cambiamento si vede
- **[Bug fix]** **La pagina non salta più portando in cima la riga a
  fuoco.** Lo scorrimento automatico aggiunto per il blocco nato dietro
  la tastiera usava `animateScrollToItem`, che incolla la riga al bordo
  superiore: siccome una riga di testo scorrevole è alta quanto tutto
  il gruppo, dopo un Invio o una fusione le caselle sopra finivano
  fuori schermo e sembravano sparite (non lo erano: bastava scorrere,
  e nel database c'erano tutte). Ora si scorre **solo se la riga è
  tutta fuori**, e solo di quanto serve
- **[Bug fix]** Il quadratino è alzato di 4,5dp invece di 3: misurando
  ingrandendo lo schermo, il suo centro restava sotto il centro ottico
  delle maiuscole

### 🆕 v0.37 — 21/09/2026 · 20:09 — Un elenco numerato può partire da zero
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Di base, ogni numbered list deve partire da 1, ma se io manualmente dovessi scrivere 0. , fai sì che quella lista parta da 0, poi continua normalmente a 1, poi 2, eccetera»

- **[Nuova funzionalità]** **Un elenco può partire da zero.** Di suo
  parte da 1, ma scrivendo `0.` parte da 0 e poi prosegue normale: 0,
  1, 2. Serve quando c'è un passo zero — un preambolo prima del primo
  punto vero. Era un solo controllo a vietarlo
  (`numberedListPrefixJustTyped` rifiutava i numeri sotto l'uno): il
  resto reggeva già, perché il numero di partenza è un campo salvato
  sul blocco (`numberStartsAt`) e il conteggio riparte da lì.
  Verificato sul telefono e nel database: il blocco risulta salvato
  con numero di partenza 0, e le righe dopo sono `1.` e `2.`

### 🔧 v0.36.1 — 21/09/2026 · 19:48 — Caselle da spuntare: in riga col testo, con la maiuscola, e l'Invio non si blocca
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando scrivo delle check box, il testo non è allineato al quadratino. Fallo più piccolo e allineato con il testo. Inoltre, quando scrivo dopo le checkbox, la maiuscola non è automatica ad inizio frase. Altra cosa che riguarda le check box: quando faccio invio, scrivo a, faccio invio, scrivo a, capita che si bugga tutto, si chiude la tastiera e la toolbar sopra di essa si bugga come qualche giorno fa»

- **[Bug fix]** Il quadratino è **più piccolo e in riga col testo**.
  Era il `Checkbox` di Material, che porta con sé una zona di tocco da
  48dp e un quadrato da 20 che non si rimpiccioliscono: accanto a una
  riga alta 24 sembrava un bottone piazzato lì in mezzo. Ora è
  disegnato a mano (`CheckboxMark`), alto quanto le lettere, con la
  zona di tocco che resta larga. Va anche **alzato di 3dp**: centrarlo
  sulla riga non basta, perché dentro la riga il testo non sta in
  mezzo — sotto la linea di base c'è lo spazio per le code di g e p, e
  le lettere appaiono più in alto. Misurato sul telefono
- **[Bug fix]** **La maiuscola a inizio frase** ora arriva anche nei
  blocchi, caselle comprese: va chiesta alla tastiera
  (`KeyboardCapitalization.Sentences`) e non era mai stata chiesta.
  Verificato guardando la tastiera: a casella vuota mostra le lettere
  maiuscole e lo shift acceso
- **[Bug fix]** **Invio + scrivere in fretta non impasta più tutto in
  una riga sola.** Era il bug per cui, facendo Invio e scrivendo senza
  aspettare, le lettere finivano tutte attaccate nella riga di prima,
  la tastiera si chiudeva e la barra sopra restava in uno stato
  sbagliato. Tre cause diverse, trovate col log e sistemate una per
  una: la tempesta di ridisegni della rinumerazione, la riga nuova che
  nasceva dietro la tastiera e quindi non esisteva per il fuoco, e
  l'eco della tastiera che faceva dividere due volte lo stesso pezzo.
  Le prime due hanno la loro voce in "Strade già tentate"
- **[Bug fix]** **Invio su una casella vuota** la trasforma in
  paragrafo — come Notion — ma prima lasciava il cursore per strada e
  la tastiera si chiudeva in faccia: il blocco passa da "isola" a
  testo scorrevole, che per la UI è un altro posto, e il campo di
  prima viene distrutto. Ora il fuoco viene richiamato

### 🆕 v0.36 — 20/09/2026 · 10:58 — La copertina si sposta e si ingrandisce
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando metto un'immagine di copertina, dammi anche la possibilità di ingrandirla e spostarla sopra sotto destra e sinistra, sempre mantenendo la risoluzione base dell'immagine»

- **[Nuova funzionalità]** La copertina si **sposta e si ingrandisce**:
  dalla finestra della copertina c'è "Reposition", e da lì il dito la
  trascina in tutte le direzioni e la pizzicata la ingrandisce fino a
  quattro volte. In alto una riga dice come si fa — un'immagine che
  smette di rispondere al tocco senza spiegare perché sembra rotta — e
  in basso ci sono "Cancel" e "Save"
- **[Nuova funzionalità]** **Il file non viene mai toccato.** Nel
  database finiscono tre numeri (`coverScale`, `coverOffsetX`,
  `coverOffsetY`, migrazione 16→17): l'immagine resta intera alla sua
  risoluzione, e la striscia fa da finestra. Si può cambiare
  inquadratura all'infinito senza consumare niente, e togliendo
  l'ingrandimento si torna esattamente all'originale
- **[Nuova funzionalità]** Lo spostamento è salvato in **frazioni di
  striscia**, non in pixel: la stessa inquadratura vale su qualunque
  schermo, anche ruotando il telefono
- **[Nuova funzionalità]** L'inquadratura di prova vive nella
  schermata finché non si tocca "Save": scrivere nel database ad ogni
  millimetro di dito vorrebbe dire non poter più annullare, e far
  scrivere il disco sessanta volte al secondo
- **[Nuova funzionalità]** L'immagine **non si può trascinare fuori**
  dalla striscia: il limite è calcolato su quanto sporge davvero
  (`coverPanLimit`), perché lasciarla scappare vorrebbe dire restare a
  guardare un rettangolo vuoto
- **[Nuova funzionalità]** Cambiando copertina l'inquadratura **torna a
  zero**: era stata scelta guardando un'altra immagine, e su quella
  nuova darebbe un ritaglio deciso a caso
- **[Nuova funzionalità]** Ingrandita, l'immagine viene **letta più
  fine** (fino al doppio della larghezza dello schermo), altrimenti a
  ingrandimento 2 si vedrebbe sgranata. Il tetto lo mette
  `PageImageStore`, che non porta in memoria più di sei megapixel: una
  foto molto grande, al massimo dell'ingrandimento, può restare un po'
  morbida. È il prezzo per non far chiudere l'app

### 🆕 v0.35 — 20/09/2026 · 10:48 — Le icone della barra della vista più piccole
*(Claude Code sul PC)*

L'utente, parola per parola:
> «L'icona Open as full page mettimela a destra di Sort. E in generale falle tutte (e quelle future) un po' più piccole»

- **[Nuova funzionalità]** Le icone della barra della vista sono **più
  piccole** (20dp invece di 24, dentro un bottone da 40 invece che da
  48). La misura sta in `TOOLBAR_ICON_SIZE` e si usa attraverso
  `ToolbarIconButton`: le icone lì sopra devono restare tutte uguali
  fra loro, comprese quelle che si aggiungeranno, e riscrivere la
  misura a mano ogni volta è il modo sicuro perché prima o poi una sia
  diversa dalle altre

### 🔧 v0.34.1 — 20/09/2026 · 10:44 — Rinominare un database a schermo intero non si perde più
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Bug: se nascondo il titolo di un database, poi lo apro in full page, lo modifico e torno indietro, il titolo rimane quello di prima ossia Untitled, e se riapro in full page è ancora Untitled»

- **[Bug fix]** **Rinominare un database a schermo intero non si
  perde più tornando indietro.** La pagina era letta una volta sola
  all'apertura, e la stessa pagina sta aperta in due schermate insieme
  con due ViewModel diversi: quella rimasta nella pila di navigazione
  aveva in mano il titolo vecchio, e siccome ogni modifica salva la
  pagina **intera**, alla prima cosa che si toccava lì dentro — bastava
  riaccendere "Show title" — il titolo vecchio tornava nel database e
  la rinomina spariva. Verificato sul telefono che il nome nuovo veniva
  davvero salvato e poi sovrascritto, non semplicemente non salvato.
  Ora la pagina è **seguita nel tempo** (`PageDao.observeById`), in
  tutti e due i ViewModel: le due schermate vedono sempre la stessa
  cosa e nessuna delle due riscrive quello che aveva in mano

### 🆕 v0.34 — 20/09/2026 · 10:30 — "Show title" per i database dentro le pagine
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dammi la possibilità di mostrare o meno il titolo del database (tramite una voce nelle impostazioni, sempre l'icona delle tre lineette con i pallini) quando è visualizzato NON in full page in una pagina. Quando lo aprirò come full page, il titolo sarà obbligatoriamente presente»

- **[Nuova funzionalità]** **"Show title"** nelle stesse impostazioni:
  un interruttore che nasconde il titolo del database **quando è dentro
  una pagina**. Serve a chi il titolo l'ha già scritto sopra come
  intestazione e si ritrova a leggerlo due volte. La scelta è salvata
  per ogni database (`showEmbeddedTitle`, migrazione 15→16)
- **[Nuova funzionalità]** **A schermo intero il titolo c'è sempre**, e
  l'interruttore lì non compare nemmeno. Non è una dimenticanza:
  aperto da solo, il titolo è l'unica cosa che dice dove si è finiti, e
  poterlo togliere vorrebbe dire poter arrivare a una schermata senza
  nome. Verificato sul telefono: spento dentro la pagina, il titolo
  sparisce; aperto lo stesso database a schermo intero, è ancora lì

### 🆕 v0.33 — 20/09/2026 · 10:21 — Un menu solo per il database: Delete nelle impostazioni, "Open as full page" accanto a Sort
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ok, ma per il backup ne parliamo più in la, ora è presto, prima le cose fondamentali. Togli i tre puntini dal database e le opzioni Delete e Open as full page (con le icone) mettile nel simbolino delle lineette con i pallini»

Poi:
> «Metti l'icona Open as full page vicino a quella Sort»

- **[Nuova funzionalità]** Via i tre puntini accanto al titolo del
  database. **"Delete" è passato nelle impostazioni** — l'icona con le
  lineette e i pallini — in un gruppo a sé sotto quelle della vista:
  due menu diversi per le cose di uno stesso database erano un posto
  in più dove cercare, e il secondo conteneva due voci sole
- **[Nuova funzionalità]** **"Open as full page" è invece un'icona
  nella barra**, subito a destra di Sort. Le due cose sembrano simili ma non lo
  sono: aprire a schermo intero si fa spesso e di fretta, perché la
  tabella dentro la pagina sta stretta, mentre cancellare si fa una
  volta sola e non deve capitare per sbaglio. La prima merita un
  tocco, la seconda due. L'icona compare solo dove ha senso: a
  schermo intero il database è già aperto, e lì non c'è

### 🆕 v0.32 — 20/09/2026 · 10:17 — Ordinare le righe (Sort)
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Mi sono dimenticato, forse te ne ho già detta una senza farci caso. Mi riverrà in mente. Continuiamo con altre cose. Dammi la possibilità di ordinare in base alle proprietà presenti, quindi alfabeticamente se sono parole e numericamente se sono numeri o date, crescente e decrescente. Metti l'icona Sort come fa Notion come vedi nella foto»

- **[Nuova funzionalità]** Le righe si possono **ordinare per una
  qualsiasi proprietà**, crescente o decrescente, dall'icona **Sort**
  nella barra della vista — quella con le tre lineette che si
  accorciano, come su Notion. La scelta è salvata per ogni database
  (`sortColumnId` e `sortDescending`, migrazione 14→15)
- **[Nuova funzionalità]** Il confronto dipende dal tipo: i numeri si
  confrontano **come numeri** (altrimenti "10" verrebbe prima di "9"),
  le date come istanti, le date di creazione e modifica dalla riga
  stessa, e tutto il resto alfabeticamente senza distinguere maiuscole
  e minuscole. Verificato sul telefono con 9, 10 e 100: crescente dà
  9-10-100 e decrescente 100-10-9, non l'ordine alfabetico 10-100-9
- **[Nuova funzionalità]** Si può ordinare anche per **nome della
  pagina**, che è una colonna della tabella ma non una proprietà del
  database: ha un identificatore riservato (`SORT_BY_NAME`) che
  nessun id vero può avere
- **[Nuova funzionalità]** **Le celle vuote stanno in fondo in tutti e
  due i versi.** Non è una svista: invertendo l'ordine ci si aspetta di
  vedere in cima l'ultimo valore, non una fila di righe senza niente
  scritto
- **[Nuova funzionalità]** L'icona Sort è **accesa di blu** quando un
  ordinamento c'è: altrimenti non si saprebbe che le righe non sono
  più nell'ordine in cui sono state create. Il verso si cambia
  toccando l'ordinamento attivo dentro la finestra — sono due stati
  della stessa cosa, non due voci diverse

### 🆕 v0.31 — 20/09/2026 · 09:38 — Icone e copertine delle pagine
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Fai sì che io possa modificare icona e cover di ogni pagina da immagini che metto io tramite due pulsanti, Upload (che mi consente di aprire la mia galleria del telefono) e Link (dove posso mettere direttamente il link di un'immagine), persino del menu principale»

- **[Nuova funzionalità]** Ogni pagina può avere un'**icona** e una
  **copertina** scelte dall'utente, la pagina radice compresa. Si tocca
  l'icona (o la copertina, o "Add cover" quando non c'è) e si sceglie
  fra **Upload** e **Link**. Campi nuovi `iconImage` e `coverImage`
  (migrazione 13→14): senza immagine si continua a vedere l'emoji di
  sempre
- **[Nuova funzionalità]** Upload usa il **selettore di foto di
  sistema**, che non chiede il permesso di leggere tutta la galleria:
  l'utente sceglie una foto e l'app vede solo quella. Sul telefono si
  legge "notE avrà accesso solo alle foto che selezioni"
- **[Nuova funzionalità]** **Il collegamento viene scaricato una volta
  sola e il file resta sul telefono.** È la decisione importante di
  questa funzione: tenere l'indirizzo e scaricare l'immagine ogni volta
  avrebbe voluto dire un'app che senza rete mostra dei buchi, e che
  perde le immagini quando il sito le toglie. Così invece è una copia
  come tutto il resto. Il prezzo, dichiarato: se l'immagine cambia
  dall'altra parte, la nostra no
- **[Nuova funzionalità]** Per questo è comparso il permesso
  **INTERNET** nel manifest — l'unica cosa per cui l'app va in rete,
  solo quando si incolla un collegamento, mai da sola
- **[Nuova funzionalità]** Nel database si salva il **nome** del file e
  non il percorso intero: la cartella dell'app cambia posto fra
  un'installazione e l'altra, o al ripristino di un backup, e un
  percorso scritto nel database diventerebbe un puntatore a niente
- **[Nuova funzionalità]** Sostituire o togliere un'immagine
  **cancella il file vecchio**. Senza, la cartella si riempirebbe di
  immagini che nessuno può più raggiungere. Verificato: dopo aver
  tolto icona e copertina la cartella è vuota
- **[Nuova funzionalità]** Le immagini si caricano già rimpicciolite a
  quanto serve davvero, con la misura presa dal riquadro che le
  contiene. Una foto da dodici megapixel in memoria sono quarantotto
  megabyte: caricarla intera per mostrarla larga un centimetro farebbe
  chiudere l'app
- **[Nuova funzionalità]** Anche il blocco "collegamento a pagina"
  mostra l'immagine, se la pagina ne ha una: altrimenti la stessa
  pagina avrebbe due facce diverse a seconda di dove la si guarda

### 🆕 v0.30 — 19/09/2026 · 22:35 — Chiusa la tastiera, il cursore si spegne
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Prima di continuare, tre cose piccole
> 1) Fai sì che quando eseguo la gesture per andare indietro, mi si toglie il text cursor in qualsiasi parte del campo di testo o sui database»

- **[Nuova funzionalità]** Chiusa la tastiera, **il cursore si
  spegne** — nell'editor, nel titolo e nelle celle dei database, sia
  incorporati che a schermo intero. Prima il gesto indietro chiudeva
  solo la tastiera e lasciava il cursore a lampeggiare dentro il
  campo, come se si stesse ancora scrivendo lì. Il segnale non è il
  gesto ma la tastiera che si chiude, comunque la si chiuda: col
  gesto indietro la tastiera se lo mangia e all'app non arriva niente
  (vedi "Strade già tentate"). C'è un'attesa di 250 ms prima di
  spegnere il cursore, perché la tastiera Samsung sparisce e riappare
  per un istante durante i cambi di fuoco: se in quell'istante si
  togliesse il fuoco, si romperebbe l'Invio che passa da un blocco al
  successivo. Se la tastiera torna, l'attesa viene annullata. Provato
  scrivendo tre righe separate da Invio: nessuna si perde

### 🆕 v0.29 — 19/09/2026 · 22:33 — L'ora nella proprietà Date
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Prima di implementare icone e cover per ogni pagina, per poi fare la Gallery view, aggiungi anche l'ora (non per forza obbligatoria) alla proprietà Date, come fa Notion come vedi nelle foto»

- **[Nuova funzionalità]** La proprietà data può avere **anche l'ora**,
  come su Notion: un interruttore "Include time" nella finestra della
  data, spento di default. Acceso, compaiono "Start time" e — se c'è
  una data di fine — "End time". Spento, la data continua a valere il
  giorno intero, che è quello che serve alla grandissima parte delle
  pagine
- **[Nuova funzionalità]** Il valore della cella non ha cambiato
  forma: erano già millisecondi, e i millisecondi l'ora ce l'hanno
  dentro. È stato aggiunto un terzo campo, `t`, che dice se **conta**.
  Non si indovina dai millisecondi: indovinarlo vorrebbe dire dire "se
  non cade a mezzanotte allora c'è un'ora", e una pagina messa davvero
  a mezzanotte perderebbe la sua. I valori scritti prima non hanno il
  terzo campo e valgono "senza ora", che è quello che erano — nessuna
  migrazione necessaria
- **[Nuova funzionalità]** La finestra della data è diventata una
  finestra a scomparsa come quella di Notion, con i pulsanti in testa.
  In fondo, sotto al calendario e agli interruttori, finivano fuori
  schermo: si sceglieva una data senza poterla confermare. Stessa
  lezione della finestra delle proprietà. C'è anche **Clear**, che
  prima si otteneva solo confermando senza aver scelto niente
- **[Bug fix]** Il selettore di Material lavora in **mezzanotte UTC**,
  non locale, e il codice ne prendeva i millisecondi e li salvava
  così com'erano. In Italia non si notava — mezzanotte UTC è l'una o
  le due del mattino dello stesso giorno — ma a ovest di Greenwich la
  data si sarebbe spostata indietro di un giorno. Ora la conversione
  passa sempre per il giorno, in un senso e nell'altro
- **[Nuova funzionalità]** La linea del tempo colloca le barre
  all'**istante** e non al giorno: con l'ora accesa, a ingrandimento
  "ore" una pagina che comincia alle 14:30 sta a metà pomeriggio. La
  posizione è in giorni con la frazione, non in giorni interi. Una
  pagina senza ora continua a occupare il giorno intero, e una con
  l'ora ma senza fine è un istante — si vede lo stesso perché le
  barre hanno una larghezza minima
- **[Nuova funzionalità]** Anche l'ordine delle righe nel calendario e
  nella linea del tempo guarda l'istante: due pagine dello stesso
  giorno a ore diverse hanno un ordine naturale, ed è quello

### 🆕 v0.28 — 19/09/2026 · 22:15 — Sette ingrandimenti della Timeline
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Fai come fa Notion: metti la possibilità di cambiare in visualizzazione ad ore, giorno, settimana, bi settimana, mese, quatrimestrale e annuale. Penta annuale no»

- **[Nuova funzionalità]** **Sette ingrandimenti** come su Notion —
  Hours, Day, Week, Bi-week, Month, Quarter, Year — dalla stessa
  finestra ("Select a zoom level"), salvati per ogni database
  (`PageEntity.timelineZoom`, migrazione 12→13). Manca "5 anni":
  a quella scala un mese è largo due millimetri e non si distingue
  più niente. Il modello è uno solo per tutti: **la larghezza di un
  giorno**. Da lì discende tutto — dove comincia una barra, quanto è
  lunga, dove cade adesso — e le caselle dell'asse sono larghe quanti
  giorni durano, così un mese di ventotto giorni è più stretto di uno
  di trentuno e le barre restano incollate alle loro date invece di
  scivolare rispetto alle etichette
- **[Bug fix]** Agli ingrandimenti larghi i numeri dei giorni si
  leggevano sbagliati — "2" dove c'era scritto "25". La casella di un
  giorno è più stretta di due cifre e il testo veniva tagliato a metà.
  Ora si misura libero e sborda sulle caselle accanto, che tanto sono
  mute (`wrapContentWidth(unbounded = true)`), e il numero viene
  scritto una casella ogni tot invece che su tutte
- **[Bug fix]** A ingrandimento "Quarter" le etichette delle settimane
  si accavallavano: una casella da una settimana è larga 22 punti e
  "13 Sep" ne vuole il doppio. Ora portano solo il giorno, una
  settimana sì e una no; il mese lo dice l'intestazione ferma sopra
  l'asse
- **[Bug fix]** A ingrandimento "ore" la linea del tempo si apriva a
  mezzanotte, cioè mezza giornata lontano da quello che sta
  succedendo. Il bersaglio dell'apertura (e del pulsante Today) è
  l'**istante** di adesso, non la mezzanotte di oggi

### 🆕 v0.27 — 19/09/2026 · 21:55 — La vista Timeline
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Perfetto, a meno che ci siano altri bug che boh magari sarai in grado te di captare come prima senza che io me ne accorga, inizia la Timeline»

- **[Nuova funzionalità]** **Timeline**: un asse di giorni che scorre
  in orizzontale e una riga per pagina, con la barra lunga quanto i
  giorni che copre. È il calendario srotolato — lì il tempo va a capo
  ogni settimana e una pagina lunga si spezza in più pezzi, qui il
  tempo è una riga sola e la barra è intera, che è tutto il punto di
  una linea del tempo: confrontare a colpo d'occhio durate e
  sovrapposizioni
- **[Nuova funzionalità]** Le righe seguono **lo stesso ordine del
  calendario** — le pagine per data, la prima in alto, con le stesse
  regole per i pareggi — così passando da una vista all'altra non si
  perde il filo. Qui però ogni riga ha per forza la sua pagina, quindi
  non restano righe vuote
- **[Nuova funzionalità]** I nomi stanno in una colonna ferma a
  sinistra: scorrendo di mesi le barre se ne vanno, e senza i nomi
  accanto non si saprebbe più di chi è quella che si sta guardando
- **[Nuova funzionalità]** Il mese sta in un'intestazione ferma sopra
  l'asse e segue il primo giorno che si vede. Scritto sull'asse
  starebbe all'inizio del suo tratto e scorrendo di qualche giorno
  sparirebbe a sinistra, lasciando una fila di numeri senza sapere di
  quale mese sono. Legge la posizione dello scorrimento **dentro di
  sé**, così mentre il dito trascina si ridisegna solo quella scritta
  e non tutta la linea del tempo
- **[Nuova funzionalità]** I fine settimana si riconoscono dallo
  sfondo invece che da una lettera, e i giorni portano solo il numero:
  le caselle vengono composte tutte insieme, quindi ogni testo in meno
  è un testo in meno moltiplicato per la lunghezza dell'asse

### 🆕 v0.26 — 19/09/2026 · 20:14 — Today nel calendario, sempre al suo posto
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Funziona tutto. So che questa sarà una cazzata, ma vorrei chiedertelo comunque: vedo che quando sto alal settimana del giorno corrente, Today sparisce. Anche se non fa nulla, lascialo lì comunque per favore»

- **[Nuova funzionalità]** Pulsante **Today** a destra dei tre
  pulsanti della modalità: riporta a oggi da qualunque distanza, in
  tutte e tre. Sta lì **sempre**, anche quando si è già su oggi e non
  farebbe niente: era stato fatto comparire solo da lontano, ma un
  pulsante che compare e sparisce costringe a cercarlo ogni volta e fa
  cambiare aspetto alla riga da sola mentre si sfoglia. Scelta
  dell'utente, ed è quella giusta

### 🆕 v0.25 — 19/09/2026 · 20:07 — Ogni pagina ha la sua riga nel calendario, e la settimana non scatta più
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Vedi se riesci a entrare nell'app per vedere queste tre stranezze:
> 1) perché quando scorro la settimana la pagina che si vede scatta scomparendo e ricomparendo?
> 2) nella settimana della pagina 1, si vede che sta sopra perché è l'unica pagina, nella settimana dopo sta sotto, la pagina 2 sta sopra, nella settimana dopo sta sopra e la pagina 3 sta sotto. Vorrei che facessi sì che considerasse tutte le pagine presenti in quella visualizzazione e le mettesse in ordine di data (ossia la proprietà Date) dall'alto verso il basso
> 3) quando nella visualizzazione Table creo una pagina, poi creo la colonna con proprietà Date, e quando tocco sulla cella corrispondente a quella pagina sulla riga e alla proprietà Date sulla colonna, se non tocco sul - precisamente mi scrivi sotto al database. Vorrei che io potessi modificare la data semplicemente toccando ovunque sulla cella, non sul -»

Poi:
> «C'è ancora lo scattino dei rettangolini delle pagine quando scorro a destra o sinistra. Poi come vedi nellao screen dove ho fatto un disegnino per farti capire, vorrei che facessi così con le pagine: considera come se se fossero blocchi, quindi rispettando l'ordine di data dall'alto al basso, se una pagina ha finito la data, alle settimane successive nella stessa riga non ci deve essere niente»

Poi:
> «Lo scattino c'è ancora. Poi una domanda: quando dici "Quando una pagina finisce, la sua riga resta vuota nelle settimane dopo.", intendi in realtà all'infinito, oppure dopo tante settimane/ tanti mesi ritroverò una pagina nuova? perché è proprio questo che io vorrei, ossia che all'infinito quella riga diventasse occupata ed esclusiva alla pagina creata all'inizio su quella riga. Poi, un'aggiunta: nella visualizzazione Calendar, mi metti più a destra con un rettangolino curvato ai lati un 'Today', per saltare al giorno corrente se sto tanto lontano con le settimane/ mesi/ anni?»

Poi:
> «Lo scattino lo vedevo solo quando scorrevo a destra o sinistra nella visualizzazione Week. Vedevo che i rettangolini delle pagine scattavano, ossia sparivano e ricomparivano dopo pochissimi millisecondi, ma ora non c'è più (credo). Devo essere onesto poi, non penso mai che arriverò ad avere quaranta pagine, ma per quelle che mi servono vorrei che fosse fatto tutto bene. Fallo all'infinito per favore. Dopo che hai fatto riprovo gli scattini se ci sono, se non ci sono continuiamo con le altre modalità di visualizzazione di un database»

- **[Bug fix]** Cambiando settimana col dito le pagine sparivano e
  ricomparivano con uno scatto. Al termine dello scorrimento cambiano
  due cose — la settimana mostrata e l'azzeramento dello spostamento —
  e venivano scritte una dopo l'altra: potevano finire in due
  fotogrammi diversi, e in quello di mezzo si vedeva la settimana
  vecchia già al centro, o quella nuova ancora di lato. Ora le due
  scritture vengono applicate in un colpo solo
  (`Snapshot.withMutableSnapshot`), così non esiste un fotogramma che
  ne veda una senza l'altra. Lo spostamento è diventato uno stato
  normale invece di un `Animatable` proprio per poterlo fare: due
  valori si rendono veri insieme solo se sono tutti e due stati
  normali
- **[Bug fix]** Lo scatto però non era finito lì, e la causa vera era
  un'altra: **la molla dell'animazione rimbalzava**. Lanciata dalla
  spinta del dito superava la meta e tornava indietro, e superare la
  meta vuol dire scoprire il bordo oltre la settimana accanto, dove
  non c'è disegnato niente. Con `input swipe` non si vedeva, perché un
  trascinamento sintetico ha una spinta molto più debole di un dito:
  è per questo che le prime due volte sembrava a posto. Ora la molla è
  `DampingRatioNoBouncy` — non supera mai la meta — e il
  trascinamento è limitato a una settimana per parte, così nemmeno
  il dito può scoprire il vuoto. Verificato registrando lo schermo e
  leggendo il numero del primo giorno **fotogramma per fotogramma**:
  la sequenza sale monotona fino alla settimana nuova e si ferma lì,
  senza oltrepassarla e tornare indietro.
  **Nota per chi rilegge quelle registrazioni**: `screenrecord` chiude
  il file riemettendo un fotogramma vecchio, quindi l'ultimo
  fotogramma mostra spesso la settimana di partenza. È un artefatto
  della registrazione, non dell'app — succede in tutte e tre le
  registrazioni fatte, sempre in fondo
- **[Nuova funzionalità]** Le pagine del calendario si comportano come
  **blocchi impilati**: ordinate per data dall'alto verso il basso,
  ognuna con la sua riga, e **la riga non cambia mai**. Quando la data
  di una pagina finisce, la sua riga resta **vuota** nelle settimane
  successive invece di essere occupata da quella sotto — così le barre
  non si rimescolano e seguirne una con l'occhio è immediato.
  La riga viene decisa **una volta sola su tutto il database** e non
  settimana per settimana: è quella la differenza. Deciderla dentro la
  settimana vorrebbe dire farla dipendere da chi altro c'è lì, cioè
  farla cambiare appena una pagina entra o esce — ed era esattamente il
  difetto, con la stessa pagina in alto nella settimana in cui era sola
  e più in basso in quella dopo. L'ordine guarda la data della pagina,
  non il giorno in cui la barra entra nella settimana: una pagina che
  viene da prima resta sopra a una che comincia lunedì. A parità di
  data l'ordine è comunque sempre lo stesso, altrimenti due pagine
  dello stesso giorno si scambierebbero di posto ad ogni ridisegno
- **[Nuova funzionalità]** Le righe si contano su **tutto il
  database**, non sul periodo che si sta guardando: una pagina tiene la
  sua riga in ogni settimana, in ogni mese e in ogni anno, per sempre.
  È una scelta esplicita dell'utente, fatta sapendone il prezzo.
  Contarle sul periodo (come si faceva prima) le fa ricompattare ad
  ogni cambio di mese: nessuna pagina ruba la riga di un'altra, ma la
  stessa pagina si ritrova a un'altezza diversa a seconda di cosa c'è
  attorno. Contarle su tutto costa spazio vuoto — la decima pagina in
  ordine di data sta alla decima riga anche nelle settimane in cui è
  l'unica, con nove righe vuote sopra — e ogni settimana è alta quanto
  la griglia intera, perché la griglia delle righe è una cosa sola
- **[Nuova funzionalità]** Lo scorrimento della settimana è ora un
  `HorizontalPager`, lo stesso componente con cui si sfogliano le
  pagine ovunque su Android. Prima era scritto a mano: tre settimane
  affiancate, uno scostamento seguito col dito e una molla per farlo
  atterrare. Rifarsi la fisica dello scorrimento vuol dire rifare
  anche il lancio, l'aggancio alla pagina, il gesto afferrato a metà
  corsa e la velocità al rilascio — ognuna di quelle cose è un modo di
  sbagliare, e quella giusta esiste già. Le settimane ai lati vengono
  preparate prima che il dito si muova (`beyondBoundsPageCount = 1`),
  e il calcolo dei pezzi e delle loro righe è memorizzato invece di
  essere rifatto ad ogni fotogramma
- **[Bug fix]** Nella tabella, toccare una cella di tipo data apriva
  il calendario **solo centrando il trattino**, largo pochi punti: da
  qualsiasi altro punto della cella il tocco cadeva sulla pagina sotto
  il database, che rispondeva mettendosi a scrivere in fondo. La zona
  toccabile era il testo invece della cella. Ora è tutta la cella, con
  il margine laterale spostato **dentro** di essa in modo da non
  lasciare fuori nemmeno i bordi (`CellContent`, parametro
  `cellPadding`)

### 🆕 v0.24 — 19/09/2026 · 15:35 — La vista ad anno, e la settimana che si sfoglia col dito
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Funziona tutto alla grande, ottimo lavoro, anche i due bottoni Month e Week. Due cose:
> 1) puoi mettere a sinistra del bottone Month anche la visualizzazione Year?
> 2) nella visualizzazione Week, concedimi di scorrere con il dito orizzontalmente a destra e sinistra»

Poi:
> «Quando scorro a destra o a sinistra, mi fa mostrare tutto per un attimo e poi scompare tutto. Solo la prima volta però»

- **[Nuova funzionalità]** **Vista ad anno**, a sinistra di Mese e
  Settimana: dodici mesi in miniatura, tre per riga, tutto l'anno in
  una schermata. Niente barre — a un centimetro per mese non si
  leggerebbero — ma i giorni con almeno una pagina sono cerchiati, e
  basta a far vedere dove si concentra il lavoro. Toccando un mese lo
  si apre da vicino, che è l'unica cosa sensata da fare con un mese
  alto un centimetro
- **[Nuova funzionalità]** Le tre modalità hanno sostituito il vecchio
  sì/no `calendarWeekView` con `PageEntity.calendarMode`
  (migrazione 11→12), che travasa il valore precedente. La colonna
  vecchia **resta nel database**: SQLite sa cancellare una colonna solo
  dalla 3.35, più recente del minimo che l'app sostiene, e ricreare la
  tabella delle pagine per togliere un campo inutilizzato è un rischio
  che non vale il guadagno. Stessa scelta già fatta per
  `numberResetHere`
- **[Nuova funzionalità]** Nella settimana si passa da una all'altra
  **trascinando col dito**. Dentro una finestra larga una settimana ne
  stanno tre affiancate — la precedente, quella mostrata, la successiva
  — e il trascinamento sposta la fila, così sotto il dito si vede
  arrivare davvero la settimana accanto invece di vederla comparire di
  colpo al rilascio. Al rilascio, superato un quarto di schermo o con
  una spinta decisa, la fila finisce di scorrere e la settimana
  mostrata cambia; l'offset torna a zero nello stesso istante, quindi
  al centro resta lo stesso contenuto e non si vede nessun salto. Le
  tre settimane sono alte uguali apposta: se ognuna prendesse la
  propria altezza, trascinando si vedrebbe la riga crescere e
  rimpicciolirsi
- **[Bug fix]** Al primo trascinamento la settimana compariva per un
  istante e poi spariva tutto. Le tre settimane erano una `Row` larga
  tre schermi, e lì **due vincoli di larghezza si sono sommati**:
  dentro una `Row` un figlio senza peso viene misurato con *lo spazio
  che avanza*, e `Modifier.width` si lascia schiacciare dentro il
  vincolo che riceve — la prima settimana si prendeva tutta la
  larghezza e alle altre due ne restava zero. Passando a
  `requiredWidth` diventavano visibili, ma si scopriva il secondo
  problema: **una fila più larga del riquadro che la contiene non
  viene piazzata dove ci si aspetta**, risultava già spostata di una
  settimana per conto suo, e sopra ci si sommava lo scostamento del
  dito — così l'intestazione diceva una settimana e la griglia ne
  mostrava un'altra. Via la `Row`: ora le tre settimane sono
  sovrapposte nello stesso riquadro, ognuna larga esattamente quanto
  lui, e ognuna si sposta da sé di una larghezza a sinistra, zero, una
  a destra. Nessuna misura da contrattare e nessun piazzamento da
  indovinare. Stessa famiglia del vecchio bug delle linee della
  tabella invisibili: **quando una misura sembra sparire, è quasi
  sempre il contenitore che sta dando al figlio un vincolo diverso da
  quello che si immagina**

### 🆕 v0.23 — 19/09/2026 · 14:56 — Barre lunghe quanto i giorni, e la vista a settimana
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Funziona, ora fai le barre e la vista a settimana»

- **[Nuova funzionalità]** **Barre lunghe quanto i giorni coperti**.
  Ogni settimana calcola da sé quali pagine la attraversano e le
  dispone su corsie, le più lunghe in alto, in modo che due pagine
  sovrapposte nel tempo non finiscano una sopra l'altra
  (`calendarLanes`). Le larghezze sono **pesi, non misure**: una barra
  di tre giorni occupa tre settimi della riga su qualunque schermo. Una
  pagina a cavallo di due settimane produce due pezzi, e il lato dove
  prosegue viene disegnato squadrato invece che arrotondato
- **[Nuova funzionalità]** **Vista a settimana**, accanto a quella a
  mese (`PageEntity.calendarWeekView`, migrazione 10→11). Nel mese
  l'altezza di una settimana è fissa — sei settimane devono entrare
  nello schermo — e le barre oltre la terza diventano un "+N"; nella
  settimana l'altezza segue quante pagine ci sono davvero, perché lo
  spazio c'è e nasconderle sarebbe gratuito

### 🆕 v0.22 — 19/09/2026 · 14:42 — Date di inizio e di fine
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ora un'altra cosa: su Notion non solo puoi fare "Date started" e "Date finished", ma nella visualizzazione Calendar (con anche vista a settimana) le pagine rispettano la lunghezza dei giorni corrispondenti nella visualizzazione settimana»

- **[Nuova funzionalità]** **Intervalli di date**: una cella data può
  avere un inizio e una fine ("Date started"/"Date finished"), scelti
  con il selettore a intervallo di Material 3. Sono salvati come due
  numeri separati da un trattino nella stessa cella, così il tipo della
  colonna non cambia e le date singole già scritte continuano a
  leggersi

### 🆕 v0.21 — 19/09/2026 · 14:38 — La vista Calendar
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Per il momento facciamo finta che non sia successo niente. Ritorniamo all'app: fai la vista Calendar, con proprietà principale 'Date'»

Poi:
> «Nella visualizzazione Table e List, se faccio New page mi crea una pagina. Su Calendar e Board no, cioè me le crea ma non me le fa vedere, ma se cambio in visualizzazione Table o List vedo che sono state create correttamente»

Poi:
> «Non hai capito: se seleziono le modalità di visualizzazione Board o Calendar, se premo su New page sotto alla scritta "A calendar needs a Date property to place rows on. Add one from + Add property, then come back here". Se le pagine con la data le creo prima di visualizzare Calendar, me le fa vedere, ma se faccio il contrario no»

Poi:
> «Ok, ora funziona, ma dammi la possibilità di creare pagine toccando su un giorno del calendario»

- **[Nuova funzionalità]** **Calendar**: le pagine collocate nel giorno
  della loro proprietà data, mese per mese, con i giorni fuori mese
  smorzati e oggi evidenziato. La proprietà si sceglie da Settings →
  Date property (`PageEntity.calendarDateColumnId`, migrazione 9→10);
  se non è scelta si usa la prima data che c'è
- **[Bug fix]** In Calendar e Board `+ New page` sembrava non creare
  niente: la pagina nasceva senza data e senza tag, cioè **fuori da
  quello che si stava guardando** — la si ritrovava solo passando a
  Table. Ora nel calendario nasce datata oggi e il calendario torna al
  mese corrente per mostrarla; nella bacheca nasce già assegnata alla
  colonna
- **[Bug fix]** Quando la proprietà su cui la vista si regge non
  esiste, `+ New page` spariva e restava solo un messaggio: creava
  pagine invisibili. Ora il messaggio offre di creare la proprietà che
  manca (`MissingPropertyNotice`), e il pulsante torna appena c'è
- **[Nuova funzionalità]** Toccare un giorno crea lì una pagina già
  datata. I giorni stanno in uno strato sotto e le barre in uno sopra:
  gli spazi vuoti fra una barra e l'altra lasciano passare il tocco al
  giorno, mentre toccare una barra apre la sua pagina

### 🆕 v0.20 — 19/09/2026 · 14:11 — Il pulsante della vista sotto il titolo cambia la vista
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ottimo, funziona. Prima ti chiedo di darmi la possibilità di cambiare visualizzazione del database (Table, Board, List, e quelle nuove che metterai) anche sul pulsante sotto il titolo del database. Adesso c'è scritto Table, e se ci tocco non mi fa nulla. DopoAggiorna il README»

- **[Nuova funzionalità]** Il pulsante della vista sotto il titolo apre
  il selettore del layout (prima era un'etichetta inerte)

### 🔧 v0.19.1 — 19/09/2026 · 14:06 — Toccando sotto il database si scrive sotto, e il testo non finisce dietro la tastiera
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Fai sì che se tocco sotto il database (SOLO quando è in modaloità visualizzazbile in una pgina, non quando lo apro come Full page) in una pagina dove c'è altro contenuto, mi fa scrivere sotto il database. Adesso se tocco sotto, mi fa scrivere sopra»

Poi:
> «Funziona, ma c'è un problema: creo un database, tocco sotto, mi inzia a scrivere sotto, ma non viene visualizzato e se continuo a scrivere la schermata rimane bloccata come vedi nella prima foto, anche sein realtà sto scrivendo come vedi nella seconda foto. Così sono obbligato ad andare sotto, quando in realtà dappertutto mi attiva il focus subito su quello che scrivo. hai capito?»

Poi:
> «Non cambia nulla. Premo sotto il database, scrivo, e quello che scrivo rimane nascosto dietro la tastiera»

Poi:
> «Non solo continua a non funzionare, ma quando vado indietro con la gesture, e ritocco sotto, guarda come nella foto quanto spazio crea tra la barra e la tastiera»

Poi:
> «Niente, se scrivo sotto il database, quello che viene scritto rimane nascosto dietrro la tastiera e la pagina rimane ferma, non segue quello che scrivo»

Poi:
> «FUNZIONA!»

- **[Bug fix]** Toccare sotto l'ultimo blocco riportava a scrivere
  *sopra* di lui quando non era scrivibile (database, divisore,
  tabella, collegamento): si cercava l'ultimo blocco **scrivibile**
  invece dell'ultimo e basta. Ora ne viene creato uno nuovo dopo
- **[Bug fix]** Scrivendo in fondo a una pagina lunga il cursore
  finiva dietro la tastiera e la pagina restava ferma. Causa: un campo
  di testo che **cresce in altezza** invece di scorrere al proprio
  interno chiede di essere reso visibile solo quando prende il fuoco,
  non ad ogni spostamento del cursore — e il campo unito è alto quanto
  tutto il gruppo che contiene. Ora il campo calcola dove sta il
  cursore sullo schermo e chiede alla pagina di mostrare **quel punto**.
  **Tentativo fallito prima di arrivarci**: far scorrere la lista
  all'elemento che contiene il blocco. Non funziona perché tutto il
  testo scorrevole è un elemento solo: si finisce in cima a quello,
  cioè lontano dal cursore. In più quello scorrimento litigava con
  l'animazione della tastiera e lasciava un buco fra barra e tastiera

### 🆕 v0.19 — 19/09/2026 · 11:29 — I database dentro le pagine, con "Open as full page" e Delete
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Allora, ti spiego: io sto su una pagina, creo un database e mi apre direttamente il database come se fosse una pagina. Su Notion c'è un'opzione che mi permette di visualizzare il database non subito come pagina, ma come database visualizzabile e modificabile direttamente nella pagina (prima foto). Poi, se voglio vederlo come una pagina, fammi un'icona classica delle lineette con i pallini per aprire le opzioni e vederlo in Full page mode (toccando 'Open as full page' come nella seconda foto)»

Poi:
> «Se faccio un database mi appare la visualizzazione come ti ho chiesto e l'hai fatto bene, ma nell'icona delle impostazioni (dove c'è open as full page), mettimici anche Delete. Devo poter cancellarlo, no?»

- **[Nuova funzionalità]** Un database creato dal menu "+" **appare e si
  modifica dentro la pagina**, come un blocco fra gli altri, invece di
  aprirsi come schermata a sé. Il menu "..." accanto al suo nome lo apre
  a schermo intero o lo elimina. Il contenuto della schermata database è
  stato estratto in un unico pezzo (`DatabaseContent`) usato in entrambi
  i posti: due copie avrebbero significato correggere ogni cosa due volte
- **[Nuova funzionalità]** Eliminare un database incorporato cancella
  anche la pagina-database con colonne e righe. Diverso da un
  collegamento a pagina, dove il blocco rimanda a qualcosa che vive per
  conto suo: qui il blocco **è** quel contenuto, e lasciarne i dati
  vorrebbe dire lasciarli dove non si può più arrivare
- **[Bug fix]** Il database incorporato non scorre per conto suo: prende
  l'altezza che gli serve e lascia scorrere la pagina. Due aree che
  scorrono una dentro l'altra si rubano il gesto a vicenda

### 🆕 v0.18 — 19/09/2026 · 11:27 — Maiuscola automatica nei titoli, e il database largo quanto la pagina
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Due cose:
> 1) quando cancello un titolo, forse perché c'è scritto Untitled o forse perché voglio modificarlo, come vedi nella prima foto, quando sto per scrivere, non mi mette in automatico la prima lettera maiuscola. Fallo per favore
> 2) come vedi nella seconda foto, in ogni pagina o modo, fai sì che i database non abbiano lo spazio a sinistra come accade nella prima foto, ma fammeli attaccati a inistra come nella seconda foto»

- **[Nuova funzionalità]** Maiuscola automatica sui titoli (pagina,
  database, nome di una riga) e sulle celle di testo. Non su email,
  URL e telefono, dove sarebbe da cancellare ogni volta
- **[Bug fix]** Il margine laterale della pagina lo applicava la lista a
  tutti i blocchi: ora se lo mette ogni blocco, così il database può
  usare tutta la larghezza come a schermo intero (`PAGE_SIDE_PADDING`)

### 🆕 v0.17 — 19/09/2026 · 11:11 — Le proprietà Select diventano tag, come su Notion
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Facciamo così, per creare un Select, che sarebbe alla fine un Tag. Fai come Notion: creo la colonna Select, tocco sullo spazio (che deve essere vuoto, senza - o altri simboli) come nella prima foto, mi si apre una finestra (come nella seconda foto), non mettere 'Search for an option' in quella barra che metterai, ma 'Create a tag' e sotto 'Select an existing option' (per riselezionare i Tag già esistenti senza crearne altri)»

Poi:
> «Fai come fa Notion e come vedi nelle foto: quando tocco su un tag della colonna Select (prima foto), toccandolo mi si apre una finestra (seconda foto) con i tre pallini a destra di ogni tag, così posso modificare il colore (terza foto)»

Poi:
> «Scusa eh, domanda: ma perché mi appare Options (comam separated), e sotto 'Todo', 'In progress', 'Done'? Come fa? Non ho capito»

Poi:
> «Quando seleziono 'Select' o 'Multi-select' sotto 'Property name'  mi appare 'Tags are created directly in the cells of this column'. Toglilo»

- **[Nuova funzionalità]** Le proprietà a selezione sono diventate tag
  veri: pastiglie colorate, finestra dedicata con "Create a tag" e
  "Select an existing option", colori assegnati a giro (`TAG_COLORS`).
  Una cella vuota resta vuota, senza trattini

### 🆕 v0.16 — 19/09/2026 · 10:57 — La vista Board
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Ottimo, mi piace molto così. Bravissimo. Funziona, andiamo avanti col punto 4 con Board»

Poi:
> «Se creo la colonna Select, non mi fa scrivere nulla, e se tocco mi fa apparire un -»

Poi:
> «MERAVIGLIOSO! Una piccola cosa però, nella visualizzazione Board, le pagine senza un Select mettimele in fondo a destra. Prima quelle con un tag e poi quelle senza alla fine a destra»

- **[Nuova funzionalità]** **Board**: una colonna per ogni valore di una
  proprietà a selezione singola, righe come schede, colonna delle non
  smistate in fondo a destra, e `+ New` che crea la riga **già
  assegnata** a quella colonna. La proprietà si sceglie da Settings →
  Group by (`PageEntity.boardGroupColumnId`, migrazione 8→9); se non è
  scelta si usa la prima adatta. Solo selezione singola: con quella
  multipla una riga starebbe in più colonne insieme. **Non c'è ancora il
  trascinamento** delle schede tra colonne
- **[Bug fix]** Una colonna a selezione appena creata era un vicolo
  cieco: la cella mostrava solo un trattino e le opzioni si potevano
  scrivere unicamente alla creazione, in un campo che stava **sotto**
  l'elenco degli undici tipi e quindi fuori schermo. Ora si creano
  dalla cella, e il campo è sopra l'elenco

### 🆕 v0.15 — 19/09/2026 · 10:31 — I grigi di Notion dappertutto
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Dappertutto, mettimi il colore del background come nella prima foto. Invece, nelel finestre che appaiono, tipo quelle in Actions o quelle che appaiono che non coprono tutto lo sfondo che si aprono per esempio dal basso, metti il colore del background e il colore dei rettangolo di ogni opzione come quelli della seconda foto»

- **[Nuova funzionalità]** Tre grigi campionati dalle schermate di
  Notion: `#191919` per le pagine, `#202020` per le finestre che salgono
  dal basso, `#252525` per i riquadri che raggruppano le voci dentro
  quelle finestre
- **[Nuova funzionalità]** L'app è sempre scura e **non usa più i colori
  dinamici** (Material You). Erano una scelta deliberata, ma tingevano
  di viola o verde proprio gli sfondi che devono restare quei grigi
  precisi. Prima l'editor era nero fisso e il resto seguiva il telefono:
  metà app in un modo e metà nell'altro

### 🔧 v0.14.2 — 19/09/2026 · 10:22 — All'avvio subito la pagina, senza il lampo bianco
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Quando apro l'app, mi fa vedere uno sfondo bianco con l'icona dell'app al centro. Fai sì invece che quando la apro mi fa vedere direttamente il menu principale»

Poi:
> «Ora me lo fa nero lo sfondo. Vorrei che tu mi facessi immediatamente vedere la schermata principale. Mi fa ancora vedere l'icona dell'app»

Poi:
> «Non puoi farmi vedere direttamente il menu prinicipale?»

- **[Bug fix]** Lampo bianco all'avvio: il tema della finestra era
  `Theme.Material.Light`, e il suo sfondo è quello che Android usa per
  la schermata iniziale. **Quella schermata da Android 12 non si può
  disattivare**, si può solo farle prendere i colori dell'app: sfondo
  `#191919` e icona trasparente (`values-v31/themes.xml`). L'avvio a
  freddo misura 389 ms, quindi non c'è lentezza da nascondere

### 🔧 v0.14.1 — 19/09/2026 · 10:16 — La griglia della tabella, come un foglio di calcolo
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Due cose.
> 1) Nella visualizzazione principale quando creo un database, ossia Table, fai sì che vengano visualizzate lrighe e colonne della griglia, come se fosse un foglio Excel, come vedi nella foto (è orizzontale, ma è per farti capire meglio)
> 2) Invece che scrivermi Add property che sta in notE, mettimi semplicemente il + come vedi nella foto»

Poi:
> «Anche le righe, fai messo solo le colonne. Stessa cosa per gli spazzi sopra le righe (ossia dove ci sono scritte le proprietà di quella colonna)»

- **[Bug fix]** Le linee orizzontali della tabella erano invisibili:
  dentro un contenitore che scorre in orizzontale la larghezza
  disponibile è illimitata, e una linea che chiede "tutta la larghezza"
  ne ottiene zero. Ora la larghezza della tabella viene calcolata

### 🆕 v0.14 — 19/09/2026 · 10:09 — Le viste: Table e List, da Settings → Layout
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Funziona, andiamo avanti col punto 4 con Layout»

Poi:
> «Se faccio New property della colonna a destra di quella principale, mi apre il menu, seleziono per esempio Number ma non mi fa fare nulla, rimane lì impiantatro»

- **[Nuova funzionalità]** La vista si sceglie da Settings → Layout, con
  la griglia di riquadri come su Notion, ed è salvata per ogni database
  (`PageEntity.databaseLayout`, migrazione 7→8). Oltre a **Table** ci
  sono **List** (le righe senza griglia; si tocca e si apre la pagina) e
  **Board**
- **[Bug fix]** Nella finestra delle proprietà i pulsanti Cancel e Add
  stavano in fondo, dopo l'elenco dei tipi: si sceglieva un tipo e non
  si vedeva più come confermare. Ora sono in un'intestazione fissa. E
  con il nome vuoto Add non faceva niente **in silenzio**: adesso la
  proprietà prende il nome del tipo

### 🆕 v0.13 — 19/09/2026 · 09:58 — Il database un punto alla volta: impostazioni, azioni di una riga, Done nei titoli
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Allora, non ti spaventare. Facciamo un punto alla volta, e del quarto punto una visualizzazione alla volta (altrimenti sarebbe troppo lavoro e ho paura che potresti commettere troppi errori se facessi tutto insieme). Quando ne hai fatto uno, lo provo, vedo se va bene e passiamo al punto successivo, ok?
>
> 1) Toglimi il + in alto a destra su notE: aggiunge una riga e già posso farlo con + New Page. È ridondante
> 2) Al posto del + che toglierai, metti il simbolino a destra del + blu della prima foto per aprire le opzioni riguardanti quel database
> 3) Quando premo quel simbolino (che sarebbe quell'icon con due linee, che quella sopra ha il pallino a destra e quella sotto a sinistra sempre come nella prima foto), fai sì che mi appaia la finestra di settings come nella seconda foto
> 4) Aggiungimi la possibilità di cambiare la visualizzazione toccando su Table (come nella prima foto) o Layout (come nella seconda), mettendo Board, Timeline, Calendar, List, Gallery e Chart come nella terza foto. Ovviamente, queste visualizzazioni devono essere pertinenti alla modalità stessa di visualizzazione come su Notion in base alle proprietà principali, quindi:
>
> * Table: la classica visualizzazione di un database
> * Board: visualizzazione a colonne in base ad una singola proprietà (come nella quarta foto)
> * Timeline: visualizzazione classica alla Google Calendar ma con i blocchi  orizzontali che rispettano i giorni della settimana (come nella quinta foto)
> * Calendar: classico calendario
> * List: molto simile alla visualizzazione Databse, ma senza la griglia di una classica tabella (come nella sesta foto)
> * Gallery: visualizzazione di una sorta di rettangolo (decidiamo insieme come si può fare meglio) di ogni pagina a modalità galleria rispettando lo sfondo che metterò in quella pagina (come nella settima foto)
> * Chart: se vedi bene l'ottava foto, sotto Chart Type hai i quattro tipi di visualizzazione di un grafico classico anche di Excel»

Poi:
> «Ok, però mi devi dare la possibilità di cancellare le pagine. Fai sì che se tengo premuto su una pagina, mi vengono fuori le opzioni come in questa foto, così ne approfittiamo e mettiamo anche le altre opzioni che vedi»

Poi:
> «Due problemi: se tengo premuto sul nomeme fa vedere i classici pallino di selezione di un testo sul telefono, non mi appare nessun menu Actions. Poi, se tengo premuto su una parola, apro in alto a destra del databse Settings, premo Edit Properties, poi New Property, i pallini di selezione del testo rimangono come vedi nella foto»

Poi:
> «Quando creo una pagina (ossia una riga) mi scrive Untitled, se tocco non mi fa nulla, se tengo premuto mi fa vedere Actions con Edit property  e Delete. Se apro la pagina e la rinomino scrivnedo Esempio, mi fa modificare il nome ma non viene sincronizzato nel database. E infatti ritornando indietro mi fa ancora vedere Untitled, non Esempio»

Poi:
> «Funziona tutto, ma una Piccola cosa del punto 2 (che c'è sempre stata purtroppo): se faccio invio quando scrivo un titolo, fai sì che io vada direttamente a capo non del titolo ma nel campo di testo di quella pagina. Stessa cosa dell'andare a capo in un titolo nella visualizzazione principale di un database: non ha senso»

Poi:
> «Funziona. Posso chiederti di togliermi proprio la possibilità di fare Invio e mettimi Done quando scrivo qualsiasi titolo dappertutto?»

Poi:
> «Sì, metti anche lì Done. Il simbolino dell'invio quando scrivo un titolo non ha senso. Se voglio modificare qualcosa del corpo di una testo tocco direttamente il testo.»

- **[Bug fix]** Il tocco prolungato su una riga non apriva mai le
  azioni: il campo di testo della cella si prendeva il gesto per
  selezionare le parole, e restavano i pallini della selezione appesi
  sopra le finestre. Il nome è testo semplice finché non lo si tocca,
  così un unico rilevatore gestisce tocco e tocco prolungato
- **[Bug fix]** Il titolo della pagina di una riga e il nome della riga
  erano due valori scollegati: rinominando dall'interno, tornando
  indietro si ritrovava il nome vecchio. Ora sono allineati nei due sensi

### 🆕 v0.12 — 18/09/2026 · 23:14 — I database rifatti da capo, come quelli di Notion
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Perfetto, funziona. Ora andiamo sui databse, penso la parte che più mi interessa di tutto questo progetto. Come faccio a chiederti di crearmi in questa app il concetto dei databse uguale a Notion? Nella creazione, nell'ordine delle colonne, nell'utilizzo di ogni riga come pagina?»

Poi:
> «Cancella completamente tutto ciò che riguarda i database su notE. Rifalli daccapo seguendo la UI dei databse di Notion su teelfono come nella prima foto. Quando creo una colonna, fai sì che mi appaia una finestra come nella seconda foto con tutte le opzioni da fare per quella colonna»

Poi:
> «Metti anche Last edited time»

Poi:
> «Quando rinomino una pagina in un database, proibiscimi letteralmente di andare a capo. Non serve»

- **[Nuova funzionalità]** `DatabaseViewScreen` riscritta da zero
  seguendo la UI mobile di Notion: titolo grande e modificabile dentro
  il contenuto (non nella barra), riga con la vista attiva ("Table") e
  il pulsante per aggiungere una pagina, intestazioni con l'icona del
  tipo, `+ Add property` in coda alle colonne, `OPEN` su ogni riga per
  aprirla come pagina, `+ New page` in fondo. **Il magazzino dati non è
  stato toccato**: colonne/righe/celle con la riga collegata a una
  pagina *sono già* il modello di Notion, era la UI a non assomigliargli
- **[Nuova funzionalità]** Finestra delle proprietà a scomparsa dal
  basso, come su Notion: nome, elenco dei tipi con ricerca e icone, e —
  quando si modifica una colonna esistente — sposta a sinistra/destra
  ed elimina. Si apre toccando l'intestazione di una colonna o
  `+ Add property`. Prima una colonna creata era immutabile: sbagliare
  nome o tipo costringeva a rifarla perdendo i dati
- **[Nuova funzionalità]** Sei tipi di proprietà nuovi: Multi-select,
  URL, Email, Phone, Created time, Last edited time. Quelli mostrati da
  Notion e **non** realizzabili qui sono elencati nei Limiti noti
- **[Nuova funzionalità]** `updatedAt` sulle righe (migrazione 6→7) per
  "Last edited time": viene aggiornato sia cambiando il titolo sia
  modificando una cella. Ammette null, perché per le righe già
  esistenti la data vera non è ricostruibile — in quel caso si mostra
  la data di creazione
- **[Bug fix]** Una colonna aggiunta dopo averne eliminata una in mezzo
  riceveva un `orderIndex` già occupato (veniva contato il numero di
  colonne invece di prendere il massimo più uno). Due colonne con lo
  stesso indice non si possono riordinare: lo scambio non cambia nulla.
  Lo stesso difetto è ancora presente in `addRow`, dove per ora non si
  nota perché le righe non si possono riordinare
- **[Bug fix]** Cambiare il titolo di una riga riscriveva l'intera
  entità partendo dalla copia in mano alla UI: scrivendo mentre la
  pagina collegata veniva creata si poteva azzerare `linkedPageId`, e
  la riga perdeva la sua pagina. Ora il salvataggio tocca solo il
  titolo (stessa lezione del tocco sul numero negli elenchi)

### 🔧 v0.11.1 — 18/09/2026 · 22:51 — Backspace su una riga rientrata toglie un rientro per volta
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Perdonami, lo riscrivo:
>
> Un piccolo problema: quando scrivo 1., 2., 3., 4., sposto a destra 4. e diventa 3.1, poi a capo 3.2, 3.3, se io elimino 3.3 facendo backspace mi rimane sulla stessa altezza del 3.2, ma se rifaccio backspace non mi ritorna al paragrafo iniziale del 3., bensì sul 3.2. Dappertutto, se io rifaccio backspace mi ritorna alll átezza del 3.. Hai capito cosa intendo?»

- **[Bug fix]** Backspace a inizio di una riga rientrata (un `3.3` sotto
  un `3.2`) ora toglie **un rientro per volta** fino al margine, e solo
  allora fonde la riga con quella sopra: è il "gradino alla volta" di
  v0.8

### 🆕 v0.11 — 18/09/2026 · 22:43 — Elenchi numerati su più livelli: 1.1, 1.2
*(Claude Code sul PC)*

L'utente, parola per parola:
> «No no, un elenco con decimali mi andrebbe benissimo, tipo così
> 1.a
> 1.1ab
> 1.2ac
> 1.3ad
> 2.b
> 2.1ba
> 2.2bb
> Ovviamente fai sì che funzioni tutto anche quando sposto a destra quello che voglio spostare»

Poi:
> «Perché questi spazi enorme?»

Poi:
> «Perché se sposto a destra il 5.9 (quello che ho lasciato selezionato con la lineetta verticale), in realtà mi sposta il 5.8, ossia quello che è diventato nella foto il 5.7.1?»

Poi:
> «Perfetto, risolto. Mi sono però accorto di un'altra cosa: quando scrivo 1., mi crea la lista, scrivo a (sempre per esempio, per velocizzare il riconoscimento dei bug), vado a capo, e vedo come se il text cursor lampeggiasse all'inizio del nuovo blocco, ossia prima del 2 nel blocco dove si trova 2., ma proprio per un istante che però lo riconosco. Come mai?»

- **[Nuova funzionalità]** Numerazione su più livelli guidata dal
  rientro: `1.` al margine, `1.1` `1.2` spostando a destra, `1.1.1` a
  due rientri. Spostare un elemento lo rinumera, e con lui tutti quelli
  che seguono. Il conteggio tiene un contatore per livello: risalendo,
  quelli più profondi vengono dimenticati, ed è per questo che `2.1`
  riparte da 1 (`PageEditorViewModel.numberedListLabel`)
- **[Bug fix]** Spazi verticali enormi sopra e sotto le righe rientrate.
  Il rientro era fatto con un `ParagraphStyle`, ma applicare uno stile
  di paragrafo a una parte di un `AnnotatedString` spezza il testo in
  paragrafi separati, e l'a-capo che finisce al confine viene reso come
  una riga vuota in più. Ora il rientro sono spazi dentro il prefisso
  (`INDENT_SPACES`) e il testo resta un paragrafo solo
- **[Bug fix]** Il pulsante rientro (e sposta su/giù, ed elimina)
  agivano sulla riga precedente se premuti subito dopo un Invio: la
  riga nuova esiste nel campo prima di esistere nella lista dei
  blocchi, quindi non si riusciva ad associarla e il "blocco a fuoco"
  restava indietro di uno. Ora il calcolo viene rifatto anche quando la
  lista dei blocchi cambia, non solo quando si scrive
- **[Bug fix]** Per un istante dopo l'Invio il cursore lampeggiava al
  margine sinistro della riga nuova, prima di saltare dopo il numero.
  La riga esiste nel campo prima che il suo blocco arrivi dal database,
  quindi per un attimo veniva disegnata senza numero. Ora il prefisso
  delle righe non ancora salvate viene previsto, ereditandolo dalla
  riga sopra con la stessa regola di `updateRun`. È una previsione solo
  di ciò che si disegna: non tocca né i dati né l'editing, quindi nel
  caso peggiore sbaglia un numero per un fotogramma

### 🆕 v0.10 — 18/09/2026 · 22:24 — Toccare il numero fa ripartire l'elenco, anche nel testo
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Mi rimetti la feature che mi consente di resettare ad 1 un numero nella lista semplicemente otccando precisamente quel numero?»

Poi:
> «Toccando il quinto numero dall'alto, mi resetta perfettamente la lista, ma c'è un problema, come ben vedi: mi cancella quello che c'era scritto»

- **[Nuova funzionalità]** Toccare il numero di un elenco funziona anche
  nel testo unito, non più solo dentro i toggle: il numero è disegnato
  dalla trasformazione visiva, quindi gli si mette sopra una zona
  invisibile posizionata con il `TextLayoutResult`. Il calcolo dei
  prefissi è stato estratto in `runPrefixes` perché chi li disegna e chi
  li rende toccabili devono per forza essere d'accordo
- **[Bug fix]** Toccare il numero cancellava il testo di quella riga.
  `pointerInput` non si rigenera quando cambia solo il contenuto del
  blocco (la chiave era l'id), quindi la lambda del tocco teneva una
  copia dell'entità com'era alla nascita — vuota — e salvarla riscriveva
  sopra il testo. `toggleNumberReset` ora prende l'id e aggiorna solo le
  colonne della numerazione. **Regola generale: non catturare entità
  dentro una lambda che non si ricompone; passare l'id e rileggere.**

### 🆕 v0.9 — 18/09/2026 · 22:14 — Scrivere "1." a inizio riga crea un elenco numerato
*(Claude Code sul PC)*

L'utente, parola per parola:
> «Aggiungi la possibilità che io scriva il numero 1, un punto (ossia questo . ) subito dopo, ed automaticamente mi crea una lista numerata. Ovviamente, se per un motivo X decido di partire dal numero 5, potrò scrivere 5, poi il punto, e mi crea la lista»

Poi:
> «La mia richiesta è specifrica in una situazione, ossia quando lo scrivere 1. è all'inizio del blocco. Se io scrivo 1., 2. eccetera dopo la primissima lettera/ numero di un blocco, non voglio creare una lista numerata. Hai capito?»

- **[Nuova funzionalità]** Scrivere `1.` (o `5.`, o qualsiasi numero) e
  basta, a inizio blocco, crea un elenco numerato che parte da quel
  numero. Scatta solo se il numero è l'unica cosa scritta nel blocco:
  `abc 1.` non converte niente, come richiesto
- **[Nuova funzionalità]** Elenchi numerati che partono da un numero
  scelto: nuovo campo `numberStartsAt`, che sostituisce il vecchio
  `numberResetHere` (era il caso particolare "riparti da 1")
- **[Nuova funzionalità]** Prima migrazione vera dello schema (5→6) e
  rimozione di `fallbackToDestructiveMigration`. Da qui in poi i dati
  sul telefono sopravvivono ai cambi di schema

### 🔧 v0.8.1 — 18/09/2026 · 22:02 — L'Invio va a capo e la lista continua
*(Claude Code sul PC)*

L'utente, parola per parola:
> «La prima volta ho fatto numbered, ho scritto a, sono andato a capo e non mi è andato a capo, ma è rimasto nel punto primo. Sono riandato a capo ed è andato a capo. Facendo backspace ha cancellato tutto, ma rifacendo tutto daccapo, rifaccio numbered, scrivo a, vado a capo e non mi crea il secondo numero. Non mi continua la lista»

Poi:
> «Uguale a prima. Faccio numbered, scrivo a, vado a capo, mi crea il 2., ma non mi va a capo e se volessi scrivere un a, rimarrebbe al punto 1., facendo sì che ce ne siamo 2 di a. Sarebbe una cosa del genere:
>
> 1. aa
> 2. a
> 3. a
>
> Ma io la seconda del punto 1. non l'ho voluta scrivere lì, ma andando a capo in realtà non mi va a capo»

Poi:
> «Non mi cancella il primo numero»

Poi:
> «L'app mi si è aperta con il 1. già scritto. Cosa faccio?»

Poi:
> «FUNZIONA TUTTO»

- **[Bug fix]** Il primo carattere scritto dopo un Invio finiva sulla
  riga precedente. Causa: l'area "tocca lo spazio vuoto sotto l'ultimo
  blocco per scrivere" usava `Modifier.clickable`, che in Compose si
  attiva **anche col tasto Invio** quando un elemento interno ha il
  focus. Ogni a-capo faceva quindi partire anche `focusLastBlock()`,
  che dopo 60 ms riportava il cursore sull'ultimo blocco *noto al
  momento della chiamata* — cioè quello vecchio, perché il blocco nuovo
  non era ancora stato salvato. Ora quell'area usa `detectTapGestures`,
  che risponde solo a tocchi veri. Diagnosticato leggendo il log sul
  dispositivo: l'Invio risultava corretto (cursore a 2), e 79 ms dopo
  arrivava una richiesta di focus che lo riportava a 1

### 🆕 v0.8 — 18/09/2026 · 21:19 — Backspace a inizio riga smonta la riga un gradino alla volta
*(Claude Code sul PC: la prima cosa fatta sul progetto arrivato
dall'altra chat, scelta dall'utente fra i «Limiti noti» del README)*

L'utente, parola per parola:
> «Ciao, posso metterti qui il pacchetto dell'app che stavi facendo in Home?»

E poi:
> «@"C:\Users\Utente\Downloads\NotionLocal/"
> Come ti invio la cartella? Così va bene, o te lo mando zippato?»

- **[Nuova funzionalità]** Backspace a inizio riga "smonta" la riga un
  gradino alla volta invece di fonderla subito con quella sopra, come
  in Notion: prima toglie il marcatore di elenco, poi toglie un rientro
  per volta fino al margine, e solo allora fonde. Vale **anche per il
  primo elemento** — il vecchio limite in cima ai "Limiti noti" non
  c'è più. Riconosciuto senza intercettare tasti: sulle righe dopo la
  prima perché sparisce l'a-capo esattamente dov'è il cursore, sulla
  prima riga perché sparisce il `RUN_LEAD` e il cursore finisce a zero,
  posizione altrimenti irraggiungibile. La decisione sta nel ViewModel
  (`backspaceAtLineStart`), non nella UI, perché solo lì il gruppo di
  blocchi è quello aggiornato

### 🆕 v0.7 — 17/09/2026 · 22:37 — L'icona dalla foto dell'utente e il nome "notE"
*(nell'altra chat, «Home»: la data e l'ora sono quelle dei file
dell'icona e del nome dell'app)*

- **[Nuova funzionalità]** Icona adattiva da foto dell'utente; nome
  app cambiato in "notE"

### 🔧 v0.6.2 — fra l'8 e il 18/09/2026 · ora non registrata — Sfondo nero e barra dello stesso colore
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Bug fix]** Sfondo nero fisso con testo e cursore bianchi
- **[Bug fix]** Barra sopra la tastiera dello stesso colore dello
  sfondo, invece di un grigio che creava una cucitura visibile

### 🔧 v0.6.1 — fra l'8 e il 18/09/2026 · ora non registrata — Altri bug fix: divisore, tabelle, menu "+"
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- Crash quando il focus finiva su un blocco divisore
- Ridimensionare una tabella cancellava il testo già scritto
- Ciclo di apertura/chiusura continua del menu "+"

### 🆕 v0.6 — fra l'8 e il 18/09/2026 · ora non registrata — Database: il calendario per le date, le righe come pagine
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Nuova funzionalità]** Selettore di data vero (calendario)
- **[Nuova funzionalità]** Apertura di una riga come pagina
- **[Nuova funzionalità]** Eliminazione colonne con tocco prolungato

### 🆕 v0.5 — fra l'8 e il 18/09/2026 · ora non registrata — Tastiera e interazione: Invio, backspace e tastiera Samsung
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Bug fix]** Modifiche scartate silenziosamente quando la lista di
  blocchi nota alla UI era obsoleta — causa profonda di Invio da
  premere due volte, liste che non continuavano, tipo ereditato a caso
- **[Bug fix]** Invio che non andava a capo al primo colpo
  (risincronizzazione col database troppo aggressiva)
- **[Bug fix]** Tipo di blocco ereditato dal blocco sbagliato premendo
  Invio; titoli che si propagavano alla riga successiva
- **[Bug fix]** Cambio tipo dal menu "+" applicato al blocco sbagliato
  mentre si scriveva
- **[Bug fix]** Backspace su blocco vuoto: due tentativi falliti di
  intercettarlo come tasto, poi rimosso — resta il pulsante nella barra
- **[Nuova funzionalità]** Tocco ovunque sotto il titolo per scrivere
- **[Bug fix]** Caratteri duplicati e tastiera che si chiudeva da sola
  (mitigazioni per la tastiera Samsung)
- **[Bug fix]** Selezione più fluida: niente salvataggio su database ad
  ogni movimento della selezione

### 🆕 v0.4 — fra l'8 e il 18/09/2026 · ora non registrata — Aa, Undo/Redo, numerazione e rientri
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- **[Nuova funzionalità]** Formattazione inline "Aa"
  (grassetto/corsivo/sottolineato/barrato), poi estesa al testo unito
- **[Nuova funzionalità]** Undo/Redo
- **[Nuova funzionalità]** Reset della numerazione nelle liste
- **[Nuova funzionalità]** Indenta/disindenta e sposta su/giù

### 🆕 v0.3 — fra l'8 e il 18/09/2026 · ora non registrata — Struttura e navigazione: la radice è una pagina, il "+" nella barra
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

- Rimossa la Home come schermata separata: la radice è ora una pagina
  normale e modificabile
- Rimosso il pulsante "+" flottante — sostituito dai blocchi
  Page/Database nel menu "+" della barra
- Menu "+" ridisegnato come griglia a icone su due colonne

### 🆕 v0.2 — fra l'8 e il 18/09/2026 · ora non registrata — Il testo in un campo solo: ripensato il modello di editing
*(nell'altra chat, «Home», fra l'8 e il 18/09/2026: di queste modifiche
non restano né la data né l'ora né l'ordine, e sono nell'ordine in cui
le elencava il README del 18/09)*

**[Nuova funzionalità] Ripensato il modello di editing del testo** — il
cambiamento più grande dello sviluppo. Vedi "Decisioni di design
deliberate" sopra per il contesto completo.

### 🆕 v0.1 — 08/09/2026 · 09:19 — Nasce notE
*(nell'altra chat, «Home»: la data e l'ora sono quelle dei primi file
del progetto, `build.gradle.kts` e `settings.gradle.kts`)*

- **[Nuova funzionalità]** **notE**: un'app di note e database in stile
  Notion per Android, con **tutti i dati salvati sul telefono** (SQLite
  via Room): nessuna rete, nessun account, nessun cloud. Il nome
  interno del progetto è NotionLocal

## Struttura del progetto

```
app/src/main/java/com/gabriele/notionlocal/
├── data/           # Entità Room, DAO, Repository, AppDatabase
├── ui/
│   ├── screen/     # PageEditorScreen, DatabaseViewScreen, SearchScreen
│   ├── theme/      # Colori, tipografia, tema Material 3
│   └── navigation/ # Routes e NavHost
├── viewmodel/      # Un ViewModel per schermata + ViewModelFactory
└── MainActivity.kt

app/src/test/java/com/gabriele/notionlocal/
└── data/           # Test automatici senza telefono (dal 24/09/2026:
    │               # TextStatsTest, il conteggio del testo)
    └── repository/ # PageRepositoryTest: spostare, duplicare, buttare,
                    # Annulla e Ripristina, l'indice della ricerca nei
                    # database, su un database vero (Robolectric + Room)
app/src/test/java/com/gabriele/notionlocal/viewmodel/
                    # DatabaseSearchTest: quali righe tiene la ricerca
                    # dentro un database e quali illumina;
                    # LinkedViewModelTest: una vista collegata vera
app/src/test/java/com/gabriele/notionlocal/data/entity/
                    # RichTextScriptTest: pedice e apice nel testo salvato
app/src/test/java/com/gabriele/notionlocal/data/widgets/
                    # WidgetsTest: le barre dell'avanzamento, l'ordine
                    # ovest-est, il pomodoro che avanza, il salvataggio
app/src/test/java/com/gabriele/notionlocal/ui/
                    # WidgetsRenderTest: disegna i widget e salva
                    # l'immagine in app/build/widget-screenshots/;
                    # ScriptRenderTest: i pulsanti pedice e apice
app/src/test/java/com/gabriele/notionlocal/data/MigrationTest.kt
                    # la migrazione 28→29 aperta da Room come fa l'app
app/src/test/resources/schema/v28.sql
                    # lo schema della versione 28, per MigrationTest

app/src/main/res/font/            # I 10 font occidentali delle pagine (sosia liberi)
app/src/main/res/values/font_certs.xml   # Certificati per scaricare i font da Google
app/src/main/assets/font_licenses/OFL.txt # Licenza e copyright dei font inclusi
```

`ui/theme/PageFonts.kt` è il catalogo dei font delle pagine: nomi,
sosia, gruppi, da dove arrivano, e `PageTypography`, che porta font e
corpo a tutto il testo.

`data/widgets/` sono i widget della barra laterale: `Widgets.kt` com'è
fatto ognuno e i calcoli (barre, ordine dei fusi, pomodoro), senza
Android; `WidgetStore.kt` dove si salvano e l'attesa che fa suonare il
pomodoro. `ui/screen/SidebarWidgets.kt` è come si vedono.

`data/patchnotes/PatchNotes.kt` e `ui/screen/PatchNotesScreen.kt` sono
**Settings → Patch Notes**: la Cronologia di questo README divisa in
versioni e disegnata come su GitHub — l'elenco dei titoli
(`PatchNotesScreen`) e la pagina di ogni versione (`PatchNoteScreen`). Il testo lo copia il task
`patchNotes` di `app/build.gradle.kts` (vedi "Nota tecnica").

`ui/screen/BlockActionsSheet.kt` è il menu di un blocco (i sei puntini):
le voci per una riga di testo, per una pagina e per un database, e tutte
le finestre che apre. Chi lo usa passa solo l'id del blocco.

**File chiave da capire per primi**:
- `PageEditorViewModel.updateRun` — il cuore della logica di modifica
  del testo unito: confronta righe prima/dopo e decide se è modifica,
  divisione o fusione
- `PageEditorScreen.MergedTextRunField` — il campo condiviso e la sua
  sincronizzazione col database
- `MergedRunVisualTransformation` + `MergedRunOffsetMapping` — resa dei
  prefissi (• e numeri) e mappatura fra posizioni reali e mostrate.
  **Questa mappatura è il punto più delicato del progetto**: un errore
  qui sposta il cursore o la selezione in modo difficile da
  diagnosticare. Attenzione: il testo vero comincia a 1, non a 0, per
  via del `RUN_LEAD` — è il parametro `leading` della mappatura
- `DatabaseContent` — il database, usato sia a schermo intero sia
  dentro una pagina. Le uniche differenze le decide `embedded`: il
  titolo più piccolo (e nascondibile, vedi `showEmbeddedTitle`), la
  voce "Delete" nelle impostazioni, e il non scorrere per conto suo
- `backspacedLineStart` + `PageEditorViewModel.backspaceAtLineStart`
  — riconoscimento del backspace a inizio riga e rifiuto della fusione
  finché la riga ha ancora un marcatore o un rientro da perdere

## Nota tecnica

Lo schema è alla **versione 52** (la 52, del 06/10/2026, aggiunge
`blocks.attrsJson`, le voci in più dei blocchi nuovi del menu "/" — vedi `BlockAttrs`; la 51, del 05/10/2026, aggiunge
`pages.thenSortColumnId` e `thenSortDescending`, la seconda regola di ordinamento; la 50, del 30/09/2026, aggiunge
`database_rows.eventColor`, il colore degli eventi del calendario; la 49, dello stesso giorno, aggiunge
`pages.colorBoardCards`, "Color cards" delle bacheche; la 48, dello stesso giorno, aggiunge
`pages.wrapCells`, oggi il testo lungo nelle celle delle tabelle (`PageEntity.cellOverflow`: 0 tre puntini, 1 a capo, 2 colonna allargata); la 47, del 29/09/2026, aggiunge
`database_columns.durationFormat`, la proprietà Duration; la 46, dello stesso giorno, aggiunge
`pages.nameColumnOrder`, il posto della colonna Name; la 45, dello stesso giorno, aggiunge
`database_rows.parentRowId` e `subRowsCollapsed`, le sotto-righe; la 44, dello stesso giorno, aggiunge
`pages.columnWidths` e `fitColumns`, la larghezza delle colonne della tabella; la 43, dello stesso giorno, aggiunge
`pages.viewHostId`, `viewName` e `activeViewId`, le viste di un database; la 42, dello stesso giorno, aggiunge
`pages.hideEmptySubGroups`, l'interruttore dei sottogruppi vuoti; la 41, dello stesso giorno, aggiunge
`pages.tableSubGroupColumnId`, i sottogruppi della tabella; la 40, del 28/09/2026, aggiunge
`pages.chartSettings`, le impostazioni della vista Chart; la 39, del
26/09/2026, aggiunge i
template delle pagine; la 38, dello stesso giorno, aggiunge il
formato della data e dell'ora delle colonne Date; la 37, dello stesso
giorno, aggiunge l'allineamento delle celle; la 36, dello stesso giorno, aggiunge le
impostazioni delle colonne Number e i calcoli delle viste; la 35, dello
stesso giorno, aggiunge i
quaderni: le tabelle `notebooks` e `notebook_sections` e
`pages.notebookSectionId`; la 34, dello stesso giorno, aggiunge
`pages.isProtected`, le pagine bloccate con la password; la 33, del
25/09/2026, aggiunge la tabella `ink_strokes` dei disegni a mano; la 32, dello stesso giorno,
aggiunge `blocks.notifyAt`, l'ora delle righe "/notification"; la 31, dello
stesso giorno, sposta font e
corpo dalla pagina al testo, senza cambiare la forma delle tabelle; la
30, dello stesso giorno, toglie MS YaHei
e aggiunge l'inquadratura delle schede della galleria; dalla 25 alla 29 vengono dalla
sessione cloud del 24/09/2026, vedi Diario: migrazioni simulate su
SQLite in cloud e **provate su un telefono vero la sera stessa**, da
un database alla versione 24 con note di prova, vedi Cronologia
"Ripresa sul PC del 24/09/2026"; la 27 non cambia la
forma delle tabelle, trasforma i titoli in paragrafi; la 28 aggiunge il
database semplice; la 29 le viste collegate, `sourceDatabaseId` e
`viewHiddenColumnIds` in `pages` — questa è anche **provata da un test
automatico con Room**, `MigrationTest`, che apre un database vero alla
versione 28 come fa l'app e fa controllare a Room lo schema che ne
esce), e
da qui in avanti **ogni cambio di
schema vuole una migrazione vera** in `AppDatabase`. Fino alla 5 c'era
`fallbackToDestructiveMigration()`, che ad ogni cambio ricreava il
database da zero perdendo tutto: accettabile finché sul telefono
c'erano solo dati di prova, non più ora che ci sono note vere. La
scorciatoia è stata tolta insieme alla migrazione 5→6, quindi un campo
aggiunto senza la sua migrazione fa rifiutare l'avvio all'app — un
errore rumoroso, molto meglio delle note cancellate in silenzio.

**Le Patch Notes dell'app vengono da questo README** (dal 30/09/2026). A
ogni compilazione il task `patchNotes` (`PatchNotesTask` in
`app/build.gradle.kts`) copia la sezione "Cronologia degli
aggiornamenti" — dal suo titolo fino al `## ` dopo — negli asset
dell'app come `patch_notes.md` (con `addGeneratedSourceDirectory`, così
Gradle sa che va rifatta quando il README cambia). Dallo stesso README
vengono `versionName`, il numero del primo titolo "### … v0.N — " (la
versione più recente), e `versionCode`, quanti titoli di versione ci
sono. Quindi: **una voce nuova nel README è una versione nuova
dell'app** alla prossima compilazione. `PatchNotesTest` legge l'asset
come fa l'app e lo confronta col README.

Prima di installare una build che cambia lo schema conviene portarsi
via una copia del database dal telefono:

```
adb exec-out run-as com.gabriele.notionlocal cat databases/notion_local.db > backup.db
```

(su Windows va fatto passare da `cmd /c`, perché la redirezione di
PowerShell rovina i file binari; servono anche `-wal` e `-shm`. La
redirezione **sul telefono**, dentro `adb shell`, produce invece un
file vuoto: il processo dell'app non può scrivere su `/sdcard`).

Dopo l'installazione conviene verificare che la migrazione sia
avvenuta davvero e non di nascosto ricreando il database: nei byte
60-63 del file c'è `user_version` (deve essere il numero nuovo), e la
colonna aggiunta deve comparire **in fondo** al `CREATE TABLE` che sta
in `sqlite_master` — se fosse in mezzo, o fra apici inversi come le
altre, vorrebbe dire che la tabella è stata rifatta.

### Un backup si legge senza il file `-shm`

**Quasi tutto quello che c'è nel database sta nel `-wal`, non nel
file principale.** L'app viene chiusa uccidendo il processo ad ogni
installazione, quindi SQLite non fa quasi mai il travaso (checkpoint):
il `.db` può contenere solo la pagina radice del primo avvio e tutto
il resto vivere nel giornale.

Il `-shm` è l'indice di quel giornale. Se si apre una copia del
database **con il `-shm` che è stato copiato insieme**, SQLite si fida
di quell'indice, dichiara che il giornale ha zero frammenti validi e
**mostra un database quasi vuoto** — senza errori e senza avvisi.
Sembra che il backup sia da buttare, o peggio che i dati siano andati
persi.

Quindi: copiare tutti e tre i file, ma per leggere il backup tenere
solo `.db` e `-wal`, **cancellando il `-shm`**. Così SQLite ricostruisce
l'indice dal giornale e i dati tornano tutti.

Un database che dopo questa accortezza risulta comunque quasi vuoto lo
è davvero — succede normalmente, perché i database di prova vengono
cancellati dall'app appena finito di provarli.
