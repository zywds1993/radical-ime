package com.davidwang.radicaleime

/**
 * 输入引擎核心
 * 从 Python 版本移植到 Kotlin
 */
class InputEngine(private val dataStore: DataStore) {

    var output: String = ""
    var currentMode: String = "radical"  // radical | stroke | pinyin | voice
    var selectedRadical: String? = null
    var strokeFilter: Int? = null
    var candidates: MutableList<String> = mutableListOf()
    var page: Int = 0
    val pageSize: Int = 7
    var voiceText: String = ""
    var strokeSequence: MutableList<String> = mutableListOf()
    var pinyinConsumeLength: Int = 0

    fun switchMode(mode: String) {
        currentMode = mode
        resetInputState()
    }

    fun resetInputState() {
        selectedRadical = null
        strokeFilter = null
        candidates.clear()
        page = 0
        voiceText = ""
        strokeSequence.clear()
        pinyinConsumeLength = 0
    }

    /**
     * 输入偏旁
     */
    fun inputRadical(radical: String) {
        selectedRadical = radical
        strokeFilter = null
        page = 0
        candidates = dataStore.getRadicalCandidates(radical).toMutableList()
    }

    /**
     * 笔画筛选
     */
    fun filterByStroke(strokeCount: Int) {
        selectedRadical?.let { radical ->
            strokeFilter = strokeCount
            page = 0
            candidates = dataStore.getRadicalCandidates(radical, strokeCount).toMutableList()
        }
    }

    /**
     * 笔画输入
     */
    fun inputStroke() {
        page = 0
        val seq = strokeSequence.joinToString("")
        candidates = dataStore.searchByStrokeSeq(seq).toMutableList()
    }

    /**
     * 拼音输入
     */
    fun inputPinyin(pinyin: String) {
        val py = pinyin.trim().lowercase()
        val direct = dataStore.searchByPinyin(py)
        if (direct.isNotEmpty()) {
            candidates = direct.toMutableList()
            pinyinConsumeLength = py.length
        } else {
            val split = dataStore.searchFirstPinyinSegment(py)
            if (split != null) {
                pinyinConsumeLength = split.first.length
                candidates = split.second.toMutableList()
            } else {
                pinyinConsumeLength = 0
                candidates.clear()
            }
        }
        page = 0
    }

    /**
     * 选择候选字
     */
    fun selectCandidate(index: Int): String? {
        val absoluteIndex = page * pageSize + index
        return if (absoluteIndex in 0 until candidates.size) {
            val char = candidates[absoluteIndex]
            output += char
            resetInputState()
            char
        } else {
            null
        }
    }

    /**
     * 获取当前页候选字
     */
    fun getPageCandidates(): Pair<List<String>, Int> {
        val start = page * pageSize
        val end = minOf(start + pageSize, candidates.size)
        return Pair(candidates.subList(start, end), candidates.size)
    }

    fun nextPage() {
        val totalPages = (candidates.size + pageSize - 1) / pageSize
        if (page < totalPages - 1) page++
    }

    fun prevPage() {
        if (page > 0) page--
    }

    fun backspace(): String {
        if (output.isNotEmpty()) {
            output = output.dropLast(1)
        }
        return output
    }

    fun clear(): String {
        output = ""
        resetInputState()
        return output
    }

    fun getAssociations(char: String): List<String> {
        return dataStore.getAssociations(char)
    }
}
