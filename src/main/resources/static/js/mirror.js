/* The Mirror — vanilla JS behaviours (no framework).
   Adapted for server-rendered Thymeleaf pages: the booking wizard and the
   "add to bag" localStorage demo from the original design handoff have been
   removed since this app drives both through real server state (session
   cart, real availability, real OTP). Everything else — ripple, stagger,
   reveal-on-scroll, toast, filter/search and the reviews carousel — is
   unchanged and still opt-in via data-* attributes. */
(function () {
  'use strict';
  var EASE = 'cubic-bezier(.2,.7,.2,1)';
  var reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  function anim(el, frames, opts) { if (!reduce && el.animate) return el.animate(frames, opts); }
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

  /* 2. Page-entrance stagger — [data-stagger] elements fade/rise in sequence */
  function stagger(els, step) {
    els.forEach(function (el, i) {
      anim(el, [{ opacity: 0, transform: 'translateY(18px)' }, { opacity: 1, transform: 'none' }],
        { duration: 560, delay: Math.min(i, 12) * step, easing: EASE, fill: 'backwards' });
    });
  }
  stagger($$('[data-stagger]'), 70);

  /* 3. Reveal on scroll — [data-reveal] below the fold fades up when 12% visible */
  if ('IntersectionObserver' in window && !reduce) {
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) {
        if (!en.isIntersecting) return;
        io.unobserve(en.target);
        en.target.classList.remove('is-reveal-hidden');
        anim(en.target, [{ opacity: 0, transform: 'translateY(30px)' }, { opacity: 1, transform: 'none' }], { duration: 800, easing: EASE });
      });
    }, { threshold: 0.12 });
    $$('[data-reveal]').forEach(function (el) {
      if (el.getBoundingClientRect().top > window.innerHeight) { el.classList.add('is-reveal-hidden'); io.observe(el); }
    });
  }

  /* 4. Toast — Mirror.toast('message') shows a pill for 2.4s. Also auto-shown
     for any element carrying data-toast (e.g. a flash message from the server). */
  var toastTimer;
  function toast(msg) {
    var el = $('#m-toast');
    if (!el) { el = document.createElement('div'); el.id = 'm-toast'; el.className = 'm-toast'; el.setAttribute('role', 'status'); document.body.appendChild(el); }
    el.innerHTML = '<span class="m-diamond"></span>';
    el.appendChild(document.createTextNode(msg));
    el.hidden = false;
    anim(el, [{ opacity: 0, transform: 'translate(-50%, 20px) scale(.96)' }, { opacity: 1, transform: 'translate(-50%, 0)' }], { duration: 380, easing: EASE });
    clearTimeout(toastTimer);
    toastTimer = setTimeout(function () { el.hidden = true; }, 2400);
  }
  document.addEventListener('DOMContentLoaded', function () {
    var flash = $('[data-toast]');
    if (flash) toast(flash.getAttribute('data-toast'));
  });

  /* 5. Filter + search — [data-filter-scope] containing chips [data-filter], items [data-item],
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
      anim(entry.item, [{ boxShadow: '0 0 0 3px var(--c-gold)' }, { boxShadow: '0 0 0 0 rgba(0,0,0,0)' }], { duration: 900, easing: EASE });
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

  /* 6. Reviews — [data-reviews] with list items [data-rev-index], prev/next buttons */
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
      anim(feature, [{ opacity: .2, transform: 'translateY(12px)' }, { opacity: 1, transform: 'none' }], { duration: 480, easing: EASE });
    }
    items.forEach(function (it, i) { it.addEventListener('click', function () { show(i); }); });
    var prev = $('[data-rev-prev]', scope), next = $('[data-rev-next]', scope);
    if (prev) prev.addEventListener('click', function () { show(idx - 1); });
    if (next) next.addEventListener('click', function () { show(idx + 1); });
  });

  window.Mirror = { toast: toast, stagger: stagger };
})();
