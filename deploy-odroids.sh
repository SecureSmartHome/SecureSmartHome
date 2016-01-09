#!/bin/bash

MASTER='192.168.0.100:5555'
SLAVE='192.168.0.101:5555'

./gradlew master:assembleDebug slave:assembleDebug

adb connect $MASTER
adb -s $MASTER install -r ./master/build/outputs/apk/master-debug.apk
adb -s $MASTER shell 'am start -n "de.unipassau.isl.evs.ssh.master/de.unipassau.isl.evs.ssh.master.activity.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'

adb connect $SLAVE
adb -s $SLAVE install -r ./slave/build/outputs/apk/slave-debug.apk
adb -s $SLAVE shell 'am start -n "de.unipassau.isl.evs.ssh.slave/de.unipassau.isl.evs.ssh.slave.activity.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'
