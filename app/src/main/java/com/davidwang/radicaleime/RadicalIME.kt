package com.davidwang.radicaleime

import android.inputmethodservice.InputMethodService
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import kotlin.math.abs

class RadicalIME : InputMethodService() {

    private lateinit var dataStore: DataStore
    private lateinit var inputEngine: InputEngine

    private lateinit var rootView: LinearLayout
    private var tvInput: TextView? = null
    private var tvStatus: TextView? = null
    private var tvPage: TextView? = null
    private var tvAssociations: TextView? = null
    private var etPinyin: EditText? = null
    private var candidateBar: LinearLayout? = null
    private var pageBar: LinearLayout? = null
    private var layoutCandidates: LinearLayout? = null
    private var btnCandidates = arrayOfNulls<Button>(7)
    private var btnStrokeFilters = arrayOfNulls<Button>(20)
    private var btnRadicalKeys = arrayOfNulls<Button>(60)
    private var btnPrev: Button? = null
    private var btnNext: Button? = null
    private var btnBackspace: Button? = null
    private var btnClear: Button? = null
    private var btnSpace: Button? = null
    private var btnEnter: Button? = null
    private var btnVoice: Button? = null
    private var btnSymbol: Button? = null
    private var btnStrokeMode: Button? = null
    private var btnHandwritingMode: Button? = null
    private var handwritingPad: HandwritingPadView? = null
    private var tvStrokeSeq: TextView? = null
    private var rbRadical: RadioButton? = null
    private var rbPinyin: RadioButton? = null
    private var scrollStrokeFilter: HorizontalScrollView? = null
    private var layoutStrokeKeys: LinearLayout? = null
    private var functionBar: LinearLayout? = null
    private var associationMode = false
    private var radicalPage: Int = 0
    private val radicalsPerPage: Int = 20
    private var currentKeyboardMode: String = "radical"
    private var symbolPage: Int = 0
    private val symbolsPerPage: Int = 50
    private var associationPrefix = ""
    private val commonSymbols = listOf(
        "，", "。", "、", "？", "！", "：", "；", "……", "——", "·",
        "“", "”", "‘", "’", "（", "）", "【", "】", "《", "》",
        "〈", "〉", "「", "」", "『", "』", "〔", "〕", "〖", "〗",
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
        "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
        "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d",
        "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
        "o", "p", "q", "r", "s", "t", "u", "v", "w", "x",
        "y", "z", "@", "#", "$", "%", "&", "*", "_", "/",
        "\\", "|", ".", ",", "?", "!", ":", ";", "'", "\"",
        "(", ")", "[", "]", "{", "}", "<", ">", "+", "-",
        "=", "~", "^", "`", "￥", "$", "€", "£", "¥", "₩",
        "＋", "－", "×", "÷", "＝", "≠", "≈", "≤", "≥", "±",
        "√", "∞", "∑", "∏", "∫", "∵", "∴", "∈", "∩", "∪",
        "℃", "℉", "°", "‰", "㎡", "m³", "kg", "g", "km", "cm",
        "→", "←", "↑", "↓", "↔", "↕", "⇒", "⇐", "⇑", "⇓",
        "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
        "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ",
        "☆", "★", "○", "●", "□", "■", "◇", "◆", "△", "▲",
        "▽", "▼", "※", "№", "©", "®", "™", "✓", "✔", "✕",
        "α", "β", "γ", "δ", "ε", "θ", "λ", "μ", "π", "ω"
    )

    override fun onCreate() {
        super.onCreate()
        Log.i("RadicalIME", "onCreate")
        dataStore = DataStore(applicationContext)
        inputEngine = InputEngine(dataStore)
    }

