package ru.dotaspy.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static ru.dotaspy.game.Glass.INK;
import static ru.dotaspy.game.Glass.MINT;
import static ru.dotaspy.game.Glass.MUTED;
import static ru.dotaspy.game.Glass.ROSE;
import static ru.dotaspy.game.Glass.TEXT;

public final class MainActivity extends Activity {
    private enum Screen { LOBBY, SETTINGS, ROLE, DISCUSSION, ENDED, RESULTS }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::updateClock;
    private final Random random = new Random();

    private LocalData data;
    private ArrayList<String> names;
    private List<GameRules.Card> deck;
    private GameRules.Round round;
    private Screen screen = Screen.LOBBY;
    private int categories = GameRules.BOTH;
    private int spies = 1;
    private int minutes = 4;
    private int turn;
    private boolean roleOpen;
    private boolean timerPaused;
    private boolean foreground;
    private long deadline;
    private long left;

    private FrameLayout host;
    private LinearLayout page;
    private ScrollView scroll;
    private TextView clock;
    private ProgressBar progress;
    private ToneGenerator tone;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        FrameLayout root = new FrameLayout(this);
        Glass.Backdrop backdrop = new Glass.Backdrop(this);
        if (Build.VERSION.SDK_INT >= 31) {
            float blur = dp(18);
            backdrop.setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP));
        }
        root.addView(backdrop, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        host = new FrameLayout(this);
        root.addView(host, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                host.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                return insets;
            });
        }
        setContentView(root);

        data = new LocalData(this);
        names = data.loadNames();
        try {
            deck = data.loadDeck();
        } catch (IOException | JSONException error) {
            render(false, false);
            return;
        }
        if (state != null) restore(state);
        render(false, false);
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
        updateClock();
    }

    @Override protected void onPause() {
        foreground = false;
        handler.removeCallbacks(tick);
        if (screen == Screen.ROLE && roleOpen) {
            roleOpen = false;
            render(false, false);
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(tick);
        if (tone != null) tone.release();
        super.onDestroy();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("screen", screen.name());
        state.putInt("categories", categories);
        state.putInt("spies", spies);
        state.putInt("minutes", minutes);
        if (round == null) return;
        state.putString("card", round.card.id);
        state.putStringArrayList("players", new ArrayList<>(round.players));
        state.putBooleanArray("spyPositions", round.spies);
        state.putLong("duration", round.duration);
        state.putInt("turn", turn);
        state.putLong("left", left);
        state.putLong("deadline", deadline);
        state.putBoolean("timerPaused", timerPaused);
    }

    private void restore(Bundle state) {
        categories = state.getInt("categories", GameRules.BOTH);
        spies = state.getInt("spies", 1);
        minutes = state.getInt("minutes", 4);
        Screen savedScreen = Screen.valueOf(state.getString("screen", Screen.LOBBY.name()));
        if (savedScreen == Screen.SETTINGS && GameRules.canStart(names)) screen = Screen.SETTINGS;
        ArrayList<String> players = state.getStringArrayList("players");
        boolean[] spyPositions = state.getBooleanArray("spyPositions");
        if (players == null || spyPositions == null || players.size() != spyPositions.length) return;
        String cardId = state.getString("card");
        for (GameRules.Card card : deck) {
            if (!card.id.equals(cardId)) continue;
            round = new GameRules.Round(players, spyPositions, card, state.getLong("duration"));
            turn = state.getInt("turn");
            deadline = state.getLong("deadline");
            left = state.getLong("left");
            timerPaused = state.getBoolean("timerPaused");
            screen = savedScreen;
            break;
        }
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        switch (screen) {
            case LOBBY:
                super.onBackPressed();
                break;
            case SETTINGS:
            case ENDED:
            case RESULTS:
                resetRound();
                break;
            case ROLE:
                if (roleOpen) {
                    roleOpen = false;
                    render(false, false);
                    break;
                }
                confirmExit();
                break;
            case DISCUSSION:
                confirmExit();
                break;
        }
    }

    private void confirmExit() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Выйти из партии?")
                .setMessage("Игроки останутся в лобби.")
                .setNegativeButton("Продолжить", null)
                .setPositiveButton("Выйти", (ignored, which) -> resetRound())
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(Glass.surface(0xf3394b43, 0xf31a2522, 0x80d3f4e3, dp(24)));
        }
    }

    private void resetRound() {
        handler.removeCallbacks(tick);
        round = null;
        roleOpen = false;
        screen = Screen.LOBBY;
        render(true, false);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        return box;
    }

    private LinearLayout row() {
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        return box;
    }

    private void append(LinearLayout box, View view, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        params.topMargin = dp(top);
        box.addView(view, params);
    }

    private void padding(View view, int sides, int vertical) {
        view.setPadding(dp(sides), dp(vertical), dp(sides), dp(vertical));
    }

    private void flexible(LinearLayout box, View view) {
        box.addView(view, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
    }

    private void fixed(LinearLayout box, View view, int width, int height, int start) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(width), dp(height));
        params.setMarginStart(dp(start));
        box.addView(view, params);
    }

    private TextView text(CharSequence value, int size, int color, boolean bold) {
        return Glass.text(this, value, size, color, bold);
    }

    private void heading(String title, String description) {
        TextView name = text(title, 36, TEXT, true);
        name.setLetterSpacing(-.052f);
        if (Build.VERSION.SDK_INT >= 28) name.setAccessibilityHeading(true);
        append(page, name, 16);
        TextView detail = text(description, 15, MUTED, false);
        detail.setLineSpacing(dp(2), 1);
        append(page, detail, 2);
    }

    private void render(boolean transition, boolean flip) {
        host.removeAllViews();
        scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);
        host.addView(scroll, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        FrameLayout frame = new FrameLayout(this);
        scroll.addView(frame, new ScrollView.LayoutParams(MATCH_PARENT, WRAP_CONTENT));

        page = column();
        page.setFocusableInTouchMode(true);
        padding(page, 18, 18);
        int width = getResources().getConfiguration().screenWidthDp > 660 ? dp(610) : MATCH_PARENT;
        frame.addView(page, new FrameLayout.LayoutParams(width, WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL));

        LinearLayout brand = row();
        ImageView emblem = new ImageView(this);
        emblem.setImageResource(R.drawable.ic_dota_spy);
        fixed(brand, emblem, 38, 38, 0);
        TextView label = text("Дота Шпион", 16, TEXT, true);
        flexible(brand, label);
        TextView offline = text("●  Офлайн", 12, MINT, false);
        offline.setBackground(Glass.surface(0x1e8ce9b4, 0x098ce9b4, 0x417dce9e, dp(20)));
        padding(offline, 10, 5);
        brand.addView(offline);
        append(page, brand, 0);

        if (deck == null) {
            heading("Колода недоступна", "Переустановите приложение из APK.");
            return;
        }

        switch (screen) {
            case LOBBY: renderLobby(); break;
            case SETTINGS: renderSettings(); break;
            case ROLE: renderRole(flip); break;
            case DISCUSSION: renderDiscussion(); break;
            case ENDED: renderEnded(); break;
            case RESULTS: renderResults(); break;
        }
        page.requestFocus();
        if (transition) {
            page.setAlpha(.72f);
            page.setTranslationY(dp(6));
            page.animate().alpha(1).translationY(0).setDuration(200).start();
        }
    }

    private LinearLayout panel(boolean rose) {
        LinearLayout box = column();
        box.setBackground(rose ? Glass.surface(0x4d984759, 0x2432272c, 0x69f4aeb9, dp(25))
                : Glass.panel(dp(25)));
        box.setElevation(dp(3));
        padding(box, 14, 13);
        return box;
    }

    private void panelTitle(LinearLayout box, String value, Glass.Icon icon, String suffix) {
        LinearLayout line = row();
        Glass.Glyph glyph = new Glass.Glyph(this, icon, MINT);
        glyph.setBackground(Glass.surface(0x2ea6ebc4, 0x0ba6ebc4, 0x409df1c5, dp(11)));
        padding(glyph, 6, 6);
        fixed(line, glyph, 35, 35, 0);
        TextView title = text(value, 17, TEXT, true);
        title.setPaddingRelative(dp(9), 0, 0, 0);
        flexible(line, title);
        if (suffix != null) line.addView(text(suffix, 14, MINT, true));
        append(box, line, 0);
    }

    private void clickable(View view, String label, Runnable action) {
        view.setContentDescription(label);
        view.setFocusable(true);
        view.setOnClickListener(clicked -> action.run());
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View hostView, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(hostView, info);
                info.setClassName("android.widget.Button");
            }
        });
    }

    private View button(String label, Glass.Icon icon, boolean primary, Runnable click) {
        LinearLayout view = row();
        view.setGravity(Gravity.CENTER);
        view.setMinimumHeight(dp(53));
        padding(view, 12, 6);
        view.setBackground(Glass.touch(primary
                ? Glass.surface(0xffc4f9dc, 0xff81d4aa, 0xffe0ffed, dp(15))
                : Glass.panel(dp(15))));
        int foregroundColor = primary ? 0xff102d21 : TEXT;
        fixed(view, new Glass.Glyph(this, icon, foregroundColor), 23, 23, 0);
        TextView caption = text(label, 16, foregroundColor, true);
        caption.setPaddingRelative(dp(8), 0, 0, 0);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        view.addView(caption);
        clickable(view, label, click);
        return view;
    }

    private View iconButton(Glass.Icon icon, String label, boolean enabled, Runnable click) {
        Glass.Glyph view = new Glass.Glyph(this, icon, MINT);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        view.setBackground(Glass.touch(Glass.panel(dp(11))));
        clickable(view, label, click);
        enable(view, enabled);
        return view;
    }

    private void enable(View view, boolean value) {
        view.setEnabled(value);
        view.setAlpha(value ? 1f : .45f);
    }

    private void renderLobby() {
        heading("Дота Шпион", "Кто за столом знает загаданную карту, а кто только делает вид?");
        LinearLayout box = panel(false);
        panelTitle(box, "Игроки", Glass.Icon.PLAYERS, String.valueOf(names.size()));
        append(page, box, 16);

        View settings = button("Настройки партии", Glass.Icon.ARROW, true, () -> {
            if (!GameRules.canStart(names)) return;
            hideKeyboard();
            spies = Math.min(spies, GameRules.maxSpies(names.size()));
            screen = Screen.SETTINGS;
            render(true, false);
        });
        TextView hint = text("", 13, MUTED, false);
        hint.setGravity(Gravity.CENTER);

        for (int index = 0; index < names.size(); index++) {
            final int position = index;
            LinearLayout line = row();
            line.setBackground(Glass.panel(dp(14)));
            padding(line, 6, 3);
            fixed(line, new Glass.Glyph(this, Glass.Icon.PERSON, MUTED), 30, 30, 0);
            EditText input = new EditText(this);
            input.setTag("player-" + index);
            input.setBackgroundColor(Color.TRANSPARENT);
            input.setTextColor(TEXT);
            input.setHintTextColor(MUTED);
            input.setTextSize(16);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            input.setSingleLine(true);
            input.setImeOptions(EditorInfo.IME_ACTION_DONE);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(GameRules.MAX_NAME)});
            input.setHint("Имя игрока " + (index + 1));
            input.setText(names.get(index));
            input.setOnEditorActionListener((view, action, event) -> {
                if (action != EditorInfo.IME_ACTION_DONE) return false;
                hideKeyboard();
                page.requestFocus();
                return true;
            });
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                    names.set(position, value.toString());
                    data.saveNames(names);
                    refreshLobby(settings, hint);
                }
                @Override public void afterTextChanged(Editable value) {}
            });
            flexible(line, input);
            View remove = iconButton(Glass.Icon.REMOVE, "Удалить игрока " + (index + 1), true, () -> {
                hideKeyboard();
                names.remove(position);
                data.saveNames(names);
                render(false, false);
            });
            fixed(line, remove, 44, 44, 4);
            append(box, line, 8);
        }

        append(box, button("Добавить игрока", Glass.Icon.ADD, false, () -> {
            names.add("");
            data.saveNames(names);
            render(false, false);
            EditText added = page.findViewWithTag("player-" + (names.size() - 1));
            if (added != null) {
                added.requestFocus();
                added.post(() -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(added, InputMethodManager.SHOW_IMPLICIT));
            }
        }), 10);
        append(page, settings, 13);
        append(page, hint, 7);
        refreshLobby(settings, hint);
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void refreshLobby(View settings, TextView hint) {
        boolean ready = GameRules.canStart(names);
        enable(settings, ready);
        if (names.size() < 3) hint.setText("Для старта нужны минимум 3 игрока.");
        else if (!ready) hint.setText("Заполните имена всех игроков.");
        else hint.setText("Имена сохраняются на этом устройстве.");
    }

    private void renderSettings() {
        append(page, button("К игрокам", Glass.Icon.BACK, false, this::resetRound), 13);
        heading("Настройки партии", "Выберите карты, число шпионов и время на обсуждение.");

        View start = button("Начать игру", Glass.Icon.ARROW, true, () -> {
            if (categories == 0) return;
            round = GameRules.deal(names, categories, spies, minutes, deck, random);
            turn = 0;
            roleOpen = false;
            screen = Screen.ROLE;
            render(true, false);
        });
        LinearLayout pool = panel(false);
        panelTitle(pool, "Пул карточек", Glass.Icon.CARDS, null);
        addCategory(pool, "Герои Dota 2", GameRules.HEROES, start);
        addCategory(pool, "Предметы Dota 2", GameRules.ITEMS, start);
        append(page, pool, 16);

        LinearLayout spyBox = panel(false);
        panelTitle(spyBox, "Шпионы", Glass.Icon.EYE, null);
        stepper(spyBox, true);
        append(page, spyBox, 9);

        LinearLayout timeBox = panel(false);
        panelTitle(timeBox, "Обсуждение", Glass.Icon.TIMER, null);
        stepper(timeBox, false);
        append(page, timeBox, 9);

        enable(start, categories != 0);
        append(page, start, 13);
    }

    private void addCategory(LinearLayout panel, String label, int category, View start) {
        CheckBox option = new CheckBox(this);
        option.setText(label);
        option.setTextColor(TEXT);
        option.setTextSize(16);
        option.setMinHeight(dp(47));
        option.setBackground(Glass.touch(Glass.panel(dp(13))));
        option.setButtonTintList(ColorStateList.valueOf(MINT));
        option.setChecked((categories & category) != 0);
        option.setOnCheckedChangeListener((view, checked) -> {
            categories = checked ? categories | category : categories & ~category;
            enable(start, categories != 0);
        });
        append(panel, option, 7);
    }

    private void stepper(LinearLayout panel, boolean spyCounter) {
        int maximum = spyCounter ? GameRules.maxSpies(names.size()) : 10;
        String field = spyCounter ? "число шпионов" : "время обсуждения";
        LinearLayout line = row();
        TextView caption = text("", 16, MINT, true);
        caption.setGravity(Gravity.CENTER);
        caption.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);

        View minus = iconButton(Glass.Icon.MINUS, "Уменьшить " + field, false, () -> {});
        View plus = iconButton(Glass.Icon.PLUS, "Увеличить " + field, false, () -> {});
        Runnable refresh = () -> {
            int value = spyCounter ? spies : minutes;
            caption.setText(spyCounter ? String.valueOf(value) : value + " мин");
            enable(minus, value > 1);
            enable(plus, value < maximum);
        };
        minus.setOnClickListener(view -> {
            if (spyCounter) spies--;
            else minutes--;
            refresh.run();
        });
        plus.setOnClickListener(view -> {
            if (spyCounter) spies++;
            else minutes++;
            refresh.run();
        });
        fixed(line, minus, 46, 45, 0);
        flexible(line, caption);
        fixed(line, plus, 46, 45, 0);
        append(panel, line, 7);
        refresh.run();
    }

    private void renderRole(boolean flip) {
        TextView indicator = text("Игрок " + (turn + 1) + " из " + round.players.size(), 13, MINT, true);
        append(page, indicator, 17);
        heading(round.players.get(turn), roleOpen
                ? "Запомни роль и скрой карту перед передачей телефона."
                : "Возьми телефон и нажми на карту. Остальным нужно отвернуться.");
        if (!roleOpen) {
            LinearLayout cover = sealed();
            TextView label = text("Показать роль", 17, MINT, true);
            label.setGravity(Gravity.CENTER);
            append(cover, label, 10);
            cover.setBackground(Glass.touch(Glass.panel(dp(25))));
            clickable(cover, "Показать роль", () -> {
                roleOpen = true;
                render(false, true);
            });
            append(page, cover, 15);
            return;
        }

        View card = round.spies[turn] ? spyCard() : secretCard();
        append(page, card, 15);
        if (flip && (Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled())) {
            card.setCameraDistance(dp(1600));
            card.setRotationY(-85);
            card.animate().rotationY(0).setDuration(410).setInterpolator(new DecelerateInterpolator()).start();
        }
        append(page, button(turn + 1 == round.players.size() ? "Скрыть и начать обсуждение" : "Скрыть и передать",
                Glass.Icon.LOCK, true, this::nextTurn), 13);
    }

    private LinearLayout sealed() {
        LinearLayout cover = panel(false);
        cover.setGravity(Gravity.CENTER);
        cover.setMinimumHeight(dp(237));
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_dota_spy);
        cover.addView(mark, new LinearLayout.LayoutParams(dp(105), dp(105)));
        return cover;
    }

    private View spyCard() {
        LinearLayout card = panel(true);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_dota_spy);
        card.addView(mark, new LinearLayout.LayoutParams(dp(94), dp(94)));
        TextView title = text("Ты — шпион!", 32, ROSE, true);
        title.setGravity(Gravity.CENTER);
        append(card, title, 5);
        TextView clue = text("Не выдавай себя. Слушай подсказки и попробуй угадать, какого героя или предмет обсуждают остальные.",
                16, TEXT, false);
        clue.setGravity(Gravity.CENTER);
        clue.setLineSpacing(dp(3), 1);
        append(card, clue, 8);
        return card;
    }

    private View secretCard() {
        GameRules.Card card = round.card;
        boolean hero = card.category == GameRules.HEROES;
        LinearLayout wrap = column();
        wrap.setBackground(Glass.panel(dp(25)));
        wrap.setClipToOutline(true);
        wrap.setElevation(dp(3));

        FrameLayout art = new FrameLayout(this);
        Bitmap bitmap = data.art(card);
        boolean portrait = bitmap != null && bitmap.getHeight() > bitmap.getWidth();
        art.addView(new Glass.CardFallback(this, card), new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        if (bitmap != null) {
            ImageView image = new ImageView(this);
            image.setImageBitmap(bitmap);
            image.setScaleType(hero && !portrait ? ImageView.ScaleType.CENTER_CROP : ImageView.ScaleType.FIT_CENTER);
            art.addView(image, new FrameLayout.LayoutParams(hero ? MATCH_PARENT : dp(154),
                    hero ? MATCH_PARENT : dp(110), Gravity.CENTER));
        } else {
            art.addView(new Glass.Glyph(this, hero ? Glass.Icon.PERSON : Glass.Icon.BLADE, MINT),
                    new FrameLayout.LayoutParams(dp(74), dp(74), Gravity.CENTER));
        }
        int artHeight = hero ? (portrait ? 250 : 200) : 150;
        wrap.addView(art, new LinearLayout.LayoutParams(MATCH_PARENT, dp(artHeight)));
        LinearLayout copy = column();
        padding(copy, 17, 13);
        append(copy, text(hero ? "ГЕРОЙ DOTA 2" : "ПРЕДМЕТ DOTA 2", 12, MINT, true), 0);
        append(copy, text(card.name, 27, TEXT, true), 4);
        TextView description = text(card.description, 16, MUTED, false);
        description.setLineSpacing(dp(2), 1);
        append(copy, description, 5);
        append(wrap, copy, 0);
        return wrap;
    }

    private void nextTurn() {
        roleOpen = false;
        turn++;
        if (turn < round.players.size()) {
            render(true, false);
            return;
        }
        screen = Screen.DISCUSSION;
        left = round.duration;
        deadline = SystemClock.elapsedRealtime() + left;
        timerPaused = false;
        render(true, false);
        updateClock();
    }

    private void renderDiscussion() {
        heading("Обсуждение", "Задавайте друг другу вопросы и попробуйте вычислить шпиона.");
        LinearLayout dial = panel(false);
        dial.setGravity(Gravity.CENTER_HORIZONTAL);
        Glass.Glyph timer = new Glass.Glyph(this, Glass.Icon.TIMER, MINT);
        dial.addView(timer, new LinearLayout.LayoutParams(dp(42), dp(42)));
        clock = text(GameRules.clock(left), 70, TEXT, true);
        clock.setGravity(Gravity.CENTER);
        clock.setFontFeatureSettings("tnum");
        append(dial, clock, 3);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax((int) round.duration);
        progress.setProgress((int) left);
        progress.setProgressTintList(ColorStateList.valueOf(MINT));
        progress.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        append(dial, progress, 5);
        if (timerPaused) {
            TextView status = text("Пауза", 14, MINT, true);
            status.setGravity(Gravity.CENTER);
            append(dial, status, 3);
        }
        append(page, dial, 16);
        append(page, button(timerPaused ? "Продолжить" : "Пауза", timerPaused ? Glass.Icon.PLAY : Glass.Icon.PAUSE,
                true, this::toggleTimer), 14);
        append(page, button("Завершить досрочно", Glass.Icon.FLAG, false, this::finishRound), 8);
    }

    private void updateClock() {
        handler.removeCallbacks(tick);
        if (!foreground || screen != Screen.DISCUSSION || timerPaused) return;
        left = GameRules.remaining(deadline, SystemClock.elapsedRealtime());
        if (left == 0) {
            finishRound();
        } else {
            String value = GameRules.clock(left);
            if (!value.contentEquals(clock.getText())) clock.setText(value);
            progress.setProgress((int) left);
            handler.postDelayed(tick, 130);
        }
    }

    private void toggleTimer() {
        if (timerPaused) {
            deadline = SystemClock.elapsedRealtime() + left;
            timerPaused = false;
        } else {
            left = GameRules.remaining(deadline, SystemClock.elapsedRealtime());
            if (left == 0) {
                finishRound();
                return;
            }
            timerPaused = true;
        }
        render(false, false);
        updateClock();
    }

    private void finishRound() {
        if (screen != Screen.DISCUSSION) return;
        handler.removeCallbacks(tick);
        screen = Screen.ENDED;
        try {
            if (tone == null) tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 75);
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 450);
        } catch (RuntimeException ignored) {
            // The results stay available if audio playback is unavailable.
        }
        render(true, false);
    }

    private void renderEnded() {
        heading("Обсуждение завершено", "Пора узнать, кто был шпионом и какую карту видели мирные.");
        append(page, sealed(), 16);
        append(page, button("Раскрыть карты", Glass.Icon.EYE, true, () -> {
            screen = Screen.RESULTS;
            render(true, false);
        }), 13);
    }

    private void renderResults() {
        heading("Карты раскрыты", "Загаданная карта и имена шпионов.");
        append(page, secretCard(), 16);
        LinearLayout box = panel(true);
        int spyCount = 0;
        for (boolean spy : round.spies) if (spy) spyCount++;
        panelTitle(box, spyCount == 1 ? "Шпион" : "Шпионы", Glass.Icon.EYE, null);
        for (int index = 0; index < round.players.size(); index++) {
            if (round.spies[index]) {
                String name = round.players.get(index);
                if (java.util.Collections.frequency(round.players, name) > 1) name += " · игрок " + (index + 1);
                append(box, text(name, 17, ROSE, true), 5);
            }
        }
        append(page, box, 10);
        append(page, button("Играть снова", Glass.Icon.REPLAY, true, this::resetRound), 13);
    }
}
