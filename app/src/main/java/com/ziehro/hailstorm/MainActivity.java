package com.ziehro.hailstorm;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText frequencyInput;
    private ProgressBar progressBar;
    private TextView portfolioTextView;
    private Button updateFrequencyButton, uploadModelButton, fetchPortfolioButton, openPortfolioActivityButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();

        frequencyInput = findViewById(R.id.frequencyInput);
        progressBar = findViewById(R.id.progressBar);
        portfolioTextView = findViewById(R.id.portfolioTextView);
        updateFrequencyButton = findViewById(R.id.updateFrequencyButton);
        uploadModelButton = findViewById(R.id.uploadModelButton);
        fetchPortfolioButton = findViewById(R.id.fetchPortfolioButton);
        openPortfolioActivityButton = findViewById(R.id.openPortfolioActivityButton);  // Initialize the button


        updateFrequencyButton.setOnClickListener(v -> updateFrequency());
        uploadModelButton.setOnClickListener(v -> uploadModelToCloud());
        fetchPortfolioButton.setOnClickListener(v -> fetchPortfolio());
        openPortfolioActivityButton.setOnClickListener(v -> openPortfolioActivity());  // Set click listener

    }

    private void updateFrequency() {
        String frequency = frequencyInput.getText().toString();
        progressBar.setVisibility(View.VISIBLE);

        // Prepare the data to update
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("frequency", frequency);
        updateData.put("reschedule", "yes");  // Set the reschedule flag to "yes"

        db.collection("config").document("updateFrequency")
                .set(updateData)  // Update the document with new data
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

    private void uploadModelToCloud() {
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                // Assuming we have saved the models locally in the device's storage
                String[] tickers = {"AAPL", "MSFT", "GOOG", "AMZN", "META"};
                FirebaseStorage storage = FirebaseStorage.getInstance();
                for (String ticker : tickers) {
                    String localModelPath = getExternalFilesDir(null) + "/" + ticker + "_model.h5";
                    File file = new File(localModelPath);
                    if (file.exists()) {
                        // Upload the model file to GCS
                        StorageReference storageRef = storage.getReference().child("models/" + ticker + "_model.h5");
                        UploadTask uploadTask = storageRef.putFile(Uri.fromFile(file));
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            Log.d("TAG", "Model uploaded to GCS: " + ticker + "_model.h5");
                        }).addOnFailureListener(e -> {
                            Log.e("TAG", "Error uploading model to GCS: " + ticker + "_model.h5", e);
                        });
                    }
                }
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Models uploaded successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Error uploading models", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void fetchPortfolio() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("portfolio").document("latest")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        updateUI(data);
                    } else {
                        Log.d("TAG", "No portfolio data found");
                        portfolioTextView.setText("No portfolio data found");
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error fetching portfolio", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void updateUI(Map<String, Object> data) {
        StringBuilder portfolioText = new StringBuilder();
        portfolioText.append("Capital: $").append(data.get("capital")).append("\n");

        Map<String, Double> holdings = (Map<String, Double>) data.get("holdings");
        for (String stock : holdings.keySet()) {
            portfolioText.append(stock).append(": ").append(holdings.get(stock)).append(" shares\n");
        }
        portfolioTextView.setText(portfolioText.toString());
    }
}
