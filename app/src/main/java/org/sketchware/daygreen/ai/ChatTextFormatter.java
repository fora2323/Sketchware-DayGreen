package org.sketchware.daygreen.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;

/**
 * Renders plain agent text as Markdown (bold, inline code, lists),
 * and highlights Android event names in blue with a thin rounded background.
 * Tapping one calls the listener.
 */
public final class ChatTextFormatter {

    public interface OnEventClick {
        void onClick(String eventName);
    }

    private static final int COLOR_EVENT = 0xFF4FA3FF;
    private static final int COLOR_EVENT_BG = 0x264FA3FF;

    private static final Pattern EVENT_PATTERN = buildEventPattern();

    private final Markwon markwon;
    private final float density;

    public ChatTextFormatter(Context context) {
        this.markwon = Markwon.create(context);
        this.density = context.getResources().getDisplayMetrics().density;
    }

    public Spannable format(String text, OnEventClick listener) {
        Spannable out = new SpannableStringBuilder(markwon.toMarkdown(text));

        Matcher m = EVENT_PATTERN.matcher(out);
        while (m.find()) {
            final String name = m.group(1);
            int start = m.start(1);
            int end = m.end(1);

            out.setSpan(new RoundedEventSpan(start, end, density), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.setSpan(new ForegroundColorSpan(COLOR_EVENT), start, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            out.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    if (listener != null) {
                        listener.onClick(name);
                    }
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(COLOR_EVENT);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }

    private static Pattern buildEventPattern() {
        List<String> names = new ArrayList<>();
        names.add("onCreate");
        names.add("initializeLogic");
        for (AiEventCatalog.Event e : AiEventCatalog.activityEvents()) {
            // "Import" is a normal English word, skip it to avoid false matches.
            if (!"Import".equals(e.name)) {
                names.add(e.name);
            }
        }

        StringBuilder alt = new StringBuilder();
        for (String n : names) {
            if (alt.length() > 0) alt.append('|');
            alt.append(Pattern.quote(n));
        }
        return Pattern.compile("\\b(" + alt + ")\\b");
    }

    /**
     * Draws a thin rounded background behind the event name.
     * If the name wraps to the next line, each line gets its own piece, so it still reads as one.
     */
    static final class RoundedEventSpan implements LineBackgroundSpan {
        private final int spanStart;
        private final int spanEnd;
        private final float padX;
        private final float padY;
        private final float radius;
        private final RectF rect = new RectF();

        RoundedEventSpan(int spanStart, int spanEnd, float density) {
            this.spanStart = spanStart;
            this.spanEnd = spanEnd;
            this.padX = 4 * density;
            this.padY = 1 * density;
            this.radius = 6 * density;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline,
                                   int bottom, CharSequence text, int lineStart, int lineEnd, int lineNum) {
            int s = Math.max(lineStart, spanStart);
            int e = Math.min(lineEnd, spanEnd);
            if (s >= e) return;

            float x1 = left + p.measureText(text, lineStart, s) - padX;
            float x2 = left + p.measureText(text, lineStart, e) + padX;
            rect.set(x1, top + padY, x2, bottom - padY);

            int old = p.getColor();
            p.setColor(COLOR_EVENT_BG);
            c.drawRoundRect(rect, radius, radius, p);
            p.setColor(old);
        }
    }
}