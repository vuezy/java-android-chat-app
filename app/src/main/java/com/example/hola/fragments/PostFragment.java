package com.example.hola.fragments;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.hola.R;
import com.example.hola.activities.AddPostActivity;
import com.example.hola.adapters.PostAdapter;
import com.example.hola.databinding.FragmentPostBinding;
import com.example.hola.models.Post;
import com.example.hola.models.User;
import com.example.hola.utils.Constants;
import com.example.hola.utils.DataStoreManager;
import com.example.hola.utils.Utils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PostFragment extends Fragment {

    private FragmentPostBinding binding;
    private FirebaseFirestore db;
    private PostAdapter adapter;
    private User user;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        DataStoreManager dataStoreManager = DataStoreManager.getInstance(requireActivity().getApplicationContext());
        db = FirebaseFirestore.getInstance();

        Bundle arguments = getArguments();
        if (arguments != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                user = arguments.getSerializable(Constants.KEY_USER, User.class);
            }
            else {
                user = (User) arguments.getSerializable(Constants.KEY_USER);
            }
        }

        if (user == null) {
            adapter = new PostAdapter(PostAdapter.ALL_POSTS, dataStoreManager.getValue(Constants.FIELD_ID));
        }
        else {
            if (user.getId().equals(dataStoreManager.getValue(Constants.FIELD_ID)))
                adapter = new PostAdapter(PostAdapter.MY_POSTS, dataStoreManager.getValue(Constants.FIELD_ID));
            else
                adapter = new PostAdapter(PostAdapter.USER_POSTS, dataStoreManager.getValue(Constants.FIELD_ID));
        }
        binding.recyclerViewPost.setAdapter(adapter);
        binding.recyclerViewPost.setLayoutManager(new LinearLayoutManager(getContext()));
        setListeners();
    }

    @Override
    public void onStart() {
        super.onStart();
        getPosts();
    }

    private void getPosts() {
        if (user == null)
            getAllPosts();
        else
            getPostsByUser();
    }

    private void getAllPosts() {
        if (adapter.getItemCount() == 0) showLoading();
        List<Post> posts = new ArrayList<>();

        db.collection(Constants.COLLECTION_POSTS).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("Firestore", "Error getting document", task.getException());
                showErrorDialog();
                return;
            }

            QuerySnapshot queryDocumentSnapshots = task.getResult();
            if (queryDocumentSnapshots.isEmpty()) {
                showHelperMessage();
                binding.swipeRefreshLayout.setRefreshing(false);
            }
            else {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    Post post = Objects.requireNonNull(doc.toObject(Post.class));
                    db.collection(Constants.COLLECTION_USERS).document(post.getPoster()).get()
                            .addOnCompleteListener(aTask -> {
                                if (!aTask.isSuccessful()) {
                                    Log.w("Firestore", "Error getting document", aTask.getException());
                                    showErrorDialog();
                                    return;
                                }

                                post.setUser(aTask.getResult().toObject(User.class));
                                posts.add(post);

                                if (posts.size() == queryDocumentSnapshots.getDocuments().size()) {
                                    adapter.setPosts(posts);
                                    showRecyclerView();
                                    binding.swipeRefreshLayout.setRefreshing(false);
                                }
                            });
                }
            }
        });
    }

    private void getPostsByUser() {
        if (adapter.getItemCount() == 0) showLoading();
        List<Post> posts = new ArrayList<>();

        db.collection(Constants.COLLECTION_POSTS).whereEqualTo(Constants.FIELD_POSTER, user.getId()).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("Firestore", "Error getting document", task.getException());
                        showErrorDialog();
                        return;
                    }

                    QuerySnapshot queryDocumentSnapshots = task.getResult();
                    if (queryDocumentSnapshots.isEmpty()) {
                        showHelperMessage();
                        binding.swipeRefreshLayout.setRefreshing(false);
                    }
                    else {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Post post = Objects.requireNonNull(doc.toObject(Post.class));
                            post.setUser(user);
                            posts.add(post);

                            if (posts.size() == queryDocumentSnapshots.getDocuments().size()) {
                                adapter.setPosts(posts);
                                showRecyclerView();
                                binding.swipeRefreshLayout.setRefreshing(false);
                            }
                        }
                    }
                });
    }

    private void setListeners() {
        binding.btnAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddPostActivity.class);
            startActivity(intent);
        });
        binding.swipeRefreshLayout.setOnRefreshListener(this::getPosts);
    }

    private void showLoading() {
        binding.swipeRefreshLayout.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    private void showHelperMessage() {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerViewPost.setVisibility(View.GONE);

        if (user == null) {
            binding.textNoPost.setText(getResources().getString(R.string.no_post_1));
            binding.textNoPost.setVisibility(View.VISIBLE);
            binding.btnAddPost.setVisibility(View.VISIBLE);
        }
        else {
            binding.btnAddPost.setVisibility(View.GONE);
            binding.textNoPost.setText(getResources().getString(R.string.no_post_2));
            binding.textNoPost.setVisibility(View.VISIBLE);
        }

        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showRecyclerView() {
        binding.progressBar.setVisibility(View.GONE);
        binding.textNoPost.setVisibility(View.GONE);
        binding.btnAddPost.setVisibility(View.GONE);
        binding.recyclerViewPost.setVisibility(View.VISIBLE);
        binding.swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showErrorDialog() {
        Utils.showAlertDialog(
                getContext(),
                getResources().getString(R.string.error),
                getResources().getString(R.string.error_msg),
                getResources().getString(R.string.ok)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}