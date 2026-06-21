package com.davidwang.radicaleime

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader

/**
 * 数据存储和加载
 * 从 assets 目录加载 JSON 数据文件
 */
class DataStore(private val context: Context) {

    var radicalIndex: Map<String, List<String>> = emptyMap()
    var radicals: List<String> = listOf()
    var words: Map<String, List<String>> = emptyMap()
    var standaloneChars: List<String> = listOf()
    var pinyinMap: Map<String, List<String>> = emptyMap()
    var pinyinPhraseMap: Map<String, List<String>> = emptyMap()
    var charStrokes: Map<String, List<Int>> = emptyMap()
    var charStrokeSeq: Map<String, String> = emptyMap()
    private var charRank: Map<String, Int> = emptyMap()

    init {
        loadAllData()
    }

    private fun loadAllData() {
        try {
            radicalIndex = loadJsonStringList("radical_index.json")
            Log.d("DataStore", "radical_index: ${radicalIndex.size} 个组合")

            pinyinMap = loadJsonStringList("pinyin_map.json")
            Log.d("DataStore", "pinyin_map: ${pinyinMap.size} 个类别")

            pinyinPhraseMap = loadJsonStringList("pinyin_phrase_map.json")
            Log.d("DataStore", "pinyin_phrase_map: ${pinyinPhraseMap.size} 个首字母组合")

            charStrokes = loadJsonIntList("char_strokes.json")
            Log.d("DataStore", "char_strokes: ${charStrokes.size} 个字")

            charStrokeSeq = loadJsonStringValue("char_stroke_seq.json")
            Log.d("DataStore", "char_stroke_seq: ${charStrokeSeq.size} 个字")

            charRank = buildCharRank()

            words = loadJsonStringList("association_words.json")
            Log.d("DataStore", "association_words: ${words.size} 个词头")
        } catch (e: Exception) {
            Log.e("DataStore", "加载数据失败", e)
        }

        val commonRadicals = listOf(
            "口", "氵", "亻", "木", "扌", "日", "艹", "讠", "纟", "火",
            "月", "目", "足", "辶", "犭", "宀", "山", "王", "石", "田",
            "女", "贝", "刂", "车", "禾", "米", "饣", "囗", "土", "钅",
            "忄", "阝", "心", "竹", "彳", "页", "广", "攵", "冫", "巾",
            "虫", "力", "疒", "礻", "衤", "雨", "酉", "皿", "骨", "黑",
            "鬼", "尸", "门", "穴", "大", "弓", "马", "耳", "走", "欠",
            "戈", "血", "手", "人", "女", "子", "一", "二", "三", "四",
            "五", "六", "七", "八", "九", "十", "百", "千", "万"
        )
        val allRadicals = radicalIndex.keys.map { it.substringBefore("|") }.distinct()
        radicals = (commonRadicals + allRadicals)
            .distinct()
            .filter { it.isNotBlank() }

        standaloneChars = listOf(
            "一", "不", "大", "了", "中", "人", "上", "个", "我", "以",
            "也", "子", "为", "之", "与", "来",
            "小", "工", "才", "马", "书", "乐", "久"
        )
    }

