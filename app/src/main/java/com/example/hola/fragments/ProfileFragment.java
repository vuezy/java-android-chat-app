package com.example.hola.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.hola.R;
import com.example.hola.activities.AddPostActivity;
import com.example.hola.activities.ChatActivity;
import com.example.hola.databinding.FragmentProfileBinding;
import com.example.hola.models.Chat;
import com.example.hola.models.UnreadMessage;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private DataStoreManager dataStoreManager;
    private User user;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private AlertDialog progressDialog;

    private final ActivityResultLauncher<Intent> chooseImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        Uri uri = result.getData().getData();
                        uploadImage(uri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataStoreManager = DataStoreManager.getInstance(requireActivity().getApplicationContext());
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        if (savedInstanceState == null) {
            Bundle arguments = getArguments();
            if (arguments != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    user = arguments.getSerializable(Constants.KEY_USER, User.class);
                }
                else {
                    user = (User) arguments.getSerializable(Constants.KEY_USER);
                }
            }

            Bundle bundle = new Bundle();
            if (user == null) {
                User currentUser = new User(
                        dataStoreManager.getValue(Constants.FIELD_IMAGE),
                        dataStoreManager.getValue(Constants.FIELD_NAME),
                        dataStoreManager.getValue(Constants.FIELD_USERNAME),
                        dataStoreManager.getValue(Constants.FIELD_EMAIL),
                        dataStoreManager.getValue(Constants.FIELD_PASSWORD)
                );
                currentUser.setId(dataStoreManager.getValue(Constants.FIELD_ID));
                bundle.putSerializable(Constants.KEY_USER, currentUser);
            }
            else {
                bundle.putSerializable(Constants.KEY_USER, user);
            }

            getChildFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentPost, PostFragment.class, bundle)
                    .commit();
        }

        if (user == null) setMyProfile();
        else setUserProfile();
    }

    private void setMyProfile() {
        String image = dataStoreManager.getValue(Constants.FIELD_IMAGE);
        String name = dataStoreManager.getValue(Constants.FIELD_NAME);
        String username = "@" + dataStoreManager.getValue(Constants.FIELD_USERNAME);
        String email = dataStoreManager.getValue(Constants.FIELD_EMAIL);

        if (!image.isEmpty()) {
            Picasso.get().load(image)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imageProfile);
        }
        binding.textName.setText(name);
        enableHorizontalScroll(binding.textName);
        binding.textUsername.setText(username);
        enableHorizontalScroll(binding.textUsername);
        binding.textEmail.setText(email);
        enableHorizontalScroll(binding.textEmail);

        binding.textChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            chooseImage.launch(intent);
        });

        binding.textAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddPostActivity.class);
            startActivity(intent);
        });
    }

    private void setUserProfile() {
        String image = user.getImage();
        String name = user.getName();
        String username = "@" + user.getUsername();

        if (!image.isEmpty()) {
            Picasso.get().load(image)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(binding.imageProfile);
        }
        binding.textName.setText(name);
        enableHorizontalScroll(binding.textName);
        binding.textUsername.setText(username);
        enableHorizontalScroll(binding.textUsername);

        binding.textEmail.setVisibility(View.GONE);
        binding.textChangeImage.setVisibility(View.GONE);
        binding.textAddPost.setVisibility(View.GONE);
        binding.btnChat.setVisibility(View.VISIBLE);

        binding.btnChat.setOnClickListener(v -> startChat());
    }

    @SuppressLint("ClickableViewAccessibility")
    private void enableHorizontalScroll(TextView textView) {
        textView.setOnTouchListener((v, event) -> {
            if (v.getParent() != null) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.setSelected(true);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP:
                        v.setSelected(false);
                }
                v.getParent().requestDisallowInterceptTouchEvent(true);
            }
            return false;
        });
        textView.setHorizontallyScrolling(true);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }

    private void uploadImage(Uri uri) {
        progressDialog = Utils.showProgressDialog(
                getContext(),
                getLayoutInflater(),
                getResources().getString(R.string.uploading_image)
        );

        String fileName = new SimpleDateFormat(Constants.FORMAT_FILE_NAME, Locale.US).format(new Date());
        StorageReference storageRef = storage.getReference(
                Constants.STORAGE_BASE_PATH + fileName + ".jpg"
        );

        storageRef.putFile(uri)
            .continueWithTask(task  -> {
                if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                return storageRef.getDownloadUrl();
            })
            .continueWithTask(task -> {
                if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());

                Uri downloadUri = task.getResult();
                dataStoreManager.putValue(Constants.FIELD_IMAGE, downloadUri.toString());
                return db.collection(Constants.COLLECTION_USERS)
                        .document(dataStoreManager.getValue(Constants.FIELD_ID))
                        .update(Constants.FIELD_IMAGE, downloadUri.toString());
            })
            .addOnCompleteListener(task -> {
                progressDialog.dismiss();

                if (!task.isSuccessful()) {
                    Log.w("Cloud Storage", "Error uploading image", task.getException());
                    showErrorDialog();
                    return;
                }
                binding.imageProfile.setImageURI(uri);
            });
    }

    private void startChat() {
        progressDialog = Utils.showProgressDialog(
                getContext(),
                getLayoutInflater(),
                getResources().getString(R.string.preparing_chat)
        );

        db.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains(Constants.FIELD_USERS, dataStoreManager.getValue(Constants.FIELD_ID))
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("Firestore", "Error getting document", task.getException());
                        showErrorDialog();
                        return;
                    }

                    QuerySnapshot queryDocumentSnapshots = task.getResult();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Chat> chats = queryDocumentSnapshots.toObjects(Chat.class);
                        Chat chat = chats.stream()
                                .filter(aChat -> aChat.getUsers().contains(user.getId()))
                                .findFirst()
                                .orElse(null);

                        if (chat != null) prepareExistingChat(chat);
                        else createChat();
                    }
                    else createChat();
                });
    }

    private void prepareExistingChat(Chat chat) {
        db.collection(Constants.COLLECTION_UNREAD_MESSAGES)
                .document(chat.getId() + dataStoreManager.getValue(Constants.FIELD_ID))
                .get()
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    if (!task.isSuccessful()) {
                        Log.w("Firestore", "Error getting document", task.getException());
                        showErrorDialog();
                        return;
                    }

                    UnreadMessage unreadMessage = Objects.requireNonNull(task.getResult().toObject(UnreadMessage.class));
                    chat.setReceiver(user);
                    chat.setUnreadMessages(unreadMessage.getTotal());
                    dataStoreManager.putValue(Constants.FIELD_ACTIVE_CHAT_ID, chat.getId());

                    Intent intent = new Intent(getContext(), ChatActivity.class);
                    intent.putExtra(Constants.FIELD_CHAT, chat);
                    startActivity(intent);
                });
    }

    private void createChat() {
        WriteBatch batch = db.batch();
        DocumentReference chatDocRef = db.collection(Constants.COLLECTION_CHATS).document();
        String chatId = chatDocRef.getId();
        String user1 = dataStoreManager.getValue(Constants.FIELD_ID);
        String user2 = user.getId();

        DocumentReference unreadMessageDocRef1 = db.collection(Constants.COLLECTION_UNREAD_MESSAGES).document(chatId + user1);
        DocumentReference unreadMessageDocRef2 = db.collection(Constants.COLLECTION_UNREAD_MESSAGES).document(chatId + user2);

        List<String> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        Chat chat = new Chat(
                users,
                "",
                new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, new Locale("in", "ID")).format(new Date())
        );
        UnreadMessage unreadMessage1 = new UnreadMessage(chatId, user1, 0);
        UnreadMessage unreadMessage2 = new UnreadMessage(chatId, user2, 0);

        batch.set(chatDocRef, chat);
        batch.set(unreadMessageDocRef1, unreadMessage1);
        batch.set(unreadMessageDocRef2, unreadMessage2);
        batch.commit().addOnCompleteListener(task -> {
            progressDialog.dismiss();

            if (!task.isSuccessful()) {
                Log.w("Firestore", "Error adding document", task.getException());
                showErrorDialog();
                return;
            }

            chat.setId(chatId);
            chat.setReceiver(user);
            dataStoreManager.putValue(Constants.FIELD_ACTIVE_CHAT_ID, chatId);

            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra(Constants.FIELD_CHAT, chat);
            startActivity(intent);
        });
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
        super.onDestroyView();
        binding = null;
    }
}