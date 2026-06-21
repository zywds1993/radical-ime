package com.davidwang.radicaleime

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import androidx.core.content.ContextCompat

class FloatingKeyboardService : AccessibilityService() {

    private lateinit var dataStore: DataStore
    private lateinit var inputEngine: InputEngine
    private lateinit var windowManager: WindowManager
    
    private var floatingView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    
    private var isKeyboardVisible = false
    private var lastX = 0
    private var lastY = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            notificationTimeout = 100
        }
        serviceInfo = info
        
        dataStore = DataStore(applicationContext)
        inputEngine = InputEngine(dataStore)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                val className = it.className?.toString() ?: ""
                if (className.contains("EditText") || className.contains("SearchView") || className.contains("WebView")) {
                    showFloatingKeyboard()
                }
            }
        }
    }

    private fun showFloatingKeyboard() {
        if (isKeyboardVisible) return
        isKeyboardVisible = true

        floatingView = LayoutInflater.from(this).inflate(R.layout.keyboard_overlay, null)
        
        overlayParams = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            gravity = Gravity.BOTTOM
            format = PixelFormat.TRANSLUCENT
            windowAnimations = android.R.style.Animation_Dialog
            
            // 设置透明度（0.85 = 85%不透明）
            alpha = 0.85f
        }

        // 让浮动窗口可以拖拽
        setupDraggable(floatingView!!)

        windowManager.addView(floatingView!!, overlayParams)
        setupKeyboardUI()
    }

    private fun setupDraggable(view: View) {
        val touchHandler = View.OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX.toInt() - lastX
                    val deltaY = event.rawY.toInt() - lastY
                    
                    overlayParams?.let { params ->
                        params.x += deltaX
                        params.y += deltaY
                        windowManager.updateViewLayout(v, params)
                    }
                    
                    lastX = event.rawX.toInt()
                    lastY = event.rawY.toInt()
                    true
                }
                else -> false
            }
        }
        view.setOnTouchListener(touchHandler)
    }

    private fun setupKeyboardUI() {
        val view = floatingView ?: return
        
        view.findViewById<RadioButton>(R.id.rb_overlay_radical)?.setOnClickListener {
            inputEngine.switchMode("radical")
            updateKeyboardUI()
        }
        
        view.findViewById<RadioButton>(R.id.rb_overlay_pinyin)?.setOnClickListener {
            inputEngine.switchMode("pinyin")
            updateKeyboardUI()
        }

        val etPinyin = view.findViewById<EditText>(R.id.et_overlay_pinyin)
        etPinyin.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP) {
                val pinyin = etPinyin.text.toString().trim().lowercase()
                if (pinyin.isNotEmpty()) {
                    inputEngine.inputPinyin(pinyin)
                    updateCandidates()
                }
            }
            false
        }

        for (i in 0 until 7) {
            val btnId = resources.getIdentifier("btn_overlay_candidate_$i", "id", packageName)
            if (btnId != 0) {
                view.findViewById<Button>(btnId).setOnClickListener {
                    val char = inputEngine.selectCandidate(i)
                    if (char != null) {
                        commitText(char)
                        updateCandidates()
                    }
                }
            }
        }

        view.findViewById<Button>(R.id.btn_overlay_prev)?.setOnClickListener {
            inputEngine.prevPage()
            updateCandidates()
        }
        view.findViewById<Button>(R.id.btn_overlay_next)?.setOnClickListener {
            inputEngine.nextPage()
            updateCandidates()
        }

        view.findViewById<Button>(R.id.btn_overlay_stroke_all)?.setOnClickListener {
            inputEngine.strokeFilter = null
            inputEngine.candidates = dataStore.getRadicalCandidates(inputEngine.selectedRadical ?: "").toMutableList()
            inputEngine.page = 0
            updateCandidates()
        }

        for (stroke in 1..20) {
            val btnId = resources.getIdentifier("btn_overlay_stroke_$stroke", "id", packageName)
            if (btnId != 0) {
                view.findViewById<Button>(btnId).setOnClickListener {
                    inputEngine.filterByStroke(stroke)
                    updateCandidates()
                }
            }
        }

        val radicals = listOf(
            "亅", "丿", "丶", "木", "氵", "艹", "竹", "⺮", "⺮", "⺮",
            "⺮", "⺮", "⺮", "⺮", "⺮",
            "⺮", "山", "王", "石", "田", "女", "贝", "刂", "车", "示",
            "米", "食", "囗", "土", "钅",
            "阝", "心", "立", "弓", "页", "广", "攵", "冫", "巛", "虫",
            "力", "疒", "礻", "衤", "雨", "酉",
            "皿", "骨", "黑", "鬼", "尸", "门", "穴", "大", "弓", "马",
            "耳", "走", "欠"
        )

        for (i in radicals.indices) {
            val btnId = resources.getIdentifier("btn_overlay_radical_$i", "id", packageName)
            if (btnId != 0) {
                view.findViewById<Button>(btnId).setOnClickListener {
                    inputEngine.inputRadical(radicals[i])
                    updateCandidates()
                }
            }
        }

        view.findViewById<Button>(R.id.btn_overlay_backspace)?.setOnClickListener {
            inputEngine.backspace()
        }
        view.findViewById<Button>(R.id.btn_overlay_clear)?.setOnClickListener {
            inputEngine.clear()
        }
        view.findViewById<Button>(R.id.btn_overlay_space)?.setOnClickListener {
            commitText(" ")
        }
        view.findViewById<Button>(R.id.btn_overlay_enter)?.setOnClickListener {
            commitText("\n")
        }

        view.findViewById<Button>(R.id.btn_overlay_close)?.setOnClickListener {
            hideFloatingKeyboard()
        }

        updateKeyboardUI()
    }

    private fun updateKeyboardUI() {
        val view = floatingView ?: return
        updateCandidates()
        val tvStatus = view.findViewById<TextView>(R.id.tv_overlay_status)
        tvStatus?.text = "模式: " + inputEngine.currentMode
    }

    private fun updateCandidates() {
        val view = floatingView ?: return
        val result = inputEngine.getPageCandidates()
        val pageChars = result.first
        val total = result.second
        
        for (i in 0 until 7) {
            val btnId = resources.getIdentifier("btn_overlay_candidate_$i", "id", packageName)
            if (btnId != 0) {
                view.findViewById<Button>(btnId)?.text = 
                    if (i < pageChars.size) "$i: ${pageChars[i]}" else ""
            }
        }
        
        val totalPages = if (total > 0) (total + 6) / 7 else 1
        val tvPage = view.findViewById<TextView>(R.id.tv_overlay_page)
        tvPage?.text = "(" + (inputEngine.page + 1) + "/" + totalPages + ") 共" + total + "个"
    }

    private fun commitText(text: String) {
        // 通过剪贴板提交文字
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("radical_input", text)
            clipboard.setPrimaryClip(clip)
            
            // 发送 Ctrl+V 模拟粘贴
            // 注意：这里需要实际的 InputConnection，暂时用剪贴板替代
        } catch (e: Exception) {
            // 降级处理
        }
    }

    private fun hideFloatingKeyboard() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
        overlayParams = null
        isKeyboardVisible = false
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        hideFloatingKeyboard()
        return super.onUnbind(intent)
    }
}