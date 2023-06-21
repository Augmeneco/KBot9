package plugins

import PluginBase
import Plugins
import Utils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

typealias TmpHistory = MutableMap<Long,MutableList<MutableMap<String, String>>>

class KBotGPT(skipInit: Boolean = false): PluginBase() {
    override val names = mutableListOf("чат", "chat")
    override val desc = "Общение через проприетарный KBotGPT"
    override val level = 1
    var gptBin = "data/kbotgpt.bin"

    init {
        if (!skipInit)
            Plugins.initPlugin(this)
    }

    override fun main(msg: Utils.Msg){
        if (System.getProperty("os.name").lowercase().contains("win"))
            gptBin = "data/kbotgpt.exe"

        var prompt = msg.userText

        if (!Utils.registry.tmpData.contains("chats_history"))
            Utils.registry.tmpData["chats_history"] = JSONObject()

        val chatsHistory = Utils.registry.tmpData["chats_history"] as JSONObject
        if (!chatsHistory.has(msg.user.id.toString())) {
            chatsHistory.put(msg.user.id.toString(), JSONArray())
            prompt = "Меня зовут ${msg.user.realName}. А теперь мой вопрос: $prompt"
        }

        val chatHistory = chatsHistory.getJSONArray(msg.user.id.toString())
        if (chatHistory.length() >= 10){
            chatHistory.remove(0)
        }

        val reqBody = JSONObject()
        reqBody.put("prompt", prompt)
        reqBody.put("chat", chatHistory)

        msg.data = true
        Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(gptBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        var response = process.inputStream.bufferedReader().use { it.readText() }
        msg.data = false
        
        if (response == "Unable to fetch the response, Please try again.\n")
            response = "Хз. Чатбот не работает, пробуй позже"

        chatHistory.put(mutableMapOf(
            "question" to msg.userText,
            "answer" to response
        ))

        msg.sendMessage(response)
    }
}