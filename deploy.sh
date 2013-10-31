#!/bin/bash

sbt one-jar
scp target/scala-2.10/ssas_2.10-0.1-one-jar.jar ssas:/opt/ssas/ssas.jar
scp -r static ssas:/opt/ssas
