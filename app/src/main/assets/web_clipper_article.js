// notE Web Clipper, "Article": tiene solo l'articolo della pagina (titolo, testo, immagini),
// senza menu, pubblicità, barre laterali e commenti, impaginato pulito per la lettura.
// Come "Reader" dei browser: si cerca il blocco con più paragrafi di testo veri (pochi link),
// lo si copia, e la pagina diventa solo lui. Restituisce il titolo e quanto testo è rimasto.
(function () {
  function textOf(el) { return ((el && (el.innerText || el.textContent)) || '').replace(/\s+/g, ' ').trim(); }
  function meta(sel) { var m = document.querySelector(sel); return m ? (m.getAttribute('content') || '').trim() : ''; }

  // 1. I punteggi: ogni paragrafo vero dà punti al suo contenitore e (metà) a quello sopra.
  var scores = new Map();
  var paragraphs = document.querySelectorAll('p, pre, blockquote, li, td, h2, h3');
  for (var i = 0; i < paragraphs.length; i++) {
    var p = paragraphs[i], text = textOf(p);
    if (text.length < 25) continue;
    var points = 1 + text.split(/[,.;]/).length + Math.min(Math.floor(text.length / 100), 3);
    var parent = p.parentElement;
    if (parent) scores.set(parent, (scores.get(parent) || 0) + points);
    var grand = parent && parent.parentElement;
    if (grand) scores.set(grand, (scores.get(grand) || 0) + points / 2);
  }
  var best = null, bestScore = 0;
  scores.forEach(function (score, el) {
    var text = textOf(el), links = 0;
    el.querySelectorAll('a').forEach(function (a) { links += textOf(a).length; });
    var adjusted = score * (1 - Math.min(links / Math.max(text.length, 1), 1));
    if (/comment|footer|sidebar|related|share|social|promo|advert/i.test((el.id || '') + ' ' + (el.className || ''))) adjusted *= 0.2;
    if (adjusted > bestScore) { bestScore = adjusted; best = el; }
  });
  if (!best) best = document.querySelector('article') || document.querySelector('main') || document.body;
  var article = best.closest('article');
  if (article && textOf(article).length < textOf(best).length * 3) best = article;

  // 2. La copia, pulita.
  var clone = best.cloneNode(true);
  clone.querySelectorAll('script, style, noscript, iframe, form, nav, aside, footer, button, input, select, textarea, ' +
    '[aria-hidden="true"], [class*="share"], [class*="social"], [class*="related"], [class*="comment"], [id*="comment"], ' +
    '[class*="advert"], [class*="adthrive"], [id^="ad-"], [class*="newsletter"], [class*="promo"], [class*="breadcrumb"]')
    .forEach(function (e) { e.remove(); });
  clone.querySelectorAll('img').forEach(function (img) {
    var src = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original');
    if (src) img.setAttribute('src', src);
    var set = img.getAttribute('data-srcset');
    if (set) img.setAttribute('srcset', set);
    if (img.src) img.setAttribute('src', img.src);
    img.removeAttribute('loading'); img.removeAttribute('width'); img.removeAttribute('height');
  });
  clone.querySelectorAll('a').forEach(function (a) { if (a.href) a.setAttribute('href', a.href); });
  clone.querySelectorAll('*').forEach(function (e) { e.removeAttribute('style'); e.removeAttribute('class'); e.removeAttribute('id'); });

  // Il titolo scritto sulla pagina: quello dei metadati spesso si porta dietro il nome del sito.
  // Il più lungo fra gli H1: certi siti mettono in un H1 anche il logo.
  var headings = Array.prototype.slice.call(document.querySelectorAll('h1')).map(textOf)
    .filter(function (t) { return t.length >= 8 && t.length <= 200; })
    .sort(function (a, b) { return b.length - a.length; });
  var title = headings[0] || meta('meta[property="og:title"]') || document.title;
  var site = meta('meta[property="og:site_name"]') || location.hostname.replace(/^www\./, '');
  var author = meta('meta[name="author"]') || meta('meta[property="article:author"]');
  var heading = clone.querySelector('h1');
  if (heading && textOf(heading) === title.trim()) heading.remove();

  // 3. La pagina diventa solo l'articolo.
  document.querySelectorAll('style, link[rel="stylesheet"], script').forEach(function (e) { e.remove(); });
  [document.documentElement, document.body].forEach(function (e) { e.removeAttribute('class'); e.removeAttribute('style'); });
  var css = document.createElement('style');
  css.textContent =
    'html,body{margin:0;padding:0;background:#fff;}' +
    'body{font-family:Georgia,"Noto Serif",serif;color:#1f1f1f;font-size:20px;line-height:1.65;}' +
    '.note-article{max-width:720px;margin:0 auto;padding:56px 36px 72px;}' +
    '.note-site{font-family:Roboto,Arial,sans-serif;font-size:13px;color:#787774;text-transform:uppercase;letter-spacing:.09em;margin-bottom:14px;}' +
    'h1.note-title{font-family:Roboto,Arial,sans-serif;font-size:38px;line-height:1.2;margin:0 0 12px;color:#111;}' +
    '.note-byline{font-family:Roboto,Arial,sans-serif;font-size:15px;color:#787774;margin-bottom:36px;}' +
    'img,video,picture,svg{max-width:100%;height:auto;}' +
    'figure{margin:28px 0;}figcaption{font-size:15px;color:#787774;}' +
    'a{color:#0b6e99;text-decoration:none;}' +
    'h2,h3,h4{font-family:Roboto,Arial,sans-serif;line-height:1.3;color:#111;margin:36px 0 12px;}' +
    'pre,code{font-family:monospace;font-size:16px;background:#f4f4f2;}pre{padding:14px;white-space:pre-wrap;}' +
    'blockquote{margin:20px 0;padding-left:18px;border-left:4px solid #e3e2e0;color:#444;}' +
    'table{border-collapse:collapse;max-width:100%;margin:20px 0;}td,th{border:1px solid #e3e2e0;padding:6px 10px;font-size:17px;}';
  (document.head || document.documentElement).appendChild(css);
  var wrapper = document.createElement('div');
  wrapper.className = 'note-article';
  var siteLine = document.createElement('div'); siteLine.className = 'note-site'; siteLine.textContent = site;
  var h1 = document.createElement('h1'); h1.className = 'note-title'; h1.textContent = title;
  wrapper.appendChild(siteLine);
  wrapper.appendChild(h1);
  if (author) { var by = document.createElement('div'); by.className = 'note-byline'; by.textContent = author; wrapper.appendChild(by); }
  wrapper.appendChild(clone);
  document.body.innerHTML = '';
  document.body.appendChild(wrapper);
  window.scrollTo(0, 0);
  return JSON.stringify({ title: title, text: textOf(wrapper).length });
})();
