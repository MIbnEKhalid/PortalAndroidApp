package com.example.portalmbktechstudio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class WebsiteAdapter extends ArrayAdapter<WebsiteItem> {
    private int selectedPosition = -1;
    private boolean isStartupSelection = false;
    private OnWebsiteClickListener clickListener;

    public interface OnWebsiteClickListener {
        void onWebsiteClick(WebsiteItem website);
    }

    public WebsiteAdapter(@NonNull Context context, ArrayList<WebsiteItem> websites) {
        super(context, 0, websites);
    }

    public void setOnWebsiteClickListener(OnWebsiteClickListener listener) {
        this.clickListener = listener;
    }

    public void setStartupSelection(boolean isStartupSelection) {
        this.isStartupSelection = isStartupSelection;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        WebsiteItem website = getItem(position);
        
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_website, parent, false);
        }

        TextView titleTextView = convertView.findViewById(R.id.titleTextView);
        TextView urlTextView = convertView.findViewById(R.id.urlTextView);
        CheckBox checkBox = convertView.findViewById(R.id.checkBox);
        RadioButton radioButton = convertView.findViewById(R.id.radioButton);

        titleTextView.setText(website.getTitle());
        urlTextView.setText(website.getUrl());

        // Add click listener to the website title and URL
        View.OnClickListener websiteClickListener = v -> {
            if (clickListener != null && !isStartupSelection) {
                clickListener.onWebsiteClick(website);
            }
        };
        titleTextView.setOnClickListener(websiteClickListener);
        urlTextView.setOnClickListener(websiteClickListener);

        if (isStartupSelection) {
            checkBox.setVisibility(View.GONE);
            radioButton.setVisibility(View.VISIBLE);
            radioButton.setChecked(position == selectedPosition);
            radioButton.setOnClickListener(v -> {
                selectedPosition = position;
                notifyDataSetChanged();
            });
        } else {
            checkBox.setVisibility(View.VISIBLE);
            radioButton.setVisibility(View.GONE);
            checkBox.setChecked(website.isSelected());
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                website.setSelected(isChecked);
            });
        }

        return convertView;
    }
}
