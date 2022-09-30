package main

import (
    whgrpc "gitlab.meetwhale.com/whale/whale-framework/grpc"
    "google.golang.org/grpc"
    handler "${serviceName}/grpc"
    ${cleanName}_pb "${protoPackage}"
)

func main() {
    server := whgrpc.NewServer("${shortName}")
    server.RegisterService(func(grpcServer *grpc.Server) {
        ${cleanName}_pb.Register${CleanName}ServiceServer(grpcServer, new(handler.${CleanName}Handler))
    })
    server.Start()
}
