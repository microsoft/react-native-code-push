#!/bin/sh

kill -9 `lsof -t -i4TCP:8081`
