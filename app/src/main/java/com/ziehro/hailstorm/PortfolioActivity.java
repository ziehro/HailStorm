package com.ziehro.hailstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class PortfolioActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView capitalView;
    private TextView holdingsView;
    private ProgressBar progressBar;
    private Button fetchPortfolioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        db = FirebaseFirestore.getInstance();
        capitalView = findViewById(R.id.capital);
        holdingsView = findViewById(R.id.holdings);
        progressBar = findViewById(R.id.progressBar);
        fetchPortfolioButton = findViewById(R.id.fetchPortfolioButton);

        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());

        fetchPortfolio(); // Fetch portfolio when activity is created
    }

    private void fetchPortfolio() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("portfolio").document("latest")
                .addSnapshotListener((documentSnapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        Toast.makeText(PortfolioActivity.this, "Error fetching portfolio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updateUI(documentSnapshot.getData());
                    } else {
                        Log.d("TAG", "Current data: null");
                        Toast.makeText(PortfolioActivity.this, "No portfolio data found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(Map<String, Object> data) {
        capitalView.setText("Capital: $" + data.get("capital"));
        Map<String, Double> holdings = (Map<String, Double>) data.get("holdings");
        StringBuilder holdingsText = new StringBuilder();
        for (String stock : holdings.keySet()) {
            holdingsText.append(stock).append(": ").append(holdings.get(stock)).append(" shares\n");
        }
        holdingsView.setText("Holdings:\n" + holdingsText.toString());
    }
}
