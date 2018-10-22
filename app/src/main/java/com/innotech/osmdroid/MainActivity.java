package com.innotech.osmdroid;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.LocationUtils;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MapEventsReceiver {
    private final static String TAG = "OSMdroid";

    private int STORAGE_PERMISSION_CODE = 0;
    private int LOCATION_PERMISSION_CODE = 1;
    Context ctx;
    MapView mMap;
    OnlineTileSourceBase MY_ELIT_MAP, MAPNIK;
    public static IMapController mapController;
    Marker myMarker, taxiMarker;
    Polyline pathOverlay;
    TextView textViewCurrentLocation;
    protected ImageButton btCenterMap;

    private ArrayList<OverlayItem> mItemList;
    OverlayItem startOverlayItem, finishOvelayItem;

    private Drawable mMarkerIcon = null, mTaxiIcon = null, pointA = null, pointB = null;

    private float ys, y;
    private boolean zoomMode;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MY_ELIT_MAP = new XYTileSource("Elitmap", 0, 18, 256, ".png", new String[]{"http://185.151.245.60/"}, "© ElitMap");
        MAPNIK = new XYTileSource("https://tiles.wmflabs.org/bw-mapnik/", 0,
                18, 256, ".png", new String[]{"https://tiles.wmflabs.org/bw-mapnik/"}, "https://tiles.wmflabs.org/bw-mapnik/");
        //Permision code that will be checked in the method onRequestPermissionsResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // request permissions and handle the result in onRequestPermissionsResult()
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            } else {
                initMap();
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
            }
        }
    }

    private void initMap() {
        // !!! it's highly important for Android>M to call setContentView after Permission READ_EXTERNAL_STORAGE is granted
        setContentView(R.layout.activity_main);
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        // !!! it's highly important for Android>M make mapView programmatically
        LinearLayout contentLayout = (LinearLayout) findViewById(R.id.contentLayout);
        mMap = new MapView(ctx);
        //set custom mMap tile source
        mMap.setTileSource(MY_ELIT_MAP);
        //custom
        org.osmdroid.views.MapView.LayoutParams mapParams = new org.osmdroid.views.MapView.LayoutParams(
                org.osmdroid.views.MapView.LayoutParams.MATCH_PARENT,
                org.osmdroid.views.MapView.LayoutParams.MATCH_PARENT,
                null, 0, 0, 0);
        contentLayout.addView(mMap, mapParams);
        textViewCurrentLocation = (TextView) findViewById(R.id.centerCoords);


        final GestureDetector gd = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener(){
            //here is the method for double tap
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                //your action here for double tap e.g.
                //Log.d(TAG, "onDoubleTap MotionEvent = "+e);
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                zoomMode = true;
                super.onLongPress(e);
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                //Log.d(TAG, "onDoubleTap onDoubleTapEvent = "+e);
                return true;
            }

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

//here yourView is the View on which you want to set the double tap action
/*
        mMap.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d(TAG, "onTouchEvent = "+event);
                return gd.onTouchEvent(event);
            }
        });
*/
        /*final MapTileProviderBasic tileProvider = new MapTileProviderBasic(ctx);
        tileProvider.setTileSource(MY_ELIT_MAP);
        final TilesOverlay mTilesOverlay = new TilesOverlay(tileProvider, ctx);
        mMap.setOverlayManager(new MyOverlayManager(mTilesOverlay));*/
        mMap.setOverlayManager(MyOverlayManager.create(mMap, ctx));

        // add default zoom buttons
        mMap.setBuiltInZoomControls(true);
        // add ability to zoom with 2 fingers (multi-touch)
        mMap.setMultiTouchControls(true);
        mMap.setTilesScaledToDpi(true);


        mMap.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                IGeoPoint mapCenter = mMap.getMapCenter();
                textViewCurrentLocation.setText("Center: Lat = "+ mapCenter.getLatitude()+", Lon = "+
                        mapCenter.getLongitude()+", zoom = "+ mMap.getZoomLevel());
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }
        });


        /** Handling Map events
         *  tool to get coords from tapped point on map
         *  https://github.com/MKergall/osmbonuspack/wiki/Tutorial_5
         */
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
        mMap.getOverlays().add(0, mapEventsOverlay);
        //**********

        //We can move the mMap on a default view point. For this, we need access to the mMap controller
        mapController = mMap.getController();
        mapController.setZoom(17);//max zoom = 19
        GeoPoint startPoint = new GeoPoint(50.417380, 30.494161);//ТЕСТ ОФИС 1000
        mapController.setCenter(startPoint);

        mMarkerIcon = ContextCompat.getDrawable(ctx, R.drawable.person);
        myMarker = new Marker(mMap);
        myMarker.setPosition(startPoint);
        myMarker.setIcon(mMarkerIcon);
        myMarker.setTitle("Это Я");
        myMarker.showInfoWindow();

        mTaxiIcon = ContextCompat.getDrawable(ctx, R.drawable.mm_or);
        taxiMarker = new Marker(mMap);
        taxiMarker.setPosition(new GeoPoint(50.417872, 30.492798));
        taxiMarker.setIcon(mTaxiIcon);
        taxiMarker.setTitle("Такси на Дубровку");
        taxiMarker.setSnippet("Maserati quattroporte AX7777XA");
        taxiMarker.showInfoWindow();

        mMap.getOverlays().add(myMarker);
        mMap.getOverlays().add(taxiMarker);

        mMap.invalidate();

        btCenterMap = (ImageButton) findViewById(R.id.ic_center_map);

        btCenterMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.setZoom(13);
                setLastKnownLocation();
            }
        });

        initLocationOverlay();


    }

    private void initLocationOverlay() {
        final MyLocationNewOverlay mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), mMap);
        mLocationOverlay.enableMyLocation();
        mMap.getOverlays().add(mLocationOverlay);
        setLastKnownLocation();
        mLocationOverlay.runOnFirstFix(new Runnable() {
            public void run() {
                mMap.getController().animateTo(mLocationOverlay.getMyLocation());
            }
        });
    }
    private void setLastKnownLocation() {
        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        final Location location = LocationUtils.getLastKnownLocation(lm);
        if (location != null) {
            mMap.getController().animateTo(new GeoPoint(location.getLatitude(), location.getLongitude()));
        }
    }
    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initMap();
            } else {
                //Displaying toast if permission is not granted
                Toast.makeText(this, getResources().getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if (requestCode == LOCATION_PERMISSION_CODE) {
            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                //Displaying toast if permission is not granted
                Toast.makeText(this, getResources().getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void showRoute(View view) {

        ArrayList<GeoPoint> lg = new ArrayList<>();
        //custom route****
        lg.add(new GeoPoint(50.41685, 30.49487));
        lg.add(new GeoPoint(50.416954, 30.4950333));
        lg.add(new GeoPoint(50.416954, 30.4950333));
        lg.add(new GeoPoint(50.41801, 30.49344));
        lg.add(new GeoPoint(50.41783, 30.49298));
        lg.add(new GeoPoint(50.417872, 30.492798));
        lg.add(new GeoPoint(50.41791, 30.49283));
        lg.add(new GeoPoint(50.41688, 30.49174));
        lg.add(new GeoPoint(50.41677, 30.49165));
        lg.add(new GeoPoint(50.41656, 30.49142));
        lg.add(new GeoPoint(50.41602, 30.49086));
        lg.add(new GeoPoint(50.41542, 30.49022));
        lg.add(new GeoPoint(50.41503, 30.48981));
        lg.add(new GeoPoint(50.41482, 30.48968));
        lg.add(new GeoPoint(50.41452, 30.48968));
        lg.add(new GeoPoint(50.41409, 30.48994));
        lg.add(new GeoPoint(50.4139, 30.49013));
        lg.add(new GeoPoint(50.41355, 30.49067));
        lg.add(new GeoPoint(50.41345, 30.49099));
        lg.add(new GeoPoint(50.41337, 30.49152));
        lg.add(new GeoPoint(50.413, 30.49427));
        lg.add(new GeoPoint(50.41261, 30.49732));
        lg.add(new GeoPoint(50.41257, 30.49785));
        lg.add(new GeoPoint(50.4131, 30.49751));
        lg.add(new GeoPoint(50.4131, 30.49751));
        lg.add(new GeoPoint(50.41308, 30.4971));
        //custom route****

        //set route polyline*****
        pathOverlay = new Polyline();
        pathOverlay.setGeodesic(true);
        pathOverlay.setWidth(6);
        pathOverlay.setColor(Color.BLUE);
        pathOverlay.setPoints(lg);
        mMap.getOverlays().add(pathOverlay);
        //set route polyline*****


        //set start and finish route markers*****
        pointA = ContextCompat.getDrawable(ctx, R.drawable.flag_red);
        pointB = ContextCompat.getDrawable(ctx, R.drawable.flag_g);

        mItemList = new ArrayList<>();
        startOverlayItem = new OverlayItem("Точка A", "Старт", lg.get(0));
        startOverlayItem.setMarker(pointA);
        mItemList.add(startOverlayItem);
        finishOvelayItem = new OverlayItem("Точка Б", "Финиш", lg.get(lg.size() - 1));
        finishOvelayItem.setMarker(pointB);
        mItemList.add(finishOvelayItem);
        ItemizedOverlayWithFocus<OverlayItem> itemizedOverlay = new ItemizedOverlayWithFocus<>(mItemList,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(final int index, OverlayItem item) {
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, ctx);
        itemizedOverlay.setFocusItemsOnTap(true);
        itemizedOverlay.setFocusedItem(startOverlayItem);

        mMap.getOverlays().add(itemizedOverlay);
        //set route markers*****

        //show markers over the route
        mMap.getOverlays().add(myMarker);
        mMap.getOverlays().add(taxiMarker);

        //scale mMap camera to fit route****
        BoundingBox mBB = BoundingBox.fromGeoPoints(lg);
        mMap.zoomToBoundingBox(mBB, false);
        //scale mMap camera to fit route****

        mMap.invalidate();
    }

    public void setMapToPoint(View view) {
        GeoPoint startPoint = new GeoPoint(50.417380, 30.494161);//ТЕСТ ОФИС 1000
        mapController.setCenter(startPoint);
    }


    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        mapController.animateTo(p);
        Toast.makeText(ctx, "Lat = " + p.getLatitude() + " Lon = " + p.getLongitude(), Toast.LENGTH_LONG).show();
        return true;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        Log.d(TAG, "longPressHelper");
        zoomMode = true;
        return false;
    }


    //zoom with one finger after long press
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            ys = ev.getY();
            zoomMode = false;
            Log.d(TAG, "dispatchTouchEvent MotionEvent.ACTION_DOWN");
        }
        if (zoomMode && ev.getAction() == MotionEvent.ACTION_MOVE) {
            y = ev.getY();
            if (y>ys*1.05){
                mapController.zoomIn();
                ys = y;
            } else if (y<ys/1.05){
                mapController.zoomOut();
                ys = y;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    public static void zoomInMap(){
        mapController.zoomIn();
    }
}
