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
package org.seed.core.entity.value.event;

import org.hibernate.Session;
import org.seed.InternalException;
import org.seed.core.api.ApplicationException;
import org.seed.core.api.CallbackFunction;
import org.seed.core.codegen.CodeManager;
import org.seed.core.codegen.GeneratedCode;
import org.seed.core.entity.Entity;
import org.seed.core.entity.EntityFunction;
import org.seed.core.entity.EntityRepository;
import org.seed.core.entity.EntityStatusTransition;
import org.seed.core.entity.EntityStatusTransitionFunction;
import org.seed.core.entity.value.ValueObject;
import org.seed.core.util.Assert;
import org.seed.core.util.MiscUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ValueObjectEventHandler {
	
	private static final Logger log = LoggerFactory.getLogger(ValueObjectEventHandler.class);
	
	@Autowired
	private EntityRepository entityRepository;
	
	@Autowired
	private CodeManager codeManager;
	
	public boolean processEvent(ValueObjectEvent event) {
		Assert.notNull(event, "event");
		Assert.state(event.type != ValueObjectEventType.USERACTION, "use processUserEvent instead");
		
		switch (event.type) {
			case BEFORETRANSITION:
			case AFTERTRANSITION:
				return processStatusTransitionEvent(event);
			default:
				return processEntityEvent(event);
		}
	}
	
	public String processUserEvent(ValueObjectEvent event) {
		Assert.notNull(event, "event");
		final EntityFunction entityFunction = event.entityFunction;
		Assert.stateAvailable(entityFunction, "entity function");
		
		return callFunction(entityFunction.getEntity(), entityFunction, event.object, event.session, null, null);
	}
	
	private boolean processStatusTransitionEvent(ValueObjectEvent event) {
		boolean functionExecuted = false;
		final EntityStatusTransition statusTransition = event.statusTransition;
		Assert.stateAvailable(statusTransition, "status transition");
		final Entity entity = statusTransition.getEntity();
		
		if (statusTransition.hasFunctions()) {
			for (EntityStatusTransitionFunction transitionFunction : statusTransition.getFunctions()) {
				if (!transitionFunction.getFunction().isActive()) {
					continue;
				}
				boolean execute = false;
				switch (event.type) {
					case BEFORETRANSITION:
						execute = transitionFunction.isActiveBeforeTransition();
						break;
					case AFTERTRANSITION:
						execute = transitionFunction.isActiveAfterTransition();
						break;
					default:
						throw new UnsupportedOperationException(event.type.name());
				}
				if (execute) {
					callFunction(entity, transitionFunction.getFunction(), event.object, 
								 event.session, event.functionContext, statusTransition);
					functionExecuted = true;
				}
			}
		}
		return functionExecuted;
	}
	
	private boolean processEntityEvent(ValueObjectEvent event) {
		boolean functionExecuted = false;
		final Entity entity = entityRepository.get(event.object.getEntityId());
		
		if (entity.hasFunctions()) {
			for (EntityFunction function : entity.getFunctions()) {
				if (!function.isActive()) {
					continue;
				}
				boolean execute = false;
				switch (event.type) {
					case CREATE:
						execute = function.isActiveOnCreate();
						break;
					case MODIFY:
						execute = function.isActiveOnModify();
						break;
					case BEFOREINSERT:
						execute = function.isActiveBeforeInsert();
						break;
					case AFTERINSERT:
						execute = function.isActiveAfterInsert();
						break;
					case BEFOREUPDATE:
						execute = function.isActiveBeforeUpdate();
						break;
					case AFTERUPDATE:
						execute = function.isActiveAfterUpdate();
						break;
					case BEFOREDELETE:
						execute = function.isActiveBeforeDelete();
						break;
					case AFTERDELETE:
						execute = function.isActiveAfterDelete();
						break;
					default:
						throw new UnsupportedOperationException(event.type.name());
				}
				if (execute) {
					callFunction(entity, function, event.object, event.session, 
								 event.functionContext, null);
					functionExecuted = true;
				}
			}
		}
		return functionExecuted;
	}
	
	@SuppressWarnings("unchecked")
	private String callFunction(Entity entity, EntityFunction function, ValueObject object, Session session, 
							  ValueObjectFunctionContext functionContext, EntityStatusTransition statusTransition) {
		Assert.state(!(session == null && functionContext == null), "no session or functionContext provided");
		Assert.state(!(session != null && functionContext != null), "only session or functionContext allowed");
		
		final Class<GeneratedCode> functionClass = codeManager.getGeneratedClass(function);
		if (functionClass == null) {
			throw new IllegalStateException("function class not available: " + function.getGeneratedPackage() + '.' + function.getGeneratedClass());
		}
		try {
			final CallbackFunction<ValueObject> callbackFunction = (CallbackFunction<ValueObject>) MiscUtils.instantiate(functionClass);
			if (functionContext == null) {
				functionContext = new ValueObjectFunctionContext(session, entity.getModule(), statusTransition);
			}
			log.debug("Execute function '{}' on {} id:{}", function.getName(), function.getEntity().getName(), object.getId());
			callbackFunction.call(object, functionContext);
			return functionContext.getSuccessMessage();
		}
		catch (ApplicationException appex) {
			throw appex;
		}
		catch (Exception ex) {
			throw new InternalException(ex);
		}
	}
	
}
