package com.example.timeapk.update

/**
 * 占位实现：始终返回无更新。设置页「检查更新」当前使用此实现；
 * 接入真实更新渠道时替换为对应 [UpdateChecker] 实现即可。
 */
class StubUpdateChecker : UpdateChecker {
    override suspend fun checkUpdate(): CheckUpdateResult =
        CheckUpdateResult(hasUpdate = false)
}
