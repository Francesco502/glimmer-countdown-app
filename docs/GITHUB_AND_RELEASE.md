# 涓婁紶 GitHub 涓庢墦鍖呭彂甯冩搷浣滄寚鍗?

鏈枃璇存槑濡備綍灏?TimeAPK 涓婁紶鍒?GitHub锛屽苟瀹屾垚绛惧悕鎵撳寘涓庡彂甯冦€?

---

## 涓€銆佷笂浼犲埌 GitHub

### 1. 鍦?GitHub 涓婂垱寤轰粨搴?

1. 鐧诲綍 [GitHub](https://github.com)锛岀偣鍑诲彸涓婅 **+** 鈫?**New repository**銆?
2. 濉啓锛?
   - **Repository name**锛氬 `TimeAPK` 鎴?`ShiGuang`
   - **Description**锛氬彲閫夛紝濡傘€孉ndroid 鍊掕鏃?/ 绾康鏃ュ簲鐢ㄣ€?
   - **Public**锛?*涓嶈**鍕鹃€?鈥淎dd a README鈥濓紙鏈湴宸叉湁椤圭洰锛?
3. 鐐瑰嚮 **Create repository**锛岃涓嬩粨搴撳湴鍧€锛屼緥濡傦細  
   `https://github.com/浣犵殑鐢ㄦ埛鍚?TimeAPK.git`

### 2. 鏈湴鍒濆鍖?Git 骞舵帹閫侊紙鑻ュ皻鏈垵濮嬪寲锛?

鍦ㄩ」鐩牴鐩綍锛坄d:\LLT\Code\TimeAPK`锛夋墦寮€ PowerShell 鎴?CMD锛?

```powershell
# 鑻ヨ繕鏈垵濮嬪寲 Git
git init

# 娣诲姞杩滅▼浠撳簱锛堟妸涓嬮潰鐨?URL 鎹㈡垚浣犵殑浠撳簱鍦板潃锛?
git remote add origin https://github.com/浣犵殑鐢ㄦ埛鍚?TimeAPK.git

# 娣诲姞鎵€鏈夋枃浠讹紙.gitignore 浼氭帓闄?build銆?idea銆乲eystore 绛夛級
git add .

# 棣栨鎻愪氦
git commit -m "Initial commit: 鎷惧厜 TimeAPK 鍊掕鏃跺簲鐢?

# 鎺ㄩ€佸埌 GitHub锛堜富鍒嗘敮鍚嶅彲鑳芥槸 main 鎴?master锛?
git branch -M main
git push -u origin main
```

鑻ユ湰鍦板凡鏈?Git 浣嗘湭娣诲姞杩滅▼锛?

```powershell
git remote add origin https://github.com/浣犵殑鐢ㄦ埛鍚?TimeAPK.git
git push -u origin main
```

### 3. 纭涓嶈鎻愪氦鐨勫唴瀹?

浠ヤ笅鍐呭宸插湪 `.gitignore` 涓紝**涓嶅簲**鍑虹幇鍦ㄤ粨搴撻噷锛?

- `keystore.properties`銆乣*.keystore`銆乣*.jks`锛堢鍚嶅瘑閽ワ級
- `local.properties`
- `build/`銆乣app/build/`銆乣.gradle/`
- `.idea/`銆乣.trae/`

鎺ㄩ€佸墠鍙墽琛?`git status` 纭娌℃湁涓婅堪鏁忔劅鎴栫敓鎴愮洰褰曡鍔犲叆銆?

---

## 浜屻€佹墦鍖呭彂甯冿紙Release APK / AAB锛?

### 1. 閰嶇疆绛惧悕锛堝繀椤伙紝浠呭仛涓€娆★級

Release 鍖呭繀椤荤鍚嶅悗鎵嶈兘瀹夎鎴栦笂鏋躲€?

**鏂瑰紡 A锛氫娇鐢ㄩ」鐩嚜甯﹁剼鏈紙鎺ㄨ崘锛?*

鍦ㄩ」鐩牴鐩綍鎵ц锛?

```powershell
.\gen-keystore.ps1
```

鎸夋彁绀鸿緭鍏ュ瘑閽ュ簱瀵嗙爜銆佸鍚嶃€佺粍缁囩瓑锛屽苟**鐗㈣瀵嗙爜涓?alias锛堥粯璁?`timeapk`锛?*銆?

**鏂瑰紡 B锛氭墜鍔ㄤ娇鐢?keytool**

```powershell
keytool -genkey -v -keystore timeapk-release.keystore -alias timeapk -keyalg RSA -keysize 2048 -validity 10000
```

灏嗙敓鎴愮殑 `timeapk-release.keystore` 鏀惧湪**椤圭洰鏍圭洰褰?*锛堜笌 `build.gradle.kts` 鍚岀骇锛夈€?

**鍒涘缓 keystore.properties**

鍦ㄩ」鐩牴鐩綍鏂板缓 `keystore.properties`锛堜笉瑕佹彁浜ゅ埌 Git锛夛紝鍐呭绀轰緥锛?

```properties
storeFile=timeapk-release.keystore
storePassword=浣犵殑瀵嗛挜搴撳瘑鐮?
keyAlias=timeapk
keyPassword=浣犵殑瀵嗛挜瀵嗙爜
```

鑻?keystore 涓?`keystore.properties` 鍚屽湪鏍圭洰褰曪紝`storeFile` 鍐欐枃浠跺悕鍗冲彲銆?

### 2. 鏋勫缓 Release APK

```powershell
# 鐩磋娓犻亾锛堥粯璁?applicationId锛?
.\gradlew assembleDirectRelease

# 鎴?Play 娓犻亾锛坅pplicationId 甯?.play 鍚庣紑锛?
.\gradlew assemblePlayRelease
```

浜у嚭璺緞锛?

- 鐩磋锛歚app\build\outputs\apk\direct\release\app-direct-release.apk`
- Play锛歚app\build\outputs\apk\play\release\app-play-release.apk`

鍙皢璇?APK 鐩存帴鍙戠粰鐢ㄦ埛瀹夎锛屾垨鐢ㄤ簬搴旂敤鍟嗗簵涓婃灦銆?

### 3. 鏋勫缓 AAB锛圙oogle Play 涓婃灦鐢級

鑻ヤ笂鏋?Google Play锛屽簲浣跨敤 App Bundle锛?

```powershell
.\gradlew bundleRelease
```

浜у嚭锛歚app\build\outputs\bundle\release\app-release.aab`锛屼笂浼犲埌 Play 鍚庡彴鍗冲彲銆?

### 4. 鍙戝竷鍓嶆鏌?

- [ ] 宸查厤缃?`keystore.properties` 涓旇兘鎴愬姛鎵ц `assembleDirectRelease` 鎴?`assemblePlayRelease`
- [ ] 姣忔鍙戝竷鍓嶅湪 `app/build.gradle.kts` 涓€掑 `versionCode` 骞舵洿鏂?`versionName`锛堝 1.0 鈫?1.1锛?
- [ ] 搴旂敤鍚嶇О銆佸浘鏍囥€佹潈闄愪笌闅愮璇存槑涓庝笂鏋舵笭閬撲竴鑷?

---

## 涓夈€佸湪 GitHub 涓婂垱寤?Release锛堝繀鍋氾細鏀寔搴旂敤鍐呮洿鏂帮級

搴旂敤鍐呫€屾鏌ユ洿鏂般€嶄細璇锋眰 GitHub API 鑾峰彇**鏈€鏂?Release** 鍙婂叾涓殑 **.apk 闄勪欢**銆傚洜姝ゆ瘡娆″彂甯冩柊鐗堟湰鏃堕兘瑕佸湪 GitHub 鍒涘缓 Release 骞朵笂浼?APK銆?

### 姝ラ

1. 鎵撳紑浠撳簱锛歚https://github.com/Francesco502/glimmer-countdown-app` 鈫?**Releases** 鈫?**Create a new release**銆?
2. **Choose a tag**锛氭柊寤烘爣绛撅紝**蹇呴』涓庡綋鍓嶇増鏈彿涓€鑷?*锛屼緥濡傚綋鍓?`VERSION_NAME=3.4` 鍒欏～ **`v3.4`**锛堝甫鍓嶇紑 `v`锛夈€傞€?鈥淐reate new tag鈥?鍚庡彂甯冦€?
3. **Release title**锛氬 `v3.4`銆?
4. **Describe**锛氬啓鏇存柊璇存槑锛堜細鏄剧ず鍦ㄥ簲鐢ㄥ唴鏇存柊寮圭獥鐨勩€屾洿鏂拌鏄庛€嶄腑锛夈€傚彲浣跨敤 **[CHANGELOG.md](../CHANGELOG.md)** 涓搴旂増鏈殑鏉＄洰锛涙垨浣跨敤 `.\scripts\publish-release.ps1` 鑷姩鍙戝竷鏃讹紝鑴氭湰浼氫粠 CHANGELOG 璇诲彇銆?
5. **Attach binaries**锛氬皢鏈湴鏋勫缓濂界殑 APK 鎷栨嫿涓婁紶銆? 
   - 璺緞绀轰緥锛歚app\build\outputs\apk\direct\release\glimmer-countdown-3-4.apk`锛堥殢 VERSION_NAME 鍙樺寲锛? 
   - 闄勪欢鍚嶉渶涓?**.apk 缁撳熬**锛屽簲鐢ㄥ彧浼氳瘑鍒甫 `.apk` 鐨勯檮浠朵綔涓轰笅杞介摼鎺ャ€?
6. 鐐瑰嚮 **Publish release**銆?

瀹屾垚鍚庯細

- 鐢ㄦ埛鍙湪 GitHub Releases 椤甸潰鐩存帴涓嬭浇 APK 瀹夎銆?
- 宸插畨瑁呯敤鎴峰彲鍦ㄥ簲鐢ㄥ唴 **璁剧疆 鈫?妫€鏌ユ洿鏂?* 鏀跺埌鏂扮増鏈彁绀猴紝骞堕€夋嫨銆屼笅杞藉苟瀹夎銆嶆垨銆屽湪娴忚鍣ㄤ腑鎵撳紑銆嶅畬鎴愭洿鏂般€?

---

## 鍥涖€佺畝瑕佹祦绋嬪皬缁?

| 姝ラ | 鎿嶄綔 |
|------|------|
| 1 | 鍦?GitHub 鍒涘缓鏂颁粨搴擄紝涓嶅嬀閫?README |
| 2 | 鏈湴 `git init`锛堣嫢鏃狅級鈫?`git add .` 鈫?`git commit` 鈫?`git remote add origin <URL>` 鈫?`git push -u origin main` |
| 3 | 杩愯 `gen-keystore.ps1` 鐢熸垚 keystore锛屽湪鏍圭洰褰曞垱寤?`keystore.properties` |
| 4 | 鎵ц `.\gradlew assembleDirectRelease` 寰楀埌绛惧悕 APK |
| 5 | 锛堝彲閫夛級鍦?GitHub Releases 鏂板缓鐗堟湰骞朵笂浼?APK |

鏇寸粏鐨勭鍚嶃€佹笭閬撱€佺増鏈彿涓庢洿鏂版満鍒惰鏄庤 **[release_and_update_guide.md](release_and_update_guide.md)**銆?

