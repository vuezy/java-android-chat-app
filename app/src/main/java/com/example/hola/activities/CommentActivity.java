package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.example.hola.R;
import com.example.hola.adapters.CommentAdapter;
import com.example.hola.databinding.ActivityCommentBinding;
import com.example.hola.models.Comment;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CommentActivity extends AppCompatActivity {

    private ActivityCommentBinding binding;
    private DataStoreManager dataStoreManager;
    private FirebaseFirestore db;
    private CommentAdapter adapter;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            postId = extras.getString(Constants.FIELD_POST_ID);
        }
        binding = ActivityCommentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        db = FirebaseFirestore.getInstance();
        adapter = new CommentAdapter(dataStoreManager.getValue(Constants.FIELD_ID));
        binding.recyclerViewComment.setAdapter(adapter);
        binding.recyclerViewComment.setLayoutManager(new LinearLayoutManager(this));

        checkPostId();
    }

    private void checkPostId() {
        AlertDialog progressDialog = Utils.showProgressDialog(
                this,
                getLayoutInflater(),
                getResources().getString(R.string.loading_comments)
        );

        db.collection(Constants.COLLECTION_POSTS).document(postId).get().addOnCompleteListener(task -> {
            progressDialog.dismiss();

            if (!task.isSuccessful() || !task.getResult().exists()) {
                if (task.getException() != null)
                    Log.w("Firestore", "Error getting document", task.getException());

                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(Constants.FIELD_TAB_INDEX, 1);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return;
            }

            getComments();
            setListeners();
        });
    }

    private void getComments() {
        if (adapter.getItemCount() == 0) showLoading();
        List<Comment> comments = new ArrayList<>();

        db.collection(Constants.COLLECTION_COMMENTS).whereEqualTo(Constants.FIELD_POST_ID, postId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("Firestore", "Error getting document", task.getException());
                        showErrorDialog();
                        return;
                    }

                    QuerySnapshot queryDocumentSnapshots = task.getResult();
                    if (queryDocumentSnapshots.isEmpty()) {
                        showHelperMessage();
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Comment comment = Objects.requireNonNull(doc.toObject(Comment.class));
                            db.collection(Constants.COLLECTION_USERS).document(comment.getSender()).get()
                                    .addOnCompleteListener(aTask -> {
                                        if (!aTask.isSuccessful()) {
                                            Log.w("Firestore", "Error getting document", task.getException());
                                            showErrorDialog();
                                            return;
                                        }

                                        comment.setUser(aTask.getResult().toObject(User.class));
                                        comments.add(comment);

                                        if (comments.size() == queryDocumentSnapshots.getDocuments().size()) {
                                            adapter.setComments(comments);
                                            showRecyclerView();
                                            binding.swipeRefreshLayout.setRefreshing(false);
                                        }
                                    });
                        }
                    }
                });
    }

    private void setListeners() {
        binding.imageClose.setOnClickListener(v -> finish());

        binding.imageSend.setOnClickListener(v -> {
            EditText editTxtComment = binding.editTxtComment;
            String commentText = editTxtComment.getText().toString().trim();
            if (!editTxtComment.isEnabled() || commentText.isEmpty()) return;

            editTxtComment.setEnabled(false);
            editTxtComment.setText("");
            showLoading();

            WriteBatch batch = db.batch();
            DocumentReference commentDocRef = db.collection(Constants.COLLECTION_COMMENTS).document();
            DocumentReference postDocRef = db.collection(Constants.COLLECTION_POSTS).document(postId);

            Comment comment = new Comment(
                    postId,
                    dataStoreManager.getValue(Constants.FIELD_ID),
                    commentText,
                    new ArrayList<>(),
                    new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, new Locale("in", "ID")).format(new Date())
            );

            batch.set(commentDocRef, comment);
            batch.update(postDocRef, Constants.FIELD_COMMENTS, FieldValue.increment(1));
            batch.commit().addOnCompleteListener(task -> {
                editTxtComment.setEnabled(true);

                if (!task.isSuccessful()) {
                    Log.w("Firestore", "Error adding document", task.getException());
                    showErrorDialog();
                    editTxtComment.setText(commentText);
                    return;
                }

                getComments();
            });
        });

        binding.swipeRefreshLayout.setOnRefreshListener(this::getComments);
    }

    private void showLoading() {
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void showHelperMessage() {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewComment.setVisibility(View.GONE);
        binding.textNoComment.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        binding.progressBar.setVisibility(View.GONE);
        binding.textNoComment.setVisibility(View.GONE);
        binding.recyclerViewComment.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showErrorDialog() {
        Utils.showAlertDialog(
                this,
                getResources().getString(R.string.error),
                getResources().getString(R.string.error_msg),
                getResources().getString(R.string.ok)
        );
    }
}