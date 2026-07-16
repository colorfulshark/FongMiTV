#!/bin/bash

keytool -genkeypair \
  -v \
  -keystore release.jks \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
