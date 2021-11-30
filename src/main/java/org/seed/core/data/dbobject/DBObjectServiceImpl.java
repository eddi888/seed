/**
 * Seed
 * Copyright (C) 2021 EUU⛰ROCKS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.seed.core.data.dbobject;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.seed.C;
import org.seed.Seed;
import org.seed.core.application.AbstractApplicationEntityService;
import org.seed.core.application.ApplicationEntity;
import org.seed.core.application.ApplicationEntityService;
import org.seed.core.application.module.ImportAnalysis;
import org.seed.core.application.module.Module;
import org.seed.core.application.module.TransferContext;
import org.seed.core.config.SessionFactoryProvider;
import org.seed.core.config.changelog.ChangeLog;
import org.seed.core.data.ValidationException;
import org.seed.core.util.Assert;
import org.seed.core.util.MiscUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service
public class DBObjectServiceImpl extends AbstractApplicationEntityService<DBObject>
	implements DBObjectService {
	
	@Autowired
	private SessionFactoryProvider sessionFactoryProvider;
	
	@Autowired
	private DBObjectRepository repository;
	
	@Autowired
	private DBObjectValidator validator;
	
	@Override
	protected DBObjectRepository getRepository() {
		return repository;
	}

	@Override
	protected DBObjectValidator getValidator() {
		return validator;
	}
	
	@Override
	protected void analyzeNextVersionObjects(ImportAnalysis analysis, Module currentVersionModule) {
		if (analysis.getModule().getDBObjects() != null) {
			for (DBObject dbObject : analysis.getModule().getDBObjects()) {
				if (currentVersionModule == null) {
					analysis.addChangeNew(dbObject);
				}
				else {
					final DBObject currentVersionObject = 
						currentVersionModule.getDBObjectByUid(dbObject.getUid());
					if (currentVersionObject == null) {
						analysis.addChangeNew(dbObject);
					}
					else if (!dbObject.isEqual(currentVersionObject)) {
						analysis.addChangeModify(dbObject);
					}
				}
			}
		}
	}
	
	@Override
	protected void analyzeCurrentVersionObjects(ImportAnalysis analysis, Module currentVersionModule) {
		if (currentVersionModule!= null && currentVersionModule.getDBObjects() != null) {
			for (DBObject currentVersionObject : currentVersionModule.getDBObjects()) {
				if (analysis.getModule().getEntityByUid(currentVersionObject.getUid()) == null) {
					analysis.addChangeDelete(currentVersionObject);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends ApplicationEntityService<ApplicationEntity>>[] getImportDependencies() {
		return MiscUtils.toArray(); // independent
	}

	@Override
	public void importObjects(TransferContext context, Session session) {
		Assert.notNull(context, C.CONTEXT);
		Assert.notNull(session, C.SESSION);
		
		if (context.getModule().getDBObjects() != null) {
			for (DBObject dbObject : context.getModule().getDBObjects()) {
				final DBObject currentVersionObject = findByUid(session, dbObject.getUid());
				((DBObjectMetadata) dbObject).setModule(context.getModule());
				// db object already exist
				if (currentVersionObject != null) {
					context.addExistingDBObject(dbObject, currentVersionObject);
					((DBObjectMetadata) currentVersionObject).copySystemFieldsTo(dbObject);
					session.detach(currentVersionObject);
				}
				else { // new db Object
					context.addNewDBObject(dbObject);
				}
				repository.save(dbObject, session);
			}
		}
	}
	
	@Override
	public void createChangeLogs(TransferContext context, Session session) {
		Assert.notNull(context, C.CONTEXT);
		Assert.notNull(session, C.SESSION);
		
		final List<DBObject> newObjects = new ArrayList<>(context.getNewDBObjects());
		newObjects.sort((DBObject dbObject1, DBObject dbObject2) -> 
							Integer.compare(dbObject1.getOrder() != null ? dbObject1.getOrder() : 0, 
											dbObject2.getOrder() != null ? dbObject2.getOrder() : 0));
		for (DBObject dbObject : newObjects) {
			final ChangeLog changeLog = createChangeLog(null, dbObject);
			if (changeLog != null) {
				session.saveOrUpdate(changeLog);
			}
		}
		
		final List<DBObject> existingObjects = new ArrayList<>(context.getExistingDBObjects());
		
		existingObjects.sort((DBObject dbObject1, DBObject dbObject2) -> 
								Integer.compare(dbObject1.getOrder() != null ? dbObject1.getOrder() : 0, 
												dbObject2.getOrder() != null ? dbObject2.getOrder() : 0));
		for (DBObject dbObject : existingObjects) {
			final DBObject currentVersionObject = context.getCurrentVersionDBObject(dbObject.getUid());
			final ChangeLog changeLog = createChangeLog(currentVersionObject, dbObject);
			if (changeLog != null) {
				session.saveOrUpdate(changeLog);
			}
		}
	}
	
	@Override
	public void deleteObjects(Module module, Module currentVersionModule, Session session) {
		Assert.notNull(module, C.MODULE);
		Assert.notNull(currentVersionModule, "currentVersionModule");
		Assert.notNull(session, C.SESSION);
		
		if (currentVersionModule.getDBObjects() != null) {
			for (DBObject currentVersionObject : currentVersionModule.getDBObjects()) {
				if (module.getDBObjectByUid(currentVersionObject.getUid()) == null) {
					session.delete(currentVersionObject);
				}
			}
		}
	}
	
	@Override
	@Secured("ROLE_ADMIN_DBOBJECT")
	public void deleteObject(DBObject dbObject) throws ValidationException {
		Assert.notNull(dbObject, C.DBOBJECT);
		
		try (Session session = sessionFactoryProvider.getSessionFactory().openSession()) {
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				deleteObject(dbObject, session);
				
				final ChangeLog changeLog = createChangeLog(dbObject, null);
				if (changeLog != null) {
					session.saveOrUpdate(changeLog);
				}
				tx.commit();
			}
			catch (Exception ex) {
				handleException(tx, ex);
			}
		}
		
		Seed.updateConfiguration();
	}
	
	@Override
	@Secured("ROLE_ADMIN_DBOBJECT")
	public void saveObject(DBObject dbObject) throws ValidationException {
		Assert.notNull(dbObject, C.DBOBJECT);
		
		final boolean isInsert = dbObject.isNew();
		final DBObject currentVersionObject = !isInsert ? getObject(dbObject.getId()) : null;
		try (Session session = sessionFactoryProvider.getSessionFactory().openSession()) {
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				saveObject(dbObject, session);
				
				final ChangeLog changeLog = createChangeLog(currentVersionObject, dbObject);
				if (changeLog != null) {
					session.saveOrUpdate(changeLog);
				}
				tx.commit();
			}
			catch (Exception ex) {
				handleException(tx, ex);
			}
		}
		
		Seed.updateConfiguration();
	}
	
	private static ChangeLog createChangeLog(DBObject currentVersionObject, DBObject nextVersionObject) {
		return new DBObjectChangeLogBuilder()
						.setCurrentVersionObject(currentVersionObject)
						.setNextVersionObject(nextVersionObject)
						.build();
	}
	
}
