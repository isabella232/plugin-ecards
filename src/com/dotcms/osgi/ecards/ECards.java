package com.dotcms.osgi.ecards;

import java.util.Date;
import java.util.HashMap;

import org.quartz.CronTrigger;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.FieldsCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.business.FieldAPI;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.quartz.CronScheduledTask;
import com.dotmarketing.quartz.QuartzUtils;
import com.dotmarketing.util.CompanyUtils;
import com.dotmarketing.util.Mailer;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import com.liferay.util.FileUtil;
//import com.dotmarketing.portlets.form.business.FormAPI;



/**
 * This class manage the creation of the eCards form, folders and the inclusion of files and process 
 * required by this plugin to print/send/request ecards
 * @author Oswaldo Gallango
 *
 */
public class ECards {

	/**
	 * set this variable if you are using a host port different from 80
	 */
	public static final String HOST_PORT="";

	/**
	 * eCards Form Fields
	 */
	public static final String STRUCTURE_FORM_VAR_NAME = "eCards";
	public static final String STRUCTURE_FORM_NAME = "eCards";
	public static final String STRUCTURE_FORM_DESCRIPTION = "eCards Forms Structure";

	public static final String FORM_NAME_FIELD_VAR_NAME = "name";
	public static final String FORM_NAME_FIELD = "Your Name";
	public static final String FORM_NAME_FIELD_HINT = "";

	public static final String FORM_EMAIL_FIELD_VAR_NAME = "senderEmail";
	public static final String FORM_EMAIL_FIELD = "Your Email";
	public static final String FORM_EMAIL_FIELD_HINT = "";

	public static final String FORM_RECIPIENT_FIELD_VAR_NAME = "recipient";
	public static final String FORM_RECIPIENT_FIELD = "Recipient Name";
	public static final String FORM_RECIPIENT_FIELD_HINT = "";

	public static final String FORM_DELIVERY_FIELD_VAR_NAME = "deliveryMethod";
	public static final String FORM_DELIVERY_FIELD = "Delivery Method";
	public static final String FORM_DELIVERY_FIELD_HINT = "";
	public static final String FORM_DELIVERY_FIELD_VALUES = "Send to Room|Send to Room\r\nSend to Email|Send to Email";

	public static final String FORM_DELIVERY_ROOM_FIELD_VAR_NAME = "deliveryLocation";
	public static final String FORM_DELIVERY_ROOM_FIELD = "Delivery Location";
	public static final String FORM_DELIVERY_ROOM_FIELD_HINT = "";
	public static final String FORM_DELIVERY_ROOM_FIELD_DEFAULT_VALUE="NOTAVAILABLE";

	public static final String FORM_DELIVERY_EMAIL_FIELD_VAR_NAME = "deliveryEmail";
	public static final String FORM_DELIVERY_EMAIL_FIELD = "Delivery Email";
	public static final String FORM_DELIVERY_EMAIL_FIELD_HINT = "";
	public static final String FORM_DELIVERY_EMAIL_FIELD_DEFAULT_VALUE="NOTAVAILABLE";

	public static final String FORM_STATUS_FIELD_VAR_NAME = "status";
	public static final String FORM_STATUS_FIELD = "Status";
	public static final String FORM_STATUS_FIELD_HINT = "";
	public static final String FORM_STATUS_FIELD_VALUES = "Pending|pending\r\nExecuted|executed\r\nCancelled|cancelled\r\nError|error";
	public static final String FORM_STATUS_FIELD_DEFAULT_VALUE ="pending";

	public static final String FORM_IMAGE_TYPE_FIELD_VAR_NAME = "imageType";
	public static final String FORM_IMAGE_TYPE_FIELD = "Select type";
	public static final String FORM_IMAGE_TYPE_FIELD_HINT = "";
	public static final String FORM_IMAGE_TYPE_FIELD_VALUES = "Birthday|birthday\r\nGet Well|get_well\r\nCongratulations|congratulations\r\nLove - Valentine day|valentine\r\nHoliday|holiday\r\nNew Baby|new_baby";
	public static final String FORM_IMAGE_TYPE_FIELD_DEFAULT_VALUE ="birthday";

	public static final String FORM_BACKGROUND_FIELD_VAR_NAME = "image";
	public static final String FORM_BACKGROUND_FIELD = "Image";
	public static final String FORM_BACKGROUND_FIELD_HINT = "";

