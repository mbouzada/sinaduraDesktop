/*
 * # Copyright 2008 zylk.net 
 * # 
 * # This file is part of Sinadura. 
 * # 
 * # Sinadura is free software: you can redistribute it and/or modify 
 * # it under the terms of the GNU General Public License as published by 
 * # the Free Software Foundation, either version 2 of the License, or 
 * # (at your option) any later version. 
 * # 
 * # Sinadura is distributed in the hope that it will be useful, 
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * # GNU General Public License for more details. 
 * # 
 * # You should have received a copy of the GNU General Public License 
 * # along with Sinadura. If not, see <http://www.gnu.org/licenses/>. [^] 
 * # 
 * # See COPYRIGHT.txt for copyright notices and details. 
 * #
 */
package net.esle.sinadura.gui.view.preferences;

import java.util.Map;

import net.esle.sinadura.gui.util.LanguageUtil;
import net.esle.sinadura.gui.util.PreferencesUtil;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * @author zylk.net
 */

public class SignPreferences extends FieldEditorPreferencePage {

	private BooleanFieldEditor checkTSEnable = null;
	private BooleanFieldEditor checkOCSP = null;

	/**
	 * @param messages
	 */
	public SignPreferences() {
		super(GRID);
	}

	/**
	 * Creates the field editors
	 */
	@Override
	protected void createFieldEditors() {

		
		this.checkTSEnable = new BooleanFieldEditor(PreferencesUtil.SIGN_TS_ENABLE, LanguageUtil.getLanguage().getString(
				"preferences.sign.ts.enable"), getFieldEditorParent());
		addField(this.checkTSEnable);

		Map<String, String> map = PreferencesUtil.getTimestampPreferences();
		String[][] comboFields = new String[map.size()][2];
		int i = 0;
		for (String name : map.keySet()) {
			String[] campo2 = { name, name };
			comboFields[i] = campo2;
			i++;
		}

		Composite composite = new Composite(getFieldEditorParent(), SWT.NONE);
		composite.setLayoutData(new GridData(GridData.BEGINNING));
		
		ComboFieldEditor comboTSA = new ComboFieldEditor(PreferencesUtil.SIGN_TS_TSA, LanguageUtil.getLanguage().getString("preferences.sign.ts.tsa"),
				comboFields, composite);
		addField(comboTSA);

		this.checkOCSP = new BooleanFieldEditor(PreferencesUtil.SIGN_OCSP_ENABLE, LanguageUtil.getLanguage().getString(
				"preferences.sign.ocsp.enable"), getFieldEditorParent());
		
		boolean tsSelected = PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_TS_ENABLE);
		if (!tsSelected) {
			this.checkOCSP.setEnabled(false, getFieldEditorParent());
		} else {
			this.checkOCSP.setEnabled(true, getFieldEditorParent());
		}
		addField(this.checkOCSP);

		
		this.checkTSEnable.getDescriptionControl(getFieldEditorParent()).addListener(SWT.MouseUp, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (!checkTSEnable.getBooleanValue()) {
					checkOCSP.setEnabled(false, getFieldEditorParent());
					PreferencesUtil.getPreferences().setValue(PreferencesUtil.SIGN_OCSP_ENABLE, false);
					checkOCSP.load();
				} else {
					checkOCSP.setEnabled(true, getFieldEditorParent());
				}
			}
		});
	}
}