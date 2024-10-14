package com.ziehro.hailstorm;

import androidx.annotation.NonNull;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.github.mikephil.charting.data.Entry;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
    private LineChart lineChart;
    private BarChart barChartAccuracy;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        progressBar = findViewById(R.id.progressBar);

        barChartDailyChanges = findViewById(R.id.barChartDailyChanges); // Initialize first BarChart
        barChartGoogleChange = findViewById(R.id.barChartGoogleChange); // Initialize second BarChart
        lineChart = findViewById(R.id.lineChartPredictedVsActual); // Initialize LineChart

        // Fetch and display accuracy data
        fetchAndDisplayAccuracyData();

        // Fetch data on activity start
        fetchDailyChanges();
        fetchPredictions(); // Optional: Fetch predictions if you have predictionsView
        displayDailyAccuracies();

    }

    private class DailyAccuracy {
        private String date;
        private Double accuracy;

        public DailyAccuracy(String date, Double accuracy) {
            this.date = date;
            this.accuracy = accuracy;
        }

        public String getDate() {
            return date;
        }

        public Double getAccuracy() {
            return accuracy;
        }
    }

    /**
     * Fetches daily accuracies from Firestore and displays them in the LineChart.
     */
    private void displayDailyAccuracies() {
        // Reference to the 'daily_accuracies' collection
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("daily_accuracies")
                .orderBy("date") // Ensure data is ordered by date
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DailyAccuracy> dailyAccuracyList = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String dateStr = document.getString("date");
                                Double accuracy = document.getDouble("accuracy");

                                if (dateStr != null && accuracy != null) {
                                    dailyAccuracyList.add(new DailyAccuracy(dateStr, accuracy));
                                }
                            }

                            // Check if data exists
                            if (dailyAccuracyList.isEmpty()) {
                                Log.w("Firestore", "No daily accuracy data found.");
                                // Optionally, display a message to the user
                                lineChart.setVisibility(View.GONE);
                                // You can also show a TextView indicating no data
                                return;
                            }

                            // Sort the list by date in ascending order
                            Collections.sort(dailyAccuracyList, new Comparator<DailyAccuracy>() {
                                @Override
                                public int compare(DailyAccuracy o1, DailyAccuracy o2) {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                                    try {
                                        Date d1 = sdf.parse(o1.getDate());
                                        Date d2 = sdf.parse(o2.getDate());
                                        return d1.compareTo(d2);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                        return 0;
                                    }
                                }
                            });

                            // Prepare entries for the chart
                            List<Entry> entries = new ArrayList<>();
                            final List<String> dates = new ArrayList<>(); // For X-Axis labels
                            int index = 0;
                            for (DailyAccuracy da : dailyAccuracyList) {
                                entries.add(new Entry(index, da.getAccuracy().floatValue()));
                                dates.add(da.getDate());
                                index++;
                            }

                            // Create LineDataSet
                            LineDataSet dataSet = new LineDataSet(entries, "Daily Accuracy (%)");
                            dataSet.setColor(getResources().getColor(R.color.teal_200)); // Set your desired color
                            dataSet.setLineWidth(2f);
                            dataSet.setCircleRadius(4f);
                            dataSet.setDrawValues(false); // Hide values on the chart
                            dataSet.setDrawCircles(true);

                            // Create LineData
                            LineData lineData = new LineData(dataSet);
                            lineChart.setData(lineData);

                            // Configure X-Axis
                            XAxis xAxis = lineChart.getXAxis();
                            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                            xAxis.setGranularity(1f);
                            xAxis.setDrawGridLines(false);
                            xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.IndexAxisValueFormatter(dates));
                            xAxis.setLabelRotationAngle(-45f); // Rotate labels if dates overlap

                            // Configure Y-Axis
                            YAxis leftAxis = lineChart.getAxisLeft();
                            leftAxis.setAxisMinimum(0f); // Minimum accuracy
                            leftAxis.setAxisMaximum(100f); // Maximum accuracy
                            YAxis rightAxis = lineChart.getAxisRight();
                            rightAxis.setEnabled(false); // Disable right Y-Axis

                            // Configure Description
                            Description description = new Description();
                            description.setText("Daily Model Accuracies");
                            description.setTextColor(getResources().getColor(R.color.white));
                            lineChart.setDescription(description);

                            // Additional Chart Configurations
                            lineChart.getLegend().setEnabled(true);
                            lineChart.getDescription().setEnabled(true);
                            lineChart.animateX(1000); // Animation

                            // Refresh the chart
                            lineChart.invalidate();

                            // Make the chart visible
                            lineChart.setVisibility(View.VISIBLE);

                        } else {
                            Log.w("Firestore", "Error getting documents.", task.getException());
                        }
                    }
                });
    }
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




    private void fetchAndDisplayAccuracyData() {
        String portfolio = "portfolio1"; // Replace with dynamic portfolio if needed

        db.collection("daily_accuracies")
                .document(portfolio)
                .collection("daily_data") // Subcollection for daily accuracy data
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<BarEntry> accuracyEntries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    float index = 0f;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double correctPred = document.getDouble("correct_predictions");
                        Double totalPred = document.getDouble("total_predictions");
                        String date = document.getString("date");

                        if (correctPred != null && totalPred != null && date != null) {
                            double accuracyPercentage = (correctPred / totalPred) * 100.0;
                            accuracyEntries.add(new BarEntry(index, (float) accuracyPercentage));
                            labels.add(formatDateLabel(date)); // Format the date as needed
                            index += 1f;
                        }
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
        barChartAccuracy.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
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
