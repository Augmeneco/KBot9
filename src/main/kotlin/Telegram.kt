import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

import java.io.File

class Telegram {
    var config: JSONObject
    var token: String
    var updateId: Int = -1

    init {
        config = JSONObject(File("data/config.json").readText())
        token = "bot"+config.getString("telegram_token")
    }

    fun getUpdates(): Sequence<JSONObject> = sequence{
        val (request, response, result) = Fuel.post(
            "https://api.telegram.org/$token/getUpdates",
            listOf(
                "offset" to updateId
            )
        ).responseString()
        val updates = JSONObject(result.get()).getJSONArray("result")

        for (i in 0 until updates.length()){
            val update = updates.getJSONObject(i)

            updateId = update.getInt("update_id")+1
            if (update.has("message")){
                yield(update.getJSONObject("message"))
            }
        }
    }
}