package com.example.messmanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MemberListActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private ProgressBar progressBar;
    private ImageView ivNotification, ivProfile, ivHome, ivLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<User> memberList;
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_list);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        memberList = new ArrayList<>();

        rvMembers = findViewById(R.id.rvMembers);
        progressBar = findViewById(R.id.progressBar);
        ivNotification = findViewById(R.id.ivNotification);
        ivProfile = findViewById(R.id.ivProfile);
        ivHome = findViewById(R.id.ivHome);
        ivLogout = findViewById(R.id.ivLogout);

        // Setup RecyclerView
        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberAdapter(memberList);
        rvMembers.setAdapter(adapter);

        // Setup bottom navigation
        setupBottomNavigation();

        loadMembers();
    }

    private void loadMembers() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    memberList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        memberList.add(user);
                    }

                    adapter.updateList(memberList);
                    progressBar.setVisibility(View.GONE);

                    if (memberList.isEmpty()) {
                        Toast.makeText(this, "No members found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load members: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setupBottomNavigation() {
        ivProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
        });

        ivNotification.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationActivity.class));
        });

        ivLogout.setOnClickListener(v -> {
            showLogoutDialog();
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = loginPrefs.edit();
                    editor.clear();
                    editor.apply();

                    mAuth.signOut();

                    Intent intent = new Intent(this, WelcomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }
}
