package com.mercateo.ops.tools.springbootstatus;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.Properties;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.zwitserloot.cmdreader.CmdReader;

public class SpringBootProcessStatus {
	private static final String JMX_URL = "org.springframework.boot:type=Endpoint,name=healthEndpoint";

	private static final long WAIT_INTERVAL = 100;

	private static StatusCmdLine cmd;

	private static Class<?> CLASS_WITHIN_BOOT_JAR = SpringBootProcessStatus.class;

	public static void main(String args[]) throws Exception {

		CmdReader<StatusCmdLine> reader = CmdReader.of(StatusCmdLine.class);
		cmd = parseCommandLine(reader, args);

		includeToolsJar(cmd.toolsJar);

		debug("Attaching to target VM");
		VirtualMachine vm = VirtualMachine.attach(cmd.pid);
		debug("Attaching done");

		String version = getOutput(vm);
		System.out.println(version);

		if (cmd.block) {
			if (cmd.timeout < 1) {
				error("blockmode enabled and timeout<1. Please use a positive timeout.");
				exit(99);
			}

			long timeLimit = cmd.timeout * 1000 + System.currentTimeMillis();

			try {
				String connectorAddr = jmxConnect(vm);
				MBeanServerConnection mbsc = getConnection(connectorAddr);

				while (System.currentTimeMillis() < timeLimit) {

					try {
						ObjectName objName = createObjectName();
						String healthData = retrieveHealth(mbsc, objName);
						StatusBean status = parse(healthData);

						if (status.isUp()) {
							debug("Status UP reached: '" + status.toString() + "'");
							exit(0);
						}
					} catch (InstanceNotFoundException e) {
						// skip
					}

					Thread.sleep(WAIT_INTERVAL);
				}

				// timeout reached
				error("timeout of " + cmd.timeout + "s reached");
				exit(1);

			} catch (Throwable e) {
				e.printStackTrace();
				exit(1);
			}

		} else {

			String connectorAddr = jmxConnect(vm);

			try {

				MBeanServerConnection mbsc = getConnection(connectorAddr);
				ObjectName objName = createObjectName();

				String healthData = retrieveHealth(mbsc, objName);
				StatusBean status = parse(healthData);

				exit(status.isUp() ? 0 : 1);

			} catch (InstanceNotFoundException e) {
				error("Boot application not yet ready");
				exit(1);
			} catch (Throwable e) {
				e.printStackTrace();
				exit(1);
			}
		}
	}

	private static StatusBean parse(String healthData) {
		StatusBean sb = new StatusBean(healthData);
		debug("StatusBean parsed from healthData: '" + sb.toString() + "'");
		return sb;
	}

