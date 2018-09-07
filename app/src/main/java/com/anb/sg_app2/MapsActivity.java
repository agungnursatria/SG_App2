package com.anb.sg_app2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ImageViewCompat;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;
    String transportMode = TransportMode.DRIVING;

    @BindView(R.id.input_search)
    AutoCompleteTextView mSearchText;
    @BindView(R.id.tv_origin)
    TextView tvOrigin;
    @BindView(R.id.tv_destination)
    TextView tvDestination;
    @BindView(R.id.tv_duration)
    TextView tvDuration;
    @BindView(R.id.tv_origin_desc)
    TextView tvOriginDesc;
    @BindView(R.id.tv_destination_desc)
    TextView tvDestinationDesc;

    @BindView(R.id.iv_transportDriving)
    ImageView ivDriving;
    @BindView(R.id.iv_transportTransit)
    ImageView ivTransit;
    @BindView(R.id.iv_transportCycling)
    ImageView ivCycling;
    @BindView(R.id.iv_transportWalking)
    ImageView ivWalking;

    private PlaceAutoCompleteAdapter mPlaceAutoCompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    Geocoder geocoder;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));
    final static float default_zoom = 15f;

    Boolean permission = false;
    Boolean gpsUsable = false;
    FusedLocationProviderClient mFusedLocationClient;
    LatLng origin, destination;

    private LinearLayout layout;
    private BottomSheetBehavior bsb;

    Marker marker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        requestPermission();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        ButterKnife.bind(this);

        layout = findViewById(R.id.bottom_sheet);
        bsb = BottomSheetBehavior.from(layout);
        bsb.setState(BottomSheetBehavior.STATE_HIDDEN);

        if (permission)
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    @OnClick(R.id.ic_magnify)
    void search() {
        geoLocate();
    }


    void changeIconColor(ImageView imageView){
        ImageViewCompat.setImageTintList(ivDriving, ColorStateList.valueOf(Color.GRAY));
        ImageViewCompat.setImageTintList(ivTransit, ColorStateList.valueOf(Color.GRAY));
        ImageViewCompat.setImageTintList(ivCycling, ColorStateList.valueOf(Color.GRAY));
        ImageViewCompat.setImageTintList(ivWalking, ColorStateList.valueOf(Color.GRAY));
        ImageViewCompat.setImageTintList(imageView, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorPrimaryDark)));
    }

    @OnClick(R.id.iv_transportDriving)
    void clickedIvDriving(){
        changeIconColor(ivDriving);
        transportMode = TransportMode.DRIVING;
        geoLocate();
    }
    @OnClick(R.id.iv_transportTransit)
    void clickedIvTransit(){
        changeIconColor(ivTransit);
        transportMode = TransportMode.TRANSIT;
        geoLocate();
    }
    @OnClick(R.id.iv_transportCycling)
    void clickedIvCycling(){
        changeIconColor(ivCycling);
        transportMode = TransportMode.BICYCLING;
        geoLocate();
    }
    @OnClick(R.id.iv_transportWalking)
    void clickedIvWalking(){
        changeIconColor(ivWalking);
        transportMode = TransportMode.WALKING;
        geoLocate();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (permission) {
            mMap.setMyLocationEnabled(true);
            init();
        }
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MapsActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        } else {
            permission = true;
        }
    }

    private void init() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();
        mGoogleApiClient.connect();

        geocoder = new Geocoder(MapsActivity.this);

        mPlaceAutoCompleteAdapter = new PlaceAutoCompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);
        mSearchText.setAdapter(mPlaceAutoCompleteAdapter);

        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    geoLocate();
                }
                return false;
            }
        });

        // Turn on GPS with high accuracy
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30 * 1000);
        locationRequest.setFastestInterval(5 * 1000);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> task =
                LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());

        task.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    LocationSettingsStates state = response.getLocationSettingsStates();
                    if (state.isGpsPresent() && state.isGpsUsable()) {
                        gpsUsable = true;
                        if (permission) {
                            mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    if (location != null) {
                                        origin = new LatLng(location.getLatitude(), location.getLongitude());
                                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 18f));
                                    }
                                }
                            });
                        }
                    }
                } catch (ApiException exception) {
                    switch (exception.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvable = (ResolvableApiException) exception;
                                resolvable.startResolutionForResult(MapsActivity.this, 1000);
                            } catch (IntentSender.SendIntentException | ClassCastException ignored) {}
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            }
        });

        hideSoftKeyboard();
    }

    @SuppressLint("MissingPermission")
    private void geoLocate() {
        String searchString = mSearchText.getText().toString();
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException ignored) { }

        if (list.size() > 0) {
            final Address address = list.get(0);
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), default_zoom,
                    address.getFeatureName(), address.getAddressLine(0));

            destination = new LatLng(address.getLatitude(), address.getLongitude());
            if (permission) {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    origin = new LatLng(location.getLatitude(), location.getLongitude());
                                    getDirection(origin, destination);
                                }
                            }
                        });
            }
        }
    }

    void getDirection(final LatLng origin, final LatLng destination) {
        GoogleDirection.withServerKey(getString(R.string.google_maps_key))
                .from(origin)
                .to(destination)
                .transportMode(transportMode)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        if (direction.isOK()) {
                            List<Leg> legs = direction.getRouteList().get(0).getLegList();
                            if (legs.size() > 0) {
                                for (int i = 0; i < legs.size(); i++) {
                                    int color = (i == 0) ? Color.RED : Color.GRAY;
                                    ArrayList<LatLng> pointList = legs.get(i).getDirectionPoint();
                                    PolylineOptions polylineOptions = DirectionConverter.createPolyline(MapsActivity.this, pointList, 5, color);
                                    mMap.addPolyline(polylineOptions);
                                }
                                String duration = legs.get(0).getDuration().getText();
                                setBottomSheet(origin, destination, duration);
                            }
                        } else {
                            Toast.makeText(MapsActivity.this, direction.getErrorMessage(), Toast.LENGTH_SHORT).show();
                            setBottomSheet(origin, destination, "-");
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Toast.makeText(MapsActivity.this, "Failure Directing", Toast.LENGTH_SHORT).show();
                        setBottomSheet(origin, destination, "-");
                    }
                });
    }

    private Pair<String,String> getLocationNameAndDesc(LatLng latLng) {
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException ignored) {}

        if (list.size() > 0){
            String name = list.get(0).getFeatureName();
            String address = list.get(0).getAddressLine(0);
            return new Pair<>(name,address);
        }else{
            return new Pair<>("","");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setBottomSheet(LatLng origin, LatLng destination, String duration) {
        Pair<String,String> from = getLocationNameAndDesc(origin);
        Pair<String,String> to = getLocationNameAndDesc(destination);

        tvOrigin.setText("From : " + from.first);
        tvDestination.setText("To : " + to.first);
        tvOriginDesc.setText(from.second);
        tvDestinationDesc.setText(to.second);
        tvDuration.setText(duration);
        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void moveCamera(LatLng latLng, float zoom, String title, String desc) {
        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        MarkerOptions options = new MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(desc);
        marker = mMap.addMarker(options);

        hideSoftKeyboard();
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                Place place = PlaceAutocomplete.getPlace(this, data);
//                mMap.clear();
//                marker = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).title(place.getName().toString()).snippet(Objects.requireNonNull(place.getAddress()).toString()));
//                mMap.moveCamera(CameraUpdateFactory.newLatLng(place.getLatLng()));
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 12.0f));
//            }
//        }
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                permission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (bsb.getState() != BottomSheetBehavior.STATE_HIDDEN){
            bsb.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else if (marker!=null){
            // marker.remove() tidak berfungsi
            mMap.clear();
            marker = null;
        } else
            super.onBackPressed();
    }
}
