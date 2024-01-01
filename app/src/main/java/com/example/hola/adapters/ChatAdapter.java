package com.example.hola.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.R;
import com.example.hola.activities.ChatActivity;
import com.example.hola.databinding.ItemUserBinding;
import com.example.hola.models.Chat;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<Chat> chats = new ArrayList<>();
    private final Comparator<Chat> compareByUpdatedAt;
    private static DataStoreManager dataStoreManager;

    public ChatAdapter(Context context) {
        dataStoreManager = DataStoreManager.getInstance(context);
        compareByUpdatedAt = Comparator.comparing(Chat::getUpdatedAt).reversed();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChat(Chat chat) {
        this.chats.add(chat);
        this.chats.sort(compareByUpdatedAt);
        notifyDataSetChanged();
    }

    public void updateChat(Chat chat) {
        int prevPosition = - 1;
        for (int i = 0; i < this.chats.size(); i++) {
            if (this.chats.get(i).getId().equals(chat.getId())) {
                prevPosition = i;
                break;
            }
        }
        this.chats.removeIf(aChat -> aChat.getId().equals(chat.getId()));
        this.chats.add(0, chat);
        if (prevPosition == -1) notifyItemInserted(0);
        else notifyItemRangeChanged(0, prevPosition + 1);
    }

    public void setUnreadMessagesToZero(String chatId) {
        int position = 0;
        for (int i = 0; i < this.chats.size(); i++) {
            if (this.chats.get(i).getId().equals(chatId)) {
                position = i;
                break;
            }
        }

        if (this.chats.size() > 0) {
            this.chats.get(position).setUnreadMessages(0);
            notifyItemChanged(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindViews(chats.get(position));
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemUserBinding binding;

        public ViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.textUsername.setVisibility(View.GONE);
            binding.textLatestMessage.setVisibility(View.VISIBLE);
        }

        private void bindViews(Chat chat) {
            if (!chat.getReceiver().getImage().isEmpty()) {
                Picasso.get().load(chat.getReceiver().getImage())
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imageProfile);
            }
            else {
                binding.imageProfile.setImageResource(R.drawable.user);
            }

            binding.textName.setText(chat.getReceiver().getName());
            binding.textLatestMessage.setText(chat.getLatestMessage());
            binding.textUnreadMessages.setText(String.valueOf(chat.getUnreadMessages()));
            if (chat.getUnreadMessages() > 0) binding.textUnreadMessages.setVisibility(View.VISIBLE);
            else binding.textUnreadMessages.setVisibility(View.GONE);

            binding.getRoot().setOnClickListener(v -> {
                Context context = v.getContext();
                dataStoreManager.putValue(Constants.FIELD_ACTIVE_CHAT_ID, chat.getId());
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra(Constants.FIELD_CHAT, chat);
                context.startActivity(intent);
            });
        }
    }
}
