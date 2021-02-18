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
package org.seed.ui.zk.vm.admin;

import org.seed.core.entity.EntityFunction;
import org.seed.core.entity.transform.TransformerFunction;
import org.seed.core.task.Task;

import org.springframework.util.Assert;

class CodeDialogParameter {
	
	final AbstractAdminViewModel<?> parentViewModel;
	
	final EntityFunction entityFunction;
	
	final TransformerFunction transformerFunction;
	
	final Task task;
	
	public CodeDialogParameter(AbstractAdminViewModel<?> parentViewModel, EntityFunction entityFunction) {
		Assert.notNull(parentViewModel, "parentViewModel is null");
		Assert.notNull(entityFunction, "entityFunction is null");
		
		this.parentViewModel = parentViewModel;
		this.entityFunction = entityFunction;
		this.transformerFunction = null;
		this.task = null;
	}
	
	public CodeDialogParameter(AbstractAdminViewModel<?> parentViewModel, TransformerFunction transformerFunction) {
		Assert.notNull(parentViewModel, "parentViewModel is null");
		Assert.notNull(transformerFunction, "entityFunction is null");
		
		this.parentViewModel = parentViewModel;
		this.entityFunction = null;
		this.transformerFunction = transformerFunction;
		this.task = null;
	}
	
	public CodeDialogParameter(AbstractAdminViewModel<?> parentViewModel, Task task) {
		Assert.notNull(parentViewModel, "parentViewModel is null");
		Assert.notNull(task, "task is null");
		
		this.parentViewModel = parentViewModel;
		this.entityFunction = null;
		this.transformerFunction = null;
		this.task = task;
	}
	
}
