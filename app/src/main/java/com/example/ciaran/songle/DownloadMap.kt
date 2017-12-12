package com.example.ciaran.songle

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.data.Layer
import com.google.maps.android.data.kml.KmlLayer
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL



/**
 * Created by ciaran on 11/12/2017.
 */
class DownloadMap() : AsyncTask<GoogleMap, Int, List<KmlLayer>>() {

    var progressBar: ProgressBar? = null
    @SuppressLint("StaticFieldLeak")
    var context: Context? = null


    override fun doInBackground(vararg params: GoogleMap?): List<KmlLayer>? {
        Log.d("DownloadXml", "Inbackground")
        val layerList = ArrayList<KmlLayer>()
        for (i in 1 until 6){
            //publishProgress((i*10))
            var kmlInputStream =loadXmlFromNetwork("http://www.inf.ed.ac.uk/teaching/courses/cslp/data/songs/01/map"+i+".kml")
            var layer = KmlLayer(params[0], kmlInputStream, context)
            layerList.add(layer)
        }
        return layerList
    }


    //任务执行之前开始调用此方法，可以在这里显示进度对话框。
    override fun onPreExecute() {
        super.onPreExecute()
        Log.d("DownloadXml", "Start")
    }


    fun loadXmlFromNetwork(urlString: String): InputStream {

        var stream:InputStream = downloadUrl(urlString) // Do something with stream e.g. parse as XML, build result

        return stream
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
    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)

        //若有复杂逻辑，可以增加异常捕捉
        progressBar?.progress = values?.get(0) ?: 0
    }

    //任务执行完了后执行
    override fun onPostExecute(result: List<KmlLayer>?) {
        super.onPostExecute(result)
        Toast.makeText(context,"Finish Loading Maps", Toast.LENGTH_LONG).show()
    }


}
