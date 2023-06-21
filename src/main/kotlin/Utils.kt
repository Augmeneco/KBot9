import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.util.concurrent.*


object Utils{
    var debug: Boolean = false;
    var config = Config()
    var dataBase = DataBase()
    var telegram = Telegram()
    var context = Context()
    var registry = Registry()
    var events = Events()
    val isCmdRegex = Regex("""^/?\s?(?<botName>${config.names.joinToString("\\s|")})?\s?""", RegexOption.IGNORE_CASE)

    class DataBase{
        var dbName = "data/db.db"
        lateinit var db: Connection

        init{
            if (debug) dbName = "data/db_debug.db"
            db = DriverManager.getConnection("jdbc:sqlite:$dbName")

            //Создаём базы если их не существует
            val statement = this.db.createStatement()
            statement.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER, " +
                        "userName TEXT, " +
                        "realName TEXT, " +
                        "level INTEGER, " +
                        "data TEXT)"
            )

            statement.execute("CREATE TABLE IF NOT EXISTS system (name TEXT, data TEXT)")
            statement.execute("CREATE TABLE IF NOT EXISTS chats (id INTEGER, data TEXT, users TEXT)")
            statement.execute("CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER, peer_id INTEGER, type TEXT, context TEXT, data TEXT)")

