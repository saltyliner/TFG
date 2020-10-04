package fileManagement;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

public class WorkSpaceManager {

	public static void addWorkSpace(WorkSpace workspace) {

		File workspacefile = new File("src/main/resources/WorkSpaces.xml");
		List<WorkSpace> ws = null;
		try {
			ws = getAllWorkSpaces();
		} catch (Exception e) {
		}
		if (ws == null) {

			ws = new ArrayList<WorkSpace>();
		}
		ws.add(workspace);
		WorkSpaces newws = new WorkSpaces();
		newws.setWorkSpaces(ws);

		JAXBContext jaxbContext = null;
		try {
			jaxbContext = JAXBContext.newInstance(WorkSpaces.class);
		} catch (JAXBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Marshaller jaxbMarshaller = null;
		try {
			jaxbMarshaller = jaxbContext.createMarshaller();
		} catch (JAXBException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		} catch (PropertyException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Marshal the employees list in file
		try {
			jaxbMarshaller.marshal(newws, workspacefile);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static List<WorkSpace> getAllWorkSpaces() {

		Unmarshaller jaxbUnmarshaller = null;
		JAXBContext jaxbContext = null;
		WorkSpaces ws = null;
		try {
			jaxbContext = JAXBContext.newInstance(WorkSpaces.class);
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			ws = (WorkSpaces) jaxbUnmarshaller.unmarshal(new File("src/main/resources/WorkSpaces.xml"));
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			return ws.getWorkSpaces();
		} catch (Exception e) {
			return null;
		}

	}

}