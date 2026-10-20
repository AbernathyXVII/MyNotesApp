// notE Web Clipper: il "lettore" dell'articolo di una pagina, come Reader dei browser e come
// OneNote Web Clipper. Si carica una volta (window.noteReader) e ha due usi:
//  - render(): la pagina diventa solo l'articolo, impaginato pulito ("Article", la foto);
//  - serialize(): l'articolo come blocchi in ordine ("Text and Images"), in JSON:
//    paragrafi coi loro grassetti, corsivi e link, titoletti, elenchi, citazioni, codice,
//    tabelle e immagini (col loro indirizzo vero, anche quando la pagina le carica tardi).
// L'articolo è il blocco con più paragrafi di testo vero (pochi link), più i suoi fratelli che
// ne hanno abbastanza: la copertina, le colonne di lato, le barre, i commenti e gli "Advertisement"
// restano fuori, e anche gli indici fatti solo di link.
(function () {
  var JUNK = 'script, style, noscript, iframe, form, nav, aside, footer, button, input, select, textarea, svg, canvas, ' +
    'video, audio, object, embed, template, [hidden], [aria-hidden="true"], [role="navigation"], [role="complementary"], ' +
    '[class*="share"], [class*="social"], [class*="related"], [class*="comment"], [id*="comment"], [class*="advert"], ' +
    '[class*="adthrive"], [class*="adsbygoogle"], [class~="ad"], [class~="ads"], [class*="ad-unit"], [class*="ad-slot"], ' +
    '[class*="ad-container"], [id^="ad-"], [id*="google_ads"], [class*="newsletter"], [class*="promo"], ' +
    '[class*="breadcrumb"], [class*="sponsor"], [class*="author-bio"], [class*="byline"], [class*="tags"]';
  var AD_TEXT = /^(advertisement|advertisement\. keep scrolling for more|scroll to continue|story continues below( advertisement)?|continue reading below|pubblicità|annuncio|anzeige|werbung|publicité|publicidad|广告|광고|広告)\.?$/i;
  var BLOCK_TAGS = /^(P|DIV|SECTION|ARTICLE|HEADER|MAIN|FIGURE|FIGCAPTION|DL|DT|DD|ADDRESS|CENTER|DETAILS|SUMMARY|LI)$/;

  function textOf(el) { return ((el && (el.innerText || el.textContent)) || '').replace(/\s+/g, ' ').trim(); }
  function meta(sel) { var m = document.querySelector(sel); return m ? (m.getAttribute('content') || '').trim() : ''; }
  function linkDensity(el) {
    var text = textOf(el), links = 0;
    el.querySelectorAll('a').forEach(function (a) { links += textOf(a).length; });
    return text.length ? links / text.length : 0;
  }
  function hidden(el) {
    var cs = getComputedStyle(el);
    return cs.display === 'none' || cs.visibility === 'hidden' || cs.opacity === '0';
  }
  function junk(el) {
    if (el.matches && el.matches(JUNK)) return true;
    // Il testo si guarda solo se è corto: "Advertisement" e simili.
    var raw = el.textContent || '';
    if (raw.length > 120) return false;
    var t = raw.replace(/\s+/g, ' ').trim();
    return t.length > 0 && AD_TEXT.test(t);
  }

  // Il punteggio di ogni contenitore: i paragrafi veri dentro di lui (e, a metà, nel nonno).
  function scores() {
    var map = new Map();
    document.querySelectorAll('p, pre, blockquote, li, td, h2, h3').forEach(function (p) {
      var text = textOf(p);
      if (text.length < 25) return;
      var points = 1 + text.split(/[,.;]/).length + Math.min(Math.floor(text.length / 100), 3);
      var parent = p.parentElement;
      if (parent) map.set(parent, (map.get(parent) || 0) + points);
      var grand = parent && parent.parentElement;
      if (grand) map.set(grand, (map.get(grand) || 0) + points / 2);
    });
    var adjusted = new Map();
    map.forEach(function (score, el) {
      var value = score * (1 - Math.min(linkDensity(el), 1));
      if (/comment|footer|sidebar|related|share|social|promo|advert/i.test((el.id || '') + ' ' + (el.className || ''))) value *= 0.2;
      adjusted.set(el, value);
    });
    return adjusted;
  }

  // L'articolo: il contenitore migliore e i fratelli che hanno abbastanza testo vero.
  function contentRoots() {
    var all = scores(), best = null, bestScore = 0;
    all.forEach(function (score, el) { if (score > bestScore) { bestScore = score; best = el; } });
    if (!best) {
      var fallback = document.querySelector('article') || document.querySelector('main') || document.body;
      return [fallback];
    }
    var parent = best.parentElement;
    if (!parent) return [best];
    var threshold = Math.max(10, bestScore * 0.2);
    var roots = [];
    Array.prototype.forEach.call(parent.children, function (sibling) {
      if (sibling === best) { roots.push(sibling); return; }
      if (junk(sibling) || hidden(sibling)) return;
      var score = all.get(sibling) || 0;
      var text = textOf(sibling);
      var plainParagraph = sibling.tagName === 'P' && text.length > 80 && linkDensity(sibling) < 0.25;
      if (score >= threshold || plainParagraph) roots.push(sibling);
    });
    return roots;
  }

  function pageTitle() {
    var headings = Array.prototype.slice.call(document.querySelectorAll('h1')).filter(function (h) { return !hidden(h); }).map(textOf)
      .filter(function (t) { return t.length >= 8 && t.length <= 200; })
      .sort(function (a, b) { return b.length - a.length; });
    return headings[0] || meta('meta[property="og:title"]') || document.title;
  }

  // L'indirizzo vero di un'immagine, anche se la pagina mostra ancora il segnaposto.
  function imageSource(img) {
    var placeholder = /loading|placeholder|blank|spacer|transparent|lazy|data:image\/(gif|svg)/i;
    var current = img.currentSrc || img.src || '';
    var lazy = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original') ||
      img.getAttribute('data-url') || '';
    var picture = img.closest('picture');
    var fromSet = function (set) {
      if (!set) return '';
      var parts = set.split(',').map(function (s) { return s.trim().split(/\s+/)[0]; }).filter(Boolean);
      return parts.length ? parts[parts.length - 1] : '';
    };
    var source = '';
    if (picture) {
      picture.querySelectorAll('source').forEach(function (s) {
        var type = s.getAttribute('type') || '';
        var candidate = fromSet(s.getAttribute('srcset')) || fromSet(s.getAttribute('data-srcset'));
        if (candidate && (!source || /jpe?g|png/.test(type))) source = candidate;
      });
    }
    var chosen = current && !placeholder.test(current) && img.naturalWidth > 40 ? current : (lazy || source || fromSet(img.getAttribute('data-srcset')) || current);
    if (!chosen || /^data:/.test(chosen)) return '';
    try { return new URL(chosen, location.href).href; } catch (e) { return ''; }
  }

  function render() {
    var roots = contentRoots(), title = pageTitle();
    var site = meta('meta[property="og:site_name"]') || location.hostname.replace(/^www\./, '');
    var author = meta('meta[name="author"]') || meta('meta[property="article:author"]');
    var holder = document.createElement('div');
    roots.forEach(function (root) {
      var clone = root.cloneNode(true);
      clone.querySelectorAll(JUNK).forEach(function (e) { e.remove(); });
      holder.appendChild(clone);
    });
    // Le immagini col loro indirizzo vero (le copie non sanno più dove sono le originali).
    var originals = [];
    roots.forEach(function (root) { root.querySelectorAll('img').forEach(function (img) { originals.push(imageSource(img)); }); });
    holder.querySelectorAll('img').forEach(function (img, i) {
      img.removeAttribute('srcset'); img.removeAttribute('loading'); img.removeAttribute('width'); img.removeAttribute('height');
      if (originals[i]) img.setAttribute('src', originals[i]);
    });
    holder.querySelectorAll('source').forEach(function (s) { s.remove(); });
    holder.querySelectorAll('a').forEach(function (a) { if (a.href) a.setAttribute('href', a.href); });
    holder.querySelectorAll('*').forEach(function (e) {
      if (AD_TEXT.test(textOf(e)) && textOf(e).length < 80) { e.remove(); return; }
      e.removeAttribute('style'); e.removeAttribute('class'); e.removeAttribute('id');
    });
    holder.querySelectorAll('h1').forEach(function (h) { if (textOf(h) === title.trim()) h.remove(); });

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
      'img,picture{max-width:100%;height:auto;display:block;}' +
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
    wrapper.appendChild(holder);
    document.body.innerHTML = '';
    document.body.appendChild(wrapper);
    window.scrollTo(0, 0);
    return JSON.stringify({ title: title, text: textOf(wrapper).length });
  }

  function serialize() {
    var roots = contentRoots(), title = pageTitle();
    var blocks = [];
    var runs = [];
    var widthOfRoots = Math.max.apply(null, roots.map(function (r) { return r.getBoundingClientRect().width; }).concat([1]));

    function addText(text, style) {
      if (!text) return;
      text = text.replace(/[\s ]+/g, ' ');
      if (!runs.length) text = text.replace(/^ /, '');
      if (!text) return;
      var last = runs[runs.length - 1];
      if (last && last.b === style.b && last.i === style.i && last.u === style.u && last.s === style.s && last.h === style.h) {
        if (/ $/.test(last.t) && /^ /.test(text)) text = text.slice(1);
        last.t += text;
      } else {
        runs.push({ t: text, b: style.b, i: style.i, u: style.u, s: style.s, h: style.h });
      }
    }
    function takeRuns() {
      var taken = runs;
      runs = [];
      while (taken.length && !taken[taken.length - 1].t.replace(/ +$/, '')) taken.pop();
      if (taken.length) taken[taken.length - 1].t = taken[taken.length - 1].t.replace(/ +$/, '');
      var text = taken.map(function (r) { return r.t; }).join('');
      return text.trim() && !AD_TEXT.test(text.trim()) ? taken : null;
    }
    function flush(kind, extra) {
      var taken = takeRuns();
      if (!taken) return;
      var block = { k: kind || 'p', runs: taken };
      if (extra) for (var key in extra) block[key] = extra[key];
      blocks.push(block);
    }
    function styleFor(el, style) {
      var next = { b: style.b, i: style.i, u: style.u, s: style.s, h: style.h };
      var tag = el.tagName;
      if (tag === 'B' || tag === 'STRONG') next.b = true;
      if (tag === 'I' || tag === 'EM' || tag === 'CITE') next.i = true;
      if (tag === 'U' || tag === 'INS') next.u = true;
      if (tag === 'S' || tag === 'DEL' || tag === 'STRIKE') next.s = true;
      if (tag === 'A' && el.getAttribute('href') && !/^javascript:/i.test(el.getAttribute('href'))) next.h = el.href;
      if (tag === 'SPAN') {
        var cs = getComputedStyle(el);
        if (parseInt(cs.fontWeight, 10) >= 600) next.b = true;
        if (cs.fontStyle === 'italic') next.i = true;
      }
      return next;
    }
    function isBlock(el) {
      if (BLOCK_TAGS.test(el.tagName)) return true;
      var d = getComputedStyle(el).display;
      return d === 'block' || d === 'flex' || d === 'grid' || d === 'table' || d === 'flow-root' || d === 'list-item';
    }
    function image(img) {
      var src = imageSource(img);
      var rect = img.getBoundingClientRect();
      var w = rect.width || parseFloat(img.getAttribute('width')) || 0;
      var h = rect.height || parseFloat(img.getAttribute('height')) || 0;
      if (!src || (w && w < 48) || (h && h < 48)) return;
      flush();
      blocks.push({ k: 'img', src: src, alt: (img.getAttribute('alt') || '').trim(), w: w ? Math.min(1, w / widthOfRoots) : 1 });
    }
    function inline(node, style) {
      node.childNodes.forEach(function (child) {
        if (child.nodeType === 3) { addText(child.nodeValue, style); return; }
        if (child.nodeType !== 1) return;
        walk(child, style, 0);
      });
    }
    function list(el, ordered, depth, style) {
      flush();
      Array.prototype.forEach.call(el.children, function (li) {
        if (li.tagName !== 'LI' || junk(li) || hidden(li)) return;
        var nested = [];
        li.childNodes.forEach(function (child) {
          if (child.nodeType === 3) { addText(child.nodeValue, style); return; }
          if (child.nodeType !== 1 || junk(child) || hidden(child)) return;
          if (child.tagName === 'UL' || child.tagName === 'OL') { nested.push(child); return; }
          if (child.tagName === 'IMG' || child.tagName === 'PICTURE' || child.tagName === 'FIGURE') { nested.push(child); return; }
          if (child.tagName === 'P' || child.tagName === 'DIV') { inline(child, style); addText(' ', style); return; }
          walk(child, style, depth);
        });
        flush(ordered ? 'ol' : 'ul', { d: depth });
        nested.forEach(function (child) {
          if (child.tagName === 'UL' || child.tagName === 'OL') list(child, child.tagName === 'OL', depth + 1, style);
          else walk(child, style, depth);
        });
      });
    }
    function table(el) {
      var rows = [];
      el.querySelectorAll('tr').forEach(function (tr) {
        if (tr.closest('table') !== el) return;
        var cells = [];
        tr.querySelectorAll('td, th').forEach(function (cell) { if (cell.closest('tr') === tr) cells.push(textOf(cell)); });
        if (cells.length) rows.push(cells);
      });
      return rows;
    }
    function walk(el, style, depth) {
      if (junk(el) || hidden(el)) return;
      var tag = el.tagName;
      if (tag === 'BR') { flush(); return; }
      if (tag === 'HR') { flush(); blocks.push({ k: 'hr' }); return; }
      if (tag === 'IMG') { image(el); return; }
      if (tag === 'PICTURE') { var img = el.querySelector('img'); if (img) image(img); return; }
      if (tag === 'SOURCE') return;
      if (/^H[1-6]$/.test(tag)) {
        flush();
        if (tag === 'H1' && textOf(el) === title.trim()) return;
        inline(el, { b: false, i: false, u: false, s: false, h: undefined });
        flush('h', { l: parseInt(tag.charAt(1), 10) });
        return;
      }
      if (tag === 'UL' || tag === 'OL') {
        // Un indice fatto solo di link (ai capitoli della pagina): OneNote lo lascia fuori, anche qui.
        if (linkDensity(el) > 0.85 && el.querySelectorAll('li').length > 2) return;
        list(el, tag === 'OL', depth, style);
        return;
      }
      if (tag === 'BLOCKQUOTE') {
        flush();
        var parts = [];
        el.querySelectorAll('p').forEach(function (p) { parts.push(textOf(p)); });
        var text = parts.length ? parts.join('\n') : textOf(el);
        if (text) blocks.push({ k: 'quote', text: text });
        return;
      }
      if (tag === 'PRE') { flush(); var code = (el.innerText || el.textContent || '').replace(/\s+$/, ''); if (code) blocks.push({ k: 'code', text: code }); return; }
      if (tag === 'TABLE') {
        var rows = table(el);
        var cells = rows.reduce(function (n, r) { return n + r.length; }, 0);
        // Una "tabella" che impagina la pagina, o di una cella sola, si legge come testo.
        if (cells < 2 || el.querySelector('table, img, ul, ol, p')) { inline(el, style); flush(); return; }
        flush();
        blocks.push({ k: 'table', rows: rows });
        return;
      }
      if (tag === 'FIGCAPTION') {
        flush();
        inline(el, { b: style.b, i: true, u: style.u, s: style.s, h: style.h });
        flush('cap');
        return;
      }
      if (isBlock(el)) {
        flush();
        inline(el, style);
        flush();
        return;
      }
      inline(el, styleFor(el, style));
    }

    roots.forEach(function (root) { walk(root, { b: false, i: false, u: false, s: false, h: undefined }, 0); });
    flush();
    return JSON.stringify({ title: title, site: meta('meta[property="og:site_name"]') || location.hostname.replace(/^www\./, ''), blocks: blocks });
  }

  window.noteReader = { contentRoots: contentRoots, render: render, serialize: serialize, title: pageTitle };
  return 'ready';
})();
