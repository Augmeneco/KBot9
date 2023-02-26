import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

fun main(args: Array<String>) {
    val telegram = Telegram()

    while(true){
        for (i in telegram.getUpdates()){
            println(i)
        }
    }
}