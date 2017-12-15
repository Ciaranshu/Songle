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
 * Created by ciaran on 12/12/2017.
 */
class DownloadLyrics: AsyncTask<String, Int, String>() {

    var context: Context? = null

    override fun onPreExecute() {
        super.onPreExecute()
        Log.d("DownloadXml", "Start")
    }


    override fun doInBackground(vararg urls: String): String {
        return try {
            Log.d("DownloadXml", "Inbackground")
            loadXmlFromNetwork(urls[0])
        } catch (e: IOException) {
            "Unable to load content. Check your network connection"
        } catch (e: XmlPullParserException) {
            "Error parsing XML"
        }

    }

    private fun loadXmlFromNetwork(urlString: String): String {


        val stream:InputStream = downloadUrl(urlString) // Download TXT file of lyrics

        return stream.bufferedReader().use { it.readText() } // parse the download TXT file from stream to string
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


    //任务执行完了后执行
    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        Toast.makeText(context,"Finish Loading Songs",Toast.LENGTH_LONG).show()
    }
}