/*
# Copyright 2008 zylk.net
#
# This file is part of Sinadura.
#
# Sinadura is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 2 of the License, or
# (at your option) any later version.
#
# Sinadura is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Sinadura.  If not, see <http://www.gnu.org/licenses/>. [^]
#
# See COPYRIGHT.txt for copyright notices and details.
#
*/
package net.esle.sinadura.gui.view.preferences;

import java.util.Locale;
import java.util.StringTokenizer;

import net.esle.sinadura.gui.util.LanguageUtil;
import net.esle.sinadura.gui.util.PreferencesUtil;
import net.esle.sinadura.gui.util.PropertiesUtil;
import net.esle.sinadura.gui.view.main.InfoDialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * @author zylk.net
 */
public class GeneralPreferences extends FieldEditorPreferencePage {

	private static Log log = LogFactory.getLog(GeneralPreferences.class);
	
	private static final String	DISPLAY_LANGUAGE	= "EN";
	
	private GenericStringFieldEditor saveExtension = null; 
	
	private BooleanFieldEditor checkOutput = null;
	
	private GenericDirectoryFieldEditor outputDir2 = null; 
	
	private BooleanFieldEditor autoValidate = null;
	
	private BooleanFieldEditor statistics = null;

	/**
	 * @param messages
	 */
	public GeneralPreferences() {
		// Use the "grid" layout
		super(GRID);
	}

	/**
	 * Creates the field editors
	 */
	@Override
	protected void createFieldEditors() {

		StringTokenizer stringTokenizer = new StringTokenizer(PropertiesUtil.getConfiguration().getProperty(
				"idiomas.soportados"), ",");

		int numIdiomas = stringTokenizer.countTokens();

		String[][] comboFields = new String[numIdiomas][2];

		int index = 0;
		while (stringTokenizer.hasMoreElements()) {
			String idioma_value = (String) stringTokenizer.nextElement();
			StringTokenizer tokenizerIdiomaValue = new StringTokenizer(idioma_value, PreferencesUtil.TOKEN_LOCALE);
			String idioma = (String) tokenizerIdiomaValue.nextElement();
			String pais = (String) tokenizerIdiomaValue.nextElement();

			String idioma_label = new Locale(idioma, pais).getDisplayLanguage(new Locale(idioma, pais));
				// + " - " + new Locale(idioma, pais).getDisplayLanguage(new Locale(DISPLAY_LANGUAGE));
			
			if (idioma != null && idioma.equals("eu") && pais != null && pais.equalsIgnoreCase("es"))
				idioma_label = "Euskera";
			if (idioma!= null && idioma.equals("es") && pais != null && pais.equalsIgnoreCase("es"))
				idioma_label = "Castellano";

			String[] campo = { idioma_label.toLowerCase(), idioma_value };
			comboFields[index] = campo;
			index++;
		}

		ComboFieldEditor cfe = new ComboFieldEditor(PreferencesUtil.IDIOMA, LanguageUtil.getLanguage().getString("preferences.main.idiom"),
				comboFields, getFieldEditorParent());

		addField(cfe);
		
		checkOutput = new BooleanFieldEditor(PreferencesUtil.OUTPUT_AUTO_ENABLE, LanguageUtil.getLanguage().getString(
			"preferences.main.output.auto.enable"), getFieldEditorParent());
		addField(checkOutput);
		checkOutput.getDescriptionControl(getFieldEditorParent()).addListener(SWT.MouseUp, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				if (!checkOutput.getBooleanValue()) {
					outputDir2.setEnabled(true, getFieldEditorParent());
				} else {
					outputDir2.setEnabled(false, getFieldEditorParent());
				}
			}
		});
		
		// Add a directory field
		outputDir2 = new GenericDirectoryFieldEditor(PreferencesUtil.OUTPUT_DIR, LanguageUtil
				.getLanguage().getString("preferences.main.output_dir"), getFieldEditorParent());
		addField(outputDir2);
		
		saveExtension = new GenericStringFieldEditor(PreferencesUtil.SAVE_EXTENSION, LanguageUtil.getLanguage()
				.getString("preferences.main.extension"), getFieldEditorParent(), 150);
		addField(saveExtension);
		
		if (PreferencesUtil.getPreferences().getString(PreferencesUtil.OUTPUT_AUTO_ENABLE).equals("true")) {
			outputDir2.setEnabled(false, getFieldEditorParent());
		} else {
			outputDir2.setEnabled(true, getFieldEditorParent());
		}
		
		autoValidate = new BooleanFieldEditor(PreferencesUtil.AUTO_VALIDATE, LanguageUtil.getLanguage().getString(
			"preferences.main.auto.validate"), getFieldEditorParent());
		addField(autoValidate);
		
		statistics = new BooleanFieldEditor(PreferencesUtil.ENABLE_STATISTICS, LanguageUtil.getLanguage().getString(
			"preferences.main.enable.statistics"), getFieldEditorParent());
		addField(statistics);
	}
 	
	@Override
	public boolean performOk() {

		boolean ok = false;

		if (saveExtension != null && (saveExtension.getStringValue() == null || saveExtension.getStringValue().equals(""))) {

			log.error(LanguageUtil.getLanguage().getString("error.save_extension.empty"));
			InfoDialog id = new InfoDialog(this.getShell());
			id.open(LanguageUtil.getLanguage().getString("error.save_extension.empty"));

		} else {
			ok = super.performOk();
			LanguageUtil.reloadLanguage();
		}

		return ok;
	}
}