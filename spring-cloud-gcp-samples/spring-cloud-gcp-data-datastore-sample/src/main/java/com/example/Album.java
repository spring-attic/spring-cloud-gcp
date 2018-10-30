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
import java.util.Objects;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;

/**
 * @author Dmitry Solomakha
 */
@Entity
public class Album {
	final private String albumName;

	final private LocalDate date;

	Album(String albumName, LocalDate date) {
		this.albumName = albumName;
		this.date = date;
	}

	String getAlbumName() {
		return this.albumName;
	}

	LocalDate getDate() {
		return this.date;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Album album = (Album) o;
		return Objects.equals(getAlbumName(), album.getAlbumName())
				&& Objects.equals(this.date, album.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getAlbumName(), this.date);
	}

	@Override
	public String toString() {
		return "Album{" + "albumName='" + this.albumName + '\'' + ", date=" + this.date
				+ '}';
	}
}
