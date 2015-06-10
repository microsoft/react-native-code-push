#!/bin/sh
result=`lsof -t -i4TCP:8081`

if [ $result ]
then
  kill -9 `lsof -t -i4TCP:8081`
fi

