创建Release签名文件

```
bash other/tools/keygen.sh
```

创建`local.properties`，需包含以下内容

```
sdk.dir=<SDK路径>
storeFile=<Release签名文件路径>
keyAlias=<Release签名文件Alias>
storePassword=<Release签名文件密码>
```

当前项目由三个维度组合：

- 设备模式：`leanback`（Android TV）、`mobile`（手机/平板）
- CPU 架构：`arm64-v8a`、`armeabi-v7a`
- 构建类型：`debug`、`release`

因此一共支持 8 种 APK。

| 目标 | 构建命令 |
|---|---|
| TV arm64 Debug | `sh ./gradlew :app:assembleLeanbackArm64_v8aDebug` |
| TV arm64 Release | `sh ./gradlew :app:assembleLeanbackArm64_v8aRelease` |
| TV 32位 ARM Debug | `sh ./gradlew :app:assembleLeanbackArmeabi_v7aDebug` |
| TV 32位 ARM Release | `sh ./gradlew :app:assembleLeanbackArmeabi_v7aRelease` |
| 手机 arm64 Debug | `sh ./gradlew :app:assembleMobileArm64_v8aDebug` |
| 手机 arm64 Release | `sh ./gradlew :app:assembleMobileArm64_v8aRelease` |
| 手机 32位 ARM Debug | `sh ./gradlew :app:assembleMobileArmeabi_v7aDebug` |
| 手机 32位 ARM Release | `sh ./gradlew :app:assembleMobileArmeabi_v7aRelease` |

常用批量命令：

```bash
# 构建全部 4 个 Debug APK
sh ./gradlew :app:assembleDebug

# 构建全部 4 个 Release APK
sh ./gradlew :app:assembleRelease

# 构建全部 8 个 APK
sh ./gradlew :app:assembleDebug :app:assembleRelease

# 清理工程
sh ./gradlew clean
```

APK 输出目录：

```text
app/build/outputs/apk/<构建变体>/<debug或release>/
```

例如：

```text
app/build/outputs/apk/leanbackArm64_v8a/debug/app-leanback-arm64_v8a-debug.apk
app/build/outputs/apk/leanbackArm64_v8a/release/leanback-arm64_v8a.apk
```
