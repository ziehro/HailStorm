package com.ziehro.hailstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendForm;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.github.mikephil.charting.data.Entry;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PortfolioActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView predictionsView; // Optional: If you want to display predictions

    private BarChart barChartDailyChanges; // First BarChart
    private BarChart barChartGoogleChange; // Second BarChart
    private LineChart lineChartPredictedVsActual;
    private BarChart barChartAccuracy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        progressBar = findViewById(R.id.progressBar);
        predictionsView = findViewById(R.id.predictionsView); // Initialize if present in XML
        barChartDailyChanges = findViewById(R.id.barChartDailyChanges); // Initialize first BarChart
        barChartGoogleChange = findViewById(R.id.barChartGoogleChange); // Initialize second BarChart
        lineChartPredictedVsActual = findViewById(R.id.lineChartPredictedVsActual);
        barChartAccuracy = findViewById(R.id.barChartAccuracy);

        // Fetch and display accuracy data
        fetchAndDisplayAccuracyData();

        // Fetch data on activity start
        fetchDailyChanges();
        fetchPredictions(); // Optional: Fetch predictions if you have predictionsView
        fetchPredictionVsActualData();

    }

    /**
     * Fetch predictions data from Firestore and update the UI.
     * Optional: Remove if not needed.
     */
    private void fetchPredictions() {
        db.collection("predictions").document("latest")
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("PortfolioActivity", "Listen failed for predictions.", e);
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        updatePredictionsUI(documentSnapshot.getData());
                    } else {
                        Log.d("PortfolioActivity", "No prediction data available.");
                        //predictionsView.setText("No prediction data available.");
                    }
                });
    }

    /**
     * Update the predictions UI based on fetched data.
     *
     * @param data Map containing prediction data.
     */
    private void updatePredictionsUI(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            predictionsView.setText("No prediction data available.");
            return;
        }

        // Create a sorted list of the keys (ticker names) from the data map
        List<String> sortedKeys = new ArrayList<>(data.keySet());
        Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        for (String key : sortedKeys) {
            // Cast the details to a Map
            Map<String, Object> details = (Map<String, Object>) data.get(key);
            if (details != null) {
                sb.append(key).append(": ").append(details.get("Movement"))
                        .append(" (Last Close: ").append(details.get("Last Close"))
                        .append(", Predicted: ").append(details.get("Predicted Price")).append(")\n");
            }
        }
        predictionsView.setText(sb.toString());
    }

    /**
     * Fetch daily changes and Google stock data from Firestore.
     */
    private void fetchDailyChanges() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("daily_changes")
                .orderBy("timestamp") // Ensure chronological order by timestamp
                .get()
                .addOnSuccessListener((QuerySnapshot queryDocumentSnapshots) -> {
                    List<BarEntry> dailyChangeEntries = new ArrayList<>();
                    List<Integer> dailyChangeColors = new ArrayList<>();

                    List<BarEntry> googChangePercentageEntries = new ArrayList<>();
                    List<Integer> googChangeColors = new ArrayList<>();

                    List<String> labels = new ArrayList<>();
                    float index = 0f;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String docId = document.getId();

                        // Skip the 'last_recorded' document if present
                        if (docId.equals("last_recorded")) {
                            continue;
                        }

                        Double dailyChange = document.getDouble("daily_change");
                        Double googChangePercentage = document.getDouble("goog_daily_change_percentage");

                        // Handle daily_change entries
                        if (dailyChange != null) {
                            dailyChangeEntries.add(new BarEntry(index, dailyChange.floatValue()));

                            // Set color based on daily_change value
                            if (dailyChange >= 0) {
                                dailyChangeColors.add(Color.GREEN); // Green for positive changes
                            } else {
                                dailyChangeColors.add(Color.RED); // Red for negative changes
                            }
                        }

                        // Handle Google change percentage entries
                        if (googChangePercentage != null) {
                            googChangePercentageEntries.add(new BarEntry(index, googChangePercentage.floatValue()));

                            // Set color based on Google change percentage
                            if (googChangePercentage >= 0) {
                                googChangeColors.add(Color.GREEN); // Cyan for positive changes
                            } else {
                                googChangeColors.add(Color.RED); // Magenta for negative changes
                            }
                        }

                        // Add label for the X-axis
                        labels.add(formatDateLabel(docId));
                        index += 1f;
                    }

                    // Display the bar charts with respective datasets
                    displayDailyChangesChart(dailyChangeEntries, dailyChangeColors, labels);
                    displayGoogleChangeChart(googChangePercentageEntries, googChangeColors, labels);

                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.w("PortfolioActivity", "Error fetching daily_changes", e);
                    Toast.makeText(PortfolioActivity.this, "Error fetching daily changes", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Format the date string from yyyy-MM-dd to MMM dd (e.g., 2024-09-16 -> Sep 16).
     *
     * @param dateStr Date string in yyyy-MM-dd format.
     * @return Formatted date string.
     */
    private String formatDateLabel(String dateStr) {
        try {
            // Convert from yyyy-MM-dd to MMM dd (e.g., 2024-09-16 -> Sep 16)
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            Log.e("PortfolioActivity", "Error parsing date: " + dateStr, e);
            return dateStr;
        }
    }

    /**
     * Display the Daily Changes BarChart.
     *
     * @param entries List of BarEntry for daily changes.
     * @param colors  List of colors for daily changes.
     * @param labels  List of labels for the X-axis.
     */
    private void displayDailyChangesChart(List<BarEntry> entries, List<Integer> colors, List<String> labels) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No daily changes data available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create DataSet for Daily Changes
        BarDataSet dailyChangeDataSet = new BarDataSet(entries, "Daily Changes");
        dailyChangeDataSet.setColors(colors);
        dailyChangeDataSet.setValueTextColor(Color.WHITE);  // Set the value labels to white
        dailyChangeDataSet.setValueTextSize(12f);           // Set the value text size

        // Create BarData
        BarData data = new BarData(dailyChangeDataSet);
        data.setBarWidth(0.9f); // Set bar width

        barChartDailyChanges.setData(data);
        barChartDailyChanges.setFitBars(true); // Make the x-axis fit exactly all bars
        barChartDailyChanges.invalidate(); // Refresh the chart

        // Customize the description
        Description description = new Description();
        description.setText("");
        description.setTextColor(Color.WHITE);  // Set description color to white
        description.setTextSize(12f);           // Set description text size
        //barChartDailyChanges.setDescription(description);

        // Customize X-axis labels
        XAxis xAxis = barChartDailyChanges.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels)); // Use formatted date labels
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false); // Optional: Remove grid lines for clarity
        xAxis.setTextColor(Color.WHITE);   // Set the X-axis labels to white
        xAxis.setTextSize(12f);            // Increase the size of the X-axis labels

        // Customize Y-axis labels (left side)
        YAxis leftAxis = barChartDailyChanges.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set the Y-axis labels to white
        leftAxis.setTextSize(12f);          // Increase the size of the Y-axis labels
        leftAxis.setDrawGridLines(false);   // Optionally remove grid lines

        // Disable right Y-axis
        barChartDailyChanges.getAxisRight().setEnabled(false); // Disable right Y-axis for cleaner look

        // Configure Legend to distinguish the dataset
                // Set legend form size

        // Enable touch gestures
        barChartDailyChanges.setTouchEnabled(true);
        barChartDailyChanges.setDragEnabled(true);
        barChartDailyChanges.setScaleEnabled(true);

        // Animate the chart
        barChartDailyChanges.animateY(1000);
    }

    /**
     * Display the Google Stock Change Percentage BarChart.
     *
     * @param entries List of BarEntry for Google change percentages.
     * @param colors  List of colors for Google change percentages.
     * @param labels  List of labels for the X-axis.
     */
    private void displayGoogleChangeChart(List<BarEntry> entries, List<Integer> colors, List<String> labels) {
        if (entries.isEmpty()) {
            Toast.makeText(this, "No Google change data available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create DataSet for Google Change Percentages
        BarDataSet googChangeDataSet = new BarDataSet(entries, "Google Change (%)");
        googChangeDataSet.setColors(colors);
        googChangeDataSet.setValueTextColor(Color.WHITE);   // Set the value labels to white
        googChangeDataSet.setValueTextSize(12f);            // Set the value text size

        // Create BarData
        BarData data = new BarData(googChangeDataSet);
        data.setBarWidth(0.9f); // Set bar width

        barChartGoogleChange.setData(data);
        barChartGoogleChange.setFitBars(true); // Make the x-axis fit exactly all bars
        barChartGoogleChange.invalidate(); // Refresh the chart

        // Customize the description
        Description description = new Description();
        description.setText("Google Stock Change (%)");
        description.setTextColor(Color.WHITE);  // Set description color to white
        description.setTextSize(12f);           // Set description text size
        //barChartGoogleChange.setDescription(description);

        // Customize X-axis labels
        XAxis xAxis = barChartGoogleChange.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels)); // Use formatted date labels
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setDrawGridLines(false); // Optional: Remove grid lines for clarity
        xAxis.setTextColor(Color.WHITE);   // Set the X-axis labels to white
        xAxis.setTextSize(12f);            // Increase the size of the X-axis labels

        // Customize Y-axis labels (left side)
        YAxis leftAxis = barChartGoogleChange.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE); // Set the Y-axis labels to white
        leftAxis.setTextSize(12f);          // Increase the size of the Y-axis labels
        leftAxis.setDrawGridLines(false);   // Optionally remove grid lines

        // Disable right Y-axis
        barChartGoogleChange.getAxisRight().setEnabled(false); // Disable right Y-axis for cleaner look

                 // Set legend form size

        // Enable touch gestures
        barChartGoogleChange.setTouchEnabled(true);
        barChartGoogleChange.setDragEnabled(true);
        barChartGoogleChange.setScaleEnabled(true);

        // Animate the chart
        barChartGoogleChange.animateY(1000);
    }

    private void displayPredictionVsActualChart(List<Entry> predictedEntries, List<Entry> actualEntries, List<String> labels) {
        if (predictedEntries.isEmpty() || actualEntries.isEmpty()) {
            Toast.makeText(this, "No prediction or actual data available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create DataSets for predicted and actual prices
        LineDataSet predictedDataSet = new LineDataSet(predictedEntries, "Predicted Prices");
        predictedDataSet.setColor(Color.BLUE);
        predictedDataSet.setValueTextColor(Color.WHITE);
        predictedDataSet.setLineWidth(2f);
        predictedDataSet.setCircleColor(Color.BLUE);
        predictedDataSet.setDrawCircles(true);
        predictedDataSet.setDrawValues(false);

        LineDataSet actualDataSet = new LineDataSet(actualEntries, "Actual Prices");
        actualDataSet.setColor(Color.GREEN);
        actualDataSet.setValueTextColor(Color.WHITE);
        actualDataSet.setLineWidth(2f);
        actualDataSet.setCircleColor(Color.GREEN);
        actualDataSet.setDrawCircles(true);
        actualDataSet.setDrawValues(false);

        // Create LineData
        LineData lineData = new LineData(predictedDataSet, actualDataSet);
        lineChartPredictedVsActual.setData(lineData);
        lineChartPredictedVsActual.invalidate(); // Refresh chart

        // Customize X-axis labels
        XAxis xAxis = lineChartPredictedVsActual.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels)); // Use formatted labels
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(12f);

        // Customize Y-axis labels
        YAxis leftAxis = lineChartPredictedVsActual.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setTextSize(12f);

        // Disable right Y-axis
        lineChartPredictedVsActual.getAxisRight().setEnabled(false);

        // Enable touch gestures
        lineChartPredictedVsActual.setTouchEnabled(true);
        lineChartPredictedVsActual.setDragEnabled(true);
        lineChartPredictedVsActual.setScaleEnabled(true);

        // Animate the chart
        lineChartPredictedVsActual.animateY(1000);
    }
    private void fetchPredictionVsActualData() {
        String portfolio = "portfolio1"; // Replace with dynamic portfolio if needed
        String ticker = "GOOG"; // Replace with dynamic ticker if needed

        db.collection("predictions")
                .document(portfolio)
                .collection("tickers")
                .whereEqualTo("ticker", ticker) // Ensure you're filtering for the correct ticker
                .orderBy("timestamp")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Entry> predictedEntries = new ArrayList<>();
                    List<Entry> actualEntries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    float index = 0f;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double predictedPrice = document.getDouble("predicted_price");
                        Double actualPrice = document.getDouble("actual_price");
                        String timestamp = document.getString("timestamp");

                        if (predictedPrice != null && actualPrice != null && timestamp != null) {
                            predictedEntries.add(new Entry(index, predictedPrice.floatValue()));
                            actualEntries.add(new Entry(index, actualPrice.floatValue()));
                            labels.add(formatDateLabel(timestamp)); // Format the timestamp for X-axis labels
                            index += 1f;
                        }
                    }

                    if (predictedEntries.isEmpty() || actualEntries.isEmpty()) {
                        Toast.makeText(this, "No prediction or actual data available.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Display data on the chart
                        displayPredictionVsActualChart(predictedEntries, actualEntries, labels);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PortfolioActivity", "Error fetching prediction vs actual data", e);
                    Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchAndDisplayAccuracyData() {
        String portfolio = "portfolio1"; // Replace with dynamic portfolio if needed

        db.collection("daily_accuracies")
                .document(portfolio)
                .collection("daily_data") // Subcollection for daily accuracy data
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, List<Double>> accuracyMap = new HashMap<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Long correctPred = document.getLong("correct_predictions");
                        Long totalPred = document.getLong("total_predictions");
                        String date = document.getString("date");

                        if (correctPred != null && totalPred != null && date != null) {
                            double accuracyPercentage = ((double) correctPred / totalPred) * 100.0;

                            // Group accuracies by date if multiple entries per day
                            if (!accuracyMap.containsKey(date)) {
                                accuracyMap.put(date, new ArrayList<>());
                            }
                            accuracyMap.get(date).add(accuracyPercentage);
                        }
                    }

                    List<BarEntry> accuracyEntries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    float index = 0f;

                    for (Map.Entry<String, List<Double>> entry : accuracyMap.entrySet()) {
                        String date = entry.getKey();
                        List<Double> accuracies = entry.getValue();

                        // Calculate average accuracy for the day
                        double sum = 0.0;
                        for (Double acc : accuracies) {
                            sum += acc;
                        }
                        double averageAccuracy = sum / accuracies.size();

                        accuracyEntries.add(new BarEntry(index, (float) averageAccuracy));
                        labels.add(formatDateLabel(date)); // Format the date as needed
                        index += 1f;
                    }

                    if (accuracyEntries.isEmpty()) {
                        Toast.makeText(this, "No accuracy data available.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Display data on the BarChart
                        displayAccuracyBarChart(accuracyEntries, labels);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PortfolioActivity", "Error fetching accuracy data", e);
                    Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayAccuracyBarChart(List<BarEntry> accuracyEntries, List<String> labels) {
        if (accuracyEntries.isEmpty()) {
            Toast.makeText(this, "No accuracy data to display.", Toast.LENGTH_SHORT).show();
            barChartAccuracy.clear();
            barChartAccuracy.invalidate();
            return;
        }

        // Define a threshold for good accuracy (e.g., 70%)
        final float ACCURACY_THRESHOLD = 70f;

        List<Integer> colors = new ArrayList<>();
        for (BarEntry entry : accuracyEntries) {
            if (entry.getY() >= ACCURACY_THRESHOLD) {
                colors.add(getResources().getColor(R.color.teal_700)); // Use a defined color resource
            } else {
                colors.add(getResources().getColor(R.color.red)); // Use a defined color resource
            }
        }

        // Create BarDataSet
        BarDataSet dataSet = new BarDataSet(accuracyEntries, "Daily Prediction Accuracy (%)");
        dataSet.setColors(colors);
        dataSet.setValueTextColor(getResources().getColor(R.color.white));
        dataSet.setValueTextSize(12f);

        // Show value labels on top of bars
        dataSet.setDrawValues(true);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f%%", value);
            }
        });

        // Create BarData
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.9f); // Set custom bar width

        // Set data to BarChart
        barChartAccuracy.setData(barData);
        barChartAccuracy.setFitBars(true); // Make the x-axis fit exactly all bars
        barChartAccuracy.invalidate(); // Refresh chart

        // Customize X-axis labels
        XAxis xAxis = barChartAccuracy.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // One label per interval
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45f); // Rotate labels if dates are long
        xAxis.setTextColor(getResources().getColor(R.color.white));
        xAxis.setTextSize(12f);

        // Customize Y-axis labels
        YAxis leftAxis = barChartAccuracy.getAxisLeft();
        leftAxis.setTextColor(getResources().getColor(R.color.white));
        leftAxis.setTextSize(12f);
        leftAxis.setAxisMinimum(0f); // Start at 0
        leftAxis.setAxisMaximum(100f); // Assuming accuracy is in percentage

        // Add a limit line at the threshold
        LimitLine limitLine = new LimitLine(ACCURACY_THRESHOLD, "Target Accuracy");
        limitLine.setLineColor(getResources().getColor(R.color.red));
        limitLine.setLineWidth(2f);
        limitLine.setTextColor(getResources().getColor(R.color.red));
        limitLine.setTextSize(12f);
        leftAxis.addLimitLine(limitLine);

        // Disable right Y-axis
        barChartAccuracy.getAxisRight().setEnabled(false);

        // Customize chart appearance
        barChartAccuracy.getDescription().setEnabled(false);
        barChartAccuracy.getLegend().setTextColor(getResources().getColor(R.color.white));
        barChartAccuracy.getLegend().setFormSize(12f);
        barChartAccuracy.getLegend().setTextSize(12f);
        barChartAccuracy.getLegend().setForm(Legend.LegendForm.CIRCLE);
        barChartAccuracy.getLegend().setWordWrapEnabled(true);
        barChartAccuracy.getLegend().setEnabled(true);

        // Enable touch gestures
        barChartAccuracy.setTouchEnabled(true);
        barChartAccuracy.setDragEnabled(true);
        barChartAccuracy.setScaleEnabled(true);

        // Animate the chart
        barChartAccuracy.animateY(1000);

        // Optionally, highlight the highest accuracy day
        highlightHighestAccuracyBar(accuracyEntries);
    }

    private void highlightHighestAccuracyBar(List<BarEntry> accuracyEntries) {
        if (accuracyEntries.isEmpty()) return;

        float highestAccuracy = 0f;
        int highestIndex = 0;

        for (int i = 0; i < accuracyEntries.size(); i++) {
            if (accuracyEntries.get(i).getY() > highestAccuracy) {
                highestAccuracy = accuracyEntries.get(i).getY();
                highestIndex = i;
            }
        }

        barChartAccuracy.highlightValue(highestIndex, 0);
    }




}
