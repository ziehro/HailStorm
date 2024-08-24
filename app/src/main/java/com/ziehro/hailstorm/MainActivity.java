package com.ziehro.hailstorm;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText frequencyInput;
    private ProgressBar progressBar;
    private TextView portfolio0TotalValue;
    private TextView portfolio1TotalValue, portfolio2TotalValue, portfolio3TotalValue, portfolio4TotalValue, portfolio5TotalValue, portfolio6TotalValue, controlPortfolioTotalValue, portfolioSuperTotalValue;
    private Button updateFrequencyButton, resetControlPortfolio, fetchPortfolioButton, openPortfolioActivityButton, openPortfolioAllButton;
    private TextView portfolio0Accuracy;
    private TextView portfolio1Accuracy, portfolio2Accuracy, portfolio3Accuracy, portfolio4Accuracy, portfolio5Accuracy, portfolio6Accuracy, portfolioSuperAccuracy;


    private double initialCapital = 10000.0;
    private double controlTotalValue = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
        fetchAllPortfoliosTotalValue();
    }

    private void initViews() {
        db = FirebaseFirestore.getInstance();

        frequencyInput = findViewById(R.id.frequencyInput);
        progressBar = findViewById(R.id.progressBar);
        portfolio0TotalValue = findViewById(R.id.portfolio0TotalValue);
        portfolio1TotalValue = findViewById(R.id.portfolio1TotalValue);
        portfolio2TotalValue = findViewById(R.id.portfolio2TotalValue);
        portfolio3TotalValue = findViewById(R.id.portfolio3TotalValue);
        portfolio4TotalValue = findViewById(R.id.portfolio4TotalValue);
        portfolio5TotalValue = findViewById(R.id.portfolio5TotalValue);
        portfolio6TotalValue = findViewById(R.id.portfolio6TotalValue);
        portfolioSuperTotalValue = findViewById(R.id.portfolioSuperTotalValue);
        controlPortfolioTotalValue = findViewById(R.id.controlPortfolioTotalValue);

        portfolio0Accuracy = findViewById(R.id.portfolio0Accuracy);
        portfolio1Accuracy = findViewById(R.id.portfolio1Accuracy);
        portfolio2Accuracy = findViewById(R.id.portfolio2Accuracy);
        portfolio3Accuracy = findViewById(R.id.portfolio3Accuracy);
        portfolio4Accuracy = findViewById(R.id.portfolio4Accuracy);
        portfolio5Accuracy = findViewById(R.id.portfolio5Accuracy);
        portfolio6Accuracy = findViewById(R.id.portfolio6Accuracy);
        portfolioSuperAccuracy = findViewById(R.id.portfolioSuperAccuracy);

        updateFrequencyButton = findViewById(R.id.updateFrequencyButton);
        resetControlPortfolio = findViewById(R.id.resetControlButton);
        fetchPortfolioButton = findViewById(R.id.fetchPortfolioButton);
        openPortfolioActivityButton = findViewById(R.id.openPortfolioActivityButton);
        openPortfolioAllButton = findViewById(R.id.openPortfolioAllActivityButton);
    }

    private void initListeners() {
        updateFrequencyButton.setOnClickListener(v -> updateFrequency());
        resetControlPortfolio.setOnClickListener(v -> createControlPortfolio(getCurrentFocus()));
        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());
        openPortfolioActivityButton.setOnClickListener(v -> openPortfolioActivity());
        openPortfolioAllButton.setOnClickListener(v -> openPortfolioAllActivity());

        // Add confirmation dialogs for the reset buttons
        findViewById(R.id.resetPortfolioButton).setOnClickListener(v -> showConfirmationDialog("Are you sure you want to reset the portfolio?", () -> resetPortfolio(v)));
        findViewById(R.id.resetAllPortfoliosButton).setOnClickListener(v -> showConfirmationDialog("Are you sure you want to reset all 6 portfolios?", () -> resetAllPortfolios(v)));
        findViewById(R.id.resetControlButton).setOnClickListener(v -> showConfirmationDialog("Are you sure you want to reset the control portfolio?", () -> createControlPortfolio(v)));
    }

    private void showConfirmationDialog(String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> onConfirm.run())
                .setNegativeButton("No", null)
                .show();
    }

    private void fetchAllPortfoliosTotalValue() {
        String[] portfolioIds = {"portfolio1", "portfolio2", "portfolio3", "portfolio4", "portfolio5", "portfolio6", "portfolio", "super_portfolio"};
        Map<String, Double> portfolioValues = new HashMap<>();

        for (String portfolioId : portfolioIds) {
            db.collection(portfolioId).document("latest")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        double totalValue = calculateTotalValue(documentSnapshot);
                        portfolioValues.put(portfolioId, totalValue);

                        // Once all portfolios are processed, apply the color gradient
                        if (portfolioValues.size() == portfolioIds.length) {
                            applyColorGradient(portfolioValues);
                            fetchAllPortfoliosAccuracy(); // Fetch and display accuracy data
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w("TAG", "Error fetching portfolio data for " + portfolioId, e);
                        Toast.makeText(MainActivity.this, "Error fetching portfolio data", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchAllPortfoliosAccuracy() {
        String[] portfolioIds = {"portfolio1", "portfolio2", "portfolio3", "portfolio4", "portfolio5", "portfolio6", "portfolio0", "super_portfolio"};

        for (String portfolioId : portfolioIds) {
            db.collection("cumulative_accuracies").document(portfolioId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Double accuracy = documentSnapshot.getDouble("accuracy");
                            String accuracyText = String.format("%.2f%%", (accuracy != null) ? accuracy : 0.0);

                            switch (portfolioId) {
                                case "portfolio1":
                                    portfolio1Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio2":
                                    portfolio2Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio3":
                                    portfolio3Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio4":
                                    portfolio4Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio5":
                                    portfolio5Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio6":
                                    portfolio6Accuracy.setText(accuracyText);
                                    break;
                                case "portfolio0":
                                    portfolio0Accuracy.setText(accuracyText);
                                    break;
                                case "super_portfolio":
                                    portfolioSuperAccuracy.setText(accuracyText);
                                    break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.w("TAG", "Error fetching cumulative accuracy data for " + portfolioId, e);
                        Toast.makeText(MainActivity.this, "Error fetching cumulative accuracy data", Toast.LENGTH_SHORT).show();
                    });
        }
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
        double maxValue = Collections.max(portfolioValues.values());
        double minValue = Collections.min(portfolioValues.values());

        for (Map.Entry<String, Double> entry : portfolioValues.entrySet()) {
            String portfolioId = entry.getKey();
            double value = entry.getValue();

            int color;
            if (value < controlTotalValue) {
                // Set color to red if the portfolio value is below $10,000
                color = Color.rgb(255, 0, 0);
            } else {
                // Calculate color intensity based on the portfolio's value relative to the min/max
                float intensity = (float) ((value - minValue) / (maxValue - minValue));
                int green = (int) (255 * intensity);
                color = Color.rgb(0, green, 0);
            }

            // Set the background color of the corresponding box
            View colorBox = findViewById(getColorBoxId(portfolioId));
            if (colorBox != null) {
                colorBox.setBackgroundColor(color);
            }

            // Update the portfolio's total value text
            String displayPortfolioId = portfolioId.equals("super_portfolio") ? "S" : portfolioId.replace("portfolio", "");
            String totalValueText = String.format("Portfolio %s Total Value: $%.2f", displayPortfolioId, value);

            TextView portfolioValueTextView = findViewById(getPortfolioTextViewId(portfolioId));
            if (portfolioValueTextView != null) {
                portfolioValueTextView.setText(totalValueText);
            }
        }
    }


    private int getColorBoxId(String portfolioId) {
        switch (portfolioId) {
            case "portfolio1":
                return R.id.portfolio1ColorBox;
            case "portfolio2":
                return R.id.portfolio2ColorBox;
            case "portfolio3":
                return R.id.portfolio3ColorBox;
            case "portfolio4":
                return R.id.portfolio4ColorBox;
            case "portfolio5":
                return R.id.portfolio5ColorBox;
            case "portfolio6":
                return R.id.portfolio6ColorBox;
            case "portfolio":
                return R.id.portfolio0ColorBox;
            case "super_portfolio":
                return R.id.portfolioSuperColorBox;
            default:
                return 0;
        }
    }

    private int getPortfolioTextViewId(String portfolioId) {
        switch (portfolioId) {
            case "portfolio1":
                return R.id.portfolio1TotalValue;
            case "portfolio2":
                return R.id.portfolio2TotalValue;
            case "portfolio3":
                return R.id.portfolio3TotalValue;
            case "portfolio4":
                return R.id.portfolio4TotalValue;
            case "portfolio5":
                return R.id.portfolio5TotalValue;
            case "portfolio6":
                return R.id.portfolio6TotalValue;
            case "portfolio":
                return R.id.portfolio0TotalValue;
            case "super_portfolio":
                return R.id.portfolioSuperTotalValue;
            default:
                return 0;
        }
    }


    private void updatePortfolioTotalValue(String portfolioId, DocumentSnapshot documentSnapshot) {
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

            String totalValueText = String.format("Portfolio %s Total Value: $%.2f", portfolioId.replace("portfolio", ""), totalValue);
            updatePortfolioTextView(portfolioId, totalValueText);
        } else {
            Log.d("TAG", "No data found for " + portfolioId);
        }
    }

    private void updatePortfolioTextView(String portfolioId, String totalValueText) {
        switch (portfolioId) {
            case "portfolio1":
                portfolio1TotalValue.setText(totalValueText);
                break;
            case "portfolio2":
                portfolio2TotalValue.setText(totalValueText);
                break;
            case "portfolio3":
                portfolio3TotalValue.setText(totalValueText);
                break;
            case "portfolio4":
                portfolio4TotalValue.setText(totalValueText);
                break;
            case "portfolio5":
                portfolio5TotalValue.setText(totalValueText);
                break;
            case "portfolio6":
                portfolio6TotalValue.setText(totalValueText);
                break;
            case "super_portfolio":
                portfolioSuperTotalValue.setText(totalValueText);
                break;
            case "Control":
                controlPortfolioTotalValue.setText(totalValueText);
                break;
        }
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
        fetchAllPortfoliosTotalValue();  // This will fetch and update all portfolios' total values
        updateControlPortfolioValue(getCurrentFocus());  // Update control portfolio value

        db.collection("portfolio").document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayTotalPortfolioValue(documentSnapshot);
                    } else {
                        Log.d("TAG", "No portfolio data found");
                        portfolio0TotalValue.setText("No portfolio data found");
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error fetching portfolio", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void displayTotalPortfolioValue(DocumentSnapshot documentSnapshot) {
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

            String totalValueText = String.format("Portfolio 0 Total Value: $%.2f", totalValue);
            portfolio0TotalValue.setText(totalValueText);
        } else {
            Log.d("TAG", "No data found for portfolio");
        }
    }


    public void resetPortfolio(View view) {
        Map<String, Object> defaultPortfolio = new HashMap<>();
        defaultPortfolio.put("capital", initialCapital);
        defaultPortfolio.put("holdings", new HashMap<>());

        String portfolioId = "portfolio0"; // Replace with the actual portfolio ID

        db.collection("portfolio").document("latest")
                .set(defaultPortfolio)
                .addOnSuccessListener(aVoid -> {
                    // Reset accuracies for this portfolio
                    resetAccuracies(portfolioId);
                    Toast.makeText(MainActivity.this, "Portfolio reset successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error resetting portfolio", Toast.LENGTH_SHORT).show());

        fetchPortfolio();
    }


    public void resetAllPortfolios(View view) {
        Map<String, Object> defaultPortfolio = new HashMap<>();
        defaultPortfolio.put("capital", initialCapital);
        defaultPortfolio.put("holdings", new HashMap<>());

        String[] portfolios = {"portfolio1", "portfolio2", "portfolio3", "portfolio4", "portfolio5", "portfolio6", "super_portfolio"};

        for (String portfolio : portfolios) {
            db.collection(portfolio).document("latest")
                    .set(defaultPortfolio)
                    .addOnSuccessListener(aVoid -> {
                        // Reset accuracies for this portfolio
                        resetAccuracies(portfolio);
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error resetting " + portfolio, Toast.LENGTH_SHORT).show());
        }

        fetchPortfolio();
    }

    private void resetAccuracies(String portfolioId) {
        // Reset all documents in the 'tickers' subcollection under the portfolio's 'accuracies' document to zero accuracy
        db.collection("accuracies").document(portfolioId).collection("tickers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> resetAccuracy = new HashMap<>();
                            resetAccuracy.put("accuracy", 0);
                            resetAccuracy.put("timestamp", LocalDateTime.now().toString()); // Update the timestamp

                            db.collection("accuracies").document(portfolioId).collection("tickers").document(document.getId())
                                    .set(resetAccuracy)
                                    .addOnSuccessListener(aVoid -> Log.d("ResetAccuracies", "Successfully reset accuracy document: " + document.getId()))
                                    .addOnFailureListener(e -> Log.w("ResetAccuracies", "Error resetting accuracy document: " + document.getId(), e));
                        }
                    } else {
                        Log.w("ResetAccuracies", "Error getting accuracy documents: ", task.getException());
                    }
                });

        // Optionally, reset cumulative accuracies as well
        Map<String, Object> resetCumulativeAccuracy = new HashMap<>();
        resetCumulativeAccuracy.put("correct_predictions", 0);
        resetCumulativeAccuracy.put("total_predictions", 0);
        resetCumulativeAccuracy.put("accuracy", 0);

        db.collection("cumulative_accuracies").document(portfolioId)
                .set(resetCumulativeAccuracy)
                .addOnSuccessListener(aVoid -> Log.d("ResetAccuracies", "Successfully reset cumulative accuracy for: " + portfolioId))
                .addOnFailureListener(e -> Log.w("ResetAccuracies", "Error resetting cumulative accuracy for: " + portfolioId, e));
    }



    public void createControlPortfolio(View view) {
        db.collection("portfolio1").document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> lastClose = (Map<String, Object>) documentSnapshot.get("lastClose");
                        if (lastClose != null) {
                            setupControlPortfolio(lastClose);
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

    private void setupControlPortfolio(Map<String, Object> lastClose) {
        String[] tickers = {"AAPL", "MSFT", "GOOG", "AMZN", "META"};

        double investmentPerStock = initialCapital / tickers.length;

        Map<String, Object> controlPortfolio = new HashMap<>();
        controlPortfolio.put("capital", initialCapital);

        Map<String, Object> holdings = new HashMap<>();

        for (String ticker : tickers) {
            Double purchasePrice = (lastClose.get(ticker) instanceof Number) ? ((Number) lastClose.get(ticker)).doubleValue() : 0.0;
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

    public void updateControlPortfolioValue(View view) {
        db.collection("portfolio1").document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> lastClose = (Map<String, Object>) documentSnapshot.get("lastClose");
                        if (lastClose != null) {
                            updateControlPortfolio(lastClose);
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

    }

    private void updateControlPortfolio(Map<String, Object> lastClose) {
        db.collection("Control").document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> controlPortfolio = documentSnapshot.getData();
                        if (controlPortfolio != null) {
                            Map<String, Object> holdings = (Map<String, Object>) controlPortfolio.get("holdings");

                            if (holdings != null) {
                                double totalValue = 0.0;
                                for (String ticker : holdings.keySet()) {
                                    Map<String, Object> stockInfo = (Map<String, Object>) holdings.get(ticker);
                                    Double shares = (stockInfo.get("shares") instanceof Number) ? ((Number) stockInfo.get("shares")).doubleValue() : 0.0;
                                    Double stockPrice = (lastClose.get(ticker) instanceof Number) ? ((Number) lastClose.get(ticker)).doubleValue() : 0.0;

                                    totalValue += shares * stockPrice;
                                    controlTotalValue = totalValue;
                                }

                                controlPortfolio.put("capital", totalValue);

                                db.collection("Control").document("latest")
                                        .set(controlPortfolio)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Control portfolio updated successfully", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(MainActivity.this, "Error updating control portfolio", Toast.LENGTH_SHORT).show();
                                            Log.e("TAG", "Error updating control portfolio", e);
                                        });
                                String totalValueText = String.format("Control Total Value: $%.2f", totalValue);
                                controlPortfolioTotalValue.setText(totalValueText);

                            } else {
                                Toast.makeText(MainActivity.this, "No holdings found in control portfolio", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Control portfolio does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Error fetching control portfolio", Toast.LENGTH_SHORT).show();
                    Log.e("TAG", "Error fetching control portfolio", e);
                });
    }
}
