package com.dotcms.osgi.ecards;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.dotcms.osgi.ecards.viewtools.ECardsToolInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * Initialize the eCards OSGI plugin
 * Created by Oswaldo Gallango
 * Date: 1/22/13
 */
public class Activator extends GenericBundleActivator {

	private final String BACKEND_FOLDERS_AND_FILES = "/dotcms";
	private final String SEARCH_FOLDER_NAME = "dotcms";
	public  final String ECARD_BASE_FOLDER = "/ecards";

	public void start ( BundleContext context ) throws Exception {
		ECards.createForm();   

		Bundle yourBundle = context.getBundle();
		Enumeration<String> paths = yourBundle.getEntryPaths(BACKEND_FOLDERS_AND_FILES);
		if(UtilMethods.isSet(paths)){
			while(paths.hasMoreElements()){
				String path = paths.nextElement();
				Logger.debug(this, "ECARDS PROCESSING PATH: "+path);
				if(path.endsWith("/")){
					String backendFolder = path.replaceFirst(SEARCH_FOLDER_NAME, ECARD_BASE_FOLDER);
					ECards.createFolder(backendFolder);
					showSubFolders(yourBundle, path);
				}else if(UtilMethods.isImage(path) || isFile(path)){
					addFile(yourBundle, path);
				}
			}
		}


		//Initializing services...
		initializeServices( context );

		//Registering the ViewTool service
		registerViewToolService( context, new ECardsToolInfo() );

		//Adding quartz email process
		try {        	      	
			CronScheduledTask cronScheduledTask = ECards.generateCronScheduledTask();  
			//ECards.scheduleTask(cronScheduledTask);
			scheduleQuartzJob( cronScheduledTask );        	
		}catch(Exception e){
			Logger.error(Activator.class, e.getMessage(), e);          	
		}

	}

	public void stop ( BundleContext context ) throws Exception {
		//Unregistering the ViewTool and quartz jobs
		unregisterServices(context);
	}

	/**
	 * Validate if the file is a css, js or vtl file
	 * @param x Name or path of the file
	 * @return true if is an authorized type of file, else return false
	 */
	private boolean isFile(String x) {
		if (x == null)
			return false;
		return (x.toLowerCase().endsWith(".css") || x.toLowerCase().endsWith(".js") || x.toLowerCase().endsWith(".vtl"));
	}

	/**
	 * Generate the folder(s) and sub folder(s) in dotCMS back end
	 * @param bundle eCards bundle
	 * @param path Path of the parent folder
	 * @throws Exception 
	 */
	private void showSubFolders(Bundle bundle, String path) throws Exception{
		Enumeration<String> subpaths = bundle.getEntryPaths(path);
		if(UtilMethods.isSet(subpaths)){
			while(subpaths.hasMoreElements()){

				String subpath = subpaths.nextElement();
				Logger.debug(this, "ECARDS PROCESSING SUBPATH: "+subpath);

				if(subpath.endsWith("/")){
					String backendFolder = subpath.replaceFirst(SEARCH_FOLDER_NAME, ECARD_BASE_FOLDER);
					ECards.createFolder(backendFolder);
					showSubFolders(bundle,  subpath);
				}else if(UtilMethods.isImage(subpath) || isFile(subpath)){
					addFile(bundle,  subpath);					
				}
			}
		}		
	}

	/**
	 * Add the files as files assets in the backend
	 * @param bundle OSGI eCArds bundle
	 * @param path File path
	 * @throws Exception 
	 */
	private void addFile(Bundle bundle, String path) throws Exception{

		URL resource = bundle.getResource(path);
		String filename = path.substring(path.lastIndexOf("/")+1); 
		String folderPath = path.replaceFirst("/"+filename,"");
		folderPath = folderPath.replaceFirst(SEARCH_FOLDER_NAME, ECARD_BASE_FOLDER);
		InputStream inputStream = resource.openStream();

		User systemUser = APILocator.getUserAPI().getSystemUser();
		File tempUserFolder = new File(APILocator.getFileAPI().getRealAssetPathTmpBinary() + java.io.File.separator + systemUser.getUserId());
		if (!tempUserFolder.exists()){
			tempUserFolder.mkdirs();
		}

		File dest = new File(tempUserFolder.getAbsolutePath() + File.separator + filename);

		OutputStream out = new FileOutputStream(dest);

		int read = 0;
		byte[] bytes = new byte[1024];

		while ((read = inputStream.read(bytes)) != -1) {
			out.write(bytes, 0, read);
		}

		inputStream.close();
		out.flush();
		out.close();

		if(!ECards.existFileAsset(dest, folderPath, filename)){
			ECards.saveFile(dest, folderPath, filename);
		}
	}
}