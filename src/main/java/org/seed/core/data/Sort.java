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
package org.seed.core.data;

import org.springframework.util.Assert;

public final class Sort {

	private final boolean ascending;
	
	private final String columnName;

	public Sort(String columnName, boolean ascending) {
		Assert.notNull(columnName, "columnName is null");
		
		this.columnName = columnName;
		this.ascending = ascending;
	}

	public boolean isAscending() {
		return ascending;
	}

	public String getColumnName() {
		return columnName;
	}

}
