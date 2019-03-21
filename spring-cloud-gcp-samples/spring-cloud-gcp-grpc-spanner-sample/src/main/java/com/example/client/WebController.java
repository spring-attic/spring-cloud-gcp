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

package com.example.client;

import com.example.service.ListTradesRequest;
import com.example.service.ListTradesResponse;
import com.example.service.TradeServiceGrpc;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

	private TradeServiceGrpc.TradeServiceBlockingStub tradeServiceBlockingStub;

	public WebController() {
		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", 6565)
				.usePlaintext().build();
		this.tradeServiceBlockingStub = TradeServiceGrpc.newBlockingStub(managedChannel);
	}

	@GetMapping("trades")
	public String listTrades() throws InvalidProtocolBufferException {
		ListTradesResponse response
				= this.tradeServiceBlockingStub.listTrades(ListTradesRequest.newBuilder().build());
		return JsonFormat.printer().print(response);
	}
}
