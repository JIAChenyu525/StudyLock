package com.studylock.app.feature.focus

import kotlin.random.Random

object MotivationalQuotes {

    val inspirational = listOf(
        "今天的坚持，是明天简历上看不见的竞争力",
        "你现在的努力，是未来的你在感谢自己",
        "每一次专注，都是在为更好的自己投资",
        "没有人能替你成长，专注是你最好的武器",
        "熬过无人问津的日子，才有诗和远方"
    )

    val gentle = listOf(
        "课已经来了，手机还跑不了，先上完这节再说",
        "放下手机吧，这节课很快就结束了",
        "给自己一个专注的机会，你值得更好的课堂体验",
        "慢慢来，一节一节课上完，你会感谢现在的自己",
        "别急，下课后手机还在，但这节课的知识不会重来"
    )

    val humorous = listOf(
        "抖音算法比你更希望你不专注，别让它赢",
        "你的手机不会因为你没看它就生气，但你的成绩会",
        "放下手机，你的手指需要休息，你的大脑需要运转",
        "再刷5分钟？这话说得像'再睡5分钟'一样不靠谱",
        "手机：你不上课吗？我：再刷一会儿。手机：你确定？"
    )

    private val allQuotes = inspirational + gentle + humorous

    fun getRandom(): String {
        return allQuotes[Random.nextInt(allQuotes.size)]
    }

    fun getRandomByType(type: QuoteType): String {
        val list = when (type) {
            QuoteType.INSPIRATIONAL -> inspirational
            QuoteType.GENTLE -> gentle
            QuoteType.HUMOROUS -> humorous
        }
        return list[Random.nextInt(list.size)]
    }

    fun getRandomWithRandomType(): Pair<String, QuoteType> {
        val type = QuoteType.entries[Random.nextInt(QuoteType.entries.size)]
        return getRandomByType(type) to type
    }
}

enum class QuoteType {
    INSPIRATIONAL,
    GENTLE,
    HUMOROUS
}
