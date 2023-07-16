import org.json.JSONArray
import org.json.JSONObject
import java.security.Key

class Telegram {
    var token: String
    var updateId: Int = -1
    var requests = Utils.Requests()

    var botId: Long = -1

    init {
        token = Utils.config.telegramToken

        botId = (sendMethod("getMe") as JSONObject).getJSONObject("result").getLong("id")
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
            if (update.has("callback_query")){
                yield((update))
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

    fun sendMethod(method: String, params: MutableMap<Any, Any> = mutableMapOf()): Any{
        val result = requests.post(
            "https://api.telegram.org/$token/$method",
            params
        ).json()

        return result
    }

    fun editMessage(text: String, chatId: Long, msgId: Long, params: MutableMap<Any, Any> = mutableMapOf()) {
        params.put("message_id", msgId)
        sendMessage(text, chatId, params, method = "editMessageText")
    }

    fun sendMessage(text: String, chatId: Long, params: MutableMap<Any, Any> = mutableMapOf(), method: String = "sendMessage"): JSONObject {
        var mutableText = text
        params["chat_id"] = chatId

        //if (!params.contains("parse_mode"))
        //    params.put("parse_mode", "HTML")

        var result = JSONObject()

        for (chunk in Utils.chunks(mutableText, 4096)){
            params["text"] = chunk
            if (params["text"] == "") params["text"] = " "

            result = requests.post(
                "https://api.telegram.org/$token/$method",
                params
            ).json() as JSONObject
            if (result.has("error_code")){
                println(result)
                sendMessage("Ошибка: ${result.getInt("error_code")}\n${result.getString("description")}", chatId)
            }
        }

        return result
    }

    class Keyboard{
        var oneIime = false
        var inline = false
        lateinit var buttons: JSONArray

        //reply buttons
        fun setButtons(buttonsList: MutableList<MutableList<String>>): Keyboard{
            buttons = JSONArray()
            inline = false

            for (col in buttonsList){
                val buttonsLine = JSONArray()
                for (row in col){
                    buttonsLine.put(row)
                }
                buttons.put(buttonsLine)
            }

            return this
        }

        //inline buttons
        fun setInlineButtons(buttonsList: MutableList<MutableList<MutableMap<String, String>>>): Keyboard{
            buttons = JSONArray()
            inline = true

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
        fun isInline(mode: Boolean): Keyboard{
            inline = mode
            return this
        }

        fun build(): JSONObject{
            val keyboardObj = JSONObject()
            keyboardObj.put("one_time_keyboard", oneIime)

            if (inline)
                keyboardObj.put("inline_keyboard", buttons)
            else
                keyboardObj.put("keyboard", buttons)

            keyboardObj.put("resize_keyboard", true)

            return keyboardObj
        }
    }

}