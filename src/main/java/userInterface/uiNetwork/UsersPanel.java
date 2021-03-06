package userInterface.uiNetwork;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import commandController.CommandController;
import networkMessages.controlPassMessage;
import observerController.ObserverActions;
import observerController.PropertyChangeMessenger;
import userInterface.uiGeneral.DeveloperMainFrameWrapper;

/**
 * Ui class that contains functionality to join and disconnect from sessions as
 * well as a list rendering the users connected a session represented with
 * profile icons
 * 
 * @author Carmen G�mez Moreno
 *
 */
@SuppressWarnings("serial")
public class UsersPanel extends JPanel implements PropertyChangeListener {

	JLabel sessionowner;
	JLabel ipIndicator;
	JButton joinSessionButton;
	JButton disconnectButton;
	JPanel userIconsPanel;
	ProfileIIconComponent self;
	ProfileIIconComponent server;
	LinkedList<ProfileIIconComponent> icons;
	private JButton createSessionButton;
	private PropertyChangeMessenger support;

	public UsersPanel() {

		icons = new LinkedList<ProfileIIconComponent>();
		setLayout(new BorderLayout(0, 0));

		support = PropertyChangeMessenger.getInstance();
		JPanel panel = new JPanel();
		add(panel, BorderLayout.NORTH);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JToolBar toolBar = new JToolBar();
		panel.add(toolBar);

		joinSessionButton = new JButton("Join");
		joinSessionButton
				.setIcon(new ImageIcon(UsersPanel.class.getResource("/resources/images/joinSession_Icon.png")));
		joinSessionButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				new JoinSessionDialog();
			}

		});

		toolBar.add(joinSessionButton);

		createSessionButton = new JButton("Create");
		createSessionButton
				.setIcon(new ImageIcon(UsersPanel.class.getResource("/resources/images/createSession_Icon.png")));
		toolBar.add(createSessionButton);
		createSessionButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (CommandController.developerComponent.workSpace == null) {
					JOptionPane.showMessageDialog(DeveloperMainFrameWrapper.getInstance(),
							"You are trying to create a session without a workspace loaded , restart this software and choose a workspace to create"
									+ "a session.",
							"No Workspace error", JOptionPane.ERROR_MESSAGE);
				} else {
					new CreateSessionDialog();

				}
			}

		});

		disconnectButton = new JButton("Disconnect");
		disconnectButton.setIcon(new ImageIcon(UsersPanel.class.getResource("/resources/images/disconnect_Icon.png")));

		toolBar.add(disconnectButton);
		disconnectButton.setEnabled(false);
		disconnectButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				CommandController.developerComponent.disconnect();
			}

		});

		userIconsPanel = new JPanel();
		add(userIconsPanel, BorderLayout.WEST);
		userIconsPanel.setLayout(new BoxLayout(userIconsPanel, BoxLayout.Y_AXIS));
		

	}

	/**
	 * Implementation of propertyChange from PropertyChangeListener for this class
	 * to listen to ui notifications
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		ObserverActions action = ObserverActions.valueOf(evt.getPropertyName());
		@SuppressWarnings("unchecked")
		ArrayList<Object> result = (ArrayList<Object>) evt.getNewValue();

		switch (action) {
		case CLEAR_ALL_ICON:
			
			this.icons.clear();
			userIconsPanel.removeAll();
			userIconsPanel.updateUI();
			break;

		case SET_SELF_ICON:
			setSelf((Color) result.get(0), (String) result.get(1), (String) result.get(2));
			rearange();
			break;
		case SET_SERVER_ICON:
			setServer((String) result.get(0), (int) result.get(1), (String) result.get(2));
			rearange();

			break;
		case SET_CLIENT_ICON:

			setClient((String) result.get(0), (int) result.get(1), (String) result.get(2), (int) result.get(3));
			rearange();
			break;
		case REMOVE_CLIENT_ICON:
			removeClient((int) result.get(0));
			rearange();
			break;

		case DISABLE_JOIN_BUTTON:
			joinSessionButton.setEnabled(false);
			createSessionButton.setEnabled(false);
			break;
		case ENABLE_JOIN_BUTTON:
			joinSessionButton.setEnabled(true);
			createSessionButton.setEnabled(true);
			break;
		case ENABLE_DISCONNECT_BUTTON:
			disconnectButton.setEnabled(true);
			break;
		case DISABLE_DISCONNECT_BUTTON:
			disconnectButton.setEnabled(false);
			updateUI(); 
			break;
		case HIGHLIGHT_PROFILE_ICONS:

			this.cleanSelection((String) result.get(0));
			
			break;
		case ENABLE_USERS_PANEL:
			System.out.println("Enabling panel");
			toggleAll(this,true);
			break;
		
		case DISABLE_USERS_PANEL:
			System.out.println("Disabling panel");
			toggleAll(this,false);
			break;

		default:

			break;

		}

	}

	private void toggleAll(JPanel panel, boolean toggle) {
	
		for(ProfileIIconComponent icon : this.icons) {
			icon.canSelect = toggle;
		}
	}
	private void removeClient(int clientID) {
		Iterator<ProfileIIconComponent> i = icons.iterator();
		int index = -1;
		int count = 0;
		boolean found = false;
		while (i.hasNext() && !found) {
			ProfileIIconComponent next = i.next();
			if (next.clientID == clientID) {
				index = count;
				found = true;
			} else {
				count++;
			}
		}
		if (index != -1) {
			icons.remove(index);

			rearange();
		}

	}

	private void setSelf(Color color, String image, String name) {
		self = new ProfileIIconComponent(image, color, name, false, this);
		icons.addFirst(self);
	}

	private void setServer(String string, int color, String name) {
		server = new ProfileIIconComponent(color, string, name, -1, this);
		icons.addFirst(server);

	}

	private void setClient(String string, int color, String name, int clientID) {
		ProfileIIconComponent client = new ProfileIIconComponent(color, string, name, clientID, this);
		icons.addLast(client);

	}

	private void reHighlight(int id) {

		for (int i = 0; i < icons.size(); i++) {

			icons.get(i).setBackground(null);

		}

	}

	private void rearange() {

		this.userIconsPanel.removeAll();
		for (ProfileIIconComponent c : icons) {

			c.setMaximumSize(c.getPreferredSize());
			c.setMaximumSize(c.getPreferredSize());
			c.setMaximumSize(c.getPreferredSize());
			c.setAlignmentX(0.0f);
			userIconsPanel.add(c);

		}
		userIconsPanel.updateUI();

	}

	public void cleanSelection(String name) {
	
		for (ProfileIIconComponent icon : icons) {

			if (icon.chosenName.equals(name)) {
				icon.setHighlight();
			} else {

				icon.setOpaque(false);
				icon.setBorder(new EmptyBorder(0, 0, 0, 0));
			}
		}
		if (CommandController.developerComponent.server != null) {
			if (CommandController.developerComponent.chosenName.equals(name)) {
				support.notify(ObserverActions.ALLOW_EDIT_SERVER, null);
			} else {
				support.notify(ObserverActions.DISABLE_EDIT_SERVER, null);

			}

			controlPassMessage message = new controlPassMessage(name);
			CommandController.runOnThread(() -> CommandController.developerComponent.sendMessageToEveryone(message));
		}

	}

}
