#!/bin/bash

# Author: Jeremy Case
# Email: JeremyCase@odysseyconsult.com
# Purpose: This script will set up devs with the requisite Helm repos to do local development of Ubiquia.  


echo Running one-time setup for Ubiquia installation...
echo ...adding Helm repos...

helm repo add yugabyte https://charts.yugabyte.com

echo ...done.
