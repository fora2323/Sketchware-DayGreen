package org.sketchware.daygreen.ai;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pro.sketchware.R;
import pro.sketchware.utility.SketchwareUtil;

public final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AGENT = 1;
    private static final int TYPE_STATUS = 2;
    private static final int TYPE_THINKING = 3;

    private static final Pattern FENCE = Pattern.compile(
            "```([A-Za-z0-9_+#.\\-]*)[ \\t]*\\n([\\s\\S]*?)```");

    public static final class ChatMessage {
        final int type;
        final String text;
        /** Only used by thinking rows: whether the reasoning is open. */
        boolean expanded;

        private ChatMessage(int type, String text) {
            this.type = type;
            this.text = text;
        }

        public static ChatMessage user(String text) {
            return new ChatMessage(TYPE_USER, text);
        }

        public static ChatMessage agent(String text) {
            return new ChatMessage(TYPE_AGENT, text);
        }

        /** Live progress line. Only one exists at a time. */
        public static ChatMessage status(String text) {
            return new ChatMessage(TYPE_STATUS, text);
        }

        /** Reasoning returned by the model. Kept in the chat history, collapsed by default. */
        public static ChatMessage thinking(String text) {
            return new ChatMessage(TYPE_THINKING, text);
        }
    }

    private final List<ChatMessage> items = new ArrayList<>();
    private final ChatTextFormatter formatter;
    private final ChatTextFormatter.OnEventClick onEventClick;

    public ChatAdapter(ChatTextFormatter formatter, ChatTextFormatter.OnEventClick onEventClick) {
        this.formatter = formatter;
        this.onEventClick = onEventClick;
    }

    public void add(ChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    /**
     * Replaces the current status line. The status always sits at the bottom of the chat.
     */
    public void updateStatus(String text) {
        removeStatus();
        add(ChatMessage.status(text));
    }

    public void removeStatus() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).type == TYPE_STATUS) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        switch (viewType) {
            case TYPE_USER:
                layout = R.layout.item_chat_user;
                break;
            case TYPE_STATUS:
                layout = R.layout.item_chat_status;
                break;
            case TYPE_THINKING:
                layout = R.layout.item_chat_thinking;
                break;
            default:
                layout = R.layout.item_chat_agent;
                break;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatMessage item = items.get(position);

        switch (item.type) {
            case TYPE_USER:
                holder.text.setText(item.text);
                break;
            case TYPE_AGENT:
                bindAgent(holder, item);
                break;
            case TYPE_STATUS:
                holder.text.setText(item.text);
                break;
            case TYPE_THINKING:
                bindThinking(holder, item);
                break;
            default:
                break;
        }
    }

    /**
     * Agent reply: plain text becomes one text block, each ```fenced``` part becomes a code block.
     */
    private void bindAgent(Holder holder, ChatMessage item) {
        LinearLayout parts = holder.parts;
        parts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(parts.getContext());

        for (Segment seg : splitSegments(item.text)) {
            if (seg.isCode) {
                parts.addView(buildCodePart(inflater, parts, seg.lang, seg.text));
            } else {
                String plain = seg.text.trim();
                if (!plain.isEmpty()) {
                    parts.addView(buildTextPart(inflater, parts, plain));
                }
            }
        }
    }

    private View buildTextPart(LayoutInflater inflater, ViewGroup parent, String text) {
        View view = inflater.inflate(R.layout.item_chat_text, parent, false);
        TextView tv = view.findViewById(R.id.tv_message);
        tv.setText(formatter.format(text, onEventClick));
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        return view;
    }

    private View buildCodePart(LayoutInflater inflater, ViewGroup parent, String lang, String code) {
        View view = inflater.inflate(R.layout.item_chat_code, parent, false);

        TextView langTv = view.findViewById(R.id.tv_lang);
        langTv.setText(lang == null || lang.isEmpty() ? "code" : lang);

        TextView codeTv = view.findViewById(R.id.tv_code);
        codeTv.setText(ChatCodeHighlighter.highlight(code));

        ImageButton copy = view.findViewById(R.id.btn_copy);
        copy.setOnClickListener(v -> {
            Context ctx = v.getContext();
            ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("code", code));
                SketchwareUtil.toast("Kode disalin");
            }
        });
        return view;
    }

    private void bindThinking(Holder holder, ChatMessage item) {
        holder.title.setText((item.expanded ? "▾ " : "▸ ") + "Proses berpikir");
        holder.body.setText(item.text);
        holder.body.setVisibility(item.expanded ? View.VISIBLE : View.GONE);

        holder.title.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            item.expanded = !item.expanded;
            notifyItemChanged(pos);
        });
    }

    /** Splits the reply into plain-text parts and fenced code parts, in order. */
    static List<Segment> splitSegments(String text) {
        List<Segment> out = new ArrayList<>();
        Matcher m = FENCE.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                out.add(Segment.plain(text.substring(last, m.start())));
            }
            String code = m.group(2).replaceAll("\\n+$", "");
            out.add(Segment.code(m.group(1), code));
            last = m.end();
        }
        if (last < text.length()) {
            out.add(Segment.plain(text.substring(last)));
        }
        return out;
    }

    static final class Segment {
        final boolean isCode;
        final String lang;
        final String text;

        private Segment(boolean isCode, String lang, String text) {
            this.isCode = isCode;
            this.lang = lang;
            this.text = text;
        }

        static Segment plain(String text) {
            return new Segment(false, "", text);
        }

        static Segment code(String lang, String text) {
            return new Segment(true, lang, text);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        /** User and status rows. */
        final TextView text;
        /** Agent rows: holds text and code parts. */
        final LinearLayout parts;
        /** Thinking rows only. */
        final TextView title;
        final TextView body;

        Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_message);
            parts = itemView.findViewById(R.id.ll_parts);
            title = itemView.findViewById(R.id.tv_thinking_title);
            body = itemView.findViewById(R.id.tv_thinking_body);

            if (text != null) {
                text.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}