    override fun onCreateInputView(): View {
        Log.i("RadicalIME", "onCreateInputView")
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@RadicalIME, R.color.bg_color))
            setPadding(2, 2, 2, 2)
        }
        rootView.addView(createModeBar())
        tvAssociations = TextView(this).apply {
            textSize = 10f
            setTextColor(ContextCompat.getColor(this@RadicalIME, android.R.color.darker_gray))
            setPadding(2, 2, 2, 2)
            visibility = View.GONE
        }
        rootView.addView(tvAssociations!!)
        candidateBar = createCandidateBar()
        candidateBar!!.visibility = View.GONE
        rootView.addView(candidateBar!!)
        pageBar = createPageBar()
        pageBar!!.visibility = View.GONE
        rootView.addView(pageBar!!)
        scrollStrokeFilter = createStrokeFilterBar()
        rootView.addView(scrollStrokeFilter!!)
        scrollStrokeFilter!!.visibility = View.GONE
        rootView.addView(createRadicalKeys())
        layoutStrokeKeys = createStrokeKeys()
        layoutStrokeKeys!!.visibility = View.GONE
        rootView.addView(layoutStrokeKeys!!)
        functionBar = createFunctionBar()
        rootView.addView(functionBar!!)
        return rootView
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun lp(w: Int, h: Int) = LinearLayout.LayoutParams(w, h)
    private fun lp(w: Int, h: Int, weight: Float) = LinearLayout.LayoutParams(w, h, weight)
    private fun lpWrap() = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun setPinyinText(value: String) {
        etPinyin?.setText(value)
        etPinyin?.setSelection(value.length)
    }

    private fun learnText(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t.length > 20) return
        val prefs = getSharedPreferences("user_learning", MODE_PRIVATE)
        val key = "freq_" + t
        val old = prefs.getInt(key, 0)
        prefs.edit().putInt(key, old + 1).apply()
    }

    private fun learnedScore(text: String): Int {
        if (text.isEmpty()) return 0
        return getSharedPreferences("user_learning", MODE_PRIVATE).getInt("freq_" + text, 0)
    }

    private fun penalizeText(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        getSharedPreferences("user_learning", MODE_PRIVATE)
            .edit()
            .putInt("freq_" + t, -50)
            .apply()
    }

    private fun clearLearning() {
        getSharedPreferences("user_learning", MODE_PRIVATE).edit().clear().apply()
        inputEngine.page = 0
        updateCandidates()
        tvStatus?.text = "学习记录已清空"
    }

    private fun selectFirstCandidateOrSpace() {
        if (inputEngine.candidates.isNotEmpty()) {
            handleCandidateClick(0)
        } else {
            commitText(" ")
        }
    }

    private fun applyUserLearningRank() {
        if (inputEngine.candidates.size <= 1) return
        inputEngine.candidates = inputEngine.candidates
            .withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<String>> { learnedScore(it.value) }
                    .thenBy { it.index }
            )
            .map { it.value }
            .toMutableList()
    }

    private fun setMargins(view: View, l: Int, t: Int, r: Int, b: Int) {
        val p = view.layoutParams as? ViewGroup.MarginLayoutParams
            ?: ViewGroup.MarginLayoutParams(view.width, view.height)
        p.setMargins(l, t, r, b)
        view.layoutParams = p
    }

    private fun createModeBar(): LinearLayout {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(4, 2, 4, 2)
        }

        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        rbRadical = RadioButton(this@RadicalIME).apply {
            text = "偏旁"
            textSize = 11f
            isChecked = true
            setPadding(2, 0, 2, 0)
            setOnClickListener { switchMode("radical") }
        }
        modeRow.addView(rbRadical!!, lpWrap())

        rbPinyin = RadioButton(this@RadicalIME).apply {
            text = "拼音"
            textSize = 11f
            setPadding(2, 0, 2, 0)
            setOnClickListener { switchMode("pinyin") }
        }
        modeRow.addView(rbPinyin!!, lpWrap())

        btnSymbol = Button(this@RadicalIME).apply {
            text = "符号"
            textSize = 11f
            minWidth = 0
            minHeight = dp(30)
            setPadding(6, 1, 6, 1)
            setOnClickListener { switchMode("symbol") }
        }
        modeRow.addView(btnSymbol!!, lpWrap())

        btnStrokeMode = Button(this@RadicalIME).apply {
            text = "笔画"
            textSize = 11f
            minWidth = 0
            minHeight = dp(30)
            setPadding(6, 1, 6, 1)
            setOnClickListener { switchMode("stroke") }
        }
        modeRow.addView(btnStrokeMode!!, lpWrap())

        outer.addView(modeRow)

        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        etPinyin = EditText(this@RadicalIME).apply {
            hint = "拼音"
            textSize = 14f
            visibility = View.GONE
            setOnKeyListener { _, _, event ->
                if (event.action == KeyEvent.ACTION_UP) {
                    val pinyin = text.toString().trim().lowercase()
                    if (pinyin.isNotEmpty()) {
                        inputEngine.inputPinyin(pinyin)
                        updateCandidates()
                    }
                }
                false
            }
        }
        infoRow.addView(etPinyin!!, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        btnVoice = Button(this@RadicalIME).apply {
            text = "\uD83C\uDF99"
            textSize = 14f
            minWidth = 0
            minHeight = dp(30)
            setPadding(6, 1, 6, 1)
            setOnClickListener {
                inputEngine.voiceText = "你好世界"
                inputEngine.candidates = mutableListOf("你", "好", "世", "界")
                inputEngine.page = 0
                updateCandidates()
                tvStatus?.text = "语音: 你好世界"
            }
        }
        infoRow.addView(btnVoice!!, lpWrap())

        val btnClearLearning = Button(this@RadicalIME).apply {
            text = "清学习"
            textSize = 10f
            minWidth = 0
            minHeight = dp(30)
            setPadding(4, 1, 4, 1)
            setOnClickListener { clearLearning() }
        }
        infoRow.addView(btnClearLearning, lpWrap())

        tvStatus = TextView(this@RadicalIME).apply {
            textSize = 10f
            setTextColor(ContextCompat.getColor(this@RadicalIME, android.R.color.darker_gray))
            gravity = android.view.Gravity.END
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        infoRow.addView(tvStatus!!)
        outer.addView(infoRow)

        return outer
    }

    private fun createInputDisplay(): ScrollView {
        return ScrollView(this).apply {
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, 120)
            setBackgroundColor(android.graphics.Color.WHITE)
            tvInput = TextView(this@RadicalIME).apply {
                textSize = 18f
                setPadding(4, 2, 4, 2)
            }
            addView(tvInput!!)
        }
    }

    private fun createCandidateBar(): LinearLayout {
        var downX = 0f
        var downY = 0f
        val swipeListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (abs(dx) > dp(45) && abs(dx) > abs(dy)) {
                        if (dx < 0) inputEngine.nextPage() else inputEngine.prevPage()
                        updateCandidates()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(android.graphics.Color.WHITE)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(50))
            setOnTouchListener(swipeListener)
            layoutCandidates = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setOnTouchListener(swipeListener)
                for (i in 0 until 7) {
                    btnCandidates[i] = Button(this@RadicalIME).apply {
                        text = ""
                        textSize = 24f
                        minWidth = 0
                        minHeight = dp(46)
                        setPadding(1, 1, 1, 1)
                        layoutParams = lp(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                        setOnTouchListener(swipeListener)
                        setOnClickListener {
                            handleCandidateClick(i)
                        }
                        setOnLongClickListener {
                            val absoluteIndex = inputEngine.page * inputEngine.pageSize + i
                            if (absoluteIndex in 0 until inputEngine.candidates.size) {
                                val item = inputEngine.candidates[absoluteIndex]
                                penalizeText(item)
                                inputEngine.page = 0
                                updateCandidates()
                                tvStatus?.text = "已降低候选: " + item
                                true
                            } else {
                                false
                            }
                        }
                    }
                    addView(btnCandidates[i]!!)
                }
            }
            addView(layoutCandidates!!)
        }
    }

    private fun createPageBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(4, 2, 4, 2)
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(34))
            btnPrev = Button(this@RadicalIME).apply {
                text = "\u25C0"
                textSize = 11f
                setPadding(8, 4, 8, 4)
                setOnClickListener { inputEngine.prevPage(); updateCandidates() }
            }
            addView(btnPrev!!)
            tvPage = TextView(this@RadicalIME).apply {
                textSize = 10f
                gravity = android.view.Gravity.CENTER
                layoutParams = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
                setTextColor(ContextCompat.getColor(this@RadicalIME, android.R.color.darker_gray))
            }
            addView(tvPage!!)
            btnNext = Button(this@RadicalIME).apply {
                text = "\u25B6"
                textSize = 11f
                setPadding(8, 4, 8, 4)
                setOnClickListener { inputEngine.nextPage(); updateCandidates() }
            }
            addView(btnNext!!)
        }
    }

    private fun createStrokeFilterBar(): HorizontalScrollView {
        return HorizontalScrollView(this).apply {
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(30))
            setBackgroundColor(-0x0A0A0A)
            val innerLayout = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(4, 2, 4, 2)
                Button(this@RadicalIME).apply {
                    text = "全部"
                    textSize = 10f
                    setPadding(4, 2, 4, 2)
                    setOnClickListener {
                        inputEngine.strokeFilter = null
                        inputEngine.candidates = dataStore.getRadicalCandidates(inputEngine.selectedRadical ?: "").toMutableList()
                        inputEngine.page = 0
                        updateCandidates()
                        tvStatus?.text = "偏旁: " + (inputEngine.selectedRadical ?: "")
                    }
                    addView(this)
                }
                for (i in 1..20) {
                    btnStrokeFilters[i - 1] = Button(this@RadicalIME).apply {
                        text = "$i"
                        textSize = 10f
                        setPadding(2, 2, 2, 2)
                        setOnClickListener {
                            if (inputEngine.selectedRadical != null) {
                                inputEngine.filterByStroke(i)
                                updateCandidates()
                                tvStatus?.text = "偏旁: " + inputEngine.selectedRadical + " | 笔画: " + i
                            }
                        }
                    }
                    addView(btnStrokeFilters[i - 1]!!)
                }
            }
            addView(innerLayout)
        }
    }

    private var radicalContainer: LinearLayout? = null

    private fun createRadicalKeys(): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(2, 2, 2, 2)
        }
        radicalContainer = container
        renderRadicalPage()
        return container
    }

    private fun renderRadicalPage() {
        val container = radicalContainer ?: return
        container.removeAllViews()

        // 上区：数字 + 英文键盘。数字单独一排，字母保持三排。
        val englishRows = listOf(
            listOf("1","2","3","4","5","6","7","8","9","0"),
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("z","x","c","v","b","n","m")
        )
        for ((rowIndex, rowKeys) in englishRows.withIndex()) {
            val rowLayout = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 1)
            }
            for (k in rowKeys) {
                val btn = Button(this@RadicalIME).apply {
                    text = k
                    textSize = if (rowIndex == 0) 13f else 14f
                    minWidth = 0
                    minHeight = dp(30)
                    setPadding(1, 1, 1, 1)
                    layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        currentInputConnection?.commitText(k, 1)
                        inputEngine.output += k
                        updateInputDisplay()
                        tvStatus?.text = if (rowIndex == 0) "数字: " + k else "英文: " + k
                    }
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }

        // 下区：偏旁部首做成真正的横向长键盘。手指左右滑动时连续移动，不再分页重画。
        val all = dataStore.radicals
        val pages = all.chunked(radicalsPerPage)

        val hint = TextView(this@RadicalIME).apply {
            text = "偏旁部首：左右滑动"
            textSize = 11f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 1, 0, 1)
        }
        container.addView(hint)

        val scroll = HorizontalScrollView(this@RadicalIME).apply {
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            isHorizontalScrollBarEnabled = true
            isFillViewport = false
            setPadding(0, 0, 0, 0)
        }

        val strip = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 0)
        }

        for ((pageIndex, pageRadicals) in pages.withIndex()) {
            val pageLayout = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(2, 0, 8, 0)
                layoutParams = lp(resources.displayMetrics.widthPixels, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            val rows = pageRadicals.chunked(10)
            for (rowRadicals in rows) {
                val rowLayout = LinearLayout(this@RadicalIME).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 1)
                }
                for (ch in rowRadicals) {
                    val btn = Button(this@RadicalIME).apply {
                        text = ch
                        textSize = 18f
                        minWidth = 0
                        minHeight = dp(34)
                        setPadding(1, 1, 1, 1)
                        layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                        setOnClickListener {
                            radicalPage = pageIndex
                            associationMode = false
                            associationPrefix = ""
                            val directCandidates = dataStore.getRadicalCandidates(ch)
                            if (directCandidates.size == 1 && directCandidates[0] == ch) {
                                currentInputConnection?.commitText(ch, 1)
                                inputEngine.output += ch
                                updateInputDisplay()
                                showAssociations(ch)
                                tvStatus?.text = "已输入: " + ch
                            } else {
                                inputEngine.inputRadical(ch)
                                updateCandidates()
                                tvStatus?.text = "偏旁: " + ch
                            }
                        }
                    }
                    rowLayout.addView(btn)
                }

                // 最后一页不足 10 个时补空位，保证每页宽度稳定
                repeat(10 - rowRadicals.size) {
                    val spacer = Space(this@RadicalIME).apply {
                        layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    rowLayout.addView(spacer)
                }

                pageLayout.addView(rowLayout)
            }
            strip.addView(pageLayout)
        }

        scroll.addView(strip)
        scroll.post {
            scroll.scrollTo(radicalPage * resources.displayMetrics.widthPixels, 0)
        }
        container.addView(scroll)
    }

    private fun renderSymbolPage() {
        val container = radicalContainer ?: return
        container.removeAllViews()
        val totalPages = (commonSymbols.size + symbolsPerPage - 1) / symbolsPerPage
        if (symbolPage >= totalPages) symbolPage = 0
        if (symbolPage < 0) symbolPage = totalPages - 1
        val start = symbolPage * symbolsPerPage
        val end = minOf(start + symbolsPerPage, commonSymbols.size)
        val pageSymbols = commonSymbols.subList(start, end)
        val rows = pageSymbols.chunked(10)
        for (rowSymbols in rows) {
            val rowLayout = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 2)
            }
            for (sym in rowSymbols) {
                val btn = Button(this@RadicalIME).apply {
                    text = sym
                    textSize = if (sym.length > 1) 14f else 18f
                    minWidth = 0
                    minHeight = dp(34)
                    setPadding(2, 2, 2, 2)
                    layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        currentInputConnection?.commitText(sym, 1)
                        inputEngine.output += sym
                        updateInputDisplay()
                        tvStatus?.text = "符号: " + sym
                    }
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }
        val navRow = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 2, 0, 2)
        }
        val btnPrev = Button(this@RadicalIME).apply {
            text = "<"
            textSize = 14f
            minWidth = 0
            minHeight = dp(30)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { symbolPage--; renderSymbolPage() }
        }
        val tvPage = android.widget.TextView(this@RadicalIME).apply {
            text = "符号 ${symbolPage + 1}/${totalPages}"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
        }
        val btnNext = Button(this@RadicalIME).apply {
            text = ">"
            textSize = 14f
            minWidth = 0
            minHeight = dp(30)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { symbolPage++; renderSymbolPage() }
        }
        navRow.addView(btnPrev)
        navRow.addView(tvPage)
        navRow.addView(btnNext)
        container.addView(navRow)
    }

    private fun renderPinyinKeyboard() {
        val container = radicalContainer ?: return
        container.removeAllViews()
        val rows = listOf(
            listOf("q","w","e","r","t","y","u","i","o","p"),
            listOf("a","s","d","f","g","h","j","k","l"),
            listOf("z","x","c","v","b","n","m")
        )
        for (rowKeys in rows) {
            val rowLayout = LinearLayout(this@RadicalIME).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER
                setPadding(0, 0, 0, 2)
            }
            for (k in rowKeys) {
                val btn = Button(this@RadicalIME).apply {
                    text = k
                    textSize = 16f
                    minWidth = 0
                    minHeight = dp(38)
                    setPadding(2, 2, 2, 2)
                    layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        val cur = etPinyin?.text?.toString() ?: ""
                        val newText = cur + k
                        setPinyinText(newText)
                        inputEngine.inputPinyin(newText.trim().lowercase())
                        updateCandidates()
                        tvStatus?.text = "拼音: " + newText
                    }
                }
                rowLayout.addView(btn)
            }
            container.addView(rowLayout)
        }
        // 第四行：删除 / 空格 / 确认
        val rowLayout = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 2, 0, 2)
        }
        val btnDel = Button(this@RadicalIME).apply {
            text = "⌫"
            textSize = 16f
            minWidth = 0
            minHeight = dp(38)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                val cur = etPinyin?.text?.toString() ?: ""
                if (cur.isNotEmpty()) {
                    val nt = cur.dropLast(1)
                    setPinyinText(nt)
                    if (nt.isNotEmpty()) {
                        inputEngine.inputPinyin(nt.trim().lowercase())
                        updateCandidates()
                    } else {
                        inputEngine.candidates.clear()
                        updateCandidates()
                    }
                } else {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    if (inputEngine.output.isNotEmpty()) inputEngine.output = inputEngine.output.dropLast(1)
                    updateInputDisplay()
                }
            }
        }
        val btnClearPy = Button(this@RadicalIME).apply {
            text = "清空"
            textSize = 13f
            minWidth = 0
            minHeight = dp(38)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                setPinyinText("")
                inputEngine.candidates.clear()
                updateCandidates()
            }
        }
        val btnSpacePy = Button(this@RadicalIME).apply {
            text = "空格"
            textSize = 13f
            minWidth = 0
            minHeight = dp(38)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 4f)
            setOnClickListener {
                if ((etPinyin?.text?.toString() ?: "").isNotEmpty() && inputEngine.candidates.isNotEmpty()) {
                    handleCandidateClick(0)
                } else {
                    commitText(" ")
                }
            }
        }
        val btnEnterPy = Button(this@RadicalIME).apply {
            text = "回车"
            textSize = 13f
            minWidth = 0
            minHeight = dp(38)
            setPadding(2, 2, 2, 2)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener { commitText("\n") }
        }
        rowLayout.addView(btnDel)
        rowLayout.addView(btnClearPy)
        rowLayout.addView(btnSpacePy)
        rowLayout.addView(btnEnterPy)
        container.addView(rowLayout)
    }

    private fun renderStrokeKeyboard() {
        val container = radicalContainer ?: return
        container.removeAllViews()

        val hint = TextView(this@RadicalIME).apply {
            text = "笔画输入：一横 丨竖 丿撇 丶点/捺 乙折"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(2, 2, 2, 2)
        }
        container.addView(hint)

        tvStrokeSeq = TextView(this@RadicalIME).apply {
            textSize = 15f
            setTextColor(Color.rgb(40, 40, 40))
            gravity = android.view.Gravity.CENTER
            setPadding(2, 3, 2, 3)
            text = "当前笔画：未输入"
        }
        container.addView(tvStrokeSeq!!)

        val strokes = listOf(
            Triple("1", "一", "横"),
            Triple("2", "丨", "竖"),
            Triple("3", "丿", "撇"),
            Triple("4", "丶", "点"),
            Triple("5", "乙", "折")
        )
        val rowLayout = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 2, 0, 3)
        }
        for ((code, ch, name) in strokes) {
            val btn = Button(this@RadicalIME).apply {
                text = ch + "\n" + name
                textSize = 15f
                minWidth = 0
                minHeight = dp(58)
                setPadding(2, 2, 2, 2)
                layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    inputEngine.strokeSequence.add(code)
                    inputEngine.inputStroke()
                    updateCandidates()
                    updateStrokeSeqDisplay()
                }
            }
            rowLayout.addView(btn)
        }
        container.addView(rowLayout)

        val toolRow = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 2, 0, 3)
        }
        val btnDel = Button(this@RadicalIME).apply {
            text = "← 退一笔"
            textSize = 14f
            minWidth = 0
            minHeight = dp(48)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
            setOnClickListener {
                if (inputEngine.strokeSequence.isNotEmpty()) {
                    inputEngine.strokeSequence.removeAt(inputEngine.strokeSequence.size - 1)
                    if (inputEngine.strokeSequence.isNotEmpty()) {
                        inputEngine.inputStroke()
                    } else {
                        inputEngine.candidates.clear()
                    }
                    updateCandidates()
                    updateStrokeSeqDisplay()
                } else {
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    if (inputEngine.output.isNotEmpty()) inputEngine.output = inputEngine.output.dropLast(1)
                    updateInputDisplay()
                }
            }
        }
        val btnClr = Button(this@RadicalIME).apply {
            text = "清空笔画"
            textSize = 14f
            minWidth = 0
            minHeight = dp(48)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f)
            setOnClickListener {
                inputEngine.strokeSequence.clear()
                inputEngine.candidates.clear()
                updateCandidates()
                updateStrokeSeqDisplay()
            }
        }
        val btnSp = Button(this@RadicalIME).apply {
            text = "空格/首选"
            textSize = 13f
            minWidth = 0
            minHeight = dp(48)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                if (inputEngine.strokeSequence.isNotEmpty() && inputEngine.candidates.isNotEmpty()) {
                    handleCandidateClick(0)
                    updateStrokeSeqDisplay()
                } else {
                    commitText(" ")
                }
            }
        }
        val btnEn = Button(this@RadicalIME).apply {
            text = "回车"
            textSize = 13f
            minWidth = 0
            minHeight = dp(48)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { commitText("\n") }
        }
        toolRow.addView(btnClr)
        toolRow.addView(btnSp)
        toolRow.addView(btnEn)
        toolRow.addView(btnDel)
        container.addView(toolRow)
        updateStrokeSeqDisplay()
    }

    private fun strokeSeqLabel(): String {
        val names = mapOf("1" to "一", "2" to "丨", "3" to "丿", "4" to "丶", "5" to "乙")
        return inputEngine.strokeSequence.map { names[it] ?: it }.joinToString(" ")
    }

    private fun updateStrokeSeqDisplay() {
        val label = strokeSeqLabel()
        tvStrokeSeq?.text = if (label.isEmpty()) "当前笔画：未输入" else "当前笔画：$label"
        tvStatus?.text = if (label.isEmpty()) "笔画序列已清空" else "笔画: $label"
    }

    private fun renderHandwritingKeyboard() {
        val container = radicalContainer ?: return
        container.removeAllViews()

        val hint = TextView(this@RadicalIME).apply {
            text = "手写笔画：在下面写笔画，系统按横竖撇点折出候选"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(2, 2, 2, 2)
        }
        container.addView(hint)

        handwritingPad = HandwritingPadView(this@RadicalIME) { code ->
            inputEngine.strokeSequence.add(code)
            inputEngine.inputStroke()
            updateCandidates()
            tvStatus?.text = "手写笔画: " + strokeSeqLabel()
        }.apply {
            layoutParams = lp(ViewGroup.LayoutParams.MATCH_PARENT, dp(150))
        }
        container.addView(handwritingPad!!)

        val toolRow = LinearLayout(this@RadicalIME).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 2, 0, 3)
        }
        val btnUndo = Button(this@RadicalIME).apply {
            text = "退一笔"
            textSize = 13f
            minWidth = 0
            minHeight = dp(40)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                handwritingPad?.undoStroke()
                if (inputEngine.strokeSequence.isNotEmpty()) {
                    inputEngine.strokeSequence.removeAt(inputEngine.strokeSequence.size - 1)
                }
                if (inputEngine.strokeSequence.isNotEmpty()) {
                    inputEngine.inputStroke()
                } else {
                    inputEngine.candidates.clear()
                }
                updateCandidates()
                tvStatus?.text = "手写笔画: " + strokeSeqLabel()
            }
        }
        val btnClr = Button(this@RadicalIME).apply {
            text = "清空"
            textSize = 13f
            minWidth = 0
            minHeight = dp(40)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                handwritingPad?.clearPad()
                inputEngine.strokeSequence.clear()
                inputEngine.candidates.clear()
                updateCandidates()
                tvStatus?.text = "手写已清空"
            }
        }
        val btnSp = Button(this@RadicalIME).apply {
            text = "空格"
            textSize = 13f
            minWidth = 0
            minHeight = dp(40)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)
            setOnClickListener {
                if (inputEngine.strokeSequence.isNotEmpty() && inputEngine.candidates.isNotEmpty()) {
                    handleCandidateClick(0)
                    handwritingPad?.clearPad()
                } else {
                    commitText(" ")
                }
            }
        }
        val btnEn = Button(this@RadicalIME).apply {
            text = "回车"
            textSize = 13f
            minWidth = 0
            minHeight = dp(40)
            layoutParams = lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { commitText("\n") }
        }
        toolRow.addView(btnUndo)
        toolRow.addView(btnClr)
        toolRow.addView(btnSp)
        toolRow.addView(btnEn)
        container.addView(toolRow)
    }

    private fun createStrokeKeys(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(2, 2, 2, 2)
            setBackgroundColor(android.graphics.Color.WHITE)
            val strokes = listOf("U+4E00" to "横", "U+4E28" to "竖", "U+4E36" to "撇", "U+4E38" to "点", "U+31CF" to "捺")
            for ((hex, name) in strokes) {
                val ch = hex.substring(3).toInt(16).toChar().toString()
                Button(this@RadicalIME).apply {
                    text = ch + "(" + name + ")"
                    textSize = 11f
                    setOnClickListener {
                        inputEngine.strokeSequence.add(ch)
                        inputEngine.inputStroke()
                        updateCandidates()
                        tvStatus?.text = "笔画: " + inputEngine.strokeSequence.joinToString(" ")
                    }
                    addView(this)
                }
            }
            Button(this@RadicalIME).apply {
                text = "清除"
                textSize = 11f
                setOnClickListener {
                    inputEngine.strokeSequence.clear()
                    inputEngine.candidates.clear()
                    inputEngine.page = 0
                    updateCandidates()
                    tvStatus?.text = "笔画序列已清除"
                }
                addView(this)
            }
        }
    }

    private fun createFunctionBar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(2, 2, 2, 2)
            btnBackspace = Button(this@RadicalIME).apply {
                text = "\u232B 退格"
                textSize = 11f
                setOnClickListener {
                    inputEngine.backspace()
                    currentInputConnection?.deleteSurroundingText(1, 0)
                    updateInputDisplay()
                    updateCandidates()
                    tvAssociations?.visibility = View.GONE
                }
                addView(this)
            }
            btnClear = Button(this@RadicalIME).apply {
                text = "清空"
                textSize = 11f
                setOnClickListener {
                    inputEngine.clear()
                    updateInputDisplay()
                    updateCandidates()
                    tvAssociations?.visibility = View.GONE
                    tvStatus?.text = "已清空"
                }
                addView(this)
            }
            btnSpace = Button(this@RadicalIME).apply {
                text = "空格"
                textSize = 11f
                layoutParams = lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
                setOnClickListener { selectFirstCandidateOrSpace() }
                addView(this)
            }
            btnEnter = Button(this@RadicalIME).apply {
                text = "回车"
                textSize = 11f
                setOnClickListener { commitText("\n") }
                addView(this)
            }
        }
    }

    private fun switchMode(mode: String) {
        inputEngine.switchMode(mode)
        when (mode) {
            "radical" -> {
                currentKeyboardMode = "radical"
                rbRadical?.isChecked = true
                rbPinyin?.isChecked = false
                scrollStrokeFilter?.visibility = View.GONE
                layoutStrokeKeys?.visibility = View.GONE
                radicalContainer?.visibility = View.VISIBLE
                functionBar?.visibility = View.VISIBLE
                etPinyin?.visibility = View.GONE
                renderRadicalPage()
            }
            "pinyin" -> {
                currentKeyboardMode = "pinyin"
                rbRadical?.isChecked = false
                rbPinyin?.isChecked = true
                scrollStrokeFilter?.visibility = View.GONE
                layoutStrokeKeys?.visibility = View.GONE
                radicalContainer?.visibility = View.VISIBLE
                functionBar?.visibility = View.GONE
                etPinyin?.visibility = View.VISIBLE
                renderPinyinKeyboard()
                etPinyin?.requestFocus()
            }
            "stroke" -> {
                currentKeyboardMode = "stroke"
                rbRadical?.isChecked = false
                rbPinyin?.isChecked = false
                scrollStrokeFilter?.visibility = View.GONE
                radicalContainer?.visibility = View.VISIBLE
                functionBar?.visibility = View.GONE
                layoutStrokeKeys?.visibility = View.GONE
                etPinyin?.visibility = View.GONE
                tvAssociations?.visibility = View.GONE
                renderStrokeKeyboard()
            }
            "handwriting" -> {
                currentKeyboardMode = "handwriting"
                rbRadical?.isChecked = false
                rbPinyin?.isChecked = false
                scrollStrokeFilter?.visibility = View.GONE
                radicalContainer?.visibility = View.VISIBLE
                functionBar?.visibility = View.GONE
                layoutStrokeKeys?.visibility = View.GONE
                etPinyin?.visibility = View.GONE
                tvAssociations?.visibility = View.GONE
                renderHandwritingKeyboard()
            }
            "symbol" -> {
                currentKeyboardMode = "symbol"
                rbRadical?.isChecked = false
                rbPinyin?.isChecked = false
                scrollStrokeFilter?.visibility = View.GONE
                layoutStrokeKeys?.visibility = View.GONE
                radicalContainer?.visibility = View.VISIBLE
                functionBar?.visibility = View.VISIBLE
                etPinyin?.visibility = View.GONE
                renderSymbolPage()
            }
        }
        tvStatus?.text = "模式: " + mode
    }


    private fun handleCandidateClick(index: Int) {
        if (associationMode) {
            val absoluteIndex = inputEngine.page * inputEngine.pageSize + index
            if (absoluteIndex in 0 until inputEngine.candidates.size) {
                val word = inputEngine.candidates[absoluteIndex]
                val textToCommit = if (associationPrefix.isNotEmpty() && word.startsWith(associationPrefix)) {
                    word.substring(associationPrefix.length)
                } else {
                    word
                }
                if (textToCommit.isNotEmpty()) {
                    currentInputConnection?.commitText(textToCommit, 1)
                    inputEngine.output += textToCommit
                    learnText(word)
                }
                associationMode = false
                associationPrefix = ""
                inputEngine.resetInputState()
                updateInputDisplay()
                updateCandidates()
                tvAssociations?.visibility = View.GONE
                tvStatus?.text = "已输入词: " + word
            }
            return
        }

        val oldPinyinBeforeSelect = etPinyin?.text?.toString()?.trim()?.lowercase() ?: ""
        val pinyinConsumeBeforeSelect = inputEngine.pinyinConsumeLength.coerceAtLeast(0)
        inputEngine.selectCandidate(index)?.let { char ->
            currentInputConnection?.commitText(char, 1)
            learnText(char)
            if (currentKeyboardMode == "handwriting") {
                handwritingPad?.clearPad()
            }
            updateInputDisplay()
            if (currentKeyboardMode == "pinyin") {
                val remaining = if (pinyinConsumeBeforeSelect in 1..oldPinyinBeforeSelect.length) oldPinyinBeforeSelect.substring(pinyinConsumeBeforeSelect) else ""
                associationMode = false
                associationPrefix = ""
                tvAssociations?.visibility = View.GONE
                if (remaining.isNotEmpty()) {
                    setPinyinText(remaining)
                    inputEngine.inputPinyin(remaining)
                    updateCandidates()
                    tvStatus?.text = "已输入: " + char + "，继续拼音: " + remaining
                } else {
                    setPinyinText("")
                    inputEngine.candidates.clear()
                    updateCandidates()
                    tvStatus?.text = "已输入: " + char
                }
            } else {
                showAssociations(char)
                tvStatus?.text = "已输入: " + char
            }
        }
    }

    private fun updateCandidates() {
        applyUserLearningRank()
        val (pageChars, total) = inputEngine.getPageCandidates()
        for (i in 0 until 7) {
            btnCandidates[i]?.text = if (i < pageChars.size) pageChars[i] else ""
        }
        val hasCandidates = total > 0
        candidateBar?.visibility = if (hasCandidates) View.VISIBLE else View.GONE
        pageBar?.visibility = if (hasCandidates) View.VISIBLE else View.GONE
        val totalPages = if (total > 0) (total + 6) / 7 else 1
        val label = if (associationMode) "联想词" else "候选字"
        tvPage?.text = label + "  第" + (inputEngine.page + 1) + "/" + totalPages + "页，共" + total + "个，可左右滑动"
    }

    private fun updateInputDisplay() {
        tvInput?.text = inputEngine.output
    }

    private fun showAssociations(char: String) {
        val assoc = inputEngine.getAssociations(char).filter { it.startsWith(char) }.take(60)
        if (assoc.isNotEmpty()) {
            associationMode = true
            associationPrefix = char
            inputEngine.candidates = assoc.toMutableList()
            inputEngine.page = 0
            tvAssociations?.text = "联想"
            tvAssociations?.visibility = View.VISIBLE
            updateCandidates()
        } else {
            associationMode = false
            associationPrefix = ""
            tvAssociations?.visibility = View.GONE
            inputEngine.resetInputState()
            updateCandidates()
        }
    }

    private fun commitText(text: String) {
        Log.i("RadicalIME", "commitText: $text")
        currentInputConnection?.commitText(text, 1)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.i("RadicalIME", "onStartInput called, attribute=$attribute")
        val isPassword = attribute?.inputType?.and(android.text.InputType.TYPE_CLASS_TEXT) == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        if (isPassword) switchMode("pinyin")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.i("RadicalIME", "onFinishInput called")
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.i("RadicalIME", "onStartInputView called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("RadicalIME", "onDestroy called")
    }

    private inner class HandwritingPadView(
        context: android.content.Context,
        private val onStrokeRecognized: (String) -> Unit
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = dp(5).toFloat()
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        private val paths = mutableListOf<Path>()
        private var currentPath: Path? = null
        private val points = mutableListOf<Pair<Float, Float>>()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.WHITE)
            canvas.drawLine(width / 2f, 0f, width / 2f, height.toFloat(), gridPaint)
            canvas.drawLine(0f, height / 2f, width.toFloat(), height / 2f, gridPaint)
            for (p in paths) canvas.drawPath(p, paint)
            currentPath?.let { canvas.drawPath(it, paint) }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                    currentPath = Path().apply { moveTo(event.x, event.y) }
                    points.clear()
                    points.add(Pair(event.x, event.y))
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(event.x, event.y)
                    points.add(Pair(event.x, event.y))
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentPath?.lineTo(event.x, event.y)
                    points.add(Pair(event.x, event.y))
                    currentPath?.let { paths.add(it) }
                    currentPath = null
                    val code = recognizeStroke(points)
                    onStrokeRecognized(code)
                    invalidate()
                    return true
                }
            }
            return true
        }

        fun clearPad() {
            paths.clear()
            currentPath = null
            points.clear()
            invalidate()
        }

        fun undoStroke() {
            if (paths.isNotEmpty()) {
                paths.removeAt(paths.size - 1)
                invalidate()
            }
        }

        private fun recognizeStroke(pts: List<Pair<Float, Float>>): String {
            if (pts.size < 2) return "4"
            val first = pts.first()
            val last = pts.last()
            val dx = last.first - first.first
            val dy = last.second - first.second
            val minX = pts.minOf { it.first }
            val maxX = pts.maxOf { it.first }
            val minY = pts.minOf { it.second }
            val maxY = pts.maxOf { it.second }
            val w = maxX - minX
            val h = maxY - minY

            var changes = 0
            var lastDirX = 0
            var lastDirY = 0
            for (i in 1 until pts.size) {
                val sx = pts[i].first - pts[i - 1].first
                val sy = pts[i].second - pts[i - 1].second
                if (abs(sx) + abs(sy) < dp(3)) continue
                val dirX = if (sx > 0) 1 else if (sx < 0) -1 else 0
                val dirY = if (sy > 0) 1 else if (sy < 0) -1 else 0
                if (lastDirX != 0 || lastDirY != 0) {
                    if ((dirX != 0 && lastDirX != 0 && dirX != lastDirX) ||
                        (dirY != 0 && lastDirY != 0 && dirY != lastDirY)) {
                        changes++
                    }
                }
                if (dirX != 0) lastDirX = dirX
                if (dirY != 0) lastDirY = dirY
            }

            // 有明显转折的笔迹，归为折
            if (changes >= 1 && w > dp(18) && h > dp(18)) return "5"

            // 横、竖
            if (w > h * 1.8f) return "1"
            if (h > w * 1.8f) return "2"

            // 左下为撇，右下为点/捺；其他不确定优先归折
            if (dx < 0 && dy > 0) return "3"
            if (dy > 0) return "4"
            return "5"
        }
    }

}
