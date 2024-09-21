package com.usmanmbup.diallaleh;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.arthenica.mobileffmpeg.ExecuteCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 100;
    private static final String PREFS_KEY_CONVERTED_FILES = "converted_files";

    private List<File> convertedFiles = new ArrayList<>();
    private ConvertedFilesAdapter filesAdapter;
    private DrawerLayout drawerLayout;
    private RecyclerView convertedFilesRecyclerView;

    private TextView filePathTextView;
    private Button pickFileButton;
    private Button convertButton;
    private Button cancelButton;
    private Button openFileButton;
    private ProgressBar conversionProgressBar;

    private String selectedFilePath;
    private String outputFilePath;
    private long currentExecutionId;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        convertedFilesRecyclerView = findViewById(R.id.convertedFilesRecyclerView);
        convertedFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        filesAdapter = new ConvertedFilesAdapter(convertedFiles, this);
        convertedFilesRecyclerView.setAdapter(filesAdapter);

        filePathTextView = findViewById(R.id.filePathTextView);
        pickFileButton = findViewById(R.id.pickFileButton);
        convertButton = findViewById(R.id.convertButton);
        cancelButton = findViewById(R.id.cancelButton);
        openFileButton = findViewById(R.id.openFileButton);
        conversionProgressBar = findViewById(R.id.conversionProgressBar);

        pickFileButton.setOnClickListener(v -> openFilePicker());
        convertButton.setOnClickListener(v -> convertToMp3());
        cancelButton.setOnClickListener(v -> cancelConversion());
        openFileButton.setOnClickListener(v -> openConvertedFile());

        checkAndRequestPermissions();
        loadConvertedFiles(); // Load converted files from SharedPreferences
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadConvertedFiles();
            } else {
                Toast.makeText(this, "Storage permission is required to load files", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(Intent.createChooser(intent, "Choose M4A File"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri selectedFileUri = data.getData();
                selectedFilePath = FileUtils.getPath(this, selectedFileUri); // Assuming you have a FileUtils class for getting the path
                filePathTextView.setText(selectedFilePath);
                convertButton.setEnabled(true);
            }
        }
    }

    private void convertToMp3() {
        if (selectedFilePath == null) {
            Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC); // App's private music directory
        if (outputDir != null && !outputDir.exists()) {
            outputDir.mkdirs(); // Create directory if it doesn't exist
        }

        outputFilePath = new File(outputDir, new File(selectedFilePath).getName().replace(".m4a", ".mp3")).getPath();

        conversionProgressBar.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);
        openFileButton.setVisibility(View.GONE);
        convertButton.setEnabled(false);

        String[] cmd = {"-i", selectedFilePath, outputFilePath};

        currentExecutionId = FFmpeg.executeAsync(cmd, new ExecuteCallback() {
            @Override
            public void apply(long executionId, int returnCode) {
                conversionProgressBar.setVisibility(View.GONE);
                cancelButton.setVisibility(View.GONE);
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    Toast.makeText(MainActivity.this, "Conversion completed", Toast.LENGTH_LONG).show();
                    openFileButton.setVisibility(View.VISIBLE);
                    saveConvertedFile(outputFilePath); // Save the converted file path
                    loadConvertedFiles(); // Refresh the list
                } else {
                    Toast.makeText(MainActivity.this, "Conversion failed", Toast.LENGTH_SHORT).show();
                    convertButton.setEnabled(true);
                }
            }
        });
    }

    private void saveConvertedFile(String filePath) {
        String existingFiles = sharedPreferences.getString(PREFS_KEY_CONVERTED_FILES, "");
        if (!existingFiles.contains(filePath)) {
            existingFiles += filePath + ",";
            sharedPreferences.edit().putString(PREFS_KEY_CONVERTED_FILES, existingFiles).apply();
        }
    }

    private void loadConvertedFiles() {
        convertedFiles.clear();
        String filePaths = sharedPreferences.getString(PREFS_KEY_CONVERTED_FILES, "");
        if (!filePaths.isEmpty()) {
            String[] files = filePaths.split(",");
            for (String path : files) {
                if (!path.isEmpty()) {
                    convertedFiles.add(new File(path));
                }
            }
            filesAdapter.notifyDataSetChanged();
        }
    }

    private void cancelConversion() {
        if (currentExecutionId != -1) {
            FFmpeg.cancel(currentExecutionId);
            conversionProgressBar.setVisibility(View.GONE);
            cancelButton.setVisibility(View.GONE);
            convertButton.setEnabled(true);
            Toast.makeText(this, "Conversion canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private void openConvertedFile() {
        File outputFile = new File(outputFilePath);
        if (outputFile.exists()) {
            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", outputFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "audio/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } else {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show();
        }
    }
}