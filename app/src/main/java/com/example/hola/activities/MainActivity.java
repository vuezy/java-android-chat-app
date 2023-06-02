package com.example.hola.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;

import com.example.hola.R;
import com.example.hola.adapters.ViewPagerAdapter;
import com.example.hola.databinding.ActivityMainBinding;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.android.material.tabs.TabLayout;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DataStoreManager dataStoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        dataStoreManager = DataStoreManager.getInstance(getApplicationContext());
        configureViewPager();
        setListeners();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        int tabIndex = 0;
        if (extras != null) {
            tabIndex = extras.getInt(Constants.FIELD_TAB_INDEX);
        }
        binding.viewPager.setCurrentItem(tabIndex);
    }

    private void configureViewPager() {
        binding.viewPager.setAdapter(new ViewPagerAdapter(this));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Objects.requireNonNull(binding.tabLayout.getTabAt(position)).select();
            }
        });
    }

    private void setListeners() {
        binding.imageLogOut.setOnClickListener(v -> {
            Utils.showConfirmationDialog(
                    this,
                    getResources().getString(R.string.log_out),
                    getResources().getString(R.string.log_out_msg),
                    getResources().getString(R.string.yes),
                    getResources().getString(R.string.no),
                    (dialog, which) -> {
                        dataStoreManager.clear();
                        Intent intent = new Intent(this, LogInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
            );
        });

        binding.imageSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });
    }
}