import { canStart, deal, loadPlayers, maxSpies, nameLimit, remaining, savePlayers, timeText } from './rules.js';
import { button, cardMarkup, escape, heading, icon, mark, panelTitle } from './ui.js';

const root = document.querySelector('#game');
let storage;
try { storage = window.localStorage; } catch { /* Use the in-memory list. */ }

const players = loadPlayers(storage);
const settings = { categories: new Set(['hero', 'item']), spies: 1, minutes: 4 };
let deck;
let screen = 'lobby';
let round;
let turn = 0;
let opened = false;
let paused = false;
let left = 0;
let deadline = 0;
let interval;
let sound;
let saved = true;

function lobbyHint() {
  if (players.length < 3) return 'Для старта нужны минимум 3 игрока.';
  if (!canStart(players)) return 'Заполните имена всех игроков.';
  return saved ? 'Имена сохраняются на этом устройстве.' : 'Имена сохранятся до закрытия игры.';
}

function lobby() {
  return `${heading('Дота <em>Шпион</em>', 'Кто знает загаданную карту, а кто делает вид?')}
    <section class="panel glass" aria-label="Игроки">
      ${panelTitle('Игроки', 'players', `<span class="badge" aria-label="Участников: ${players.length}">${players.length}</span>`)}
      <ol class="players">${players.map((name, index) => `<li class="player glass">
        <span class="avatar">${icon('user')}</span>
        <input aria-label="Имя игрока ${index + 1}" data-player="${index}" value="${escape(name)}" maxlength="${nameLimit}" type="text"
          placeholder="Имя игрока" autocomplete="off" enterkeyhint="done" spellcheck="false">
        <button type="button" class="icon-button" data-action="remove" data-position="${index}" aria-label="Удалить игрока ${index + 1}">${icon('cross')}</button>
      </li>`).join('')}</ol>
      ${button('add', 'Добавить игрока', 'plus', 'ghost')}
    </section>
    ${button('settings', 'Настройки партии', 'next', 'primary trailing', !canStart(players))}
    <p id="lobby-hint" class="hint">${lobbyHint()}</p>`;
}

function stepper(title, detail, glyph, key, maximum, unit = '') {
  const value = settings[key];
  return `<section class="panel glass stepper">
    ${panelTitle(title, glyph)}
    <p>${detail}</p>
    <div class="step-controls" role="group" aria-label="${title}">
      <button type="button" class="icon-button" data-action="step" data-key="${key}" data-delta="-1" aria-label="Уменьшить" ${value <= 1 ? 'disabled' : ''}>${icon('minus')}</button>
      <output aria-live="polite">${value}${unit}</output>
      <button type="button" class="icon-button" data-action="step" data-key="${key}" data-delta="1" aria-label="Увеличить" ${value >= maximum ? 'disabled' : ''}>${icon('plus')}</button>
    </div>
  </section>`;
}

function setup() {
  const categories = [['hero', 'Герои Dota 2', 'players'], ['item', 'Предметы Dota 2', 'blade']];
  return `${button('lobby', 'К игрокам', 'back', 'back')}
    ${heading('Настройки партии', 'Выберите карты, число шпионов и время обсуждения.')}
    <section class="panel glass">
      ${panelTitle('Пул карточек', 'cards')}
      <div class="options">${categories.map(([key, label, glyph]) => `<label class="option glass">
        ${icon(glyph)}<span>${label}</span><input name="category" type="checkbox" value="${key}" ${settings.categories.has(key) ? 'checked' : ''}>
      </label>`).join('')}</div>
    </section>
    ${stepper('Шпионы', 'У мирных должно быть большинство', 'eye', 'spies', maxSpies(players.length))}
    ${stepper('Обсуждение', 'От 1 до 10 минут', 'timer', 'minutes', 10, ' мин')}
    ${button('start', 'Начать игру', 'next', 'primary trailing', !settings.categories.size)}
    <p id="pool-hint" class="hint" ${settings.categories.size ? 'hidden' : ''}>Выберите хотя бы одну категорию.</p>
    <p class="hint credit">by Caphes</p>`;
}

