import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

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

    Utils.registry.data["uptime"] = System.currentTimeMillis().toString()
    Utils.registry.data["usage"] = "0"
    Utils.registry.update()

    println("KBot started")

    while(true){
        for (update in telegram.getUpdates()){
            //println(update.toString(4))

            val msg = Utils.Msg().parseUpdate(update)
            msg.threadPool = threadPool

            if (msg.isCommand || msg.user.context != "main"){
                val future = threadPool.submit(Callable<Unit> {
                    //запускаем контекст если у юзера он имеется. команда нам не нужна тогда
                    if (msg.user.context != "main"){
                        val context = Utils.context.get(msg.user.context)
                        context?.main(msg, msg.user.data.getJSONObject("context").getJSONObject(msg.user.context))

                        return@Callable
                    }

                    val plugin = Plugins.pluginsMap[msg.command]
                    if (msg.user.level >= plugin?.level!!){
                        plugin.main(msg)
                    }else{
                        msg.sendMessage("Извини, но твоего уровня прав не достаточно, необходим ${plugin.level}, " +
                                "а у тебя лишь ${msg.user.level}")
                    }


                    println("The command \"${msg.command}\" started in thread \"${Thread.currentThread().name}\"")
                })

                // устанавливаем таймаут для задачи в 1 минуту
                try {
                    future.get(1, TimeUnit.MINUTES)
                } catch (e: TimeoutException) {
                    future.cancel(true)
                    println("Task timed out after 1 minute")
                }

                //добавляем значение счётчика сообщений
                if (!Utils.registry.exists("usageAll")) Utils.registry.data["usageAll"] = "0"

                Utils.registry.data["usageAll"] = (Utils.registry.data["usageAll"]?.toInt()?.plus(1)).toString()
                Utils.registry.data["usage"] = (Utils.registry.data["usage"]?.toInt()?.plus(1)).toString()
                Utils.registry.update()
            }
        }
    }
}