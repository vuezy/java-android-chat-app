package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.example.hola.R;
import com.example.hola.adapters.UserAdapter;
import com.example.hola.databinding.ActivitySearchBinding;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;
    private FirebaseFirestore db;
    private DataStoreManager dataStoreManager;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.searchViewUser.requestFocus();
        db = FirebaseFirestore.getInstance();
        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());

        adapter = new UserAdapter();
        binding.recyclerViewUser.setAdapter(adapter);
        binding.recyclerViewUser.setLayoutManager(new LinearLayoutManager(this));
        setListeners();
    }

    private void setListeners() {
        binding.searchViewUser.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            private final Handler handler = new Handler();
            private Runnable runnable;

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                handler.removeCallbacks(runnable);
                showProgressBar();

                runnable = () -> searchUser(newText);
                handler.postDelayed(runnable, 700);

                return true;
            }

            private void searchUser(String newText) {
                if (!newText.isEmpty()) {
                    db.collection(Constants.COLLECTION_USERS)
                            .where(Filter.or(
                                    Filter.equalTo(Constants.FIELD_NAME, newText),
                                    Filter.equalTo(Constants.FIELD_USERNAME, newText)
                            ))
                            .get()
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Log.w("Firestore", "Error getting document", task.getException());
                                    showTextNoUserFound();
                                    Utils.showAlertDialog(
                                            SearchActivity.this,
                                            getResources().getString(R.string.error),
                                            getResources().getString(R.string.error_msg),
                                            getResources().getString(R.string.ok)
                                    );
                                    return;
                                }

                                QuerySnapshot queryDocumentSnapshots = task.getResult();
                                List<User> users = queryDocumentSnapshots.toObjects(User.class);
                                users.removeIf(user -> user.getUsername().equals(dataStoreManager.getValue(Constants.FIELD_USERNAME)));
                                adapter.setUsers(users);

                                if (users.size() == 0) showTextNoUserFound();
                                else showRecyclerView();
                            });
                }
                else {
                    binding.recyclerViewUser.setVisibility(View.GONE);
                    binding.textNoUserFound.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void showProgressBar() {
        binding.recyclerViewUser.setVisibility(View.GONE);
        binding.textNoUserFound.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void showTextNoUserFound() {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewUser.setVisibility(View.GONE);
        binding.textNoUserFound.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        binding.progressBar.setVisibility(View.GONE);
        binding.textNoUserFound.setVisibility(View.GONE);
        binding.recyclerViewUser.setVisibility(View.VISIBLE);
    }
}