function role() {
  const top = `<p class="eyebrow">Игрок ${turn + 1} из ${round.players.length}</p>
    ${heading(escape(round.players[turn]), opened ? 'Запомни роль и скрой карту перед передачей телефона.' : 'Возьми телефон и нажми на карту. Остальным нужно отвернуться.')}`;
  if (!opened) {
    return `${top}<button type="button" class="sealed glass" data-action="open">
      ${mark(112)}<span>${icon('lock')} Показать роль</span>
    </button>`;
  }
  const card = round.spies.has(turn) ? `<article class="spy glass">
    ${mark(94)}<h2 tabindex="-1">Ты — шпион!</h2>
    <p>Не выдавай себя. Слушай подсказки и попробуй угадать, какого героя или предмет обсуждают остальные.</p>
  </article>` : cardMarkup(round.card);
  return `${top}<div class="flip">${card}</div>
    ${button('next', turn + 1 === round.players.length ? 'Скрыть и начать обсуждение' : 'Скрыть и передать', 'lock')}`;
}

function discussion() {
  return `${heading('Обсуждение', 'Задавайте друг другу вопросы и попробуйте вычислить шпиона.')}
    <section class="dial glass">
      <span class="glyph">${icon('timer')}</span>
      <div class="clock" role="timer" aria-label="Оставшееся время">${timeText(left)}</div>
      <progress max="${round.duration}" value="${left}" aria-label="Оставшаяся доля времени"></progress>
      ${paused ? '<p class="hint" role="status">Пауза</p>' : ''}
    </section>
    ${button('pause', paused ? 'Продолжить' : 'Пауза', paused ? 'play' : 'pause')}
    ${button('finish', 'Завершить досрочно', 'flag', 'secondary')}`;
}

function ended() {
  return `${heading('Обсуждение завершено', 'Пора узнать, кто был шпионом.')}
    <div class="sealed glass">${mark(112)}</div>
    ${button('reveal', 'Раскрыть карты', 'eye')}`;
}

function results() {
  const spies = round.players.flatMap((name, index, list) => round.spies.has(index)
    ? [list.indexOf(name) !== list.lastIndexOf(name) ? `${name} · игрок ${index + 1}` : name] : []);
  return `${heading('Карты раскрыты', 'Загаданная карта и имена шпионов.')}
    ${cardMarkup(round.card)}
    <section class="panel glass reveal">
      ${panelTitle(spies.length === 1 ? 'Шпион' : 'Шпионы', 'eye')}
      <ul>${spies.map(name => `<li>${escape(name)}</li>`).join('')}</ul>
    </section>
    ${button('again', 'Играть снова', 'repeat')}`;
}

const pages = { lobby, settings: setup, role, discussion, ended, results };

function render(focus = true) {
  root.dataset.screen = screen;
  root.innerHTML = `<div class="screen">${pages[screen]()}</div>`;
  if (focus) {
    window.scrollTo(0, 0);
    root.querySelector('h1')?.focus({ preventScroll: true });
  }
}

function go(next) {
  screen = next;
  render();
}

function unlockSound() {
  try {
    const Audio = window.AudioContext || window.webkitAudioContext;
    if (!Audio) return;
    sound ??= new Audio();
    if (sound.state !== 'running') sound.resume().catch(() => {});
  } catch {
    // The round stays playable when audio is unavailable.
  }
}

function chime() {
  if (sound?.state !== 'running') return;
  const begin = sound.currentTime;
  [523, 659, 784].forEach((frequency, index) => {
    const start = begin + index * 0.16;
    const oscillator = sound.createOscillator();
    const volume = sound.createGain();
    oscillator.frequency.value = frequency;
    volume.gain.setValueAtTime(0.001, start);
    volume.gain.exponentialRampToValueAtTime(0.20, start + 0.032);
    volume.gain.exponentialRampToValueAtTime(0.001, start + 0.33);
    oscillator.connect(volume).connect(sound.destination);
    oscillator.start(start);
    oscillator.stop(start + 0.34);
  });
}

function finish() {
  if (screen !== 'discussion') return;
  clearInterval(interval);
  chime();
  go('ended');
}

function refreshClock() {
  if (screen !== 'discussion' || paused || document.hidden) return;
  left = remaining(deadline, Date.now());
  if (!left) {
    finish();
    return;
  }
  const display = root.querySelector('.clock');
  const text = timeText(left);
  if (display.textContent !== text) display.textContent = text;
  root.querySelector('progress').value = left;
}

