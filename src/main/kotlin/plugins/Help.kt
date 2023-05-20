package plugins

import PluginBase
import Plugins
import Utils

class Help: PluginBase() {
    override val names = mutableListOf("помощь", "help", "хелп", "начать", "start")
    override val desc = "Помощь по командам бота"
    override val level = 1

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        var out = "[ Помощь ]\n"

        for (plugin in Plugins.pluginsList){
            if (msg.user.level >= plugin.level && !plugin.hidden)
                out += "• ${Utils.config.names[0]} ${plugin.names[0]} - ${plugin.desc}\n"
        }

        out += "\nУровень твоих прав: ${msg.user.level}"

        msg.sendMessage(out)
    }
}
