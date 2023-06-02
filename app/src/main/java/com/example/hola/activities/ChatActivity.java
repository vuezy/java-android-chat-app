package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import com.example.hola.R;
import com.example.hola.adapters.MessageAdapter;
import com.example.hola.databinding.ActivityChatBinding;
import com.example.hola.models.Chat;
import com.example.hola.models.Message;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public ActivityChatBinding binding;
    private FirebaseFirestore db;
    private DataStoreManager dataStoreManager;
    private ListenerRegistration listener;
    private MessageAdapter adapter;
    private Chat chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                chat = extras.getSerializable(Constants.FIELD_CHAT, Chat.class);
            }
            else {
                chat = (Chat) extras.getSerializable(Constants.FIELD_CHAT);
            }
        }
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        db = FirebaseFirestore.getInstance();
        adapter = new MessageAdapter(dataStoreManager.getValue(Constants.FIELD_ID));
        binding.recyclerViewMessage.setAdapter(adapter);
        binding.recyclerViewMessage.setLayoutManager(new LinearLayoutManager(this));

        listenToMessagesCollection();
        setViews();
        setListeners();
    }

    private void listenToMessagesCollection() {
        listener = db.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo(Constants.FIELD_CHAT_ID, chat.getId())
                .orderBy(Constants.FIELD_DATE_TIME)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("Firestore", "Error listening to document", e);
                        showErrorDialog();
                        return;
                    }

                    if (snapshot != null) {
                        List<Message> messages = snapshot.toObjects(Message.class);

                        if (chat.getUnreadMessages() == 0 || (chat.getUnreadMessages() > 0 && !snapshot.getMetadata().isFromCache())) {
                            int position = Math.max(0, Math.min(messages.size() - 1, messages.size() - chat.getUnreadMessages()));
                            adapter.setMessages(messages);
                            adapter.setNewMessagesLabelPosition(chat.getUnreadMessages() > 0 ? position : -1);
                            binding.recyclerViewMessage.smoothScrollToPosition(position);
                            chat.setUnreadMessages(0);
                        }

                        db.collection(Constants.COLLECTION_UNREAD_MESSAGES)
                                .document(chat.getId() + dataStoreManager.getValue(Constants.FIELD_ID))
                                .update(Constants.FIELD_TOTAL, 0)
                                .addOnCompleteListener(task -> {
                                    if (!task.isSuccessful()) {
                                        Log.w("Firestore", "Error updating document", task.getException());
                                        showErrorDialog();
                                    }
                                });
                    }
                });
    }

    private void setViews() {
        String image = chat.getReceiver().getImage();
        String name = chat.getReceiver().getName();
        String username = "@" + chat.getReceiver().getUsername();

        if (!image.isEmpty()) {
            Picasso.get().load(image)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imageProfile);
        }
        binding.textName.setText(name);
        binding.textUsername.setText(username);
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> backToMain());
        binding.viewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra(Constants.KEY_USER, chat.getReceiver());
            startActivity(intent);
        });
        binding.editTxtMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) sendMessage();
            return true;
        });
        binding.imageSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        EditText editTxtMessage = binding.editTxtMessage;
        String messageText = editTxtMessage.getText().toString().trim();
        if (!editTxtMessage.isEnabled() || messageText.isEmpty()) return;

        editTxtMessage.setEnabled(false);
        editTxtMessage.setText("");

        WriteBatch batch = db.batch();
        DocumentReference messageDocRef = db.collection(Constants.COLLECTION_MESSAGES).document();
        DocumentReference chatDocRef = db.collection(Constants.COLLECTION_CHATS).document(chat.getId());
        DocumentReference unreadMessageDocRef = db.collection(Constants.COLLECTION_UNREAD_MESSAGES)
                .document(chat.getId() + chat.getReceiver().getId());
        String date = new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, new Locale("in", "ID")).format(new Date());

        Message message = new Message(
                chat.getId(),
                dataStoreManager.getValue(Constants.FIELD_ID),
                messageText,
                date
        );
        batch.set(messageDocRef, message);

        Map<String, Object> updates = new HashMap<>();
        updates.put(Constants.FIELD_LATEST_MESSAGE, messageText);
        updates.put(Constants.FIELD_UPDATED_AT, date);
        batch.update(chatDocRef, updates);
        batch.update(unreadMessageDocRef, Constants.FIELD_TOTAL, FieldValue.increment(1));

        batch.commit().addOnCompleteListener(task -> {
            editTxtMessage.setEnabled(true);

            if (!task.isSuccessful()) {
                Log.w("Firestore", "Error adding document", task.getException());
                showErrorDialog();
                editTxtMessage.setText(messageText);
            }
        });
    }

    private void showErrorDialog() {
        Utils.showAlertDialog(
                this,
                getResources().getString(R.string.error),
                getResources().getString(R.string.error_msg),
                getResources().getString(R.string.ok)
        );
    }

    @Override
    public void onBackPressed() {
        backToMain();
    }

    private void backToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.FIELD_TAB_INDEX, 0);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        listener.remove();
        super.onDestroy();
    }
}