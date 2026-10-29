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

    /** Dark mode draws tiles from the palette; light keeps the XML drawables. */
    void setUi(FocusUi value) {
        if (value == ui) return;
        ui = value;
        notifyDataSetChanged();
    }

    /** Position outlined for TUNE-knob navigation, or -1. */
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
        if (ui != null && ui.p.dark) {
            label.setTextColor(ui.p.ink); ui.background(v, ui.control(ui.p.surface, 18));
        } else {
            label.setTextColor(v.getResources().getColor(R.color.text)); v.setBackgroundResource(R.drawable.tile_bg);
        }
        v.findViewById(R.id.badge).setVisibility(favorites.contains(e.key) ? View.VISIBLE : View.GONE);
        v.setActivated(position == knobPosition);
        v.setEnabled(e.launchable);
        v.setAlpha(e.launchable ? 1f : 0.45f);
        v.setContentDescription(e.launchable ? e.label : e.label + ". Unavailable");
        return v;
    }
}
