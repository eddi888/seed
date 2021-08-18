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
package org.seed.core.entity.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.hibernate.Session;

import org.seed.C;
import org.seed.InternalException;
import org.seed.core.application.AbstractApplicationEntityService;
import org.seed.core.application.ApplicationEntity;
import org.seed.core.application.ApplicationEntityService;
import org.seed.core.application.module.ImportAnalysis;
import org.seed.core.application.module.Module;
import org.seed.core.application.module.TransferContext;
import org.seed.core.config.UpdatableConfiguration;
import org.seed.core.data.Options;
import org.seed.core.data.ValidationException;
import org.seed.core.entity.Entity;
import org.seed.core.entity.EntityDependent;
import org.seed.core.entity.EntityField;
import org.seed.core.entity.EntityFieldGroup;
import org.seed.core.entity.EntityFunction;
import org.seed.core.entity.EntityService;
import org.seed.core.entity.EntityStatus;
import org.seed.core.entity.NestedEntity;
import org.seed.core.user.UserGroup;
import org.seed.core.user.UserGroupDependent;
import org.seed.core.user.UserGroupService;
import org.seed.core.util.Assert;
import org.seed.core.util.MultiKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

@Service
public class TransformerServiceImpl extends AbstractApplicationEntityService<Transformer>
	implements TransformerService, EntityDependent<Transformer>, UserGroupDependent<Transformer> {
	
	@Autowired
	private EntityService entityService;
	
	@Autowired
	private TransformerRepository repository;
	
	@Autowired
	private TransformerValidator validator;
	
	@Autowired
	private UserGroupService userGroupService;
	
	@Autowired
	private UpdatableConfiguration configuration;
	
	@Override
	protected TransformerRepository getRepository() {
		return repository;
	}

	@Override
	protected TransformerValidator getValidator() {
		return validator;
	}
	
	@Override
	public Transformer createInstance(@Nullable Options options) {
		final TransformerMetadata transformer = (TransformerMetadata) super.createInstance(options);
		transformer.createLists();
		return transformer;
	}
	
	@Override
	@Secured("ROLE_ADMIN_ENTITY")
	public TransformerFunction createFunction(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		final TransformerFunction function = new TransformerFunction();
		transformer.addFunction(function);
		return function;
	}
	
	@Override
	public Transformer getTransformerByName(Entity sourceEntity, Entity targetEntity, String name) {
		Assert.notNull(sourceEntity, C.SOURCEENTITY);
		Assert.notNull(targetEntity, C.TARGETENTITY);
		Assert.notNull(name, C.NAME);
		
		final List<Transformer> list = repository.find(queryParam(C.SOURCEENTITY, sourceEntity),
				   									   queryParam(C.TARGETENTITY, targetEntity),
				   									   queryParam(C.NAME, name));
		return !list.isEmpty() ? list.get(0) : null;
	}
	
	@Override
	public List<Transformer> findTransformers(Entity sourceEntity) {
		Assert.notNull(sourceEntity, C.SOURCEENTITY);
		
		return repository.find(queryParam(C.SOURCEENTITY, sourceEntity));
	}
	
	@Override
	public List<Transformer> findTransformers(Entity sourceEntity, Entity targetEntity) {
		Assert.notNull(sourceEntity, C.SOURCEENTITY);
		Assert.notNull(targetEntity, C.TARGETENTITY);
		
		return repository.find(queryParam(C.SOURCEENTITY, sourceEntity),
							   queryParam(C.TARGETENTITY, targetEntity));
	}
	
	@Override
	public List<Transformer> findUsage(Entity entity) {
		Assert.notNull(entity, C.ENTITY);
		
		final Set<Transformer> result = new HashSet<>();
		result.addAll(repository.find(queryParam(C.SOURCEENTITY, entity)));
		result.addAll(repository.find(queryParam(C.TARGETENTITY, entity)));
		return new ArrayList<>(result);
	}
	
	public List<TransformerPermission> getAvailablePermissions(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		final List<TransformerPermission> result = new ArrayList<>();
		for (UserGroup group : userGroupService.findAllObjects()) {
			boolean found = false;
			if (transformer.hasPermissions()) {
				for (TransformerPermission permission : transformer.getPermissions()) {
					if (permission.getUserGroup().equals(group)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				final TransformerPermission permission = new TransformerPermission();
				permission.setTransformer(transformer);
				permission.setUserGroup(group);
				result.add(permission);
			}
		}
		return result;
	}
	
	@Override
	public List<EntityStatus> getAvailableStatus(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		final List<EntityStatus> result = new ArrayList<>();
		if (transformer.getSourceEntity().hasStatus()) {
			for (EntityStatus status : transformer.getSourceEntity().getStatusList()) {
				if (!transformer.containsStatus(status)) {
					result.add(status);
				}
			}
		}
		return result;
	}
	
	@Override
	public List<TransformerElement> getMainObjectElements(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		final List<TransformerElement> result = new ArrayList<>();
		if (transformer.hasElements()) {
			for (TransformerElement element : transformer.getElements()) {
				if (transformer.getSourceEntity().containsField(element.getSourceField())) {
					result.add(element);
				}
			}
		}
		return result;
	}
	
	@Override
	public List<NestedTransformer> getNestedTransformers(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		final Map<MultiKey, NestedTransformer> resultMap = new HashMap<>();
		if (transformer.hasElements()) {
			final Entity sourceEntity = transformer.getSourceEntity();
			final Entity targetEntity = transformer.getTargetEntity();
			for (TransformerElement element : transformer.getElements()) {
				final NestedEntity sourceNested = sourceEntity.getNestedByEntityField(element.getSourceField());
				final NestedEntity targetNested = targetEntity.getNestedByEntityField(element.getTargetField());
				if (sourceNested != null && targetNested != null) {
					final MultiKey key = MultiKey.valueOf(sourceNested, targetNested);
					resultMap.computeIfAbsent(key, t -> new NestedTransformer(sourceNested, targetNested));
					resultMap.get(key).addElement(element);
				}
			}
		}
		return new ArrayList<>(resultMap.values());
	}
	
	@Override
	@Secured("ROLE_ADMIN_ENTITY")
	public void adjustElements(Transformer transformer, List<TransformerElement> elements, List<NestedTransformer> nesteds) {
		Assert.notNull(transformer, C.TRANSFORMER);
		Assert.notNull(elements, "elements");
		Assert.notNull(nesteds, "nesteds");
		
		for (TransformerElement element : elements) {
			if (!transformer.containsElement(element)) {
				transformer.addElement(element);
			}
		}
		for (NestedTransformer nested : nesteds) {
			for (TransformerElement element : nested.getElements()) {
				if (!transformer.containsElement(element)) {
					transformer.addElement(element);
				}
			}
		}
		if (transformer.hasElements()) {
			adjustTransformerElements(transformer, elements, nesteds);
		}
	}
	
	private void adjustTransformerElements(Transformer transformer, List<TransformerElement> elements, List<NestedTransformer> nesteds) {
		for (Iterator<TransformerElement> it = transformer.getElements().iterator(); it.hasNext();) {
			final TransformerElement element = it.next();
			boolean found = false;
			if (elements.contains(element)) {
				found = true;
			}
			else {
				for (NestedTransformer nested : nesteds) {
					if (nested.containsElement(element)) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				it.remove();
			}
		}
	}
	
	@Override
	@Secured("ROLE_ADMIN_ENTITY")
	public void saveObject(Transformer transformer) throws ValidationException {
		super.saveObject(transformer);
	
		if (transformer.hasFunctions()) {
			configuration.updateConfiguration();
		}
	}
	
	@Override
	@Secured("ROLE_ADMIN_ENTITY")
	public void deleteObject(Transformer transformer) throws ValidationException {
		super.deleteObject(transformer);
	}

	@Override
	public List<Transformer> findUsage(EntityField entityField) {
		Assert.notNull(entityField, C.ENTITYFIELD);
		
		final List<Transformer> result = new ArrayList<>();
		for (Transformer transformer : findAllObjects()) {
			if (transformer.hasElements()) {
				for (TransformerElement element : transformer.getElements()) {
					if (entityField.equals(element.getSourceField()) ||
						entityField.equals(element.getTargetField())) {
						result.add(transformer);
						break;
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public List<Transformer> findUsage(UserGroup userGroup) {
		Assert.notNull(userGroup, C.USERGROUP);
		
		final List<Transformer> result = new ArrayList<>();
		for (Transformer transformer : findAllObjects()) {
			if (transformer.hasPermissions()) {
				for (TransformerPermission permission : transformer.getPermissions()) {
					if (userGroup.equals(permission.getUserGroup())) {
						result.add(transformer);
						break;
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public List<Transformer> findUsage(EntityFieldGroup fieldGroup) {
		return Collections.emptyList();
	}
	
	@Override
	public List<Transformer> findUsage(EntityStatus entityStatus) {
		return Collections.emptyList();
	}
	
	@Override
	public List<Transformer> findUsage(EntityFunction entityFunction) {
		return Collections.emptyList();
	}
	
	@Override
	public List<Transformer> findUsage(NestedEntity nestedEntity) {
		return Collections.emptyList();
	}
	
	@Override
	protected void analyzeNextVersionObjects(ImportAnalysis analysis, Module currentVersionModule) {
		if (analysis.getModule().getTransformers() != null) {
			for (Transformer transformer : analysis.getModule().getTransformers()) {
				if (currentVersionModule == null) {
					analysis.addChangeNew(transformer);
				}
				else {
					final Transformer currentVersionTransformer = 
						currentVersionModule.getTransformerByUid(transformer.getUid());
					if (currentVersionTransformer == null) {
						analysis.addChangeNew(transformer);
					}
					else if (!transformer.isEqual(currentVersionTransformer)) {
						analysis.addChangeModify(transformer);
					}
				}
			}
		}
	}
	
	@Override
	protected void analyzeCurrentVersionObjects(ImportAnalysis analysis, Module currentVersionModule) {
		if (currentVersionModule.getTransformers() != null) {
			for (Transformer currentVersionTransformer : currentVersionModule.getTransformers()) {
				if (analysis.getModule().getTransformerByUid(currentVersionTransformer.getUid()) == null) {
					analysis.addChangeDelete(currentVersionTransformer);
				}
			}
		}
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Class<? extends ApplicationEntityService<ApplicationEntity>>[] getImportDependencies() {
		return new Class[] { EntityService.class };
	}
	
	@Override
	public void importObjects(TransferContext context, Session session) {
		Assert.notNull(context, C.CONTEXT);
		Assert.notNull(session, C.SESSION);
		try {
			if (context.getModule().getTransformers() != null) {
				for (Transformer transformer : context.getModule().getTransformers()) {
					initTransformer(transformer, context, session);
					saveObject(transformer, session);
				}
			}
		}
		catch (ValidationException vex) {
			throw new InternalException(vex);
		}
	}
	
	private void initTransformer(Transformer transformer, TransferContext context, Session session) {
		final Transformer currentVersionTransformer = findByUid(session, transformer.getUid());
		final Entity sourceEntity = entityService.findByUid(session, transformer.getSourceEntityUid());
		final Entity targetEntity = entityService.findByUid(session, transformer.getTargetEntityUid());
		((TransformerMetadata) transformer).setModule(context.getModule());
		((TransformerMetadata) transformer).setSourceEntity(sourceEntity);
		((TransformerMetadata) transformer).setTargetEntity(targetEntity);
		if (currentVersionTransformer != null) {
			((TransformerMetadata) currentVersionTransformer).copySystemFieldsTo(transformer);
			session.detach(currentVersionTransformer);
		}
		if (transformer.hasElements()) {
			for (TransformerElement element : transformer.getElements()) {
				initTransformerElement(element, transformer, sourceEntity, targetEntity, 
									   currentVersionTransformer);
			}
		}
		if (transformer.hasFunctions()) {
			for (TransformerFunction function : transformer.getFunctions()) {
				initTransformerFunction(function, transformer, currentVersionTransformer);
			}
		}
		if (transformer.hasPermissions()) {
			for (TransformerPermission permission : transformer.getPermissions()) {
				initTransformerPermission(permission, transformer, 
										  currentVersionTransformer, session);
			}
		}
	}
	
	private void initTransformerElement(TransformerElement element, Transformer transformer,
										Entity sourceEntity, Entity targetEntity, 
										Transformer currentVersionTransformer) {
		element.setTransformer(transformer);
		element.setSourceField(sourceEntity.findFieldByUid(element.getSourceFieldUid()));
		element.setTargetField(targetEntity.findFieldByUid(element.getTargetFieldUid()));
		final TransformerElement currentVersionElement =
			currentVersionTransformer != null 
				? currentVersionTransformer.getElementByUid(element.getUid()) 
				: null;
		if (currentVersionElement != null) {
			currentVersionElement.copySystemFieldsTo(element);
		}
	}
	
	private void initTransformerFunction(TransformerFunction function, Transformer transformer,
										 Transformer currentVersionTransformer) {
		function.setTransformer(transformer);
		final TransformerFunction currentVersionFunction =
			currentVersionTransformer != null
				? currentVersionTransformer.getFunctionByUid(function.getUid())
				: null;
		if (currentVersionFunction != null) {
			currentVersionFunction.copySystemFieldsTo(function);
		}
	}
	
	private void initTransformerPermission(TransformerPermission permission, Transformer transformer,
										   Transformer currentVersionTransformer, Session session) {
		permission.setTransformer(transformer);
		permission.setUserGroup(userGroupService.findByUid(session, permission.getUserGroupUid()));
		final TransformerPermission currentVersionPermission = 
			currentVersionTransformer != null
				? currentVersionTransformer.getPermissionByUid(permission.getUid())
				: null;
		if (currentVersionPermission != null) {
			currentVersionPermission.copySystemFieldsTo(permission);
		}
	}
	
	@Override
	public void deleteObjects(Module module, Module currentVersionModule, Session session) {
		Assert.notNull(module, C.MODULE);
		Assert.notNull(currentVersionModule, "currentVersionModule");
		Assert.notNull(session, C.SESSION);
		
		for (Transformer currentVersionTransformer : currentVersionModule.getTransformers()) {
			if (module.getTransformerByUid(currentVersionTransformer.getUid()) == null) {
				session.delete(currentVersionTransformer);
			}
		}
	}
	
	@Override
	public boolean autoMatchFields(Transformer transformer, List<TransformerElement> elements) {
		Assert.notNull(transformer, C.TRANSFORMER);
		Assert.notNull(elements, "elements");
		
		boolean matched = false;
		if (transformer.getSourceEntity().hasAllFields() &&
			transformer.getTargetEntity().hasAllFields()) {
			for (EntityField sourceField : transformer.getSourceEntity().getAllFields()) {
				for (EntityField targetField : transformer.getTargetEntity().getAllFields()) {
					if (sourceField.getType() == targetField.getType() &&
						sourceField.getName().equalsIgnoreCase(targetField.getName()) &&
						!containsElement(elements, sourceField, targetField)) {
						
						final TransformerElement element = createElement(sourceField, targetField);
						element.setTransformer(transformer);
						elements.add(element);
						matched = true;
						break;
					}
				}
			}
		}
		return matched;
	}
	
	@Override
	public boolean autoMatchFields(NestedTransformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		boolean matched = false;
		if (transformer.getSourceNested().getNestedEntity().hasAllFields() &&
			transformer.getTargetNested().getNestedEntity().hasAllFields()) {
			for (EntityField sourceField : transformer.getSourceNested().getFields(true)) {
				for (EntityField targetField : transformer.getTargetNested().getFields(true)) {
					if (sourceField.getType() == targetField.getType() &&
						sourceField.getName().equalsIgnoreCase(targetField.getName()) &&
						!transformer.containsElement(sourceField, targetField)) {
						
						transformer.addElement(createElement(sourceField, targetField));
						matched = true;
						break;
					}
				}
			}
		}
		return matched;
	}
	
	private static boolean containsElement(List<TransformerElement> elements, EntityField sourceField, EntityField targetField) {
		if (elements != null) {
			for (TransformerElement element : elements) {
				if (element.getSourceField() != null && element.getSourceField().equals(sourceField) &&
					element.getTargetField() != null && element.getTargetField().equals(targetField)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static TransformerElement createElement(EntityField sourceField, EntityField targetField) {
		final TransformerElement element = new TransformerElement();
		element.setSourceField(sourceField);
		element.setTargetField(targetField);
		return element;
	}

}
