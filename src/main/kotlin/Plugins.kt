import java.io.File
import java.net.URLClassLoader

import plugins.*

object Plugins{
    var pluginsMap = mutableMapOf<String, PluginBase>()

    fun loadPlugins(){
        Status()
    }

    fun initPlugin(plugin: PluginBase){
        for (name in plugin.names){
            if (name !in pluginsMap){
                pluginsMap[name] = plugin
            }
        }
        println("Init plugin \"${plugin.names[0]}\"")
    }
}

abstract class PluginBase{
    abstract val names: MutableList<String>
    abstract fun main(msg: Utils.Msg)
}