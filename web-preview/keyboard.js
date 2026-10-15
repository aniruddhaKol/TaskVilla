/* TaskVilla editable-field viewport handling. Aniruddha DuttaChowdhury · ADChowdhury. */
(() => {
  'use strict';
  const root = document.documentElement;
  const viewport = window.visualViewport;
  const fieldSelector = 'textarea,select,[contenteditable="true"],input:not([type="hidden"]):not([type="checkbox"]):not([type="radio"]):not([type="range"]):not([type="file"]):not([type="button"]):not([type="submit"]):not([type="color"])';
  let fullHeight = window.innerHeight, keyboardOpen = false, positions = new Map();
  let frame = 0, settleTimer = 0, restoringUntil = 0;

  function editable(node) { return node instanceof Element ? node.closest(fieldSelector) : null; }
  function remember(field) {
    for (let node = field.parentElement; node; node = node.parentElement) {
      if (/(auto|scroll)/.test(getComputedStyle(node).overflowY) && !positions.has(node)) positions.set(node, node.scrollTop);
    }
  }
  function reveal() {
    const field = editable(document.activeElement);
    if (!field || !field.isConnected || Date.now() < restoringUntil) return;
    const visualTop = viewport ? viewport.offsetTop : 0;
    const visualBottom = visualTop + (viewport ? viewport.height : innerHeight);
    for (let node = field.parentElement; node && node !== document.body; node = node.parentElement) {
      if (!/(auto|scroll)/.test(getComputedStyle(node).overflowY) || node.scrollHeight <= node.clientHeight) continue;
      const bounds = node.getBoundingClientRect(), target = field.getBoundingClientRect();
      const footer = node.classList.contains('mobile-content') ? node.querySelector('.sticky-action') : null;
      let bottom = Math.min(bounds.bottom, visualBottom) - 12;
      if (footer) bottom = Math.min(bottom, footer.getBoundingClientRect().top - 12);
      const top = Math.max(bounds.top, visualTop) + 12;
      const visibleHeight = bottom - top;
      if (visibleHeight <= 0) continue;
      if (target.height > visibleHeight || target.top < top) node.scrollTop += target.top - top;
      else if (target.bottom > bottom) node.scrollTop += target.bottom - bottom;
    }
  }
  function restore() {
    const saved = positions;
    positions = new Map();
    restoringUntil = Date.now() + 400;
    const apply = () => { for (const [node, top] of saved) if (node.isConnected) node.scrollTop = top; };
    requestAnimationFrame(() => requestAnimationFrame(apply));
    setTimeout(apply, 180);
  }
  function update() {
    frame = 0;
    if (viewport && Math.abs(viewport.scale - 1) > .05) return;
    const height = viewport ? viewport.height : innerHeight;
    fullHeight = Math.max(fullHeight, innerHeight, height);
    const open = fullHeight - height > 120;
    root.classList.toggle('keyboard-open', open);
    if (open) root.style.setProperty('--keyboard-viewport-height', Math.floor(height) + 'px');
    else root.style.removeProperty('--keyboard-viewport-height');
    if (keyboardOpen && !open) restore();
    keyboardOpen = open;
    if (open) reveal();
  }
  function schedule() {
    if (!frame) frame = requestAnimationFrame(update);
    clearTimeout(settleTimer);
    settleTimer = setTimeout(() => { update(); if (keyboardOpen) reveal(); }, 240);
  }
  document.addEventListener('pointerdown', event => {
    const field = editable(event.target);
    if (!field) return;
    restoringUntil = 0;
    if (!keyboardOpen) positions.clear();
    remember(field);
  }, true);
  document.addEventListener('focusin', event => {
    const field = editable(event.target);
    if (!field) return;
    restoringUntil = 0;
    remember(field);
    schedule();
    requestAnimationFrame(reveal);
    const textInput = field.tagName === 'TEXTAREA' || field.isContentEditable ||
      (field.tagName === 'INPUT' && ['text','email','search','tel','url','number','password'].includes(field.type));
    if (textInput && !field.readOnly && !keyboardOpen && window.LocalDatabase?.showKeyboard) LocalDatabase.showKeyboard();
  });
  document.addEventListener('input', event => { if (editable(event.target)) schedule(); });
  window.addEventListener('resize', schedule);
  if (viewport) { viewport.addEventListener('resize', schedule); viewport.addEventListener('scroll', schedule); }
  window.addEventListener('orientationchange', () => {
    fullHeight = 0; positions.clear(); keyboardOpen = false;
    setTimeout(() => { fullHeight = innerHeight; schedule(); }, 350);
  });
  schedule();
})();