	public static final String FORM_MESSAGE_FIELD_VAR_NAME = "message";
	public static final String FORM_MESSAGE_FIELD = "Message";
	public static final String FORM_MESSAGE_FIELD_HINT = "Write your message";

	public static final String FORM_JAVASCRIPTS_FIELD_VAR_NAME = "scripts";
	public static final String FORM_JAVASCRIPTS_FIELD = "Options";
	public static final String FORM_JAVASCRIPTS_FIELD_HINT = "";
	public static final String FORM_JAVASCRIPTS_FIELD_VALUES = "";

	public static final String FORM_ERROR_FIELD_VAR_NAME = "error";
	public static final String FORM_ERROR_FIELD = "Delivery problem";
	public static final String FORM_ERROR_FIELD_HINT = "";
	public static final String FORM_ERROR_FIELD_VALUES = "";

	public static final String FORM_HOST_FIELD_NAME = "Form Host";
	public static final String FORM_HOST_FIELD_VAR_NAME = "formHost";
	public static final String FORM_TITLE_FIELD_NAME = "Form Title";
	public static final String FORM_TITLE_FIELD_VAR_NAME = "formTitle";
	public static final String FORM_EMAIL_FIELD_NAME = "Form Email";
	public static final String FORM_EMAIL_FIELD_VARNAME = "formEmail";
	public static final String FORM_RETURN_PAGE_FIELD_NAME = "Form Return Page";
	public static final String FORM_RETURN_PAGE_FIELD_VAR_NAME = "formReturnPage";

	/**
	 * Role
	 */
	public static final String ECARD_ADMIN_ROLE_NAME = "eCards Admin";
	public static final String ECARD_ADMIN_ROLE_KEY= "eCard";
	public static final String ECARD_ADMIN_ROLE_DESCRIPTION = "This role receive email notification of ecards to print";

	/**
	 * Folders
	 */
	public static final String ECARD_VELOCITY_FOLDER = "/ecards/velocity";

	public static final String ECARD_GALLERY_BIRTHDAY_FOLDER = "/ecards/gallery/birthday";
	public static final String ECARD_GALLERY_GET_WELL_FOLDER = "/ecards/gallery/get_well";
	public static final String ECARD_GALLERY_CONGRATULATIONS_FOLDER = "/ecards/gallery/congratulations";
	public static final String ECARD_GALLERY_VALENTINE_FOLDER = "/ecards/gallery/valentine";
	public static final String ECARD_GALLERY_HOLIDAY_FOLDER = "/ecards/gallery/holiday";
	public static final String ECARD_GALLERY_NEW_BABY_FOLDER = "/ecards/gallery/new_baby";

	/**
	 * 
	 */
	public static final String ECARD_QUARTZ_JOB_NAME="ECards Job";
	public static final String ECARD_QUARTZ_JOB_GROUP="ECards Job";
	public static final String ECARD_QUARTZ_JOB_DESCRIPTION="ECards Job";
	public static final String ECARD_QUARTZ_JOB_CLASSNAME="com.dotcms.osgi.ecards.quartz.ECardsJob";
	public static final String ECARD_QUARTZ_JOB_CRON_EXPRESSION="0 0/5 * * * ?";

	/**
	 * APIs
	 */
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static PermissionAPI perAPI = APILocator.getPermissionAPI();
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	//private static FormAPI formAPI = APILocator.getFormAPI();
	private static FolderAPI folderAPI = APILocator.getFolderAPI();
	private static FileAssetAPI fileAssetAPI = APILocator.getFileAssetAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static ContentletAPI conAPI = APILocator.getContentletAPI();

	/**
	 * Create the eCards form used by this plugin
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static void createForm () throws DotDataException, DotSecurityException {
		User systemUser = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(systemUser, false);
		Structure form = StructureCache.getStructureByVelocityVarName(STRUCTURE_FORM_VAR_NAME);
		if(!UtilMethods.isSet(form) || !UtilMethods.isSet(form.getInode())){
			/**
			 * If the form doesn't exist, then it will be created
			 */
			form = new Structure();
			form.setName(STRUCTURE_FORM_NAME);
			form.setVelocityVarName(STRUCTURE_FORM_VAR_NAME);
			form.setDescription(STRUCTURE_FORM_DESCRIPTION);
			form.setFixed(false);
			form.setStructureType(Structure.STRUCTURE_TYPE_FORM);	
			form.setHost(host.getIdentifier());
			form.setOwner(systemUser.getUserId());
			form.setExpireDateVar("");
			form.setPublishDateVar("");
			StructureFactory.saveStructure(form);

