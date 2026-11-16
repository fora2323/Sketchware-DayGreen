package org.sketchware.daygreen.ai;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.Markwon;

/**
 * Renders agent text as Markdown (bold, code, code blocks, lists),
 * and colors Android lifecycle/event names blue. Tapping one calls the listener.
 */
public final class ChatTextFormatter {

    public interface OnEventClick {
        void onClick(String eventName);
    }

    private static final int COLOR_EVENT = 0xFF4FA3FF;

    private static final Pattern EVENT_PATTERN = buildEventPattern();

    private final Markwon markwon;

    public ChatTextFormatter(Context context) {
        this.markwon = Markwon.create(context);
    }

    public Spannable format(String text, OnEventClick listener) {
        Spannable out = new SpannableStringBuilder(markwon.toMarkdown(text));

        Matcher m = EVENT_PATTERN.matcher(out);
        while (m.find()) {
            final String name = m.group(1);
            int start = m.start(1);
            int end = m.end(1);

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
}