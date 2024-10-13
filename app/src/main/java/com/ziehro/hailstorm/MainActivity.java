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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText frequencyInput;
    private ProgressBar progressBar;

    private Button updateFrequencyButton, resetControlPortfolio, fetchPortfolioButton, dailyPerformanceBtn, openPortfolioAllButton;

    private TextView modelR2, modelMAE, modelRMSE, modelAccuracy; // Added TextViews for model metrics

    private TextView alpacaEquity, alpacaHoldings; // Added TextViews for Alpaca data

    private OkHttpClient client = new OkHttpClient();
    private Gson gson = new Gson();

    private double initialCapital = 100000.0;
    private Spinner commandSpinner;
    private Button sendCommandButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        initViews();
        initListeners();
        fetchPortfolio();
    }

    String API_KEY_ID = BuildConfig.ALPACA_API_KEY_ID;
    String SECRET_KEY = BuildConfig.ALPACA_SECRET_KEY;

    private void initViews() {
        db = FirebaseFirestore.getInstance();

        frequencyInput = findViewById(R.id.frequencyInput);
        progressBar = findViewById(R.id.progressBar);

        modelR2 = findViewById(R.id.modelR2);
        modelMAE = findViewById(R.id.modelMAE);
        modelRMSE = findViewById(R.id.modelRMSE);
        modelAccuracy = findViewById(R.id.modelAccuracy);

        alpacaEquity = findViewById(R.id.alpacaEquity);
        alpacaHoldings = findViewById(R.id.alpacaHoldings);

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
        resetControlPortfolio.setOnClickListener(v -> showConfirmationDialog("Are you sure you want to reset the control portfolio?", () -> createControlPortfolio(v)));
        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());
        dailyPerformanceBtn.setOnClickListener(v -> openPortfolioActivity());
        openPortfolioAllButton.setOnClickListener(v -> openPortfolioAllActivity());
        sendCommandButton.setOnClickListener(v -> sendSelectedCommand());
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
        String frequency = frequencyInput.getText().toString().trim();
        if (frequency.isEmpty()) {
            Toast.makeText(this, "Frequency input cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("frequency", frequency);
        updateData.put("reschedule", "yes");

        db.collection("config").document("updateFrequency")
                .set(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG", "Frequency and reschedule flag updated successfully");
                    Toast.makeText(MainActivity.this, "Frequency updated successfully.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error updating frequency", e);
                    Toast.makeText(MainActivity.this, "Error updating frequency.", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
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
        fetchModelMetrics();
        fetchModelAccuracy();
    }


    public void createControlPortfolio(View view) {
        db.collection("predictions").document("GOOG")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double currentPrice = documentSnapshot.getDouble("current_price");
                        if (currentPrice != null && currentPrice > 0) {
                            setupControlPortfolio(currentPrice);
                        } else {
                            Toast.makeText(MainActivity.this, "Invalid current price for GOOG.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "GOOG document does not exist in predictions.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error fetching current price from predictions.", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error fetching current price from predictions", e);
                });
    }

    /**
     * Sets up the control portfolio by investing as much as possible based on the current stock price.
     * @param currentPrice The current price of the stock.
     */
    private void setupControlPortfolio(double currentPrice) {
        String[] tickers = {"GOOG"};

        // Initialize the control portfolio map
        Map<String, Object> controlPortfolio = new HashMap<>();
        controlPortfolio.put("capital", initialCapital); // Total capital invested

        Map<String, Object> holdings = new HashMap<>();

        for (String ticker : tickers) {
            Double purchasePrice = currentPrice;
            if (purchasePrice > 0) {
                double numShares = initialCapital / purchasePrice; // Invest all capital into one stock
                numShares = Math.floor(numShares * 100) / 100; // Round down to 2 decimal places if needed

                Map<String, Object> stockInfo = new HashMap<>();
                stockInfo.put("shares", numShares);
                stockInfo.put("purchasePrice", purchasePrice);

                holdings.put(ticker, stockInfo);
            } else {
                Log.w("TAG", "No valid current price for " + ticker);
            }
        }

        controlPortfolio.put("holdings", holdings);

        // Save the control portfolio to Firestore
        db.collection("Control").document("latest")
                .set(controlPortfolio)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MainActivity.this, "Control portfolio created successfully", Toast.LENGTH_SHORT).show();
                    fetchPortfolio(); // Refresh portfolio data after creation
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error creating control portfolio", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error creating control portfolio", e);
                });
    }

    /**
     * Fetches Alpaca account data including equity.
     */
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
                        alpacaEquity.setText(String.format("Alpaca Equity: $%.2f", equity));
                        alpacaEquity.setTextColor(Color.YELLOW); // Set text color to yellow

                        fetchAlpacaHoldings(); // Fetch holdings after fetching equity
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


    private void fetchAlpacaHoldings() {
        String url = "https://paper-api.alpaca.markets/v2/positions";

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
                    Toast.makeText(MainActivity.this, "Failed to fetch Alpaca holdings", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
                Log.e("AlpacaAPI", "Error fetching holdings: ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    JsonArray holdingsArray = gson.fromJson(responseBody, JsonArray.class);

                    StringBuilder holdingsBuilder = new StringBuilder();
                    if (holdingsArray.size() > 0) {
                        for (JsonElement holdingElement : holdingsArray) {
                            JsonObject holding = holdingElement.getAsJsonObject();
                            String symbol = holding.get("symbol").getAsString();
                            double qty = holding.get("qty").getAsDouble();
                            double avgEntryPrice = holding.get("avg_entry_price").getAsDouble();
                            double currentPrice = holding.get("current_price").getAsDouble();
                            double marketValue = holding.get("market_value").getAsDouble();

                            holdingsBuilder.append(String.format(
                                    "%s: %.2f shares @ $%.2f each ($%.2f)\n",
                                    symbol, qty, currentPrice, marketValue
                            ));
                        }
                    } else {
                        holdingsBuilder.append("No holdings.");
                    }

                    runOnUiThread(() -> {
                        alpacaHoldings.setText("Alpaca Holdings:\n" + holdingsBuilder.toString());
                        alpacaHoldings.setTextColor(Color.YELLOW); // Set text color to yellow

                        progressBar.setVisibility(View.GONE);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch Alpaca holdings", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    });
                    Log.e("AlpacaAPI", "Unsuccessful response: " + response.code());
                }
            }
        });
    }

    /**
     * Fetches the most recent model metrics from Firebase Firestore and displays them.
     */
    private void fetchModelMetrics() {
        // Reference to the 'model_evaluations' collection
        db.collection("model_evaluations").document("portfolio1").collection("tickers")
                .orderBy("__name__") // Order by document ID (date)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Assuming documents are named as 'YYYY-MM-DD', the last document is the most recent
                        List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                        DocumentSnapshot latestDocument = documents.get(documents.size() - 1);

                        Double r2 = latestDocument.getDouble("R2_Score");
                        Double mae = latestDocument.getDouble("MAE");
                        Double rmse = latestDocument.getDouble("RMSE");

                        runOnUiThread(() -> {
                            modelR2.setText(String.format("R²: %.2f", r2 != null ? r2 : 0.0));
                            modelMAE.setText(String.format("MAE: %.2f", mae != null ? mae : 0.0));
                            modelRMSE.setText(String.format("RMSE: %.2f", mae != null ? mae : 0.0));
                            modelAccuracy.setText(String.format("Model Accuracy: %.2f%%", calculateModelAccuracy(rmse, mae)));
                        });
                    } else {
                        runOnUiThread(() -> {
                            modelR2.setText("R²: N/A");
                            modelMAE.setText("MAE: N/A");
                            modelAccuracy.setText("Model Accuracy: N/A");
                        });
                        Log.w("Firestore", "No model metrics documents found.");
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch model metrics.", Toast.LENGTH_SHORT).show();
                    });
                    Log.e("Firestore", "Error fetching model metrics", e);
                });
    }

    /**
     * Calculates model accuracy based on RMSE and MAE.
     * Modify this method based on how you define accuracy.
     * For demonstration, let's assume accuracy is inversely related to RMSE and MAE.
     */
    private double calculateModelAccuracy(double rmse, double mae) {
        // Example calculation (modify as per your actual accuracy computation)
        if (rmse == 0 && mae == 0) return 100.0;
        double accuracy = 100.0 - ((rmse + mae) / 2);
        return Math.max(accuracy, 0.0); // Ensure accuracy isn't negative
    }

    /**
     * Fetches prediction data from Firestore and calculates model accuracy.
     */
    /**
     * Fetches model accuracy from the 'predictions/GOOG/metrics/latest' document in Firestore and displays it.
     */
    private void fetchModelAccuracy() {
        // Reference to the 'predictions/GOOG/metrics/latest' document
        DocumentReference metricsRef = db.collection("predictions")
                .document("GOOG");

        metricsRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double correct = documentSnapshot.getDouble("correct_predictions");
                        Double total = documentSnapshot.getDouble("total_predictions");

                        double accuracy;
                        if (correct != null && total != null && total > 0) {
                            accuracy = (correct / total) * 100;
                        } else {
                            accuracy = 0.0;
                        }

                        runOnUiThread(() -> {
                            modelAccuracy.setText(String.format("Model Accuracy: %.2f%%", accuracy));
                        });
                    } else {
                        runOnUiThread(() -> {
                            modelAccuracy.setText("Model Accuracy: N/A");
                        });
                        Log.w("Firestore", "'predictions/GOOG/metrics/latest' document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to fetch model accuracy.", Toast.LENGTH_SHORT).show();
                    });
                    Log.e("Firestore", "Error fetching model accuracy from 'predictions/GOOG/metrics/latest'", e);
                });
    }


}
