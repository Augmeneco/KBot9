import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

fun main(args: Array<String>) {
    Plugins.loadPlugins()
    val telegram = Telegram()

    println("KBot started")

    while(true){
        for (update in telegram.getUpdates()){
            val msg = Utils.Msg().parseUpdate(update)

            if (msg.isCommand){
                Plugins.pluginsMap[msg.command]?.main(msg)
            }
        }
    }
}