package org.apache.sling.handlebars;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.webresource.WebResourceScriptRunner;
import org.apache.sling.webresource.util.JCRUtils;

public class HandlebarUtils {
	
	public static void processHandlebarHelperFolder(ResourceResolver resolver,
			InputStream consolidatedHandlebarsScriptStream,
			Resource handlebarsHelperFolderResource) throws RepositoryException {
		if(handlebarsHelperFolderResource != null)
		{
			Iterator<Resource> helperIterator = handlebarsHelperFolderResource
					.listChildren();

			while (helperIterator.hasNext()) {
				Resource currentHelper = helperIterator.next();
				if (currentHelper.isResourceType("nt:file")) {
					consolidatedHandlebarsScriptStream = new SequenceInputStream(
							consolidatedHandlebarsScriptStream,
							JCRUtils.getFileResourceAsStream(resolver,
									currentHelper.getPath()));
				}
			}
		}
	}
	

		

}
