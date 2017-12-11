package com.example.ciaran.songle

/**
 * Created by ciaran on 11/12/2017.
 */

class Song{

    var number: Int = 0
    var artist: String? = null
    var title: String? = null
    var link: String? = null

    constructor(number:Int, artist: String, title: String, link: String){
        this.number = number
        this.artist = artist
        this.title = title
        this.link = link

    }


}