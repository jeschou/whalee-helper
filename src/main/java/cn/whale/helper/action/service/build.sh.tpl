#!/usr/bin/env bash

# 在服务根目录执行此文件

BUILD_TAG=$BUILD_ID-$(git rev-parse --short HEAD)
if [ "$IMAGE_DOMAIN" == ""]; then
  IMAGE_DOMAIN="whale-registry.meetwhale.com"
fi

IMAGE_NAME = $IMAGE_DOMAIN/meetwhale/${serviceName}:$BUILD_TAG

echo "IMAGE_NAME: $IMAGE_NAME"

CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${serviceName} *.go

if [ $? -ne 0 ]; then
    echo "go build failed"
    exit 1
else
    echo "go build success"
fi

docker login -u $REGISTRY_USER -p $REGISTRY_PASSWD $IMAGE_DOMAIN
docker build --build-arg -f Dockerfile -t $IMAGE_NAME --no-cache .

if [ $? -ne 0 ]; then
    echo "docker build failed"
    exit 1
else
    echo "docker build success"
fi

docker push $IMAGE_NAME
