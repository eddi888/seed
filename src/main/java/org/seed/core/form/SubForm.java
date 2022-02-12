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
package org.seed.core.form;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import org.seed.C;
import org.seed.core.application.AbstractApplicationEntity;
import org.seed.core.application.AbstractTransferableObject;
import org.seed.core.data.Order;
import org.seed.core.entity.EntityField;
import org.seed.core.entity.EntityFunction;
import org.seed.core.entity.NestedEntity;
import org.seed.core.entity.filter.Filter;
import org.seed.core.entity.transform.Transformer;
import org.seed.core.entity.value.ValueObject;
import org.seed.core.util.Assert;

import org.springframework.util.ObjectUtils;

@Entity
@Table(name = "sys_subform")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SubForm extends AbstractTransferableObject {

	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id")
	private FormMetadata form;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nested_entity_id")
	private NestedEntity nestedEntity;
	
	@OneToMany(mappedBy = "subForm",
			   cascade = CascadeType.ALL,
			   orphanRemoval = true,
			   fetch = FetchType.LAZY)
	@OrderBy("order")
	private List<SubFormField> fields;
	
	@OneToMany(mappedBy = "subForm",
			   cascade = CascadeType.ALL,
			   orphanRemoval = true,
			   fetch = FetchType.LAZY)
	@OrderBy("order")
	private List<SubFormAction> actions;
	
	@Transient
	private String nestedEntityUid;
	
	@Transient
	private ValueObject selectedObject;
	
	@XmlTransient
	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = (FormMetadata) form;
	}
	
	@XmlTransient
	public NestedEntity getNestedEntity() {
		return nestedEntity;
	}

	public void setNestedEntity(NestedEntity nestedEntity) {
		this.nestedEntity = nestedEntity;
	}
	
	@XmlTransient
	public String getNestedEntityUid() {
		return nestedEntity != null ? nestedEntity.getUid() : nestedEntityUid;
	}

	public void setNestedEntityUid(String nestedEntityUid) {
		this.nestedEntityUid = nestedEntityUid;
	}

	@XmlTransient
	public ValueObject getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(ValueObject selectedObject) {
		this.selectedObject = selectedObject;
	}
	
	public boolean hasFields() {
		return !ObjectUtils.isEmpty(getFields());
	}
	
	public List<SubFormField> getFields() {
		return fields;
	}

	public void setFields(List<SubFormField> fields) {
		this.fields = fields;
	}
	
	public void addField(SubFormField field) {
		Assert.notNull(field, C.FIELD);
		
		if (fields == null) {
			fields = new ArrayList<>();
		}
		fields.add(field);
	}
	
	public boolean hasActions() {
		return !ObjectUtils.isEmpty(getActions());
	}
	
	public List<SubFormAction> getActions() {
		return actions;
	}
	
	public void addAction(SubFormAction action) {
		Assert.notNull(action, C.ACTION);
		
		if (actions == null) {
			actions = new ArrayList<>();
		}
		actions.add(action);
	}
	
	public SubFormField getFieldByUid(String fieldUid) {
		Assert.notNull(fieldUid, "fieldUid");
		
		return AbstractApplicationEntity.getObjectByUid(getFields(), fieldUid);
	}
	
	public SubFormAction getActionByUid(String actionUid) {
		Assert.notNull(actionUid, "actionUid");
		
		return AbstractApplicationEntity.getObjectByUid(getActions(), actionUid);
	}
	
	public SubFormField getFieldByEntityFieldUid(String entityFieldUid) {
		Assert.notNull(entityFieldUid, "entityFieldUid");
		
		if (hasFields()) {
			for (SubFormField field : getFields()) {
				if (entityFieldUid.equals(field.getEntityField().getUid())) {
					return field;
				}
			}
		}
		return null;
	}
	
	public boolean containsForm(Form form) {
		Assert.notNull(form, C.FORM);
		
		if (hasFields()) {
			for (SubFormField subFormField : getFields()) {
				if (form.equals(subFormField.getDetailForm())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsEntityField(EntityField entityField) {
		Assert.notNull(entityField, C.ENTITYFIELD);
		
		if (hasFields()) {
			for (SubFormField subFormField : getFields()) {
				if (entityField.equals(subFormField.getEntityField())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsEntityFunction(EntityFunction entityFunction) {
		Assert.notNull(entityFunction, "entityFunction");
		
		if (hasActions()) {
			for (SubFormAction action : getActions()) {
				if (entityFunction.equals(action.getEntityFunction())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsTransformer(Transformer transformer) {
		Assert.notNull(transformer, C.TRANSFORMER);
		
		if (hasFields()) {
			for (SubFormField field : getFields()) {
				if (transformer.equals(field.getTransformer())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean containsFilter(Filter filter) {
		Assert.notNull(filter, C.FILTER);
		
		if (hasFields()) {
			for (SubFormField field : getFields()) {
				if (filter.equals(field.getFilter())) {
					return true;
				}
			}
		}
		return false;
	}
	
	public SubFormAction getActionByType(FormActionType actionType) {
		if (hasActions()) {
			return getActionByType(getActions(), actionType);
		}
		return null;
	}
	
	public SubFormAction getActionByType(List<SubFormAction> actions, FormActionType actionType) {
		Assert.notNull(actions, "actions");
		Assert.notNull(actionType, "actionType");
		
		for (SubFormAction action : actions) {
			if (action.getType() == actionType) {
				return action;
			}
		}
		return null;
	}
	
	@Override
	public boolean isEqual(Object other) {
		if (other == null || !SubForm.class.isAssignableFrom(other.getClass())) {
			return false;
		}
		if (other == this) {
			return true;
		}
		final SubForm otherSubForm = (SubForm) other;
		if (!new EqualsBuilder()
			.append(nestedEntityUid, otherSubForm.getNestedEntityUid())
			.isEquals()) {
			return false;
		}
		return isEqualFields(otherSubForm) &&
			   isEqualActions(otherSubForm);
	}
	
	private boolean isEqualFields(SubForm otherSubForm) {
		if (hasFields()) {
			for (SubFormField field : getFields()) {
				if (!field.isEqual(otherSubForm.getFieldByUid(field.getUid()))) {
					return false;
				}
			}
		}
		if (otherSubForm.hasFields()) {
			for (SubFormField otherField : otherSubForm.getFields()) {
				if (getFieldByUid(otherField.getUid()) == null) {
					return false;
				}
			}
 		}
		return true;
	}
	
	private boolean isEqualActions(SubForm otherSubForm) {
		if (hasActions()) {
			for (SubFormAction action : getActions()) {
				if (!action.isEqual(otherSubForm.getActionByUid(action.getUid()))) {
					return false;
				}
			}
		}
		if (otherSubForm.hasActions()) {
			for (SubFormAction otherAction : otherSubForm.getActions()) {
				if (getActionByUid(otherAction.getUid()) == null) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	protected void setOrderIndexes() {
		Order.setOrderIndexes(getFields());
		Order.setOrderIndexes(getActions());
	}
	
}
