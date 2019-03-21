/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.rpcserver;

import com.example.TradeRepository;
import com.example.model.Trade;
import com.example.service.ListTradesRequest;
import com.example.service.ListTradesResponse;
import com.example.service.TradeServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

import org.springframework.beans.factory.annotation.Autowired;

@GRpcService
public class TradeServiceImpl extends TradeServiceGrpc.TradeServiceImplBase {

	@Autowired
	private TradeRepository tradeRepository;

	@Override
	public void listTrades(ListTradesRequest request, StreamObserver<ListTradesResponse> responseObserver) {

		ListTradesResponse.Builder responseBuilder = ListTradesResponse.newBuilder();
		tradeRepository.findAll().forEach(tradeEntity -> {
			responseBuilder.addTrade(
					Trade.newBuilder()
						.setAction(tradeEntity.getAction())
						.setPrice(tradeEntity.getPrice())
						.setShares(tradeEntity.getShares())
						.setTradeId(tradeEntity.getTradeId())
						.setSymbol(tradeEntity.getSymbol())
			);
		});

		responseObserver.onNext(responseBuilder.build());
		responseObserver.onCompleted();
	}
}
