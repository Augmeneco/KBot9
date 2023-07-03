package plugins

import PluginBase
import Plugins
import Utils
import java.io.File

class Term: PluginBase() {
    override val names = mutableListOf("терм", "терминал", "term")
    override val desc = "Терминал Bash"
    override val level = 256

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        val file = File("/tmp/kbterm.sh")
        file.writeText("#!/bin/bash\n${msg.userText}")

        val process = ProcessBuilder("bash", "/tmp/kbterm.sh")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()
        val response = process.inputStream.bufferedReader().use { it.readText() }

        msg.sendMessage(response)
    }
}
