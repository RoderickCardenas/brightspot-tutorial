@echo off
setlocal

echo Removing existing Docker containers...
docker-compose down
docker volume prune -f
echo Creating new Docker containers...
docker-compose up -d