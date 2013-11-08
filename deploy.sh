#!/bin/bash

echo "Packaging project"
echo
sbt one-jar

echo
echo "Transferring ssas.jar"
echo
scp target/scala-2.10/ssas_2.10-0.1-one-jar.jar ssas:/opt/ssas/ssas-new.jar

echo
echo "Transferring static files"
echo
scp -r static ssas:/opt/ssas

echo
echo "Transferring scripts"
echo
scp run.sh ssas:/opt/ssas
scp update.sh ssas:/opt/ssas
scp restart.sh ssas:/opt/ssas

echo
echo "To run the newest version on the server, SSH into the server, go to /opt/ssas and do the following:"
echo "Kill the current process, ex: 'sudo killall java'."
echo "Run 'restart.sh'. It will update to the new version, and start it. It will ask for your password."
