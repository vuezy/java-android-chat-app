package com.example.hola.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.hola.R;
import com.example.hola.activities.SearchActivity;
import com.example.hola.adapters.ChatAdapter;
import com.example.hola.adapters.LoadingChatAdapter;
import com.example.hola.databinding.FragmentChatBinding;
import com.example.hola.models.Chat;
import com.example.hola.models.UnreadMessage;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Objects;

public class ChatFragment extends Fragment {

    private FragmentChatBinding binding;
    private FirebaseFirestore db;
    private DataStoreManager dataStoreManager;
    private ListenerRegistration listener;
    private ChatAdapter chatAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataStoreManager = DataStoreManager.getInstance(requireActivity().getApplicationContext());
        db = FirebaseFirestore.getInstance();

        LoadingChatAdapter loadingChatAdapter = new LoadingChatAdapter();
        chatAdapter = new ChatAdapter(requireActivity().getApplicationContext());
        binding.recyclerViewChat.setAdapter(chatAdapter);
        binding.recyclerViewChat.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewLoadingChat.setAdapter(loadingChatAdapter);
        binding.recyclerViewLoadingChat.setLayoutManager(new LinearLayoutManager(getContext()));

        listenToChatsCollection();
        binding.btnSearchUser.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), SearchActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        String prevActiveChat = dataStoreManager.getValue(Constants.FIELD_ACTIVE_CHAT_ID);
        if (prevActiveChat != null) {
            chatAdapter.setUnreadMessagesToZero(prevActiveChat);
            dataStoreManager.remove(Constants.FIELD_ACTIVE_CHAT_ID);
        }
    }

    private void listenToChatsCollection() {
        String senderId = dataStoreManager.getValue(Constants.FIELD_ID);

        listener = db.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains(Constants.FIELD_USERS, senderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Error listening to document", e);
                        showErrorDialog();
                        return;
                    }

                    if (snapshot != null) {
                        if (snapshot.isEmpty()) showHelperMessage();
                        else {
                            if (chatAdapter.getItemCount() == 0) showLoading();
                            for (DocumentChange dc : snapshot.getDocumentChanges()) {
                                Chat chat = dc.getDocument().toObject(Chat.class);
                                if (chat.getLatestMessage().equals("")) {
                                    if (chatAdapter.getItemCount() == 0) showHelperMessage();
                                    return;
                                }
                                String receiverId = chat.getUsers().get(0).equals(senderId) ? chat.getUsers().get(1) : chat.getUsers().get(0);

                                db.collection(Constants.COLLECTION_USERS).document(receiverId).get()
                                        .continueWithTask(task -> {
                                            if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());

                                            chat.setReceiver(task.getResult().toObject(User.class));
                                            return db.collection(Constants.COLLECTION_UNREAD_MESSAGES)
                                                    .document(chat.getId() + senderId)
                                                    .get();
                                        })
                                        .addOnCompleteListener(task -> {
                                            if (!task.isSuccessful()) {
                                                Log.w("Firestore", "Error getting document", task.getException());
                                                showErrorDialog();
                                                return;
                                            }

                                            UnreadMessage unreadMessage = Objects.requireNonNull(task.getResult().toObject(UnreadMessage.class));
                                            chat.setUnreadMessages(unreadMessage.getTotal());

                                            switch (dc.getType()) {
                                                case ADDED:
                                                    chatAdapter.addChat(chat);
                                                    break;
                                                case MODIFIED:
                                                    chatAdapter.updateChat(chat);
                                            }
                                            showRecyclerView();
                                        });
                            }
                        }
                    }
                });
    }

    private void showHelperMessage() {
        binding.recyclerViewLoadingChat.setVisibility(View.GONE);
        binding.recyclerViewChat.setVisibility(View.GONE);
        binding.textNoChat.setVisibility(View.VISIBLE);
        binding.btnSearchUser.setVisibility(View.VISIBLE);
    }

    private void showLoading() {
        binding.textNoChat.setVisibility(View.GONE);
        binding.btnSearchUser.setVisibility(View.GONE);
        binding.recyclerViewChat.setVisibility(View.GONE);
        binding.recyclerViewLoadingChat.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        binding.textNoChat.setVisibility(View.GONE);
        binding.btnSearchUser.setVisibility(View.GONE);
        binding.recyclerViewLoadingChat.setVisibility(View.GONE);
        binding.recyclerViewChat.setVisibility(View.VISIBLE);
    }

    private void showErrorDialog() {
        Utils.showAlertDialog(
                getContext(),
                getResources().getString(R.string.error),
                getResources().getString(R.string.error_msg),
                getResources().getString(R.string.ok)
        );
    }

    @Override
    public void onDestroyView() {
        listener.remove();
        super.onDestroyView();
        binding = null;
    }
}