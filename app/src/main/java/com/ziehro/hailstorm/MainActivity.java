package com.ziehro.hailstorm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText frequencyInput;
    private ProgressBar progressBar;
    private TextView portfolio0TotalValue;
    private TextView  controlPortfolioTotalValue;
    private Button updateFrequencyButton, resetControlPortfolio, fetchPortfolioButton, dailyPerformanceBtn, openPortfolioAllButton;
    private TextView portfolio0Accuracy;
    private TextView portfolio1Accuracy, portfolio2Accuracy, portfolio3Accuracy, portfolio4Accuracy, portfolio5Accuracy, portfolio6Accuracy, portfolio7Accuracy;


    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    private double initialCapital = 100000.0;
    private double controlTotalValue = 0.0;
    private Spinner commandSpinner;
    private Button sendCommandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
        fetchPortfolio();
        // Removed redundant fetchAllPortfoliosTotalValue call
    }

    String API_KEY_ID = BuildConfig.ALPACA_API_KEY_ID;
    String SECRET_KEY = BuildConfig.ALPACA_SECRET_KEY;

    private void initViews() {
        db = FirebaseFirestore.getInstance();

        frequencyInput = findViewById(R.id.frequencyInput);
        progressBar = findViewById(R.id.progressBar);
        portfolio0TotalValue = findViewById(R.id.portfolio0TotalValue);

        controlPortfolioTotalValue = findViewById(R.id.controlPortfolioTotalValue);

        portfolio0Accuracy = findViewById(R.id.portfolio0Accuracy);


        updateFrequencyButton = findViewById(R.id.updateFrequencyButton);
        resetControlPortfolio = findViewById(R.id.resetControlButton);
        fetchPortfolioButton = findViewById(R.id.fetchPortfolioButton);
        dailyPerformanceBtn = findViewById(R.id.dailyPerformaceBtn);
        openPortfolioAllButton = findViewById(R.id.openPortfolioAllActivityButton);

        commandSpinner = findViewById(R.id.commandSpinner);
        sendCommandButton = findViewById(R.id.sendCommandButton);

        // Set up the Spinner with the command options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.command_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        commandSpinner.setAdapter(adapter);
    }

    private void initListeners() {
        updateFrequencyButton.setOnClickListener(v -> updateFrequency());
        resetControlPortfolio.setOnClickListener(v -> createControlPortfolio(getCurrentFocus()));
        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());
        dailyPerformanceBtn.setOnClickListener(v -> openPortfolioActivity());
        openPortfolioAllButton.setOnClickListener(v -> openPortfolioAllActivity());
        sendCommandButton.setOnClickListener(v -> sendSelectedCommand());
        // Add confirmation dialogs for the reset buttons
        findViewById(R.id.resetControlButton).setOnClickListener(v -> showConfirmationDialog("Are you sure you want to reset the control portfolio?", () -> createControlPortfolio(v)));
    }

    private void showConfirmationDialog(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onConfirm.run())
                .setNegativeButton("No", null)
                .show();
    }

    private void sendSelectedCommand() {
        String selectedCommand = commandSpinner.getSelectedItem().toString();

        if (selectedCommand.equals("Select a Command")) {
            Toast.makeText(this, "Please select a valid command.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a command object to send to Firebase
        Map<String, Object> commandData = new HashMap<>();
        commandData.put("frequency", selectedCommand);
        commandData.put("timestamp", FieldValue.serverTimestamp());
        commandData.put("reschedule", "yes");
        // You can choose where to store commands in Firebase. For example, in a "commands" collection.
        db.collection("config").document("updateFrequency")
                .set(commandData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Command sent successfully.", Toast.LENGTH_SHORT).show();
                    Log.d("MainActivity", "Command sent with ID: ");

                    // Optionally, reset the Spinner to prompt the user
                    commandSpinner.setSelection(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send command.", Toast.LENGTH_SHORT).show();
                    Log.w("MainActivity", "Error adding command", e);
                });
    }
    private void updateFrequency() {
        String frequency = frequencyInput.getText().toString();
        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("frequency", frequency);
        updateData.put("reschedule", "yes");

        db.collection("config").document("updateFrequency")
                .set(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG", "Frequency and reschedule flag updated successfully");
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error updating frequency", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchAllPortfoliosTotalValue() {
        String[] portfolioIds = {
                "portfolio1", "portfolio2", "portfolio3",
                "portfolio4", "portfolio5", "portfolio6", "portfolio7"
        };
        Map<String, Double> portfolioValues = new HashMap<>();

        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String portfolioId : portfolioIds) {
            DocumentReference docRef = db.collection(portfolioId).document("latest");
            tasks.add(docRef.get());
            Log.d("DebugFetch", "Added task for " + portfolioId);

        }

        Tasks.whenAllSuccess(tasks)
                .addOnSuccessListener(results -> {
                    Log.d("DebugFetch", "Tasks.whenAllSuccess onSuccessListener triggered.");

                    for (int i = 0; i < results.size(); i++) {
                        DocumentSnapshot documentSnapshot = (DocumentSnapshot) results.get(i);
                        String portfolioId = portfolioIds[i];
                        double totalValue = calculateTotalValue(documentSnapshot);
                        portfolioValues.put(portfolioId, totalValue);

                        // Update the portfolio's total value in the UI
                        String totalValueText = String.format(
                                "Portfoliosss %s Total Value: $%.2f",
                                portfolioId.replace("portfolio", ""), totalValue
                        );
                        updatePortfolioTextView(portfolioId, totalValueText);
                    }

                    // Once all portfolios are processed, apply the color gradient
                    fetchAllPortfoliosAccuracy(); // Fetch and display accuracy data
                    applyColorGradient(portfolioValues);

                    // Hide the progress bar after all operations are complete
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error fetching portfolio data", e);
                    Toast.makeText(
                            MainActivity.this,
                            "Error fetching portfolio data",
                            Toast.LENGTH_SHORT
                    ).show();
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void fetchAllPortfoliosAccuracy() {
        String[] portfolioIds = {
                "portfolio1", "portfolio2", "portfolio3",
                "portfolio4", "portfolio5", "portfolio6", "portfolio7"
        };

    }

    private double calculateTotalValue(DocumentSnapshot documentSnapshot) {
        if (documentSnapshot.exists()) {
            Double capital = documentSnapshot.getDouble("capital");
            Map<String, Object> holdings = (Map<String, Object>) documentSnapshot.get("holdings");
            Map<String, Object> lastClose = (Map<String, Object>) documentSnapshot.get("lastClose");

            double totalValue = (capital != null) ? capital : 0.0;

            if (holdings != null && lastClose != null) {
                for (String stock : holdings.keySet()) {
                    Double shares = (holdings.get(stock) instanceof Number) ? ((Number) holdings.get(stock)).doubleValue() : 0.0;
                    Double stockPrice = (lastClose.get(stock) instanceof Number) ? ((Number) lastClose.get(stock)).doubleValue() : 0.0;
                    totalValue += shares * stockPrice;
                }
            }

            return totalValue;
        }
        return 0.0;
    }

    private void applyColorGradient(Map<String, Double> portfolioValues) {
        if (portfolioValues.isEmpty()) return;

        double maxValue = Collections.max(portfolioValues.values());
        double minValue = Collections.min(portfolioValues.values());

        // Avoid division by zero
        double range = maxValue - minValue;
        if (range == 0) range = 1;

        for (Map.Entry<String, Double> entry : portfolioValues.entrySet()) {
            String portfolioId = entry.getKey();
            double value = entry.getValue();

            int color;
            if (value < controlTotalValue) {
                // Set color to red if the portfolio value is below the control total value
                color = Color.rgb(255, 0, 0);
            } else {
                // Calculate color intensity based on the portfolio's value relative to the min/max
                float intensity = (float) ((value - minValue) / range);
                intensity = Math.min(Math.max(intensity, 0f), 1f); // Clamp between 0 and 1
                int green = (int) (255 * intensity);
                color = Color.rgb(0, green, 0);
            }

            // Set the background color of the corresponding box
            View colorBox = findViewById(getColorBoxId(portfolioId));
            if (colorBox != null) {
                colorBox.setBackgroundColor(color);
            }

            // Update the portfolio's total value text
            String displayPortfolioId = portfolioId.replace("portfolio", "");
            String totalValueText = String.format("Portfolio %s Total Value: $%.2f", displayPortfolioId, value);

            TextView portfolioValueTextView = findViewById(getPortfolioTextViewId(portfolioId));
            if (portfolioValueTextView != null) {
                portfolioValueTextView.setText(totalValueText);
            }
        }
    }

    private int getColorBoxId(String portfolioId) {
        switch (portfolioId) {

            default:
                return 0;
        }
    }

    private int getPortfolioTextViewId(String portfolioId) {
        switch (portfolioId) {

            default:
                return 0;
        }
    }

    private void updatePortfolioTotalValue(String portfolioId, DocumentSnapshot documentSnapshot) {
        // Removed as it's no longer needed
    }

    private void updatePortfolioTextView(String portfolioId, String totalValueText) {
        if (portfolioId.equals("Control")) {
            controlPortfolioTotalValue.setText(totalValueText);
        }
    }



    private void openPortfolioActivity() {
        Intent intent = new Intent(MainActivity.this, PortfolioActivity.class);
        startActivity(intent);
    }

    private void openPortfolioAllActivity() {
        Intent intent = new Intent(MainActivity.this, PortfolioActivityAll.class);
        startActivity(intent);
    }

    private void fetchPortfolio() {
        progressBar.setVisibility(View.VISIBLE);
        fetchAlpacaAccount();
        fetchAllPortfoliosTotalValue();
    }



    public void createControlPortfolio(View view) {
        db.collection("predictions").document("GOOG")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double current_price = (double) documentSnapshot.get("current_price");
                        if (current_price != 3) {
                            setupControlPortfolio(current_price);
                        } else {
                            Toast.makeText(MainActivity.this, "No last close prices found in portfolio1", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Portfolio1 document does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error fetching last close prices from portfolio1", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error fetching last close prices from portfolio1", e);
                });
        fetchPortfolio();
    }

    private void setupControlPortfolio(double lastClose) {
        String[] tickers = {"GOOG"};

        double investmentPerStock = initialCapital / tickers.length;

        Map<String, Object> controlPortfolio = new HashMap<>();
        controlPortfolio.put("capital", initialCapital);

        Map<String, Object> holdings = new HashMap<>();

        for (String ticker : tickers) {
            Double purchasePrice = lastClose;
            if (purchasePrice > 0) {
                double numShares = investmentPerStock / purchasePrice;
                Map<String, Object> stockInfo = new HashMap<>();
                stockInfo.put("shares", numShares);
                stockInfo.put("purchasePrice", purchasePrice);

                holdings.put(ticker, stockInfo);
            } else {
                Log.w("TAG", "No valid last close price for " + ticker);
            }
        }

        controlPortfolio.put("holdings", holdings);

        db.collection("Control").document("latest")
                .set(controlPortfolio)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Control portfolio created successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error creating control portfolio", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error creating control portfolio", e);
                });
    }

    private void fetchAlpacaAccount() {

        String url = "https://paper-api.alpaca.markets/v2/account";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("APCA-API-KEY-ID", API_KEY_ID)
                .addHeader("APCA-API-SECRET-KEY", SECRET_KEY)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Handle request failure
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Failed to fetch Alpaca account", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
                Log.e("AlpacaAPI", "Error fetching account: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonObject account = gson.fromJson(responseBody, JsonObject.class);
                    double equity = account.get("equity").getAsDouble();

                    runOnUiThread(() -> {
                        portfolio0TotalValue.setText(String.format("Alpaca Total Value: $%.2f", equity));
                        portfolio0TotalValue.setTextColor(Color.YELLOW); // Set text color to yellow

                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch Alpaca account", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    Log.e("AlpacaAPI", "Unsuccessful response: " + response.code());
                }
            }
        });
    }
}
