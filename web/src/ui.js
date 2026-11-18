const icons = {
  players: '<circle cx="9" cy="7" r="3.5"/><path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2m6 0v-2a4 4 0 0 0-3-4m-2-11a3.5 3.5 0 0 1 0 7"/>',
  cards: '<rect x="7" y="5" width="14" height="16" rx="3"/><path d="M3 17V6a3 3 0 0 1 3-3h9m-4 10h6m-3-3v6"/>',
  blade: '<path d="m4 19 7-7m-2-2 5 5m-3-3 3-6 7-3-3 7-6 3"/>',
  user: '<circle cx="12" cy="7" r="3.5"/><path d="M5 21v-2a4 4 0 0 1 4-4h6a4 4 0 0 1 4 4v2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  minus: '<path d="M5 12h14"/>',
  cross: '<path d="m6 6 12 12M18 6 6 18"/>',
  next: '<path d="M4 12h15m-6-6 6 6-6 6"/>',
  back: '<path d="M20 12H5m6-6-6 6 6 6"/>',
  timer: '<circle cx="12" cy="13" r="8"/><path d="M9 2h6m-3 6v5l3 2"/>',
  eye: '<path d="M2 12s4-6 10-6 10 6 10 6-4 6-10 6S2 12 2 12Z"/><circle cx="12" cy="12" r="2.5"/>',
  lock: '<rect x="5" y="10" width="14" height="11" rx="2"/><path d="M8 10V7a4 4 0 1 1 8 0v3m-4 5v2"/>',
  pause: '<path d="M9 5v14m6-14v14"/>',
  play: '<path d="m8 5 11 7-11 7Z"/>',
  flag: '<path d="M5 22V3m0 1c6-5 9 5 15 0v11c-6 5-9-5-15 0"/>',
  repeat: '<path d="M20 11a8 8 0 0 0-14-5L3 9m0-5v5h5m-4 4a8 8 0 0 0 14 5l3-3m0 5v-5h-5"/>'
};

export const icon = name => `<svg aria-hidden="true" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.65" stroke-linecap="round" stroke-linejoin="round">${icons[name]}</svg>`;
const entities = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
export const escape = value => String(value).replace(/[&<>"']/g, char => entities[char]);

export function button(action, label, glyph, style = 'primary', disabled = false) {
  return `<button type="button" class="button ${style}" data-action="${action}" ${disabled ? 'disabled' : ''}>
    ${icon(glyph)}<span>${label}</span>
  </button>`;
}

export const heading = (title, detail) =>
  `<div class="heading"><h1 tabindex="-1">${title}</h1><p>${detail}</p></div>`;

export const panelTitle = (title, glyph, extra = '') =>
  `<div class="panel-title"><span class="glyph">${icon(glyph)}</span><h2>${title}</h2>${extra}</div>`;

export const mark = size => `<img src="./mark.svg" width="${size}" height="${size}" alt="">`;

export function cardMarkup(card) {
  const hero = card.category === 'hero';
  return `<article class="secret glass">
    <div class="card-image ${hero ? 'hero' : 'item'} ${card.portrait ? 'portrait' : ''}">
      <img src="${escape(card.image)}" alt="">
    </div>
    <div class="card-copy">
      <p class="eyebrow">${hero ? 'Герой Dota 2' : 'Предмет Dota 2'}</p>
      <h2 tabindex="-1">${escape(card.name)}</h2>
      <p>${escape(card.description)}</p>
    </div>
  </article>`;
}
