package plugins

import PluginBase
import Plugins
import Utils
import org.json.JSONObject

class Hryu: PluginBase() {
    override val names = mutableListOf("хрю", "Hryu")
    override val desc = "Хрюкни"
    override val level = 1

    init { Plugins.initPlugin(this) }

    val keyboard = Telegram.Keyboard()

    override fun main(msg: Utils.Msg){
        keyboard.setInlineButtons(mutableListOf(mutableListOf(
            mutableMapOf(
                "text" to "тихий хрю",
                "callback_data" to JSONObject(mutableMapOf(
                    "тихо" to true
                )).toString()
            ),
            mutableMapOf(
                "text" to "громкий ХРЮ",
                "callback_data" to JSONObject(mutableMapOf(
                    "тихо" to false
                )).toString()
            )
        )))
        if (msg.isInline)
            hryu(msg)
        else
            starthryu(msg)
    }

    private fun hryu(msg: Utils.Msg){
        val text = if ((msg.data as JSONObject).getBoolean("тихо"))
            msg.inlineMsg.text + "\n• свинка ${msg.user.realName} тихо хрюкнул"
        else
            msg.inlineMsg.text + "\n• свиняра ${msg.user.realName} громко ХРЮКНУЛ"

        msg.editMessage(
            text,
            keyboard = keyboard.build()
        )
    }

    private fun starthryu(msg: Utils.Msg){
        msg.sendMessage("Список хрюшек:\n\n", keyboard = keyboard.build())
    }

}