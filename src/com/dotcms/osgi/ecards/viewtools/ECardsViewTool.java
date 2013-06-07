package com.dotcms.osgi.ecards.viewtools;

import java.util.List;

import org.apache.velocity.tools.view.tools.ViewTool;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.Logger;
import com.liferay.portal.model.User;

public class ECardsViewTool implements ViewTool {

	@Override
	public void init(Object initData) {
	}

	/**
	 * Find the sub folders located inside the specified folder and host
	 * @param path Path to the parent folder
	 * @param hostId Host identifier
	 * @return List<Folder>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Folder> findImagesSubFoldersByParentFolderAndHost(String path, String hostId) throws DotDataException, DotSecurityException {
		List<Folder> subfolders = null;
		User systemUser = APILocator.getUserAPI().getSystemUser();
		try{
			Folder parentFolder = APILocator.getFolderAPI().findFolderByPath(path, hostId, systemUser, true);
			subfolders = APILocator.getFolderAPI().findSubFolders(parentFolder, systemUser, true);
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return subfolders;
	} 
	/**
	 * Find the images located in the specified folder and host
	 * @param path Path to the images folder
	 * @param hostId Host identifier
	 * @return List<FileAsset>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<FileAsset> findImagesByFolderAndHost(String path, String hostId) throws DotDataException, DotSecurityException {
		List<FileAsset> assets = null;
		User systemUser = APILocator.getUserAPI().getSystemUser();
		try{
			Folder parentFolder = APILocator.getFolderAPI().findFolderByPath(path, hostId, systemUser, true);
			assets = APILocator.getFileAssetAPI().fromContentlets(APILocator.getPermissionAPI().filterCollection(APILocator.getContentletAPI().search("+conHost:" +hostId +" +structureType:" + Structure.STRUCTURE_TYPE_FILEASSET+" +conFolder:" + parentFolder.getInode(), -1, 0, null , systemUser, true), PermissionAPI.PERMISSION_READ, true, systemUser));
		} catch (Exception e) {
			Logger.error(this.getClass(), e.getMessage(), e);
			throw new DotRuntimeException(e.getMessage(), e);
		}
		return assets;
	}
}