function runClock() {
  clearInterval(interval);
  deadline = Date.now() + left;
  interval = setInterval(refreshClock, 150);
}

function persist() {
  saved = savePlayers(storage, players);
}

const actions = {
  add() {
    players.push('');
    persist();
    render(false);
    root.querySelector(`[data-player="${players.length - 1}"]`).focus();
  },
  remove(control) {
    const position = Number(control.dataset.position);
    players.splice(position, 1);
    persist();
    render(false);
    const next = root.querySelector(`[data-position="${Math.min(position, players.length - 1)}"]`);
    (next || root.querySelector('[data-action="add"]')).focus({ preventScroll: true });
  },
  settings() {
    if (!canStart(players)) return;
    settings.spies = Math.min(settings.spies, maxSpies(players.length));
    go('settings');
  },
  lobby() { go('lobby'); },
  step(control) {
    const { key, delta } = control.dataset;
    const max = key === 'spies' ? maxSpies(players.length) : 10;
    const value = Math.max(1, Math.min(max, settings[key] + Number(delta)));
    settings[key] = value;
    const group = control.closest('.step-controls');
    const [minus, plus] = group.querySelectorAll('button');
    group.querySelector('output').textContent = key === 'minutes' ? `${value} мин` : value;
    minus.disabled = value <= 1;
    plus.disabled = value >= max;
    if (control.disabled) group.querySelector('button:not(:disabled)')?.focus();
  },
  start() {
    if (!canStart(players) || !settings.categories.size) return;
    unlockSound();
    round = deal(players, settings, deck);
    turn = 0;
    opened = false;
    go('role');
  },
  open() {
    opened = true;
    render(false);
    root.querySelector('.flip h2')?.focus({ preventScroll: true });
  },
  next() {
    opened = false;
    turn++;
    if (turn < round.players.length) {
      go('role');
      return;
    }
    unlockSound();
    left = round.duration;
    paused = false;
    go('discussion');
    runClock();
  },
  pause() {
    if (paused) {
      paused = false;
      unlockSound();
      runClock();
    } else {
      left = remaining(deadline, Date.now());
      if (!left) {
        finish();
        return;
      }
      paused = true;
      clearInterval(interval);
    }
    render(false);
    root.querySelector('[data-action="pause"]').focus({ preventScroll: true });
  },
  finish,
  reveal() { go('results'); },
  again() {
    round = undefined;
    go('lobby');
  }
};

root.addEventListener('click', event => {
  const control = event.target.closest('button[data-action]');
  if (control && !control.disabled) actions[control.dataset.action](control);
});

root.addEventListener('input', event => {
  const field = event.target;
  if (!field.hasAttribute('data-player')) return;
  players[Number(field.dataset.player)] = field.value;
  persist();
  root.querySelector('[data-action="settings"]').disabled = !canStart(players);
  root.querySelector('#lobby-hint').textContent = lobbyHint();
});

root.addEventListener('change', event => {
  const field = event.target;
  if (field.name !== 'category') return;
  if (field.checked) settings.categories.add(field.value);
  else settings.categories.delete(field.value);
  const ready = settings.categories.size > 0;
  root.querySelector('[data-action="start"]').disabled = !ready;
  root.querySelector('#pool-hint').hidden = ready;
});

root.addEventListener('keydown', event => {
  if (event.key === 'Enter' && !event.isComposing && event.target.hasAttribute('data-player')) {
    event.target.blur();
  }
});

function coverRole() {
  if (screen === 'role' && opened) {
    opened = false;
    render(false);
  }
}

document.addEventListener('visibilitychange', () => {
  if (document.hidden) coverRole();
  else refreshClock();
});
window.addEventListener('pagehide', coverRole);

async function boot() {
  try {
    const response = await fetch('./deck.json');
    if (!response.ok) throw new Error('Deck unavailable');
    deck = await response.json();
    render(false);
  } catch {
    root.innerHTML = '<section class="panel glass"><h1>Колода недоступна</h1><p>Обновите страницу, чтобы загрузить игру.</p></section>';
  } finally {
    root.removeAttribute('aria-busy');
  }

  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {
      // Online play is still available when offline storage is disabled.
    });
  }
}

boot();
