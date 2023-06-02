package com.example.hola.adapters;


import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.databinding.ItemLoadingChatBinding;

public class LoadingChatAdapter extends RecyclerView.Adapter<LoadingChatAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLoadingChatBinding binding = ItemLoadingChatBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setAnimation();
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemLoadingChatBinding binding;

        public ViewHolder(@NonNull ItemLoadingChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void setAnimation() {
            startAnimation(binding.imageProfile);
            startAnimation(binding.textName);
            startAnimation(binding.textLatestMessage);
        }

        private void startAnimation(View view) {
            AnimationDrawable animationDrawable = (AnimationDrawable) view.getBackground();
            animationDrawable.setEnterFadeDuration(500);
            animationDrawable.setExitFadeDuration(500);
            animationDrawable.start();
        }
    }
}
