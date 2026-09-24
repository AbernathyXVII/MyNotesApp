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
  con sessione e pausa. Una freccetta nasconde tutta la sezione, e la
  scelta resta
- **Database dentro le pagine**: creandone uno dal menu "+" compare
  nella pagina e si modifica lì; l'icona con le frecce lo apre a
  schermo intero, e dalle impostazioni lo si elimina
- **Database** con la schermata modellata sulla UI mobile di Notion:
  titolo modificabile, tabella scorrevole, `+ Add property` in coda
  alle colonne, `OPEN` per aprire una riga come pagina vera,
  `+ New page` in fondo. Toccando un'intestazione si apre la finestra
  della proprietà (nome, tipo con ricerca, elimina; dal 24/09/2026 non
  più "sposta", che sta nel menu dell'intestazione tenuta premuta). Undici
  tipi: Text, Number, Select, Multi-select, Date, Checkbox, URL,
  Email, Phone, Created time, Last edited time
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
  telefono in verticale). Si crea anche dal menu "/" con "Gallery view"
- **Data con ora facoltativa**: l'interruttore "Include time" nella
  finestra della data aggiunge l'orario all'inizio e alla fine. Spento,
  la data vale il giorno intero
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
  ("Upload") o da un **collegamento** ("Link"). Il collegamento viene
  scaricato una volta sola e il file resta sul telefono
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
- **Font e dimensione del testo per ogni pagina** *(sessione cloud del
  24/09/2026, provati sul telefono il 24/09/2026)*: nella barra Aa, prima di B,
  la voce del font (col nome in uso) e quella della dimensione (col
  numero in uso), come su OneNote. Sedici font in tre gruppi —
  occidentali, cinesi, giapponesi — che sono **sosia liberi** dei font
  commerciali chiesti; dimensione da 5 a 72, di partenza 16, **scritta
  a mano** in una finestrella. Valgono per tutta la pagina
- **Niente titoli (Heading)** dal 24/09/2026: tolti dall'app su
  richiesta dell'utente, ora che la dimensione del testo si sceglie per
  pagina. Quelli che c'erano sono diventati testo normale
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
- **Date: niente formato della data, fuso orario e promemoria**, che
  Notion ha nella stessa finestra. Formato e fuso qui seguono il
  telefono; il promemoria vorrebbe le notifiche, che sono un lavoro a
  sé
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
- **Immagini da collegamento: fino a 20 MB.** Un collegamento può puntare a qualunque cosa, e senza tetto una pagina da mezzo giga riempirebbe il telefono mentre l'utente aspetta
- **Font: nessuno è quello vero.** Helvetica, Arial, Calibri, Cambria,
  Consolas, Georgia, Verdana, Times New Roman, Futura, Garamond e MS
  YaHei sono commerciali: il telefono non li ha e l'app non li può
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
- **Widget: il pomodoro non suona ad app chiusa.** Conta giusto lo
  stesso (riaprendo è al punto giusto), ma per suonare con l'app chiusa
  servirebbe un allarme di sistema, col suo permesso ("Sveglie e
  promemoria") e una notifica. Con l'app in secondo piano suona finché
  Android non la chiude
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
- 6 ⏸️ copertine: serve una foto dell'utente o un link (pagina senza
  copertina → riquadro vuoto ✅)
- 7 🟡 testo, elenco numerato, casella, toggle ✅; mancano spoiler ed
  elenchi rientrati o partiti da un numero scelto
- 8 🟡 Small 3, Medium 2, Large 1, altezze uguali, ultima riga non
  allargata ✅; manca la rotazione
- 9 ✅ — 10 ✅
- 11 🟡 ordinamento ✅; filtro non provato
- 12 ⏸️ servono molte schede con copertina
- 13 ✅
- 14 ⏸️ da chiedere all'utente
- 15 🟡 ordine delle voci, grigie agli estremi, spostamento, Edit
  property senza Move ✅; mancano colonna nascosta in mezzo, tabella
  raggruppata, Lock view
- 16 ✅ (valori controllati a mano); giapponese, cinese e coreano solo
  dai test automatici
