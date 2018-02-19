#!/bin/sh

ps -a | grep -v isrunning.sh | grep -v grep | grep $1 > /dev/null
