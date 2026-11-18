package ru.dotaspy.game;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

final class Glass {
    static final int INK = 0xff0b1010;
    static final int TEXT = 0xffedf6f0;
    static final int MUTED = 0xffbacbc2;
    static final int MINT = 0xffa6ebc4;
    static final int ROSE = 0xfff1bbc3;

    enum Icon {
        PLAYERS, PERSON, CARDS, BLADE, EYE, TIMER, LOCK, ADD, REMOVE, PLUS, MINUS, ARROW, BACK, PAUSE, PLAY, FLAG, REPLAY
    }

    static GradientDrawable surface(int top, int bottom, int line, int radius) {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        drawable.setCornerRadius(radius);
        drawable.setStroke(1, line);
        return drawable;
    }

    static GradientDrawable panel(int radius) {
        return surface(0x24d7ffea, 0x10b8ddd0, 0x43c4e9d5, radius);
    }

    static RippleDrawable touch(GradientDrawable background) {
        return new RippleDrawable(ColorStateList.valueOf(0x399ef4c5), background, null);
    }

    static TextView text(Context context, CharSequence value, int size, int color, boolean bold) {
        TextView text = new TextView(context);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        text.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return text;
    }

    static final class Glyph extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Icon icon;

        Glyph(Context context, Icon icon, int color) {
            super(context);
            this.icon = icon;
            paint.setColor(color);
            paint.setStrokeWidth(1.55f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float size = Math.min(getWidth(), getHeight()) * .82f;
            canvas.save();
            canvas.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
            canvas.scale(size / 24, size / 24);
            switch (icon) {
                case PLAYERS:
                    canvas.drawCircle(9, 7, 3.3f, paint);
                    canvas.drawRoundRect(3, 13, 15, 21, 3, 3, paint);
                    canvas.drawArc(13, 4, 20, 11, -90, 180, false, paint);
                    line(canvas, 17, 14, 20, 16, 21, 20);
                    break;
                case PERSON:
                    canvas.drawCircle(12, 7, 3.3f, paint);
                    canvas.drawRoundRect(5, 13, 19, 21, 4, 4, paint);
                    break;
                case CARDS:
                    canvas.drawRoundRect(7, 6, 20, 21, 2, 2, paint);
                    line(canvas, 4, 17, 4, 3, 16, 3);
                    line(canvas, 10, 13.5f, 17, 13.5f);
                    line(canvas, 13.5f, 10, 13.5f, 17);
                    break;
                case BLADE:
                    line(canvas, 4, 20, 11, 13, 9, 11, 13, 6, 21, 3, 18, 11, 13, 15, 8, 10);
                    break;
                case EYE:
                    Path eye = new Path();
                    eye.moveTo(2, 12);
                    eye.quadTo(12, 0, 22, 12);
                    eye.quadTo(12, 24, 2, 12);
                    canvas.drawPath(eye, paint);
                    canvas.drawCircle(12, 12, 2.5f, paint);
                    break;
                case TIMER:
                    canvas.drawCircle(12, 13, 8, paint);
                    line(canvas, 9, 2, 15, 2);
                    line(canvas, 12, 8, 12, 13, 15, 15);
                    break;
                case LOCK:
                    canvas.drawRoundRect(5, 10, 19, 21, 2, 2, paint);
                    canvas.drawArc(8, 3, 16, 11, 180, 180, false, paint);
                    line(canvas, 8, 7, 8, 10);
                    line(canvas, 16, 7, 16, 10);
                    line(canvas, 12, 14, 12, 17);
                    break;
                case ADD:
                case PLUS:
                    line(canvas, 12, 5, 12, 19);
                    line(canvas, 5, 12, 19, 12);
                    break;
                case REMOVE:
                    line(canvas, 6, 6, 18, 18);
                    line(canvas, 18, 6, 6, 18);
                    break;
                case MINUS:
                    line(canvas, 5, 12, 19, 12);
                    break;
                case ARROW:
                    line(canvas, 4, 12, 19, 12, 13, 6);
                    line(canvas, 19, 12, 13, 18);
                    break;
                case BACK:
                    line(canvas, 20, 12, 5, 12, 11, 6);
                    line(canvas, 5, 12, 11, 18);
                    break;
                case PAUSE:
                    line(canvas, 9, 5, 9, 19);
                    line(canvas, 15, 5, 15, 19);
                    break;
                case PLAY:
                    line(canvas, 8, 5, 19, 12, 8, 19, 8, 5);
                    break;
                case FLAG:
                    line(canvas, 5, 21, 5, 4, 20, 4, 17, 9, 20, 14, 5, 14);
                    break;
                case REPLAY:
                    canvas.drawArc(4, 4, 20, 20, -145, 270, false, paint);
                    line(canvas, 4, 4, 4, 10, 10, 10);
                    break;
            }
            canvas.restore();
        }

        private void line(Canvas canvas, float... points) {
            Path path = new Path();
            path.moveTo(points[0], points[1]);
            for (int index = 2; index < points.length; index += 2) {
                path.lineTo(points[index], points[index + 1]);
            }
            canvas.drawPath(path, paint);
        }
    }

    static final class Backdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private Shader base;
        private Shader green;
        private Shader red;

        Backdrop(Context context) {
            super(context);
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            if (width == 0 || height == 0) return;
            base = new LinearGradient(0, 0, width, height, 0xff16241e, INK, Shader.TileMode.CLAMP);
            green = new RadialGradient(width * .08f, height * .22f, height * .54f, 0xff284b3a, Color.TRANSPARENT, Shader.TileMode.CLAMP);
            red = new RadialGradient(width * .95f, height * .92f, height * .46f, 0xff43262b, Color.TRANSPARENT, Shader.TileMode.CLAMP);
        }

        @Override protected void onDraw(Canvas canvas) {
            for (Shader shader : new Shader[]{base, green, red}) {
                paint.setShader(shader);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        }
    }

    static final class CardFallback extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;
        private Shader glow;

        CardFallback(Context context, GameRules.Card card) {
            super(context);
            float hue = (card.id.hashCode() & 0xffff) % 360;
            color = Color.HSVToColor(new float[]{hue, .46f, .49f});
            setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            if (width > 0 && height > 0) {
                glow = new RadialGradient(width / 2f, height / 2f, width * .56f, color, INK, Shader.TileMode.CLAMP);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            paint.setShader(glow);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(0x64d8f8e8);
            paint.setStrokeWidth(1.25f);
            float radius = getHeight() * .37f;
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius, paint);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, radius * 1.13f, paint);
        }
    }
}
