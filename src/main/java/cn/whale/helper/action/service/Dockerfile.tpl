FROM registry.cn-hangzhou.aliyuncs.com/meetwhale/whale_user:whale_tz

COPY ${serviceName} /${serviceName}

EXPOSE 50051
ENTRYPOINT ["/${serviceName}","--server_address=0.0.0.0:50051"]
