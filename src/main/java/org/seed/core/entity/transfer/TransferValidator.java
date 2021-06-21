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
package org.seed.core.entity.transfer;

import java.util.Set;

import org.seed.C;
import org.seed.core.data.AbstractSystemEntityValidator;
import org.seed.core.data.FileObject;
import org.seed.core.data.ValidationError;
import org.seed.core.data.ValidationException;
import org.seed.core.util.Assert;

import org.springframework.stereotype.Component;

@Component
public class TransferValidator extends AbstractSystemEntityValidator<Transfer> {
	
	@Override
	public void validateCreate(Transfer transfer) throws ValidationException {
		Assert.notNull(transfer, C.TRANSFER);
		final Set<ValidationError> errors = createErrorList();
		
		if (isEmpty(transfer.getEntity())) {
			errors.add(ValidationError.emptyField("label.entity"));
		}
		if (isEmpty(transfer.getFormat())) {
			errors.add(ValidationError.emptyField("label.type"));
		}
	
		validate(errors);
	}
	
	public void validateImport(FileObject importFile) throws ValidationException {
		Assert.notNull(importFile, "importFile");
		final Set<ValidationError> errors = createErrorList();
		
		if (importFile.isEmpty()) {
			errors.add(ValidationError.emptyField("label.file"));
		}
		
		validate(errors);
	}
	
	@Override
	public void validateSave(Transfer transfer) throws ValidationException {
		Assert.notNull(transfer, C.TRANSFER);
		final Set<ValidationError> errors = createErrorList();
		
		if (isEmpty(transfer.getName())) {
			errors.add(ValidationError.emptyName());
		}
		else if (!isNameLengthAllowed(transfer.getName())) {
			errors.add(ValidationError.overlongName(getMaxNameLength()));
		}
		
		if (transfer.hasElements()) {
			boolean identifierFound = false;
			for (TransferElement element : transfer.getElements()) {
				if (element.isIdentifier()) {
					if (!identifierFound) {
						identifierFound = true;
					}
					else {
						errors.add(new ValidationError("val.ambiguous.identifier"));
						break;
					}
				}
			}
		}
		
		validate(errors);
	}
	
}
