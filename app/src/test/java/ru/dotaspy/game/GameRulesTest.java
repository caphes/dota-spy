package ru.dotaspy.game;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class GameRulesTest {
    private final List<GameRules.Card> deck = Arrays.asList(
            new GameRules.Card("pudge", "Pudge", "Hero", "cards/hero/pudge.png", null, GameRules.HEROES),
            new GameRules.Card("blink", "Blink Dagger", "Item", "cards/item/blink.png", null, GameRules.ITEMS));

    @Test public void dealRespectsSelectedPoolAndUniqueSpyPositionsWithDuplicateNames() {
        List<String> names = Arrays.asList(" Лена ", "Саша", "Саша", "Илья", "Юля", "Миша", "Федя");
        GameRules.Round round = GameRules.deal(names, GameRules.ITEMS, 3, 10, deck, new Random(1));
        int spyCount = 0;
        for (boolean spy : round.spies) if (spy) spyCount++;
        assertEquals(3, spyCount);
        assertEquals("Лена", round.players.get(0));
        assertEquals("Blink Dagger", round.card.name);
        assertEquals(600000L, round.duration);
    }

    @Test public void validatesRosterCategoryAndTime() {
        List<String> names = Arrays.asList("А", "Б", "В");
        assertFalse(GameRules.canStart(Arrays.asList("А", "Б")));
        assertFalse(GameRules.canStart(Arrays.asList("А", "Б", " \u00a0")));
        assertThrows(IllegalArgumentException.class, () -> GameRules.deal(names, 0, 1, 1, deck, new Random()));
        assertThrows(IllegalArgumentException.class, () -> GameRules.deal(names, GameRules.BOTH, 2, 1, deck, new Random()));
        assertThrows(IllegalArgumentException.class, () -> GameRules.deal(names, GameRules.BOTH, 1, 11, deck, new Random()));
        int[] counts = {0, 2, 3, 4, 5, 6, 7, 100};
        int[] limits = {0, 0, 1, 1, 2, 2, 3, 3};
        for (int index = 0; index < counts.length; index++) {
            assertEquals(limits[index], GameRules.maxSpies(counts[index]));
        }
    }

    @Test public void formatsCountdownAndClampsExpiredTime() {
        assertEquals("10:00", GameRules.clock(600000L));
        assertEquals("00:01", GameRules.clock(GameRules.remaining(100_000, 99_999)));
        assertEquals("00:00", GameRules.clock(GameRules.remaining(100_000, 150_000)));
    }
}
