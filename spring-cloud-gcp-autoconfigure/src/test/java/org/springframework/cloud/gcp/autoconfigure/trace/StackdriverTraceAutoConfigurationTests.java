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

import brave.Span;
import brave.Tracer;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.devtools.cloudtrace.v1.Traces;
import org.junit.Test;
import zipkin2.reporter.Reporter;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.trace.sleuth.StackdriverTraceReporter;
import org.springframework.cloud.sleuth.autoconfig.SleuthProperties;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Ray Tsang
 * @author João André Martins
 */
public class StackdriverTraceAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					StackdriverTraceAutoConfigurationTests.MockConfiguration.class,
					StackdriverTraceAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					TraceAutoConfiguration.class,
					SleuthLogAutoConfiguration.class,
					RefreshAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.project-id=proj",
					"spring.sleuth.sampler.probability=1.0");

	@Test
	public void test() {
		this.contextRunner.run(context -> {
			SleuthProperties sleuthProperties = context.getBean(SleuthProperties.class);
			assertThat(sleuthProperties.isTraceId128()).isTrue();
			assertThat(sleuthProperties.isSupportsJoin()).isFalse();

			Reporter<zipkin2.Span> reporter = context.getBean(Reporter.class);
			assertThat(reporter).isInstanceOf(StackdriverTraceReporter.class);

			Tracer tracer = context.getBean(Tracer.class);
			Span span = tracer.newTrace()
					.start()
					.kind(Span.Kind.CLIENT)
					.name("test")
					.start();
			span.finish();

			// There should be one trace received
			MockConfiguration configuration = context.getBean(MockConfiguration.class);
			assertThat(configuration.tracesList.size()).isEqualTo(1);

			Traces traces = configuration.tracesList.get(0);
			assertThat(traces.getTracesCount()).isEqualTo(1);
			Trace trace = traces.getTraces(0);
			assertThat(trace.getSpansCount()).isEqualTo(1);
			TraceSpan traceSpan = trace.getSpans(0);
		});
	}

	public static class MockConfiguration {
		private List<Traces> tracesList = new ArrayList<>();

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public FlushableTraceConsumer traceConsumer() {
			return new FlushableTraceConsumer() {
				@Override
				public void flush() {
				}

				@Override
				public void receive(Traces traces) {
					MockConfiguration.this.tracesList.add(traces);
				}
			};
		}
	}
}
