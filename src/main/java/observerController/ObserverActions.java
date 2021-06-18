package observerController;
/**
 * Enum representing all of the different types of ui notifications , used by the component that listen to them 
 * to decide if they should process the notification and the way they should process them
 * This allows different components to listen to the same notification and implement their responses differently 
 * Almost all of the user interface functionality can be seen in this enum
 * @author Carmen G�mez Moreno
 *
 */
public enum ObserverActions {

	CONSOLE_PANEL_CONTENTS, ENABLE_TEXT_EDITOR, UPDATE_CONTENTS, ENABLE_TEXTEDITORTOOLBAR_BUTTONS, SET_TEXT_CONTENT,
	ENABLE_SAVE_BUTTONS, UPDATE_FILE_EXPLORER_PANEL_BUTTONS, UPDATE_PANEL_CONTENTS, ENABLE_CONSOLE_PANEL,
	ENABLE_READING_LISTENER,ADD_PROJECT_TREE, CLOSE_TAB, SAVED_SINGLE_FILE, UPDATE_PROJECT_TREE_ADD, UPDATE_PROJECT_TREE_REMOVE, 
	CHANGE_TAB_FOCUS, ENABLE_LOCAL_RUN, DISABLE_LOCAL_RUN, ENABLE_TERMINATE, DISABLE_TERMINATE, DISABLE_GLOBAL_RUN, ENABLE_GLOBAL_RUN,
	SAFETY_SAVE, SAFETY_DELETE, SAFETY_STOP,SET_SAVE_FLAG_TRUE, SET_SAVE_FLAG_FALSE, SAVE_FULL, DELETE_PROJECT_TREE, DELETE_CLASS_PATH, 
	DELETE_CLASS_PATH_FOCUSED, DISABLE_NEW_PROJECT, ENABLE_NEW_PROJECT, SET_SELF_ICON, SET_SERVER_ICON, SET_CLIENT_ICON, 
	REMOVE_CLIENT_ICON, DISABLE_JOIN_BUTTON, ENABLE_JOIN_BUTTON, ENABLE_DISCONNECT_BUTTON, DISABLE_DISCONNECT_BUTTON, CLEAR_PANEL,
	SET_CHOSEN_NAME, CLOSE_ALL_TABS, UPDATE_HIGHLIGHT, DISABLE_TEXT_EDITOR, DISABLE_SAVE_BUTTONS, DISABLE_RUN_CONFIG, CLEAR_CONSOLE, CLOSE_SINGLE_TAB,
	FULL_SET_TEXT, CLEAR_ALL_ICON, ALLOW_EDIT_SERVER, HIGHLIGHT_PROFILE_ICONS, DISABLE_EDIT_SERVER, DISABLE_USERS_PANEL, ENABLE_USERS_PANEL

}