package com.ctl.javadocker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;

@Path("greeting")
@ApplicationScoped
public class GreetingResource {

	private ConfigResolver.TypedResolver<String> fooResolver = ConfigResolver.resolve("foo").logChanges(true)
			.cacheFor(TimeUnit.MILLISECONDS, 10000);

	@Inject
	@ConfigProperty(name = "FOO_BAR_BAZ", defaultValue = "NOTH2ING")
	private String fooValue;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Greeting greeting() throws UnknownHostException {
		String host = InetAddress.getLocalHost().getHostName();
		String f = fooResolver.getValue();
		return new Greeting(1, "xhello!" + " > " + fooValue + " , " + f, host);
	}
}
