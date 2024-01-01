package com.example.hola.adapters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hola.activities.CommentActivity;
import com.example.hola.R;
import com.example.hola.activities.ProfileActivity;
import com.example.hola.databinding.ItemPostBinding;
import com.example.hola.models.Post;
import com.example.hola.utils.Constants;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {

    public static final Integer ALL_POSTS = 0;
    public static final Integer USER_POSTS = 1;
    public static final Integer MY_POSTS = 2;

    private List<Post> posts = new ArrayList<>();
    private final Integer viewType;
    private final FirebaseFirestore db;
    private final String userId;
    private final Comparator<Post> compareByPostedAt;

    public PostAdapter (Integer viewType, String userId) {
        this.viewType = viewType;
        this.userId = userId;
        db = FirebaseFirestore.getInstance();
        compareByPostedAt = Comparator.comparing(Post::getPostedAt).reversed();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPosts(List<Post> posts) {
        this.posts = posts;
        this.posts.sort(compareByPostedAt);
        notifyDataSetChanged();
    }

    public void removePost(String postId) {
        int position = -1;
        for (int i = 0; i < this.posts.size(); i++) {
            if (this.posts.get(i).getId().equals(postId)) {
                position = i;
                break;
            }
        }

        if (position >= 0) {
            this.posts.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindViews(posts.get(position), position);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPostBinding binding;
        private final PostAdapter adapter;

        public ViewHolder(@NonNull ItemPostBinding binding, @NonNull PostAdapter adapter) {
            super(binding.getRoot());
            this.binding = binding;
            this.adapter = adapter;
        }

        private void bindViews(Post post, int position) {
            if (!post.getUser().getImage().isEmpty()) {
                Picasso.get().load(post.getUser().getImage())
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .into(binding.imageProfile);
            }
            else {
                binding.imageProfile.setImageResource(R.drawable.user);
            }

            if (!post.getImage().isEmpty()) {
                binding.imageContent.setVisibility(View.VISIBLE);
                Picasso.get().load(post.getImage())
                        .placeholder(R.drawable.post_image)
                        .error(R.drawable.post_image)
                        .into(binding.imageContent);
            }
            else {
                binding.imageContent.setVisibility(View.GONE);
                binding.imageContent.setImageResource(R.drawable.post_image);
            }

            String username = "@" + post.getUser().getUsername();
            String postDate = formatPostDate(post.getPostedAt());
            String likes = post.getLikes() + (post.getLikes() > 1 ? " likes" : " like");
            String comments = post.getComments() + (post.getComments() > 1 ? " comments" : " comment");

            binding.textUsername.setText(username);
            binding.textPostedAt.setText(postDate);
            binding.textContent.setText(post.getContent());
            binding.textLike.setText(likes);
            binding.textComment.setText(comments);

            binding.textContent.setVisibility(post.getContent().isEmpty() ? View.GONE : View.VISIBLE);
            binding.textContent.setMaxLines(6);
            binding.textContent.setEllipsize(TextUtils.TruncateAt.END);
            binding.textReadMore.setText(binding.textReadMore.getContext().getResources().getString(R.string.read_more));
            binding.imageLike.setImageResource(
                    post.getLikedBy().contains(adapter.userId) ? R.drawable.ic_like : R.drawable.ic_outlined_like
            );

            setListeners(post, position);
        }

        private void setListeners(Post post, int position) {
            if (adapter.viewType.equals(MY_POSTS)) {
                binding.imageDelete.setVisibility(View.VISIBLE);
                binding.imageDelete.setOnClickListener(v -> {
                    Context context = v.getContext();
                    Utils.showConfirmationDialog(
                            context,
                            context.getResources().getString(R.string.delete_post),
                            context.getResources().getString(R.string.delete_post_msg),
                            context.getResources().getString(R.string.yes),
                            context.getResources().getString(R.string.no),
                            (dialog, which) -> deletePost(context, post)
                    );
                });
            }
            else if (adapter.viewType.equals(USER_POSTS)) {
                binding.imageDelete.setVisibility(View.INVISIBLE);
            }
            else {
                binding.imageDelete.setVisibility(View.INVISIBLE);
                if (!post.getPoster().equals(adapter.userId)) {
                    binding.viewProfile.setVisibility(View.VISIBLE);
                    binding.viewProfile.setOnClickListener(v -> {
                        Context context = v.getContext();
                        Intent intent = new Intent(context, ProfileActivity.class);
                        intent.putExtra(Constants.KEY_USER, post.getUser());
                        context.startActivity(intent);
                    });
                }
                else {
                    binding.viewProfile.setVisibility(View.GONE);
                }
            }


            if (post.getContentIsExpandable()) {
                binding.textReadMore.setVisibility(View.VISIBLE);
                binding.textReadMore.setOnClickListener(v -> {
                    Context context = v.getContext();
                    if (binding.textContent.getLineCount() > 6) {
                        binding.textContent.setMaxLines(6);
                        binding.textContent.setEllipsize(TextUtils.TruncateAt.END);
                        ((TextView) v).setText(context.getResources().getString(R.string.read_more));
                    }
                    else {
                        binding.textContent.setMaxLines(Integer.MAX_VALUE);
                        binding.textContent.setEllipsize(null);
                        ((TextView) v).setText(context.getResources().getString(R.string.show_less));
                    }
                });
            }
            else {
                binding.textReadMore.setVisibility(View.GONE);
            }

            binding.imageLike.setOnClickListener(v -> likePost(post, position));

            binding.imageComment.setOnClickListener(v -> {
                Context context = v.getContext();
                Intent intent = new Intent(context, CommentActivity.class);
                intent.putExtra(Constants.FIELD_POST_ID, post.getId());
                context.startActivity(intent);
            });
        }

        private void deletePost(Context context, Post post) {
            FirebaseFirestore db = adapter.db;
            AlertDialog progressDialog = Utils.showProgressDialog(
                    context,
                    LayoutInflater.from(context),
                    context.getResources().getString(R.string.deleting_post)
            );

            db.collection(Constants.COLLECTION_POSTS).document(post.getId()).delete()
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw Objects.requireNonNull(task.getException());
                        return db.collection(Constants.COLLECTION_COMMENTS)
                                .whereEqualTo(Constants.FIELD_POST_ID, post.getId())
                                .get();
                    })
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w("Firestore", "Error deleting document", task.getException());
                            progressDialog.dismiss();
                            showErrorDialog(context);
                            return;
                        }

                        QuerySnapshot queryDocumentSnapshots = task.getResult();
                        if (task.getResult().isEmpty()) {
                            progressDialog.dismiss();
                            adapter.removePost(post.getId());
                            return;
                        }

                        List<DocumentReference> deletedDocuments = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            DocumentReference docRef = doc.getReference();
                            docRef.delete().addOnCompleteListener(deleteTask -> {
                                if (!deleteTask.isSuccessful()) {
                                    Log.w("Firestore", "Error deleting document", task.getException());
                                    progressDialog.dismiss();
                                    return;
                                }

                                deletedDocuments.add(docRef);
                                if (deletedDocuments.size() == post.getComments()) {
                                    progressDialog.dismiss();
                                    adapter.removePost(post.getId());
                                }
                            });
                        }
                    });
        }

        private void likePost(Post post, int position) {
            FirebaseFirestore db = adapter.db;
            String id = adapter.userId;

            db.collection(Constants.COLLECTION_POSTS).document(post.getId())
                    .update(
                            Constants.FIELD_LIKED_BY,
                            post.getLikedBy().contains(id) ? FieldValue.arrayRemove(id) : FieldValue.arrayUnion(id)
                    )
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.w("Firestore", "Error updating document", task.getException());
                            return;
                        }

                        if (post.getLikedBy().contains(id)) {
                            post.getLikedBy().remove(id);
                        }
                        else {
                            post.getLikedBy().add(id);
                        }
                        adapter.notifyItemChanged(position);
                    });
        }

        private String formatPostDate(String date) {
            String postDate = "";

            try {
                Locale locale = new Locale("in", "ID");
                postDate = new SimpleDateFormat(Constants.FORMAT_DATE_TIME, locale)
                        .format(
                                Objects.requireNonNull(new SimpleDateFormat(Constants.FORMAT_DATE_TIME_IN_DOC, locale).parse(date))
                        );
            }
            catch (Exception e) {
                Log.w("SimpleDateFormat", e);
            }

            return postDate;
        }

        private void showErrorDialog(Context context) {
            Utils.showAlertDialog(
                    context,
                    context.getResources().getString(R.string.error),
                    context.getResources().getString(R.string.error_msg),
                    context.getResources().getString(R.string.ok)
            );
        }
    }
}
