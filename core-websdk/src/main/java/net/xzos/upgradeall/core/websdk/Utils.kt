package net.xzos.upgradeall.core.websdk

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


fun getJsonMap(json:String):Map<String,String>{
    val type = object : TypeToken<Map<String, String>>() {}.type
    return Gson().fromJson(json, type)
}
