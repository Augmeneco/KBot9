package plugins

import java.io.File
import java.lang.Runtime.getRuntime

import PluginBase
import Plugins

class Status: PluginBase() {
    override val names = mutableListOf("стат", "стата", "статус")

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        var out: String = """[ Статистика ]
Система:
 Процессор:
${getCpuLoad()} ОЗУ:
${getRamUsage()}
Бот:
 Время работы: null
 Обращений: null"""

        msg.sendMessage(out)
    }

    fun getCpuLoad(): String{
        var cpuCoresLoad = ""

        val statFile = File("/proc/stat")
        val cpuLines = statFile.readLines().filter { it.startsWith("cpu") }
        for (i in cpuLines.indices) {
            if (i==0) continue
            val lineParts = cpuLines[i].split("\\s+".toRegex())
            val coreName = "Ядро №" + i
            val totalTicks = lineParts.slice(1 until lineParts.size).map { it.toLong() }.sum()
            val idleTicks = lineParts[4].toLong()
            val usage = (1.0 - (idleTicks.toDouble() / totalTicks.toDouble())) * 100
            cpuCoresLoad += "&#8195;&#8195;$coreName: %.0f%%\n".format(usage)
        }
        return cpuCoresLoad
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
}
