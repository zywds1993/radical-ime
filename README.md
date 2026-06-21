# radical-ime

Android 中文输入法实验项目，支持偏旁、拼音、笔画和联想输入。

## 当前版本

`v1.3.10`

## 功能

- 偏旁部首输入
- 拼音输入
- 五笔画输入：横、竖、撇、点/捺、折
- 笔画候选常用字优先、完整匹配优先
- 候选学习排序：本机记录常选字词，常用候选自动靠前
- 选字后联想词组
- 符号输入
- 偏旁词库约 9773 字
- 笔画词库约 9662 字

## 工程结构

```text
app/src/main/java/com/davidwang/radicaleime/
  DataStore.kt
  InputEngine.kt
  RadicalIME.kt
  SettingsActivity.kt

app/src/main/assets/
  radical_index.json
  pinyin_map.json
  pinyin_phrase_map.json
  char_stroke_seq.json
  char_strokes.json
  association_words.json
```

## 构建

使用 Android Studio 打开本目录，等待 Gradle 同步后运行 `assembleDebug`。

```text
gradle assembleDebug
```

## 说明

这是一个个人输入法原型项目，重点探索偏旁、拼音、笔画组合输入体验。
