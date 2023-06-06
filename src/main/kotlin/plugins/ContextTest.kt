package plugins

import PluginBase
import Plugins
import Utils
import org.json.JSONObject

class ContextTest: PluginBase() {
    override val names = mutableListOf("контекст","кон","тест")
    override val desc = "Тест переключения контекстов"
    override val hidden = true

    override fun main(msg: Utils.Msg){
        msg.changeContext("context_test1", mutableMapOf(
            "time" to System.currentTimeMillis()
        ))
        /*Utils.events.add(msg, "timer", "timer_test_msg", mutableMapOf(
            "timer" to System.currentTimeMillis()+10000,
            "repeat" to 10000
        ))*/

        msg.sendMessage("Жду твоего следующего сообщения. Так же я завела таймер который пришлёт сообщение через 10 секунд.")
    }

    val context_test1: (Utils.Msg, JSONObject) -> Unit = { msg, args ->
        msg.sendMessage("Нихуя ты долго отвечал, целых ${
            (System.currentTimeMillis() - args.getLong("time")) / 1000
        } секунд. Жду твоё ещё одно твоё сообщение")

        args.put("time", System.currentTimeMillis())
        msg.changeContext("context_test2", args.toMap())
    }

    val context_test2: (Utils.Msg, JSONObject) -> Unit = { msg, args ->
        msg.sendMessage("Дождалась, ты отвечал ${
            (System.currentTimeMillis() - args.getLong("time")) / 1000
        } секунд")

        msg.deleteContext()
    }

    val timer_test_msg: (Utils.Msg, JSONObject) -> Unit = { msg, args ->
        msg.sendMessage("Вот и запланированное сообщение через 10 секунд")
    }

    init {
        contexts["context_test1"] = context_test1
        contexts["context_test2"] = context_test2
        contexts["timer_test_msg"] = timer_test_msg

        Plugins.initPlugin(this)
    }
}
