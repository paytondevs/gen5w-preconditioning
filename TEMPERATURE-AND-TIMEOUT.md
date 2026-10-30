# Battery temperature, warm advisory, and pending-request timeout

This source contribution follows the exact EV6 US 260507 profile in
[upstream PR #2](https://github.com/paytondevs/ioniq5-preconditioning/pull/2).
It has no new signed release or public version bump. The
[EV6 quick-start guide](EV6-QUICK-START.md) describes the user-facing behavior.

## Scope

| Behavior | EV6 exact profile | Ioniq 5 NEEV US exact profile | Ioniq 5 NEEV23 US profile | Ioniq 5 NEEV23 US 260206 exact profile | Ioniq 5 NEEV23 US 260903 exact profile | Ioniq 6 CEEV US exact profile |
| --- | --- | --- | --- | --- | --- | --- |
| Native minimum-temperature read/display | Enabled after firmware validation | Enabled after firmware validation | Enabled after firmware validation; same contract, confirmed from the hashed framework (not yet field-tested) | Enabled after firmware validation; same contract per the porting kit report | Enabled after firmware validation; same contract per both porting kit reports | Enabled after firmware validation; same contract per the porting kit report |
| Warm advisory and ten-second reset | At 20°C / 68°F | At 21°C / 70°F | At 21°C / 70°F | At 21°C / 70°F | At 21°C / 70°F | At 20°C / 68°F |
| Cancellation of an unconfirmed request | After five minutes | After two minutes (shared Ioniq 5 deadline; not yet exercised on this car) | After two minutes (activation normally follows in about 60–70 seconds) | After two minutes (shared Ioniq 5 deadline) | After two minutes (shared Ioniq 5 deadline) | After two minutes (shared Ioniq 5 deadline) |
| Existing Start/maintain/Stop command arguments and TUNE mapping | Preserved | Preserved | Preserved | Preserved | Preserved | Preserved |

The caller still requires the correct firmware, READY, enabled conditioning,
and an Off indicator. The BMS remains responsible for actual activation. No OBD
adapter, phone bridge, raw CAN write, heater override, or navigation route is
added.

## Native read contract and bounds

Every supported profile reaches `ClusterBatteryTemperatureReader`, but only
after its firmware hashes have been validated; there is no per-car switch.
The reader returns no reading unless the service's descriptor matches. Its
contract is service `com.hkmc.micom.cluster`, descriptor
`com.hkmc.cluster.IClusterService`, registration 37, request 109 without
arguments, and removal 38. The listener descriptor is
`com.hkmc.cluster.INavigationClusterListener`; callback 16 carries an
eight-integer array. Index 1 is interpreted as minimum temperature in whole
degrees Celsius. Other array fields are not exposed as calibrated readings.

The Ioniq 5 NEEV23 US 260507 framework (`automotive.odex` / `automotive-service.odex`
matching the profile hashes, collected read-only from a vehicle over USB) names
these codes `addNavigationClusterListener`, `requestBatteryInfoReq()`,
`removeNavigationClusterListener` and `onHandleBatteryInfoResp(int[])`. Its
`Navigation.BATTERY_INFO_RX` constants index the array as 0 `XEV_SOCDIS`,
1 `XEV_MIN_BATT_TEMP`, 2 `EV_AVLBL_ENRG`, 3 `VCRM_SW_VER`, 4 `DATC_OUT_TEMP_SNSR`,
5 `VCU_BATT_COND_IND_STA`, 6 `VCU_DTE_BASE`, 7 `VCU_DTE_ADD`, and the service
logs index 1 as the minimum battery temperature. No maximum pack temperature is
exposed by either framework. The EV6 contract and decoder are unchanged.

The decoder rejects the wrong length, implausible temperature (-40 to 60°C is
the accepted envelope), or a first field outside 0 to 200. These are plausibility
checks, not a calibration proof. Reply payloads must match the expected shape;
callbacks before the explicit request is armed do not supply a reading.

Reads use one helper at a time, including across service recreation. Overlapping
display and Start callers may share an in-flight request with its original
1.8-second deadline. They never extend the deadline or reuse a completed late
result. Registration cleanup is attempted in `finally`; a permanently hung
Binder call cannot be forcibly cleaned up, but does not create additional
helpers. A missing or failed read returns unknown rather than zero or warm.

## Display behavior

While Launchpad is bound/visible, a separate worker polls about ten seconds
after each response. The temperature worker is separate from the serialized
control worker. Unbinding removes future polls; a generation check discards
completions from an earlier binding. Closing the app does not stop a requested
conditioning session.

Home shows `Battery temp: 61°F`. Advanced shows
`Lowest Battery Pack Temperature: 61°F (16°C)` and response age. Equal values
still refresh the receipt time. A failed refresh marks the old reading stale
immediately, and 30 seconds without a successful response also marks it stale.
Home hides the old number while stale; Advanced retains the number and age.
No sample shows checking/unavailable instead of an invented zero.

Receipt age is the app's response time, not a verified vehicle sensor timestamp.
Whole-degree Celsius readings yield stepped Fahrenheit values (16°C -> 61°F,
17°C -> 63°F). Constant readings do not by themselves establish a polling fault.

## Warm advisory

A normal Start requests temperature and then rechecks READY/Off before sending
control commands. A plausible value at or above the profile's threshold
(`PreconditioningProfile.warmThresholdC`: 20°C on the EV6, 21°C on both Ioniq 5
profiles) shows a ten-second notice
without journaling ownership or sending Start, maintain, or cancel. Start returns
when the notice expires if vehicle conditions still permit it. A duplicate normal
Start cannot accept the warning; the user must choose the separate Start anyway
action while it is offered. An expired override or lost eligibility cannot send.
Unknown/late temperature follows the normal manual-request path.

The 20°C / 68°F working threshold is an advisory assumption informed by
[Kia US bulletin ELE291 Rev 1, page 1](https://static.nhtsa.gov/odi/tsbs/2023/MC-10238481-0001.pdf),
which describes heating below 68°F for its specified 2022-model-year retrofit
scope. It does not establish this 2023 vehicle's exact BMS calibration or which
temperature aggregation the BMS uses. The app does not claim that the BMS
explicitly refused heating because of temperature, that the pack has reached a
charging target, or that a certain charging speed is guaranteed. Start anyway
preserves a manual request path; vehicle constraints still apply.

On the Ioniq 5 NEEV23 US and NEEV US profiles the owner reports that the car
stops heating at 70°F. Readings are whole degrees Celsius, so the threshold is
21°C (69.8°F, shown as 70°F). The EV6 keeps 20°C / 68°F until its cutoff is
confirmed. The Ioniq 6 uses the same 20°C / 68°F. The NEEV23 US 260206 and 260903 profiles run
on the same NEEV23 US head unit and use its 21°C / 70°F.

## Pending-request timeout

At the first status refresh at or after the profile deadline (five minutes on the
EV6, two on the Ioniq 5 and Ioniq 6), a request with no observed
Active indication sends the existing cancel sequence. A fresh Active indication
at the deadline wins. Sessions that have already been observed Active have no
pending cutoff. Unknown/missing feedback cannot indefinitely extend a pending
request. No repeated Start pulses or automatic retry loop are introduced.

Cancellation is not completion: ownership stays recorded until cancellation has
succeeded and a fresh Off indication is received. A failed cancellation retains
Stop for a user retry. An uncertain Off leaves the request unresolved. Only
confirmed completion restores Start and displays No activation. Recovery still
cancels uncertain ownership and never automatically restarts conditioning.

## Evidence and release checks

Field observations are from a US 2023 EV6 Wind RWD on the exact 260507 profile,
using the local development build:

- App Start/Active/Stop/Off and Ready after restart following Stop were confirmed
  previously. TUNE opened Launchpad after reboot.
- The native minimum response matched an owner-supplied OBD minimum-temperature
  reading at one comparison point. OBD is not used by the app.
- On October 25, 2026, the owner confirmed the new labels, successful responses
  about every ten seconds in Advanced, and a temperature increase after a little
  over five minutes of conditioning. Exact before/after numbers were not supplied.
- The owner also confirmed that temperature display refresh resumes after
  leaving and reopening Launchpad. This verifies the observed reopen behavior,
  not independently that native polling ceased while the app was hidden.
- In a later October 25 test, the owner reported a displayed 68°F, tapped Start,
  saw Battery warm, chose Start anyway, and observed conditioning begin. It
  appeared to stop when the displayed temperature reached 70°F, suggesting a
  possible temperature-based stop. This verifies
  the observed notice and explicit manual-request path. No logs or exact Off
  transition were supplied for the apparent stop. It does not establish 70°F
  as a fixed BMS cutoff; it also shows that the 68°F advisory is not a hard
  activation boundary on this vehicle. In follow-up, the owner also confirmed
  that the warm notice reset.
- During that session the owner left Launchpad and returned before conditioning
  stopped. The temperature reading was then stale for about 350 seconds and
  recovered after closing and reopening Launchpad. This is a known display
  refresh issue; its cause is described below. Reopening Launchpad is the
  observed workaround.
  The owner considers this a low-priority issue for the intended pre-charger
  use. A stale reading is not evidence of the battery's current temperature or
  whether conditioning is still active.
- On 2026-10-28, on a US 2022 Ioniq 5 SE AWD (NEEV US profile) using a local
  build, battery conditioning ran for about 15 minutes while driving, then ended
  on its own, and Launchpad detected the end of the session. The temperature
  display went stale during the session and refreshed correctly after reopening
  Launchpad. This records conditioning running while driving on that car; it
  does not establish when the car ends conditioning or verify Start while
  driving.
- Cause of the stale display: `MainActivity.onStop` unbinds, and `onUnbind`
  stops the poll. It returned false, so when Launchpad returned to a service kept
  alive by an active session, Android reused the cached binder without calling
  `onBind` or `onRebind`, and nothing restarted polling until the session ended
  and a later reopen created a new service. The service now returns true from
  `onUnbind` and restarts polling from both `onBind` and `onRebind`.
  `tools/test_service_rebind.py` covers unbind and return during a session with
  host fixtures. On 2026-10-28 the owner confirmed the fix in the Ioniq 5 using
  a local 1.3.1 build: after starting conditioning, leaving Launchpad for more
  than 30 seconds and returning, the reading resumed updating without reopening
  the app. The fix is in shared service code but has not been rechecked on the
  EV6.
- Also on 2026-10-28, on the Ioniq 5 with the battery already warm, tapping Start
  showed the Battery warm notice. Start anyway and the notice reset were not
  separately reported on that car.

Host tests cover the warm notice, override, unknown-temperature fallback,
eligibility loss, timeout boundaries, cancellation failure, retained ownership,
stale/equal-value display samples, and native registration/request/removal. Host
Binder fixtures also cover a hung call and overlapping readers. They do not
execute the Android service/activity lifecycle on a vehicle.

The five-minute pending timeout has host-test coverage but has not been verified
in the car. The owner clarified that the observed stop followed Active, so it
does not test this timeout. Release preparation is proceeding with this limit
disclosed; no artificial vehicle failure is required to test it. Failed-read
recovery also remains unverified. The display-resume issue above is fixed and
confirmed on the Ioniq 5; reopening Launchpad remains a harmless workaround on
older builds.

Before publishing binaries, verify the public install/update USB bundle,
including an update over a build signed with a different key, and removal from
NEMESIS Manager. Active-session process/power-loss
behavior, native-navigation coexistence, and driving activation remain unverified.
These limits do not negate the owner's successful manual conditioning/display
tests; they keep separate behaviors from being marked tested by inference.

On the Ioniq 5 NEEV US 260507 profile, `automotive.odex` (hash `dd72e04b…3661338`)
declares the same transaction numbers and signatures: `addNavigationClusterListener`
37, `requestBatteryInfoReq()` 109, `removeNavigationClusterListener` 38, and
listener `onHandleBatteryInfoResp(int[])` 16, with the same service and listener
descriptors. The array layout is not part of that interface, so it was checked
in the car: on 2026-10-28, on a US 2022 Ioniq 5 SE AWD using a local build, the
owner confirmed the reading appeared on Launchpad and agreed with an OBD reader's
temperature. Exact values were not recorded, and the comparison was not repeated
across a wide temperature range. During a longer conditioning session the
displayed value no longer matched the OBD reading; after relaunching Launchpad
it updated and agreed again once the pack had cooled. Whether the display showed
the stale label was not recorded; it was consistent with the refresh issue above,
which is now fixed. The Battery warm notice was later shown on that car (see the
evidence list); the pending timeout has not been exercised there.

Native reads remain unverified on other firmware, trims, model years or markets.
The deadline is per profile (`PreconditioningProfile.activationTimeoutMs`): five minutes on the
EV6 and two minutes on the Ioniq 5 profiles and the Ioniq 6, where the NEEV23 US owner reports activation in
about 60–70 seconds.
No proprietary firmware/disassembly, raw vehicle logs, APKs, private updater,
signing key, or password is included in this contribution.

EV6 development, documentation, and vehicle testing: **Abro86**. Development,
documentation, and host-test assistance: **OpenAI Codex**. Original application:
**paytondevs** and contributors.