			APILocator.getWorkflowAPI().saveSchemeForStruct(form, APILocator.getWorkflowAPI().findDefaultScheme());

			Permission p = new Permission();
			p.setInode(form.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_READ);
			perAPI.save(p, form, systemUser, true);

			p = new Permission();
			p.setInode(form.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_EDIT);
			perAPI.save(p, form, systemUser, true);

			p = new Permission();
			p.setInode(form.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_PUBLISH);
			perAPI.save(p, form, systemUser, true);	

			//formAPI.createBaseFormFields(form);
		}

		/**
		 * If the fields doesn't exist then they are going to be created
		 */
		Field formTitle = form.getFieldVar(FORM_TITLE_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(formTitle) || !UtilMethods.isSet(formTitle.getInode())) {
			formTitle = new Field(FORM_TITLE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,form,false,false,false,1,"", "", "", true, true, true);
			formTitle.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
			formTitle.setVelocityVarName(FORM_TITLE_FIELD_VAR_NAME);
			FieldFactory.saveField(formTitle);
		}

		Field formEmail = form.getFieldVar(FORM_EMAIL_FIELD_VARNAME);
		if(!UtilMethods.isSet(formEmail) || !UtilMethods.isSet(formEmail.getInode())) {
			formEmail = new Field(FORM_EMAIL_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,form,false,false,false,2,"", "", "", true, true, true);
			formEmail.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
			formEmail.setVelocityVarName(FORM_EMAIL_FIELD_VARNAME);
			FieldFactory.saveField(formEmail);
		}

		Field returnPage = form.getFieldVar(FORM_RETURN_PAGE_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(returnPage) || !UtilMethods.isSet(returnPage.getInode())) {
			returnPage = new Field(FORM_RETURN_PAGE_FIELD_NAME,Field.FieldType.HIDDEN,Field.DataType.TEXT,form,false,false,false,3,"", "", "", true, true, true);
			returnPage.setFieldContentlet(FieldAPI.ELEMENT_CONSTANT);
			returnPage.setVelocityVarName(FORM_RETURN_PAGE_FIELD_VAR_NAME);
			FieldFactory.saveField(returnPage);
		}

		Field hostField = form.getFieldVar(FORM_HOST_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(hostField) || !UtilMethods.isSet(hostField.getInode())) {	
			hostField = new Field(FORM_HOST_FIELD_NAME,Field.FieldType.HOST_OR_FOLDER,Field.DataType.TEXT,form,false,false,true,4,"", "", "", true, true, true);
			hostField.setVelocityVarName(FORM_HOST_FIELD_VAR_NAME);
			FieldFactory.saveField(hostField);
		}

		Field status = form.getFieldVar(FORM_STATUS_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(status) || !UtilMethods.isSet(status.getInode())) {
			status = new Field(FORM_STATUS_FIELD,Field.FieldType.SELECT,Field.DataType.TEXT,form,true,true,true,5,"", "", "", false, false, false);
			status.setVelocityVarName(FORM_STATUS_FIELD_VAR_NAME);
			status.setHint(FORM_STATUS_FIELD_HINT);
			status.setValues(FORM_STATUS_FIELD_VALUES);
			status.setFieldRelationType("");
			status.setDefaultValue(FORM_STATUS_FIELD_DEFAULT_VALUE);
			FieldFactory.saveField(status);
			FieldsCache.addField(status);
		}

		Field name = form.getFieldVar(FORM_NAME_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(name) || !UtilMethods.isSet(name.getInode())) {
			name = new Field(FORM_NAME_FIELD,Field.FieldType.TEXT,Field.DataType.TEXT,form,true,true,true,6,"", "", "", false, false, true);
			name.setVelocityVarName(FORM_NAME_FIELD_VAR_NAME);
			name.setHint(FORM_NAME_FIELD_HINT);
			name.setFieldRelationType("");
			FieldFactory.saveField(name);
			FieldsCache.addField(name);
		}

		Field email = form.getFieldVar(FORM_EMAIL_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(email) || !UtilMethods.isSet(email.getInode())) {
			email = new Field(FORM_EMAIL_FIELD,Field.FieldType.TEXT,Field.DataType.TEXT,form,true,true,true,7,"", "", "", false, false, true);
			email.setVelocityVarName(FORM_EMAIL_FIELD_VAR_NAME);
			email.setHint(FORM_EMAIL_FIELD_HINT);
			email.setFieldRelationType("");
			FieldFactory.saveField(email);
			FieldsCache.addField(email);
		}

		Field destinatary = form.getFieldVar(FORM_RECIPIENT_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(destinatary) || !UtilMethods.isSet(destinatary.getInode())) {
			destinatary = new Field(FORM_RECIPIENT_FIELD,Field.FieldType.TEXT,Field.DataType.TEXT,form,true,true,true,8,"", "", "", false, false, true);
			destinatary.setVelocityVarName(FORM_RECIPIENT_FIELD_VAR_NAME);
			destinatary.setHint(FORM_RECIPIENT_FIELD_HINT);
			destinatary.setFieldRelationType("");
			FieldFactory.saveField(destinatary);
			FieldsCache.addField(destinatary);
		}

		Field deliveryMethod = form.getFieldVar(FORM_DELIVERY_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(deliveryMethod) || !UtilMethods.isSet(deliveryMethod.getInode())) {
			deliveryMethod = new Field(FORM_DELIVERY_FIELD,Field.FieldType.CHECKBOX,Field.DataType.TEXT,form,true,true,true,9,"", "", "", false, false, true);
			deliveryMethod.setVelocityVarName(FORM_DELIVERY_FIELD_VAR_NAME);
			deliveryMethod.setHint(FORM_DELIVERY_FIELD_HINT);
			deliveryMethod.setValues(FORM_DELIVERY_FIELD_VALUES);
			deliveryMethod.setFieldRelationType("");
			FieldFactory.saveField(deliveryMethod);
			FieldsCache.addField(deliveryMethod);
		}

		Field deliveryLocation = form.getFieldVar(FORM_DELIVERY_ROOM_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(deliveryLocation) || !UtilMethods.isSet(deliveryLocation.getInode())) {
			deliveryLocation = new Field(FORM_DELIVERY_ROOM_FIELD,Field.FieldType.TEXT,Field.DataType.TEXT,form,true,true,true,10,"", "", "", false, false, true);
			deliveryLocation.setVelocityVarName(FORM_DELIVERY_ROOM_FIELD_VAR_NAME);
			deliveryLocation.setHint(FORM_DELIVERY_ROOM_FIELD_HINT);
			deliveryLocation.setDefaultValue(FORM_DELIVERY_ROOM_FIELD_DEFAULT_VALUE);
			deliveryLocation.setFieldRelationType("");
			FieldFactory.saveField(deliveryLocation);
			FieldsCache.addField(deliveryLocation);
		}

		Field deliveryEmail = form.getFieldVar(FORM_DELIVERY_EMAIL_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(deliveryEmail) || !UtilMethods.isSet(deliveryEmail.getInode())) {
			deliveryEmail = new Field(FORM_DELIVERY_EMAIL_FIELD,Field.FieldType.TEXT,Field.DataType.TEXT,form,true,true,true,11,"", "", "", false, false, true);
			deliveryEmail.setVelocityVarName(FORM_DELIVERY_EMAIL_FIELD_VAR_NAME);
			deliveryEmail.setHint(FORM_DELIVERY_EMAIL_FIELD_HINT);
			deliveryEmail.setDefaultValue(FORM_DELIVERY_EMAIL_FIELD_DEFAULT_VALUE);
			deliveryEmail.setFieldRelationType("");
			FieldFactory.saveField(deliveryEmail);
			FieldsCache.addField(deliveryEmail);
		}

		Field imageType = form.getFieldVar(FORM_IMAGE_TYPE_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(imageType) || !UtilMethods.isSet(imageType.getInode())) {
			imageType = new Field(FORM_IMAGE_TYPE_FIELD,Field.FieldType.SELECT,Field.DataType.TEXT,form,true,false,false,12,"", "", "", false, false, false);
			imageType.setVelocityVarName(FORM_IMAGE_TYPE_FIELD_VAR_NAME);
			imageType.setHint(FORM_IMAGE_TYPE_FIELD_HINT);
			imageType.setValues(FORM_IMAGE_TYPE_FIELD_VALUES);
			imageType.setDefaultValue(FORM_IMAGE_TYPE_FIELD_DEFAULT_VALUE);
			imageType.setFieldRelationType("");
			FieldFactory.saveField(imageType);
			FieldsCache.addField(imageType);
		}

		Field image = form.getFieldVar(FORM_BACKGROUND_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(image) || !UtilMethods.isSet(image.getInode())) {
			image = new Field(FORM_BACKGROUND_FIELD,Field.FieldType.CUSTOM_FIELD,Field.DataType.LONG_TEXT,form,true,false,false,13,"", "", "", false, false, false);
			image.setVelocityVarName(FORM_BACKGROUND_FIELD_VAR_NAME);
			image.setHint(FORM_BACKGROUND_FIELD_HINT);
			String filePath="#dotParse('//"+host.getHostname()+"/ecards/velocity/ecards-backgrounds.vtl')";
			image.setValues(filePath);
			image.setFieldRelationType("");
			FieldFactory.saveField(image);
			FieldsCache.addField(image);
		}

		Field message = form.getFieldVar(FORM_MESSAGE_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(message) || !UtilMethods.isSet(message.getInode())) {
			message = new Field(FORM_MESSAGE_FIELD,Field.FieldType.WYSIWYG,Field.DataType.LONG_TEXT,form,true,false,false,14,"", "", "", false, false, false);
			message.setVelocityVarName(FORM_MESSAGE_FIELD_VAR_NAME);
			message.setHint(FORM_MESSAGE_FIELD_HINT);
			message.setFieldRelationType("");
			FieldFactory.saveField(message);
			FieldsCache.addField(message);
		}

		Field scripts = form.getFieldVar(FORM_JAVASCRIPTS_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(scripts) || !UtilMethods.isSet(scripts.getInode())) {
			scripts = new Field(FORM_JAVASCRIPTS_FIELD,Field.FieldType.CUSTOM_FIELD,Field.DataType.LONG_TEXT,form,false,false,false,15,"", "", "", false, false, false);
			scripts.setVelocityVarName(FORM_JAVASCRIPTS_FIELD_VAR_NAME);
			scripts.setHint(FORM_JAVASCRIPTS_FIELD_HINT);
			String filePath="#dotParse('//"+host.getHostname()+"/ecards/velocity/ecards-scripts-and-buttons.vtl')";
			scripts.setValues(filePath);
			scripts.setFieldRelationType("");
			FieldFactory.saveField(scripts);
			FieldsCache.addField(scripts);
		}

		Field error = form.getFieldVar(FORM_ERROR_FIELD_VAR_NAME);
		if(!UtilMethods.isSet(error) || !UtilMethods.isSet(error.getInode())) {
			error = new Field(FORM_ERROR_FIELD,Field.FieldType.TEXT_AREA,Field.DataType.LONG_TEXT,form,false,false,false,16,"", "", "", false, false, false);
			error.setVelocityVarName(FORM_ERROR_FIELD_VAR_NAME);
			error.setHint(FORM_ERROR_FIELD_HINT);
			error.setFieldRelationType("");
			FieldFactory.saveField(error);
			FieldsCache.addField(error);
		}

		FieldsCache.removeFields(form);
		StructureCache.removeStructure(form);
		FieldsCache.addFields(form, form.getFieldsBySortOrder());

		/**
		 * Add the eCards admin role if this doesn't exist
		 */
		Role eCardsAdmin = roleAPI.loadRoleByKey(ECARD_ADMIN_ROLE_KEY);
		if(!UtilMethods.isSet(eCardsAdmin) || !UtilMethods.isSet(eCardsAdmin.getId())){
			eCardsAdmin = new Role();
			eCardsAdmin.setName(ECARD_ADMIN_ROLE_NAME);
			eCardsAdmin.setRoleKey(ECARD_ADMIN_ROLE_KEY);	
			eCardsAdmin.setDescription(ECARD_ADMIN_ROLE_DESCRIPTION);	
			eCardsAdmin.setSystem(false);
			eCardsAdmin.setEditLayouts(false);
			eCardsAdmin.setEditPermissions(false);
			eCardsAdmin.setEditUsers(true);
			roleAPI.save(eCardsAdmin);
		}

	}

	/**
	 * Generate the specified folder in dotcms backend under the default host
	 * @param path String with the folder path
	 * @return Folder object
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public static Folder createFolder (String path) throws DotDataException, DotSecurityException {
		User systemUser = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(systemUser, false);

		Folder folder = folderAPI.findFolderByPath(path, host, systemUser, true);
		if(!UtilMethods.isSet(folder.getInode())){
			folder = folderAPI.createFolders(path, host, systemUser, true);

			Permission p = new Permission();
			p.setInode(folder.getPermissionId());
			p.setRoleId(roleAPI.loadCMSAnonymousRole().getId());
			p.setPermission(PermissionAPI.PERMISSION_READ);
			perAPI.save(p, folder, systemUser, true);
		}
		return folder;
	}

	/**
	 * Indicates if the file asset already exist
	 * @param uploadedFile File to add
	 * @param folderPath File folder path
	 * @param fileName File name
	 * @return true if the file already exist
	 * @throws Exception
	 */
	public static boolean existFileAsset(java.io.File uploadedFile, String folderPath, String fileName) throws Exception {
		User systemUser = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(systemUser, false);
		Folder folder = folderAPI.findFolderByPath(folderPath, host,systemUser,false);

		return fileAssetAPI.fileNameExists(host,folder, fileName, "");		
	}

	/**
	 * Add the file as a file assets in dotCMS backend under the specified folder
	 * @param uploadedFile File to add
	 * @param folderPath File folder path
	 * @param title File name
	 * @return FileAsset
	 * @throws Exception
	 */
	public static FileAsset saveFile(java.io.File uploadedFile, String folderPath, String title) throws Exception {
		User systemUser = userAPI.getSystemUser();
		Host host = hostAPI.findDefaultHost(systemUser, false);

		Folder folder = folderAPI.findFolderByPath(folderPath, host,systemUser,false);
		if(!UtilMethods.isSet(folder.getInode())){
			folder = createFolder(folderPath);
		}

		byte[] bytes = FileUtil.getBytes(uploadedFile);

		if (bytes!=null) {
			String name = UtilMethods.getFileName(title);
			String fileName = name + "." + UtilMethods.getFileExtension(title);
			Contentlet cont = null;

			if(fileAssetAPI.fileNameExists(host,folder, fileName, "")){
				Identifier folderId = APILocator.getIdentifierAPI().find(folder);
				String path = folder.getInode().equals(FolderAPI.SYSTEM_FOLDER)?"/"+fileName:folderId.getPath()+fileName;
				Identifier fileAsset = APILocator.getIdentifierAPI().find(host, path);

				cont = conAPI.findContentletByIdentifier(fileAsset.getInode(), false, APILocator.getLanguageAPI().getDefaultLanguage().getId(), systemUser, false);
				cont.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
				cont.setInode("");
			}else{
				cont = new Contentlet();
				cont.setStructureInode(folder.getDefaultFileType());
				cont.setStringProperty(FileAssetAPI.TITLE_FIELD, UtilMethods.getFileName(name));
				cont.setFolder(folder.getInode());
				cont.setHost(host.getIdentifier());
				cont.setBinary(FileAssetAPI.BINARY_FIELD, uploadedFile);
				cont.setStringProperty("fileName", title);
			}

			cont = conAPI.checkin(cont, systemUser,false);
			if(perAPI.doesUserHavePermission(cont, PermissionAPI.PERMISSION_PUBLISH, systemUser))
				APILocator.getVersionableAPI().setLive(cont);

			return fileAssetAPI.fromContentlet(cont);
		}

		return null;
	}

	public static ClassLoader findFirstLoader ( ClassLoader loader ) {

		if ( loader.getParent() == null ) {
			return loader;
		} else {
			return findFirstLoader( loader.getParent() );
		}
	}

	/**
	 * /**
	 * Create the CronScheduledTask 
	 * @return CronScheduledTask
	 * @throws Exception
	 */
	public static CronScheduledTask generateCronScheduledTask() throws Exception {
		CronScheduledTask cronScheduledTask = new CronScheduledTask(ECARD_QUARTZ_JOB_NAME, ECARD_QUARTZ_JOB_GROUP, ECARD_QUARTZ_JOB_DESCRIPTION, ECARD_QUARTZ_JOB_CLASSNAME, new Date(), null, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW, new HashMap<String, Object>(), ECARD_QUARTZ_JOB_CRON_EXPRESSION);
		return cronScheduledTask;
	}

	/**
	 * Remove the Quartz job
	 * @throws Exception
	 */
	public static void removeQuartzJob() throws Exception{
		QuartzUtils.removeJob(ECARD_QUARTZ_JOB_NAME, ECARD_QUARTZ_JOB_GROUP);
	}

	/**
	 * Send email
	 * @param destinataryEmail To email
	 * @param body Email text
	 * @return true if the email was sent, otherwise false
	 */
	public static boolean sendEmail(String destinataryEmail, String subject, String body) throws Exception{
		Mailer m = new Mailer();
		m.setFromEmail(CompanyUtils.getDefaultCompany().getEmailAddress());
		m.setToEmail(destinataryEmail);
		m.setSubject(subject);
		m.setHTMLBody(body);
		return m.sendMessage();
	}
}
