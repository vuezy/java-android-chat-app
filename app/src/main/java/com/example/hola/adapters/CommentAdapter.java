package com.example.hola.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.R;
import com.example.hola.activities.ProfileActivity;
import com.example.hola.databinding.ItemCommentBinding;
import com.example.hola.models.Comment;
import com.example.hola.utils.Constants;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private final FirebaseFirestore db;
    private final String userId;
    private final Comparator<Comment> comparator;

    public CommentAdapter(String userId) {
        this.userId = userId;
        db = FirebaseFirestore.getInstance();
        comparator = Comparator.comparing(Comment::getLikes).thenComparing(Comment::getDateTime).reversed();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setComments(List<Comment> comments) {
        this.comments = comments;
        this.comments.sort(comparator);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCommentBinding binding = ItemCommentBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindViews(comments.get(position), position);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemCommentBinding binding;
        private final CommentAdapter adapter;

        public ViewHolder(@NonNull ItemCommentBinding binding, @NonNull CommentAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;
        }

        private void bindViews(Comment comment, int position) {
            if (!comment.getUser().getImage().isEmpty()) {
                Picasso.get().load(comment.getUser().getImage())
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imageProfile);
            }
            else {
                binding.imageProfile.setImageResource(R.drawable.user);
            }

            String username = "@" + comment.getUser().getUsername();
            String commentDate = formatCommentDate(comment.getDateTime());
            String likes = comment.getLikes() + (comment.getLikes() > 1 ? " likes" : " like");

            binding.textUsername.setText(username);
            binding.textDateTime.setText(commentDate);
            binding.textComment.setText(comment.getContent());
            binding.textLike.setText(likes);
            binding.imageLike.setImageResource(
                    comment.getLikedBy().contains(adapter.userId) ? R.drawable.ic_like : R.drawable.ic_outlined_like
            );

            setListeners(comment, position);
        }

        private void setListeners(Comment comment, int position) {
            if (!comment.getSender().equals(adapter.userId)) {
                binding.viewProfile.setVisibility(View.VISIBLE);
                binding.viewProfile.setOnClickListener(v -> {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ProfileActivity.class);
                    intent.putExtra(Constants.KEY_USER, comment.getUser());
                    context.startActivity(intent);
                });
            }
            else {
                binding.viewProfile.setVisibility(View.GONE);
            }

            binding.imageLike.setOnClickListener(v -> {
                String id = adapter.userId;
                adapter.db.collection(Constants.COLLECTION_COMMENTS).document(comment.getId())
                        .update(
                                Constants.FIELD_LIKED_BY,
                                comment.getLikedBy().contains(id) ? FieldValue.arrayRemove(id) : FieldValue.arrayUnion(id)
                        )
                        .addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.w("Firestore", "Error updating document", task.getException());
                                return;
                            }

                            if (comment.getLikedBy().contains(id)) {
                                comment.getLikedBy().remove(id);
                            }
                            else {
                                comment.getLikedBy().add(id);
                            }
                            adapter.notifyItemChanged(position);
                        });
            });
        }

        private String formatCommentDate(String date) {
            String commentDate = "";

            try {
                Locale locale = new Locale("in", "ID");
                commentDate = new SimpleDateFormat(Constants.FORMAT_DATE_TIME, locale)
                        .format(
                                Objects.requireNonNull(new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, locale).parse(date))
                        );
            }
            catch (Exception e) {
                Log.w("SimpleDateFormat", e);
            }

            return commentDate;
        }
    }
}
