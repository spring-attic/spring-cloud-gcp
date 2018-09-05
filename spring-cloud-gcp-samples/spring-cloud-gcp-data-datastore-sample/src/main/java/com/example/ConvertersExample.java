/*
 *  Copyright 2018 original author or authors.
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

package com.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.common.collect.ImmutableSet;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Dmitry Solomakha
 */
public class ConvertersExample {
	//Converter to read ImmutableSet
	//Note that you don't need a ImmutableSet to List converter
	static final Converter<List<?>, ImmutableSet<?>> LIST_IMMUTABLE_SET_CONVERTER =
			new Converter<List<?>, ImmutableSet<?>>() {
				@Override
				public ImmutableSet<?> convert(List<?> source) {
					return ImmutableSet.copyOf(source);
				}
			};

	//Converter to write custom Singer.Album type
	static final Converter<Album, String> ALBUM_STRING_CONVERTER =
			new Converter<Album, String>() {
				@Override
				public String convert(Album album) {
					return album.getAlbumName() + " " + album.getDate().format(DateTimeFormatter.ISO_DATE);
				}
			};

	//Converters to read custom Singer.Album type
	static final Converter<String, Album> STRING_ALBUM_CONVERTER =
			new Converter<String, Album>() {
				@Override
				public Album convert(String s) {
					String[] parts = s.split(" ");
					return new Album(parts[0], LocalDate.parse(parts[parts.length - 1], DateTimeFormatter.ISO_DATE));
				}
			};
}
