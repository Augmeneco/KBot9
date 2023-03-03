import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.util.*

object Utils{
    class Config{
        var telegramToken: String
        var names: MutableList<String>
        var configJSON: JSONObject

        init{
            configJSON = JSONObject(File("data/config.json").readText())
            names = configJSON.getJSONArray("names").toMutableList().map{
                it.toString()
            }.toMutableList()
            telegramToken = "bot"+configJSON.getString("telegram_token")
        }
    }
    var config: Config = Config()
    var telegram: Telegram = Telegram()
    val isCmdRegex = Regex("""^/?\s?(?<botName>${config.names.joinToString("|")})?\s?""", RegexOption.IGNORE_CASE)

    class Msg {
        var text: String = ""
        var chatId: Int = 0
        var fromId: Int = 0
        var files: List<Any> = emptyList()
        var argv: List<String> = emptyList()
        var userText: String = ""
        var data: Any? = null
        var reply: Msg? = null
        var isCommand: Boolean = false
        var command: String = ""

        fun toMap(): MutableMap<String, Any>{
            val out = mutableMapOf(
                "text" to text,
                "chatId" to chatId,
                "fromId" to fromId,
                "files" to files,
                "argv" to argv,
                "userText" to userText,
                "data" to data,
                "reply" to reply,
                "command" to command,
                "isCommand" to isCommand
            ) as MutableMap<String, Any>

            return out
        }

        fun parseCommand(){

            val prefix = isCmdRegex.find(this.text)?.value
            if (prefix == "" || prefix == null){
                return
            }

            val argv = isCmdRegex.replaceFirst(this.text, "").split(" ").toMutableList()
            val command = argv[0].lowercase()


            if (!Plugins.pluginsMap.contains(command)){
                this.argv = argv
                this.userText = argv.joinToString()
                this.command = this.userText
                return
            }

            argv.removeAt(0)

            this.command = command
            this.isCommand = true
            this.argv = argv
            this.userText = argv.joinToString()

            return
        }

        fun parseUpdate(update: JSONObject): Msg{
            if (update.has("reply_to_message")){
                //parseUpdate(update.getJSONObject("reply_to_message"))
                this.reply = Msg().parseUpdate(update.getJSONObject("reply_to_message"))
            }

            if (update.has("text")){
                this.text = update.getString("text")
            }

            if (update.has("photo")){
                //val fileId = update.getJSONArray("photo").toList()

                if (update.has("caption")){
                    this.text = update.getString("caption")
                }
            }

            this.chatId = update.getJSONObject("chat").getInt("id")
            this.fromId = update.getJSONObject("from").getInt("id")

            this.parseCommand()
            return this
        }

        fun sendMessage(text: String): Any{
            return Utils.telegram.sendMessage(text, this.chatId)
        }
    }

    class Requests{
        fun post(url: String, params: MutableMap<Any, Any>): RequestResponse{
            val (request, response, result) = Fuel.post(url, map2List(params)).response()

            val requestResponse = RequestResponse()
            requestResponse.content = result.get()
            requestResponse.text = String(requestResponse.content)
            requestResponse.status = response.statusCode

            return requestResponse
        }

        class RequestResponse{
            lateinit var content: ByteArray
            lateinit var text: String
            var status: Int = 0

            fun json(): Any{
                return try {
                    JSONObject(text)
                } catch (e: Exception) {
                    JSONArray(text)
                }
            }
        }
    }

    fun map2List(map: MutableMap<Any, Any>): List<Pair<String, Any>>{
        return map.toList().map { (key, value) -> key.toString() to value }
    }
}

var config = Utils.Config()