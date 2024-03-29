import plugins.KBotGPT

import java.util.concurrent.*

fun main(args: Array<String>) {
    Plugins.loadPlugins()

    val telegram = Telegram()

    val threadPool = ThreadPoolExecutor(
        100, // максимальное количество потоков
        100, // максимальное количество потоков в очереди
        60L, TimeUnit.SECONDS, // время простоя потока
        LinkedBlockingQueue()
    )
    threadPool.allowCoreThreadTimeOut(true) // разрешаем убивать потоки при простое
    //val pool = Executors.newFixedThreadPool(10) // создаем экземпляр пула потоков

    Utils.events.get()
    Utils.events.handle()

    Utils.registry.data.put("uptime", System.currentTimeMillis())
    Utils.registry.data.put("usage", 0)
    Utils.registry.update()

    println("\nKBot started")

    while(true){
        try {for (update in telegram.getUpdates()){
            //println(update.toString(4))
            val msg: Utils.Msg = if (!update.has("callback_query"))
                Utils.Msg().parseUpdate(update)
            else
                Utils.Msg().parseUpdate(update.getJSONObject("callback_query"))

            if (msg.user.level < 0) continue

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
                if (!Utils.registry.data.has("usageAll")) Utils.registry.data.put("usageAll", 0)

                Utils.registry.data.put("usageAll", Utils.registry.data.getLong("usageAll")+1)
                Utils.registry.data.put("usage", Utils.registry.data.getLong("usage")+1)
                Utils.registry.update()

            } else if (msg.botMention){
                //Такой команды не существует по этому используем GPT для ответа

                threadPool.execute{
                    //val kbotGPT = KBotGPT(skipInit = true).main(msg)

                    Plugins.pluginsMap["kbotgpt"]!!.main(msg)
                }
            }
        }} catch(e: Exception){
            e.printStackTrace()
            Thread.sleep(1000)
        }
    }
}