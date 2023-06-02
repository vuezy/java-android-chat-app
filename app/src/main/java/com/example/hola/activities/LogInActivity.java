package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.hola.R;
import com.example.hola.databinding.ActivityLogInBinding;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LogInActivity extends AppCompatActivity {

    private ActivityLogInBinding binding;
    private DataStoreManager dataStoreManager;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLogInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        Boolean isLoggedIn = dataStoreManager.getValue(Constants.FIELD_IS_LOGGED_IN);
        if (isLoggedIn != null && isLoggedIn) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }

        db = FirebaseFirestore.getInstance();
        setListeners();
    }

    private void setListeners() {
        binding.textCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
        });

        binding.btnLogIn.setOnClickListener(v -> logIn());
    }

    private void logIn() {
        String user = binding.editTxtUser.getText().toString();
        String password = binding.editTxtPassword.getText().toString();

        if (user.isEmpty()) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.empty_user),
                    getResources().getString(R.string.empty_user_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else if (password.isEmpty()) {
            Utils.showAlertDialog(
                    this,
                    getResources().getString(R.string.empty_password),
                    getResources().getString(R.string.empty_password_msg),
                    getResources().getString(R.string.ok)
            );
        }
        else {
            AlertDialog progressDialog = Utils.showProgressDialog(
                    this,
                    getLayoutInflater(),
                    getResources().getString(R.string.logging_in)
            );

            db.collection(Constants.COLLECTION_USERS)
                    .where(Filter.and(
                            Filter.or(
                                    Filter.equalTo(Constants.FIELD_USERNAME, user),
                                    Filter.equalTo(Constants.FIELD_EMAIL, user)
                            ),
                            Filter.equalTo(Constants.FIELD_PASSWORD, password)
                    ))
                    .get()
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();

                        if (!task.isSuccessful()) {
                            Log.w("Firestore", "Error getting document", task.getException());
                            Utils.showAlertDialog(
                                    this,
                                    getResources().getString(R.string.error),
                                    getResources().getString(R.string.error_msg),
                                    getResources().getString(R.string.ok)
                            );
                            return;
                        }

                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (queryDocumentSnapshots.isEmpty()) {
                            Utils.showAlertDialog(
                                    this,
                                    getResources().getString(R.string.log_in_failed),
                                    getResources().getString(R.string.log_in_failed_msg),
                                    getResources().getString(R.string.ok)
                            );
                        }
                        else {
                            DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);
                            User loggedInUser = documentSnapshot.toObject(User.class);
                            if (loggedInUser != null) {
                                putUserDataToDataStore(loggedInUser);

                                Intent intent = new Intent(this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                        }
                    });
        }
    }

    private void putUserDataToDataStore(User user) {
        dataStoreManager.putValue(Constants.FIELD_ID, user.getId());
        dataStoreManager.putValue(Constants.FIELD_IMAGE, user.getImage());
        dataStoreManager.putValue(Constants.FIELD_NAME, user.getName());
        dataStoreManager.putValue(Constants.FIELD_USERNAME, user.getUsername());
        dataStoreManager.putValue(Constants.FIELD_EMAIL, user.getEmail());
        dataStoreManager.putValue(Constants.FIELD_PASSWORD, user.getPassword());
        dataStoreManager.putValue(Constants.FIELD_IS_LOGGED_IN, true);
    }
}