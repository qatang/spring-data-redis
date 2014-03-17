/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.data.redis.connection.jedis;

import static org.hamcrest.core.IsEqual.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.springframework.data.redis.connection.AbstractConnectionUnitTestBase;
import org.springframework.data.redis.connection.RedisServerCommands.ShutdownOption;
import org.springframework.data.redis.connection.jedis.JedisConnectionUnitTestSuite.JedisConnectionPipelineUnitTests;
import org.springframework.data.redis.connection.jedis.JedisConnectionUnitTestSuite.JedisConnectionUnitTests;

import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;

/**
 * @author Christoph Strobl
 */
@RunWith(Suite.class)
@SuiteClasses({ JedisConnectionUnitTests.class, JedisConnectionPipelineUnitTests.class })
public class JedisConnectionUnitTestSuite {

	public static class JedisConnectionUnitTests extends AbstractConnectionUnitTestBase<Client> {

		protected JedisConnection connection;

		@Before
		public void setUp() {
			connection = new JedisConnection(new MockedClientJedis("http://localhost:1234", getNativeRedisConnectionMock()));
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownWithNullShouldDelegateCommandCorrectly() {

			connection.shutdown(null);
			verifyNativeConnectionInvocation().shutdown();
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownNosaveShouldBeSentCorrectlyUsingLuaScript() {

			connection.shutdown(ShutdownOption.NOSAVE);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verifyNativeConnectionInvocation().eval(captor.capture(), Matchers.any(byte[].class),
					Matchers.any(byte[][].class));

			assertThat(captor.getValue(), equalTo("return redis.call('SHUTDOWN','NOSAVE')".getBytes()));
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Test
		public void shutdownSaveShouldBeSentCorrectlyUsingLuaScript() {

			connection.shutdown(ShutdownOption.SAVE);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verifyNativeConnectionInvocation().eval(captor.capture(), Matchers.any(byte[].class),
					Matchers.any(byte[][].class));

			assertThat(captor.getValue(), equalTo("return redis.call('SHUTDOWN','SAVE')".getBytes()));
		}

	}

	public static class JedisConnectionPipelineUnitTests extends JedisConnectionUnitTests {

		@Before
		public void setUp() {
			super.setUp();
			connection.openPipeline();
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Override
		@Test(expected = UnsupportedOperationException.class)
		public void shutdownNosaveShouldBeSentCorrectlyUsingLuaScript() {
			super.shutdownNosaveShouldBeSentCorrectlyUsingLuaScript();
		}

		/**
		 * @see DATAREDIS-184
		 */
		@Override
		@Test(expected = UnsupportedOperationException.class)
		public void shutdownSaveShouldBeSentCorrectlyUsingLuaScript() {
			super.shutdownSaveShouldBeSentCorrectlyUsingLuaScript();
		}

	}

	/**
	 * {@link Jedis} extension allowing to use mocked object as {@link Client}.
	 */
	private static class MockedClientJedis extends Jedis {

		public MockedClientJedis(String host, Client client) {
			super(host);
			this.client = client;
		}

	}

}
