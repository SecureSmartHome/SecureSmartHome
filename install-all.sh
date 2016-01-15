#!/bin/bash
./gradlew master:installDebug app:installDebug slave:installDebug
adb shell "am start  -n 'de.unipassau.isl.evs.ssh.master/de.unipassau.isl.evs.ssh.master.activity.MasterMainActivity' -a android.intent.action.MAIN -c android.intent.category.LAUNCHER"