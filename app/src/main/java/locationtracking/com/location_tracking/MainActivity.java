package locationtracking.com.location_tracking;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private Marker marker;

    private Queue<LatLng> points = new LinkedList<>();

    private AnimatorSet animatorSet = new AnimatorSet();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Objects.requireNonNull(mapFragment).getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Registering the listener to update location for every 100m displacement
        startLocationUpdates();
        //getting the latest point from the queue for every 5 seconds
        SubscribetoTimer();
    }

    // Trigger new location updates at interval
    @SuppressLint("MissingPermission")
    protected void startLocationUpdates() {

        // Create the location request to start receiving updates
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(100f);
        mLocationRequest.setInterval(0);
        mLocationRequest.setFastestInterval(0);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        points.add(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));

                    }
                },
                Looper.myLooper());
    }

    private void SubscribetoTimer() {

        Observable.interval(5, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        SendNextPoints();

                    }
                });


    }


    private void SendNextPoints() {

        if (!animatorSet.isRunning() && !points.isEmpty())
            UpdateMarker(points.poll()); // taking the points f rom head of the queue.

    }

    private void UpdateMarker(LatLng newlatlng) {

        if (marker != null) {
            float bearingangle = Calculatebearingagle(newlatlng);
            marker.setAnchor(0.5f, 0.5f);
            animatorSet = new AnimatorSet();
            animatorSet.playTogether(rotateMarker(Float.isNaN(bearingangle) ? -1 : bearingangle, marker.getRotation()), moveVechile(newlatlng, marker.getPosition()));
            animatorSet.start();
        } else
            AddMarker(newlatlng);


        mMap.animateCamera(CameraUpdateFactory.newCameraPosition
                (new CameraPosition.Builder().target(newlatlng)
                        .zoom(16f).build()));


    }


    private void AddMarker(LatLng initialpos) {

        MarkerOptions markerOptions = new MarkerOptions().position(initialpos).flat(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.scooter));
        marker = mMap.addMarker(markerOptions);


    }

    private float Calculatebearingagle(LatLng newlatlng) {
        Location destinationLoc = new Location("service Provider");
        Location userLoc = new Location("service Provider");

        userLoc.setLatitude(marker.getPosition().latitude);
        userLoc.setLongitude(marker.getPosition().longitude);

        destinationLoc.setLatitude(newlatlng.latitude);
        destinationLoc.setLongitude(newlatlng.longitude);


        return userLoc.bearingTo(destinationLoc);

    }

    public synchronized ValueAnimator rotateMarker(final float toRotation, final float startRotation) {

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(1555);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                float t = Float.parseFloat(valueAnimator.getAnimatedValue().toString());

                float rot = t * toRotation + (1 - t) * startRotation;

                marker.setRotation(-rot > 180 ? rot / 2 : rot);


            }
        });
        return valueAnimator;
    }


    public synchronized ValueAnimator moveVechile(final LatLng finalPosition, final LatLng startPosition) {

        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.setDuration(3000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float t = Float.parseFloat(valueAnimator.getAnimatedValue().toString());

                LatLng currentPosition = new LatLng(
                        startPosition.latitude * (1 - t) + (finalPosition.latitude) * t,
                        startPosition.longitude * (1 - t) + (finalPosition.longitude) * t);
                marker.setPosition(currentPosition);


            }
        });

        return valueAnimator;
    }

}
