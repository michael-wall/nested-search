/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Liferay Enterprise
 * Subscription License ("License"). You may not use this file except in
 * compliance with the License. You can obtain a copy of the License by
 * contacting Liferay, Inc. See the License for the specific language governing
 * permissions and limitations under the License, including but not limited to
 * distribution rights of the Software.
 *
 *
 *
 */

package com.mw.search;

import com.liferay.object.model.ObjectEntry;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.object.service.ObjectRelationshipLocalService;
import com.liferay.portal.kernel.exception.ModelListenerException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.BaseModelListener;
import com.liferay.portal.kernel.model.ModelListener;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistryUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael Wall
 */
@Component(service = ModelListener.class)
public class RequestorObjectEntryModelListener extends BaseModelListener<ObjectEntry> {
	
	private boolean isRelevantFieldChanged(ObjectEntry originalModel, ObjectEntry model) {
		
		// Check only the fields that are added to the Case Elasticsearch document.
		
		Map<String, Serializable> originalValues = originalModel.getValues();
		Map<String, Serializable> newValues = model.getValues();
		
		String originalName = "";
		String newName = "";
		
		if (originalValues.containsKey("name")) originalName = (String)originalValues.get("name");  // HARDCODED: Field name from Requestor Object
		if (newValues.containsKey("name")) newName = (String)newValues.get("name");  // HARDCODED: Field name from Requestor Object
		
		if (!originalName.equalsIgnoreCase(newName)) return true;
		
		String originalCompany = "";
		String newCompany = "";
		
		if (originalValues.containsKey("company")) originalCompany = (String)originalValues.get("company");  // HARDCODED: Field name from Requestor Object
		if (newValues.containsKey("company")) newCompany = (String)newValues.get("company");  // HARDCODED: Field name from Requestor Object
		
		if (!originalCompany.equalsIgnoreCase(newCompany)) return true;
		
		String originalAddress = "";
		String newAddress = "";
		
		if (originalValues.containsKey("address")) originalAddress = (String)originalValues.get("address");  // HARDCODED: Field name from Requestor Object
		if (newValues.containsKey("address")) newAddress = (String)newValues.get("address");  // HARDCODED: Field name from Requestor Object
		
		if (!originalAddress.equalsIgnoreCase(newAddress)) return true;
		
		return false;
	}

	@Override
	public void onAfterUpdate(ObjectEntry originalModel, ObjectEntry model) throws ModelListenerException {

		if (originalModel.getObjectDefinitionId() != 34704) { // HARDCODED: Requestor Object Definition ID.
			super.onAfterUpdate(originalModel, model);
			
			return;
		}
		
		_log.info("onAfterUpdate for requestor: " + originalModel.getObjectEntryId());

		boolean isRelevantFieldChanged = isRelevantFieldChanged(originalModel, model);
		
		_log.info("isRelevantFieldChanged: " + isRelevantFieldChanged);
		
		if (!isRelevantFieldChanged) {	
			super.onAfterUpdate(originalModel, model);
			
			return;
		}
		
		ObjectRelationship objectRelationship = objectRelationshipLocalService.fetchObjectRelationshipByObjectDefinitionId(originalModel.getObjectDefinitionId(), "case"); // HARDCODED: Relationship name from Requestor > Relationships.
		
		_log.info("objectRelationship: " + objectRelationship.getObjectRelationshipId());
		
		Indexer<ObjectEntry> indexer = IndexerRegistryUtil.nullSafeGetIndexer("com.liferay.object.model.ObjectDefinition#34644"); // HARDCODED: Case Object Definition ID.

		List<ObjectEntry> caseObjectEntries = new ArrayList<ObjectEntry>();
		try {
			caseObjectEntries = objectEntryLocalService.getOneToManyObjectEntries(originalModel.getCompanyId(), objectRelationship.getObjectRelationshipId(), originalModel.getObjectEntryId(), true, null, -1, -1);

			_log.info("getOneToManyObjectEntries count: " + caseObjectEntries.size());
			
			for (ObjectEntry caseObjectEntry: caseObjectEntries) {
				_log.info("reindex Case: " + caseObjectEntry.getObjectEntryId());
				
				indexer.reindex(caseObjectEntry);
			}		
		} catch (PortalException e) {
			e.printStackTrace();
		}
		
		super.onAfterUpdate(originalModel, model);
	}
	
	@Reference(unbind = "-")
	private ObjectEntryLocalService objectEntryLocalService;
	
	@Reference(unbind = "-")
	private ObjectRelationshipLocalService objectRelationshipLocalService;
	
	private static Log _log = LogFactoryUtil.getLog(RequestorObjectEntryModelListener.class);	
}