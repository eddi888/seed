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
package org.seed.core.report.generate;

import org.seed.core.report.Report;
import org.seed.core.report.ReportFormat;
import org.seed.core.report.ReportGenerator;
import org.seed.core.report.ReportGeneratorProvider;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class ReportGeneratorFactory implements ReportGeneratorProvider {

	@Override
	public ReportGenerator getGenerator(Report report, ReportFormat format) {
		Assert.notNull(format, "format is null");
		
		switch (format) {
			case EXCEL:
				return new ExcelReportGenerator(report);
			case PDF:
				return new PDFReportGenerator(report);
			default:
				throw new UnsupportedOperationException(format.name());
		}
	}

}
