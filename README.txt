JM Scout  v1.3.2  (禁漫查号)
=========================

完整原生安卓工程，装上就能用。查号、封面、收藏都在手机里，不填网址。

Windows 必读
------------
工程路径不能有中文。请解压到：

  C:\Users\Jiahao\jm-scout

然后用 Android Studio 打开：

  C:\Users\Jiahao\jm-scout\android

不要放在「下载\禁漫查号」这类带中文的文件夹。
若已经打开旧工程：File → Close Project，再 Open 上面这个新路径。

打开后
------
1. Gradle JVM 选 21（弹出不兼容时点 Use JVM 21，不要选 25）。
2. 不要点 AGP Upgrade Assistant。
3. 等同步结束，菜单 Build → Build Bundle(s) / APK(s) → Build APK(s)。
4. 编好的包会复制到：

     C:\Users\Jiahao\jm-scout\apk\jm-scout-v1.3.2.apk

   把这个文件拷到小米覆盖安装即可。不要先卸载，卸载会清掉收藏。
   包名一直是 com.jinman.chahao。

   想改名字或保存位置：打开 android/app/build.gradle.kts
   - outputFileName 改文件名
   - resolve("../apk") 改保存目录

本版已包含
----------
- 暗号：同时有阿拉伯数字和中文数字时，只用阿拉伯数字
  例：14次…一腔…63次…52次…1张  →  1463521
- 纯中文暗号仍可用：十四小时…六万七千九百四十六条  →  1467946
- 查询前匿名访问 /setting（不是登录，没有账号）
- 封面/内页解乱算法与 JMComic-Crawler-Python 一致
- 收藏、导出、搬家导入
