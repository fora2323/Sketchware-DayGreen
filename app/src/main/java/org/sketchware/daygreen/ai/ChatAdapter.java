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
    private static final int TYPE_LOADING = 2;

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

        public static ChatMessage loading() {
            return new ChatMessage(TYPE_LOADING, "");
        }
    }

    private final List<ChatMessage> items = new ArrayList<>();

    public void add(ChatMessage message) {
        items.add(message);
        notifyItemInserted(items.size() - 1);
    }

    public void removeLoading() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (items.get(i).type == TYPE_LOADING) {
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
        if (viewType == TYPE_USER) {
            layout = R.layout.item_chat_user;
        } else if (viewType == TYPE_AGENT) {
            layout = R.layout.item_chat_agent;
        } else {
            layout = R.layout.item_chat_loading;
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder.text != null) {
            holder.text.setText(items.get(position).text);
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