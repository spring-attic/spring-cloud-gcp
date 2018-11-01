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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Reference;
import org.springframework.data.annotation.Id;

/**
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 */
@Entity(name = "singers")
public class Singer {

	@Id
	@Field(name = "singer_id")
	private String singerId;

	@Field(name = "first_name")
	private String firstName;

	@Field(name = "last_name")
	private String lastName;

	@Reference
	private Band firstBand;

	@Reference
	private List<Band> bands;

	@Descendants
	private Set<Instrument> personalInstruments;

	private Set<Album> albums;

	public Singer() {
	}

	public Singer(String id, String firstName, String lastName, Set<Album> albums) {
		this.singerId = id;
		this.firstName = firstName;
		this.lastName = lastName;
		this.albums = albums;
	}

	public String getSingerId() {
		return this.singerId;
	}

	public void setSingerId(String singerId) {
		this.singerId = singerId;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Band getFirstBand() {
		return this.firstBand;
	}

	public void setFirstBand(Band firstBand) {
		this.firstBand = firstBand;
	}

	public List<Band> getBands() {
		return this.bands;
	}

	public void setBands(List<Band> bands) {
		this.bands = bands;
	}

	public Set<Instrument> getPersonalInstruments() {
		return this.personalInstruments;
	}

	public void setPersonalInstruments(Set<Instrument> personalInstruments) {
		this.personalInstruments = personalInstruments;
	}

	public Set<Album> getAlbums() {
		return this.albums;
	}

	public void setAlbums(Set<Album> albums) {
		this.albums = albums;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Singer singer = (Singer) o;
		return Objects.equals(getSingerId(), singer.getSingerId())
				&& Objects.equals(getFirstName(), singer.getFirstName())
				&& Objects.equals(getLastName(), singer.getLastName())
				&& Objects.equals(this.albums, singer.albums);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getSingerId(), getFirstName(), getLastName(), this.albums);
	}

	@Override
	public String toString() {
		return "Singer{" + "singerId='" + this.singerId + '\'' + ", firstName='"
				+ this.firstName + '\'' + ", lastName='" + this.lastName + '\''
				+ ", albums=" + this.albums + ", firstBand=" + this.firstBand + ", bands="
				+ (this.bands == null ? ""
						: Strings.join(this.bands.stream().map(x -> x.getName())
								.collect(Collectors.toList()), ','))
				+ ", personalInstruments="
				+ (this.personalInstruments == null ? ""
						: Strings.join(this.personalInstruments.stream()
								.map(x -> x.getType()).collect(Collectors.toList()), ','))
				+ '}';
	}
}
