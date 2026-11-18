const key = 'dota-spy.players.v1';
export const nameLimit = 28;
export const canStart = players => players.length >= 3 && players.every(name => name.trim());
export const maxSpies = count => Math.max(0, Math.min(3, Math.floor((count - 1) / 2)));

export function loadPlayers(storage) {
  try {
    const players = JSON.parse(storage.getItem(key));
    if (Array.isArray(players) && players.every(name => typeof name === 'string')) {
      return players.map(name => name.slice(0, nameLimit));
    }
  } catch {
    // An unavailable storage backend still allows a party.
  }
  return ['Игрок 1', 'Игрок 2', 'Игрок 3'];
}

export function savePlayers(storage, players) {
  try {
    storage.setItem(key, JSON.stringify(players));
    return true;
  } catch {
    return false;
  }
}

export function deal(players, settings, deck, random = Math.random) {
  const pool = deck.filter(card => settings.categories.has(card.category));
  if (!canStart(players) || !pool.length ||
      !Number.isInteger(settings.spies) || settings.spies < 1 || settings.spies > maxSpies(players.length) ||
      !Number.isInteger(settings.minutes) || settings.minutes < 1 || settings.minutes > 10) {
    throw new RangeError('Проверьте игроков и настройки партии.');
  }

  const positions = players.map((_, position) => position);
  for (let index = 0; index < settings.spies; index++) {
    const swap = index + Math.floor(random() * (positions.length - index));
    [positions[index], positions[swap]] = [positions[swap], positions[index]];
  }
  return {
    players: players.map(name => name.trim()),
    spies: new Set(positions.slice(0, settings.spies)),
    card: pool[Math.floor(random() * pool.length)],
    duration: settings.minutes * 60_000
  };
}

export const remaining = (deadline, now) => Math.max(0, deadline - now);

export function timeText(milliseconds) {
  const seconds = Math.ceil(milliseconds / 1000);
  return [Math.floor(seconds / 60), seconds % 60].map(part => String(part).padStart(2, '0')).join(':');
}
