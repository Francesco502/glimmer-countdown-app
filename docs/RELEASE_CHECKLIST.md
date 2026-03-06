# 鍙戝竷鍓嶆鏌ユ竻鍗?

**检查日期**：2026-03-06  
**版本**：3.4 (versionCode 8)
---

## 1. 鏋勫缓涓庣紪璇?

| 椤圭洰 | 鐘舵€?| 璇存槑 |
|------|------|------|
| Release 鏋勫缓 | 閫氳繃 | `assembleDirectRelease` 鎴愬姛 |
| Lint 妫€鏌?| 閫氳繃 | 鏃犻樆濉炴€ч敊璇?|
| ProGuard/R8 | 宸查厤缃?| `isMinifyEnabled=true`, `proguard-rules.pro` 宸蹭繚鐣?Room/Compose |
| 绛惧悕 | 闇€閰嶇疆 | 闇€鍒涘缓 `keystore.properties` 骞堕厤缃?release 绛惧悕锛涙湭閰嶇疆鏃朵娇鐢?debug 绛惧悕 |

---

## 2. Manifest 涓庨厤缃?

| 椤圭洰 | 鐘舵€?| 璇存槑 |
|------|------|------|
| MainActivity | 宸蹭慨澶?| 娣诲姞 `launchMode="singleTop"` 涓?`onNewIntent`锛屽皬缁勪欢鐐瑰嚮鍦ㄥ簲鐢ㄥ凡鎵撳紑鏃惰兘姝ｇ‘璺宠浆浜嬩欢璇︽儏 |
| 鏉冮檺 | 姝ｅ父 | `POST_NOTIFICATIONS`, `INTERNET`, `REQUEST_INSTALL_PACKAGES` |
| 瀵煎嚭鏍囧織 | 鍚堢悊 | MainActivity銆丆ountdownAppWidgetProvider 涓?exported锛汧ileProvider銆丆ountdownWidgetService 涓?false |
| FileProvider | 姝ｅ父 | `file_paths.xml` 閰嶇疆 `updates/` 鐢ㄤ簬 APK 鏇存柊 |

---

## 3. 鐗堟湰涓庡彂甯?

| 椤圭洰 | 鍊?|
|------|-----|
| versionCode | 8 |
| versionName | 3.4 |
| minSdk | 26 |
| targetSdk | 35 |
| 娓犻亾 | direct / play (`applicationIdSuffix=".play"`) |
| APK 杈撳嚭 | `app/build/outputs/apk/direct/release/glimmer-countdown-3-4.apk` |

---

## 4. 澶氳瑷€涓庤祫婧?

| 椤圭洰 | 鐘舵€?|
|------|------|
| values (榛樿) | 鏈?strings.xml |
| values-zh | 鏈?|
| values-en | 鏈?|
| 澶囦唤瑙勫垯 | `backup_rules.xml`銆乣data_extraction_rules.xml` 宸查厤缃?|

---

## 5. 娼滃湪闂涓庣┖鎸囬拡

| 妫€鏌ラ」 | 缁撹 |
|--------|------|
| UpdateInstaller | 宸叉湁 `response.body == null` 妫€鏌ワ紝`!!` 浣跨敤瀹夊叏 |
| GitHubReleaseUpdateChecker | 鍚屼笂 |
| TimeApplication.repository | 鍗曚緥娉ㄥ叆锛屾甯?|
| Widget runBlocking | 鍦?RemoteViewsService 绾跨▼鎵ц锛岄潪涓荤嚎绋嬶紝鍙帴鍙?|

---

## 6. 缂栬瘧鏈熻鍛婏紙闈為樆濉烇級

浠ヤ笅涓?deprecation/浼樺寲绫昏鍛婏紝涓嶅奖鍝嶅彂甯冿細

- `Theme.kt`: `statusBarColor` 宸插純鐢?
- `CountdownAppWidgetProvider.kt`: `setRemoteAdapter` 宸插純鐢紙鍙悗缁縼绉诲埌 `setRemoteViewsAdapter`锛?
- `WidgetUpdater.kt`: `notifyAppWidgetViewDataChanged` 宸插純鐢紙鏆傛棤鏇夸唬 API锛?
- Gradle properties 涓儴鍒嗛€夐」灏嗗湪 AGP 10.0 绉婚櫎

---

## 7. Lint 鎶ュ憡锛堥儴鍒嗭級

- **OldTargetApi**锛歚tools:targetApi="31"` 鍙€冭檻鏇存柊
- **SmallSp**锛氬皬缁勪欢 tag 浣跨敤 10sp锛岀暐浣庝簬 11sp 寤鸿锛屽彲淇濈暀
- **UnusedResources**锛氬彲鑳芥湁鏈紩鐢ㄨ祫婧?
- 瀹屾暣鎶ュ憡锛歚app/build/reports/lint-results-directRelease.html`

---

## 8. 鏈淇

1. **MainActivity 灏忕粍浠惰烦杞?*锛氬綋搴旂敤宸插湪鍚庡彴鎴栧墠鍙版椂锛岀偣鍑诲皬缁勪欢鍒楄〃椤逛細姝ｇ‘鎵撳紑瀵瑰簲浜嬩欢璇︽儏锛坄launchMode="singleTop"` + `onNewIntent`锛夈€?

---

## 9. 鍙戝竷鍓嶅缓璁楠?

1. [ ] 鎸?**[3.1 鐗堟湰鏈€缁堟祴璇曟竻鍗昡(TEST_CHECKLIST_3.1.md)** 閫愭ā鍧楁墽琛岋細棣栭〉 / 璇︽儏 / 鏂板缓缂栬緫 / 灏忕粍浠?/ 鏇存柊妫€娴?
2. [ ] 閰嶇疆 `keystore.properties` 骞跺畬鎴?Release 绛惧悕
3. [ ] 鍦ㄧ湡鏈轰笂娴嬭瘯 direct 涓?play 娓犻亾瀹夎銆佸崌绾?
4. [ ] 娴嬭瘯灏忕粍浠讹細娣诲姞/缂栬緫/鍒犻櫎浜嬩欢鍚庡嵆鏃跺埛鏂帮紱鐐瑰嚮鍒楄〃椤硅烦杞鎯?
5. [ ] 娴嬭瘯妫€鏌ユ洿鏂般€佷笅杞藉畨瑁呮祦绋?
6. [ ] 娴嬭瘯瀵煎嚭/瀵煎叆 JSON銆丆SV
7. [ ] 楠岃瘉娣辫壊/娴呰壊妯″紡涓庤嚜瀹氫箟涓婚
8. [ ] 纭 `applicationId` 涓庡彂甯冩笭閬撲竴鑷达紙Play 鍟嗗簵浣跨敤 `.play` 鍚庣紑锛?


