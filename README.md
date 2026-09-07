# JMComic-Assistant（禁漫助手）

原生 Android 助手：从抖音评论识别禁漫车号，查询封面与作品信息，本地收藏、阅读、导出。

当前版本 **v1.4.2**。

> 内容面向成人，请自行判断是否使用。

## 编译

1. 克隆后用 Android Studio 打开**仓库根目录**。
2. Gradle JVM 选 **21**。不要升级 AGP。
3. Build → Build APK(s)。编好的包会复制到仓库上一级 `apk/jm-assistant-v版本号.apk`。

工程路径不要有中文，例如：

```
git clone https://github.com/wjh183322/JMComic-Assistant.git C:\Users\Jiahao\jm-assistant
```

覆盖安装，不要先卸载（卸载会清掉收藏）。包名仍是 `com.jinman.chahao`，所以和旧版「禁漫查号」是同一个应用。

## 版本

最初存档为 **v1.2**。之后每个版本单独一次 commit，见 [docs/开发文档.md](docs/开发文档.md)。
