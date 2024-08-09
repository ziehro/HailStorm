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
    private TextView predictionsView;
    private TextView totalValueView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        db = FirebaseFirestore.getInstance();
        capitalView = findViewById(R.id.capital);
        holdingsView = findViewById(R.id.holdings);
        progressBar = findViewById(R.id.progressBar);
        fetchPortfolioButton = findViewById(R.id.fetchPortfolioButton);
        totalValueView = findViewById(R.id.totalValue); // Add this line


        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());

        predictionsView = findViewById(R.id.predictionsView);  // Ensure you have a TextView in your layout for predictions

        fetchPortfolio();
        fetchPredictions();
    }

    private void fetchPredictions() {
        db.collection("predictions").document("latest")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updatePredictionsUI(documentSnapshot.getData());
                    } else {
                        Log.d("TAG", "No prediction data available.");
                    }
                });
    }

    private void updatePredictionsUI(Map<String, Object> data) {
        StringBuilder sb = new StringBuilder();
        for (String key : data.keySet()) {
            Map<String, Object> details = (Map<String, Object>) data.get(key);
            sb.append(key).append(": ").append(details.get("Movement"))
                    .append(" (Last Close: ").append(details.get("Last Close"))
                    .append(", Predicted: ").append(details.get("Predicted Price")).append(")\n");
        }
        predictionsView.setText(sb.toString());
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
        // Safely convert "capital" to double
        double capital = 0.0;
        if (data.get("capital") instanceof Number) {
            capital = ((Number) data.get("capital")).doubleValue();
        }
        capitalView.setText("Capital: $" + capital);

        // Safely cast "holdings" and "lastClose" to Maps
        Map<String, Object> holdings = (Map<String, Object>) data.get("holdings");
        Map<String, Object> lastClose = (Map<String, Object>) data.get("lastClose");

        double totalValue = capital;
        StringBuilder holdingsText = new StringBuilder();

        for (String stock : holdings.keySet()) {
            // Safely convert holdings value to double
            double shares = 0.0;
            if (holdings.get(stock) instanceof Number) {
                shares = ((Number) holdings.get(stock)).doubleValue();
            }

            // Safely convert lastClose value to double
            double stockPrice = 0.0;
            if (lastClose.get(stock) instanceof Number) {
                stockPrice = ((Number) lastClose.get(stock)).doubleValue();
            }

            double stockValue = shares * stockPrice;
            totalValue += stockValue;
            holdingsText.append(stock).append(": ").append(shares).append(" shares @ $")
                    .append(stockPrice).append(" each (:$").append(String.format("%.2f", stockValue)).append(")\n");
        }

        holdingsView.setText("Holdings:\n" + holdingsText.toString());
        totalValueView.setText(String.format("Total Portfolio Value: $%.2f", totalValue));
    }


}
