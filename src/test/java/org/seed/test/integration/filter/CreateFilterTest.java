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
package org.seed.test.integration.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.openqa.selenium.WebElement;

@TestMethodOrder(OrderAnnotation.class)
public class CreateFilterTest extends AbstractFilterTest {
	
	@Test
	@Order(1)
	void testCreateFilter() {
		openMenu("administration-entitaeten");
		clickMenu("administration-entitaeten-filter");
		assertEquals("Filter", findTab("filter").getText());
		
		WebElement tabpanel = findTabpanel("filter");
		clickButton(tabpanel, "new");
		
		WebElement window = findWindow("new-filter");
		assertEquals("Neuen Filter erstellen", findWindowHeader(window).getText());
		findCombobox(window, "entity").sendKeys("IntegrationTest");
		findCombobox(window, "module").sendKeys("Testmodule");
		clickButton(window, "create");
		
		clickButton(tabpanel, "save");
		findValidationMessage(); // name is empty
		findTextbox(tabpanel, "name").sendKeys("Testfilter");
		saveFilter(tabpanel);
	}
	
	@Test
	@Order(2)
	void testAddCriteria() {
		WebElement tabpanel = showFilter("testfilter");
		assertEquals("Filterkriterien", findTab(tabpanel, "criteria").getText());
		WebElement tabpanelCriteria = findTabpanel(tabpanel, "criteria");
		clickButton(tabpanelCriteria, "new");
		
		findOptionCombobox(tabpanelCriteria, "field").sendKeys("Textfield");
		findOptionCombobox(tabpanelCriteria, "operator").sendKeys("gleich");
		clickTab(tabpanel, "criteria"); // lose focus
		findOptionTextbox(tabpanelCriteria, "stringvalue").sendKeys("test");
		saveFilter(tabpanel);
	}
	
	@Test
	@Order(3)
	void testAddPermission() {
		WebElement tabpanel = showFilter("testfilter");
		assertEquals("Berechtigungen", findTab(tabpanel, "permissions").getText());
		clickTab(tabpanel, "permissions");
		WebElement tabpanelPermissions = findTabpanel(tabpanel, "permissions");
		dragAndDrop(tabpanelPermissions, "testrole", "selected");
		saveFilter(tabpanel);
	}
	
}
