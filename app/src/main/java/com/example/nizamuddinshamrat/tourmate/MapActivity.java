package com.example.nizamuddinshamrat.tourmate;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlacePhotoMetadata;
import com.google.android.gms.location.places.PlacePhotoMetadataBuffer;
import com.google.android.gms.location.places.PlacePhotoMetadataResponse;
import com.google.android.gms.location.places.PlacePhotoResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.ClusterRenderer;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, PlacesAdapter.ClickListener {
    private static final int GEO_FENCE_CODE = 10;
    private static final int GEOFENCE_PENDIND_CODE = 111;
    GoogleMap map;
    Address address;
    GoogleMapOptions options;
    public static final int PERMISSION_REQUEST = 1;
    ArrayList<MyItem> clusterItems = new ArrayList<>();
    ArrayList<MyItem> nearByItems = new ArrayList<>();
    ClusterManager<MyItem> clusterManager;
    private FusedLocationProviderClient client;
    private Location lastLocation;
    double lat, lng;
    LatLng latLng;
    Place place;
    private String origin;
    private String destination;
    private final int AUTOCOMPLTE_REQUEST = 2;
    SearchBox search;
    FloatingActionButton direction;
    ImageView Drawer;
    AutocompleteFilter typeFilter;
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/";
    NearByPlacesService service;
    String urlString;
    double nearByLat;
    double nearByLng;
    double radius;
    String type;
    double desLng;
    double desLat;
    View bottomSheetHeaderColor;
    String key;
    private BottomSheetBehavior mBottomSheetBehavior;
    List<NearByPlacesResponse.Result> places;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    String nextPage;
    int responceCode;
    String nearByString;
    boolean remove;
    boolean dir;
    GeoDataClient mGeoDataClient;
    AlertDialog.Builder alert;
    MyItem item;
    boolean check;
    GeofencingClient geofencingClient;
    PendingIntent pendingIntent;
    ArrayList<Geofence> geofences = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        //bottom navigation
        //bottom navigation
        BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
                = new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.tourProjectMap:
                        startActivity(new Intent(MapActivity.this,EventActivity.class));
                        return true;
                    case R.id.tourWeatherMap:
                        startActivity(new Intent(MapActivity.this,WeatherActivity.class));
                        return true;
                }
                return false;
            }
        };
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        geofencingClient = LocationServices.getGeofencingClient(MapActivity.this);
        pendingIntent = null;
        places = new ArrayList<>();
        direction = findViewById(R.id.direction);
        direction.setVisibility(View.INVISIBLE);
        mGeoDataClient = Places.getGeoDataClient(this, null);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        bottomSheetHeaderColor = findViewById(R.id.color);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        nearByLat = 181;
        sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedpreferences.edit();
        editor.commit();
        //Create retrofit object
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        service = retrofit.create(NearByPlacesService.class);
        key = getString(R.string.google_near_by_places_api);


        //navigation drawer
        Drawer = findViewById(R.id.drawer_logo);

        Drawer.setVisibility(View.INVISIBLE);

        search = findViewById(R.id.searchbox);
        search.setLogoText("Search Places");
        search.setLogoTextColor(R.color.colorSearchText);
        //search view listener
        search.setSearchListener(new SearchBox.SearchListener() {
            @Override
            public void onSearchOpened() {
                Drawer.setVisibility(View.INVISIBLE);
                search.toggleSearch();
                try {
                    address = getAddress(lat, lng);
                    if (address != null) {
                        typeFilter = new AutocompleteFilter.Builder()
                                .setCountry(address.getCountryCode())
                                .build();
                    } else {
                        typeFilter = new AutocompleteFilter.Builder()
                                .build();
                    }

                    Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                            .setFilter(typeFilter)
                            .build(MapActivity.this);
                    startActivityForResult(intent, AUTOCOMPLTE_REQUEST);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onSearchCleared() {
                Drawer.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onSearchClosed() {
                Drawer.setVisibility(View.INVISIBLE);
                getWindow().setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                );
            }

            @Override
            public void onSearchTermChanged(String s) {
                Drawer.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onSearch(String s) {

            }

            @Override
            public void onResultClick(SearchResult searchResult) {

            }
        });

        //map fragment
        options = new GoogleMapOptions();
        options.zoomControlsEnabled(true).compassEnabled(true).mapType(GoogleMap.MAP_TYPE_TERRAIN);
        SupportMapFragment mapFragment = SupportMapFragment.newInstance(options);
        android.support.v4.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction().replace(R.id.map, mapFragment);
        ft.commit();
        mapFragment.getMapAsync(this);
        client = LocationServices.getFusedLocationProviderClient(this);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_DRAGGING
                        || mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetHeaderColor.setBackgroundColor(getResources().getColor(R.color.darkSky));
                } else {
                    bottomSheetHeaderColor.setBackgroundColor(getResources().getColor(R.color.bluish_gray));
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        this.map = googleMap;

        clusterManager = new ClusterManager<MyItem>(MapActivity.this, googleMap);
        map.setOnCameraIdleListener(clusterManager);
        map.setOnMarkerClickListener(clusterManager);
        clusterManager.clearItems();

        //set my location enabled
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST);
            return;
        }

        //get last location
        client.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    lastLocation = task.getResult();
                    lat = lastLocation.getLatitude();
                    lng = lastLocation.getLongitude();
                    latLng = new LatLng(lat, lng);

                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
                    address = getAddress(lat, lng);
                    if (address != null) {
                        clusterItems.add(new MyItem(lat, lng, address.getLocality(), address.getAdminArea()));
                    } else clusterItems.add(new MyItem(lat, lng));
                    clusterManager.addItems(clusterItems);
                }
            }
        });
        map.setMyLocationEnabled(true);
        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                if(check) {
                    nearByLat = lat;
                    nearByLng = lng;
                    if (address != null) {
                        nearByString = address.getLocality();
                        clusterItems.add(new MyItem(lat, lng, address.getLocality(), address.getAdminArea()));
                    } else clusterItems.add(new MyItem(lat, lng));
                    clusterManager.addItems(clusterItems);
                    clusterManager.cluster();
                    dir = false;
                    direction.setVisibility(View.INVISIBLE);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 12));
                }
                return false;
            }
        });

        clusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MyItem>() {
            @Override
            public boolean onClusterItemClick(MyItem myItem) {
                if (nearByItems.contains(myItem)) {
                    item = myItem;
                    getPhotos(myItem.getPlaceId(), null);

                }
                return false;
            }
        });


        //Disable Map Toolbar:
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Address geoAddress=getAddress(latLng.latitude,latLng.longitude);
                String address="Selected area";
                if(geoAddress!=null){
                    address=geoAddress.getAddressLine(0);
                }
                Geofence geofence = new Geofence.Builder().setRequestId(address)
                        .setCircularRegion(latLng.latitude, latLng.longitude, 200)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setExpirationDuration(6 * 60 * 60 * 1000)
                        .build();
                geofences.add(geofence);
                if(geofences.size()>0)
                    registerGeofence();
                Toast.makeText(MapActivity.this, "Added to Geofenced Area", Toast.LENGTH_SHORT).show();
            }
        });

        googleMap.getUiSettings().setMapToolbarEnabled(false);
        this.map = googleMap;
    }

    private void registerGeofence() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MapActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},GEO_FENCE_CODE);
            return;
        }
        geofencingClient.addGeofences(getGeofencingRequest(),getPendingIntent()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(MapActivity.this, "Geofence Added", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MapActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("error", task.getException().getMessage());
                }
            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==PERMISSION_REQUEST&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            onMapReady(map);
            if(geofences.size()>0)
                registerGeofence();
            check=true;
        }else {
            check=false;
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show();
        }
    }
    private GeofencingRequest getGeofencingRequest(){
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);
        GeofencingRequest request=builder.build();
        return request;
    }
    private PendingIntent getPendingIntent(){
        if(pendingIntent != null){
            return pendingIntent;
        }else{
            Intent intent = new Intent(MapActivity.this,GeofencePendingIntentService.class);
            pendingIntent = PendingIntent.getService(this,GEOFENCE_PENDIND_CODE,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }
    }

    public void getNearByPlaces(){
        Call<NearByPlacesResponse> nearByPlacesResponseCall;
        nearByPlacesResponseCall=service.getNearByPlaces(urlString);
        nearByPlacesResponseCall.enqueue(new Callback<NearByPlacesResponse>() {
            @Override
            public void onResponse(Call<NearByPlacesResponse> call, Response<NearByPlacesResponse> response) {
                editor.putInt("code",response.code());
                editor.apply();
                editor.commit();
                if(response.code()==200){
                    NearByPlacesResponse placesResponse;
                    placesResponse=response.body();
                    editor.putString("token",placesResponse.getNextPageToken());
                    editor.apply();
                    editor.commit();
                    places.addAll(placesResponse.getResults());
                    try {
                        if(places.size()!=0)
                            Toast.makeText(MapActivity.this,places.size()+" places found!",Toast.LENGTH_SHORT).show();
                    }catch (NullPointerException e){}

                    Log.e("Url",BASE_URL+urlString);
                    getNextPage();
                }else {
                    Toast.makeText(MapActivity.this,response.message(),Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onFailure(Call<NearByPlacesResponse> call, Throwable t) {
                Toast.makeText(MapActivity.this,t.getMessage(),Toast.LENGTH_SHORT).show();
                editor.putInt("code",0);
                editor.apply();
                editor.commit();
            }
        });

    }

    //get Address
    public Address getAddress(double latitude, double longitude) {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
            return addresses.get(0);
        } catch (Exception e){}
        return null;
    }

    //get searched place
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTOCOMPLTE_REQUEST) {
            if (resultCode == RESULT_OK) {
                place = PlaceAutocomplete.getPlace(this, data);
                desLat = place.getLatLng().latitude;
                desLng = place.getLatLng().longitude;
                destination = "" + desLat + "," + desLng;
                nearByLng=desLng;
                nearByLat=desLat;
                nearByString=place.getAddress().toString();
                origin = "" + lastLocation.getLatitude() + "," + lastLocation.getLongitude();
                map.clear();
                clusterItems.clear();
                clusterManager.clearItems();
                if(address!=null) {
                    clusterItems.add(new MyItem(lat, lng, address.getLocality(), address.getAdminArea()));
                }
                else clusterItems.add(new MyItem(lat,lng));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(desLat, desLng), 12));
                clusterItems.add(new MyItem(desLat, desLng, place.getName().toString(), place.getAddress().toString()));
                clusterManager.addItems(clusterItems);
                clusterManager.cluster();
                dir=true;
                direction.setVisibility(View.VISIBLE);
            }else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                // TODO: Handle the error.
                Log.i("",status.getStatusMessage());

            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //go to direction activity and get direction
    public void direction(View view) {
        if(!remove&&dir) {
            Intent intent = new Intent(MapActivity.this, DirecctionActivity.class);
            intent.putExtra("desLat", place.getLatLng().latitude);
            intent.putExtra("desLng", place.getLatLng().longitude);
            intent.putExtra("name", place.getName().toString());
            intent.putExtra("map", true);
            finish();
            startActivity(intent);
        }
        else if(remove){
            clusterManager.clearItems();
            nearByItems.clear();
            clusterManager.addItems(clusterItems);
            clusterManager.cluster();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(nearByLat,nearByLng),12));
            remove=false;

            direction.setImageResource(R.drawable.ic_directions_black_24dp);
            direction.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));
            if(!dir){
                direction.setVisibility(View.INVISIBLE);
            }else direction.setVisibility(View.VISIBLE);
        }
    }
    public void nearBy(View view) {
        clusterManager.clearItems();
        nearByItems.clear();
        clusterManager.addItems(clusterItems);
        clusterManager.cluster();
        if(nearByLat==181){
            nearByLat=lat;
            nearByLng=lng;
            if(address!=null) {
                nearByString = address.getAdminArea();
            }
        }
        places.clear();
        switch (view.getId()){
            case R.id.rest_ibtn:
                Toast.makeText(MapActivity.this,"Restaurants",Toast.LENGTH_SHORT).show();
                type="restaurant";
                break;
            case R.id.cafe_ibtn:
                Toast.makeText(MapActivity.this,"Hotels",Toast.LENGTH_SHORT).show();
                type="lodging";
                break;
            case R.id.gas_ibtn:
                Toast.makeText(MapActivity.this,"Gas Stations",Toast.LENGTH_SHORT).show();
                type="gas_station";
                break;
            case R.id.atm_ibtn:
                Toast.makeText(MapActivity.this,"ATMs",Toast.LENGTH_SHORT).show();
                type="atm";
                break;
            case R.id.pharma_ibtn:
                Toast.makeText(MapActivity.this,"Pharmacies",Toast.LENGTH_SHORT).show();
                type="pharmacy";
                break;
            case R.id.groc_ibtn:
                Toast.makeText(MapActivity.this,"Super Markets",Toast.LENGTH_SHORT).show();
                type="supermarket";
                break;

        }
        radius=1000;
        urlString=String.format("json?location=%f,%f&radius=%f&type=%s&key=%s",nearByLat,nearByLng,radius,type,key);
        getNearByPlaces();

    }

    public void getNextPage() {
        responceCode = sharedpreferences.getInt("code", 0);
        if (responceCode == 200) {
            nextPage = sharedpreferences.getString("token", null);
            if (nextPage != null) {
                urlString = String.format("json?pagetoken=%s&key=%s", nextPage, key);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getNearByPlaces();
            }else {
                showPlaces();
            }
        }
    }
    public void showPlaces(){
        if(places.size()!=0) {
            final Dialog dialog = new Dialog(MapActivity.this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);
            View view = LayoutInflater.from(MapActivity.this).inflate(R.layout.places, null);
            dialog.setContentView(view);
            TextView title = view.findViewById(R.id.typeTV);
            title.setText("Nearby " + type + " list");
            RecyclerView recyclerView = dialog.findViewById(R.id.placeRV);
            PlacesAdapter adapter = new PlacesAdapter(MapActivity.this, places, this);
            RecyclerView.LayoutManager manager = new LinearLayoutManager(MapActivity.this, LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(manager);
            recyclerView.setAdapter(adapter);
            adapter.Update(places);
            TextView cancel=dialog.findViewById(R.id.cancel);
            TextView showAll=dialog.findViewById(R.id.showAll);
            cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                    Toast.makeText(MapActivity.this,"No places selected",Toast.LENGTH_SHORT).show();
                }
            });
            showAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nearByItems.clear();
                    LatLngBounds.Builder builder=new LatLngBounds.Builder();
                    for (NearByPlacesResponse.Result place:places){
                        builder.include(new LatLng(place.getGeometry().getLocation().getLat(),
                                place.getGeometry().getLocation().getLng()));
                        MyItem item=new MyItem(place.getGeometry().getLocation().getLat(),
                                place.getGeometry().getLocation().getLng(),place.getName());
                        item.setPlaceId(place.getPlaceId());
                        nearByItems.add(item);

                    }
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),30));
                    clusterManager.addItems(nearByItems);
                    clusterManager.cluster();
                    direction.setImageResource(R.drawable.ic_remove_black_24dp);
                    direction.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                    direction.setVisibility(View.VISIBLE);
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    remove=true;
                    dialog.dismiss();
                }
            });
            dialog.show();
        }else {
            Toast.makeText(MapActivity.this,"No places found",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(NearByPlacesResponse.Result place) {
        getPhotos(place.getPlaceId(),place);
    }
    private void getPhotos(String placeId, final NearByPlacesResponse.Result place) {
        Task<PlacePhotoMetadataResponse> photoMetadataResponse = mGeoDataClient.getPlacePhotos(placeId);
        photoMetadataResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoMetadataResponse>() {
            @Override
            public void onComplete(@NonNull Task<PlacePhotoMetadataResponse> task) {
                // Get the list of photos.
                PlacePhotoMetadataResponse photos = task.getResult();
                // Get the PlacePhotoMetadataBuffer (metadata for all of the photos).
                PlacePhotoMetadataBuffer photoMetadataBuffer = photos.getPhotoMetadata();
                try {

                    // Get the first photo in the list.
                    PlacePhotoMetadata photoMetadata = photoMetadataBuffer.get(0);
                    // Get the attribution text.
                    CharSequence attribution = photoMetadata.getAttributions();
                    // Get a full-size bitmap for the photo.
                    Task<PlacePhotoResponse> photoResponse = mGeoDataClient.getPhoto(photoMetadata);
                    photoResponse.addOnCompleteListener(new OnCompleteListener<PlacePhotoResponse>() {
                        @Override
                        public void onComplete(@NonNull Task<PlacePhotoResponse> task) {
                            PlacePhotoResponse photo = task.getResult();
                            Bitmap bitmap = photo.getBitmap();
                            getImage(bitmap,place);
                        }
                    });

                }catch (Exception e){getImage(null,place);}

            }
        });
    }

    private void getImage(Bitmap bitmap, final NearByPlacesResponse.Result plac) {
        alert=new AlertDialog.Builder(MapActivity.this);
        if(bitmap!=null) {
            final ImageView imageView = new ImageView(this);
            imageView.setImageBitmap(bitmap);
            alert.setView(imageView);
        }
        if(plac==null) {
            alert.setTitle(item.getTitle());
        }else alert.setTitle(plac.getName());
        alert.setCancelable(false);
        alert.setNegativeButton("Cancel",null);
        alert.setPositiveButton("Show Direction", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(plac==null) {
                    Intent intent = new Intent(MapActivity.this, DirecctionActivity.class);
                    intent.putExtra("startLat", nearByLat);
                    intent.putExtra("startLng", nearByLng);
                    intent.putExtra("startName", nearByString);
                    intent.putExtra("start", true);
                    intent.putExtra("desLat", item.getPosition().latitude);
                    intent.putExtra("desLng", item.getPosition().longitude);
                    intent.putExtra("name", item.getTitle());
                    intent.putExtra("map", true);
                    finish();
                    startActivity(intent);
                    Toast.makeText(MapActivity.this, item.getTitle(), Toast.LENGTH_SHORT).show();
                }else {

                    Intent intent=new Intent(MapActivity.this,DirecctionActivity.class);
                    intent.putExtra("startLat",nearByLat);
                    intent.putExtra("startLng",nearByLng);
                    intent.putExtra("startName",nearByString);
                    intent.putExtra("start",true);
                    intent.putExtra("desLat",plac.getGeometry().getLocation().getLat());
                    intent.putExtra("desLng",plac.getGeometry().getLocation().getLng());
                    intent.putExtra("name",plac.getName().toString());
                    intent.putExtra("map",true);
                    finish();
                    startActivity(intent);
                    Toast.makeText(MapActivity.this,plac.getName(),Toast.LENGTH_SHORT).show();
                }
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
    }

}
