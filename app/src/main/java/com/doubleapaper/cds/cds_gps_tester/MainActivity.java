package com.doubleapaper.cds.cds_gps_tester;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends Activity {

    private String TAG ="CDS GPS Tester";
    Boolean isOnPause = false;

    /* Location service */
    private boolean gps_enabled; /* GPS Enable status */
    private LocationManager locManager; /* GPS Location manager */

    /* GPS */
    private static final long MIN_TIME_BW_UPDATES = 500;
    private static final float MIN_DISTANCE_UPDATES = (float) 0;
    // network provider
    private static final long MIN_TIME_BW_UPDATES_NETWORK = 500;

    /* Image capture info */
    private double CurrentLat = 0; /* Current GPS position */
    private double CurrentLon = 0; /* Current GPS position */
    private double CurrentAlt = 0; /* Current GPS position */
    private long CurrentTime = 0; /* Current GPS time */
    private double AddressLat = 0; /* Last location position */
    private double AddressLon = 0; /* Last location position */
    private double CaptureLat = 0; /* Capture position */
    private double CaptureLon = 0; /* Capture position */
    private double CaptureAlt = 0; /* Capture position */
    private long CaptureTime = 0; /* Capture time */

    TextView txtStatus;
    ListView listResult;

    ArrayList<String> list;

    /** Declaring an ArrayAdapter to set items to ListView */
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtStatus= (TextView)findViewById(R.id.txtStatus);
        listResult = (ListView)findViewById(R.id.listResult);

        txtStatus.setText("- Try to find gps service -");


        list = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list);
