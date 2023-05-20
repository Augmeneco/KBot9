package plugins

import ContextBase
import PluginBase
import Plugins
import Utils
import org.json.JSONObject

class ContextTest: PluginBase() {
    override val names = mutableListOf("контекст","кон","тест")
    override val desc = "Тест переключения контекстов"
    override val hidden = true

    init {
        Plugins.initPlugin(this)
        ContextTest()
    }

    override fun main(msg: Utils.Msg){
        msg.user.changeContext("context_test", mutableMapOf(
            "time" to System.currentTimeMillis(),
            "firstAnsw" to true
        ))

        msg.sendMessage("Жду твоего следующего сообщения")
    }

    class ContextTest : ContextBase() {
        init { Utils.context.register("context_test", this) }

        override fun main(msg: Utils.Msg, args: JSONObject){
            if (args.getBoolean("firstAnsw")){
                msg.sendMessage("Нихуя ты долго отвечал, целых ${
                    (System.currentTimeMillis() - args.getLong("time")) / 1000
                } секунд. Жду твоё ещё одно твоё сообщение")

                args.put("time", System.currentTimeMillis())
                args.put("firstAnsw",false)
                msg.user.changeContext("context_test", args.toMap())

            } else {
                msg.sendMessage("Дождалась, ты отвечал ${
                    (System.currentTimeMillis() - args.getLong("time")) / 1000
                } секунд")

                msg.user.changeContext("main")
            }
        }
    }
}
