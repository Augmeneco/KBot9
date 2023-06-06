import org.json.JSONObject
import java.io.File
import java.net.URLClassLoader

import plugins.*

object Plugins{
    var pluginsMap = mutableMapOf<String, PluginBase>()
    var pluginsList = mutableListOf<PluginBase>()

    fun loadPlugins(){
        Status()
        Help()
        ContextTest()
        KBotGPT()
        GenshinArts()
    }

    fun initPlugin(plugin: PluginBase){
        pluginsList.add(plugin)

        for (name in plugin.names){
            if (name !in pluginsMap){
                pluginsMap[name] = plugin
            }
        }

        if (plugin.contexts.isNotEmpty()){
            for ((key, value) in plugin.contexts){
                Utils.context.register(key, value)
            }
        }

        println("Init plugin \"${plugin.names[0]}\"")
    }
}

abstract class PluginBase(skipInit: Boolean = false){
    abstract val names: MutableList<String>
    abstract val desc: String
    open val level: Int = 1
    open val hidden: Boolean = false
    abstract fun main(msg: Utils.Msg)
    val contexts: MutableMap<String, (Utils.Msg, JSONObject) -> Unit> = mutableMapOf()
}