package com.example.ciaran.songle

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import com.google.maps.android.data.kml.KmlLayer
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputEditText
import pl.hypeapp.materialtimelinesample.adapter.TimelineRecyclerAdapter
import pl.hypeapp.materialtimelinesample.model.Timepoint
import pl.hypeapp.materialtimelinesample.model.Word
import java.util.*


class MainActivity : AppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private lateinit var mMap: GoogleMap
    private lateinit var mGoogleApiClient: GoogleApiClient
    private val PERMISSIONS_REQUESR_ACESS_FINE_LOCATION = 1
    private val TAG = "MapsActivity" // for logging
    private val markerList = mutableListOf<Marker>()

    private var lyrics:String ?= null // store the whole lyrics of a given song
    private var songList:List<Song>? = null //store all available songs
    private var layer:KmlLayer ?= null
    private val random = Random()
    private var answer:String ?= null //store the answer of song name
    private lateinit var timelineRecyclerAdapter: TimelineRecyclerAdapter
    private lateinit var countDownTimer: CountDownTimer

    //FLAG values of game status
    private var GAMESTATUS:Boolean = false // True: Game started, False: Game stopped
    private var GAMEMODE:Int = 1 // 1:esay mode, 2:moderate mode, 3:hard mode
    private var TIMING:Boolean = true //True: Timing mode, False: Not Timing mode
    private var GAMETIME:Long = 60 //1:esay mode: 60 mins , 2:moderate mode: 45 mins, 3:hard mode: 30 mins


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        createTimeline() // create the time line for word collection
        Timer_progress.visibility  = View.INVISIBLE // make the progress bar invisible


        //use the floating button to start and end game
        fab.setOnClickListener { view ->

            if (!GAMESTATUS) {
                onGameBegin() // Start game when it hasn't started
            }
            else{
                alert {
                    title = "Have a guess?"
                    message = "Please enter you answer here:"

                    customView {
                       var input = textInputEditText()
                        positiveButton("Confirm") {
                            if (input.text.toString() == answer){
                                toast("Correct! Congratulations!")
                                onGameStop() // if guess is correct, game stops
                            }
                            else{
                                toast("Guess incorrect!") // if guess is wrong, game resumes
                            }
                        }
                        negativeButton("Give Up!") {
                            onGameStop() // End game when it has started
                        }

                    }

                }.show()

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

        // refresh the list of available songs when application starts each time
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


    // Game logic
    private fun onGameBegin() {
        //randomly choose a song from the song list
        val songNum = chooseSong()

        //randomly choose the option of map
        var option:Int = rand(4,6) - GAMEMODE

        //generate current Map object according to songNum and mode option
        var currenMap = Map(mMap,songNum,option)

        //load the maps
        loadMap (currenMap)

        //parse to marker
        parseToMarkers()

        //set Timer
        setTimer()

        //loading the lyrics accord to selected song
        var refreshLyrics = DownloadLyrics()
        refreshLyrics.context = this
        lyrics  = refreshLyrics.execute("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+songNum+"/lyrics.txt").get()

    }

    private fun onGameStop(){
        Timer_progress.visibility  = View.INVISIBLE 
        GAMESTATUS = false
        mMap.clear()    //clear the markers on map
        markerList.clear()   //clear the marker list
        createTimeline()
        if(TIMING){
        countDownTimer.cancel() //reset the timer
        }



    }
    
    // generate random number from specfic range
    private fun rand(from: Int, to: Int) : Int {
        return random.nextInt(to - from) + from
    }

    //randomly choose a song from song list
    private fun chooseSong() : String {
        var Num:Int = rand(1,songList!!.size)

        var songNum: String?
        if (Num < 10){
            songNum = Num.toString().padStart(2,'0')
        }else{
            songNum = Num.toString()
        }
        
        //acquire the name of selected song as the answer 
        answer = songList!![Num - 1].title

        return songNum

    }

    private fun loadMap(map:Map){
        GAMESTATUS = true
        
        //load map according to the attribute of Map object
        var refreshMap = DownloadMap()
        refreshMap.context = this
        layer = refreshMap.execute(map).get() 

        layer?.addLayerToMap()
        layer?.removeLayerFromMap()

    }

    private fun parseToMarkers() {
        for (x in layer!!.containers) {
            for (i in x.placemarks.toList()) {
                if (i.hasGeometry()) {
                    val ob = i.geometry.geometryObject.toString()
                    val markLat = ob.split("(", ",", ")")[1].toDouble()
                    val markLng = ob.split("(", ",", ")")[2].toDouble()
                    val lyricsY = i.properties.toList()[0].toString().split("=", ":")[1] // acquire the number of given line
                    val lyricsX = i.properties.toList()[0].toString().split("=", ":")[2] // acquire the number of given word
                    val description = i.properties.toList()[1].toString().split("=")[1] //acquire the description of given word
                    var icon: BitmapDescriptor? = null

                    // change the icon of marker according to its description
                    if (description == "unclassified") {
                        icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.pin_unclassified))
                        
                    } else if (description == "boring") {
                        icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.pin_boring))
                        
                    } else if (description == "notboring") {
                        icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.pin_not_boring))
                        
                    } else if (description == "interesting") {
                        icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.pin_intersting))
                        
                    } else if (description == "veryinteresting") {
                        icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.drawable.pin_very_interesting))

                    }

                    // add markes to the map and store the return value of addMarker function into markerList as reference
                    var marker: Marker = mMap.addMarker(MarkerOptions().position(LatLng(markLat, markLng))
                            .title(description)
                            .draggable(false)
                            .visible(true).icon(icon).snippet(lyricsY + ":" + lyricsX + ":" + description))

                    markerList.add(marker)

                }

            }

        }
    }

    private fun setTimer(){
        if (TIMING) {
            //set countdown timer

            Timer_progress.visibility = View.VISIBLE
            Timer_progress.max = (GAMETIME*60).toInt() // accroding to the difficulty mode
            toast("You got $GAMETIME minutes to finish the game!")

            countDownTimer = object : CountDownTimer(GAMETIME * 60 * 1000, 100) {
                override fun onTick(millisUntilFinished: Long) {

                    Timer_progress.progress = ((millisUntilFinished / 1000)).toInt()

                    if ((millisUntilFinished / (1000*60)).toInt() == 10){
                        toast("You only got 10 minutes left!") // notify user when it's only 10 minutes left
                    }
                }

                override fun onFinish() {
                    onGameStop() //stop the game when time ups
                    toast("Time over! Failed to guess the song!")
                }
            }.start()
        }
    }

    //create the timeline view for storage of collected word
    private fun createTimeline(){
        timelineRecyclerAdapter = TimelineRecyclerAdapter()
        word_list.layoutManager = LinearLayoutManager(this)
        word_list.adapter = timelineRecyclerAdapter

        timelineRecyclerAdapter.addWeather(Word("Words are collected here:","Please enjoy the game!",false))
    }

    //functions regarding network connection and  google map service
    override fun onConnected(connectionHint : Bundle?) {

        try { createLocationRequest();

        } catch (ise : IllegalStateException) {

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
        // if game has started,
        else if (GAMESTATUS == true){
            //automatically move the camera to track the position of user
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(current.latitude,current.longitude), 17F))

            //check if there is any word can be collected at current position
            collectWords(current.latitude, current.longitude)
        }else {
            println("""[onLocationChanged] Lat/long now

            (${current.getLatitude()},

            ${current.getLongitude()})"""

            )
        }

    }

    private fun collectWords(latitude: Double, longitude: Double) {
        val min = 15F //minimum collection distance is 15 meters
        val results = FloatArray(10)

        //judge if the distance between current position and any markers is less than the minimum distance for successful collection
        for (i in this.markerList){
            //get the estimate distance from user to each markers
            Location.distanceBetween(latitude, longitude, i.position.latitude, i.position.longitude,results)
                //if any distance is less than preset minimum value, notify user to collect
                if (results[0] < min) {
                    val lyricsY = i.snippet.split(":")[0].toInt() // number of line
                    val lyricsX = i.snippet.split(":")[1].toInt() // number of word
                    var answer = lyrics?.split('\n')?.get(lyricsY-1)?.split(" ", ", ", ".")?.get(lyricsX-1) // collected word
                    alert("you successfully found one word:\n") {
                        title = "Congratulations!"
                        customView {
                            textView {
                                text = answer
                                textSize = 23f
                                textColor = Color.parseColor("#56b994")
                                gravity = Gravity.CENTER_HORIZONTAL
                                typeface = Typeface.DEFAULT_BOLD
                            }

                            positiveButton("Collect") {
                                i.remove()
                                markerList.remove(i)

                                val collectWordView = TextView(this@MainActivity)
                                collectWordView.textSize = 22f
                                collectWordView.text = answer

                                val current = Calendar.getInstance()

                                val time: String = current.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0') + ":" + current.get(Calendar.MINUTE).toString().padStart(2, '0')
                                val date: String = current.get(Calendar.DAY_OF_MONTH).toString() + "/" + (current.get(Calendar.MONTH)+1).toString()

                                //add collected word to the timeline view
                                timelineRecyclerAdapter.addTimepoint(Timepoint(time, date))
                                timelineRecyclerAdapter.addWeather(Word(answer!!, i.snippet.split(":")[2], false))

                                toast("Successfully collected!")
                            }
                            negativeButton("Skip") { toast("Maybe collect it later!")}

                        }
                    }.show()

                    break
                }
        }


}

    private fun createLocationRequest() {

        // Set the parameters for the location request
        val mLocationRequest = LocationRequest()
        mLocationRequest.interval = 5000 // preferably every 5 seconds
        mLocationRequest.fastestInterval = 1000 // at most every second
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        // check if we can access the user’s current location

        val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,this)
        }

    }

    override fun onConnectionSuspended(ﬂag : Int) {
        println(" >>>> onConnectionSuspended")
        onGameStop()
    }

    override fun onConnectionFailed(result : ConnectionResult) {
        println(" >>>> onConnectionFailed")
        onGameStop()
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

    //userinterface logics
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
            R.id.game_mode ->{
                if (!GAMESTATUS){
                    val modes = listOf("Easy", "Moderate", "Hard")
                    selector("Please select the game mode:", modes, { dialogInterface, i ->
                        run {
                            toast("You are playing with ${modes[i]} mode!")
                            GAMEMODE = i + 1
                            GAMETIME = (60 - i*15).toLong()
                        }
                    })
                }
                else{
                    toast(("Please change mode after finishing the game!"))
                    return true
                }

            }

            R.id.timing_switch -> {
                if (!GAMESTATUS){
                    val modes = listOf("Timing", "Not Timing")
                    selector("Please select the timing mode:", modes, { dialogInterface, i ->
                        run {
                            toast("You are playing with ${modes[i]} mode!")
                            TIMING = i == 0
                        }
                    })
                }
                else{
                    toast(("Please change mode after finishing the game!"))
                    return true
                }
            }
            
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)

            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {


        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}


