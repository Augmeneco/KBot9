import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

fun main(args: Array<String>) {
    val telegram = Telegram()

    while(true){
        for (update in telegram.getUpdates()){
            val msg = Utils.Msg().parseUpdate(update)

            if (msg.isCommand && msg.command == "стат"){
                msg.sendMessage("хуй соси")
            }
        }
    }
}