package com.example.orderservice.grpc

import com.example.order.v1.GetOrdersRequest
import com.example.order.v1.GetOrdersResponse
import com.example.order.v1.OrderQueryServiceGrpc
import com.example.orderservice.service.OrderService
import io.grpc.stub.StreamObserver
import org.springframework.grpc.server.service.GrpcService

/**
 * order-service의 gRPC 조회 어댑터.
 *
 * 기존 REST 컨트롤러/도메인 로직은 그대로 두고, OrderService.getOrdersByUserId 결과를
 * proto 응답으로 매핑해 노출한다. 타임스탬프는 ISO-8601(ISO_LOCAL_DATE_TIME) 문자열로 인코딩한다.
 */
@GrpcService
class OrderQueryGrpcService(
    private val orderService: OrderService
) : OrderQueryServiceGrpc.OrderQueryServiceImplBase() {

    override fun getOrders(
        request: GetOrdersRequest,
        responseObserver: StreamObserver<GetOrdersResponse>
    ) {
        val orders = orderService.getOrdersByUserId(request.userId)
        val response = GetOrdersResponse.newBuilder()
            .addAllOrders(orders.map { OrderProtoMapper.toProto(it) })
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}
