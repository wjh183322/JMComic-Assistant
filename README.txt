JM Assistant  v1.4.3  (禁漫助手)
=============================

完整原生安卓工程，装上就能用。查号、封面、收藏、阅读都在手机里，不填网址。

Windows 必读
------------
工程路径不能有中文。请解压到：

  C:\Users\Jiahao\jm-assistant

然后用 Android Studio 打开：

  C:\Users\Jiahao\jm-assistant\android

不要放在「下载\禁漫助手」这类带中文的文件夹。
若已经打开旧工程：File → Close Project，再 Open 上面这个新路径。

打开后
------
1. Gradle JVM 选 21（弹出不兼容时点 Use JVM 21，不要选 25）。
2. 不要点 AGP Upgrade Assistant。
3. 等同步结束，菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)。
4. 编好的包会复制到：

     C:\Users\Jiahao\jm-assistant\apk\jm-assistant-v1.4.3.apk

   本版是新包名 com.jinman.assistant，和旧版「禁漫查号」不是同一个应用。
   请先在旧版里点搬家导出，装上本版后再导入。不要指望覆盖安装保留收藏。

   想改名字或保存位置：打开 android/app/build.gradle.kts
   - outputFileName 改文件名
   - resolve("../apk") 改保存目录

本版已包含
----------
- 显示名：禁漫助手；APK / zip：jm-assistant
- 暗号：同时有阿拉伯数字和中文数字时，只用阿拉伯数字
- 封面/内页解乱、收藏、阅读、黑名单
