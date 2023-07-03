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

        var prompt = msg.userText


        if (msg.isInline && msg.data.getLong("user_id") == msg.user.id){
            if (msg.data.getString("button") == "clear"){
                (Utils.registry.tmpData["chats_history"] as JSONObject).remove(msg.user.id.toString())
                msg.sendMessage("${msg.user.realName}, история очищена")
            }

            if (msg.data.getString("button") == "change_prompt"){
                msg.sendMessage("Вы собираетесь сменить системный промпт бота. В нём надо описать как должен себя вести бот.\n" +
                        "Жду ваше следующее сообщение с описанием", keyboard = Telegram.Keyboard().setInlineButtons(
                            mutableListOf(mutableListOf(mutableMapOf(
                                "text" to "Отмена",
                                "callback_data" to JSONObject(mutableMapOf(
                                    "button" to "cancel"
                                )).toString()
                            ),mutableMapOf(
                                "text" to "Удалить",
                                "callback_data" to JSONObject(mutableMapOf(
                                    "button" to "delete"
                                )).toString()
                            )))
                        ).build())
                msg.changeContext("change_bot_prompt")
            }

            return
        }

        val chatsHistory = Utils.registry.tmpData["chats_history"] as JSONObject
        if (!chatsHistory.has(msg.user.id.toString())) {
            chatsHistory.put(msg.user.id.toString(), JSONArray())

            if (Utils.registry.data.getJSONObject("system_prompts").has(msg.user.id.toString())){
                chatsHistory.getJSONArray(msg.user.id.toString()).put(
                    mutableMapOf(
                        "role" to "system",
                        "content" to Utils.registry.data.getJSONObject("system_prompts").getString(msg.user.id.toString())
                    )
                )
            } else
                chatsHistory.getJSONArray(msg.user.id.toString()).put(systemPrompt)
        }

        val chatHistory = chatsHistory.getJSONArray(msg.user.id.toString())

        val reqBody = JSONObject()
        reqBody.put("prompt", prompt)
        reqBody.put("chat", chatHistory)
        //reqBody.put("jailbreak", true)
        //reqBody.getJSONArray("chat").put(systemPrompt)

        msg.activityEvent = true
        Utils.sendActivity(msg)

        val process = ProcessBuilder(listOf(gptBin, reqBody.toString()))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }
        msg.activityEvent = false

        if (response == "" || response == "\n"){
            msg.sendMessage("Хз, не отвечает бот")
            return
        }

        val promptButtons = mutableListOf(mutableListOf(
            mutableMapOf(
                "text" to "Повторить запрос",
                "callback_data" to JSONObject(mutableMapOf("user_id" to msg.user.id, "button" to "again")).toString()
            ),
            mutableMapOf(
                "text" to "Очистить историю",
                "callback_data" to JSONObject(mutableMapOf("user_id" to msg.user.id, "button" to "clear")).toString()
            )
        ))

        if (msg.user.level >= 255){
            promptButtons.add(mutableListOf(
                mutableMapOf(
                    "text" to "Задать свой системный промпт",
                    "callback_data" to JSONObject(mutableMapOf("user_id" to msg.user.id, "button" to "change_prompt")).toString()
                )
            ))
        }

        val keyboard = Telegram.Keyboard()
        keyboard.setInlineButtons(promptButtons)

        chatHistory.put(mutableMapOf(
            "role" to "user",
            "content" to msg.userText
        ))
        chatHistory.put(mutableMapOf(
            "role" to "assistant",
            "content" to response
        ))

        if (!msg.isInline)
            msg.sendMessage(response, keyboard = keyboard.build())
        else {
            msg.editMessage(response, keyboard = keyboard.build())
        }
    }

    val change_bot_prompt: (Utils.Msg, JSONObject) -> Unit = { msg, args ->
        if (msg.isInline){
            if (msg.data.getString("button") == "cancel")
                msg.text = "/отмена"
            if (msg.data.getString("button") == "delete")
                msg.text = "/удалить"
        }

        if (mutableListOf("/отмена","отмена").contains(msg.text.lowercase())){
            msg.sendMessage("Вы отменили изменение промпта")
            msg.deleteContext()

        } else if(mutableListOf("/удалить","удалить").contains(msg.text.lowercase())) {
            Utils.registry.data.getJSONObject("system_prompts").remove(msg.user.id.toString())
            (Utils.registry.tmpData["chats_history"] as JSONObject).remove(msg.user.id.toString())
            msg.sendMessage("Вы удалили свой промпт")
            msg.deleteContext()

        } else {
            val userPrompts = Utils.registry.data.getJSONObject("system_prompts")
            userPrompts.put(msg.user.id.toString(), msg.text)

            (Utils.registry.tmpData["chats_history"] as JSONObject).remove(msg.user.id.toString())

            msg.sendMessage("Промпт изменен")
            msg.deleteContext()
        }
        Utils.registry.update()
    }

    init {
        if (!Utils.registry.tmpData.contains("chats_history"))
            Utils.registry.tmpData["chats_history"] = JSONObject()
        if (!Utils.registry.data.has("system_prompts"))
            Utils.registry.data.put("system_prompts",JSONObject())

        contexts["change_bot_prompt"] = change_bot_prompt
        Plugins.initPlugin(this)

        Utils.registry.update()
    }
}