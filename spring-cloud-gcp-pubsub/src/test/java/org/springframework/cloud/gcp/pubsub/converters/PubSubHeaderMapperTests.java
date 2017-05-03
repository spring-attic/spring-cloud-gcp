package org.springframework.cloud.gcp.pubsub.converters;

import java.io.File;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.messaging.MessageHeaders;

/**
 * @author Vinicius Carvalho
 */
public class PubSubHeaderMapperTests {

	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

	@Test
	public void toHeaders() throws Exception{
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		mapper.afterPropertiesSet();
		Map<String,String> source = new HashMap<>();
		source.put("bool","false");
		source.put("int","42");
		source.put("float","1.0");
		source.put("double",Double.MAX_VALUE+"");
		source.put("long",Long.MAX_VALUE+"");
		source.put("string","foo");
		source.put("date",df.format(new Date()));
		source.put("objectToString","http://www.spring.io");
		MessageHeaders result = mapper.toHeaders(source);
		Assert.assertEquals(Boolean.class,result.get("bool").getClass());
		Assert.assertEquals(Integer.class,result.get("int").getClass());
		Assert.assertEquals(Float.class,result.get("float").getClass());
		Assert.assertEquals(Double.class,result.get("double").getClass());
		Assert.assertEquals(Long.class,result.get("long").getClass());
		Assert.assertEquals(String.class,result.get("string").getClass());
		Assert.assertEquals(String.class,result.get("objectToString").getClass());
		Assert.assertEquals(Date.class,result.get("date").getClass());
	}

	@Test
	public void fromHeaders() throws Exception{
		PubSubHeaderMapper mapper = new PubSubHeaderMapper();
		mapper.afterPropertiesSet();
		Map<String,Object> headerMap = new HashMap<>();
		Map<String,String> source = new HashMap<>();
		Date date = new Date();
		Object o = new Object();
		headerMap.put("bool",true);
		headerMap.put("int",1);
		headerMap.put("double",2.010012);
		headerMap.put("float",3.0f);
		headerMap.put("date",date);
		headerMap.put("object",o);
		mapper.fromHeaders(new MessageHeaders(headerMap),source);
		Assert.assertEquals("true",source.get("bool"));
		Assert.assertEquals("1",source.get("int"));
		Assert.assertEquals("3.0",source.get("float"));
		Assert.assertEquals("2.010012",source.get("double"));
		Assert.assertEquals(df.format(date),source.get("date"));
		Assert.assertEquals(o.toString(),source.get("object"));
	}
}