	private static String retrieveHealth(MBeanServerConnection mbsc, ObjectName objName) throws MBeanException,
			AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
		String healthData = mbsc.getAttribute(objName, "Data").toString();
		debug("Health endpoint returned: '" + healthData + "'");
		return healthData;
	}

	private static ObjectName createObjectName() throws MalformedObjectNameException {
		debug("Query JMX for '" + JMX_URL + "'");
		return new ObjectName(JMX_URL);
	}

	private static MBeanServerConnection getConnection(String connectorAddr) throws MalformedURLException, IOException {
		JMXServiceURL serviceURL = new JMXServiceURL(connectorAddr);
		JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
		MBeanServerConnection mbsc = connector.getMBeanServerConnection();
		return mbsc;
	}

	private static String jmxConnect(VirtualMachine vm)
			throws IOException, AgentLoadException, AgentInitializationException {
		debug("Looking for JMX Connector");
		String connectorAddr = getLocalConnector(vm);
		if (connectorAddr == null) {
			debug("JMX Connector not found, creating one.");

			String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator
					+ "management-agent.jar";

			debug("loading management agent from: '" + agent + "'");
			vm.loadAgent(agent);
			connectorAddr = getLocalConnector(vm);
		}
		debug("JMX Connector available");
		return connectorAddr;
	}

	private static void exit(int returnCode) {
		System.exit(returnCode);
	}

	private static String getLocalConnector(VirtualMachine vm) throws IOException {
		return vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress");
	}

	private static String getOutput(VirtualMachine vm)
			throws ClassNotFoundException, AgentLoadException, AgentInitializationException, IOException {
		debug("Loading Agent into target VM");
		vm.loadAgent(Util.findJarContaining(CLASS_WITHIN_BOOT_JAR).getName());
		Properties props = vm.getSystemProperties();
		Object error = props.get(MetaDataAgent.ERR_PREFIX + "msg");

		if (error != null) {
			debug("Detected Agent-side errors");
			error(error.toString());
		}

		debug("Fetching Data");
		if (cmd.verbose) {
			props.entrySet().stream().filter(e -> e.getKey().toString().startsWith(MetaDataAgent.MF_PREFIX))
					.forEach(e -> debug(e.getKey() + ":" + e.getValue()));
		}

		String alt = cmd.alternativeVersionAttribute;
		if (alt != null) {
			Object altVersion = props.get(MetaDataAgent.MF_PREFIX + alt);

			if (altVersion != null) {
				String ret = altVersion.toString();
				if (ret.trim().length() > 0)
					return ret;
			}
		}

		// fall through, if altVersion is not set or not found

		return Optional.ofNullable(props.get(MetaDataAgent.MF_PREFIX + "Implementation-Version"))
				.map(Object::toString)
				.orElse("n/a");
	}

	private static void includeToolsJar(String toolsJar) throws Exception {

		if (toolsJar == null) {
			toolsJar = findToolsJar();
		}
		File tools = new File(toolsJar);
		if (tools.exists() && tools.canRead()) {
			addToClassPath(tools);
		} else {
			error("Cannot read '" + tools.getAbsolutePath() + "'");

		}

	}

	private static String findToolsJar() {
		debug("Loooking for tools.jar");

		String homeLib = System.getProperty("java.home") + File.separator + ".." + File.separator + "lib"
				+ File.separator + "tools.jar";
		if (checkFileExists(homeLib)) {
			return homeLib;
		}

		String jreLib = System.getProperty("java.home") + File.separator + "lib" + File.separator + "tools.jar";
		if (checkFileExists(jreLib)) {
			return jreLib;
		}

		String fruitLib = System.getProperty("java.home") + File.separator + "Classes" + File.separator + "classes.jar";
		if (checkFileExists(fruitLib)) {
			return fruitLib;
		}

		error("Unable to find tools.jar");
		return null;
	}

	private static void error(String string) {
		System.err.println("[ERROR] " + string);
		exit(1);
	}

	private static boolean checkFileExists(String homeLib) {
		boolean exists = new File(homeLib).exists();
		debug((!exists ? "missing " : "found   ") + "'" + homeLib + "'");
		return exists;
	}

	private static void debug(String string) {
		if (cmd.verbose) {
			System.err.println("[DEBUG] " + string);
		}
	}

	private static StatusCmdLine parseCommandLine(CmdReader<StatusCmdLine> reader, String[] args) {
		StatusCmdLine cmd = null;
		try {
			cmd = reader.make(args);
			if (cmd.help) {
				exit(help(reader));
			}
		} catch (Throwable e) {
			if ((cmd != null) && cmd.verbose) {
				e.printStackTrace();
			}
			exit(help(reader));
		}
		return cmd;
	}

	private static int help(CmdReader<StatusCmdLine> reader) {
		System.err.println(reader.generateCommandLineHelp("sb-status"));
		return -1;
	}

	private static void addToClassPath(File file) throws Exception {
		debug("Adding " + file.getCanonicalPath() + " to the classpath");
		Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
		method.setAccessible(true);
		method.invoke(ClassLoader.getSystemClassLoader(), new Object[] { file.toURI().toURL() });
	}
}
