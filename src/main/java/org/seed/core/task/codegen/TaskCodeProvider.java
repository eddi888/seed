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
package org.seed.core.task.codegen;

import java.util.ArrayList;
import java.util.List;

import org.seed.core.codegen.SourceCode;
import org.seed.core.codegen.SourceCodeBuilder;
import org.seed.core.codegen.SourceCodeBuilder.BuildMode;
import org.seed.core.codegen.SourceCodeProvider;
import org.seed.core.task.Task;
import org.seed.core.task.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskCodeProvider implements SourceCodeProvider<Task> {
	
	@Autowired
	private TaskRepository taskRepository;
	
	public String getFunctionTemplate(Task task) {
		return new TaskCodeBuilder(task).build(BuildMode.TEMPLATE).getContent();
	}
	
	public SourceCode<Task> getTaskSource(Task task) {
		return new TaskCodeBuilder(task).build();
	}
	
	@Override
	public List<SourceCodeBuilder<Task>> getSourceCodeBuilders() {
		final List<SourceCodeBuilder<Task>> result = new ArrayList<>();
		for (Task task : taskRepository.find()) {
			result.add(new TaskCodeBuilder(task));
		}
		return result;
	}
	
}
