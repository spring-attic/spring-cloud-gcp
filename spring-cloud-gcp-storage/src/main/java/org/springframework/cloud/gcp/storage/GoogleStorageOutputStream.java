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

package org.springframework.cloud.gcp.storage;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;

/**
 * Wraps Google Cloud Storage blob into an {@link OutputStream} to allow writing.
 *
 * <p>
 * Note that {@link #flush()} is not supported, and written data will only be visible
 * after calling {@link #close()} .
 *
 * @author Mike Eltsufin
 */
class GoogleStorageOutputStream extends OutputStream {

	private WriteChannel writeChannel;

	GoogleStorageOutputStream(Blob blob) {
		this.writeChannel = blob.writer();
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b });
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		this.writeChannel.write(ByteBuffer.wrap(b, off, len));
	}

	@Override
	public void flush() throws IOException {
		throw new UnsupportedOperationException("Flushing the output stream is not supported");
	}

	@Override
	public void close() throws IOException {
		this.writeChannel.close();
	}
}
