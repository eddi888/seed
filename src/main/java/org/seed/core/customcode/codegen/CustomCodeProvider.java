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
package org.seed.core.customcode.codegen;

import java.util.ArrayList;
import java.util.List;

import org.seed.core.codegen.SourceCode;
import org.seed.core.codegen.SourceCodeBuilder;
import org.seed.core.codegen.SourceCodeProvider;
import org.seed.core.customcode.CustomCode;
import org.seed.core.customcode.CustomCodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomCodeProvider implements SourceCodeProvider {

	@Autowired
	private CustomCodeService customCodeService;
	
	public SourceCode getCustomCodeSource(CustomCode customCode) {
		return new CustomCodeBuilder(customCode).build();
	}
	
	@Override
	public List<SourceCodeBuilder> getSourceCodeBuilders() {
		final List<SourceCodeBuilder> result = new ArrayList<>();
		for (CustomCode customCode : customCodeService.findAllObjects()) {
			result.add(new CustomCodeBuilder(customCode));
		}
		return result;
	}

}
