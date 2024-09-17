package com.ziehro.hailstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PortfolioActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView capitalView;
    private TextView holdingsView;
    private ProgressBar progressBar;
    private Button fetchPortfolioButton;
    private TextView predictionsView;
    private TextView totalValueView;

    private BarChart barChart; // BarChart variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        db = FirebaseFirestore.getInstance();
        capitalView = findViewById(R.id.capital);
        holdingsView = findViewById(R.id.holdings);
        progressBar = findViewById(R.id.progressBar);


        barChart = findViewById(R.id.barChart); // Initialize BarChart





        fetchDailyChanges();
    }


    private void updatePredictionsUI(Map<String, Object> data) {
        // Create a sorted list of the keys (ticker names) from the data map
        List<String> sortedKeys = new ArrayList<>(data.keySet());
        Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            // Cast the details to a Map
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
                    if (e != null) {
                        progressBar.setVisibility(View.GONE);
                        Log.w("TAG", "Listen failed.", e);
                        Toast.makeText(PortfolioActivity.this, "Error fetching portfolio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updateUI(documentSnapshot.getData());
                        fetchDailyChanges(); // Fetch daily changes after portfolio data is fetched
                    } else {
                        progressBar.setVisibility(View.GONE);
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

        // Create a sorted list of stock names (keys)
        List<String> sortedStocks = new ArrayList<>(holdings.keySet());
        Collections.sort(sortedStocks); // Sorts alphabetically

        for (String stock : sortedStocks) {
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

    private void fetchDailyChanges() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("daily_changes")
                .get()  // Fetch all documents in the collection
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    List<Integer> colors = new ArrayList<>(); // List to store colors for each bar
                    float index = 0f;

                    // Iterate through all documents in the collection
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId(); // The document ID is the date (e.g., "2024-09-16")
                        Double dailyChange = document.getDouble("daily_change"); // Extract the daily_change field

                        if (dailyChange != null) {
                            // Use the document ID as the label and daily_change as the value
                            entries.add(new BarEntry(index, dailyChange.floatValue()));
                            labels.add(formatDateLabel(docId));  // Format the document ID to a user-friendly date format

                            // Set color based on daily_change value
                            if (dailyChange >= 0) {
                                colors.add(Color.GREEN); // Green for positive changes
                            } else {
                                colors.add(Color.RED); // Red for negative changes
                            }

                            index += 1f;
                        }
                    }

                    // Display the bar chart with the entries, labels, and colors
                    displayBarChart(entries, labels, colors);
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error fetching daily_changes", e);
                    Toast.makeText(PortfolioActivity.this, "Error fetching daily changes", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
    }


    private String formatDateLabel(String timestamp) {
        try {
            // Parse the timestamp from the yyyy-MM-dd format
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);

            // Format the Date object to a user-friendly string (e.g., "Sep 16")
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            // In case of error, return the original timestamp
            Log.e("DateFormatError", "Error parsing date", e);
            return timestamp;
        }
    }


    private void displayBarChart(List<BarEntry> entries, List<String> labels, List<Integer> colors) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No daily changes data available.", Toast.LENGTH_SHORT).show();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Changes");

        // Apply the colors to the dataset
        dataSet.setColors(colors);

        // Set value text size and color for the bars (the values on top of the bars)
        dataSet.setValueTextColor(Color.WHITE);  // Set the value labels to white
        dataSet.setValueTextSize(12f);           // Set the value text size

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f); // Set bar width

        barChart.setData(data);
        barChart.setFitBars(true); // Make the x-axis fit exactly all bars
        barChart.invalidate(); // Refresh the chart

        // Customize the description
        Description description = new Description();
        description.setText("Daily Equity Changes");
        description.setTextColor(Color.WHITE);  // Set description color to white
        description.setTextSize(12f);           // Set description text size
        barChart.setDescription(description);

        // Customize X-axis labels
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels)); // Use formatted date labels
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false); // Optional: Remove grid lines for clarity
        xAxis.setTextColor(Color.WHITE);   // Set the X-axis labels to white
        xAxis.setTextSize(12f);            // Increase the size of the X-axis labels

        // Customize Y-axis labels (left side)
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set the Y-axis labels to white
        leftAxis.setTextSize(12f);          // Increase the size of the Y-axis labels
        leftAxis.setDrawGridLines(false);   // Optionally remove grid lines

        // Customize Y-axis labels (right side - disabled)
        barChart.getAxisRight().setEnabled(false); // Disable right Y-axis for cleaner look

        // Enable touch gestures
        barChart.setTouchEnabled(true);
        barChart.setDragEnabled(true);
        barChart.setScaleEnabled(true);

        // Animate the chart
        barChart.animateY(1000);
    }



}
