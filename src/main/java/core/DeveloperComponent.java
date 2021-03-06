package core;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import commandController.CommandController;
import fileManagement.FILE_PROPERTIES;
import fileManagement.FILE_TYPE;
import fileManagement.FileManager;
import fileManagement.WorkSpace;
import fileManagement.WorkSpaceManager;
import javaMiniSockets.clientSide.AsynchronousClient;
import javaMiniSockets.serverSide.AsynchronousServer;
import javaMiniSockets.serverSide.ServerMessageHandler;
import network.ClientHandler;
import network.ServerHandler;
import networkMessages.GlobalRunDone;
import networkMessages.GlobalRunRequestMessage;
import networkMessages.GlobalRunRequestResponse;
import networkMessages.ResponseCreateFileMessage;
import networkMessages.WriteToConsoleMessage;
import observerController.ObserverActions;
import observerController.PropertyChangeMessenger;
import userInterface.uiFileNavigation.CustomTreeNode;
import userInterface.uiGeneral.DeveloperMainFrame;
import userInterface.uiGeneral.DeveloperMainFrameWrapper;
import userInterface.uiGeneral.RunConfigDialog;

public class DeveloperComponent implements PropertyChangeListener {

	public DeveloperMainFrame developerMainFrame;
	public AsynchronousServer server;
	public AsynchronousClient client;
	public Thread runningThread;
	public String expectedWorkSpaceLocation = null;
	public boolean isConnected;

	// Project to run code from
	public String focusedProject;
	// Structure that , given a project name will return the focused class from this
	// project
	// The focused class is the entrypoint of the program , for example , the main
	// class
	public HashMap<String, String> projectFocusPairs;
	public CountDownLatch waitingResponses;
	public ArrayBlockingQueue<String> consoleBuffer = new ArrayBlockingQueue<>(100);
	LinkedList<ResponseCreateFileMessage> responses = null;
	private PersonalCompiler compiler;
	private FileManager fileManager;
	private PropertyChangeMessenger support;
	private int defaultQueueSize = 100;
	private ServerHandler handler;
	private ClientHandler clientHandler;
	// The workspace the user chose
	public WorkSpace workSpace = null;
	// Structure that saves classpaths and uses projects as its keys
	private HashMap<String, ClassPath> classpaths;
	private String separator = "pairLeap.codeString";
	public String chosenName = null;
	private Thread consoleSender;

