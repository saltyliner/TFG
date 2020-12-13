package userInterface;

import java.beans.PropertyChangeListener;

import core.DEBUG;
import userInterface.fileNavigation.FileExplorerToolbar;
import userInterface.textEditing.TextEditorPanel;
import userInterface.textEditing.TextEditorContainer;
import userInterface.fileNavigation.FileExplorerPanel;

public class DeveloperMainFrameWrapper implements Runnable {

	private static DeveloperMainFrame instance = null;
	private static DeveloperMainFrameWrapper singleInstance = null;

	public static FileExplorerToolbar getFileExplorerToolbar() {
		DEBUG.debugmessage(" por aqui si pasa aaaaa");

		return instance.fileExplorerToolbar;
	}

	public static MenuToolbar getMenuToolbar() {
		return instance.menuToolbar;
	}

	public static TextEditorPanel getTextEditorPanel() {
		DEBUG.debugmessage(" por aqui si pasa bbbbb");
		return instance.textEditorContainer.textEditorPanel;
	}

	public static TextEditorContainer getTextEditorToolbar() {
		return instance.textEditorContainer;
	}

	public static ConsolePanel getConsolePanel() {
		DEBUG.debugmessage("ME cago n la leche");
		return instance.textEditorContainer.consolePanel;
	}

	public static FileExplorerPanel getFileExplorerPanel() {
		DEBUG.debugmessage(" por aqui si pasa ccc");

		return instance.fileExplorerToolbar.fileExplorerPanel;
	}

	public static DeveloperMainFrameWrapper getSingleInstance() {
		if (singleInstance == null) {
			singleInstance = new DeveloperMainFrameWrapper();
		}
		return singleInstance;
	}

	private DeveloperMainFrameWrapper() {
		
	}

	@Override
	public void run() {
		DEBUG.debugmessage("RUN BITCH RUN");
		if (instance == null) {
			instance = new DeveloperMainFrame();
		}

	}

	public static DeveloperMainFrame getInstance() {
		return instance;
	}

	public static PropertyChangeListener getUsersPanel() {
		
		return instance.usersPanel;
	}

}
