package com.example.ciaran.songle

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by ciaran on 11/12/2017.
 */
class DownloadSongs: AsyncTask<String, Int, List<Song>>() {

    var context: Context? = null

    override fun onPreExecute() {
        super.onPreExecute()
        Log.d("DownloadXml", "Start")

    }

    override fun doInBackground(vararg urls: String): List<Song>? {
        Log.d("DownloadXml", "Inbackground")
        return loadXmlFromNetwork(urls[0])

    }

    private fun loadXmlFromNetwork(urlString: String): List<Song>? {
        val Songlist = SongsListParser()
        var songs: List<Song>?=null
        try {
            var stream:InputStream = downloadUrl(urlString) // Get the stream of the XML file from the given URL
            songs = Songlist.parse(stream) // Parse the stream of XML file into a list of song with SongListParser
        } catch (e: IOException) {
            Log.d("DownloadSongs","Unable to load content. Check your network connection")
        } catch (e: XmlPullParserException) {
            Log.d("DownloadSongs", "Error parsing XML")
        }

        return songs
    }

    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {

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
    override fun onPostExecute(result: List<Song>?) {
        super.onPostExecute(result)

        //Notify the user that songs have been download successfully
        Toast.makeText(context,"Finish Loading Songs",Toast.LENGTH_LONG).show()
    }
}