	public DeveloperComponent(WorkSpace workSpace) {

		// Initialize everything
		isConnected = false;
		focusedProject = null;
		support = PropertyChangeMessenger.getInstance();
		this.workSpace = workSpace;

		projectFocusPairs = new HashMap<>();
		classpaths = new HashMap<>();
		fileManager = new FileManager();
		compiler = new PersonalCompiler();

		// Other parts of this software may need references to developer component , so
		// we save it in uicontroller
		CommandController.getInstance().setDeveloperComponent(this);

		// Start the user interface
		try {
			SwingUtilities.invokeAndWait(DeveloperMainFrameWrapper.getSingleInstance());
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}

		// Add a series of ui elements to the property change listeners so they can
		// receive ui notifications
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getFileExplorerToolbar());
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getTextEditorToolbar());
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getTextEditorPanel());
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getConsolePanel());
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getMenuToolbar());
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getUsersPanel());
		support.addPropertyChangeListener(compiler);
		support.addPropertyChangeListener(this);
		support.addPropertyChangeListener(DeveloperMainFrameWrapper.getInstance());

		// If the user has chosen a workspace we now scan it to search for folders and
		// files
		if (workSpace != null) {
			fileManager.scanWorkSpace(workSpace);
		}
	}

	/**
	 * Used to notify some gui components of the users name when connected to a
	 * session
	 * 
	 * @param newname
	 */
	public void setNewName(String newname) {

		this.chosenName = newname;
		Object[] message = { newname };

		support.notify(ObserverActions.SET_CHOSEN_NAME, message);

	}

	/**
	 * Used to connect to a session by joining it as client
	 * 
	 * @param serverAddress : The address the server is open at
	 * @param ownAddress    : Your own ip address the server will connect back to
	 * @param serverPort    : The port the server is open at
	 * @param chosenName    : The username of this client
	 * @param imageByteData : The data of the profile picture chosen by the user
	 * @param chosenColor   : The Color object representing the color that the user
	 *                      chose
	 */
	public void setAsClient(String serverAddress, String ownAddress, int serverPort, String chosenName,
			String imageByteData, Color chosenColor) {
		support.notify(ObserverActions.DISABLE_RUN_CONFIG, null);
		this.chosenName = chosenName;

		clientHandler = new ClientHandler(chosenName, imageByteData, chosenColor);
		client = new AsynchronousClient(serverAddress, ownAddress, serverPort,  clientHandler, separator);
		if (ownAddress == null) {

			client.setAutomaticIP();

		}
		support.notify(ObserverActions.DISABLE_NEW_PROJECT, null);
		support.notify(ObserverActions.DISABLE_JOIN_BUTTON, null);
		try {

			client.Connect();
			isConnected = true;

		} catch (IOException e) {
			e.printStackTrace();
		}
		support.notify(ObserverActions.ENABLE_DISCONNECT_BUTTON, null);
		DeveloperMainFrameWrapper.getTextEditorPanel().canEdit = false;
		setNewName(chosenName);
	}

	/**
	 * Join a session by creating it as server
	 * 
	 * @param name          : The username of this server
	 * @param ip            : The ip address this server will be open at
	 * @param port          : The port this server will listen at
	 * @param queueSize     : The size of the queue the server uses to process
	 *                      messages
	 * @param imageByteData : The data of the profile picture chosen by the user
	 * @param chosenColor   : The Color object representing the color that the user
	 *                      chose
	 */
	public void setAsServer(String name, String ip, int maxClients, int port, int queueSize, String imageByteData,
			Color chosenColor) {

		waitingResponses = new CountDownLatch(maxClients);
		chosenName = name;

		responses = (LinkedList<ResponseCreateFileMessage>) fileManager.scanAndReturn(workSpace.getPath(),
				workSpace.getName());
		handler = new ServerHandler(name, maxClients, imageByteData, chosenColor);
		if (queueSize == -1) {
			queueSize = defaultQueueSize;
		}

		server = new AsynchronousServer(name, (ServerMessageHandler) handler, maxClients, port, ip, queueSize, separator);
		if (ip == null) {
			server.setAutomaticIP();
		}

		support.notify(ObserverActions.DISABLE_NEW_PROJECT, null);
		support.notify(ObserverActions.DISABLE_JOIN_BUTTON, null);
		boolean success = server.Start();
		if(success) {
		isConnected = true;
		support.notify(ObserverActions.ENABLE_DISCONNECT_BUTTON, null);

		setNewName(name);
		Object[] message = {name};
		support.notify(ObserverActions.HIGHLIGHT_PROFILE_ICONS,message);
		support.notify(ObserverActions.DISABLE_USERS_PANEL, null);
		}

	}

	/**
	 * Calls filemanager method to write on closed files. Used on sync method which
	 * requires writing entire files
	 * 
	 * @param path     : Path of the file to write on
	 * @param contents : Content to write to
	 */
	public void writeOnClosed(String path, String contents) {

		fileManager.writeOnClosed(path, contents);

	}

	/**
	 * Creates a classpath given a project and saves it in the classpaths hashmap
	 * for later
	 * 
	 * @param classes : Array of file paths to add to the classpath
	 * @param project : Project the files belong to
	 */
	public void loadClassPath(String[] classes, String project) {

		ClassPath newclasspath = new ClassPath(project, classes);
		classpaths.put(project, newclasspath);

	}

	/**
	 * Edits the files that comprise the classpath of a given project
	 * 
	 * @param adding   : The files to add to the classpath if any
	 * @param removing : The files to remove from the classpath if any
	 * @param project  : The project these files belong to
	 */
	public void editClassPath(String[] adding, String[] removing, String project) {

		classpaths.get(project).edit(adding, removing);

	}

	/**
	 * Adds a class file into a given project and inmediately adds it to the
	 * classpath
	 * 
	 * @param name    : The new class name
	 * @param path    : The path of the file of this class
	 * @param project : The project this class belongs to
	 * @param isMain  : Indicates if the filemanager should write a main method into
	 *                the class
	 */
	public void createNewClassFile(String name, String path, String project, boolean isMain) {

		// Create the class file in the system
		fileManager.createClassFile(name, path, project, isMain);
		// Compose the path of the file where the filemanager added it
		String adding = path + "\\" + name + ".java";
		// Add the class file to the classpath for its project
		String[] input = { adding };
		classpaths.get(project).edit(input, null);

	}

	/**
	 * Add a project to the current workspace.
	 * 
	 * @param name               : Name of the project to add
	 * @param includeHelpFolders : Tells the filemanager if it should add bin and
	 *                           src folders
	 * @param updateEditor       : Tells the filemanage if it should update the gui
	 *                           for the creation of this project
	 */
	public void createNewProject(String name, boolean includeHelpFolders, boolean updateEditor) {
		fileManager.newProject(name, workSpace, includeHelpFolders, updateEditor);

	}

	/**
	 * Method used by the file navigation gui to open files by calling a method on
	 * the fileManager The filemanager will notify the gui with a message containing
	 * the contents of the file to open
	 *
	 * @param name    : Name of the file to open
	 * @param path    : Path of the file to pen
	 * @param project : Project the file belongs to
	 * @return : not necessary
	 */
	public String openFile(String name, String path, String project) {

		return fileManager.openTextFile(name, path, project);

	}

	/**
	 * Method used to save all of the files that are opened in the editor
	 * 
	 * @param names    : Array containing the paths of the files to save
	 * @param contents : Array containing the contents of the files to be saved
	 * @throws IOException
	 */
	public void saveAllFiles(String[] names, String[] contents) {

		fileManager.saveAllOpen(names, contents);

	}

	/**
	 * Method used to save the contents of a file that is both opened in the editor
	 * and focused by the user
	 * 
	 * @param editorContents : The contents of the file to be saved
	 * @param path           : The path of the file to save
	 * @throws IOException
	 */
	public void saveCurrentFile(String editorContents, String path) throws IOException {

		fileManager.saveCurrentFile(editorContents, path);

	}

	/**
	 * Method used by multiple classes in this program for messages that require to
	 * be broadcasted if the user is acting as server, it behaves differently
	 * depending of the role of the user
	 * 
	 * @param message : A serializable object to be sent as a message
	 */
	public void sendMessageToEveryone(Serializable message) {

		if (server != null) {

			Serializable[] messages = { message };
			try {
				server.broadcastAllMessage(messages);
			} catch (IOException e) {
				e.printStackTrace();
			}

		} else if (client != null) {
			try {
				client.send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Method used to signal a running proccess that it should read input and
	 * continue running Called by the console gui when the user has entered input
	 * into the console
	 * 
	 * @param retrieved : The input the user entered
	 */
	public void reactivateRunningProccess(String retrieved) {

		compiler.reactivateRunningProccess(retrieved);

	}

	/**
	 * Method called by the gui of every text editor tab. If there are unsaved
	 * changes this method will display a warning message asking the user if they
	 * would like to save their progress The method will call the save function if
	 * the user decides to save their progress or cancel the close operation
	 *
	 * @param path           : Path of the closing file
	 * @param unsavedChanges : Flag used to indicate if there are changes left to
	 *                       save
	 * @param contents       : Contents of the edited file
	 */
	public void closeTab(String path, boolean unsavedChanges, String contents) {

		// Show dialog if there are unsaved changes
		if (unsavedChanges) {

			Object[] options = { "Save and close", "Close without saving", "Cancel" };
			int n = JOptionPane.showOptionDialog(this.developerMainFrame,
					"The file at : " + path + " has unsaved changes.", "Unsaved changes",
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			// If the user decides to save , call the save method
			if (n == JOptionPane.OK_OPTION) {

				try {
					this.fileManager.saveCurrentFile(contents, path);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
			// If the user did not choose the cancel option nor the save option , close the
			// tab and loose the changes
			if (n != JOptionPane.CANCEL_OPTION) {

				Object[] message = { path };
				support.notify(ObserverActions.CLOSE_TAB, message);
			}

		} else {
			// If there were no unsaved data , just close the tab

			Object[] message = { path };
			support.notify(ObserverActions.CLOSE_TAB, message);

		}

	}

	/**
	 * Method to close all existing tabs , the calls to this method are always
	 * preceded by a full save of all of the user's progress , there is no function
	 * for the user to access this feature directly.
	 */
	public void closeAllTabs() {

		support.notify(ObserverActions.CLOSE_ALL_TABS, null);

	}

	/**
	 * Method used to delete a classpath , called when the user deletes a file
	 * 
	 * @param path    : The path of the file that is being deleted
	 * @param project : The project the file belonged to
	 */
	public void deleteClassPath(String path, String project) {
		String[] input = { path };
		classpaths.get(project).edit(null, input);

	}

	/**
	 * Method used to delete a file or folder from the file system
	 * 
	 * @param name     : Name of the file to be deleted
	 * @param path     : Path of the file to be deleted
	 * @param isFolder : Flag to tell the fileManager if we are deleting a file or a
	 *                 folder , the deletion process has to behave differently
	 * @param project  : Project the file or folder belonged to
	 * @param node     : Node of the file explorer that represented this element
	 */
	public void deleteFile(String name, String path, boolean isFolder, String project, CustomTreeNode node) {
		fileManager.deleteFile(name, path, isFolder, project, node);

	}

	/**
	 * Method that sets the focused project in order to run code from it
	 * 
	 * @param project : The project to focus
	 */
	public void setProjectFocus(String project) {

		this.focusedProject = project;
	}

	public void setProjectFocus(String name, String project, String path) {
		this.focusedProject = project;
		String retrieveContents = fileManager.openTextFile(name, path, project);
		Object[] message = { path, retrieveContents };
		support.notify(ObserverActions.FULL_SET_TEXT, message);

	}

	/**
	 * Method used to update the entrypoint for a given project
	 * 
	 * @param name : The name of the class that will be the new entrypoint , usually
	 *             a main class
	 */
	public void updateFocusedPair(String name) {

		projectFocusPairs.put(this.focusedProject, name);

	}

	/**
	 * Method used to kill the running proccess , it uses stop to ensure that a
	 * problematic thread will be killed Example : A thread running an infinite loop
	 * is a problematic thread
	 */
	@SuppressWarnings("deprecation")
	public void terminateProcess() {
		if (runningThread != null && runningThread.isAlive()) {
			runningThread.stop();

		}

	}

	/**
	 * Method that calls the full save feature from the filemanager
	 */
	public void saveAllFull() {

		fileManager.saveAllFull();
	}

	/**
	 * Method that triggers the Dialog used to configurate the entrypoint for
	 * running code The dialog won't open unless there is a focused project
	 */
	public void triggerRunConfig(boolean global) {
		if (focusedProject == null || focusedProject.equals("") || !stillExists(focusedProject)) {

			triggerNoProjectDialog();
		} else {
			new RunConfigDialog(this.classpaths.get(focusedProject).getClassPath(), global);
		}
	}

	/**
	 * Method used to trigger the fileManager method that writes folders specifying
	 * that the new folder should be created as a source folder
	 * 
	 * @param path : The path the new folder will be created in
	 * @param name : The name of the new folder
	 */
	public void createSrcFolder(String path, String name) {
		fileManager.writeFolder(path, FILE_TYPE.SRC_FOLDER, true, name, focusedProject);

	}

	/**
	 * Method used to set the users own profile icon
	 * 
	 * @param color     : The Color object representing this user's chosen color
	 * @param imagepath : The path of the image for the icon in the file system
	 * @param name      : The chosen username of this user
	 */
	public void setIcon(Color color, String imagepath, String name) {
		Object[] message = { color, imagepath, name };
		support.notify(ObserverActions.SET_SELF_ICON, message);

	}

	/**
	 * Method used by users acting as clients to send messages to their servers
	 * 
	 * @param message : A serializable object to be sent as a message
	 */
	public void sendMessageToServer(Serializable message) {

		if (this.client != null) {
			try {
				this.client.send(message);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method to disconnect from the current session , behaves differently depending
	 * on the users role
	 */
	public void disconnect() {

		try {
			if (client != null) {
				client.disconnect();
				client = null;
				clientHandler = null;
			} else if (server != null) {
				server.Stop();
				server = null;
				handler = null;
				
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		support.notify(ObserverActions.DISABLE_GLOBAL_RUN, null);
		support.notify(ObserverActions.ENABLE_LOCAL_RUN, null);
		support.notify(ObserverActions.ENABLE_TEXT_EDITOR, null);
		support.notify(ObserverActions.ENABLE_SAVE_BUTTONS, null);
		support.notify(ObserverActions.ENABLE_NEW_PROJECT, null);
		support.notify(ObserverActions.ENABLE_JOIN_BUTTON, null);
		support.notify(ObserverActions.ENABLE_USERS_PANEL,null);
		support.notify(ObserverActions.DISABLE_DISCONNECT_BUTTON, null);
		support.notify(ObserverActions.CLEAR_ALL_ICON, null);
		support.notify(ObserverActions.ALLOW_EDIT_SERVER, null);

	}

	/**
	 * Support method used to get the current worspace name
	 * 
	 * @return
	 */
	public String getWorkSpaceName() {

		return workSpace.getName();

	}

	/**
	 * Method used by users acting as server to send messages to specific clients
	 * 
	 * @param response : A serialiszable object to be sent as a message
	 * @param clientID : The client this message will be sent to
	 */
	public void sendToClient(Serializable response, int clientID) {
		if (server != null) {
			Serializable[] messages = { response };
			int[] clientIDS = { clientID };
			try {
				server.sendMessage(clientIDS, messages);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Method used by users acting as servers to send the client a single message
	 * before sending them workspace data , this messages indicates the name of the
	 * workspace and allows the user to create a folder for the data if needed
	 * 
	 * @param message  : The message to be sent to the client
	 * @param clientID : The client this message will be sent to
	 */
	public void sendSyncToClient(ResponseCreateFileMessage message, int clientID) {

		sendToClient(message, clientID);

		scanAndSend(clientID);
	}

	/**
	 * Method used to create a new workspace by users acting as clients when the
	 * server sends instruction to recreate a workspace when syncing
	 * 
	 * @param path : The path the workspace will be created in
	 */
	public void createWorkSpace(String path) {

		WorkSpace ws = new WorkSpace();
		ws.setName(path);
		ws.setPath(expectedWorkSpaceLocation + "\\" + path);
		File wslocation = new File(ws.getPath());

		if (!wslocation.exists()) {
			wslocation.mkdir();
		}

		WorkSpaceManager wsm = WorkSpaceManager.getInstance();
		wsm.addWorkSpace(ws);
		workSpace = ws;

	}

	/**
	 * Method used to write any kind of folder given the path of the folder and it's
	 * type
	 * 
	 * @param path     : The path for the new folder
	 * @param property : The type of folder
	 */
	public void writeFolder(String path, FILE_TYPE property) {

		String writingname = path.substring(path.lastIndexOf("\\") + 1, path.length());
		String writingpath = workSpace.getPath() + path;
		String firsthalf = writingpath.replace(workSpace.getPath(), "");
		firsthalf = firsthalf.substring(1, firsthalf.length());
		firsthalf = firsthalf.substring(0, firsthalf.indexOf("\\"));
		String projectpath = workSpace.getPath() + FILE_PROPERTIES.doubleSlash + firsthalf;
		fileManager.writeFolder(writingpath, property, false, writingname, projectpath);

	}

	/**
	 * Method used to write any kind of file given it's path , it's content and its
	 * type
	 * 
	 * @param path     : The path where the file will be written
	 * @param contents : The contents of the file
	 * @param type     : The type of file
	 */
	public void writeFile(String path, String contents, FILE_TYPE type) {
		path = workSpace.getPath() + path;
		fileManager.writeFile(path, contents, type);

	}

	/**
	 * Method used by users acting as clients to reload the workspace after syncing
	 * with the server
	 */
	public void reloadWorkSpace() {

		support.notify(ObserverActions.CLEAR_PANEL, null);
		fileManager.scanWorkSpace(workSpace);

	}

	/**
	 * Method used by users acting as servers to prevent more clients from
	 * connecting to the session
	 */
	public void closeServer(int newClients) {
		if (server != null) {
			waitingResponses = new CountDownLatch(newClients);

			server.close();
		}

	}

	/**
	 * Method used to add other user's `profile icon
	 * 
	 * @param image    : The image data representing the profile's icon image
	 * @param color    : The color data representing the profile's icon color
	 * @param name     : The name of the user for this profile icon
	 * @param isServer : Flag to indicate if this is the server's profile icon
	 * @param clientID : Id of the user this icon belongs to
	 */
	public void addProfilePicture(String image, int color, String name, boolean isServer, int clientID) {

		Object[] message = { image, color, name, clientID };

		if (!isServer) {
			support.notify(ObserverActions.SET_CLIENT_ICON, message);
		} else {
			support.notify(ObserverActions.SET_SERVER_ICON, message);

		}
	}

	/**
	 * Method used to remove profile pictures of other users that have disconnected
	 * This is only used if a client has disconnected
	 * 
	 * @param clientID : The user who disconnecte
	 */
	public void removeProfilePicture(int clientID) {

		Object[] message = { clientID };
		support.notify(ObserverActions.REMOVE_CLIENT_ICON, message);
	}

	/**
	 * Manages global runs
	 */
	public void startRunGlobal(boolean countdown) {

		if (focusedProject == null || focusedProject.equals("") || !stillExists(focusedProject)) {
			triggerNoProjectDialog();
		} else {

			if (server != null) {
				support.notify(ObserverActions.DISABLE_GLOBAL_RUN, null);
				DEBUG.debugmessage("Starting running thread");
				runningThread = new Thread(() -> localGlobalRunningThread(countdown));
				runningThread.start();

			}
		}

	}

	/**
	 * Launches the thread used for running code by calling the compileAndRun Method
	 * 
	 * @param popupDialog
	 */
	public void startLocalRunningThread() {
		runningThread = new Thread(() -> compileAndRun(false));
		runningThread.start();
	}

	/**
	 * Used by users acting as clients to request a global run from a server
	 */
	public void requestGlobalRun() {
		clientHandler.blocked = true;
		support.notify(ObserverActions.DISABLE_TEXT_EDITOR, null);
		support.notify(ObserverActions.DISABLE_USERS_PANEL,null);
		GlobalRunRequestMessage message = new GlobalRunRequestMessage(this.chosenName);
		sendMessageToServer(message);
	}

	/**
	 * Support method to check if a file still exists
	 * 
	 * @param name : The name of the file to check
	 * @return true if the file exists
	 */
	public boolean stillExists(String name) {

		File f = new File(name);
		return f.exists();
	}

	/**
	 * Support method used to trigger the warning dialog indicating that there is no
	 * focused project
	 */
	public void triggerNoProjectDialog() {

		JOptionPane.showMessageDialog(this.developerMainFrame,
				"There is no project selected or the project you are trying to run does not exist, choose a tab from a project to run.",
				"Run error", JOptionPane.ERROR_MESSAGE);

	}

	/**
	 * Implementation of the PropertyChangeListener method : propertyChange This
	 * method allows this component to listen for ui notifications
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		ObserverActions action = ObserverActions.valueOf(evt.getPropertyName());

		ArrayList<Object> results = (ArrayList<Object>) evt.getNewValue();
		switch (action) {
		case UPDATE_PANEL_CONTENTS:
			String editingpath = (String) results.get(0);
			String partialPath = workSpace.getPath().substring(0, workSpace.getPath().lastIndexOf("\\"));
			fileManager.updatePanelContents(partialPath + editingpath, results);
			break;
		case DELETE_CLASS_PATH:
			String path = (String) results.get(0);
			String project = (String) results.get(1);
			deleteClassPath(path, project);
			break;
		case DELETE_CLASS_PATH_FOCUSED:
			String pathfocused = (String) results.get(0);
			deleteClassPath(pathfocused, focusedProject);
			break;
		case SAVE_FULL:
			saveAllFull();
			break;
		case SAFETY_STOP:

			if (server != null) {
				handler.shutdownProcessors();
			} else if (client != null) {
				clientHandler.shutdownProcessors();
			}
			disconnect();

			break;
		default:
			break;
		}

	}

	/**
	 * Method used to send messages to other users consoles
	 */
	private void sendConsoleMessages() {
		String message = null;
		boolean loopAround = true;
		while (loopAround) {
			try {
				DEBUG.debugmessage("PROCESSING MESSAGE");
				message = consoleBuffer.take();
				WriteToConsoleMessage out = new WriteToConsoleMessage(message);
				sendMessageToEveryone(out);
				Thread.sleep(50);

			} catch (InterruptedException e) {
				loopAround = false;
				Thread.currentThread().interrupt();
			}

		}

	}

	/**
	 * Starts the running thread set to a global run
	 */
	private void localGlobalRunningThread(boolean countdown) {

		try {
			if (countdown) {
				GlobalRunRequestMessage message = new GlobalRunRequestMessage(chosenName);
				sendMessageToEveryone(message);
				waitingResponses.await();
			}
			compileAndRun(true);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
		}

	}

	/**
	 * Method used by users acting as server to scan their workspace for files and
	 * folders and send special messages that the clients will use to recreate the
	 * server's workspace
	 * 
	 * @param clientID
	 */
	private void scanAndSend(int clientID) {

		if (responses != null) {

			LinkedList<ResponseCreateFileMessage> responseCopy = responses;
			for (ResponseCreateFileMessage message : responseCopy) {
				// send messages to the client one after the other
				this.sendToClient(message, clientID);
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					e.printStackTrace();
				}

			}
		}
		handler.syncComplete();

	}

	/**
	 * Method that calls the compiler to compile the class files and run code If
	 * there is no project focused it will trigger a dialog message If there is no
	 * entrypoint class selected it will trigger the configuration dialog Will
	 * disable and enable run buttons before and after running
	 * 
	 * @param global : Flag to indicate if this is a local or global run
	 * @throws IOException
	 */
	private void compileAndRun(boolean global) {

		support.notify(ObserverActions.SAVE_FULL, null);
		support.notify(ObserverActions.CLEAR_CONSOLE, null);
		// Check if there is no project focused
		if (focusedProject == null || focusedProject.equals("") || !stillExists(focusedProject)) {
			if (!global) {
				triggerNoProjectDialog();
			}
		} else {

			// Get the classpath for the focused project
			URLData[] files = classpaths.get(focusedProject).getClassPath();
			// Try to get an entrypoint class
			String focusedClassName = projectFocusPairs.get(focusedProject);
			// If there is an entrypoint class configured
			if (focusedClassName != null && !focusedClassName.equals("") && stillExists(focusedClassName)) {
				// Disable run buttons
				if (!global) {
					support.notify(ObserverActions.DISABLE_SAVE_BUTTONS, null);
					support.notify(ObserverActions.DISABLE_LOCAL_RUN, null);
				}
				// Get the name of the focused class
				String subname = focusedClassName.substring(focusedClassName.lastIndexOf("\\") + 1,
						focusedClassName.length());
				// Call the compile function with the files from the classpath and the name of
				// the entrypoint class
				consoleSender = new Thread(() -> sendConsoleMessages());
				consoleSender.start();
				compile(files, subname);
				// After the code has run enable run buttons
				if (!global) {
					support.notify(ObserverActions.ENABLE_LOCAL_RUN, null);
				} else {
					waitingResponses = new CountDownLatch(handler.nClients);
					GlobalRunRequestResponse finished = new GlobalRunRequestResponse();
					finished.ok = true;
					sendMessageToEveryone(finished);
					handler.running = false;
					support.notify(ObserverActions.ENABLE_GLOBAL_RUN, null);
					GlobalRunDone message = new GlobalRunDone();
					sendMessageToEveryone(message);

				}

				consoleSender.interrupt();
				support.notify(ObserverActions.ENABLE_SAVE_BUTTONS, null);
				support.notify(ObserverActions.ENABLE_TEXT_EDITOR, null);
				support.notify(ObserverActions.ENABLE_USERS_PANEL, null);

				// Disable the terminate button , this button is enabled by the compiler when
				// terminating the
				// running process is safe
				support.notify(ObserverActions.DISABLE_TERMINATE, null);

			} else {

				// If there is no entrypoint class selected the run configuration dialog is
				// called

				new RunConfigDialog(files, global);

			}
		}

	}

	/**
	 * Method used to call the compilers compile method
	 * 
	 * @param files     : The files to be compiled
	 * @param className : The name of the entrypoint class
	 * @param global
	 * @return
	 */
	private void compile(URLData[] files, String className) {

		compiler.run(className, files);

	}
	/**
	 * Directly sets contents in memory
	 * @param path
	 * @param contents
	 */
	public void setInternalContent( String path, String contents) {

			fileManager.overrideOnClose(path,contents);
	}

}
