package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;

import com.example.hola.R;
import com.example.hola.databinding.ActivitySignUpBinding;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private DataStoreManager dataStoreManager;
    private FirebaseFirestore db;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        db = FirebaseFirestore.getInstance();
        setListeners();
    }

    private void setListeners() {
        binding.textLogIn.setOnClickListener(v -> finish());
        binding.btnSignUp.setOnClickListener(v -> signUp());
    }

    private void signUp() {
        String name = binding.editTxtName.getText().toString();
        String username = binding.editTxtUsername.getText().toString();
        String email = binding.editTxtEmail.getText().toString();
        String password = binding.editTxtPassword.getText().toString();

        if (name.isEmpty() || name.length() > 30 || !name.matches("[a-zA-Z ]+")) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.invalid_name),
                    getResources().getString(R.string.invalid_name_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else if (username.length() < 3 || name.length() > 15 || !username.matches("[a-z0-9]+")) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.invalid_username),
                    getResources().getString(R.string.invalid_username_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.invalid_email),
                    getResources().getString(R.string.invalid_email_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else if (password.length() < 8 || password.length() > 32) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.invalid_password),
                    getResources().getString(R.string.invalid_password_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else {
            progressDialog = Utils.showProgressDialog(
                    this,
                    getLayoutInflater(),
                    getResources().getString(R.string.creating_account)
            );
            User user = new User("", name, username, email, password);

            db.collection(Constants.COLLECTION_USERS)
                    .where(Filter.or(
                            Filter.equalTo(Constants.FIELD_USERNAME, username),
                            Filter.equalTo(Constants.FIELD_EMAIL, email)
                    ))
                    .get()
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            Exception e = Objects.requireNonNull(task.getException());
                            Log.w("Firestore", "Error getting document", e);
                            throw e;
                        }

                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (queryDocumentSnapshots.isEmpty()) {
                            return db.collection(Constants.COLLECTION_USERS).add(user);
                        }
                        else {
                            throw new UserAlreadyExistsException();
                        }
                    })
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();

                        if (!task.isSuccessful()) {
                            Exception e = task.getException();
                            if (e instanceof UserAlreadyExistsException) {
                                Utils.showAlertDialog(
                                        this,
                                        getResources().getString(R.string.invalid_username_or_email),
                                        getResources().getString(R.string.invalid_username_or_email_msg),
                                        getResources().getString(R.string.ok)
                                );
                            }
                            else {
                                Log.w("Firestore", "Error adding document", task.getException());
                                Utils.showAlertDialog(
                                        this,
                                        getResources().getString(R.string.error),
                                        getResources().getString(R.string.error_msg),
                                        getResources().getString(R.string.ok)
                                );
                            }
                            return;
                        }

                        user.setId(task.getResult().getId());
                        putUserDataToDataStore(user);

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
        }
    }

    private void putUserDataToDataStore(User user) {
        dataStoreManager.putValue(Constants.FIELD_ID, user.getId());
        dataStoreManager.putValue(Constants.FIELD_IMAGE, "");
        dataStoreManager.putValue(Constants.FIELD_NAME, user.getName());
        dataStoreManager.putValue(Constants.FIELD_USERNAME, user.getUsername());
        dataStoreManager.putValue(Constants.FIELD_EMAIL, user.getEmail());
        dataStoreManager.putValue(Constants.FIELD_PASSWORD, user.getPassword());
        dataStoreManager.putValue(Constants.FIELD_IS_LOGGED_IN, true);
    }

    private static class UserAlreadyExistsException extends Exception {}
}