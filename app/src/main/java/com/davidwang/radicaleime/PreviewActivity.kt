package com.davidwang.radicaleime

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.davidwang.radicaleime.databinding.ActivityPreviewBinding

class PreviewActivity : AppCompatActivity() {

    private lateinit var dataStore: DataStore
    private lateinit var inputEngine: InputEngine
    private lateinit var binding: ActivityPreviewBinding

    companion object {
        private const val TAG = "PreviewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataStore = DataStore(applicationContext)
        inputEngine = InputEngine(dataStore)

        setupListeners()
        switchModeUI("radical")
    }

    private fun setupListeners() {
        binding.rbRadical.setOnClickListener { switchModeUI("radical") }
        binding.rbPinyin.setOnClickListener { switchModeUI("pinyin") }

        // 拼音输入
        binding.etPinyin.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_UP) {
                val pinyin = binding.etPinyin.text.toString().trim().lowercase()
                if (pinyin.isNotEmpty()) {
                    inputEngine.inputPinyin(pinyin)
                    updateCandidates()
                }
            }
            false
        }

        // 候选字按钮 0-6
        val candidateBtns = listOf(
            binding.btnCandidate0, binding.btnCandidate1, binding.btnCandidate2,
            binding.btnCandidate3, binding.btnCandidate4, binding.btnCandidate5,
            binding.btnCandidate6
        )
        for (i in candidateBtns.indices) {
            candidateBtns[i].setOnClickListener {
                val char = inputEngine.selectCandidate(i)
                if (char != null) {
                    commitText(char)
                    updateCandidates()
                    showAssociations(char)
                }
            }
        }

        // 翻页
        binding.btnPrev.setOnClickListener {
            inputEngine.prevPage()
            updateCandidates()
        }
        binding.btnNext.setOnClickListener {
            inputEngine.nextPage()
            updateCandidates()
        }

        // 笔画筛选（按笔画数过滤偏旁候选字）
        binding.btnStrokeAll.setOnClickListener {
            inputEngine.strokeFilter = null
            inputEngine.candidates = dataStore.getRadicalCandidates(inputEngine.selectedRadical ?: "").toMutableList()
            inputEngine.page = 0
            updateCandidates()
            binding.tvStatus.text = "偏旁: " + (inputEngine.selectedRadical ?: "")
        }

        for (stroke in 1..20) {
            val btnId = resources.getIdentifier("btn_stroke_$stroke", "id", packageName)
            if (btnId != 0) {
                findViewById<Button>(btnId).setOnClickListener { filterByStroke(stroke) }
            }
        }

        // 60个偏旁按钮
        val radicals = listOf(
            "口", "氵", "亻", "木", "扌", "日", "艹", "讠", "纟", "火", "月", "目", "足", "辶", "犭",
            "宀", "山", "王", "石", "田", "女", "贝", "刂", "车", "禾", "米", "饣", "囗", "土", "钅", "忄",
            "阝", "心", "竹", "彳", "页", "广", "攵", "冫", "巾", "虫", "力", "疒", "礻", "衤", "雨", "酉",
            "皿", "骨", "黑", "鬼", "尸", "门", "穴", "大", "弓", "马", "耳", "走", "欠"
        )

        for (i in radicals.indices) {
            val btnId = resources.getIdentifier("btn_radical_$i", "id", packageName)
            if (btnId != 0) {
                findViewById<Button>(btnId).setOnClickListener {
                    inputRadical(radicals[i])
                }
            }
        }

        // 底部控制
        binding.btnBackspace.setOnClickListener {
            val text = binding.etInput.text.toString()
            if (text.isNotEmpty()) {
                binding.etInput.setText(text.substring(0, text.length - 1))
                binding.etInput.setSelection(binding.etInput.text.length)
            }
            inputEngine.backspace()
        }
        binding.btnClear.setOnClickListener {
            binding.etInput.setText("")
            inputEngine.clear()
            binding.tvStatus.text = "已清空"
            binding.tvAssociations.visibility = View.GONE
        }
        binding.btnSpace.setOnClickListener { commitText(" ") }
        binding.btnEnter.setOnClickListener { commitText("\n") }
    }

    /** 按笔画数过滤候选字 */
    private fun filterByStroke(stroke: Int) {
        inputEngine.filterByStroke(stroke)
        updateCandidates()
        binding.tvStatus.text = "偏旁: " + (inputEngine.selectedRadical ?: "") + " | 笔画: " + stroke
    }

    private fun inputRadical(radical: String) {
        inputEngine.inputRadical(radical)
        switchModeUI("radical")

        val count = inputEngine.candidates.size
        Log.d(TAG, "Clicked " + radical + ", candidates=" + count)
        if (count > 0) {
            Log.d(TAG, "First few: " + inputEngine.candidates.take(5))
        }
        binding.tvStatus.text = "已输入偏旁: " + radical + " (" + count + " 个字)"

        updateCandidates()
    }

    private fun switchModeUI(mode: String) {
        when (mode) {
            "radical" -> {
                binding.rbRadical.isChecked = true
                binding.etPinyin.visibility = View.GONE
            }
            "pinyin" -> {
                binding.rbPinyin.isChecked = true
                binding.etPinyin.visibility = View.VISIBLE
                binding.etPinyin.requestFocus()
            }
        }
    }

    private fun updateCandidates() {
        val (pageChars, total) = inputEngine.getPageCandidates()
        Log.d(TAG, "updateCandidates: pageChars=" + pageChars.size + " total=" + total)

        val btnRefs = listOf(
            binding.btnCandidate0, binding.btnCandidate1, binding.btnCandidate2,
            binding.btnCandidate3, binding.btnCandidate4, binding.btnCandidate5,
            binding.btnCandidate6
        )

        for (i in 0 until 7) {
            btnRefs[i].text = if (i < pageChars.size) (i.toString() + ": " + pageChars[i]) else ""
            Log.d(TAG, "  btn" + i + " text='" + btnRefs[i].text + "'")
        }

        val totalPages = if (total > 0) (total + 6) / 7 else 1
        binding.tvPage.text = ((inputEngine.page + 1).toString() + "/" + totalPages)
    }

    private fun showAssociations(char: String) {
        val assoc = inputEngine.getAssociations(char)
        if (assoc.isNotEmpty()) {
            binding.tvAssociations.text = "联想词: " + assoc.joinToString("  ")
            binding.tvAssociations.visibility = View.VISIBLE
        } else {
            binding.tvAssociations.visibility = View.GONE
        }
    }

    /** 提交文字到输入框 */
    private fun commitText(text: String) {
        binding.etInput.append(text)
        Log.d(TAG, "Committed: '" + text + "'")
    }
}
