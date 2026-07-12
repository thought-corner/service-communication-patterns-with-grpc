package com.example.userservice.config

import com.example.order.v1.OrderQueryServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * order-service를 향한 gRPC 채널/블로킹 스텁 배선.
 *
 * 채널은 lazy다: ManagedChannelBuilder.build()는 즉시 연결하지 않고 첫 RPC에서 연결하므로,
 * order-service가 떠 있지 않아도 컨텍스트 로딩(contextLoads)은 통과한다.
 */
@Configuration
class GrpcClientConfig {

    @Bean
    fun orderServiceChannel(
        @Value("\${order-service.grpc.host}") host: String,
        @Value("\${order-service.grpc.port}") port: Int
    ): ManagedChannel =
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build()

    @Bean
    fun orderQueryBlockingStub(
        channel: ManagedChannel
    ): OrderQueryServiceGrpc.OrderQueryServiceBlockingStub =
        OrderQueryServiceGrpc.newBlockingStub(channel)
}
