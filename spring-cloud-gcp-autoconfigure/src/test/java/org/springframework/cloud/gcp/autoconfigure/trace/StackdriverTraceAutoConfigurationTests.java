/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.trace;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import brave.Span;
import brave.Tracer;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.mockito.Mockito.mock;

/**
 * @author Ray Tsang
 * @author João André Martins
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		StackdriverTraceAutoConfigurationTests.MockConfiguration.class,
		StackdriverTraceAutoConfiguration.class,
		GcpContextAutoConfiguration.class,
		TraceAutoConfiguration.class,
		SleuthLogAutoConfiguration.class,
		RefreshAutoConfiguration.class }, properties = {
				"spring.cloud.gcp.project-id=proj",
				"spring.sleuth.sampler.probability=1.0",
				"spring.cloud.gcp.config.enabled=false"
		})
public abstract class StackdriverTraceAutoConfigurationTests {

	@Configuration
	public static class MockConfiguration {
		private List<Traces> tracesList = new ArrayList<>();

		@Bean
		public FlushableTraceConsumer traceConsumer() {
			return new FlushableTraceConsumer() {
				@Override
				public void flush() {
					// do nothing
				}

				@Override
				public void receive(Traces traces) {
					MockConfiguration.this.tracesList.add(traces);
				}
			};
		}

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}
	}

	public static class TraceConsumerSpanTests extends StackdriverTraceAutoConfigurationTests {
		@Autowired
		MockConfiguration configuration;

		@Autowired
		Tracer tracer;

		@PostConstruct
		public void init() {
			this.configuration.tracesList.clear();
		}

		@Test
		public void test() {
			Span span = this.tracer.newTrace()
					.kind(Span.Kind.CLIENT)
					.name("test")
					.start();
			span.finish();

			// There should be one trace received
			Assert.assertEquals(1, this.configuration.tracesList.size());
			Traces traces = this.configuration.tracesList.get(0);
			Assert.assertEquals(1, traces.getTracesCount());
			Trace trace = traces.getTraces(0);
			Assert.assertEquals(1, trace.getSpansCount());
			TraceSpan traceSpan = trace.getSpans(0);
		}

	}
}
