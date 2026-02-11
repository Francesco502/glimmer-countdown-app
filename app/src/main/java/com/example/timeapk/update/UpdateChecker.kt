package com.example.timeapk.update

/**
 * 更新检查接口。后续可替换为：
 * - [PlayStoreUpdateChecker]：上架 Google Play 时使用 In-App Update API
 * - [CustomServerUpdateChecker]：自建或第三方（如蒲公英、fir）提供版本号与 APK 链接
 */
interface UpdateChecker {
    suspend fun checkUpdate(): CheckUpdateResult
}
