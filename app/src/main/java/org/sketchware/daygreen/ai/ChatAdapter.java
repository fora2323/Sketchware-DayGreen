package org.sketchware.daygreen.ai;

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

        /** Reasoning returned by the model. Kept in the chat history. */
        public static ChatMessage thinking(String text) {
            return new ChatMessage(TYPE_THINKING, text);
        }
    }

    private final List<ChatMessage> items = new ArrayList<>();

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
        int layout = viewType == TYPE_USER ? R.layout.item_chat_user : R.layout.item_chat_agent;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatMessage item = items.get(position);
        if (holder.text == null) {
            return;
        }
        if (item.type == TYPE_THINKING) {
            holder.text.setText("Thinking: " + item.text);
        } else {
            holder.text.setText(item.text);
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView text;

        Holder(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.tv_message);
        }
    }
}