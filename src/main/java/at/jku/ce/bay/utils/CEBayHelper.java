package at.jku.ce.bay.utils;

import akka.actor.ActorRef;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CEBayHelper {

  private static String SYS_NAME = "sys_name";
  private static String SYS_HOST = "sys_host";
  private static int SYS_PORT = 2552;

  private static String REGISTRY_SYS_NAME = "cebay";
  private static String REGISTRY_SYS_HOST = "140.78.73.158";
  private static int REGISTRY_SYS_PORT = 2552;

  /**
	* Gets the remote actor address of the cebay registry.
	* @return the remote actor address
	*/
  public static String GetRegistryActorRef() {
	 return "akka.tcp://" + REGISTRY_SYS_NAME + "@" + REGISTRY_SYS_HOST + ":" + REGISTRY_SYS_PORT + "/user/cebay-registry";
  }

  /**
	* Gets the remote actor address of an actor.
	* @param actor the actor
	* @return the remote actor address
	*/
  public static String GetRemoteActorRef(ActorRef actor) {
	 return "akka.tcp://" + SYS_NAME + "@" + SYS_HOST + ":" + SYS_PORT + actor.path().toStringWithoutAddress();
  }

  /**
	* Gets the sha-256 value of a file.
	* @param file the file
	* @return the sha-256 value of a file
	*/
  public static String GetHash(File file) throws IOException, NoSuchAlgorithmException {
	 BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	 byte[] bytes = IOUtils.toByteArray(bis);
	 return new String(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

}
