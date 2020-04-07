
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class JoinSessionPanel extends JPanel {

	JLabel contraseña;
	JTextField campoContraseña;
	JButton botonOk;
	JLabel ip;
	JLabel nombre;
	JTextField campoNombre;
	JTextField campoIp;

	JPanel areaContraseña;
	JPanel areaNombre;
	JPanel areaIP;
	JFrame frame;

	public JoinSessionPanel(JFrame f) {

		// Añade todos los elementos necesarios para unirse a una sesión
		frame = f;
		setLayout(new BorderLayout());

		setSize(new Dimension(600, 600));

		ip = new JLabel("Input your host IP");
		campoIp = new JTextField(25);

		contraseña = new JLabel("Input your host Password ");
		campoContraseña = new JTextField(25);

		JLabel nombre = new JLabel("Choose your name");
		campoNombre = new JTextField(25);

		JButton botonOk = new JButton("OK");

		botonOk.addActionListener(new ActionListener() {

			// Si los campos con la información necesaria para unirse a una sesión no se han
			// dejado en
			// blanco llama al método para invocar una nueva ventana

			@Override
			public void actionPerformed(ActionEvent e) {
				if (!campoNombre.getText().equals("") && !campoContraseña.getText().equals("")
						&& !campoIp.getText().equals("")) {
					initMainFrame(campoNombre.getText(), campoIp.getText());
				}
				setVisible(false);
			}

			private void initMainFrame(String nombre, String ad) {

				// Método que invoca en un nuevo hilo la nueva ventana, crea un Socket
				// y lo conecta a la ip dada

				SwingUtilities.invokeLater(new Runnable() {

					String n = nombre;
					String address = ad;

					@Override
					public void run() {
						Socket socket;
						try {
							socket = new Socket(address, 8080);

						} catch (IOException e) {
							socket = null;
							e.printStackTrace();
						}

						DeveloperMainFrame frame = new DeveloperMainFrame(socket, null);

						frame.setSessionOwner(n);

					}

				});
			}
		});

		areaIP = new JPanel();
		areaContraseña = new JPanel();
		areaNombre = new JPanel();
		areaContraseña.setLayout(new FlowLayout(FlowLayout.CENTER));
		areaNombre.setLayout(new FlowLayout(FlowLayout.CENTER));
		areaIP.setLayout(new FlowLayout(FlowLayout.CENTER));

		areaIP.add(ip);
		areaIP.add(campoIp);

		areaContraseña.add(contraseña);
		areaContraseña.add(campoContraseña);
		areaContraseña.add(botonOk);

		areaNombre.add(nombre);
		areaNombre.add(campoNombre);

		add(areaNombre, BorderLayout.NORTH);
		add(areaIP, BorderLayout.CENTER);
		add(areaContraseña, BorderLayout.SOUTH);

	}
}
