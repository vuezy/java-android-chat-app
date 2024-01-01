package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;

import com.example.hola.R;
import com.example.hola.databinding.ActivityProfileBinding;
import com.example.hola.fragments.ProfileFragment;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            User user = null;
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    user = extras.getSerializable(Constants.KEY_USER, User.class);
                }
                else {
                    user = (User) extras.getSerializable(Constants.KEY_USER);
                }
            }

            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.KEY_USER, user);

            getSupportFragmentManager().beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragmentProfile, ProfileFragment.class, bundle)
                    .commit();
        }

        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v ->  finish());
    }
}