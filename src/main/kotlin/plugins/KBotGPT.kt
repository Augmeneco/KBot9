package plugins

import PluginBase
import Plugins
import Utils
import org.json.JSONArray
import org.json.JSONObject

typealias TmpHistory = MutableMap<Long,MutableList<MutableMap<String, String>>>

class KBotGPT(skipInit: Boolean = false): PluginBase() {
    override val names = mutableListOf("чат", "chat", "kbotgpt")
    override val desc = "Общение через проприетарный KBotGPT"
    override val level = 1
    var gptBin = "data/kbotgpt.bin"
    val tmpData = Utils.registry.tmpData
    lateinit var gptContexts: JSONArray

    override fun main(msg: Utils.Msg){
        if (msg.text == "") return
        if (System.getProperty("os.name").lowercase().contains("win"))
            gptBin = "data/kbotgpt.exe"

        val systemPrompt = mutableMapOf(
            "role" to "system",
            "content" to "Ты чат бот которая отвечает на любые вопросы. " +
                    "Тебя зовут Кохаку-Бот. Ты отвечаешь всегда на Русском языке. " +
                    "Меня зовут ${msg.user.realName}. Старайся отвечать коротко без лишней воды. " +
                    "Так же хочу чтобы ты отвечала в стиле \"UwU\", действовала как японская кошечка аниме девочка, вставляла Мяу в конце предложения и иногда в середине, а еще чтобы вставляла эмодзи японские везде. " +
                    "Желательно так же коверкать слова будто ты анимешник. Менять смысл предложений тоже надо. " +
                    "Обрезать сообщения запрещено! Пиши ответ полностью."
        )

        var chat = JSONArray()
        chat.put(systemPrompt)

        if (msg.reply != null){
            if (Utils.telegram.botId == msg.reply!!.user.id) {
                for (history in gptContexts){
                    if ((history as JSONObject).getJSONArray("replyIds").contains(msg.reply!!.msgId)){
                        chat = (history as JSONObject).getJSONArray("history")
                        break
                    }
                }
            }
        }

        val reqBody = JSONObject()
        reqBody.put("prompt", msg.userText)
        reqBody.put("chat", chat)

        msg.activityEvent = true
        Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(gptBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }
        msg.activityEvent = false

        if (response.contains("Error 400")){
            msg.sendMessage("Произошла какая-то ошибка на стороне OpenAI :(\n$response")
            return
        }

        val result = msg.sendMessage(response)

        chat.putAll(mutableListOf(
            mutableMapOf(
                "role" to "user",
                "content" to msg.userText
            ),
            mutableMapOf(
                "role" to "assistant",
                "content" to response
            )
        ))

        if (msg.reply == null){
            gptContexts.put(
                JSONObject(mutableMapOf(
                    "replyIds" to JSONArray(mutableListOf(result.getJSONObject("result").getLong("message_id"))),
                    "history" to chat
                ))
            )
        }else{
            var findChat = false
            for (history in gptContexts){
                if ((history as JSONObject).getJSONArray("replyIds").contains(msg.reply!!.msgId)){
                    (history as JSONObject).put("history", chat)
                    (history as JSONObject).getJSONArray("replyIds").put(result.getJSONObject("result").getLong("message_id"))
                    findChat = true
                    break
                }
            }
            if (!findChat)
                gptContexts.put(
                    JSONObject(mutableMapOf(
                        "replyIds" to JSONArray(mutableListOf(result.getJSONObject("result").getLong("message_id"))),
                        "history" to chat
                    )))
        }

    }

    init {
        if (!tmpData.contains("gpt_contexts"))
            tmpData.put("gpt_contexts",JSONArray())
        gptContexts = tmpData["gpt_contexts"] as JSONArray
        Plugins.initPlugin(this)
    }
}