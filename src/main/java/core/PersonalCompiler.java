package core;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;

import console.ConsoleWrapper;
import fileManagement.FILE_PROPERTIES;
import userInterface.ObserverActions;
import userInterface.PropertyChangeMessenger;

public class PersonalCompiler implements PropertyChangeListener {

	public InputStream in;
	public OutputStream out;
	public OutputStream err;
	ByteArrayOutputStream outbaos = new ByteArrayOutputStream();
	ByteArrayOutputStream errbaos = new ByteArrayOutputStream();
	PrintStream out3 = new PrintStream(errbaos);
	PrintStream out2 = new PrintStream(outbaos);
	private PropertyChangeMessenger support;
	ConsoleWrapper console;
	PrintStream stdout = System.out;
	PrintStream stderr = System.err;
	InputStream stdin = System.in;
	private String[] compiling; 

	public PersonalCompiler() {

		support = PropertyChangeMessenger.getInstance();
		console = new ConsoleWrapper();
	}

	public String run(String className, URLData[] added) {

		try {
			// Copiar ficheros
			File bindir = new File(added[0].project + "\\bin"); 
			if (!bindir.exists()) {
				
				bindir.mkdir(); 
				ArrayList<Object> list = new ArrayList<Object>();
				final Path file = Paths.get(bindir.getAbsolutePath());
				final UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);

				String property = FILE_PROPERTIES.binProperty;

				byte[] bytes = null;

				try {
					bytes = property.getBytes("UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}

				final ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
				writeBuffer.put(bytes);
				writeBuffer.flip();
				try {
					view.write(property, writeBuffer);
				} catch (IOException e) {
					e.printStackTrace();
				}

		
				list.add(added[0].project);
				list.add("bin");
				list.add(added[0].project);
				
				support.notify(ObserverActions.UPDATE_PROJECT_TREE_ADD, null, list);
			}

			File[] copied = new File[added.length];

			for (int i = 0; i < added.length; i++) {

				File destFile = new File(added[i].project + "\\" + "bin\\" + added[i].name + ".java");
			
				File sourceFile = new File(added[i].path);
				try {
					Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}

				copied[i] = destFile;

			}

			// Compilar
			// Comprobar que esta el jdk
			JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				DEBUG.debugmessage("SE REQUIERE EL USO DE JAVA JDK ");
			} else {
				DEBUG.debugmessage("SE PUEDE COMPILAR EL FICHERO");
			}

			compiling = new String[copied.length];
			DEBUG.setExecuting();
			System.setOut(console.outPrint);
			System.setErr(console.errPrint);
			System.setIn(console.inStream);
			boolean cantcompile = false;

			for (int i = 0; i < copied.length; i++) {
				String actualname = copied[i].getName().substring(copied[i].getName().lastIndexOf("\\") + 1,
						copied[i].getName().lastIndexOf("."));
				String extension = copied[i].getName().substring(copied[i].getName().lastIndexOf("."),
						copied[i].getName().length());

				if (extension.equals(".java")) {

					compiling[i] = added[i].project + "\\bin\\" + actualname + ".java";

				}
			}

			int compilationResult = compiler.run(null, null, null, compiling);
			if (compilationResult != 0) {
				cantcompile = true;
			}

			if (!cantcompile) {

				URL[] binfolder = new URL[1];
				try {
					binfolder[0] = new File(added[0].project + "\\bin").toURI().toURL();
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				URLClassLoader classLoader = new URLClassLoader(binfolder);

				try {
					String rawname = className.substring(0, className.lastIndexOf("."));
					Class<?> cls = Class.forName(rawname, true, classLoader);
					Object instance = cls.newInstance();
					Class[] argTypes = new Class[] { String[].class };
					
					

					Method method = cls.getDeclaredMethod("main", argTypes);

					String[] args = new String[0];

					String[] mainArgs = Arrays.copyOfRange(args, 0, args.length);
					try {
						support.notify(ObserverActions.ENABLE_TERMINATE, null, null);
						method.invoke(instance, (Object) mainArgs);
					} catch (Exception e) {
						console.reset();

					}

				} catch (Exception e) {
					System.setOut(stdout);
					System.setErr(stderr);
					System.setIn(stdin);
					e.printStackTrace();

					DEBUG.debugmessage("HA OCURRIDO UN ERROR A LA HORA DE COMPILAR EXTERNO AL CODIGO");
				}

				
				safetyDelete(); 

				DEBUG.unsetExecuting();
				System.setOut(stdout);
				System.setErr(stderr);
				System.setIn(stdin);


				DEBUG.debugmessage("SE HA ACABADO LA EJECUCION");

				return null;

			} else {
				DEBUG.unsetExecuting();
				System.setOut(stdout);
				System.setErr(stderr);
				System.setIn(stdin);
				console.reset();

				return null;
			}

		} catch (Exception e) {
			DEBUG.unsetExecuting();
			System.setOut(stdout);
			System.setErr(stderr);
			System.setIn(stdin);

			console.reset();
			return null;

		}
	}

	public void reactivateRunningProccess(String retrieved) {
		console.setLastRead(retrieved);
		console.releaseSemaphore();

	}

	public void safetyDelete() {
		if(compiling != null ) {
			
			for (String s : compiling) {
				File f = new File(s);
				f.delete();
			}

		}
		
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		ObserverActions action = ObserverActions.valueOf(evt.getPropertyName());
		switch(action) {
		case SAFETY_DELETE:
			
			safetyDelete();
		break; 
		default:
			break;
		}
		
	}
}