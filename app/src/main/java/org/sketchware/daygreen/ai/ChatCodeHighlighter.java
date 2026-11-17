package org.sketchware.daygreen.ai;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sketchware.daygreen.ai.AiEventCatalog;

/**
 * Simple syntax highlight for code blocks in chat.
 * Colors comments, strings, numbers, keywords, and event names.
 */
public final class ChatCodeHighlighter {

    private static final int COLOR_COMMENT = 0xFF7F8C8D;
    private static final int COLOR_STRING = 0xFF9ECE6A;
    private static final int COLOR_NUMBER = 0xFFE5C07B;
    private static final int COLOR_KEYWORD = 0xFFC586C0;
    private static final int COLOR_EVENT = 0xFF4FA3FF;

    private static final String KEYWORDS =
            "abstract|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum"
                    + "|extends|final|finally|float|for|fun|if|implements|import|instanceof|int|interface|long"
                    + "|new|null|override|package|private|protected|public|return|short|static|super|switch"
                    + "|this|throw|throws|true|false|try|val|var|void|volatile|while|String";

    private static final Pattern TOKEN = Pattern.compile(
            "(//[^\\n]*|/\\*[\\s\\S]*?\\*/)"                              // 1: comment
                    + "|(\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*')"  // 2: string
                    + "|(\\b\\d+(?:\\.\\d+)?[fFlL]?\\b)"                    // 3: number
                    + "|(\\b(?:" + KEYWORDS + ")\\b)"                       // 4: keyword
                    + "|(\\b(?:" + eventNames() + ")\\b)");                 // 5: event

    private ChatCodeHighlighter() {
    }

    public static SpannableStringBuilder highlight(String code) {
        SpannableStringBuilder out = new SpannableStringBuilder(code);
        Matcher m = TOKEN.matcher(code);
        while (m.find()) {
            int color;
            if (m.group(1) != null) {
                color = COLOR_COMMENT;
            } else if (m.group(2) != null) {
                color = COLOR_STRING;
            } else if (m.group(3) != null) {
                color = COLOR_NUMBER;
            } else if (m.group(4) != null) {
                color = COLOR_KEYWORD;
            } else {
                color = COLOR_EVENT;
            }
            out.setSpan(new ForegroundColorSpan(color), m.start(), m.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return out;
    }

    private static String eventNames() {
        List<String> names = new ArrayList<>();
        names.add("onCreate");
        names.add("initializeLogic");
        for (AiEventCatalog.Event e : AiEventCatalog.activityEvents()) {
            if (!"Import".equals(e.name)) {
                names.add(e.name);
            }
        }
        return String.join("|", names);
    }
}