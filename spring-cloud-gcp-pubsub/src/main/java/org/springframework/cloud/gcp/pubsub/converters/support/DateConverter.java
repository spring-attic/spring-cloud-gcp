package org.springframework.cloud.gcp.pubsub.converters.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.cloud.gcp.pubsub.converters.HeaderConverter;

/**
 * @author Vinicius Carvalho
 */
public class DateConverter implements HeaderConverter<Date> {

	private String datePattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	private DateFormat df;

	public DateConverter(){
		this(null);
	}

	public DateConverter(String datePattern) {
		if(datePattern != null){
			this.datePattern = datePattern;
		}
		this.df = new SimpleDateFormat(this.datePattern);
	}

	@Override
	public String encode(Date value) {
		return this.df.format(value);
	}

	@Override
	public Date decode(String value) {
		Date result = null;
		try{
			result = df.parse(value);
		}
		catch (ParseException e) {
		}
		return result;
	}
}
