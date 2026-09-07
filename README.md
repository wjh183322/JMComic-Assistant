# JMComic-Assistant（禁漫助手）

原生 Android 助手：从抖音评论识别禁漫车号，查询封面与作品信息，本地收藏、阅读、导出。

当前版本 **v1.4.8**。

> 内容面向成人，请自行判断是否使用。

## 编译

1. 克隆后用 Android Studio 打开**仓库根目录**。
2. Gradle JVM 选 **21**。不要升级 AGP，不要点 Upgrade Assistant。
3. 若 Sync 报 SSL / Could not resolve：本仓库已改国内镜像，点 File → Sync Project with Gradle Files。仍失败就关掉 Studio 再开一次。
3. Build → Build APK(s)。编好的包会复制到仓库上一级 `apk/jm-assistant-v版本号.apk`。

工程路径不要有中文，例如：

```
git clone https://github.com/wjh183322/JMComic-Assistant.git C:\Users\Jiahao\jm-assistant
```

覆盖安装旧版「禁漫查号」**不会**带上收藏（包名已改为 `com.jinman.assistant`）。请先在旧版里搬家导出，装上本版后再导入。

## 版本

最初存档为 **v1.2**。之后每个版本单独一次 commit，见 [docs/开发文档.md](docs/开发文档.md)。
