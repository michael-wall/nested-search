package com.mw.search;

import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.search.spi.model.index.contributor.ModelDocumentContributor;

import java.io.Serializable;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael Wall
 */
@Component(
		property = "indexer.class.name=com.liferay.object.model.ObjectDefinition#34644",  // HARDCODED: Case Object Definition ID.
		service = ModelDocumentContributor.class
	)
	public class CaseObjectEntryDocumentContributor
		implements ModelDocumentContributor<ObjectEntry> {

	@Override
	public void contribute(Document document, ObjectEntry objectEntry) {		
		try {
			Map<String, Serializable> caseObjectValues = objectEntryLocalService.getValues(objectEntry.getObjectEntryId());
			
			long requestorId = 0;
			
			if (caseObjectValues.containsKey("r_case_c_requestorId")) { // HARDCODED: Relationship field name from Elasticsearch document from nestedFieldsArray.
				requestorId = (Long)caseObjectValues.get("r_case_c_requestorId"); // HARDCODED: Relationship field name from Elasticsearch document from nestedFieldsArray.
			}
			
			String requestorKeywords = "";
			
			if (requestorId > 0) {
				Map<String, Serializable> requestorObjectValues = objectEntryLocalService.getValues(requestorId);
				
				if (requestorObjectValues.containsKey("name")) { // HARDCODED: Field name from Requestor Object
					requestorKeywords += (String)requestorObjectValues.get("name"); // HARDCODED: Field name from Requestor Object
					requestorKeywords += " ";
				}
				
				if (requestorObjectValues.containsKey("company")) { // HARDCODED: Field name from Requestor Object
					requestorKeywords += (String)requestorObjectValues.get("company"); // HARDCODED: Field name from Requestor Object
					requestorKeywords += " ";
				}
				
				if (requestorObjectValues.containsKey("address")) { // HARDCODED: Field name from Requestor Object
					requestorKeywords += (String)requestorObjectValues.get("address"); // HARDCODED: Field name from Requestor Object
				}
			}

			if (!requestorKeywords.equalsIgnoreCase("")) {
				try {
					document.addKeyword("requestor", requestorKeywords);  // HARDCODED: Custom field name added to Case Elasticsearch document
					
					_log.info("Added or updated requestor field on " + objectEntry.getObjectEntryId());
				}
				catch (Exception exception) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"Unable to append requestor to object entry " +
								objectEntry.getObjectEntryId(),
							exception);
					}
				}
			}
		} catch (PortalException e) {
			e.printStackTrace();
		}
	}
	
	@Reference(unbind = "-")
	private ObjectEntryLocalService objectEntryLocalService;
	
	private static Log _log = LogFactoryUtil.getLog(CaseObjectEntryDocumentContributor.class);
}