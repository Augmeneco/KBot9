package plugins

import PluginBase
import Plugins

class Status: PluginBase() {
    override val names = mutableListOf("стат", "стата", "статус")

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        msg.sendMessage("хуй соси")
    }
}
