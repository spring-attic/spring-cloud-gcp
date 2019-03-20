/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.util.Assert;

/**
 * A factory for creating SpannerTemplates that allows the user to customize database
 * clients for each method using the arguments each operation was called with.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public final class ConfigurableDatabaseClientSpannerTemplateFactory {

	private ConfigurableDatabaseClientSpannerTemplateFactory() {

	}

	/**
	 * Apply the database client settings from user-defined logic to a standard
	 * SpannerTemplate that can execute real operations.
	 * @param workTemplate the template that can execute real operations on Cloud Spanner.
	 * @param clientSetterTemplate a custom override template instance where the database
	 *     client from which the database client is extracted.
	 * @return a SpannerTemplate that executes operations using the database client determined
	 * by the {@code clientSetterTemplate}.
	 */
	public static SpannerTemplate applyDatabaseClientSetterBehavior(SpannerTemplate workTemplate,
			DatabaseClientSettingSpannerTemplate clientSetterTemplate) {
		return (SpannerTemplate) Enhancer.create(SpannerTemplate.class,
				new DatabaseClientSetterInvocationHandler(workTemplate, clientSetterTemplate));
	}

	/**
	 * Accepts an overriden template and produces a template that intercept's and utilizes the
	 * user's per-method and parameter-based database clients.
	 * @param configuringTemplate the template that the user has created setting database
	 *     clients on a per-call and parameter-based basis.
	 * @return a configuring template that is ready to be used by
	 * {@code applyDatabaseClientSetterBehavior}.
	 */
	public static DatabaseClientSettingSpannerTemplate prepareDatabaseClientConfigurationSpannerTemplate(
			SettableClientSpannerTemplate configuringTemplate) {
		return (DatabaseClientSettingSpannerTemplate) Enhancer.create(DatabaseClientSettingSpannerTemplate.class,
				new DatabaseClientSettingSpannerTemplateHandler(configuringTemplate));
	}

	private static final class DatabaseClientSettingSpannerTemplateHandler implements MethodInterceptor {

		private final SettableClientSpannerTemplate settableClientSpannerTemplate;

		private final Set<Integer> declaredMethodHashes = new HashSet<>();

		private DatabaseClientSettingSpannerTemplateHandler(SettableClientSpannerTemplate configuringTemplate) {
			this.settableClientSpannerTemplate = configuringTemplate;
			Arrays.stream(configuringTemplate.getClass().getDeclaredMethods())
					.forEach(x -> this.declaredMethodHashes.add(getMethodHash(x)));
		}

		private static int getMethodHash(Method method) {
			return Objects.hash(method.getName(), method.getReturnType(), Arrays.hashCode(method.getParameterTypes()));
		}

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy)
				throws Throwable {
			if (this.declaredMethodHashes.contains(getMethodHash(method))
					|| SettableClientSpannerTemplate.class.isAssignableFrom(method.getDeclaringClass())) {
				return method.invoke(this.settableClientSpannerTemplate, objects);
			}
			return null;
		}
	}

	private static final class DatabaseClientSetterInvocationHandler implements MethodInterceptor {

		private final SpannerTemplate workTemplate;

		private final DatabaseClientSettingSpannerTemplate clientSetterTemplate;

		private DatabaseClientSetterInvocationHandler(SpannerTemplate workTemplate,
				DatabaseClientSettingSpannerTemplate clientSetterTemplate) {
			Assert.notNull(workTemplate, "A non-null SpannerTemplate to execute Cloud Spanner operations is required.");
			Assert.notNull(workTemplate,
					"A non-null SpannerTemplate that sets the database client by-operation is required.");
			this.workTemplate = workTemplate;
			this.clientSetterTemplate = clientSetterTemplate;
		}

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			method.invoke(this.clientSetterTemplate, objects);
			this.workTemplate.setDatabaseClientProvider(this.clientSetterTemplate.getDatabaseClientProvider());
			return method.invoke(this.workTemplate, objects);
		}
	}
}
