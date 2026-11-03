package com.paytondev.launchpad;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class TileAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final Favorites favorites;
    private final List<AppCatalog.Entry> items = new ArrayList<>();
    private int knobPosition = -1;
    private FocusUi ui;

    TileAdapter(LayoutInflater inflater, Favorites favorites) {
        this.inflater = inflater;
        this.favorites = favorites;
    }

    void setItems(List<AppCatalog.Entry> entries) {
        items.clear();
        items.addAll(entries);
        notifyDataSetChanged();
    }

    /** Tiles are drawn from the palette, so they follow day and night. */
    void setUi(FocusUi value) {
        if (value == ui) return;
        ui = value;
        notifyDataSetChanged();
    }

    /** Position highlighted for TUNE-knob navigation, or -1. */
    void setKnobPosition(int position) {
        if (position == knobPosition) return;
        knobPosition = position;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public AppCatalog.Entry getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }
    @Override public boolean areAllItemsEnabled() { return false; }
    @Override public boolean isEnabled(int position) { return items.get(position).launchable; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView : inflater.inflate(R.layout.tile, parent, false);
        AppCatalog.Entry e = items.get(position);
        ((ImageView) v.findViewById(R.id.icon)).setImageDrawable(e.icon);
        TextView label = (TextView) v.findViewById(R.id.label);
        label.setText(e.label);
        if (ui != null) {
            label.setTextColor(ui.p.ink); label.setTypeface(Fonts.get(ui.context, Fonts.Weight.REGULAR));
            ui.background(v, ui.secondary(16));
            ui.background(v.findViewById(R.id.badge), ui.rounded(ui.p.accent, 6, 0));
        }
        v.findViewById(R.id.badge).setVisibility(favorites.contains(e.key) ? View.VISIBLE : View.GONE);
        v.setActivated(position == knobPosition);
        v.setEnabled(e.launchable);
        v.setAlpha(e.launchable ? 1f : 0.45f);
        v.setContentDescription(e.launchable ? e.label : e.label + ". Unavailable");
        return v;
    }
}