    private fun loadJsonStringList(fileName: String): Map<String, List<String>> {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            val jsonStr = reader.readText()
            reader.close()
            val map = mutableMapOf<String, List<String>>()
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key) as? Any
                if (value is JSONArray) {
                    val list = mutableListOf<String>()
                    for (i in 0 until value.length()) {
                        list.add(value.getString(i))
                    }
                    map[key] = list
                }
            }
            map
        } catch (e: Exception) {
            Log.w("DataStore", "加载 $fileName 失败: ${e.message}")
            emptyMap()
        }
    }

    private fun loadJsonStringValue(fileName: String): Map<String, String> {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            val jsonStr = reader.readText()
            reader.close()
            val map = mutableMapOf<String, String>()
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.getString(key)
            }
            map
        } catch (e: Exception) {
            Log.w("DataStore", "加载 $fileName 失败: ${e.message}")
            emptyMap()
        }
    }

    private fun buildCharRank(): Map<String, Int> {
        val rank = mutableMapOf<String, Int>()
        var i = 0

        // 手工高频字序：用于偏旁/笔画候选排序，保证前几笔先出日常常用字
        val common = (
            "的一是在不了有和人这中大为上个国我以要他时来用们生到作地于出就分对成会可主发年动同工也能下过子说产种面而方后多定行学法所民得经十三之进着等部度家电力里如水化高自二理起小物现实加量都两体制机当使点从业本去把性好应开它合还因由其些然前外天政四日那社义事平形相全表间样与关各重新线内数正心反你明看原又么利比或但质气第向道命此变条只没结解问意建月公无系军很情者最立代想已通并提直题党程展五果料象员革位入常文总次品式活设及管特件长求老头基资边流路级少图山统接知较将组见计别她手角期根论运农指几九区强放决西被干做必战先回则任取据处队南给色光门即保治北造百规热领七海口东导器压志世金增争济阶油思术极交受联什认六共权收证改清己美再采转更单风切打白教速花带安场身车例真务具万每目至达走积示议声报斗完类八离华名确才科张信马节话米整空元况今集温传土许步群广石记需段研界拉林律叫且究观越织装影算低持音众书布复容儿须际商非验连断深难近矿千周委素技备半办青省列习响约支般史感劳便团往酸历市克何除消构府称太准精值号率族维划选标写存候毛亲快效斯院查江型眼王按格养易置派层片始却专状育厂京识适属圆包火住调满县局照参红细引听该铁价严龙飞"
        )
        for (ch in common) {
            val c = ch.toString()
            if (!rank.containsKey(c)) {
                rank[c] = i
                i++
            }
        }

        for (chars in pinyinMap.values) {
            for (ch in chars) {
                if (!rank.containsKey(ch)) {
                    rank[ch] = i
                    i++
                }
            }
        }
        return rank
    }

    private fun loadJsonIntList(fileName: String): Map<String, List<Int>> {
        return try {
            val inputStream = context.assets.open(fileName)
            val reader = InputStreamReader(inputStream)
            val jsonStr = reader.readText()
            reader.close()
            val map = mutableMapOf<String, List<Int>>()
            val jsonObject = JSONObject(jsonStr)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key) as? Any
                if (value is JSONArray) {
                    val list = mutableListOf<Int>()
                    for (i in 0 until value.length()) {
                        val str = value.getString(i)
                        list.add(str.toIntOrNull() ?: 0)
                    }
                    map[key] = list
                }
            }
            map
        } catch (e: Exception) {
            Log.w("DataStore", "加载 $fileName 失败: ${e.message}")
            emptyMap()
        }
    }

    private fun sortRadicalCandidates(radical: String, chars: List<String>): List<String> {
        val distinct = chars.distinct()
        return distinct.sortedWith(
            compareBy<String> { if (it == radical) 0 else 1 }
                .thenBy { charRank[it] ?: 999999 }
        )
    }

    fun getRadicalCandidates(radical: String, strokeCount: Int? = null, commonOnly: Boolean = false): List<String> {
        if (strokeCount != null) {
            val key = "$radical|$strokeCount"
            return sortRadicalCandidates(radical, radicalIndex[key] ?: emptyList())
        }
        val result = mutableListOf<String>()
        for ((key, chars) in radicalIndex) {
            if (key.startsWith("$radical|")) {
                result.addAll(chars)
            }
        }
        val sorted = sortRadicalCandidates(radical, result)
        return if (commonOnly) sorted.take(84) else sorted
    }

    private fun searchSinglePinyin(pyRaw: String): List<String> {
        val py = pyRaw.trim().lowercase()
        if (py.isEmpty()) return emptyList()
        val pyNoTone = py.replace(Regex("[1-5]$"), "")
        val result = mutableListOf<String>()
        pinyinMap[py]?.let { result.addAll(it) }
        pinyinMap[pyNoTone]?.let { result.addAll(it) }
        if (result.isEmpty()) {
            for ((key, chars) in pinyinMap) {
                if (key.replace(Regex("[1-5]$"), "") == pyNoTone) {
                    result.addAll(chars)
                    break
                }
            }
        }
        return result.distinct()
    }

    fun searchByPinyin(pinyin: String): List<String> {
        val py = pinyin.trim().lowercase()
        if (py.isEmpty()) return emptyList()
        val pyNoTone = py.replace(Regex("[1-5]$"), "")
        val result = mutableListOf<String>()

        // 首字母/整句拼音短语优先，例如 wm -> 我们，zgrm -> 中国人民
        pinyinPhraseMap[py]?.let { result.addAll(it) }
        if (pyNoTone != py) pinyinPhraseMap[pyNoTone]?.let { result.addAll(it) }

        // 单字拼音候选
        result.addAll(searchSinglePinyin(pyNoTone))
        return result.distinct().take(160)
    }

    fun searchFirstPinyinSegment(pinyin: String): Pair<String, List<String>>? {
        val py = pinyin.trim().lowercase().replace(Regex("[1-5]$"), "")
        if (py.length <= 1) return null
        val maxLen = minOf(6, py.length - 1)
        for (len in maxLen downTo 1) {
            val segment = py.substring(0, len)
            val chars = searchSinglePinyin(segment).take(160)
            if (chars.isNotEmpty()) {
                return Pair(segment, chars)
            }
        }
        return null
    }

    fun searchByStrokeSeq(seq: String): List<String> {
        val s = seq.trim()
        if (s.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        for ((char, strokeSeq) in charStrokeSeq) {
            if (strokeSeq.startsWith(s)) {
                result.add(char)
            }
        }

        // 排序原则：
        // 1. 如果笔画已经完整匹配某些字，完整匹配优先；
        // 2. 短序列阶段优先常用字，让前两三笔就能看到“我、人、你、的”等；
        // 3. 常用度相近时再按总笔画少的靠前。
        return result.sortedWith(
            compareBy<String> { if (charStrokeSeq[it] == s) 0 else 1 }
                .thenBy { charRank[it] ?: 999999 }
                .thenBy { charStrokeSeq[it]?.length ?: 99 }
        ).take(160)
    }

    fun searchByStrokeCount(strokeCount: Int): List<String> {
        val result = mutableListOf<String>()
        for ((char, strokes) in charStrokes) {
            if (strokeCount in strokes) {
                result.add(char)
            }
        }
        return result.take(30)
    }

    fun getAssociations(char: String): List<String> {
        return words[char] ?: emptyList()
    }

    fun getCharStrokes(char: String): List<Int> {
        return charStrokes[char] ?: emptyList()
    }
}
