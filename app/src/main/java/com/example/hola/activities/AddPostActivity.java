package com.example.hola.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.example.hola.R;
import com.example.hola.databinding.ActivityAddPostBinding;
import com.example.hola.models.Post;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class AddPostActivity extends AppCompatActivity {

    private ActivityAddPostBinding binding;
    private DataStoreManager dataStoreManager;
    private FirebaseStorage storage;
    private FirebaseFirestore db;
    private Uri imageUri;

    private final ActivityResultLauncher<Intent> chooseImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                    if (result.getData() != null) {
                        imageUri = result.getData().getData();

                        binding.imageContent.setVisibility(View.VISIBLE);
                        binding.imageContent.setImageURI(imageUri);
                        binding.textAddImage.setText(getResources().getString(R.string.change_image));
                        binding.textRemoveImage.setVisibility(View.VISIBLE);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        storage = FirebaseStorage.getInstance();
        db = FirebaseFirestore.getInstance();

        setListeners();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> finish());
        binding.textAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            chooseImage.launch(intent);
        });
        binding.textRemoveImage.setOnClickListener(v -> {
            binding.imageContent.setVisibility(View.GONE);
            binding.imageContent.setImageResource(R.drawable.post_image);
            imageUri = null;

            binding.textAddImage.setText(getResources().getString(R.string.add_image));
            v.setVisibility(View.GONE);
        });
        binding.btnAddPost.setOnClickListener(v -> addPost());
    }

    private void addPost() {
        AlertDialog progressDialog = Utils.showProgressDialog(
                this,
                getLayoutInflater(),
                getResources().getString(R.string.posting)
        );

        String content = binding.editTxtContent.getText().toString().trim();
        if (content.isEmpty() && imageUri == null || content.length() > 1000) {
            progressDialog.dismiss();
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.empty_post),
                    getResources().getString(R.string.empty_post_msg),
                    getResources().getString(R.string.ok)
            );
            return;
        }

        String date = new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, new Locale("in", "ID")).format(new Date());

        if (imageUri == null) {
            Post post = new Post(
                    dataStoreManager.getValue(Constants.FIELD_ID),
                    content,
                    binding.editTxtContent.getLineCount() > 6,
                    "",
                    new ArrayList<>(),
                    0,
                    date
            );

            db.collection(Constants.COLLECTION_POSTS).add(post).addOnCompleteListener(task -> {
                progressDialog.dismiss();

                if (!task.isSuccessful()) {
                    Log.w("Firestore", "Error adding document", task.getException());
                    showErrorDialog();
                    return;
                }

                backToMain();
            });
        }
        else {
            String fileName = "post" + new SimpleDateFormat(Constants.FORMAT_FILE_NAME, Locale.US).format(new Date());
            StorageReference storageRef = storage.getReference(
                    Constants.STORAGE_BASE_PATH + fileName + ".jpg"
            );

            storageRef.putFile(imageUri)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                        return storageRef.getDownloadUrl();
                    })
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());

                        Post post = new Post(
                                dataStoreManager.getValue(Constants.FIELD_ID),
                                content,
                                binding.editTxtContent.getLineCount() > 6,
                                task.getResult().toString(),
                                new ArrayList<>(),
                                0,
                                date
                        );
                        return db.collection(Constants.COLLECTION_POSTS).add(post);
                    })
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();

                        if (!task.isSuccessful()) {
                            Log.w("Firestore", "Error adding document", task.getException());
                            showErrorDialog();
                            return;
                        }

                        backToMain();
                    });
        }
    }

    private void backToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.FIELD_TAB_INDEX, 2);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
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