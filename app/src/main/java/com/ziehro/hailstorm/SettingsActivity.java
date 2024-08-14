package com.ziehro.hailstorm;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.Collections;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private EditText frequencyInput;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        frequencyInput = findViewById(R.id.frequencyInput);
        Button updateFrequencyButton = findViewById(R.id.updateFrequencyButton);
        Button uploadModelButton = findViewById(R.id.resetControlButton);
        progressBar = findViewById(R.id.progressBar);

        updateFrequencyButton.setOnClickListener(v -> updateFrequency());
        uploadModelButton.setOnClickListener(v -> uploadModelToCloud());
    }

    private void updateFrequency() {
        String frequency = frequencyInput.getText().toString();
        progressBar.setVisibility(View.VISIBLE);

        db.collection("config").document("updateFrequency")
                .set(Collections.singletonMap("frequency", frequency))
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG", "Frequency updated successfully");
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.w("TAG", "Error updating frequency", e);
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void uploadModelToCloud() {
        progressBar.setVisibility(View.VISIBLE);

        // Logic to upload the model to Google Cloud Storage
        new Thread(() -> {
            try {
                // Assuming we have saved the models locally in the device's storage
                String[] tickers = {"AAPL", "MSFT", "GOOG", "AMZN", "META"};
                for (String ticker : tickers) {
                    String localModelPath = getExternalFilesDir(null) + "/" + ticker + "_model.h5";
                    File file = new File(localModelPath);
                    if (file.exists()) {
                        // Upload the model file to GCS
                        saveModelToGCS(localModelPath, "your-bucket-name", "models/" + ticker + "_model.h5");
                    }
                }
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Models uploaded successfully", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Error uploading models", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveModelToGCS(String localModelPath, String bucketName, String modelPath) {
        /*try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucketName, modelPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            storage.create(blobInfo, Files.readAllBytes(Paths.get(localModelPath)));
            Log.d("TAG", "Model uploaded to GCS: " + modelPath);
        } catch (Exception e) {
            Log.e("TAG", "Error uploading model to GCS", e);
        }*/
    }
}
