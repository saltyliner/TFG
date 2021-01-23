package userInterface.fileNavigation;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import javax.swing.tree.DefaultMutableTreeNode;
import fileManagement.FILE_PROPERTIES;
import fileManagement.FILE_TYPE;

/**
 * Nodes of the trees that serve as file navigation for the user 
 * @author Carmen G�mez Moreno
 *
 */
@SuppressWarnings({ "unused", "serial" })
public class CustomTreeNode extends DefaultMutableTreeNode   {
	
	
	public String path;
	public String name;
	public String project; 
	public boolean isFile;
	public boolean isProject;
	public boolean hideContents; 
	
	
	
	private static String folderPath =  "Icons/warning_icon.png";
	private String packagePath = "Icons/warning_icon.png";
	private String filePath = "Icons/warning_icon.png";

	
	
	/**
	 * 
	 * @param path : The path of the element this node represents
	 * @param name : The name of this node
	 * @param project : The project this node belongs to 
	 * @param isFile : Flag indicating if this node represents a file
	 * @param isProject : Flag indicating if this node represents a project 
	 */
	public CustomTreeNode(String path, String name , String project , boolean isFile , boolean isProject ) {
		
		
		
		super();
		hideContents = false; 
		this.isProject = isProject; 
		this.isFile = isFile; 
		this.path = path;
		this.name = name; 
		this.project = project; 
		
		getMetaData(path);
		

	}
	
	/**
	 * 
	 * @return the path of this node's parent node
	 */
	public String getParentPath() {
		
		int last = path.lastIndexOf("\\");
		String parentpath = path.substring(0,last);
		
		return parentpath; 
	}	
	
	
	/**
	 * Support method that returns this node metadata if this is a folder
	 * @param path : TODO  what is this??
	 */
	private void getMetaData(String path) {
		
		FILE_TYPE returning = null; 
		
		final Path file = Paths.get(path);
		final UserDefinedFileAttributeView view = Files.getFileAttributeView(file,
				UserDefinedFileAttributeView.class);
		ByteBuffer readBuffer = null;
		boolean success = false;
		int count = 0;
		while(count < FILE_PROPERTIES.properties.length && !success) {
			

			try {
				readBuffer = ByteBuffer.allocate(view.size(FILE_PROPERTIES.properties[count]));
			} catch (IOException e) {
			}
			try {
				view.read(FILE_PROPERTIES.properties[count], readBuffer);
			} catch (IOException e) {
			}
		
			try {
				readBuffer.flip();
				final String valueFromAttributes = new String(readBuffer.array(), "UTF-8");
				if (valueFromAttributes.equals(FILE_PROPERTIES.properties[count])) {
					switch (FILE_PROPERTIES.properties[count]) {
						
					case FILE_PROPERTIES.projectProperty:
						hideContents = false; 
						break;
					case FILE_PROPERTIES.srcProperty:
						hideContents = false;
						break;
					case FILE_PROPERTIES.binProperty:
						hideContents = true; 
						break;
					default:
						break;
					
					
					}
					success = true; 
				}else {
					count++;
				}
			} catch (Exception e) {
				count++;
			}
					
		}	
	}
	
	
	

	
	
}