listResult.setAdapter(adapter);
        /* Initial location service */
        CheckGPSStatus();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void CheckGPSStatus() {
        Log.i(TAG, "=========> CheckGPSStatus");
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locManager.removeUpdates(networkLocationListener);
        locManager.removeUpdates(gpsLocationListener);

        if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES_NETWORK, MIN_DISTANCE_UPDATES,
                    networkLocationListener);
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_UPDATES,
                    gpsLocationListener);

            gps_enabled = true;

        } else if (locManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && !locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES_NETWORK, MIN_DISTANCE_UPDATES,
                    networkLocationListener);

            gps_enabled = true;

        } else if (!locManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                && locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES, MIN_DISTANCE_UPDATES,
                    gpsLocationListener);

            gps_enabled = true;

        } else {
            txtStatus.setText("GPS Not Found");
            gps_enabled = false;

            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("Attention!");
            dialog.setMessage("Enable GPS before start application");

            dialog.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            finish();
                        }
                    });

            dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface arg0, int keyCode,
                                     KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        finish();
                    }
                    return true;
                }
            });

            dialog.create().show();
        }
    }


    private final LocationListener gpsLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    gps_enabled = false;
                    txtStatus.setText("Location out of service.");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    gps_enabled = false;
                    txtStatus.setText("Location found by gps service with unstable signal.");
                    break;
                case LocationProvider.AVAILABLE:
                    gps_enabled = true;
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            locManager.removeUpdates(networkLocationListener);
            if (locManager != null) {

                location = locManager
                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);



                if (location != null) {
                    CurrentLat = location.getLatitude();
                    CurrentLon = location.getLongitude();
                    CurrentAlt = location.getAltitude();
                    CurrentTime = location.getTime();

                    txtStatus.setText("Location found by gps service.");

                    list.add("Location : "+location.getLatitude()+", "+ location.getLongitude() +" Accu : "+location.getAccuracy() + " m.");
                    adapter.notifyDataSetChanged();

                    // Toast.makeText(getApplicationContext(),
                    // "GPS - รัศมีความแม่นยำ " + location.getAccuracy() + " m."
                    // + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new
                    // Date(CurrentTime)),
                    // Toast.LENGTH_SHORT).show();
                }
            }

			/* Calculate current position from start position */
            float dx = get_x_distance(CurrentLat, AddressLat); /*
																		 * X
																		 * axis
																		 */
            float dy = get_y_distance(CurrentLon, AddressLon); /*
																		 * Y
																		 * axis
																		 */
            float ds = (float) (Math.sqrt((dx * dx) + (dy * dy)));

			/* Update address if moving over 500 m */

            // joke
            if (ds == 0) {
                if (Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ALLOW_MOCK_LOCATION).equals("1")) {
                    // Log.d("Oceanus","ALLOW_MOCK_LOCATION return true");
                    TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    PackageInfo pInfo = null;
                    try {
                        pInfo = getPackageManager().getPackageInfo(
                                getPackageName(), 0);
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    // Toast.makeText(getApplicationContext(),
                    // "Open Use Fake GPS", Toast.LENGTH_LONG).show();
                } else {
                    // Log.d("Oceanus","ALLOW_MOCK_LOCATION return false");
                }

            }

            // Log.d("Oceanus", "========== onLocationChanged ds: " + ds);
            if (ds >= MIN_DISTANCE_UPDATES  && !isOnPause) {
                if (CheckInternetStatus(getApplicationContext()) == true) {
					/* Update last get location */
                    AddressLat = CurrentLat;
                    AddressLon = CurrentLon;

                } else { /*
						  Toast.makeText(getApplicationContext(),
						  "Cannot connect internet", Toast.LENGTH_LONG).show();
						 */
                }
            }
        }
    };


    private final LocationListener networkLocationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    gps_enabled = false;
                    txtStatus.setText("Location out of service.");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    gps_enabled = false;
                    txtStatus.setText("Location found by location service with unstable signal.");
                    break;
                case LocationProvider.AVAILABLE:
                    gps_enabled = true;
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onLocationChanged(Location location) {
            if (locManager != null) {
                location = locManager
                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    CurrentLat = location.getLatitude();
                    CurrentLon = location.getLongitude();
                    CurrentAlt = location.getAltitude();
                    CurrentTime = location.getTime();

                    txtStatus.setText("Location found by location service.");
                    list.add("Location : "+location.getLatitude()+", "+ location.getLongitude() +" Accu : "+location.getAccuracy() + " m.");
                    adapter.notifyDataSetChanged();
                    // Toast.makeText(getApplicationContext(),
                    // "NETWORK - รัศมีความแม่นยำ " + location.getAccuracy() +
                    // " m." + new
                    // SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new
                    // Date(CurrentTime)),
                    // Toast.LENGTH_SHORT).show();
                }
            }

			/* Calculate current position from start position */
            float dx = get_x_distance(CurrentLat, AddressLat); /*
																		 * X
																		 * axis
																		 */
            float dy = get_y_distance(CurrentLon, AddressLon); /*
																		 * Y
																		 * axis
																		 */
            float ds = (float) (Math.sqrt((dx * dx) + (dy * dy)));

			/* Update address if moving over 500 m */

            // joke
            if (ds == 0) {
                if (Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ALLOW_MOCK_LOCATION).equals("1")) {
                    // Log.d("Oceanus","ALLOW_MOCK_LOCATION return true");
                    TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    PackageInfo pInfo = null;
                    try {
                        pInfo = getPackageManager().getPackageInfo(
                                getPackageName(), 0);
                    } catch (PackageManager.NameNotFoundException e) {
                    }

                    // Toast.makeText(getApplicationContext(),
                    // "Open Use Fake GPS", Toast.LENGTH_LONG).show();
                } else {
                    // Log.d("Oceanus","ALLOW_MOCK_LOCATION return false");
                }

            }

            // Log.d("Oceanus", "========== onLocationChanged ds: " + ds);
            if (ds >= MIN_DISTANCE_UPDATES && !isOnPause) {
                if (CheckInternetStatus(getApplicationContext()) == true) {
					/* Update last get location */
                    AddressLat = CurrentLat;
                    AddressLon = CurrentLon;
                } else { /*
						 * Toast.makeText(getApplicationContext(),
						 * "Cannot connect internet", Toast.LENGTH_LONG).show();
						 */
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();


        if (locManager != null && gps_enabled) {
            isOnPause = true;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        listResult.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Log.d( "Oceanus", "onDestroy");

        locManager.removeUpdates(networkLocationListener);
        locManager.removeUpdates(gpsLocationListener);

		/* StartCDS_BackGroundTask(); */
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public static boolean CheckInternetStatus( Context c )
    {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo    = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting())
        { return true; }

        return false;
    }

    public static float get_x_distance( double lat1, double lat2 )
    {
        float results[] = new float[3];
        Location.distanceBetween(lat1, 0, lat2, 0, results);

        float dir = results[0];

        if( lat1 > lat2 )
        { dir = -dir; }

        return dir;
    }

    public static float get_y_distance( double lon1, double lon2 )
    {
        float results[] = new float[3];
        Location.distanceBetween(0, lon1, 0, lon2, results);

        float dir = results[0];

        if( lon1 > lon2 )
        { dir = -dir; }

        return dir;
    }

}
