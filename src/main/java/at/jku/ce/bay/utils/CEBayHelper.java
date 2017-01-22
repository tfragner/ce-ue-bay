package at.jku.ce.bay.utils;

import akka.actor.ActorRef;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CEBayHelper {

//  private static String SYS_NAME = "server139System";
//  private static String SYS_HOST = "91.119.52.245";
  private static String SYS_HOST = "140.78.196.52";
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
  	String sysName = actor.path().address().toString().substring(7);
	 return "akka.tcp://" + sysName + "@" + SYS_HOST + ":" + SYS_PORT + actor.path().toStringWithoutAddress();

  }

	/**
	 * Gets the sha-256 value of a file.
	 * @param file the file
	 * @return the sha-256 value of a file
	 * @throws IOException Fehler
	 * @throws NoSuchAlgorithmException Fehler
	 */
  public static String GetHash(File file) throws IOException, NoSuchAlgorithmException {
	 BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	 byte[] bytes = IOUtils.toByteArray(bis);
	 return new String(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

}
