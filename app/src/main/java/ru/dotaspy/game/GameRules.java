package ru.dotaspy.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class GameRules {
    static final int HEROES = 1;
    static final int ITEMS = 2;
    static final int BOTH = HEROES | ITEMS;
    static final int MAX_NAME = 28;

    static final class Card {
        final String id;
        final String name;
        final String description;
        final String asset;
        final String localAsset;
        final int category;

        Card(String id, String name, String description, String asset, String localAsset, int category) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.asset = asset;
            this.localAsset = localAsset;
            this.category = category;
        }
    }

    static final class Round {
        final List<String> players;
        final boolean[] spies;
        final Card card;
        final long duration;

        Round(List<String> players, boolean[] spies, Card card, long duration) {
            this.players = Collections.unmodifiableList(new ArrayList<>(players));
            this.spies = spies.clone();
            this.card = card;
            this.duration = duration;
        }
    }

    static int maxSpies(int playerCount) {
        return Math.max(0, Math.min(3, (playerCount - 1) / 2));
    }

    static boolean canStart(List<String> players) {
        if (players.size() < 3) return false;
        for (String player : players) {
            if (cleanName(player).isEmpty()) return false;
        }
        return true;
    }

    static String cleanName(String name) {
        int first = 0;
        int last = name.length();
        while (first < last && isSpace(name.charAt(first))) first++;
        while (last > first && isSpace(name.charAt(last - 1))) last--;
        return name.substring(first, last);
    }

    private static boolean isSpace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    static Round deal(List<String> players, int mask, int spyCount, int minutes, List<Card> deck, Random random) {
        List<Card> pool = new ArrayList<>();
        for (Card card : deck) {
            if ((card.category & mask) != 0) pool.add(card);
        }
        if (!canStart(players) || pool.isEmpty() || (mask & ~BOTH) != 0 ||
                spyCount < 1 || spyCount > maxSpies(players.size()) || minutes < 1 || minutes > 10) {
            throw new IllegalArgumentException("Проверьте игроков и настройки партии.");
        }

        List<String> names = new ArrayList<>();
        List<Integer> positions = new ArrayList<>();
        for (int index = 0; index < players.size(); index++) {
            positions.add(index);
            names.add(cleanName(players.get(index)));
        }
        boolean[] spies = new boolean[names.size()];
        for (int index = 0; index < spyCount; index++) {
            int swap = index + random.nextInt(positions.size() - index);
            Collections.swap(positions, index, swap);
            spies[positions.get(index)] = true;
        }
        return new Round(names, spies, pool.get(random.nextInt(pool.size())), minutes * 60_000L);
    }

    static long remaining(long deadline, long now) {
        return Math.max(deadline - now, 0);
    }

    static String clock(long millis) {
        long seconds = (millis + 999) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
