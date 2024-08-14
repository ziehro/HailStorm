package com.ziehro.hailstorm;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PortfolioFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView capitalView;
    private TextView holdingsView;
    private ProgressBar progressBar;
    private TextView predictionsView;
    private TextView totalValueView;
    private String portfolioId;

    public PortfolioFragment(String portfolioId) {
        this.portfolioId = portfolioId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_portfolio, container, false);

        db = FirebaseFirestore.getInstance();
        capitalView = view.findViewById(R.id.capital);
        holdingsView = view.findViewById(R.id.holdings);
        progressBar = view.findViewById(R.id.progressBar);
        predictionsView = view.findViewById(R.id.predictionsView);
        totalValueView = view.findViewById(R.id.totalValue);

        fetchPortfolio();
        fetchPredictions();

        return view;
    }

    private void fetchPredictions() {
        db.collection(portfolioId).document("predictions")
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
        List<String> sortedKeys = new ArrayList<>(data.keySet());
        Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            Map<String, Object> details = (Map<String, Object>) data.get(key);
            sb.append(key).append(": ").append(details.get("Movement"))
                    .append(" (Last Close: ").append(details.get("Last Close"))
                    .append(", Predicted: ").append(details.get("Predicted Price")).append(")\n");
        }
        predictionsView.setText(sb.toString());
    }

    private void fetchPortfolio() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection(portfolioId).document("latest")
                .addSnapshotListener((documentSnapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        Toast.makeText(getContext(), "Error fetching portfolio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updateUI(documentSnapshot.getData());
                    } else {
                        Log.d("TAG", "Current data: null");
                        Toast.makeText(getContext(), "No portfolio data found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUI(Map<String, Object> data) {
        double capital = 0.0;
        if (data.get("capital") instanceof Number) {
            capital = ((Number) data.get("capital")).doubleValue();
        }
        capitalView.setText("Capital: $" + capital);

        Map<String, Object> holdings = (Map<String, Object>) data.get("holdings");
        Map<String, Object> lastClose = (Map<String, Object>) data.get("lastClose");

        double totalValue = capital;
        StringBuilder holdingsText = new StringBuilder();

        List<String> sortedStocks = new ArrayList<>(holdings.keySet());
        Collections.sort(sortedStocks);

        for (String stock : sortedStocks) {
            double shares = 0.0;
            if (holdings.get(stock) instanceof Number) {
                shares = ((Number) holdings.get(stock)).doubleValue();
            }

            double stockPrice = 0.0;
            if (lastClose.get(stock) instanceof Number) {
                stockPrice = ((Number) lastClose.get(stock)).doubleValue();
            }

            double stockValue = shares * stockPrice;
            totalValue += stockValue;
            holdingsText.append(stock).append(": ").append(shares).append(" shares @ $")
                    .append(String.format("%.2f", stockPrice)).append(" each ($").append(String.format("%.2f", stockValue)).append(")\n");
        }

        holdingsView.setText("Holdings:\n" + holdingsText.toString());
        totalValueView.setText(String.format("Total Portfolio Value: $%.2f", totalValue));
    }
}