            //statement.close()
        }

    }

    class Context{
        var contexts = mutableMapOf<String, (Msg, JSONObject) -> Unit>()

        fun register(name: String, pointer: (Msg, JSONObject) -> Unit): Context{
            println("Registered context \"$name\"")
            contexts[name] = pointer

            return this
        }

        fun get(name: String): ((Msg, JSONObject) -> Unit)? {
            return this.contexts[name]
        }

    }

    class Registry{
        lateinit var data: JSONObject
        var tmpData: MutableMap<String, Any> = mutableMapOf()

        init { load() }

        fun load(): Registry{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM system WHERE name=\"registry\""
            )
            val result = cur.executeQuery()

            if (result.next()){
                data = JSONObject(result.getString("data"))
            } else {
                data = JSONObject()
                Utils.dataBase.db.prepareStatement(
                    "INSERT INTO system VALUES (\"registry\", \"{}\")"
                ).executeUpdate()
            }

            return this
        }

        fun update(): Registry{
            val cur = Utils.dataBase.db.prepareStatement(
                "UPDATE system SET \"data\" = ? WHERE name = \"registry\""
            )
            cur.setString(1, data.toString())
            cur.executeUpdate()

            return this
        }
    }

    class User{
        var id: Long = -1
        var userName: String = ""
        var realName: String = ""
        var language: String = ""
        var isBot: Boolean = false

        var level: Int = 1
        lateinit var data: JSONObject

        fun updateUser(): User{
            val cur = Utils.dataBase.db.prepareStatement(
                "UPDATE users SET " +
                    "id = ?," +
                    "userName = ?," +
                    "realName = ?," +
                    "level = ?," +
                    "data = ?" +
                "WHERE id = ?"
            )
            cur.setLong(1, this.id)
            cur.setString(2, this.userName)
            cur.setString(3, this.realName)
            cur.setInt(4, this.level)
            cur.setString(5, this.data.toString())
            cur.setLong(6, this.id)

            cur.executeUpdate()

            return this
        }

        fun reloadUser(): User{
            return getUser(this.id)
        }

        fun getUser(id: Long): User{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM users WHERE id=?"
            )
            cur.setLong(1, id)

            val result = cur.executeQuery()

            if (result.next()){
                do {
                    this.id = result.getLong("id")
                    this.userName = result.getString("userName")
                    this.realName = result.getString("realName")
                    this.level = result.getInt("level")
                    this.data = JSONObject(result.getString("data"))
                    this.language = data.getString("language")
                    this.isBot = data.getBoolean("is_bot")

                    return this
                } while (result.next())
            }else{
                throw java.lang.Exception("This user don't exists")
            }

            return this
        }

        fun getUser(update: JSONObject): User{
            val fromObject = update.getJSONObject("from")
            this.id = fromObject.getLong("id")

            try {
                return getUser(this.id)
            } catch(e: Exception){
                return addUser(update)
            }

            return this
        }

        fun addUser(update: JSONObject): User{
            println(update)
            val fromObject = update.getJSONObject("from")

            this.id = fromObject.getLong("id")

            if (fromObject.has("username"))
                this.userName = fromObject.getString("username")
            else
                this.userName = "username"

            this.realName = fromObject.getString("first_name")
            if (fromObject.has("last_name"))
                this.realName += "  ${fromObject.getString("last_name")}"

            if (fromObject.has("language_code")) this.language = fromObject.getString("language_code")
            else this.language = "ru"

            this.isBot = fromObject.getBoolean("is_bot")

            this.data = JSONObject()
            this.data.put("language", this.language)
            this.data.put("is_bot", this.isBot)

            val cur = Utils.dataBase.db.prepareStatement(
                "INSERT INTO users VALUES (?,?,?,?,?)"
            )
            cur.setLong(1, this.id)
            cur.setString(2, this.userName)
            cur.setString(3, this.realName)
            cur.setInt(4, this.level)
            cur.setString(5, this.data.toString())

            cur.executeUpdate()

            return this
        }
    }

    class Chat{
        var id: Long = -1
        lateinit var data: JSONObject
        var users: MutableList<Long> = mutableListOf()
        var contexts: MutableMap<Long, String> = mutableMapOf()
        var type: String = ""
        var title: String = ""

        //нужно чтоб потом кидать приветствие
        var newChat: Boolean = false

        fun updateChat(): Chat{
            this.data.put("contexts", JSONObject(this.contexts))
            val cur = Utils.dataBase.db.prepareStatement(
                "UPDATE chats SET " +
                    "id = ?," +
                    "data = ?," +
                    "users = ?" +
                "WHERE id = ?"
            )
            cur.setLong(1, this.id)
            cur.setString(2, this.data.toString())
            cur.setString(3, this.users.toString())
            cur.setLong(4, this.id)

            cur.executeUpdate()

            return this
        }

        fun reloadChat(): Chat{
            return getChat(this.id)
        }

        fun getChat(id: Long): Chat{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM chats WHERE id=?"
            )
            cur.setLong(1, id)

            val result = cur.executeQuery()

            if (result.next()){
                do {
                    this.id = result.getLong("id")
                    this.data = JSONObject(result.getString("data"))
                    this.users = JSONArray(result.getString("users")).toList().toMutableList() as MutableList<Long>
                    this.title = this.data.getString("title")
                    this.type = this.data.getString("type")

                    //ето пиздец, пришлось конвертить <String, Any> в <Long, String>
                    this.contexts = this.data.getJSONObject("contexts").toMap()
                        .mapValues { (_, value) -> value as String }
                        .mapKeys { (key, _) -> key.toLong() } as MutableMap<Long, String>

                    return this
                } while (result.next())
            }else{
                throw java.lang.Exception("This chat don't exists")
            }

            return this
        }

        fun getChat(update: JSONObject): Chat{
            val chatObject = update.getJSONObject("chat")
            this.id = chatObject.getLong("id")

            try {
                return getChat(this.id)
            } catch(e: Exception){
                return addChat(update)
            }

            return this
        }

        fun addChat(update: JSONObject): Chat{
            val chatObject = update.getJSONObject("chat")

            this.id = chatObject.getLong("id")
            this.type = chatObject.getString("type")
            this.users = mutableListOf()
            this.data = JSONObject()

            if (!chatObject.has("last_name")) chatObject.put("last_name","")

            if (this.type == "group" || this.type == "supergroup"){
                this.title = chatObject.getString("title")
            } else {
                this.title = "${chatObject.getString("first_name")} ${chatObject.getString("last_name")}"
            }

            val cur = Utils.dataBase.db.prepareStatement(
                "INSERT INTO chats VALUES (?,?,?)"
            )

            this.data.put("title", this.title)
            this.data.put("type", this.type)
            this.data.put("contexts", JSONObject())

            cur.setLong(1, this.id)
            cur.setString(2, this.data.toString())
            cur.setString(3, list2Json(this.users as MutableList<Any>).toString())

            cur.executeUpdate()

            return this
        }
    }

    class Config{
        var telegramToken: String
        var names: MutableList<String>
        var configJSON: JSONObject
        var botPrefix: String

        init{
            configJSON = JSONObject(File("data/config.json").readText())

            names = configJSON.getJSONArray("names").toMutableList().map{
                it.toString()
            }.toMutableList()
            if (debug) names = mutableListOf("кбд","дебаг")

            telegramToken = "bot"+configJSON.getString("telegram_token")
            botPrefix = configJSON.getString("bot_prefix")
        }
    }

    class Msg {
        var text: String = ""
        var chatId: Long = 0
        var fromId: Long = 0
        var msgId: Long = 0
        var files: List<Any> = emptyList()
        var argv: List<String> = emptyList()
        var userText: String = ""
        var data: Any? = null
        var reply: Msg? = null
        var isCommand: Boolean = false
        var botMention: Boolean = false
        var command: String = ""
        var threadPool: ThreadPoolExecutor? = null
        lateinit var user: User
        lateinit var chat: Chat

        fun toMap(): MutableMap<String, Any>{
            val out = mutableMapOf(
                "text" to text,
                "chatId" to chatId,
                "fromId" to fromId,
                "files" to files,
                "argv" to argv,
                "msgId" to msgId,
                "userText" to userText,
                "data" to data,
                "reply" to reply,
                "command" to command,
                "isCommand" to isCommand
            ) as MutableMap<String, Any>

            return out
        }

        fun changeContext(name: String, args: MutableMap<String, Any> = mutableMapOf()){
            this.chat.contexts[user.id] = name
            if (!this.user.data.has("context"))
                this.user.data.put("context", JSONObject())

            this.user.data.getJSONObject("context").put(name, JSONObject(args))
            this.user.updateUser()
            this.chat.updateChat()
        }

        fun deleteContext(){
            chat.contexts.remove(user.id)
            chat.updateChat()
        }

        fun parseCommand(){
            if (this.text.contains(config.botPrefix))
                this.text = this.text.replace(config.botPrefix, "")


            val matchResult = isCmdRegex.find(this.text)

            if (matchResult?.groupValues?.get(1) != "")
                botMention = true

            val prefix = matchResult?.value
            if (prefix == "" || prefix == null){
                return
            }

            val argv = isCmdRegex.replaceFirst(this.text, "").split(" ").toMutableList()
            val command = argv[0].lowercase()

            if (!Plugins.pluginsMap.contains(command)){
                this.argv = argv
                this.userText = argv.joinToString(separator = " ")
                this.command = this.userText
                return
            }

            argv.removeAt(0)

            this.command = command
            this.isCommand = true
            this.argv = argv
            this.userText = argv.joinToString(separator = " ")

            return
        }

        fun parseUpdate(update: JSONObject): Msg{
            if (update.has("reply_to_message")){
                //parseUpdate(update.getJSONObject("reply_to_message"))
                this.reply = Msg().parseUpdate(update.getJSONObject("reply_to_message"))
            }
            if (update.has("message_id")){
                this.msgId = update.getLong("message_id")
            }

            if (update.has("text")){
                this.text = update.getString("text")
            }

            if (update.has("photo")){
                //val fileId = update.getJSONArray("photo").toList()

                if (update.has("caption")){
                    this.text = update.getString("caption")
                }
            }

            this.chatId = update.getJSONObject("chat").getLong("id")
            this.fromId = update.getJSONObject("from").getLong("id")
            this.user = User().getUser(update)

            this.chat = Chat().getChat(update)

            var userExist = false
            for (user in this.chat.users)
                if (user == this.user.id){
                    userExist = true
                    break
                }
            if (!userExist){
                this.chat.users.add(this.user.id)
                this.chat.updateChat()
            }

            this.parseCommand()
            return this
        }

        fun sendChatAction(){
            Utils.telegram.sendChatAction(this.chatId)
        }

        fun sendMessage(text: String,
                        params: MutableMap<Any, Any> = mutableMapOf(),
                        keyboard: JSONObject = JSONObject()): Any{
            params.put("reply_to_message_id", this.msgId)
            if (!keyboard.isEmpty)
                params.put("reply_markup", keyboard)

            return Utils.telegram.sendMessage(text, this.chatId, params)
        }
    }

    class Requests{
        var httpClient = OkHttpClient()

        companion object {
            fun buildProxy(domain: String, port: Int): Proxy{
                return Proxy(Proxy.Type.SOCKS, InetSocketAddress(domain, port))
            }
        }

        fun protoRequest(
            type: String,
            url: String,
            params: MutableMap<Any, Any> = mutableMapOf(),
            proxy: MutableList<Any> = mutableListOf<Any>()
        ): RequestResponse{
            val formBodyBuilder = FormBody.Builder()

            for ((key, value) in params) {
                formBodyBuilder.add(key.toString(), value.toString())
            }
            val requestBody = formBodyBuilder.build()
            var request = Request.Builder()
                .url(url)
            if (type == "post")
                request = request.post(requestBody)


            val requestResponse: RequestResponse = RequestResponse()

            var client: OkHttpClient.Builder = httpClient.newBuilder()
            if (proxy.isNotEmpty())
                client = client.proxy(buildProxy(
                    proxy[0] as String, proxy[1] as Int
                ))

            client.build().newCall(request.build()).execute().use { response ->
                val responseBody = response.body

                requestResponse.content = responseBody!!.bytes()
                requestResponse.text = String(requestResponse.content)
                requestResponse.status = response.code
            }

            return requestResponse
        }

        fun post(
            url: String,
            params: MutableMap<Any, Any> = mutableMapOf(),
            proxy: MutableList<Any> = mutableListOf<Any>()
        ): RequestResponse{
            return protoRequest("post", url, params, proxy)
        }

        fun get(
            url: String,
            params: MutableMap<Any, Any> = mutableMapOf(),
            proxy: MutableList<Any> = mutableListOf<Any>()
        ): RequestResponse{
            return protoRequest("get", url, params, proxy)
        }

        class RequestResponse{
            lateinit var content: ByteArray
            lateinit var text: String
            var status: Int = 0

            fun json(): Any{
                return try {
                    JSONObject(text)
                } catch (e: Exception) {
                    JSONArray(text)
                }
            }
        }
    }

    fun map2List(map: MutableMap<Any, Any>): List<Pair<String, Any>>{
        return map.toList().map { (key, value) -> key.toString() to value }
    }

    fun list2Json(list: MutableList<Any>): JSONArray{
        val jsonArray = JSONArray()
        for (item in list){
            jsonArray.put(item)
        }

        return jsonArray
    }

    fun formatElapsedTime(elapsedTime: Long): String {
        val hours = elapsedTime / (1000 * 60 * 60)
        val minutes = (elapsedTime % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (elapsedTime % (1000 * 60)) / 1000

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun sendActivity(msg: Msg): Thread{
        val thread = Thread{
            while(msg.data as Boolean){
                msg.sendChatAction()
                Thread.sleep(5000)
            }
        }
        thread.start()

        return thread
    }

    fun chunks(s: String, n: Int): List<String> {
        val result = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val chunk = s.substring(i, minOf(i + n, s.length))
            result.add(chunk)
            i += n
        }
        return result
    }
}