import org.json.JSONObject
import PluginBase
import Plugins
import Utils
import java.lang.Exception

class EventBase {
    var id: Long = 0
    var userId: Long = 0
    var peerId: Long = 0
    lateinit var type: String
    lateinit var context: (Utils.Msg, JSONObject) -> Unit
    lateinit var data: JSONObject

    fun update(){
        val cur = Utils.dataBase.db.prepareStatement(
            "UPDATE events SET data=? WHERE id=?")
        cur.setString(1, data.toString())
        cur.setLong(2, id)
        cur.executeUpdate()
    }
}

class Events {
    val events: MutableList<EventBase> = mutableListOf()

    fun add(msg: Utils.Msg, type: String, context: String, data: MutableMap<String, Any>){
        add(msg, type, context, JSONObject(data))
    }

    fun add(msg: Utils.Msg, type: String, context: String, data: JSONObject){
        val cur = Utils.dataBase.db.prepareStatement(
            "INSERT INTO events (user_id,peer_id,type,context,data) VALUES (?,?,?,?,?)"
        )
        cur.setLong(1, msg.user.id)
        cur.setLong(2, msg.chatId)
        cur.setString(3, type)
        cur.setString(4, context)
        cur.setString(5, data.toString())

        cur.executeUpdate()
        get()
    }

    fun get(): MutableList<EventBase> {
        val cur = Utils.dataBase.db.prepareStatement(
            "SELECT * FROM events"
        )
        val result = cur.executeQuery()

        if (result.next()) {
            outerLoop@ do {
                for (event in events.toList())
                    if (result.getLong("id") == event.id) continue@outerLoop

                val event = EventBase()
                event.id = result.getLong("id")
                event.userId = result.getLong("user_id")
                event.peerId = result.getLong("peer_id")
                event.type = result.getString("type")
                event.context = Utils.context.get(result.getString("context"))!!
                event.data = JSONObject(result.getString("data"))

                events.add(event)
            } while (result.next())
        }

        return this.events
    }

    fun delete(id: Long){
        val cur = Utils.dataBase.db.prepareStatement(
            "DELETE FROM events WHERE id=?"
        )
        cur.setLong(1, id)
        cur.executeUpdate()

        for (event in events.toList())
            if (id == event.id) events.remove(event)
    }

    fun handle(){ Thread(){
        while (true){ //try{
            for (event in events){
                val msg: Utils.Msg = Utils.Msg()
                msg.user = Utils.User().getUser(event.userId)
                msg.chatId = event.peerId

                if (event.type == "timer"){
                    if (event.data.getLong("timer") <= System.currentTimeMillis()){
                        event.context.invoke(msg, event.data)

                        if (event.data.has("repeat")){
                            event.data.put("timer",System.currentTimeMillis()+event.data.getLong("repeat"))
                            event.update()
                        } else delete(event.id)
                    }
                }
            }

            //} catch (e: Exception){ println("ашибка"); println(e.message) }
            Thread.sleep(1000)
        } }.start()
    }
}