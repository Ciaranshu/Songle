package com.example.ciaran.songle

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.data.kml.KmlLayer
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by ciaran on 12/12/2017.
 */
class DownloadLyrics: AsyncTask<String, Int, String>() {
    var progressBar: ProgressBar? = null
    var context: Context? = null
    //任务执行之前开始调用此方法，可以在这里显示进度对话框。


    override fun onPreExecute() {
        super.onPreExecute()
        Log.d("DownloadXml", "Start")
    }
    //此方法在后台线程 执行，完成任务的主要工作，通常需要较长的时间。
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


        var stream:InputStream = downloadUrl(urlString) // Do something with stream e.g. parse as XML, build result

        return stream.toString()
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


    //更新UI
    override fun onProgressUpdate(vararg values: Int?) {
        super.onProgressUpdate(*values)

        //若有复杂逻辑，可以增加异常捕捉
        progressBar?.progress = values?.get(0) ?: 0
    }

    //任务执行完了后执行
    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)

        Toast.makeText(context,"Finish Loading Songs",Toast.LENGTH_LONG).show()
    }
}