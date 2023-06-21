package plugins

import PluginBase
import Plugins
import Telegram
import Utils

import org.jsoup.Jsoup

class GenshinArts: PluginBase() {
    override val names = mutableListOf("гиартс", "giarts")
    override val desc = "Модератор в группе по артам"
    override val level = 256
    override val hidden = true

    val requests = Utils.Requests()
    var apiUrl = "https://gelbooru.com/index.php?page=dapi&s=post&q=index&limit=50&tags="
    val tags = mutableListOf<String>("genshin_impact", "rating:general")
    val blacklist = mutableListOf<String>("loli")

    init { Plugins.initPlugin(this) }

    override fun main(msg: Utils.Msg){
        apiUrl = "$apiUrl${tags.joinToString(separator = "+")}+-${blacklist.joinToString(separator = "+-")}"

        val response = requests.get(apiUrl,
            proxy = mutableListOf("localhost", 25344)
        ).text
        val doc = Jsoup.parse(response)

        val posts = doc.getElementsByTag("post")
        for (post in posts){
            //println(post.getElementsByTag("file_url").text())
        }

        msg.sendMessage("тест",
            keyboard = Telegram.Keyboard().setButtons(
                mutableListOf(
                    mutableListOf("пук","хрю"),
                )
            ).build())

    }
}
