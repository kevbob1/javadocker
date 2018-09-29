package com.ctl.javadocker.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.deltaspike.core.api.config.Config;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

public class ZooKeeperConfigSource implements ConfigSource {

	private static final Logger								logger												= LoggerFactory
	    .getLogger(ZooKeeperConfigSource.class);

	//Apache Curator framework used to access Zookeeper
	private AtomicReference<CuratorFramework>	curatorReference							= new AtomicReference<>();

	//Root node of an application's configuration
	private String														applicationId;

	//Prefix of ignored properties
	private static final String								IGNORED_PREFIX								= "io.smallrye.configsource.zookeeper";

	//Property the URL of the Zookeeper instance will be read from
	private static final String								ZOOKEEPER_URL_KEY							= "io.smallrye.configsource.zookeeper.url";

	//Property of the Application Id. This will be the root znode for an application's properties
	private static final String								APPLICATION_ID_KEY						= "io.smallrye.configsource.zookeeper.applicationId";

	//Name of this ConfigSource
	private static final String								ZOOKEEPER_CONFIG_SOURCE_NAME	= "io.smallrye.configsource.zookeeper";

	public ZooKeeperConfigSource() {
	}

	@Override
	public int getOrdinal() {
		return 150;
	}

	@Override
	public Set<String> getPropertyNames() {

		final Set<String> propertyNames = new HashSet<>();

		try {
			final List<String> children = getCuratorClient().getChildren().forPath(applicationId);
			propertyNames.addAll(children);
		} catch (Exception e) {
			logger.warn(e.getMessage(), e);
		}

		return propertyNames;
	}

	@Override
	public Map<String, String> getProperties() {

		final Map<String, String> props = new HashMap<>();

		try {
			final List<String> children = getCuratorClient().getChildren().forPath(applicationId);
			for (final String key : children) {
				final String value = new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
				props.put(key, value);
			}
		} catch (Exception e) {
			logger.log(Level.WARN, e.getMessage(), e);
		}

		return props;
	}

	@Override
	public String getValue(final String key) {

		/*
		 * Explicitly ignore all keys that are prefixed with the prefix used to configure the Zookeeper connection.
		 * Other wise a stack overflow obviously happens.
		 */
		if (key.startsWith(IGNORED_PREFIX)) {
			return null;
		}
		try {
			final Stat stat = getCuratorClient().checkExists().forPath(applicationId + "/" + key);

			if (stat != null) {
				return new String(getCuratorClient().getData().forPath(applicationId + "/" + key));
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.log(Level.WARN, e.getMessage(), e);
		}
		return null;
	}

	@Override
	public String getName() {
		return ZOOKEEPER_CONFIG_SOURCE_NAME;
	}

	private CuratorFramework getCuratorClient() throws ZooKeeperConfigException {

		CuratorFramework cachedClient = curatorReference.get();
		if (cachedClient == null) {

			final Config cfg = ConfigResolver.getConfig();

			final Optional<String> zookeeperUrl = cfg.getOptionalValue(ZOOKEEPER_URL_KEY, String.class);
			final Optional<String> optApplicationId = cfg.getOptionalValue(APPLICATION_ID_KEY, String.class);

			//Only create the ZK Client if the properties exist.
			if (zookeeperUrl.isPresent() && optApplicationId.isPresent()) {

				logger.info("Configuring ZooKeeperConfigSource using url: " + zookeeperUrl + ", applicationId: " + optApplicationId.get());

				applicationId = optApplicationId.get();

				if (!applicationId.startsWith("/")) {
					applicationId = "/" + applicationId;
				}
				cachedClient = CuratorFrameworkFactory.newClient(zookeeperUrl.get(), new ExponentialBackoffRetry(1000, 3));
				curatorReference.compareAndSet(null, cachedClient);
				cachedClient.start();

			} else {
				throw new ZooKeeperConfigException(
				    "Please set properties for \"" + ZOOKEEPER_URL_KEY + "\" and \"" + APPLICATION_ID_KEY + "\"");
			}
		}
		return cachedClient;
	}
}