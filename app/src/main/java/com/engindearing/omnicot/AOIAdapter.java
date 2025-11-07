package com.engindearing.omnicot;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.List;

public class AOIAdapter extends RecyclerView.Adapter<AOIAdapter.AOIViewHolder> {

    private static final String TAG = AOIAdapter.class.getSimpleName();

    private final Context context;
    private final MapView mapView;
    private List<AOIItem> aoiItems;

    public AOIAdapter(Context context, MapView mapView, List<AOIItem> aoiItems) {
        this.context = context;
        this.mapView = mapView;
        this.aoiItems = aoiItems;
    }

    public void updateData(List<AOIItem> newItems) {
        this.aoiItems = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AOIViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.aoi_list_item, parent, false);
        return new AOIViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AOIViewHolder holder, int position) {
        AOIItem item = aoiItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return aoiItems.size();
    }

    public class AOIViewHolder extends RecyclerView.ViewHolder {
        private TextView aoiName;
        private TextView aoiType;
        private TextView aoiAlertStatus;
        private Button btnZoomTo;
        private Button btnConfigureAlert;

        public AOIViewHolder(@NonNull View itemView) {
            super(itemView);
            aoiName = itemView.findViewById(R.id.aoiName);
            aoiType = itemView.findViewById(R.id.aoiType);
            aoiAlertStatus = itemView.findViewById(R.id.aoiAlertStatus);
            btnZoomTo = itemView.findViewById(R.id.btnZoomTo);
            btnConfigureAlert = itemView.findViewById(R.id.btnConfigureAlert);
        }

        public void bind(final AOIItem item) {
            aoiName.setText(item.getName());
            aoiType.setText(item.getType());
            aoiAlertStatus.setText(item.getAlertStatus());

            btnZoomTo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticFeedbackHelper.performLightClick(v);
                    zoomToAOI(item);
                }
            });

            btnConfigureAlert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HapticFeedbackHelper.performMediumClick(v);
                    configureAlert(item);
                }
            });
        }

        private void zoomToAOI(AOIItem item) {
            try {
                GeoBounds bounds = item.getShape().getBounds(null);
                if (bounds != null) {
                    // Calculate center point
                    GeoPoint center = bounds.getCenter(null);

                    // Just pan to the center without zooming
                    mapView.getMapController().panTo(center, true);

                    Log.d(TAG, "Panned to AOI: " + item.getName() + " at " + center);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error panning to AOI", e);
            }
        }

        private void configureAlert(AOIItem item) {
            // Show alert configuration dialog
            AlertConfigDialog dialog = new AlertConfigDialog(context, mapView, item);
            dialog.show();
            Log.d(TAG, "Opened alert config for: " + item.getName());
        }
    }
}