- 17 🟡 ✅ salvo caselle, tabelle, collegamenti, grassetto/corsivo
  simulati, modalità aereo
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
  sigla "CEST" che la voce si aspettava (da guardare). Mancano: menu
  degli orologi, scelta del fuso, elenco, date proprie e colori,
  rinomina, pomodoro a fine fase (suono) e ad app chiusa, tema chiaro
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

Le voci nate nelle sessioni cloud stanno qui in cima, la più recente
per prima, e portano scritto che **vanno ancora verificate sul
telefono**: quando lo sono, si aggiunge "(verificato sul telefono il
gg/mm/aaaa)" accanto al titolo.

**Ripresa sul PC del 24/09/2026 — il lavoro cloud provato sul
telefono** *(Claude Code sul PC, S25 Ultra in debug wireless, build
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

**Pedice e apice nella barra Aa** *(verificato in parte sul telefono
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

**Widget nella barra laterale — fusi orari, avanzamento, contatore,
pomodoro** *(verificato in parte sul telefono il 24/09/2026: voce 29)*
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

**Viste collegate — lo stesso database in un'altra pagina**
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

**Ricerca dentro un database — la lente accanto a Sort**
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

**Annulla e Ripristina anche per Move to, Duplicate e Move to trash**
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

**Move to — l'albero con la ricerca** *(verificato in parte sul
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

**Il menu del blocco — i sei puntini** *(verificato in parte sul
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

**Database semplice — le righe sono solo testo** *(verificato in parte
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

**Menu "+" — le stesse voci del menu "/", nello stesso ordine**
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

**Pagine — la dimensione si scrive a mano, e via i titoli (Heading)**
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

**Pagine — font e dimensione del testo (barra Aa)** *(verificato sul
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
  | MS YaHei | Noto Sans SC | scaricato |
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

**Pagine — il conteggio del testo ("X words")** *(verificato sul
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

**Colonne — spostarle tenendo premuta l'intestazione** *(verificato in
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

**Database — vista Gallery** *(verificato in parte sul telefono il
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

**[Nuova funzionalità] Ripensato il modello di editing del testo** — il
cambiamento più grande dello sviluppo. Vedi "Decisioni di design
deliberate" sopra per il contesto completo.

**Struttura e navigazione**
- Rimossa la Home come schermata separata: la radice è ora una pagina
  normale e modificabile
- Rimosso il pulsante "+" flottante — sostituito dai blocchi
  Page/Database nel menu "+" della barra
- Menu "+" ridisegnato come griglia a icone su due colonne

**Editing e formattazione**
- **[Nuova funzionalità]** Formattazione inline "Aa"
  (grassetto/corsivo/sottolineato/barrato), poi estesa al testo unito
- **[Nuova funzionalità]** Undo/Redo
- **[Nuova funzionalità]** Reset della numerazione nelle liste
- **[Nuova funzionalità]** Indenta/disindenta e sposta su/giù

**Database — ordinamento**
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

**Database — un menu solo**
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
- **[Nuova funzionalità]** Le icone della barra della vista sono **più
  piccole** (20dp invece di 24, dentro un bottone da 40 invece che da
  48). La misura sta in `TOOLBAR_ICON_SIZE` e si usa attraverso
  `ToolbarIconButton`: le icone lì sopra devono restare tutte uguali
  fra loro, comprese quelle che si aggiungeranno, e riscrivere la
  misura a mano ogni volta è il modo sicuro perché prima o poi una sia
  diversa dalle altre

**Icone e copertine delle pagine**
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

**Gli elenchi puntati cambiano segno a ogni rientro**
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

**Gli elenchi numerati cambiano modo di contare a ogni rientro**
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

**Duplicate: si sceglie dove va la copia, e la copia è davvero una copia**
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

**Icone per le pagine dei database**
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

**La barra laterale, e con lei preferiti, albero, ricerca, cestino, avvio e impostazioni**
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

**Il menu "/" filtra davvero (e il divisore non fa più saltare la pagina)**
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

**Gli scatti dentro i toggle (Invio e backspace)**
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

**Icone anche sui database**
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

**La copertina sale fin sotto la barra del telefono**
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

**Turn into page / Turn into database**
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

**I toggle, rifatti (e un difetto grave nel salvataggio)**
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

**Come si chiude il menu del "+"**
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

**Colore del testo e colore dietro al testo (il pennello)**
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

**Il menu dei tre puntini di una pagina**
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

**Updates: la cronologia delle modifiche**
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

**Preferiti (solo la base, nessun elenco ancora)**
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

**Testo coperto (spoiler)**
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

**La formattazione sopravvive alla scrittura**
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

**Copertina e titolo scorrono con la pagina**
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

**La barra sopra la tastiera, rifatta**
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

**La barra della formattazione non se ne va più a mezz'aria**
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

**Le caselle da spuntare entrano nel campo di testo condiviso**
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

**L'Invio dentro una casella non sposta più il fuoco**
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

**Filtri**
- **[Nuova funzionalità]** Le righe si possono **filtrare**: l'icona a
  sinistra di Sort nella barra della vista, e la voce **"Filter"**
  nelle impostazioni. Non sono due schermate gemelle ma **la stessa
  finestra** aperta da due posti: due copie si sarebbero separate al
  primo cambiamento, ed è il tipo di differenza che si nota solo
  usandole
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

**Celle e allineamento**
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

**Colonne che si nascondono**
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

**Date che si ripetono**
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

**Il menu "/"**
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

**Calendario senza proprietà data**
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

**Database — raggruppare la tabella**
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

**Immagini e tastiera**
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

**Caselle da spuntare — cancellare e uscire**
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

**Caselle da spuntare**
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

**Copertina — inquadratura**
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

**Tastiera e interazione**
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
- **[Bug fix]** Toccare sotto l'ultimo blocco riportava a scrivere
  *sopra* di lui quando non era scrivibile (database, divisore,
  tabella, collegamento): si cercava l'ultimo blocco **scrivibile**
  invece dell'ultimo e basta. Ora ne viene creato uno nuovo dopo
- **[Nuova funzionalità]** Maiuscola automatica sui titoli (pagina,
  database, nome di una riga) e sulle celle di testo. Non su email,
  URL e telefono, dove sarebbe da cancellare ogni volta
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

**Aspetto**
- **[Bug fix]** Sfondo nero fisso con testo e cursore bianchi
- **[Bug fix]** Barra sopra la tastiera dello stesso colore dello
  sfondo, invece di un grigio che creava una cucitura visibile
- **[Nuova funzionalità]** Icona adattiva da foto dell'utente; nome
  app cambiato in "notE"

**Elenchi numerati — il menu del numero**
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

**Elenchi numerati a livelli**
- **[Bug fix]** **L'Invio continua la lista anche quando sotto ci sono
  righe vuote.** Scrivendo `0.` (o qualsiasi numero), poi il testo, poi
  Invio, la riga nuova usciva come paragrafo e la lista si fermava. La
  causa non era la velocità: una riga vuota inserita in mezzo ad altre
  righe vuote è **indistinguibile** dalle sue vicine, e il confronto
  attribuiva l'inserimento all'ultima riga del gruppo, facendo
  ereditare il tipo dal blocco sbagliato. Ora il campo dice anche dove
  sta il cursore. Trovato col log dopo una diagnosi sbagliata: la voce
  per esteso, con l'errore incluso, sta in "Strade già tentate"
- **[Nuova funzionalità]** **Un elenco può partire da zero.** Di suo
  parte da 1, ma scrivendo `0.` parte da 0 e poi prosegue normale: 0,
  1, 2. Serve quando c'è un passo zero — un preambolo prima del primo
  punto vero. Era un solo controllo a vietarlo
  (`numberedListPrefixJustTyped` rifiutava i numeri sotto l'uno): il
  resto reggeva già, perché il numero di partenza è un campo salvato
  sul blocco (`numberStartsAt`) e il conteggio riparte da lì.
  Verificato sul telefono e nel database: il blocco risulta salvato
  con numero di partenza 0, e le righe dopo sono `1.` e `2.`
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
- **[Bug fix]** Per un istante dopo l'Invio il cursore lampeggiava al
  margine sinistro della riga nuova, prima di saltare dopo il numero.
  La riga esiste nel campo prima che il suo blocco arrivi dal database,
  quindi per un attimo veniva disegnata senza numero. Ora il prefisso
  delle righe non ancora salvate viene previsto, ereditandolo dalla
  riga sopra con la stessa regola di `updateRun`. È una previsione solo
  di ciò che si disegna: non tocca né i dati né l'editing, quindi nel
  caso peggiore sbaglia un numero per un fotogramma
- **[Bug fix]** Il pulsante rientro (e sposta su/giù, ed elimina)
  agivano sulla riga precedente se premuti subito dopo un Invio: la
  riga nuova esiste nel campo prima di esistere nella lista dei
  blocchi, quindi non si riusciva ad associarla e il "blocco a fuoco"
  restava indietro di uno. Ora il calcolo viene rifatto anche quando la
  lista dei blocchi cambia, non solo quando si scrive

**Scorciatoie di scrittura**
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
- **[Nuova funzionalità]** Scrivere `1.` (o `5.`, o qualsiasi numero) e
  basta, a inizio blocco, crea un elenco numerato che parte da quel
  numero. Scatta solo se il numero è l'unica cosa scritta nel blocco:
  `abc 1.` non converte niente, come richiesto

**Aspetto — i colori di Notion**
- **[Nuova funzionalità]** Tre grigi campionati dalle schermate di
  Notion: `#191919` per le pagine, `#202020` per le finestre che salgono
  dal basso, `#252525` per i riquadri che raggruppano le voci dentro
  quelle finestre
- **[Nuova funzionalità]** L'app è sempre scura e **non usa più i colori
  dinamici** (Material You). Erano una scelta deliberata, ma tingevano
  di viola o verde proprio gli sfondi che devono restare quei grigi
  precisi. Prima l'editor era nero fisso e il resto seguiva il telefono:
  metà app in un modo e metà nell'altro
- **[Bug fix]** Lampo bianco all'avvio: il tema della finestra era
  `Theme.Material.Light`, e il suo sfondo è quello che Android usa per
  la schermata iniziale. **Quella schermata da Android 12 non si può
  disattivare**, si può solo farle prendere i colori dell'app: sfondo
  `#191919` e icona trasparente (`values-v31/themes.xml`). L'avvio a
  freddo misura 389 ms, quindi non c'è lentezza da nascondere

**Database dentro le pagine**
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
- **[Nuova funzionalità]** Il pulsante della vista sotto il titolo apre
  il selettore del layout (prima era un'etichetta inerte)
- **[Bug fix]** Il database incorporato non scorre per conto suo: prende
  l'altezza che gli serve e lascia scorrere la pagina. Due aree che
  scorrono una dentro l'altra si rubano il gesto a vicenda
- **[Bug fix]** Il margine laterale della pagina lo applicava la lista a
  tutti i blocchi: ora se lo mette ogni blocco, così il database può
  usare tutta la larghezza come a schermo intero (`PAGE_SIDE_PADDING`)

**Database — viste**
- **[Nuova funzionalità]** La vista si sceglie da Settings → Layout, con
  la griglia di riquadri come su Notion, ed è salvata per ogni database
  (`PageEntity.databaseLayout`, migrazione 7→8). Oltre a **Table** ci
  sono **List** (le righe senza griglia; si tocca e si apre la pagina) e
  **Board**
- **[Nuova funzionalità]** **Board**: una colonna per ogni valore di una
  proprietà a selezione singola, righe come schede, colonna delle non
  smistate in fondo a destra, e `+ New` che crea la riga **già
  assegnata** a quella colonna. La proprietà si sceglie da Settings →
  Group by (`PageEntity.boardGroupColumnId`, migrazione 8→9); se non è
  scelta si usa la prima adatta. Solo selezione singola: con quella
  multipla una riga starebbe in più colonne insieme. **Non c'è ancora il
  trascinamento** delle schede tra colonne
- **[Nuova funzionalità]** Le proprietà a selezione sono diventate tag
  veri: pastiglie colorate, finestra dedicata con "Create a tag" e
  "Select an existing option", colori assegnati a giro (`TAG_COLORS`).
  Una cella vuota resta vuota, senza trattini
- **[Bug fix]** Una colonna a selezione appena creata era un vicolo
  cieco: la cella mostrava solo un trattino e le opzioni si potevano
  scrivere unicamente alla creazione, in un campo che stava **sotto**
  l'elenco degli undici tipi e quindi fuori schermo. Ora si creano
  dalla cella, e il campo è sopra l'elenco
- **[Bug fix]** Nella finestra delle proprietà i pulsanti Cancel e Add
  stavano in fondo, dopo l'elenco dei tipi: si sceglieva un tipo e non
  si vedeva più come confermare. Ora sono in un'intestazione fissa. E
  con il nome vuoto Add non faceva niente **in silenzio**: adesso la
  proprietà prende il nome del tipo
- **[Bug fix]** Le linee orizzontali della tabella erano invisibili:
  dentro un contenitore che scorre in orizzontale la larghezza
  disponibile è illimitata, e una linea che chiede "tutta la larghezza"
  ne ottiene zero. Ora la larghezza della tabella viene calcolata
- **[Bug fix]** Il tocco prolungato su una riga non apriva mai le
  azioni: il campo di testo della cella si prendeva il gesto per
  selezionare le parole, e restavano i pallini della selezione appesi
  sopra le finestre. Il nome è testo semplice finché non lo si tocca,
  così un unico rilevatore gestisce tocco e tocco prolungato
- **[Bug fix]** Il titolo della pagina di una riga e il nome della riga
  erano due valori scollegati: rinominando dall'interno, tornando
  indietro si ritrovava il nome vecchio. Ora sono allineati nei due sensi

**Database — vista Calendario**
- **[Nuova funzionalità]** **Calendar**: le pagine collocate nel giorno
  della loro proprietà data, mese per mese, con i giorni fuori mese
  smorzati e oggi evidenziato. La proprietà si sceglie da Settings →
  Date property (`PageEntity.calendarDateColumnId`, migrazione 9→10);
  se non è scelta si usa la prima data che c'è
- **[Nuova funzionalità]** **Intervalli di date**: una cella data può
  avere un inizio e una fine ("Date started"/"Date finished"), scelti
  con il selettore a intervallo di Material 3. Sono salvati come due
  numeri separati da un trattino nella stessa cella, così il tipo della
  colonna non cambia e le date singole già scritte continuano a
  leggersi
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
- **[Nuova funzionalità]** Toccare un giorno crea lì una pagina già
  datata. I giorni stanno in uno strato sotto e le barre in uno sopra:
  gli spazi vuoti fra una barra e l'altra lasciano passare il tocco al
  giorno, mentre toccare una barra apre la sua pagina
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
- **[Nuova funzionalità]** Pulsante **Today** a destra dei tre
  pulsanti della modalità: riporta a oggi da qualunque distanza, in
  tutte e tre. Sta lì **sempre**, anche quando si è già su oggi e non
  farebbe niente: era stato fatto comparire solo da lontano, ma un
  pulsante che compare e sparisce costringe a cercarlo ogni volta e fa
  cambiare aspetto alla riga da sola mentre si sfoglia. Scelta
  dell'utente, ed è quella giusta
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

**Database — l'ora nella proprietà data**
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

**Database — vista Timeline**
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

**Database — schermata rifatta sul modello di Notion**
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

**Database**
- **[Nuova funzionalità]** Elenchi numerati che partono da un numero
  scelto: nuovo campo `numberStartsAt`, che sostituisce il vecchio
  `numberResetHere` (era il caso particolare "riparti da 1")
- **[Nuova funzionalità]** Prima migrazione vera dello schema (5→6) e
  rimozione di `fallbackToDestructiveMigration`. Da qui in poi i dati
  sul telefono sopravvivono ai cambi di schema
- **[Nuova funzionalità]** Selettore di data vero (calendario)
- **[Nuova funzionalità]** Apertura di una riga come pagina
- **[Nuova funzionalità]** Eliminazione colonne con tocco prolungato

**Altri bug fix**
- Crash quando il focus finiva su un blocco divisore
- Ridimensionare una tabella cancellava il testo già scritto
- Ciclo di apertura/chiusura continua del menu "+"

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

Lo schema è alla **versione 29** (dalla 25 alla 29 vengono dalla
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
