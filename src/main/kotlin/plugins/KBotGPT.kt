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
    lateinit var gptHistory: JSONObject

    override fun main(msg: Utils.Msg){
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

        val reqBody = JSONObject()
        reqBody.put("prompt", msg.userText)
        reqBody.put("chat", JSONArray())

        msg.activityEvent = true
        Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(gptBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }
        msg.activityEvent = false

        msg.sendMessage(response)
    }

    init {
        if (!tmpData.contains("gpt_history"))
            tmpData.put("gpt_history",JSONObject())
        gptHistory = tmpData["gpt_history"] as JSONObject
        Plugins.initPlugin(this)
    }
}