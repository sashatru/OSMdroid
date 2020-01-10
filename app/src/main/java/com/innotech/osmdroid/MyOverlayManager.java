package com.innotech.osmdroid;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.DefaultOverlayManager;
import org.osmdroid.views.overlay.TilesOverlay;

public class MyOverlayManager extends DefaultOverlayManager {
    private final static String TAG = "MyOverlayManager";

    /**
     * Create MyOverlayManager
     */
    public static MyOverlayManager create(MapView mapView, Context context) {
        MapTileProviderBase mTileProvider = mapView.getTileProvider();
        TilesOverlay tilesOverlay = new TilesOverlay(mTileProvider, context);
        mapView.getTileProvider();
        mapView.setOverlayManager(new MyOverlayManager(tilesOverlay));
        return new MyOverlayManager(tilesOverlay);
    }

    public MyOverlayManager(final TilesOverlay tilesOverlay) {
        super(tilesOverlay);
    }


    @Override
    public boolean onDoubleTap(MotionEvent e, MapView pMapView) {
        Log.d(TAG, "onDoubleTap "+e);
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e, MapView pMapView) {
        //Log.d(TAG, "onDoubleTapEvent "+e);
        //if (e.getAction() == MotionEvent.ACTION_UP) {MainActivity.zoomInMap();}
        return false;
    }

}
