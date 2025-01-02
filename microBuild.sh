#!/bin/bash
if [ "$1" == "d" ] || [ "$1" == "df" ] || [ "$1" == "dff" ] || [ "$1" == "rb" ] || [ "$1" == "" ]; then
  echo "run push and ./MicroBuild $1"
  cd ../Android
  ./MicroBuild push
  cd ../module_playback_audio
  ./MicroBuild push
  cd ../QPlayerSDKDemo
if [ "$1" == "" ]; then
  ./MicroBuild d
else
  ./MicroBuild $1
fi
elif [ "$2" != "" ]; then
  echo "run ./MicroBuild $1 \"$2\""
  ./MicroBuild $1 "$2"
else
  echo "run ./MicroBuild $1"
  ./MicroBuild $1
fi