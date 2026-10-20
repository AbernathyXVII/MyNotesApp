// notE Web Clipper: toglie dalla pagina quello che non va nella foto, prima dello scatto.
// 1) i banner dei cookie: prima si tocca "Rifiuta" (la scelta che protegge di più), poi si
//    nasconde quello che resta; 2) le barre fisse in basso e i riquadri che galleggiano
//    (pubblicità, chat), che nella foto finirebbero in mezzo alla pagina; 3) lo scorrimento
//    bloccato dai banner. Le barre fisse in alto restano, ferme in cima alla pagina.
// Restituisce cosa ha fatto, per i test.
(function () {
  var REJECT = /^(reject( all| all cookies| cookies)?|decline( all| cookies)?|deny( all)?|refuse( all)?|disagree( and close)?|i do not accept|do not accept|no,? thanks|only (necessary|essential|required)( cookies)?|(use )?necessary( cookies)? only|essential( cookies)? only|continue without (accepting|agreeing|consent)|rifiuta( tutto| tutti| i cookie)?|non accetto|solo (necessari|essenziali|tecnici)|(usa )?solo (i )?cookie (necessari|tecnici)|continua senza accettare|alle ablehnen|ablehnen|nur (notwendige|erforderliche|essenzielle)( cookies)?|tout refuser|refuser( tout)?|continuer sans accepter|rechazar( todo| todas)?|solo (necesarias|esenciales)|recusar( tudo)?)$/i;
  var WORDS = /cookie|consent|privacy|gdpr|datenschutz|confidentialit|consenso|partner/i;
  var KNOWN = [
    '#cmpwrapper', '#cmpbox', '#cmpbox2', '#onetrust-consent-sdk', '#onetrust-banner-sdk', '#CybotCookiebotDialog',
    '#CybotCookiebotDialogBodyUnderlay', '.fc-consent-root', '#didomi-host', '#usercentrics-root', '#usercentrics-cmp-ui',
    'div[id^="sp_message_container"]', '.qc-cmp2-container', '#qc-cmp2-container', '#truste-consent-track',
    '#trustarc-banner-overlay', '.truste_overlay', '.truste_box_overlay', '#cookie-law-info-bar', '.cc-window',
    '#cookie-notice', '#CookieConsent', '#iubenda-cs-banner', '#axeptio_overlay', '#tarteaucitronRoot', '#klaro',
    '#cookiescript_injected', '#cmplz-cookiebanner-container', '#BorlabsCookieBox', '#hs-eu-cookie-confirmation',
    '.osano-cm-window', '#ez-cookie-dialog-wrapper', '.termly-styles-root', '#pubtech-cmp', '#_evidon_banner',
    '#consent_blackbar', '#cookie-banner', '#cookieBanner', '#cookies-banner', '.cookie-banner', '.cookies-banner'
  ];
  var report = { rejected: 0, hidden: 0, pinned: 0 };

  function roots() {
    var out = [document];
    (function walk(root) {
      var all = root.querySelectorAll('*');
      for (var i = 0; i < all.length; i++) {
        if (all[i].shadowRoot) { out.push(all[i].shadowRoot); walk(all[i].shadowRoot); }
      }
    })(document);
    return out;
  }
  function textOf(el) { return ((el.innerText || el.value || el.textContent || '') + '').trim().replace(/\s+/g, ' '); }
  function visible(el) { var r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0; }
  function hide(el) {
    if (el.getAttribute('data-note-hidden')) return;
    el.setAttribute('data-note-hidden', '1');
    el.style.setProperty('display', 'none', 'important');
    report.hidden++;
  }

  // 1. "Rifiuta": nella pagina e dentro i pezzi di pagina a parte (shadow DOM).
  roots().forEach(function (root) {
    var buttons = root.querySelectorAll('button, [role=button], a, input[type=button], input[type=submit]');
    for (var i = 0; i < buttons.length; i++) {
      var b = buttons[i], t = textOf(b);
      if (t.length > 0 && t.length < 45 && REJECT.test(t) && visible(b)) {
        try { b.click(); report.rejected++; } catch (e) {}
      }
    }
  });

  // 2. I banner conosciuti, anche se il tocco non li ha chiusi.
  KNOWN.forEach(function (sel) {
    var found = document.querySelectorAll(sel);
    for (var i = 0; i < found.length; i++) hide(found[i]);
  });

  // 3. Quello che sta fermo sullo schermo.
  var vw = window.innerWidth, vh = window.innerHeight;
  var all = document.querySelectorAll('body *');
  for (var i = 0; i < all.length; i++) {
    var el = all[i];
    var cs = getComputedStyle(el);
    if (cs.position !== 'fixed' && cs.position !== 'sticky') continue;
    if (cs.display === 'none' || !visible(el)) continue;
    // Quello che si attacca scorrendo torna al suo posto: nella foto si ripeterebbe.
    if (cs.position === 'sticky') { el.style.setProperty('position', 'static', 'important'); report.pinned++; continue; }
    var r = el.getBoundingClientRect();
    var area = (r.width * r.height) / (vw * vh);
    var talksConsent = WORDS.test(textOf(el).slice(0, 2000)) || WORDS.test(el.id + ' ' + el.className);
    if (talksConsent && cs.position === 'fixed') { hide(el); continue; }
    // Un velo sopra tutta la pagina (lo sfondo scuro di un banner).
    if (cs.position === 'fixed' && area > 0.6 && textOf(el).length < 40) { hide(el); continue; }
    // In cima: la barra del sito resta, ma ferma in cima alla pagina.
    if (r.top <= 2 && r.height < vh * 0.4) {
      if (cs.position === 'fixed') { el.style.setProperty('position', 'absolute', 'important'); report.pinned++; }
      continue;
    }
    // In basso o a galleggiare: pubblicità, chat, pulsanti. Nella foto andrebbero in mezzo.
    if (cs.position === 'fixed') hide(el);
  }

  // 4. Lo scorrimento sbloccato.
  [document.documentElement, document.body].forEach(function (el) {
    if (!el) return;
    el.style.setProperty('overflow', 'visible', 'important');
    el.style.setProperty('overflow-y', 'visible', 'important');
    // Certi banner bloccano la pagina fissandola: torna al suo posto.
    if (getComputedStyle(el).position === 'fixed') el.style.setProperty('position', 'static', 'important');
    el.classList.remove('modal-open', 'no-scroll', 'noscroll', 'overflow-hidden', 'cmp-open', 'didomi-popup-open');
  });
  return JSON.stringify(report);
})();
