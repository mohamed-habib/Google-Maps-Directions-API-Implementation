package com.mohamedhabib.map;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.ui.IconGenerator;
import com.mohamedhabib.map.api.APIs;
import com.mohamedhabib.map.api.DataParser;
import com.mohamedhabib.map.api.Route;
import com.mohamedhabib.map.utils.MapUtils;
import com.mohamedhabib.map.utils.Utils;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    /**
     * used to determine the zoom level of the map
     */
    public static final int ZOOM_LEVEL = 15;

    public static float MY_LOCATION_BUTTON_MARGIN_BOTTOM;
    public static float MY_LOCATION_BUTTON_MARGIN_RIGHT;
    private static final String TAG = MapActivity.class.getSimpleName();

    private SupportMapFragment mMapFragment;
    private GoogleMap mMap;
    private EditText mSearchBarText;
    private ImageButton mSearchBarButton;
    ProgressDialog progressDialog;

    LatLng selectedSourceLocation = null;
    LatLng selectedDestinationLocation = null;

    Marker selectedSourceMarker = null;
    Marker selectedDestinationMarker = null;

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    /**
     * Constant used in the location settings dialog.
     */
    private static final int REQUEST_CHECK_SETTINGS = 0x1;

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 50000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Provides access to the Location Settings API.
     */
    private SettingsClient mSettingsClient;

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;

    /**
     * Stores the types of location services the client is interested in using. Used for checking
     * settings to determine if the device has optimal location settings.
     */
    private LocationSettingsRequest mLocationSettingsRequest;

    /**
     * Callback for Location events.
     */
    private LocationCallback mLocationCallback;

    /**
     * Represents a geographical location.
     */
    private Location mCurrentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        MY_LOCATION_BUTTON_MARGIN_BOTTOM = getResources().getDimension(R.dimen.margin_map_location_bottom);
        MY_LOCATION_BUTTON_MARGIN_RIGHT = getResources().getDimension(R.dimen.margin_map_location_right);

        mSearchBarText = (EditText) findViewById(R.id.search_et);
        mSearchBarButton = (ImageButton) findViewById(R.id.search_ib);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        showSnackbar(R.string.select_source, Snackbar.LENGTH_LONG);

        //clicking on the search button at the keyboard
        mSearchBarText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch();
                    return true;
                }
                return false;
            }
        });
        mSearchBarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSearch();
            }
        });
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                //track user location
                mCurrentLocation = locationResult.getLastLocation();
            }
        };
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. We use ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Uses a {@link com.google.android.gms.location.LocationSettingsRequest.Builder} to build
     * a {@link com.google.android.gms.location.LocationSettingsRequest} that is used for checking
     * if a device has the needed location settings.
     */
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        finish();
                        break;
                }
                break;
        }
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        //noinspection MissingPermission
                        mFusedLocationClient.getLastLocation()
                                .addOnSuccessListener(MapActivity.this, new OnSuccessListener<Location>() {
                                    @Override
                                    public void onSuccess(Location location) {
                                        // Got last known location. In some rare situations this can be null.
                                        if (location != null) {
                                            showLocationOnMap(new LatLng(location.getLatitude(), location.getLongitude()));
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MapActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }

                    }
                });
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (checkPermissions()) {
            startLocationUpdates();
        } else if (!checkPermissions()) {
            requestPermissions();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MapActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }


    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param duration         How long to display the message.
     */
    Snackbar showSnackbar(final int mainTextStringId, int duration) {
        Snackbar snackbar = Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                duration);
        snackbar.show();
        return snackbar;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permission granted, updates requested, starting location updates");

                if (!mMap.isMyLocationEnabled())
                    //noinspection MissingPermission
                    mMap.setMyLocationEnabled(true);
                startLocationUpdates();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation,
                        R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
            }
        }
    }

    /**
     * gets the text from the searchBar and updates the UI with the search results
     */
    private void performSearch() {
        if (!TextUtils.isEmpty(mSearchBarText.getText().toString())) {
            Utils.hideKeyboard(this);
            if (Utils.isConnectingToInternet(this)) {
                Address address = MapUtils.getAddressByLocationName(this, mSearchBarText.getText().toString());
                if (address != null) {
                    updateUI(address);
                } else {
                    showSnackbar(R.string.can_not_search, Snackbar.LENGTH_SHORT);
                }
            } else {
                showSnackbar(R.string.no_internet_connection, R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        performSearch();
                    }
                });
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                MapActivity.this.onMapClick(latLng);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return MapActivity.this.onMarkerClick(marker);
            }
        });
        initializeMapLocationSettings();
    }

    private void onMapClick(final LatLng latLng) {
        if (Utils.isConnectingToInternet(this)) {
            if (mMap != null && latLng != null) {
                if (selectedSourceLocation != null && selectedDestinationLocation != null) {
                    //user clicked on the map after selecting source and destination
                    showSnackbar(R.string.clear_markers, R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedSourceLocation = null;
                            selectedDestinationLocation = null;
                            mMap.clear();
                            showSnackbar(R.string.select_source, Snackbar.LENGTH_INDEFINITE);
                        }
                    });
                } else {
                    if (selectedSourceMarker != null && selectedSourceLocation == null)
                        //user selected source, and didn't confirm it, remove the source marker and select new source
                        selectedSourceMarker.remove();

                    if (selectedDestinationMarker != null && selectedDestinationLocation == null)
                        //user selected destination, and didn't confirm it, remove the destination marker and select new destination
                        selectedDestinationMarker.remove();

                    new ShowAddressByLatLng().execute(latLng);
                }
            }
        } else {
            showSnackbar(R.string.no_internet_connection, R.string.retry, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onMapClick(latLng);
                }
            });
        }
    }

    private boolean onMarkerClick(Marker marker) {
        if (selectedSourceLocation == null && marker.equals(selectedSourceMarker)) {
            //source marker clicked, confirm its location as a source
            selectedSourceLocation = marker.getPosition();
            showSnackbar(R.string.souce_selected, Snackbar.LENGTH_LONG);
        } else if (selectedDestinationLocation == null && marker.equals(selectedDestinationMarker)) {
            //destination marker clicked, confirm its location as a destination
            showSnackbar(R.string.dest_selected, Snackbar.LENGTH_LONG);
            selectedDestinationLocation = marker.getPosition();

            //source and destination are selected, show the directions from source to destination
            showDirectionsOnMap();
        }
        return false;
    }

    /**
     * call Directions API and draws the routes on the map
     */
    private void showDirectionsOnMap() {
        progressDialog = createProgressDialog();
        APIs.callDirectionsAPI(this, selectedSourceLocation, selectedDestinationLocation, true, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                //parse the response
                new ParserTask().execute(response);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.hide();
            }
        });
    }

    private ProgressDialog createProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Calculating Directions between source and destination");
        progressDialog.setCancelable(false);
        progressDialog.show();
        return progressDialog;
    }

    /**
     * move the map buttons to the bottom of screen
     */
    @SuppressWarnings({"ResourceType"})
    private void initializeMapLocationSettings() {
        if (mMap != null) {
            if (!mMap.isMyLocationEnabled())
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(true);
                }

            //move the buttons to the bottom of the search bar
            View mapView = mMapFragment.getView();
            if (mapView != null && mapView.findViewById(1) != null) {
                // Get the button view
                View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
                // and next place it, on bottom right (as Google Maps app)
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
                // position on right bottom
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
                layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
                layoutParams.setMargins(0, 0, (int) MY_LOCATION_BUTTON_MARGIN_RIGHT, (int) MY_LOCATION_BUTTON_MARGIN_BOTTOM);
            }
        }
    }

    private Marker createMarker(LatLng markerLatLng, String markerTitle) {
        IconGenerator iconFactory = new IconGenerator(this);
        Marker mSelectedLocationMarker = mMap.addMarker(new MarkerOptions().position(markerLatLng));
        Bitmap bitmap = iconFactory.makeIcon(markerTitle);
        mSelectedLocationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));

        return mSelectedLocationMarker;
    }

    private Marker createDirectionMarker(LatLng markerLatLng, String markerTitle, boolean fastest) {
        int drawableBackground = R.drawable.layout_bg_filled_full_corners_gray;
        if (fastest) {
            drawableBackground = R.drawable.layout_bg_filled_full_corners_blue;
        }
        IconGenerator iconFactory = new IconGenerator(this);
        iconFactory.setBackground(ContextCompat.getDrawable(this, drawableBackground));
        iconFactory.setTextAppearance(R.style.iconGenText);
        Marker mSelectedLocationMarker = mMap.addMarker(new MarkerOptions().position(markerLatLng));
        Bitmap bitmap = iconFactory.makeIcon(markerTitle);
        mSelectedLocationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        return mSelectedLocationMarker;
    }

    private String getAddressName(Address address, int i) {
        if (address != null && address.getMaxAddressLineIndex() > 0)
            return address.getAddressLine(i);
        return "";
    }

    private void showAddressOnSearchBar(String completeAddress) {
        //show the address on the search bar
        mSearchBarText.setText(completeAddress);
        mSearchBarText.setSelection(mSearchBarText.getText().length());
    }

    private String getCompleteAddressString(Address returnedAddress) {
        StringBuilder strReturnedAddress = new StringBuilder("");
        if (returnedAddress != null) {
            for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                strReturnedAddress.append(returnedAddress.getAddressLine(i));
                if (i != returnedAddress.getMaxAddressLineIndex() - 1)
                    strReturnedAddress.append(", ");
            }
        }
        return strReturnedAddress.toString();
    }

    private void showLocationOnMap(LatLng location) {
        if (mMap != null && location != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, ZOOM_LEVEL));
        }
    }

    /**
     * adds the address to search bar and shows a marker on the map
     *
     * @param address
     */
    private void updateUI(Address address) {
        if (address != null) {

            //show on the search bar
            showAddressOnSearchBar(getCompleteAddressString(address));

            //show markers on map and show snackbars
            if (selectedSourceLocation == null) {
                //show the source marker on map
                selectedSourceMarker = createMarker(new LatLng(address.getLatitude(), address.getLongitude()), getAddressName(address, 0));
                showSnackbar(R.string.click_to_confirm_source, Snackbar.LENGTH_LONG);
            } else if (selectedDestinationLocation == null) {
                //show the destination marker on map
                selectedDestinationMarker = createMarker(new LatLng(address.getLatitude(), address.getLongitude()), getAddressName(address, 0));
                showSnackbar(R.string.click_to_confirm_destination, Snackbar.LENGTH_LONG);
            }

            // Animating to the touched position
            showLocationOnMap(new LatLng(address.getLatitude(), address.getLongitude()));
        }
    }

    /**
     * AsyncTask class used to run getAddressByLatLng method on background and updateUI
     */
    private class ShowAddressByLatLng extends AsyncTask<LatLng, Void, Address> {

        protected Address doInBackground(LatLng... latLngs) {
            return MapUtils.getAddressByLatLng(MapActivity.this, latLngs[0]);
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Address address) {
            updateUI(address);
        }
    }

    /**
     * AsyncTask class used to parse the data in non-ui thread
     */
    private class ParserTask extends AsyncTask<String, Integer, List<Route>> {
        @Override
        protected List<Route> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<Route> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d(TAG, jsonData[0].toString());

                // Starts parsing data
                routes = DataParser.parse(jObject);

                Log.d(TAG, "Executing routes");
                Log.d(TAG, routes.toString());
                Log.d(TAG, routes.size() + "");

            } catch (Exception e) {
                Log.d(TAG, e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<Route> routes) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = routes.size() - 1; i >= 0; i--) {
                Route route = routes.get(i);
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = route.mRoutesLatLng.get(i);
                Log.d(TAG, path.toString());

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                drawRouteOnMap(points, lineOptions, i, route);
            }
            //finished long operations, hide the dialog
            progressDialog.hide();
        }
    }

    private void drawRouteOnMap(ArrayList<LatLng> points, PolylineOptions lineOptions, int i, Route route) {
        int color = Color.parseColor("#828282");
        boolean fastest = false;
//                first route should be blue, others shoud be gray
        if (i == 0) {
            color = Color.parseColor("#05b1fb");
            fastest = true;
        }
        // Adding all the points in the route to LineOptions
        lineOptions.addAll(points);
        lineOptions.width(15);
        lineOptions.color(color);

        // Drawing polyline in the Google Map for the i-th route
        mMap.addPolyline(lineOptions);
        createDirectionMarker(points.get(points.size() / 2), route.getDuration(), fastest);
    }
}