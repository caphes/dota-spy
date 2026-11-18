package ru.dotaspy.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class LocalData {
    private final Context context;
    private final SharedPreferences preferences;
    private static final String PLAYERS = "players";

    LocalData(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("dota-spy", Context.MODE_PRIVATE);
    }

    ArrayList<String> loadNames() {
        String raw = preferences.getString(PLAYERS, null);
        if (raw != null) {
            try {
                JSONArray stored = new JSONArray(raw);
                ArrayList<String> names = new ArrayList<>(stored.length());
                for (int index = 0; index < stored.length(); index++) names.add(stored.getString(index));
                return names;
            } catch (JSONException ignored) {
                // Restore the default lobby if saved data cannot be read.
            }
        }
        return new ArrayList<>(Arrays.asList("Игрок 1", "Игрок 2", "Игрок 3"));
    }

    void saveNames(List<String> players) {
        preferences.edit().putString(PLAYERS, new JSONArray(players).toString()).apply();
    }

    List<GameRules.Card> loadDeck() throws IOException, JSONException {
        try (InputStream stream = context.getAssets().open("deck.json");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            JSONArray rows = new JSONArray(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
            List<GameRules.Card> cards = new ArrayList<>(rows.length());
            for (int index = 0; index < rows.length(); index++) {
                JSONObject row = rows.getJSONObject(index);
                int category = row.getString("category").equals("hero") ? GameRules.HEROES : GameRules.ITEMS;
                String localImage = row.optString("localImage");
                cards.add(new GameRules.Card(row.getString("id"), row.getString("name"),
                        row.getString("description"), assetPath(row.getString("image")),
                        localImage.isEmpty() ? null : assetPath(localImage), category));
            }
            return cards;
        }
    }

    Bitmap art(GameRules.Card card) {
        if (card.localAsset != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decodeArt(card.localAsset, options);
            if (options.outWidth > 0 && options.outHeight > 0) {
                options.inJustDecodeBounds = false;
                options.inSampleSize = 1;
                while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1600) {
                    options.inSampleSize *= 2;
                }
                Bitmap photo = decodeArt(card.localAsset, options);
                if (photo != null) return photo;
            }
        }
        return decodeArt(card.asset, null);
    }

    private static String assetPath(String path) {
        return path.substring("./assets/".length());
    }

    private Bitmap decodeArt(String path, BitmapFactory.Options options) {
        try (InputStream stream = context.getAssets().open(path)) {
            return BitmapFactory.decodeStream(stream, null, options);
        } catch (IOException ignored) {
            return null;
        }
    }
}
