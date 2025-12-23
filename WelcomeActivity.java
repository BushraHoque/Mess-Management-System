package com.example.messmanagement;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    private Button btnSignUp, btnLogin, btnContact;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, go to home
            startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
            finish();
            return;
        }

        // Initialize views
        btnSignUp = findViewById(R.id.btnSignUp);
        btnLogin = findViewById(R.id.btnLogin);
        btnContact = findViewById(R.id.btnContact);

        // Set click listeners
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, SignUpActivity.class));
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
            }
        });

        btnContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContactDialog();
            }
        });
    }

    private void showContactDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_contact);

        TextView tvPrivacy = dialog.findViewById(R.id.tvPrivacy);
        TextView tvDisclaimer = dialog.findViewById(R.id.tvDisclaimer);
        TextView tvDeveloper = dialog.findViewById(R.id.tvDeveloper);
        Button btnUnderstand = dialog.findViewById(R.id.btnUnderstand);

        tvPrivacy.setText("Privacy Policy:\nYour data is securely stored and will not be shared with third parties.");
        tvDisclaimer.setText("Disclaimer:\nThis app is for educational purposes. Use at your own risk.");
        tvDeveloper.setText("Developer Info:\nDeveloped by [Tahura Akter Tripty, Bushra Hoque]\nEmail: [2022-1-60-051@std.ewubd.edu , 2022-1-60-154@std.ewubd.edu]");

        btnUnderstand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
