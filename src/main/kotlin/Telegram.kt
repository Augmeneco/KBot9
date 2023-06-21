import org.json.JSONArray
import org.json.JSONObject

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

    fun sendChatAction(chatId: Long, action: String = "typing"){
        requests.post(
            "https://api.telegram.org/$token/sendChatAction",
            mutableMapOf(
                "chat_id" to chatId,
                "action" to action
            )
        )
    }

    fun sendMessage(text: String, chatId: Long, params: MutableMap<Any, Any> = mutableMapOf()) {
        params.putAll(
            mutableMapOf(
                "chat_id" to chatId,
                "parse_mode" to "HTML"
            )
        )

        for (chunk in Utils.chunks(text, 4096)){
            params["text"] = chunk
            if (params["text"] == "") params["text"] = " "

            val result = requests.post(
                "https://api.telegram.org/$token/sendMessage",
                params
            ).json()
        }
    }

    class Keyboard{
        var oneIime = false
        lateinit var buttons: JSONArray

        fun setButtons(buttonsList: MutableList<MutableList<String>>): Keyboard{
            buttons = JSONArray()

            for (col in buttonsList){
                val buttonsLine = JSONArray()
                for (row in col){
                    buttonsLine.put(row)
                }
                buttons.put(buttonsLine)
            }

            return this
        }

        fun isOneTime(mode: Boolean): Keyboard{
            oneIime = mode
            return this
        }

        fun build(): JSONObject{
            val keyboardObj = JSONObject()
            keyboardObj.put("one_time_keyboard", oneIime)
            keyboardObj.put("keyboard", buttons)
            keyboardObj.put("resize_keyboard", true)

            return keyboardObj
        }
    }

}