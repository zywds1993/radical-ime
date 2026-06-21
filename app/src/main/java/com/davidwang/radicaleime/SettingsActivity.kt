package com.davidwang.radicaleime

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

/**
 * 输入法设置页面
 * 可以从这里启动输入法的主界面预览
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 简单的设置界面
        val listView = findViewById<ListView>(R.id.list_settings)
        val items = arrayOf(
            "偏旁部首模式",
            "笔画输入模式",
            "拼音输入模式",
            "语音输入模式（待实现）"
        )
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
    }
}
