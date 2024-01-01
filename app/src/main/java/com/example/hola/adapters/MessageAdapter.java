package com.example.hola.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.databinding.ItemReceivedMessageBinding;
import com.example.hola.databinding.ItemSentMessageBinding;
import com.example.hola.models.Message;
import com.example.hola.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int RECEIVED_MESSAGE = 0;
    private static final int SENT_MESSAGE = 1;

    private List<Message> messages;
    private final String sender;
    private int newMessagesLabelPosition = -1;

    public MessageAdapter(String sender) {
        this.sender = sender;
        this.messages = new ArrayList<>();
    }

    public void setMessages(List<Message> messages) {
        int positionStart = this.messages.size();
        this.messages = messages;
        notifyItemRangeInserted(positionStart, messages.size() - positionStart);
    }

    public void setNewMessagesLabelPosition(int position) {
        if (position <= 0) {
            int prevPosition = newMessagesLabelPosition;
            newMessagesLabelPosition = -1;
            notifyItemChanged(prevPosition);
        }
        else {
            newMessagesLabelPosition = position;
            notifyItemChanged(newMessagesLabelPosition);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSender().equals(sender) ? SENT_MESSAGE : RECEIVED_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case SENT_MESSAGE:
                ItemSentMessageBinding sentMessageBinding = ItemSentMessageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
                return new SentMessageViewHolder(sentMessageBinding);

            case RECEIVED_MESSAGE:
            default:
                ItemReceivedMessageBinding receivedMessageBinding = ItemReceivedMessageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                );
                return new ReceivedMessageViewHolder(receivedMessageBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        switch (viewType) {
            case SENT_MESSAGE:
                SentMessageViewHolder sentMessageViewHolder = (SentMessageViewHolder) holder;
                sentMessageViewHolder.bindViews(messages.get(position));
                break;

            case RECEIVED_MESSAGE:
            default:
                ReceivedMessageViewHolder receivedMessageViewHolder = (ReceivedMessageViewHolder) holder;
                receivedMessageViewHolder.bindViews(messages.get(position), position == newMessagesLabelPosition);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private static String formatMessageDate(String date) {
        String messageDate = "";

        try {
            Locale locale = new Locale("in", "ID");
            messageDate = new SimpleDateFormat(Constants.FORMAT_DATE_TIME, locale)
                    .format(
                            Objects.requireNonNull(new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, locale).parse(date))
                    );
        }
        catch (Exception e) {
            Log.w("SimpleDateFormat", e);
        }

        return messageDate;
    }

    private static class SentMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemSentMessageBinding binding;

        public SentMessageViewHolder(@NonNull ItemSentMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bindViews(Message message) {
            String messageDate = formatMessageDate(message.getDateTime());
            binding.textMessage.setText(message.getContent());
            binding.textDate.setText(messageDate);
        }
    }

    private static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(@NonNull ItemReceivedMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bindViews(Message message, Boolean showNewMessagesLabel) {
            String messageDate = formatMessageDate(message.getDateTime());
            binding.textMessage.setText(message.getContent());
            binding.textDate.setText(messageDate);
            binding.textNewMessages.setVisibility(showNewMessagesLabel ? View.VISIBLE : View.GONE);
        }
    }

}
