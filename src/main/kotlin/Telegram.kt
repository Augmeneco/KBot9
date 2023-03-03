import org.json.JSONObject

import java.io.File

class Telegram {
    var token: String
    var updateId: Int = -1
    var requests = Utils.Requests()

    init {
        token = Utils.config.telegramToken
    }

    fun getUpdates(): Sequence<JSONObject> = sequence{
        val result = requests.post(
            "https://api.telegram.org/$token/getUpdates",
            mutableMapOf(
                "offset" to updateId
            )
        ).json() as JSONObject
        val updates = result.getJSONArray("result")

        for (i in 0 until updates.length()){
            val update = updates.getJSONObject(i)

            updateId = update.getInt("update_id")+1
            if (update.has("message")){
                yield(update.getJSONObject("message"))
            }
        }
    }

    fun sendMessage(text: String, chatId: Int, params: MutableMap<Any, Any> = mutableMapOf()): Any {
        params.putAll(
            mutableMapOf(
                "chat_id" to chatId,
                "text" to text,
                "parse_mode" to "HTML"
            )
        )

        return requests.post(
            "https://api.telegram.org/$token/sendMessage",
            params
        ).json()
    }
}