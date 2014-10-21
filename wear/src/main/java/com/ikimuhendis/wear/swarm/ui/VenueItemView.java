package com.ikimuhendis.wear.swarm.ui;


import android.content.Context;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ikimuhendis.wear.swarm.R;

public class VenueItemView extends LinearLayout implements WearableListView.Item {

    private CircledImageView imgView;
    private TextView txtView;
    private float mScale;
    private final int mFadedCircleColor;
    private final int mChosenCircleColor;
    private float mNormalCircleRadius;
    private float mSelectedCircleRadius;

    public VenueItemView(Context context) {
        super(context);
        mNormalCircleRadius = getResources().getDimension(R.dimen.normal_circle_radius);
        mSelectedCircleRadius = getResources().getDimension(R.dimen.selected_circle_radius);
        View.inflate(context, R.layout.list_item_venue, this);
        imgView = (CircledImageView) findViewById(R.id.img);
        txtView = (TextView) findViewById(R.id.txt);
        mFadedCircleColor = getResources().getColor(android.R.color.darker_gray);
        mChosenCircleColor = getResources().getColor(android.R.color.holo_blue_dark);
    }

    @Override
    public float getProximityMinValue() {
        return mNormalCircleRadius;
    }

    @Override
    public float getProximityMaxValue() {
        return mSelectedCircleRadius;
    }

    @Override
    public float getCurrentProximityValue() {
        return mScale;
    }

    @Override
    public void setScalingAnimatorValue(float value) {
        mScale = value;
        imgView.setCircleRadius(mScale);
        imgView.setCircleRadiusPressed(mScale);
    }

    @Override
    public void onScaleUpStart() {
        imgView.setAlpha(1f);
        txtView.setAlpha(1f);
        imgView.setCircleColor(mChosenCircleColor);
    }

    @Override
    public void onScaleDownStart() {
        imgView.setAlpha(0.5f);
        txtView.setAlpha(0.5f);
        imgView.setCircleColor(mFadedCircleColor);
    }
}
