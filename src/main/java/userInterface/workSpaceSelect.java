package userInterface;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatDarkLaf;

import fileManagement.WorkSpace;
import fileManagement.WorkSpaceManager;
import fileManagement.customWorkSpaceElement;

public class workSpaceSelect extends JFrame {

	private JPanel contentPane;
	LinkedList<Component> selectPanelComponents;
	private JLabel lblNewLabel;
	private JButton newWorkSpaceButton;
	private JPanel panel;
	private JButton btnNewButton_1;
	private Color frameDark = new Color(0, 0, 0);

	/**
	 * Launch the application.
	 */

	/**
	 * Create the frame.
	 */

	private void readAndGenerate() {
		ArrayList<WorkSpace> ws = (ArrayList<WorkSpace>) WorkSpaceManager.getAllWorkSpaces();
		if (ws != null) {
			for (WorkSpace workspace : ws) {
				customWorkSpaceElement cwse = new customWorkSpaceElement(workspace.getName(), workspace.getPath());
				cwse.setAlignmentX(0.5f);
				panel.add(cwse);

			}

		}

	}

	public workSpaceSelect() {
		super("PairLeap");
		try {
			UIManager.setLookAndFeel(new FlatDarkLaf());
		} catch (Exception ex) {
			System.err.println("Failed to initialize LaF");
		}
//aaaa�

		selectPanelComponents = new LinkedList<Component>();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 197, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 21, 0, 0, 0 };
		gbl_contentPane.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		lblNewLabel = new JLabel("LAUNCH");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.anchor = GridBagConstraints.WEST;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPane.add(lblNewLabel, gbc_lblNewLabel);

		btnNewButton_1 = new JButton("Add existing WorkSpace");
		GridBagConstraints gbc_btnNewButton_1 = new GridBagConstraints();
		gbc_btnNewButton_1.anchor = GridBagConstraints.WEST;
		gbc_btnNewButton_1.insets = new Insets(0, 0, 5, 0);
		gbc_btnNewButton_1.gridx = 0;
		gbc_btnNewButton_1.gridy = 1;
		contentPane.add(btnNewButton_1, gbc_btnNewButton_1);

		newWorkSpaceButton = new JButton("New WorkSpace");
		newWorkSpaceButton.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbc_newWorkSpaceButton = new GridBagConstraints();
		gbc_newWorkSpaceButton.anchor = GridBagConstraints.WEST;
		gbc_newWorkSpaceButton.insets = new Insets(0, 0, 5, 0);
		gbc_newWorkSpaceButton.gridx = 0;
		gbc_newWorkSpaceButton.gridy = 2;
		contentPane.add(newWorkSpaceButton, gbc_newWorkSpaceButton);

		panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.anchor = GridBagConstraints.WEST;
		gbc_panel.fill = GridBagConstraints.VERTICAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 3;
		contentPane.add(panel, gbc_panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		readAndGenerate();

		setSize(600, 600);
		setResizable(false);

		newWorkSpaceButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				AddWorkSpaceDialog d = new AddWorkSpaceDialog();

			}

		});

		this.setVisible(true);

	}
}
