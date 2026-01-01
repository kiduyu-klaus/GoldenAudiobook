package com.example.goldenaudiobook.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.goldenaudiobook.R;
import com.example.goldenaudiobook.model.NavItem;

import java.util.List;

/**
 * Adapter for navigation drawer items
 */
public class NavDrawerAdapter extends ArrayAdapter<NavItem> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    public NavDrawerAdapter(Context context, List<NavItem> items) {
        super(context, 0, items);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        NavItem item = getItem(position);
        return item.isHeader() ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        NavItem item = getItem(position);

        if (convertView == null) {
            if (item.isHeader()) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_nav_header, parent, false);
            } else {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.item_nav, parent, false);
            }
        }

        if (item.isHeader()) {
            TextView headerText = convertView.findViewById(R.id.nav_header_text);
            headerText.setText(item.getTitle());
        } else {
            TextView navText = convertView.findViewById(R.id.nav_item_text);
            navText.setText(item.getTitle());

            // Show sub-item indicator if has sub-items
            View indicator = convertView.findViewById(R.id.submenu_indicator);
            if (item.hasSubItems()) {
                indicator.setVisibility(View.VISIBLE);
            } else {
                indicator.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}
