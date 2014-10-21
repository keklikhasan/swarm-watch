package com.ikimuhendis.wear.swarm.adapters;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ikimuhendis.wear.swarm.R;
import com.ikimuhendis.wear.swarm.models.Venue;
import com.ikimuhendis.wear.swarm.ui.VenueItemView;

import java.util.List;

public class VenuesAdapter extends WearableListView.Adapter {


    private final List<Venue> mVenuesList;

    public VenuesAdapter(List<Venue> venueList) {
        this.mVenuesList = venueList;
    }

    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new WearableListView.ViewHolder(new VenueItemView(viewGroup.getContext()));
    }

    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        VenueItemView itemView = (VenueItemView) holder.itemView;

        Venue venue = mVenuesList.get(position);

        TextView txtView = (TextView) itemView.findViewById(R.id.txt);
        txtView.setText(venue.name);

        Bitmap bitmap = venue.bitmap;
        if (bitmap != null) {
            CircledImageView imgView = (CircledImageView) itemView.findViewById(R.id.img);
            Resources resources = itemView.getContext().getResources();
            imgView.setImageDrawable(new BitmapDrawable(resources, bitmap));
        }
        itemView.setTag(venue.id);
    }

    @Override
    public int getItemCount() {
        return mVenuesList.size();
    }

    public List<Venue> getVenuesList() {
        return mVenuesList;
    }
}