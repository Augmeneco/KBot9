package plugins

import PluginBase
import Plugins
import Utils
import java.lang.Runtime.getRuntime

class Status: PluginBase() {
    override val names = mutableListOf("стат", "стата", "статус")
    override val desc = "Статистика бота"
    override val level = 1

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        var out: String = """[ Статистика ]
Система:
&#8195;${getCpuLoad()} 
ОЗУ:
${getRamUsage()}
Бот:
&#8195;Активные потоки тредпула: ${msg.threadPool?.activeCount}
&#8195;Время работы: ${
    formatElapsedTime(System.currentTimeMillis() - Utils.registry.data["uptime"]?.toLong()!!)
}
&#8195;Обращений всего: ${Utils.registry.data["usageAll"]}
&#8195;Обращений с включения: ${Utils.registry.data["usage"]}"""

        msg.sendMessage(out)
    }

    fun getCpuLoad(): String{

        return getRuntime().exec("uptime").inputStream.bufferedReader().readText().trim()
    }

    fun getRamUsage(): String{
        val runtime = Runtime.getRuntime()
        val totalMemoryJVM = runtime.totalMemory() / 1024 /1024
        val freeMemoryJVM = runtime.freeMemory() / 1024 / 1024
        val maxMemoryJVM = runtime.maxMemory() / 1024 / 1024
        val usedMemoryJVM = totalMemoryJVM - freeMemoryJVM

        val result = getRuntime().exec("free -m").inputStream.bufferedReader().useLines { lines ->
            lines.drop(1).first().split("\\s+".toRegex())
        }

        val totalMemory = result[1].toLong()
        val freeMemory = result[3].toLong()
        val usedMemory = totalMemory - freeMemory

        return "&#8195;&#8195;Всего в системе: $totalMemory MB\n" +
                "&#8195;&#8195;Всего в JVM: $totalMemoryJVM MB\n" +
                "&#8195;&#8195;Использовано в системе: $usedMemory MB\n" +
                "&#8195;&#8195;Использовано ботом: $usedMemoryJVM MB\n" +
                "&#8195;&#8195;Свободно в системе: $freeMemory MB\n" +
                "&#8195;&#8195;Свободно в JVM: $freeMemoryJVM MB"
    }

    fun formatElapsedTime(elapsedTime: Long): String {
        val hours = elapsedTime / (1000 * 60 * 60)
        val minutes = (elapsedTime % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (elapsedTime % (1000 * 60)) / 1000

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}
