/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.platform.config.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.springframework.platform.config.Environment;
import org.springframework.platform.config.PropertySource;
import org.springframework.security.rsa.crypto.RsaSecretEncryptor;

/**
 * @author Dave Syer
 *
 */
public class EncryptionControllerTests {

	private EncryptionController controller = new EncryptionController();

	@Test(expected = KeyNotInstalledException.class)
	public void cannotDecryptWithoutKey() {
		controller.decrypt("foo");
	}

	@Test(expected = KeyFormatException.class)
	public void cannotUploadPublicKey() {
		controller.uploadKey("ssh-rsa ...");
	}

	@Test(expected = KeyFormatException.class)
	public void cannotUploadPublicKeyPemFormat() {
		controller.uploadKey("---- BEGIN RSA PUBLIC KEY ...");
	}

	@Test(expected = InvalidCipherException.class)
	public void invalidCipher() {
		controller.uploadKey("foo");
		controller.decrypt("foo");
	}

	@Test
	public void sunnyDaySymmetricKey() {
		controller.uploadKey("foo");
		String cipher = controller.encrypt("foo");
		assertEquals("foo", controller.decrypt(cipher));
	}

	@Test
	public void sunnyDayRsaKey() {
		controller.setEncryptor(new RsaSecretEncryptor());
		String cipher = controller.encrypt("foo");
		assertEquals("foo", controller.decrypt(cipher));
	}

	@Test
	public void publicKey() {
		controller.setEncryptor(new RsaSecretEncryptor());
		String key = controller.getPublicKey();
		assertTrue("Wrong key format: " + key, key.startsWith("ssh-rsa"));
	}

	@Test
	public void decryptEnvironment() {
		controller.uploadKey("foo");
		String cipher = controller.encrypt("foo");
		Environment environment = new Environment("foo", "bar");
		environment.add(new PropertySource("spam", Collections
				.<Object, Object> singletonMap("my.secret", cipher)));
		Environment result = controller.decrypt(environment);
		assertEquals("foo", result.getPropertySources().get(0).getSource().get("my"));
	}

	@Test
	public void randomizedCipher() {
		controller.uploadKey("foo");
		String cipher = controller.encrypt("foo");
		assertNotEquals(cipher, controller.encrypt("foo"));
	}

}