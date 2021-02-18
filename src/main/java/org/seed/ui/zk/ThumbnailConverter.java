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
package org.seed.ui.zk;

import org.zkoss.bind.BindContext;
import org.zkoss.bind.Converter;
import org.zkoss.image.AImage;
import org.zkoss.zul.Image;

public class ThumbnailConverter implements Converter<AImage, byte[], Image> {
	
	private final int thumbnailWidth;
	
	public ThumbnailConverter(int thumbnailWidth) {
		this.thumbnailWidth = thumbnailWidth;
	}

	@Override
	public AImage coerceToUi(byte[] beanProp, Image component, BindContext ctx) {
		if (beanProp != null) {
			return ImageUtils.createThumbnail(beanProp, thumbnailWidth);
		}
		return null;
	}

	@Override
	public byte[] coerceToBean(AImage compAttr, Image component, BindContext ctx) {
		return compAttr.getByteData();
	}
	
}
