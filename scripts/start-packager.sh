#!/bin/sh

$ROOT=$1

command=`node -e "console.log(require('./package').scripts.start)"`
$command &
