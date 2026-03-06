# 鎷惧厜 (Glimmer)

> 鐧介┕杩囬殭锛屾嬀鍏夌暀鐥曘€? 
> *Time flies, casting shadows.*

**v3.4** 路 涓€娆惧熀浜?**Jetpack Compose** 涓?**Material 3** 鐨?Android 鍊掕鏃?/ 绾康鏃ュ簲鐢紝鏀寔澶氳瑷€锛堜腑/鑻憋級銆佷富棰樺垏鎹€佹闈㈠皬閮ㄤ欢銆佸簲鐢ㄥ唴妫€鏌ユ洿鏂颁笌鎻愰啋閫氱煡锛屾彁渚涘畬鏁寸殑鍐滃巻鐢熸棩 / 绾康鏃ユ敮鎸侊紝骞跺彲灏嗘彁閱掑悓姝ュ埌绯荤粺鏃ョ▼銆?
## 涓昏鐗规€?
- **鍐滃巻浜嬩欢鍏ㄩ摼璺敮鎸?*锛氭敮鎸併€屾寜鍐滃巻姣忓勾閲嶅銆嶇殑鐢熸棩涓庣邯蹇垫棩锛屽熀浜?`cn.6tail:lunar` 璁＄畻涓嬫鍙戠敓鏃ヤ笌宸插巻骞存暟锛屽苟鍦ㄨ鎯呴〉浠ャ€屽瞾娆?鐢茬敵 鑵婃湀 鍒濆叓銆嶇瓑鏍煎紡灞曠ず銆?- **瀹嬩唬缇庡涓婚**锛氶粯璁ら厤鑹蹭笌鎺掔増閬靛惊銆屽畫浠ｅ伐绗旂敾銆嶄笌缁㈡湰璁捐壊椋庢牸锛屾彁渚涚槮閲戜綋绛夊瓧浣撻璁俱€?- **鐏垫椿鐨勯椤佃鍥?*锛氭寜銆屽叏閮?/ 鐢熸棩 / 绾康鏃?/ 鍏朵粬銆嶅垎绫荤瓫閫夛紝鏀寔鎸夊墿浣欏ぉ鏁般€佺洰鏍囨棩鏈熶笌鍒涘缓鏃堕棿鎺掑簭銆?- **妗岄潰灏忕粍浠朵笌鎻愰啋**锛氬€掕鏃跺皬缁勪欢瀹炴椂鍒锋柊锛屾敮鎸佹寜浜嬩欢璁剧疆鎻愰啋锛屽苟鍦ㄦ洿鏂扮増鏈椂閫氳繃 GitHub Release 鑷姩妫€鏌ユ洿鏂般€?
## 鎶€鏈爤

| 绫诲埆     | 鎶€鏈?|
|----------|------|
| 璇█     | Kotlin |
| UI       | Jetpack Compose銆丮aterial 3 |
| 鏋舵瀯     | MVVM |
| 鏈湴瀛樺偍 | Room |
| 瀵艰埅     | Navigation Compose |
| 鍚庡彴浠诲姟 | WorkManager锛堟彁閱掞級 |
| 鍋忓ソ     | DataStore |

## 鐜瑕佹眰

- **Android Studio**  Ladybug (2024.2.1) 鎴栨洿楂橈紙鎺ㄨ崘锛?- **JDK** 17
- **minSdk** 26 路 **targetSdk** 35

## 蹇€熷紑濮?
### 鍏嬮殕涓庢墦寮€

```bash
git clone https://github.com/Francesco502/glimmer-countdown-app.git
cd glimmer-countdown-app
```

鐢?Android Studio 鎵撳紑椤圭洰鏍圭洰褰曞嵆鍙€?
### 杩愯 Debug 鍖?
```bash
# 鐩磋娓犻亾锛堥粯璁わ級
./gradlew installDirectDebug

# 鎴?Play 娓犻亾
./gradlew installPlayDebug
```

