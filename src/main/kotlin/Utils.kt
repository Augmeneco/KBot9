import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.*

import org.json.JSONArray
import org.json.JSONObject

import java.io.File
import java.sql.Connection
import java.util.*
import java.util.concurrent.*
import java.sql.DriverManager
import java.sql.Statement


object Utils{
    class DataBase{
        var db: Connection = DriverManager.getConnection("jdbc:sqlite:data/db.db")

        init{
            //Создаём базы если их не существует
            val statement = this.db.createStatement()
            statement.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                        "id INTEGER, " +
                        "userName TEXT, " +
                        "realName TEXT, " +
                        "level INTEGER, " +
                        "context TEXT, " +
                        "data TEXT)"
            )

            statement.execute("CREATE TABLE IF NOT EXISTS system (name TEXT, data TEXT)")
            statement.execute("CREATE TABLE IF NOT EXISTS chats (id numeric, data TEXT, users TEXT)")

            //statement.close()
        }

    }

    class Context{
        var contexts = mutableMapOf<String, ContextBase>()

        fun register(name: String, pointer: ContextBase): Context{
            println("Registered context \"$name\"")
            contexts[name] = pointer

            return this
        }

        fun get(name: String): ContextBase? {
            return this.contexts[name]
        }

    }

    class Registry{
        var data: MutableMap<String, String> = mutableMapOf()

        init{
            reload()
        }

        fun exists(name: String): Boolean{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM system WHERE name=?"
            )
            cur.setString(1, name)

            val result = cur.executeQuery()

            return result.next()
        }

        fun update(): Registry{
            for ((key, value) in this.data){
                if (!this.exists(key)){
                    val cur = Utils.dataBase.db.prepareStatement(
                        "INSERT INTO system VALUES (?, ?)"
                    )
                    cur.setString(1, key)
                    cur.setString(2, value)

                    cur.executeUpdate()
                } else {
                    val cur = Utils.dataBase.db.prepareStatement(
                        "UPDATE system SET " +
                            "name = ?," +
                            "data = ?" +
                        "WHERE name = ?"
                    )
                    cur.setString(1, key)
                    cur.setString(2, value)
                    cur.setString(3, key)

                    cur.executeUpdate()
                }
            }

            return this
        }

        fun reload(): Registry{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM system"
            )
            val result = cur.executeQuery()

            while (result.next()) {
                data.clear()
                data[result.getString("name")] = result.getString("data")
            }

            return this
        }
    }

    class User{
        var id: Int = -1
        var userName: String = ""
        var realName: String = ""
        var language: String = ""
        var isBot: Boolean = false

        var level: Int = 1
        lateinit var context: String
        lateinit var data: JSONObject

        fun changeContext(name: String, args: MutableMap<String, Any> = mutableMapOf()): User{
            this.context = name
            if (!this.data.has("context"))
                this.data.put("context", JSONObject())

            //if (!this.data.getJSONObject("context").has(name))
            this.data.getJSONObject("context").put(name, JSONObject(args))
            //else
            //    this.data.getJSONObject("context").

            this.updateUser()




            return this
        }

        fun updateUser(): User{
            val cur = Utils.dataBase.db.prepareStatement(
                "UPDATE users SET " +
                    "id = ?," +
                    "userName = ?," +
                    "realName = ?," +
                    "level = ?," +
                    "context = ?," +
                    "data = ?" +
                "WHERE id = ?"
            )
            cur.setInt(1, this.id)
            cur.setString(2, this.userName)
            cur.setString(3, this.realName)
            cur.setInt(4, this.level)
            cur.setString(5, this.context.toString())
            cur.setString(6, this.data.toString())
            cur.setInt(7, this.id)

            cur.executeUpdate()

            return this
        }

        fun reloadUser(): User{
            return getUser(this.id)
        }

        fun getUser(id: Int): User{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM users WHERE id=?"
            )
            cur.setInt(1, id)

            val result = cur.executeQuery()

            if (result.next()){
                do {
                    this.id = result.getInt("id")
                    this.userName = result.getString("userName")
                    this.realName = result.getString("realName")
                    this.level = result.getInt("level")
                    this.context = result.getString("context")
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
            this.id = fromObject.getInt("id")

            try {
                return getUser(this.id)
            } catch(e: Exception){
                return addUser(update)
            }

            return this
        }

        fun addUser(update: JSONObject): User{
            val fromObject = update.getJSONObject("from")

            this.id = fromObject.getInt("id")
            this.userName = fromObject.getString("username")
            this.realName = "${fromObject.getString("first_name")} ${fromObject.getString("last_name")}"
            this.language = fromObject.getString("language_code")
            this.isBot = fromObject.getBoolean("is_bot")
            this.context = "main"

            this.data = JSONObject()
            this.data.put("language", this.language)
            this.data.put("is_bot", this.isBot)

            val cur = Utils.dataBase.db.prepareStatement(
                "INSERT INTO users VALUES (?,?,?,?,?,?)"
            )
            cur.setInt(1, this.id)
            cur.setString(2, this.userName)
            cur.setString(3, this.realName)
            cur.setInt(4, this.level)
            cur.setString(5, this.context.toString())
            cur.setString(6, this.data.toString())

            cur.executeUpdate()

            return this
        }
    }

    class Chat{
        var id: Int = -1
        lateinit var data: JSONObject
        var users: MutableList<Int> = mutableListOf()
        var type: String = ""
        var title: String = ""

        //нужно чтоб потом кидать приветствие
        var newChat: Boolean = false

        fun updateChat(): Chat{
            val cur = Utils.dataBase.db.prepareStatement(
                "UPDATE chats SET " +
                    "id = ?," +
                    "data = ?," +
                    "users = ?" +
                "WHERE id = ?"
            )
            cur.setInt(1, this.id)
            cur.setString(2, this.data.toString())
            cur.setString(3, this.users.toString())
            cur.setInt(4, this.id)

            cur.executeUpdate()

            return this
        }

        fun reloadChat(): Chat{
            return getChat(this.id)
        }

        fun getChat(id: Int): Chat{
            val cur = Utils.dataBase.db.prepareStatement(
                "SELECT * FROM chats WHERE id=?"
            )
            cur.setInt(1, id)

            val result = cur.executeQuery()

            if (result.next()){
                do {
                    this.id = result.getInt("id")
                    this.data = JSONObject(result.getString("data"))
                    this.users = JSONArray(result.getString("users")).toList().toMutableList() as MutableList<Int>
                    this.title = this.data.getString("title")
                    this.type = this.data.getString("type")

                    return this
                } while (result.next())
            }else{
                throw java.lang.Exception("This chat don't exists")
            }

            return this
        }

        fun getChat(update: JSONObject): Chat{
            val chatObject = update.getJSONObject("chat")
            this.id = chatObject.getInt("id")

            try {
                return getChat(this.id)
            } catch(e: Exception){
                return addChat(update)
            }

            return this
        }

        fun addChat(update: JSONObject): Chat{
            val chatObject = update.getJSONObject("chat")

            this.id = chatObject.getInt("id")
            this.type = chatObject.getString("type")
            this.users = mutableListOf()
            this.data = JSONObject()
            this.title = if (this.type == "group") chatObject.getString("title")
                else "${chatObject.getString("first_name")} ${chatObject.getString("last_name")}"

            val cur = Utils.dataBase.db.prepareStatement(
                "INSERT INTO chats VALUES (?,?,?)"
            )

            this.data.put("title", this.title)
            this.data.put("type", this.type)

            cur.setInt(1, this.id)
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

        init{
            configJSON = JSONObject(File("data/config.json").readText())
            names = configJSON.getJSONArray("names").toMutableList().map{
                it.toString()
            }.toMutableList()
            telegramToken = "bot"+configJSON.getString("telegram_token")
        }
    }
    var config: Config = Config()
    var dataBase = Utils.DataBase()
    var telegram: Telegram = Telegram()
    var registry: Registry = Registry()
    var context: Context = Context()
    val isCmdRegex = Regex("""^/?\s?(?<botName>${config.names.joinToString("|")})?\s?""", RegexOption.IGNORE_CASE)

    class Msg {
        var text: String = ""
        var chatId: Int = 0
        var fromId: Int = 0
        var files: List<Any> = emptyList()
        var argv: List<String> = emptyList()
        var userText: String = ""
        var data: Any? = null
        var reply: Msg? = null
        var isCommand: Boolean = false
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
                "userText" to userText,
                "data" to data,
                "reply" to reply,
                "command" to command,
                "isCommand" to isCommand
            ) as MutableMap<String, Any>

            return out
        }

        fun parseCommand(){

            val prefix = isCmdRegex.find(this.text)?.value
            if (prefix == "" || prefix == null){
                return
            }

            val argv = isCmdRegex.replaceFirst(this.text, "").split(" ").toMutableList()
            val command = argv[0].lowercase()


            if (!Plugins.pluginsMap.contains(command)){
                this.argv = argv
                this.userText = argv.joinToString()
                this.command = this.userText
                return
            }

            argv.removeAt(0)

            this.command = command
            this.isCommand = true
            this.argv = argv
            this.userText = argv.joinToString()

            return
        }

        fun parseUpdate(update: JSONObject): Msg{
            if (update.has("reply_to_message")){
                //parseUpdate(update.getJSONObject("reply_to_message"))
                this.reply = Msg().parseUpdate(update.getJSONObject("reply_to_message"))
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

            this.chatId = update.getJSONObject("chat").getInt("id")
            this.fromId = update.getJSONObject("from").getInt("id")
            this.user = User().getUser(update)

            this.chat = Chat().getChat(update)

            if (!this.chat.users.contains(this.user.id))
                this.chat.users.add(this.user.id)
                this.chat.updateChat()

            this.parseCommand()
            return this
        }

        fun sendMessage(text: String): Any{
            return Utils.telegram.sendMessage(text, this.chatId)
        }
    }

    class Requests{
        fun post(url: String, params: MutableMap<Any, Any>): RequestResponse{
            val (request, response, result) = Fuel.post(url, map2List(params)).response()

            val requestResponse = RequestResponse()
            requestResponse.content = result.get()
            requestResponse.text = String(requestResponse.content)
            requestResponse.status = response.statusCode

            return requestResponse
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
}