package com.example.ciaran.songle

import com.google.android.gms.maps.GoogleMap

/**
 * Created by ciaran on 12/12/2017.
 */
class Map{
    var map: GoogleMap? = null
    var Num: String ?= null
    var option: Int = 1 //option indicates the version of maps


    constructor(map: GoogleMap,  mapNum: String, option: Int){
        this.map = map
        this.Num = mapNum
        this.option = option
    }
}