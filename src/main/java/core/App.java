package core;

import java.net.UnknownHostException;

import userInterface.workSpaceSelect;

/*La clase app es la primera clase de toda aplicacion , en nuestro caso inicializa el primer componente
 * de interfaz que contendrá los elemntos necesarios para lanzar la interfaz principal de la aplicación
 */

public class App {

	public static void main(String[] args) throws UnknownHostException {

		// DeveloperComponent dp = new DeveloperComponent(null);
		workSpaceSelect a = new workSpaceSelect();

	}

}
