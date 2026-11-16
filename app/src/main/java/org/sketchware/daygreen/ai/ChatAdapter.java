package org.sketchware.daygreen.ai;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import pro.sketchware.R;

public final class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AGENT = 1;
    private static final int TYPE_STATUS = 2;
    private static final int TYPE_THINKING = 3;

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
                holder.text.setText(formatter.format(item.text, onEventClick));
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

    static final class Holder extends RecyclerView.ViewHolder {
        /** Message text for user, agent and status rows. */
        final TextView text;
        /** Thinking rows only. */
        final TextView title;
        final TextView body;

        Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_message);
            title = itemView.findViewById(R.id.tv_thinking_title);
            body = itemView.findViewById(R.id.tv_thinking_body);

            TextView link = text != null ? text : (TextView) itemView.findViewById(R.id.tv_status);
            if (link == null) {
                link = body;
            }
            if (link != null) {
                link.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }
}