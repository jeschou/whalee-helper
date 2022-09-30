syntax = "proto3";

package whale.${cleanName}.proto;

option go_package = "${protoPackage};${cleanName}_pb";

service ${CleanName}Service {
  rpc Hello(HelloRequest) returns (HelloResponse) {}
}
message HelloRequest {
  string name = 1;
}

message HelloResponse {
  string message = 1;
}
