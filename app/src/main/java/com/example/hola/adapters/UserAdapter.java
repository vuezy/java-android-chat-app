package com.example.hola.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.R;
import com.example.hola.activities.ProfileActivity;
import com.example.hola.databinding.ItemUserBinding;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.squareup.picasso.Picasso;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> users;

    @SuppressLint("NotifyDataSetChanged")
    public void setUsers(List<User> users) {
        this.users = users;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindViews(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users != null ? users.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemUserBinding binding;

        public ViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        private void bindViews(User user) {
            if (!user.getImage().isEmpty()) {
                Picasso.get().load(user.getImage())
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imageProfile);
            }
            else {
                binding.imageProfile.setImageResource(R.drawable.user);
            }

            binding.textName.setText(user.getName());
            String username = "@" + user.getUsername();
            binding.textUsername.setText(username);

            binding.getRoot().setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra(Constants.KEY_USER, user);
                context.startActivity(intent);
            });
        }
    }
}
