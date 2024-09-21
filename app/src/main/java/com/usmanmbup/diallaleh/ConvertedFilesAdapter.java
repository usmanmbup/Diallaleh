package com.usmanmbup.diallaleh;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class ConvertedFilesAdapter extends RecyclerView.Adapter<ConvertedFilesAdapter.ViewHolder> {

    private List<File> convertedFiles;
    private Context context;

    public ConvertedFilesAdapter(List<File> convertedFiles, Context context) {
        this.convertedFiles = convertedFiles;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File file = convertedFiles.get(position);
        holder.fileNameTextView.setText(file.getName());

        holder.itemView.setOnClickListener(v -> {
            Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileUri, "audio/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return convertedFiles.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(android.R.id.text1);
        }
    }
}
