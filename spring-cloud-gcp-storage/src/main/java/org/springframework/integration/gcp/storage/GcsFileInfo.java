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

package org.springframework.integration.gcp.storage;

import java.util.Date;

import com.google.cloud.storage.BlobInfo;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;

/**
 * @author João André Martins
 */
public class GcsFileInfo extends AbstractFileInfo<BlobInfo> {

	private BlobInfo gcsFile;

	public GcsFileInfo(BlobInfo gcsFile) {
		Assert.notNull(gcsFile, "The GCS blob can't be null.");
		this.gcsFile = gcsFile;
	}

	@Override
	public boolean isDirectory() {
		return this.gcsFile.isDirectory();
	}

	@Override
	public boolean isLink() {
		return false;
	}

	@Override
	public long getSize() {
		return this.gcsFile.getSize();
	}

	@Override
	public long getModified() {
		return this.gcsFile.getUpdateTime();
	}

	@Override
	public String getFilename() {
		return this.gcsFile.getName();
	}

	@Override
	public String getPermissions() {
		throw new UnsupportedOperationException("Use [BlobInfo.getAcl()] to obtain permissions.");
	}

	@Override
	public BlobInfo getFileInfo() {
		return this.gcsFile;
	}

	@Override
	public String toString() {
		return "FileInfo [isDirectory=" + isDirectory() + ", isLink=" + isLink()
				+ ", Size=" + getSize() + ", ModifiedTime="
				+ new Date(getModified()) + ", Filename=" + getFilename()
				+ ", RemoteDirectory=" + getRemoteDirectory() + "]";
	}
}
