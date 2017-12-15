package com.example.ciaran.songle

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import com.google.maps.android.data.kml.KmlLayer
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


/**
 * Created by ciaran on 11/12/2017.
 */
class DownloadMap() : AsyncTask<Map, Int, KmlLayer>() {

    var context: Context? = null


    override fun onPreExecute() {
        super.onPreExecute()
        Log.d("DownloadXml", "Start")

    }

    override fun doInBackground(vararg params: Map?): KmlLayer {
        Log.d("DownloadXml", "In background")

        var layer:KmlLayer ?= null
        try {
            //Download the KML file from URL into KMLStream
            val kmlInputStream =downloadUrl("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/"+params[0]?.Num+"/map"+params[0]?.option.toString()+".kml")
            layer = KmlLayer(params[0]?.map, kmlInputStream, context)
            return layer

        } catch (e: IOException) {
            Log.d("DownloadSongs","Unable to load content. Check your network connection")
        }
        return layer!!

    }

    @Throws(IOException::class)
fun downloadUrl(urlString: String): InputStream {

        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        // Also available: HttpsURLConnection

        conn.readTimeout = 10000 // milliseconds
        conn.connectTimeout = 15000 // milliseconds
        conn.requestMethod = "GET"
        conn.doInput = true

        // Starts the query
        conn.connect()
        return conn.inputStream

    }

    //Execute when the task is done
    override fun onPostExecute(result: KmlLayer?) {
        super.onPostExecute(result)

        //Notify the user that songs have been download successfully
        Toast.makeText(context,"Finish Loading Maps", Toast.LENGTH_LONG).show()
    }


}
