/* The Mirror — vanilla JS behaviours (design v2, no framework).
   Server-rendered Thymeleaf pages: the booking wizard and cart are driven by
   real server state, so only presentation behaviour lives here. Everything
   is opt-in via data-* attributes:
     loader (first visit per session) · scroll progress bar · header state
     [data-lines] headline line reveal · [data-stagger] · [data-reveal] (+ [data-img] clip reveal)
     [data-hero-bg] / [data-parallax] · [data-kenburns] · [data-ripple]
     [data-filter-scope] filter + search · [data-reviews] carousel
     [data-menu] dashboard sidebar toggle · .m-header__toggle mobile nav       */
(function () {
  'use strict';
  var EASE = 'cubic-bezier(.2,.7,.2,1)', EASE_IO = 'cubic-bezier(.76,0,.24,1)';
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var html = document.documentElement;
  function anim(el, frames, opts) { if (!reduce && el && el.animate) return el.animate(frames, opts); }
  function $(sel, root) { return (root || document).querySelector(sel); }
  function $$(sel, root) { return Array.prototype.slice.call((root || document).querySelectorAll(sel)); }

  /* 1. Ripple on press — any [data-ripple] element */
  document.addEventListener('pointerdown', function (e) {
    var t = e.target.closest && e.target.closest('[data-ripple]');
    if (!t || t.disabled || reduce) return;
    var b = t.getBoundingClientRect(), size = Math.max(b.width, b.height) * 2.2;
    var s = document.createElement('span');
    s.className = 'm-ripple';
    s.style.cssText = 'left:' + (e.clientX - b.left - size / 2) + 'px;top:' + (e.clientY - b.top - size / 2) + 'px;width:' + size + 'px;height:' + size + 'px';
    t.appendChild(s);
    s.addEventListener('animationend', function () { s.remove(); });
  });

  /* 2. Loader — public pages, first visit of the session only */
  var loader = $('.m-loader');
  var base = 0;
  if (loader && html.classList.contains('m-loading') && !reduce) {
    base = 1700;
    var logo = $('.m-loader__logo', loader), line = $('.m-loader__line', loader);
    anim(logo, [{ opacity: 0, transform: 'translateY(16px) scale(.96)', filter: 'blur(8px)' }, { opacity: 1, transform: 'none', filter: 'blur(0)' }], { duration: 1000, easing: EASE, fill: 'forwards' });
    anim(line, [{ transform: 'scaleX(0)' }, { transform: 'scaleX(1)' }], { duration: 1000, delay: 250, easing: EASE, fill: 'forwards' });
    var out = anim(loader, [{ transform: 'translateY(0)' }, { transform: 'translateY(-100%)' }], { duration: 900, delay: 1400, easing: EASE_IO, fill: 'forwards' });
    var done = function () { html.classList.remove('m-loading'); };
    if (out) out.onfinish = done; else done();
  } else {
    html.classList.remove('m-loading');
  }
  try { if (loader) sessionStorage.setItem('mirror-loaded', '1'); } catch (e) { /* storage blocked: loader just replays */ }

  /* 3. Headline lines — [data-lines] is split on <br> into overflow-hidden rows
        that slide up (translateY 110% + skew → none, 1.1s, 120ms stagger) */
  $$('[data-lines]').forEach(function (el) {
    var parts = el.innerHTML.split(/<br\s*\/?>/i);
    el.innerHTML = parts.map(function (p) { return '<span class="m-line"><span data-line>' + p + '</span></span>'; }).join('');
  });
  $$('[data-line]').forEach(function (el, i) {
    anim(el, [{ transform: 'translateY(110%) skewY(4deg)' }, { transform: 'none' }], { duration: 1100, delay: base + i * 120, easing: EASE_IO, fill: 'backwards' });
  });

  /* 4. Entrance stagger — [data-stagger] elements fade/rise in sequence */
  function stagger(els, step, delay) {
    els.forEach(function (el, i) {
      anim(el, [{ opacity: 0, transform: 'translateY(22px)' }, { opacity: 1, transform: 'none' }],
        { duration: 700, delay: (delay || 0) + Math.min(i, 12) * step, easing: EASE, fill: 'backwards' });
    });
  }
  stagger($$('[data-stagger]'), 80, base + 200);

  /* 5. Hero Ken Burns */
  $$('[data-kenburns]').forEach(function (el) {
    anim(el, [{ transform: 'scale(1.18)' }, { transform: 'scale(1.02)' }], { duration: 2800, delay: Math.max(0, base - 300), easing: EASE, fill: 'both' });
  });

  /* 6. Reveal on scroll — [data-reveal] fades up; [data-img] inside clips open */
  function clipOpen(el) { anim(el, [{ clipPath: 'inset(100% 0 0 0)' }, { clipPath: 'inset(0 0 0 0)' }], { duration: 1300, easing: EASE_IO }); }
  if ('IntersectionObserver' in window && !reduce) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (!en.isIntersecting) return;
        var el = en.target;
        io.unobserve(el);
        el.classList.remove('is-reveal-hidden');
        anim(el, [{ opacity: 0, transform: 'translateY(40px)' }, { opacity: 1, transform: 'none' }], { duration: 1000, easing: EASE });
        $$('[data-img]', el).forEach(clipOpen);
        if (el.hasAttribute('data-img')) clipOpen(el);
      });
    }, { threshold: 0.12 });
    $$('[data-reveal]').forEach(function (el) {
      if (el.getBoundingClientRect().top > window.innerHeight * 0.92) { el.classList.add('is-reveal-hidden'); io.observe(el); }
    });
  }

  /* 7. Scroll effects — progress bar, header state, hero + element parallax */
  var progress = $('.m-progress'), header = $('.m-header'), heroBg = $('[data-hero-bg]');
  var parallaxEls = $$('[data-parallax]'), ticking = false;
  function onScroll() {
    ticking = false;
    var y = window.scrollY, h = document.documentElement.scrollHeight - window.innerHeight;
    if (progress) progress.style.transform = 'scaleX(' + (h > 0 ? y / h : 0) + ')';
    if (header) header.classList.toggle('is-scrolled', y > 40);
    if (reduce) return;
    if (heroBg) heroBg.style.transform = 'translateY(' + (y * 0.3) + 'px)';
    parallaxEls.forEach(function (el) {
      var b = el.getBoundingClientRect();
      var c = b.top + b.height / 2 - window.innerHeight / 2;
      el.style.transform = 'translateY(' + (c * parseFloat(el.getAttribute('data-parallax'))) + 'px)';
    });
  }
  window.addEventListener('scroll', function () { if (!ticking) { ticking = true; requestAnimationFrame(onScroll); } }, { passive: true });
  window.addEventListener('resize', onScroll);
  onScroll();

  /* 8. Mobile nav (public header) and dashboard sidebar */
  var toggle = $('.m-header__toggle');
  if (toggle && header) {
    toggle.addEventListener('click', function () {
      var open = header.classList.toggle('is-open');
      toggle.setAttribute('aria-expanded', open);
    });
    $$('.m-nav a', header).forEach(function (a) { a.addEventListener('click', function () { header.classList.remove('is-open'); }); });
  }
  var shell = $('.m-shell');
  if (shell) {
    $$('[data-menu]').forEach(function (b) { b.addEventListener('click', function () { shell.classList.toggle('is-menu-open'); }); });
    var scrim = $('.m-scrim', shell);
    if (scrim) scrim.addEventListener('click', function () { shell.classList.remove('is-menu-open'); });
  }

  /* 9. Toast — Mirror.toast('message') shows a pill for 2.4s. Also auto-shown
     for any element carrying data-toast (e.g. a flash message from the server). */
  var toastTimer;
  function toast(msg) {
    var el = $('#m-toast');
    if (!el) { el = document.createElement('div'); el.id = 'm-toast'; el.className = 'm-toast'; el.setAttribute('role', 'status'); document.body.appendChild(el); }
    el.innerHTML = '<span class="m-diamond"></span>';
    el.appendChild(document.createTextNode(msg));
    el.hidden = false;
    anim(el, [{ opacity: 0, transform: 'translate(-50%, 20px) scale(.96)' }, { opacity: 1, transform: 'translate(-50%, 0)' }], { duration: 400, easing: EASE });
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.hidden = true; }, 2400);
  }
  document.addEventListener('DOMContentLoaded', function () {
    var flash = $('[data-toast]');
    if (flash) toast(flash.getAttribute('data-toast'));
  });

  /* 10. Filter + search — [data-filter-scope] containing chips [data-filter], items [data-item],
        an optional search box [data-search-input] and its suggestion dropdown [data-suggest] */
  $$('[data-filter-scope]').forEach(function (scope) {
    var chips = $$('[data-filter]', scope), items = $$('[data-item]', scope);
    var search = $('[data-search-input]', scope), empty = $('[data-empty]', scope);
    var suggestBox = $('[data-suggest]', scope);
    var cat = 'all', q = '';

    /* Suggestion index: one entry per searchable name (item title, plus any
       sub-names like individual service names inside a category card). */
    var index = [];
    items.forEach(function (it) {
      var title = it.getAttribute('data-title') || '';
      if (title) index.push({ label: title, item: it });
      (it.getAttribute('data-names') || '').split('|').forEach(function (n) {
        n = n.trim();
        if (n && n.toLowerCase() !== title.toLowerCase()) index.push({ label: n, item: it });
      });
    });

    function apply() {
      var shown = [];
      items.forEach(function (it) {
        var names = (it.getAttribute('data-names') || '').split('|');
        var hits = q ? names.filter(function (n) { return n.toLowerCase().indexOf(q) > -1; }) : [];
        var title = (it.getAttribute('data-title') || '').toLowerCase();
        var ok = (cat === 'all' || it.getAttribute('data-cat') === cat) && (!q || hits.length || title.indexOf(q) > -1);
        it.hidden = !ok;
        if (ok) shown.push(it);
        var m = $('[data-matches]', it);
        if (m) { m.hidden = !hits.length; m.textContent = hits.slice(0, 3).join(' · '); }
      });
      if (empty) { empty.hidden = shown.length > 0; var qs = $('[data-empty-q]', empty); if (qs) qs.textContent = q; }
      stagger(shown, 35);
    }

    function closeSuggestions() {
      if (!suggestBox) return;
      suggestBox.hidden = true;
      suggestBox.innerHTML = '';
    }

    function pick(entry) {
      if (search) search.value = entry.label;
      q = entry.label.toLowerCase();
      closeSuggestions();
      apply();
      entry.item.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'center' });
      anim(entry.item, [{ boxShadow: '0 0 0 3px #C6A16A' }, { boxShadow: '0 0 0 0 rgba(0,0,0,0)' }], { duration: 900, easing: EASE });
    }

    function renderSuggestions() {
      if (!suggestBox) return;
      if (!q) { closeSuggestions(); return; }
      var seen = {}, matches = [];
      index.forEach(function (entry) {
        var key = entry.label.toLowerCase();
        if (key.indexOf(q) === -1 || seen[key]) return;
        seen[key] = true;
        matches.push(entry);
      });
      matches.sort(function (a, b) {
        var ai = a.label.toLowerCase().indexOf(q), bi = b.label.toLowerCase().indexOf(q);
        return ai !== bi ? ai - bi : a.label.length - b.label.length;
      });
      matches = matches.slice(0, 6);
      if (!matches.length) { closeSuggestions(); return; }
      suggestBox.innerHTML = '';
      matches.forEach(function (entry) {
        var li = document.createElement('li');
        li.className = 'm-suggest__item';
        li.setAttribute('role', 'option');
        li.textContent = entry.label;
        li.addEventListener('mousedown', function (e) { e.preventDefault(); pick(entry); });
        suggestBox.appendChild(li);
      });
      suggestBox.hidden = false;
    }

    chips.forEach(function (c) {
      c.addEventListener('click', function () {
        cat = c.getAttribute('data-filter');
        chips.forEach(function (x) { x.classList.toggle('is-active', x === c); x.setAttribute('aria-pressed', x === c); });
        apply();
      });
    });
    if (search) {
      search.addEventListener('input', function () { q = search.value.trim().toLowerCase(); apply(); renderSuggestions(); });
      search.addEventListener('focus', function () { renderSuggestions(); });
      document.addEventListener('click', function (e) {
        if (suggestBox && !suggestBox.hidden && e.target !== search && !suggestBox.contains(e.target)) closeSuggestions();
      });
    }
    var clear = $('[data-clear]', scope);
    if (clear) clear.addEventListener('click', function () {
      if (search) search.value = ''; q = ''; cat = 'all';
      chips.forEach(function (x) { x.classList.toggle('is-active', x.getAttribute('data-filter') === 'all'); });
      closeSuggestions();
      apply();
    });
  });

  /* 11. Reviews — [data-reviews] with list items [data-rev-index], prev/next buttons */
  $$('[data-reviews]').forEach(function (scope) {
    var items = $$('[data-rev-index]', scope), feature = $('[data-rev-feature]', scope), idx = 0;
    if (!items.length || !feature) return;
    function show(i) {
      idx = (i + items.length) % items.length;
      var it = items[idx];
      $('[data-rev-text]', feature).textContent = it.getAttribute('data-text');
      $('[data-rev-name]', feature).textContent = it.getAttribute('data-name');
      $('[data-rev-svc]', feature).textContent = it.getAttribute('data-svc');
      items.forEach(function (x, j) { x.classList.toggle('is-active', j === idx); });
      anim(feature, [{ opacity: 0, transform: 'translateY(16px)', filter: 'blur(4px)' }, { opacity: 1, transform: 'none', filter: 'blur(0)' }], { duration: 560, easing: EASE });
    }
    items.forEach(function (it, i) { it.addEventListener('click', function () { show(i); }); });
    var prev = $('[data-rev-prev]', scope), next = $('[data-rev-next]', scope);
    if (prev) prev.addEventListener('click', function () { show(idx - 1); });
    if (next) next.addEventListener('click', function () { show(idx + 1); });
  });

  window.Mirror = { toast: toast, stagger: stagger };
})();
