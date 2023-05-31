package plugins

import PluginBase
import Plugins
import Utils
import org.json.JSONArray
import org.json.JSONObject

class KBotGPT(skipInit: Boolean = false): PluginBase() {
    override val names = mutableListOf("чат", "chat")
    override val desc = "Общение через проприетарный KBotGPT"
    override val level = 1
    val augGPTBin = "data/kbotgpt.bin"
    var answerMode = false

    init {
        if (!skipInit)
            Plugins.initPlugin(this)
    }

    override fun main(msg: Utils.Msg){
        var gptMode = "based"

        if (answerMode) gptMode = "normal"

        val reqBody = JSONObject()
        reqBody.put("prompt", msg.userText)
        reqBody.put("mode", gptMode)
        if (answerMode)
            if (!msg.user.data.has("chat_history")) msg.user.data.put("chat_history", JSONArray())
            reqBody.put("chat", msg.user.data["chat_history"])

        val activity = Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(augGPTBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }

        activity.interrupt()
        msg.sendMessage(response)

        //Если в режиме неизвестных команд, то можно посохранять историю сообщений
        if (answerMode){
            if (msg.user.data.getJSONArray("chat_history").length() == 10)
                msg.user.data.getJSONArray("chat_history").remove(0)
            msg.user.data.getJSONArray("chat_history").put(
                JSONObject(mutableMapOf(
                    "question" to msg.userText,
                    "answer" to response
                ))
            )
            msg.user.updateUser()
        }
    }
}