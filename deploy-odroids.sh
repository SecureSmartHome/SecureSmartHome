#!/bin/bash

MASTER='192.168.0.100:5555'
SLAVE='192.168.0.101:5555'
APP=$(adb devices -l | grep -m1 usb | cut -f1 -d ' ')

adb connect $MASTER
adb connect $SLAVE

./gradlew master:assembleDebug slave:assembleDebug app:assembleDebug

echo
echo "deploying master on: $MASTER"
adb -s $MASTER install -r ./master/build/outputs/apk/master-debug.apk
adb -s $MASTER shell 'am start -n "de.unipassau.isl.evs.ssh.master/de.unipassau.isl.evs.ssh.master.activity.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'

echo
echo "deploying app on: $APP"
adb -s $APP install -r ./app/build/outputs/apk/app-debug.apk
adb -s $APP shell 'am start -n "de.unipassau.isl.evs.ssh.app/de.unipassau.isl.evs.ssh.app.activity.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'

echo
echo "deploying slave on: $SLAVE"
adb -s $SLAVE install -r ./slave/build/outputs/apk/slave-debug.apk
adb -s $SLAVE shell 'am start -n "de.unipassau.isl.evs.ssh.slave/de.unipassau.isl.evs.ssh.slave.activity.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER'
