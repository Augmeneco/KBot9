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
        reqBody.put("chat", JSONArray())

        msg.data = true
        val activity = Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(augGPTBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }

        msg.data = false

        msg.sendMessage(response)
    }
}