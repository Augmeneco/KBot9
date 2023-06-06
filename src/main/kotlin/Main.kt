import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import Utils

import org.json.JSONArray
import org.json.JSONObject
import plugins.KBotGPT

import java.util.concurrent.*

fun main(args: Array<String>) {
    Plugins.loadPlugins()

    val telegram = Telegram()

    val threadPool = ThreadPoolExecutor(
        10, // максимальное количество потоков
        10, // максимальное количество потоков в очереди
        1, TimeUnit.MINUTES, // время простоя потока
        LinkedBlockingQueue(), // безлимитная очередь задач
        ThreadPoolExecutor.CallerRunsPolicy() // обработчик задач, которые не могут быть обработаны
    )
    threadPool.allowCoreThreadTimeOut(true) // разрешаем убивать потоки при простое
    //val pool = Executors.newFixedThreadPool(10) // создаем экземпляр пула потоков

    Utils.events.get()
    Utils.events.handle()

    Utils.registry.data["uptime"] = System.currentTimeMillis().toString()
    Utils.registry.data["usage"] = "0"
    Utils.registry.update()

    println("\nKBot started")

    while(true){
        for (update in telegram.getUpdates()){
            //println(update.toString(4))

            val msg = Utils.Msg().parseUpdate(update)
            msg.threadPool = threadPool

            if (msg.isCommand || msg.chat.contexts.contains(msg.user.id)){
                threadPool.execute{
                    //запускаем контекст если у юзера он имеется. команда нам не нужна тогда
                    if (msg.chat.contexts.contains(msg.user.id)){
                        val context = Utils.context.get(msg.chat.contexts[msg.user.id]!!)
                        context?.invoke(msg, msg.user.data.getJSONObject("context")
                                                          .getJSONObject(msg.chat.contexts[msg.user.id]))

                        return@execute
                    }

                    println("The command \"${msg.command}\" started in thread \"${Thread.currentThread().name}\"")

                    val plugin = Plugins.pluginsMap[msg.command]
                    if (msg.user.level >= plugin?.level!!){
                        plugin.main(msg)
                    }else{
                        msg.sendMessage("Извини, но твоего уровня прав не достаточно, необходим ${plugin.level}, " +
                            "а у тебя лишь ${msg.user.level}")
                    }
                }

                //добавляем значение счётчика сообщений
                if (!Utils.registry.exists("usageAll")) Utils.registry.data["usageAll"] = "0"

                Utils.registry.data["usageAll"] = (Utils.registry.data["usageAll"]?.toInt()?.plus(1)).toString()
                Utils.registry.data["usage"] = (Utils.registry.data["usage"]?.toInt()?.plus(1)).toString()
                Utils.registry.update()

            } else if (msg.botMention){
                //Такой команды не существует по этому используем GPT для ответа

                threadPool.execute{
                    val kbotGPT = KBotGPT(skipInit = true)
                    kbotGPT.answerMode = true

                    kbotGPT.main(msg)
                }
            }
        }
    }
}