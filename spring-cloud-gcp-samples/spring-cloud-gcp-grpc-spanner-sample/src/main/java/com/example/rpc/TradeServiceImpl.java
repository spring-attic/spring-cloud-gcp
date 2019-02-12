/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.rpc;

import com.example.model.Trade;
import com.example.service.ListTradesRequest;
import com.example.service.ListTradesResponse;
import com.example.service.TradeServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

@GRpcService
public class TradeServiceImpl extends TradeServiceGrpc.TradeServiceImplBase {

	@Override
	public void listTrades(ListTradesRequest request, StreamObserver<ListTradesResponse> responseObserver) {

		ListTradesResponse response = ListTradesResponse.newBuilder()
				.addTrade(Trade.newBuilder().setSymbol("ABC").setShares(10).setPrice(3.14))
				.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
}