鎴栧湪 Android Studio 涓洿鎺?Run銆?
### 鎵撳寘 Release APK锛堥渶鍏堥厤缃鍚嶏級

1. 鍦ㄩ」鐩牴鐩綍鐢熸垚绛惧悕瀵嗛挜搴擄紙浠呭仛涓€娆★級锛?   ```powershell
   .\gen-keystore.ps1
   ```
   鎴栦娇鐢?[release_and_update_guide.md](docs/release_and_update_guide.md) 涓殑 `keytool` 鍛戒护銆?
2. 鍦ㄩ」鐩牴鐩綍鍒涘缓 `keystore.properties`锛堜笉瑕佹彁浜ゅ埌 Git锛夛紝濉啓锛?   ```properties
   storeFile=timeapk-release.keystore
   storePassword=浣犵殑瀵嗛挜搴撳瘑鐮?   keyAlias=timeapk
   keyPassword=浣犵殑瀵嗛挜瀵嗙爜
   ```

3. 鎵ц鏋勫缓锛?   ```bash
   ./gradlew assembleDirectRelease
   ```
   浜у嚭璺緞锛歚app/build/outputs/apk/direct/release/glimmer-countdown-3-4.apk`锛堢増鏈彿闅?gradle.properties 涓?VERSION_NAME 鍙樺寲锛?
鏇村鍙戝竷涓庢洿鏂拌鏄庤 **[docs/release_and_update_guide.md](docs/release_and_update_guide.md)**銆? 
涓婁紶 GitHub 涓庢墦鍖呭彂甯冨畬鏁存楠よ **[docs/GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md)**銆?
## 椤圭洰缁撴瀯姒傝

```
app/src/main/
鈹溾攢鈹€ java/com/example/timeapk/
鈹?  鈹溾攢鈹€ data/           # Room銆丷epository
鈹?  鈹溾攢鈹€ ui/              # Compose 鐣岄潰涓?ViewModel
鈹?  鈹溾攢鈹€ notifications/   # 鎻愰啋璋冨害
鈹?  鈹溾攢鈹€ update/          # 搴旂敤鍐呮鏌ユ洿鏂帮紙GitHub Release锛?鈹?  鈹溾攢鈹€ widget/          # 妗岄潰灏忛儴浠?鈹?  鈹溾攢鈹€ MainActivity.kt
鈹?  鈹溾攢鈹€ TimeApplication.kt
鈹?  鈹斺攢鈹€ TimeApp.kt       # 瀵艰埅鏍?Composable
鈹斺攢鈹€ res/                 # 璧勬簮涓庡璇█
```

## 鏂囨。

| 鏂囨。 | 璇存槑 |
|------|------|
| [CHANGELOG.md](CHANGELOG.md) | 鐗堟湰鏇存柊鏃ュ織锛堝惈 v1.0 鈫?v3.4 绛夛級 |
| [release_and_update_guide.md](docs/release_and_update_guide.md) | 鍙戝竷 APK銆佺鍚嶃€佺増鏈笌鏇存柊娓犻亾璇存槑 |
| [GITHUB_AND_RELEASE.md](docs/GITHUB_AND_RELEASE.md) | 涓婁紶 GitHub 涓庢墦鍖呭彂甯冩搷浣滄楠?|
| [RELEASE_CHECKLIST.md](docs/RELEASE_CHECKLIST.md) | 鍙戝竷鍓嶆鏌ユ竻鍗?|
| [LUNAR_IMPLEMENTATION_PLAN.md](docs/LUNAR_IMPLEMENTATION_PLAN.md) | 鍐滃巻浜嬩欢鍏ㄩ摼璺敮鎸佸疄鏂芥柟妗堜笌瀹炵幇璇存槑锛?.1 宸插叏閮ㄨ惤鍦帮級 |

## 璁稿彲璇?
璇锋牴鎹疄闄呮儏鍐垫坊鍔?License 鏂囦欢鎴栬鏄庛€?



