@echo off
protoc.exe --descriptor_set_out=src/test/resources/protobin/mailbox.protobin src/test/resources/proto/mailbox.proto
protoc.exe --descriptor_set_out=src/test/resources/protobin/player.protobin src/test/resources/proto/player.proto