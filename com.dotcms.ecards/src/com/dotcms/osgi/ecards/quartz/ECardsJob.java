package com.dotcms.osgi.ecards.quartz;

import java.io.FileInputStream;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.dotcms.osgi.ecards.ECards;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.business.UserAPI;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.business.DotContentletStateException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;

/**
 * This class manage the email notifications
 * @author Oswaldo Gallango
 *
 */
public class ECardsJob implements Job{

	private static ContentletAPI conAPI = APILocator.getContentletAPI();
	private static UserAPI userAPI = APILocator.getUserAPI();
	private static HostAPI hostAPI = APILocator.getHostAPI();
	private static RoleAPI roleAPI = APILocator.getRoleAPI();
	private static LanguageAPI langAPI = APILocator.getLanguageAPI();

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {		
		try {
			User systemUser = userAPI.getSystemUser();
			Role ecardsAdmin = roleAPI.findRoleByName(ECards.ECARD_ADMIN_ROLE_NAME, null);
			List<User> eCardsAdmins = roleAPI.findUsersForRole(ecardsAdmin);

			Structure form = StructureCache.getStructureByVelocityVarName(ECards.STRUCTURE_FORM_VAR_NAME);
			List<Contentlet> pendingEcardsForms = conAPI.search("+structureName:"+ECards.STRUCTURE_FORM_VAR_NAME+" +working:true +deleted:false +eCards.status:pending", 0, 0, "", systemUser, false);
			for(Contentlet cont : pendingEcardsForms){

				Field deliveryMethod = form.getFieldVar(ECards.FORM_DELIVERY_FIELD_VAR_NAME);
				String deliveryMethodString = (String)conAPI.getFieldValue(cont, deliveryMethod);

				Field name = form.getFieldVar(ECards.FORM_NAME_FIELD_VAR_NAME);
				String senderNameString = (String)conAPI.getFieldValue(cont, name);

				Field email = form.getFieldVar(ECards.FORM_EMAIL_FIELD_VAR_NAME);
				String senderEmailString = (String)conAPI.getFieldValue(cont, email);

				Field destinatary = form.getFieldVar(ECards.FORM_RECIPIENT_FIELD_VAR_NAME);
				String destinataryNameString = (String)conAPI.getFieldValue(cont, destinatary);

				Field message = form.getFieldVar(ECards.FORM_MESSAGE_FIELD_VAR_NAME);
				String messageString = (String)conAPI.getFieldValue(cont, message);
				messageString = messageString.replaceAll("\\$\\{esc.hash}", "#");

				Field status = form.getFieldVar(ECards.FORM_STATUS_FIELD_VAR_NAME);
				Field error = form.getFieldVar(ECards.FORM_ERROR_FIELD_VAR_NAME);
				String errorString = (String)conAPI.getFieldValue(cont, error);

				Field imageType = form.getFieldVar(ECards.FORM_IMAGE_TYPE_FIELD_VAR_NAME);
				String imageTypeString = (String)conAPI.getFieldValue(cont, imageType);

				Field hostField = form.getFieldVar(ECards.FORM_HOST_FIELD_VAR_NAME);
				String hostFieldString = (String)conAPI.getFieldValue(cont, hostField);
				Host host = hostAPI.find(hostFieldString, systemUser, false);

				if(deliveryMethodString.indexOf("Send to Room") != -1){
					//Notify to eCards administrator
					Field deliveryLocation = form.getFieldVar(ECards.FORM_DELIVERY_ROOM_FIELD_VAR_NAME);
					String deliveryLocationString = (String)conAPI.getFieldValue(cont, deliveryLocation);

					try{
						String subject = host.getHostname()+" - eCard to deliver notification!!";
						StringBuffer body = new StringBuffer();
						body.append("<html><head><link href=\"http://"+hostAPI.findDefaultHost(systemUser, false).getHostname()+"/ecards/css/ecards-styles.css\" rel=\"stylesheet\" type=\"text/css\" /></head><body>");
						body.append("<p>The following ecard has been requested to be delivered:</p><table>");
						body.append("<tr><td>Host:</td><td>"+host.getHostname()+"</td></tr>");
						body.append("<tr><td>Sender:</td><td>"+senderNameString+" ("+senderEmailString+")</td></tr>");					
						body.append("<tr><td>Recipient</td><td>"+destinataryNameString+"</td></tr>");
						body.append("<tr><td>Recipient Location</td><td>"+deliveryLocationString+"</td></tr>");
						body.append("<tr><td>Message</td><td>"+messageString+"</td></tr>");
						body.append("</table>");		
						body.append("</body></html>");

						for(User eCardAdmin : eCardsAdmins){
							ECards.sendEmail(eCardAdmin.getEmailAddress(), subject, body.toString());
						}
					}catch(Exception ex){
						conAPI.setContentletProperty(cont, error, errorString+"\r\n"+ex.getMessage());
						conAPI.setContentletProperty(cont, status, "error");
						cont.setInode("");
						conAPI.checkin(cont, systemUser, false);
					}
				}

				if(deliveryMethodString.indexOf("Send to Email") != -1){
					//Send email to recipient
					Field deliveryEmail = form.getFieldVar(ECards.FORM_DELIVERY_EMAIL_FIELD_VAR_NAME);
					String destinataryEmailString = (String)conAPI.getFieldValue(cont, deliveryEmail);

					Field image = form.getFieldVar(ECards.FORM_BACKGROUND_FIELD_VAR_NAME);
					String imageString = (String)conAPI.getFieldValue(cont, image);
					Contentlet imageCont = conAPI.findContentletByIdentifier(imageString, true, langAPI.getDefaultLanguage().getId(), systemUser, false);
					FileAsset imageFileAsset = APILocator.getFileAssetAPI().fromContentlet(imageCont);
					int tempWidth = 720;
					Double tempHeight = Math.ceil((imageFileAsset.getHeight() * tempWidth) / imageFileAsset.getWidth());
									
					String hostString = hostAPI.findDefaultHost(systemUser, false).getHostname();
					String subject = host.getHostname()+" - "+ imageTypeString.replace("_", " ")+" eCard";
					StringBuffer body = new StringBuffer();
					body.append("<html><style>");
					List<Contentlet> cssContentletList = conAPI.search("+structureName:FileAsset +conhost:"+host.getIdentifier()+" +FileAsset.fileName:*ecards\\-styles.css", 1, 0, "", systemUser, false);
					List<FileAsset> cssList = APILocator.getFileAssetAPI().fromContentlets(cssContentletList);
					for(FileAsset css : cssList){
						java.io.File fileIO = css.getFileAsset();
						FileInputStream fios = new FileInputStream(fileIO);
						byte[] data = new byte[fios.available()];
						fios.read(data);
						body.append( new String(data));
					}
					body.append("</style><body>");
					body.append("<div id=\"ecard-print-info\" style=\"border: 2px solid black; margin: 5px;  visibility: visible;  width:720px;\">");
					body.append("<div id=\"ecard-print-from\" style=\"margin: 20px 20px 10px 20px;\"><h3>From: "+senderNameString+"</h3></div>");
					body.append("<div id=\"ecard-print-to\" style=\"margin: 0px 20px 0px 20px;\"><h3>To: "+destinataryNameString+"</h3></div>");
					body.append("</div>");
					body.append("<div id=\"ecard-print-background\" class=\"print\" style=\"border: 2px solid black; margin: 5px; visibility: visible;width:" + tempWidth + "px; height:" +tempHeight.intValue()+ "px; background-image:url(http://"+hostString+(UtilMethods.isSet(ECards.HOST_PORT)?":"+ECards.HOST_PORT:"")+"/contentAsset/image/"+imageCont.getInode()+"/fileAsset/byInode/1/filter/Resize/resize_w/"+ tempWidth + "/resize_h/"+ tempHeight.intValue() +"/)\">");
					body.append("<div id=\"ecard-print-body\" style=\"margin: 20px;overflow:hidden;display:inline-block;\">"+messageString+"</div>");
					body.append("</div>");		
					body.append("</body></html>");
					try{
						Logger.info(ECardsJob.class, "eCards plugin: Sending email");
						ECards.sendEmail(destinataryEmailString, subject, body.toString());
						conAPI.setContentletProperty(cont, status, "executed");
						cont.setInode("");
						conAPI.checkin(cont, systemUser, false);
						//Send copy to sender
						subject = host.getHostname()+" - "+ imageTypeString.replace("_", " ")+" eCard (Copy)";
						ECards.sendEmail(senderEmailString, subject, body.toString());						
					}catch(Exception ex){
						Logger.info(ECardsJob.class, "eCards plugin: Sending email fails."+ex.getMessage());
						conAPI.setContentletProperty(cont, error, errorString+"\r\n"+ex.getMessage());
						conAPI.setContentletProperty(cont, status, "error");
						cont.setInode("");
						conAPI.checkin(cont, systemUser, false);
					}
				}
			}
		} catch (DotDataException e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		} catch (DotSecurityException e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		} catch (DotContentletValidationException e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		} catch (DotContentletStateException e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		} catch (IllegalArgumentException e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		} catch (Exception e) {
			Logger.error(ECardsJob.class, e.getMessage(), e);
		}		
	}

}
