package handler

import (
    "context"
    "${protoPackage}"
)

type ${CleanName}Handler struct {
    ${cleanName}_pb.${CleanName}ServiceServer
}

func (h *${CleanName}Handler) Hello(ctx context.Context, req *${cleanName}_pb.HelloRequest) (*${cleanName}_pb.HelloResponse, error) {
    resp := &${cleanName}_pb.HelloResponse{}
    resp.Message = "Hi, " + req.Name
    return resp, nil
}
