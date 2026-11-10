# gen5w-preconditioning

[English](README.md) | 한국어

아이오닉 5 또는 EV6에서 배터리 프리컨디셔닝을 수동으로 요청하는 버튼을 제공하는 앱입니다.

TUNE 노브를 길게 누르고 **Start**를 누르면 Launchpad가 차량에 배터리 컨디셔닝 시작을 요청합니다. 내비게이션 목적지를 설정할 필요는 없습니다.

이 앱은 원래 순정 헤드유닛 런처를 대체하도록 설계됐지만, 프리컨디셔닝과 Wi-Fi 설정 외에는 활용도가 낮았습니다. 원하시면 기존 런처 기능도 그대로 사용할 수 있습니다.

## 내 차에서 사용할 수 있나요?

**먼저 [호환성 표](COMPATIBILITY.md#compatibility-chart)를 확인하세요.** 현재 Launchpad가 지원하는 차량과 헤드유닛 소프트웨어 버전이 나열되어 있습니다. 연식보다 헤드유닛 소프트웨어 버전이 중요합니다.

차량이 목록에 없나요? 호환성 표에서 **차량 포팅 키트** 사용법도 확인할 수 있습니다. 이 USB 도구는 지원 추가에 필요한 차량 정보를 안전하게 읽습니다.

**EV6 사용자:** [EV6 설치 및 일상 사용 안내](EV6-QUICK-START.md)부터 확인하세요.

지원 대상이 제한된 순정 ★ 바로가기의 조건은 [HMG-API 연동 안내](HMG-STAR.ko.md)에서 확인하세요.

## 시작하기 전에

> [!WARNING]
> 이 과정은 **차량 인포테인먼트 시스템을 무단으로 수정하는 작업**입니다. 잘못 조작하면 시스템을 **전혀 사용할 수 없게 되어 수리 비용이 매우 커질 수 있습니다.** 차량의 파일을 **임의로 삭제하거나 수정하지 않도록** 주의하세요. **이 문서에서 확실하지 않은 내용**이 있다면 추측하지 마세요. [Twitter](https://x.com/paytondev) 또는 [Reddit](https://reddit.com/u/paytondev)으로 메시지를 보내 주시면 도움을 드리겠습니다.

차량 인포테인먼트 시스템을 수정하기 부담스럽다면, 이 기능을 더 쉽게 되돌릴 수 있고 더 안전한 방식으로 제공하는 [하드웨어 개조 제품](https://electroniqbuttons.com/products/ioniq-5-6-ev6-gv60-manual-preconditioning-button-beta-pre-order)도 있습니다.

**준비물:**
- 컴퓨터(Mac, Windows 또는 Linux)
- **FAT32 또는 exFAT**로 포맷된 USB 드라이브. 용량이 작고 비어 있는 드라이브가 가장 간편합니다.
- [헤드유닛을 재부팅하는 방법](https://www.facebook.com/groups/theioniqguy/posts/1355171539173388/)(공조 조작부의 핀홀 버튼)
- 주차 상태에서 약 15분

ADB, Wi-Fi, 터미널은 **필요하지 않습니다.**

## 설치

### 1. NEMESIS 설치

[NEMESIS](https://github.com/xwtk/hmg_nemesis)는 헤드유닛에 프로그램을 설치할 수 있게 해 줍니다. 한 번만 설치하면 됩니다. [README](https://github.com/xwtk/hmg_nemesis#quick-installation-guidance)의 **Quick Installation Guidance**를 따르세요. 간단히 정리하면 다음과 같습니다.

1. [`ota_package.tar`](https://github.com/xwtk/hmg_nemesis/blob/main/ota_package.tar)를 내려받아 USB 드라이브 최상위에 복사합니다. 직접 빌드하거나 압축을 풀 필요는 없습니다.
2. USB를 차량에 연결합니다. “No Supported Files” 메시지가 표시되는 것은 정상입니다.
3. Engineering Mode → **USB Copy** → **Update DSP** → **Update OTA**를 엽니다.
4. 설치가 끝날 때까지 기다린 뒤 헤드유닛을 재부팅합니다. 이후 **Error 684**가 표시되는 것은 정상입니다. **651**은 파일 압축이 풀리지 않았다는 뜻이므로 드라이브를 확인하고 다시 시도하세요.

설치 확인: **NAV** 버튼을 빠르게 세 번 누르면 **NEMESIS Manager**가 열려야 합니다.

완료한 뒤 USB에서 `ota_package.tar`를 삭제하거나 다른 USB를 사용하세요.

### 2. Launchpad 설치 파일을 USB에 복사

[릴리스 페이지](../../releases)에서 **`Launchpad-install-usb.zip`**을 내려받으세요. 가장 최신 릴리스는 맨 위에 있습니다. 이 파일은 NEMESIS 1.16 이상이 필요합니다.

이전 NEMESIS를 사용한다면 **`Launchpad-manual-install-usb.zip`**을 내려받으세요. 동작은 같지만 USB에 설치 스크립트가 들어 있는 `launchpad/` 폴더가 추가되고, NEMESIS는 설치 대신 스크립트 *실행*을 묻습니다. 업데이트할 때도 확인 창은 한 번 표시됩니다. 실행 결과는 USB의 `launchpad-install.log`에 기록됩니다.

여기서 `paytondev.launchpad.zip`을 사용하지 마세요. 이 파일은 패키지 자체이므로 USB에 풀면 “Invalid package descriptor”가 표시됩니다.

ZIP의 **내용물**을 드라이브의 **최상위**에 풀고, 이전 NEMESIS 관련 파일은 남기지 마세요. 드라이브 구성은 다음과 정확히 같아야 합니다.

```text
USB drive/
├── README.txt
├── NEMESIS.data
└── packages/
    └── paytondev.launchpad.zip
```

- **Mac:** ZIP을 두 번 클릭하면 `Launchpad-install-usb` 폴더가 만들어집니다(Safari가 이미 풀었을 수도 있습니다). 폴더 자체가 아니라 폴더 **안의 모든 내용**을 USB에 복사하세요.
- **Windows:** ZIP을 마우스 오른쪽 버튼으로 클릭 → **Extract All…**을 선택하고 대상 위치를 드라이브 자체(예: `E:\`)로 변경하세요. Windows가 기본으로 만드는 하위 폴더에 넣으면 작동하지 않습니다.
- `packages/paytondev.launchpad.zip`의 압축은 풀지 마세요. NEMESIS가 ZIP 그대로 사용합니다.
- 예전 `NEMESIS.data` 또는 `NEMESIS.sh`가 USB에 있다면 먼저 다른 곳으로 옮기세요. 차량은 최상위의 `NEMESIS.data`만 확인합니다.

<details>
<summary>도우미 스크립트 사용하기(이 저장소를 클론한 경우)</summary>

같은 작업을 수행하며, USB에 이미 있는 NEMESIS 관련 파일은 덮어쓰지 않고 `NEMESIS-backup-<date>` 폴더로 옮깁니다.

```sh
sh tools/usb/write-usb.sh /Volumes/YOUR_USB ~/Downloads
```

Windows PowerShell:
```powershell
.\tools\usb\write-usb.ps1 E:\ $HOME\Downloads
```

마지막 인자는 ZIP을 내려받은 폴더입니다.
</details>

### 3. 차량에서 설치 실행

1. 주차한 뒤 차량을 켜고 헤드유닛 부팅이 끝날 때까지 기다립니다.
2. USB를 **데이터** 포트에 연결합니다. 음악 또는 CarPlay에 사용하는 포트이며 충전 전용 포트가 아닙니다.
3. NEMESIS가 USB의 패키지를 설치할지 물으면 **Yes**를 누릅니다. Launchpad가 이미 설치되어 있다면 업데이트 여부를 다시 묻습니다. 이때도 **Yes**를 누릅니다. 각 확인 창의 제한 시간은 60초입니다. 놓쳤다면 USB를 뽑았다가 다시 연결하세요.
4. 결과 창이 뜰 때까지 기다립니다. 보통 몇 초가 걸리며 최대 약 20초가 걸릴 수 있습니다.

실패 메시지가 표시되면 NEMESIS Manager → **Logs** → **Copy to USB**를 여세요. USB에 `NEMESIS-logs` 폴더가 생성됩니다. 도움을 요청할 때 이 폴더를 함께 보내 주세요.

### 4. 재부팅하고 앱 열기

핀홀 버튼으로 헤드유닛을 재부팅하고 USB를 제거합니다. 부팅이 끝나면 **TUNE 노브를 약 1초 동안 길게 눌러** Launchpad를 엽니다.

NEMESIS Manager에서도 열 수 있습니다. Manager 홈 화면에서 자체 타일 다음에 **Precondition**으로 표시됩니다.

이후에는 업데이트할 때만 USB가 다시 필요합니다.

## 사용 방법

1. TUNE을 길게 눌러 Launchpad를 열고 프리컨디셔닝으로 이동합니다.
2. **Start**를 누릅니다. 차량의 응답을 기다리는 동안 **Request sent**가 표시됩니다. 배터리가 이미 따뜻하면 먼저 확인을 요청하고 **Start anyway**를 선택할 수 있습니다.
3. 차량이 확인하면 **Active**가 표시됩니다. 이것이 배터리 컨디셔닝이 실제로 작동 중임을 알려 주는 유일한 표시입니다. 배터리가 충분히 따뜻하거나 충전량이 너무 낮은 경우처럼 차량이 요청을 거절할 수도 있습니다.

EV6에서는 5분, 아이오닉 5에서는 2분 동안 차량이 확인하지 않으면 Launchpad가 요청을 취소합니다. 이미 **Active**인 세션은 이 제한으로 중단하지 않습니다.

기다리는 중에도 **Stop**을 눌러 취소할 수 있습니다. Launchpad를 닫아도 이미 실행 중인 요청은 **중지되지 않습니다.**

### 예약

**Start** 옆의 원형 **시계** 버튼으로 시간을 지정할 수 있습니다. 모드는 두 가지입니다.

- **Start by:** 지정한 시각까지 컨디셔닝이 작동 중이어야 합니다. 차량이 반응하는 데 약 1분이 걸리므로 Launchpad는 70초 먼저 요청을 보냅니다. 1:30을 선택하면 1:28:50에 요청합니다.
- **Arrive by:** 예를 들어 급속 충전소에 도착할 때 배터리가 따뜻하도록 합니다. Launchpad가 배터리 온도와 차량의 외기 온도 센서를 이용해 시작 시각을 계산하고 대기 중에도 계획을 조정합니다. 차량이 약 30분 후 세션을 종료하므로 계획도 최대 30분으로 제한됩니다. 사용하는 가열 속도는 계측값이 아니라 사용자 보고를 바탕으로 한 추정치이므로 시작 시각은 예상값으로 보세요.

예약 후 카드에 시작까지 남은 시간이 표시되며, **Arrive by**에서는 계획에 사용하는 배터리·외기 온도도 표시됩니다. **Cancel schedule**을 누르면 예약이 취소되고 시계 버튼으로 시간을 바꿀 수 있습니다. 직접 **Start**를 눌러도 예약이 취소됩니다.

Launchpad를 닫아도 예약은 백그라운드에서 유지되며 헤드유닛 재부팅 후에도 남습니다. 다음 사항을 확인하세요.

- 요청을 보낼 때 차량이 켜져 있고 READY 상태여야 합니다. **Start**를 직접 누를 때와 같습니다. 그렇지 않으면 선택 시각 이후 5분까지 계속 시도한 뒤 포기하고 **missed**로 기록합니다. 그 이후에는 늦게 요청을 보내지 않습니다.
- 해당 시점에 배터리가 이미 따뜻하면 요청을 보내지 않고 그 사실을 기록합니다.
- 결과는 알림으로도 게시되지만 헤드유닛에 표시되지 않을 수 있습니다.

**온도 단위:** **Settings → Temperature units**에서 Auto(자동), °C 또는 °F를 선택하세요. 기본값인 자동은 헤드유닛의 `ro.build.product`에 표시된 차량 시장을 사용합니다(KOR → °C, USA → °F, 그 외 또는 확인 불가는 °F). °C 또는 °F를 직접 선택하면 자동 설정보다 우선하며 재부팅과 앱 업데이트 후에도 유지됩니다. 배터리 온도 표시·게이지·그래프와 현재 예약의 입력·예측 설명에 적용됩니다. 과거 로그는 저장된 원문을 유지하며, 센서 값·예약 계산·차량 명령은 바뀌지 않습니다.

**다크 모드:** **Advanced → Appearance**에서 설정합니다. **Follow car**(기본값)는 차량의 주간·야간 화면에 맞추고, **Light** 또는 **Dark**를 선택하면 하나의 화면 모드를 유지합니다. Follow car는 자동 모드를 포함한 차량의 주간·야간 표시 설정을 따릅니다. 차량에서 전환되지 않는다면 Advanced 하단의 **Appearance signals** 패널에서 차량이 보고하는 값을 확인하세요.

## 업데이트

설치와 동일합니다. 최신 릴리스의 **`Launchpad-install-usb.zip`**(NEMESIS 1.16 미만은 **`Launchpad-manual-install-usb.zip`**)을 USB에 넣고 실행한 뒤 재부팅하세요. Launchpad가 이미 설치되어 있으면 기존 설치를 업데이트합니다.

즐겨찾기와 설정은 유지됩니다. 같은 USB를 두 번 실행해도 괜찮습니다.

### 1.x에서 이전

추가 작업 없이 같은 방법으로 USB를 실행하세요. 차량에 Launchpad 1.x가 있으면 같은 실행에서 즐겨찾기와 설정을 옮기고 이전 버전을 제거합니다. NEMESIS Manager로 이미 1.x를 제거했어도 새 버전을 설치할 수 있으며, 제거 후 헤드유닛을 재부팅하지 않았다면 설정도 이전됩니다. ADB로 1.x를 설치한 경우에도 적용됩니다.

## 제거

1. 프리컨디셔닝이 실행 중이면 **Stop**을 누르고 종료될 때까지 기다립니다.
2. 제거 보호가 나타나지 않더라도 본체와 핸들의 ★ 버튼을 순정 기능으로 바꾸세요. 제거 중에는 버튼 설정을 변경하지 마세요.
3. NEMESIS Manager를 열고(NAV 세 번) → **Packages** → **Precondition** → **Uninstall**을 선택합니다.
4. 헤드유닛을 재부팅합니다.

Launchpad와 TUNE 바로가기가 제거됩니다. HMG-API ★ 모듈은 별도로 관리되며 HMG-API에서 직접 제거할 때까지 남습니다. [★ 바로가기 자세히 보기](HMG-STAR.ko.md). Launchpad 1.x가 아직 남아 있지만 Packages 목록에 없다면(예: ADB로 설치한 경우), 먼저 2.0을 설치해 1.x를 제거한 뒤 삭제하세요. NEMESIS는 유지됩니다. NEMESIS도 제거하려면 NEMESIS Manager의 **Wipe NEMESIS**를 사용하세요. [NEMESIS 문서](https://github.com/xwtk/hmg_nemesis/blob/main/03-Install-and-Uninstall.md#5-uninstall-wipe)를 참고하세요.

## 문제 해결

| 증상 | 해결 방법 |
| --- | --- |
| USB를 연결해도 아무 일도 일어나지 않음 | `NEMESIS.data`가 하위 폴더가 아닌 USB **최상위**에 있는지 확인합니다. FAT32/exFAT 포맷과 데이터 포트 연결 여부도 확인하세요. NEMESIS 설치 여부는 NAV를 세 번 눌러 Manager가 열리는지 확인합니다. |
| “Invalid package descriptor” | USB에 잘못된 파일이 있거나 복사가 불완전한 경우입니다. `paytondev.launchpad.zip`이 아닌 `Launchpad-install-usb.zip`을 풀었는지, 위의 USB 구성과 같은지 확인하고 다시 압축을 푸세요. |
| 결과 창에 실패 표시 | NEMESIS Manager → **Logs** → **Copy to USB**를 열어 USB에 `NEMESIS-logs` 폴더를 만드세요. 수동 설치 USB를 사용했다면 `launchpad-install.log`도 함께 보내 주세요. |
| NEMESIS Manager 홈 화면에 Precondition이 없음 | 해당 화면은 NEMESIS 1.16 이상이 필요합니다. Manager를 닫았다가 다시 여세요. 여전히 없다면 **Packages** 목록에 Precondition이 있는지 확인하고 설치 USB를 다시 실행하세요. |
| TUNE을 길게 눌러도 Launchpad가 열리지 않음 | 한 번 더 재부팅하세요. NEMESIS Manager → **Services**에서 `paytondev.launchpad/knobd`가 실행 중이어야 합니다. 다른 헤드유닛은 다른 TUNE 프로필이 필요할 수 있으므로 문의해 주세요. |
| 프리컨디셔닝에 **Unavailable** 표시 | 차량 또는 소프트웨어 버전이 아직 지원되지 않아 Launchpad가 차량에 요청을 보내지 않습니다. [호환성 표](COMPATIBILITY.md#compatibility-chart)를 확인하고, 차량이 없다면 [차량 포팅 키트](COMPATIBILITY.md#car-not-listed-run-the-vehicle-porting-kit)를 실행하세요. |
| **Request sent** 상태로 유지됨 | 차량이 아직 확인하지 않은 상태입니다. **Stop**을 누르거나 기다리세요. Launchpad가 EV6에서는 5분, 아이오닉 5에서는 2분 후 요청을 취소합니다. 차량이 Off를 보고하면 **Start**가 다시 표시됩니다. |

## 차량에서 변경되는 사항

- Launchpad는 일반 NEMESIS 패키지로 설치되어 NEMESIS Manager → **Packages**에 **Precondition**으로 표시됩니다. 제거 후에도 별도로 관리되는 HMG-API ★ 모듈은 남습니다. [★ 바로가기 자세히 보기](HMG-STAR.ko.md).
- NEMESIS가 일반 Android 앱처럼 설치하므로 Manager 홈 화면에도 표시됩니다. 차량 시스템 영역에는 아무것도 쓰지 않으며 현대·기아 파일을 변경하지 않습니다.
- 업데이트 시 이전 버전을 교체하는 동안 설정을 NEMESIS 백업 폴더에 복사한 뒤 복원합니다.
- 업데이트가 중간에 실패하면 USB를 다시 실행하세요. 설치 과정이 복원할 때까지 설정은 백업 폴더에 남아 있습니다.

<details>
<summary>자세히 보기</summary>

- TUNE 길게 누르기 서비스와 앱 사본은 `/data/nemesis/apps/paytondev.launchpad/`에 있습니다.
- NEMESIS는 그 위치의 앱을 `pm install`로 설치하고 `pm uninstall`로 제거합니다. NEMESIS가 이전 버전을 제거한 뒤 새 버전을 설치하므로 각 릴리스가 서로 다른 키로 서명되어도 작동합니다.
- 업데이트 중 설정 파일은 `/data/nemesis/backup/paytondev.launchpad/`에 복사된 뒤 새 설치에 복원됩니다. 제거 시 이 백업 폴더는 삭제되지만 별도로 관리되는 HMG-API ★ 모듈은 남습니다.
- Launchpad 1.x는 `/system/app/Launchpad.apk`에 자신을 복사했습니다. 2.0은 1.x를 발견했을 때 해당 파일을 한 번 제거하는 것만 `/system`에서 수행합니다. 이를 위해서만 `/system`을 쓰기 가능하게 바꾸고 직후 다시 읽기 전용으로 돌립니다.
- 2.0의 Android 패키지는 `com.paytondev.precondition`이며 1.x는 `com.paytondev.launchpad`였습니다. Android는 1.x를 제거해도 헤드유닛을 재부팅할 때까지 이전 이름을 예약해 두므로, 새 이름을 사용해 2.0을 바로 설치할 수 있습니다.
</details>

이미 차량에서 ADB를 사용 중인가요? [수동 ADB 설치 방법](ADVANCED-ADB-INSTALL.md)도 있지만 필수는 아닙니다.

## 소스에서 빌드

[BUILDING.md](BUILDING.md)를 참고하세요. 릴리스는 `tools/build_apk.sh`와 `tools/build_usb_bundles.py`로 빌드합니다.

문제를 고치거나 차량을 추가하고 싶다면 [CONTRIBUTING.md](CONTRIBUTING.md)를 확인하세요.

## 크레딧

- EV6 개발, 문서화 및 차량 시험: [Abro86](https://github.com/Abro86). 개발·문서·호스트 테스트에 OpenAI Codex의 도움을 받았습니다.
- [https://github.com/xwtk/hmg_nemesis](https://github.com/xwtk/hmg_nemesis) — 모든 작업을 가능하게 한 NEMESIS 익스플로잇
- [https://github.com/tylerharvey/Ioniq5_CAN](https://github.com/tylerharvey/Ioniq5_CAN) — 프리컨디셔닝 시작에 쓰이는 CAN 메시지 역공학
- [https://github.com/Helloyunho/gen5w-utils](https://github.com/Helloyunho/gen5w-utils) — Gen5W 역공학을 위한 다른 스크립트와 도구
