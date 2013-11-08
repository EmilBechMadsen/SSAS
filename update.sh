#!/bin/sh

echo "Setting permissions on script"
sudo chmod g+x *.sh
sudo chgrp admin *.sh

echo "Backing up old version of ssas.jar"
rm ssas-old.jar
mv ssas.jar ssas-old.jar

echo "Setting up new version of ssas.jar"
sudo chgrp ssas ssas-new.jar
mv ssas-new.jar ssas.jar
