package com.mercateo.ops.tools.springbootstatus;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.VirtualMachine;
import com.zwitserloot.cmdreader.CmdReader;

public class SpringBootProcessStatus {
	private static StatusCmdLine cmd;

	private static Class<?> CLASS_WITHIN_BOOT_JAR = SpringBootProcessStatus.class;

	public static void main(String args[]) throws Exception {

		CmdReader<StatusCmdLine> reader = CmdReader.of(StatusCmdLine.class);
		cmd = parseCommandLine(reader, args);

		includeToolsJar(cmd.toolsJar);

		debug("Attaching to target VM");
		VirtualMachine vm = VirtualMachine.attach(cmd.pid);
		String version = getOutput(vm);
		System.out.println(version);

		String connectorAddr = jmxConnect(vm);

		try {

			JMXServiceURL serviceURL = new JMXServiceURL(connectorAddr);
			JMXConnector connector = JMXConnectorFactory.connect(serviceURL);
			MBeanServerConnection mbsc = connector.getMBeanServerConnection();
			String jmxUrl = "org.springframework.boot:type=Endpoint,name=healthEndpoint";
			debug("Query JMX for '" + jmxUrl + "'");
			ObjectName objName = new ObjectName(jmxUrl);

			String healthData = mbsc.getAttribute(objName, "Data").toString();
			debug("Health endpoint returned: '" + healthData + "'");

			StatusBean status = new StatusBean(healthData);
			debug("StatusBean parsed from healthData: '" + status.toString() + "'");

			exit(status.isUp() ? 0 : 1);

		} catch (InstanceNotFoundException e) {
			error("Boot application not yet ready");
			exit(1);
		} catch (Throwable e) {
			e.printStackTrace();
			exit(1);
		}
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
		Object error = vm.getSystemProperties().get(MetaDataAgent.ERR_PREFIX + "msg");
		if (error != null) {
			debug("Detected Agent-side errors");
			error(error.toString());
		}

		debug("Fetching Data");

		String alt = cmd.alternativeVersionAttribute;
		if (alt != null) {
			Object altVersion = vm.getSystemProperties().get(MetaDataAgent.MF_PREFIX + alt);

			if (altVersion != null) {
				String ret = altVersion.toString();
				if (ret.trim().length() > 0)
					return ret;
			}
		}

		// fall through, if altVersion is not set or not found

		Object maven_version = vm.getSystemProperties().get(MetaDataAgent.MF_PREFIX + "Implementation-Version");

		return maven_version.toString();
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
