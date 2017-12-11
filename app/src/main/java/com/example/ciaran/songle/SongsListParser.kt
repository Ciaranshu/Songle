package com.example.ciaran.songle

import org.xmlpull.v1.XmlPullParser
import android.util.Xml
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream


/**
 * Created by ciaran on 11/12/2017.
*/

class SongsListParser{
    // We don’t use namespaces private val ns: String? = null

    @Throws(XmlPullParserException::class, IOException::class)
    fun parse(input : InputStream): List<Song> {

        input.use {

            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(input, null)
            parser.nextTag()
            return readFeed(parser)

        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readFeed(parser: XmlPullParser): List<Song> {

        val entries = ArrayList<Song>()
        parser.require(XmlPullParser.START_TAG, null, "Songs")
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            } // Starts by looking for the entry tag
            if (parser.name == "Song") {
                entries.add(readSong(parser))
            } else {
                skip(parser)
            }

        }
        return entries

    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readSong(parser: XmlPullParser): Song {

        parser.require(XmlPullParser.START_TAG, null, "Song")
        var number = 0
        var artist = ""
        var title = ""
        var link = ""
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG)
                continue
            when(parser.name){
                "Number" -> number = readNumber(parser)
                "Artist" -> artist = readArtist(parser)
                "Title" -> title = readTitle(parser)
                "Link" -> link = readLink(parser)
                else -> skip(parser)
            }

        }
        return Song(number,artist, title, link)

    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
    @Throws(IOException::class, XmlPullParserException::class)
    private fun readNumber(parser: XmlPullParser): Int {
        parser.require(XmlPullParser.START_TAG, null, "Number")
        val Number = readText(parser).toInt()
        parser.require(XmlPullParser.END_TAG, null, "Number")
        return Number
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readArtist(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "Artist")
        val Artist = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "Artist")
        return Artist
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readTitle(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "Title")
        val title = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "Title")
        return title
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun readLink(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, null, "Link")
        val Link = readText(parser)
        parser.require(XmlPullParser.END_TAG, null, "Link")
        return Link
    }



    @Throws(XmlPullParserException::class, IOException::class)
    private fun skip(parser: XmlPullParser) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

}