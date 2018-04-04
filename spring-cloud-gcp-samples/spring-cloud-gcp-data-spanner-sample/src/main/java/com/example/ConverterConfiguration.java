package com.example;

import java.util.Arrays;

import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConverterConfiguration {

	@Bean
	public SpannerConverter spannerConverter(SpannerMappingContext spannerMappingContext) {
		return new MappingSpannerConverter(spannerMappingContext,
        Arrays.asList(new Person.PersonWriteConverter()),
        Arrays.asList(new Person.PersonReadConverter()));
	}
}
