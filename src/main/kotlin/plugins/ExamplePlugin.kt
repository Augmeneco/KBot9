package plugins

import PluginBase
import Plugins
import Utils

class ExamplePlugin: PluginBase() {
    override val names = mutableListOf("пример", "example")
    override val desc = "Пример плагина бота"
    override val level = 256

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        msg.sendMessage("*hello world*", params = mutableMapOf("parse_mode" to "MarkdownV2"))
    }
}

