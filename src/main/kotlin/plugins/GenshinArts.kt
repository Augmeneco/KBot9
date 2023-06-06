package plugins

import PluginBase
import Plugins
import Utils

class GenshinArts: PluginBase() {
    override val names = mutableListOf("геншин")
    override val desc = "Помощь по командам бота"
    override val level = 256
    override val hidden = true

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        test(msg)
    }

    val test: (Utils.Msg) -> Unit = { msg ->
        msg.sendMessage("test")
        msg.sendMessage("kek")
    }
}
