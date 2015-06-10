#!/bin/sh

npm start &
lsof -t -i4TCP:8081
