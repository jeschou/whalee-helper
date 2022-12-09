#!/usr/bin/env bash

# execute this script under service root, INLINE MODE
# chmod +x build.sh && . ./build.sh

if [ -z "$IMAGE_DOMAIN" ]; then
  IMAGE_DOMAIN="whale-registry.meetwhale.com"
fi

POD_NAME="${serviceName}"

IMAGE_NAME="$IMAGE_DOMAIN/meetwhale"

# execute ci.sh INLINE MODE
curl -fsSL http://proto.data.meetwhale.com:30380/proto/infra/ci.sh/ci.sh -o ci.sh && chmod +x ci.sh && . ./ci.sh

# now $REPORT_IMAGE_NAME is available

CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o ${serviceName} *.go

if [ $? -ne 0 ]; then
    echo "go build failed"
    exit 1
else
    echo "go build success"
fi

docker login -u $REGISTRY_USER -p $REGISTRY_PASSWD $IMAGE_DOMAIN
docker build -f Dockerfile -t $REPORT_IMAGE_NAME --no-cache .

if [ $? -ne 0 ]; then
    echo "docker build failed"
    exit 1
else
    echo "docker build success"
fi

docker push $REPORT_IMAGE_NAME
