package com.example.ciaran.songle

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import java.net.URL



import com.google.maps.android.data.kml.KmlLayer
import com.google.maps.android.data.kml.KmlPlacemark
import com.google.maps.android.data.kml.KmlPoint
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.io.*
import java.net.HttpURLConnection
import java.util.*

class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    val PERMISSIONS_REQUESR_ACESS_FINE_LOCATION = 1
    var mLocationPermissionGranted = false 
    private lateinit var mLastLocation : Location
    val TAG = "MapsActivity"
    var Markers:Iterable<KmlPlacemark> ?= null
    var gameStarted:Boolean = false
    var lyrics:String ?= null
    var songList:List<Song>? = null
    var gameMode:Int = 1 // 1:esay mode, 2:moderate mode, 3:hard mode


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Game Start!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            if (!gameStarted) {
                Snackbar.make(view, "Game Start!", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                onGameBegin()
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    override fun onStart() {
        super.onStart()
        mGoogleApiClient.connect()
        var refresh = DownloadSongs()
        refresh.context = this
        songList = refresh.execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/songs.xml").get()

    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient.isConnected) {
            mGoogleApiClient.disconnect()
        }
    }

    fun createLocationRequest() {

        // Set the parameters for the location request
        val mLocationRequest = LocationRequest()
            mLocationRequest.interval = 5000 // preferably every 5 seconds
            mLocationRequest.fastestInterval = 1000 // at most every second
            mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // Can we access the user’s current location?

        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this)
        }

    }

    fun onGameBegin() {
        //randomly choose the a song from the song list
        var Num:Int = rand(1,songList!!.size)
        var songNum:String ?= null
        if (Num!! < 10){
            songNum = "0" + Num.toString()
        }else{
            songNum = Num.toString()
        }

        //randomly choose the option of map
        var option:Int = rand(4,6) - gameMode


        //generate current Map object according to songNum and mode option
        var currenMap = Map(mMap,songNum,option)

        //loading the maps
        gameStarted = true
        progressBar?.max = 50
        var refreshMap = DownloadMap()
        refreshMap.progressBar = progressBar
        refreshMap.context = this

        var layer:KmlLayer = refreshMap.execute(currenMap).get()
        layer.addLayerToMap()
        for (i in layer.containers){
            this.Markers = i.placemarks
        }


        //loading the according lyrics
        var refreshLyrics = DownloadLyrics()
        refreshLyrics.context = this
        lyrics  = refreshLyrics.execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+songNum+"/lyrics.txt").get()

    }

    val random = Random()

    fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }


    override fun onConnected(connectionHint : Bundle?) {

        try { createLocationRequest(); } catch (ise : IllegalStateException) {

            println("IllegalStateException thrown [onConnected]"); } // Can we access the user’s current location?

        if (ContextCompat.checkSelfPermission(this,

                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

           // mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUESR_ACESS_FINE_LOCATION); }

    }

    override fun onLocationChanged(current: Location?) {


        if (current == null) {
            println("[onLocationChanged] Location unknown")
        }
        else if (gameStarted == true){
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(current.latitude,current.longitude), 17F))
            CollectMarker(current.latitude, current.longitude)
        }else {
            println("""[onLocationChanged] Lat/long now

            (${current.getLatitude()},

            ${current.getLongitude()})"""

            )

        }

    }

    fun CollectMarker(latitude: Double, longitude: Double):  Boolean {
        var min = 3F //minimum collection distance is 3 meters
        var markLat:Double = 0.0
        var markLng:Double = 0.0
        var ob: String? = null
        val results = FloatArray(10)
        var lyricsY:Int = 0
        var lyricsX: Int = 0

        //judge if the distance between current position and any markers is less than the minimum distance for successful collection
        for (i in this.Markers!!){
            if (i.hasGeometry()){
                ob = i.geometry.geometryObject.toString()
                markLat = ob.split("(", ",", ")")[1].toDouble()
                markLng = ob.split("(", ",", ")")[2].toDouble()
                Location.distanceBetween(latitude, longitude, markLat,markLng,results)
                if (results[0] < min) {
                    lyricsY = i.properties.toList()[0].toString().split("=",":")[1].toInt()
                    lyricsX = i.properties.toList()[0].toString().split("=",":")[2].toInt()

                }
            }

        }

        return false
    }



    override fun onConnectionSuspended(ﬂag : Int) {
        println(" >>>> onConnectionSuspended")
    }

    override fun onConnectionFailed(result : ConnectionResult) {
        println(" >>>> onConnectionFailed")
    }


    override fun onMapReady(googleMap: GoogleMap) {
        println(">>>>> [$TAG] onMapReady")
        mMap = googleMap

        // Move and zoom the camera
        val centre = LatLng(55.944424999999995, -3.188396)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centre, 17F))

        try {
            // Visualise current position with a small blue circle
            mMap.isMyLocationEnabled = true

            // Add "My location" button to the user interface
            mMap.uiSettings.isMyLocationButtonEnabled = true

            // Add [+] and [-] zoom controls
            mMap.uiSettings.isZoomControlsEnabled = true

        } catch (se : SecurityException) {
            println(">>>>> [$TAG] Security exception thrown [onMapReady]")
        }


        //val layer = KmlLayer(mMap, kmlInputStream, this)
        //layer.addLayerToMap()

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }

            R.id.nav_manage -> {

            }

        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}


