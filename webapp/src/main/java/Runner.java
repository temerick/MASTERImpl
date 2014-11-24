import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

import java.net.URL;
import java.security.ProtectionDomain;

public class Runner {
    public static final int DEFAULT_PORT = 8081;
    private final Config conf = ConfigFactory.load().getConfig("bullseye");
    private Integer port;

    public Runner(final Integer port) {
	this.port = port;
    }

    public void start() {
	try {
	    final Server server = new Server();

	    if (port != null) {
		final SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(port);
		connector.setMaxIdleTime(60000);
		server.addConnector(connector);
	    }

	    final ProtectionDomain domain = Runner.class.getProtectionDomain();
	    final URL location = domain.getCodeSource().getLocation();
	    final WebAppContext webapp = new WebAppContext();
	    webapp.setContextPath(conf.getString("app.contextPath"));
	    webapp.setWar(location.toExternalForm());
	    server.setHandler(webapp);

	    server.start();
	    server.join();
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(-1);
	}
    }

    private static Integer getPort(final String[] args) {
	if (args.length >= 1) {
	    try {
		return Integer.parseInt(args[0]);
	    } catch (NumberFormatException e) {
		System.out.println("Error parsing port number as an integer: " + args[0]);
	    }
	}
	return null;
    }

    public static void main(final String[] args) {
	Integer port = getPort(args);
	if (port == null) port = DEFAULT_PORT;
	System.out.println("Starting embedded server on port [" + port + "]");
	final Runner runner = new Runner(port);
	runner.start();
    }
}
