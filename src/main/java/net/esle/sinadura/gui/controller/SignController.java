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
package net.esle.sinadura.gui.controller;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.esle.sinadura.core.exceptions.ConnectionException;
import net.esle.sinadura.core.exceptions.CoreException;
import net.esle.sinadura.core.exceptions.CorePKCS12Exception;
import net.esle.sinadura.core.exceptions.OCSPCoreException;
import net.esle.sinadura.core.exceptions.OCSPIssuerRequiredException;
import net.esle.sinadura.core.exceptions.OCSPUnknownUrlException;
import net.esle.sinadura.core.exceptions.PKCS11Exception;
import net.esle.sinadura.core.exceptions.PdfSignatureException;
import net.esle.sinadura.core.exceptions.RevokedException;
import net.esle.sinadura.core.exceptions.XadesSignatureException;
import net.esle.sinadura.core.keystore.KeyStoreBuilderFactory;
import net.esle.sinadura.core.keystore.KeyStoreBuilderFactory.KeyStoreTypes;
import net.esle.sinadura.core.keystore.PKCS11Helper;
import net.esle.sinadura.core.model.KsSignaturePreferences;
import net.esle.sinadura.core.model.PdfSignaturePreferences;
import net.esle.sinadura.core.model.XadesSignaturePreferences;
import net.esle.sinadura.core.password.DummyCallbackHandler;
import net.esle.sinadura.core.password.PasswordExtractor;
import net.esle.sinadura.core.service.PdfService;
import net.esle.sinadura.core.service.XadesService;
import net.esle.sinadura.core.util.FileUtil;
import net.esle.sinadura.gui.events.ProgressWriter;
import net.esle.sinadura.gui.exceptions.AliasesNotFoundException;
import net.esle.sinadura.gui.exceptions.DriversNotFoundException;
import net.esle.sinadura.gui.exceptions.OverwritingException;
import net.esle.sinadura.gui.exceptions.SignProgressInterruptedException;
import net.esle.sinadura.gui.model.DocumentInfo;
import net.esle.sinadura.gui.util.LanguageUtil;
import net.esle.sinadura.gui.util.PreferencesUtil;
import net.esle.sinadura.gui.util.StatisticsUtil;
import net.esle.sinadura.gui.view.main.SlotDialog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Image;

public class SignController {

	private static final Log log = LogFactory.getLog(SignController.class);

	public static Map<String, Long> loadSlot() throws NoSuchAlgorithmException, KeyStoreException, PKCS11Exception,
			CoreException, CorePKCS12Exception, DriversNotFoundException {

		String certificadoType = PreferencesUtil.getPreferences().getString(PreferencesUtil.CERT_TYPE);

		// compruebo las preferencias y cargo el certificado del dispositivo
		// hardware, o del el file-system
		Map<String, Long> slotsByReader = new HashMap<String, Long>();
		
		if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_HARDWARE)) 
		{
			if (!System.getProperty("os.name").toLowerCase().contains("win")) {
			
				String pkcs11Path = PreferencesUtil.getDefaultHardware();
				PKCS11Helper pk11h = new PKCS11Helper(pkcs11Path, "");
				// TODO revisar si es necesario
				// long[] slots = null;
				// slots = pk11h.getSignatureCapableSlots();
				slotsByReader = pk11h.getSoltsByReaderName();
			}

		} else if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_SOFTWARE)) {

			//do nothing
		}

		else if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_MSCAPI)) {

			//do nothing

		}

		return slotsByReader;
	}

	public static KsSignaturePreferences loadKeyStore(Shell sShell, String slot) throws NoSuchAlgorithmException, KeyStoreException, PKCS11Exception,
			CoreException, CorePKCS12Exception, DriversNotFoundException {

		String certificadoType = PreferencesUtil.getPreferences().getString(PreferencesUtil.CERT_TYPE);

		PasswordCallbackHandlerDialog o = new PasswordCallbackHandlerDialog(sShell);
		PasswordExtractor pe = (PasswordExtractor) o;

		KsSignaturePreferences ksSignaturePreferences = new KsSignaturePreferences();
		KeyStore ks = null;

		// compruebo las preferencias y cargo el certificado del dispositivo
		// hardware, o del el file-system
		if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_HARDWARE)) {

			String pkcs11Path = PreferencesUtil.getDefaultHardware();
			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_CERTTYPE, StatisticsUtil.VALUE_HARD);
			StatisticsUtil.log(StatisticsUtil.KEY_LOAD_HARDWAREDRIVER, pkcs11Path);

			
			
			ks = KeyStoreBuilderFactory.getKeyStore("HARD", KeyStoreTypes.PKCS11, pkcs11Path, slot, new KeyStore.CallbackHandlerProtection(
					o));

		} else if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_SOFTWARE)) {

			String pkcs12Path = PreferencesUtil.getSoftwarePreferences().get(
					PreferencesUtil.getPreferences().getString(PreferencesUtil.SOFTWARE_DISPOSITIVE));
			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_CERTTYPE, StatisticsUtil.VALUE_SOFT);
			ks = KeyStoreBuilderFactory.getKeyStore("SOFT", KeyStoreTypes.PKCS12, pkcs12Path, new KeyStore.CallbackHandlerProtection(o));

		}

		else if (certificadoType.equalsIgnoreCase(PreferencesUtil.CERT_TYPE_VALUE_MSCAPI)) {

			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_CERTTYPE, StatisticsUtil.VALUE_MSCAPI);
			DummyCallbackHandler a = new DummyCallbackHandler(null);
			pe = (PasswordExtractor) a;
			ks = KeyStoreBuilderFactory.getKeyStore("MSCAPI", KeyStoreTypes.MSCAPI, null, new KeyStore.CallbackHandlerProtection(a));

		}

		ksSignaturePreferences.setKs(ks);

		// fijo el passwordprotection para el PKCS12, para el PKCS11 no es
		// necesario pero por coherencia lo uso tambien.
		ksSignaturePreferences.setPasswordProtection(pe.getPasswordProtection());

		return ksSignaturePreferences;
	}

	public static void logout(KeyStore ks, String alias) {

		// una vez que se ha firmado... hago un logout de la session del
		// provider
		KeyStoreBuilderFactory.logout(ks, alias);
	}

	public static List<String> getAlias(KeyStore ks) throws AliasesNotFoundException, KeyStoreException {

		List<String> list = new ArrayList<String>();

		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			String string = aliases.nextElement();
			list.add(string);
		}
		if (list.size() == 0) {
			throw new AliasesNotFoundException();
		}

		return list;
	}

	public static void sign(DocumentInfo pdfParameter, KsSignaturePreferences ksSignaturePreferences) throws OCSPCoreException,
			RevokedException, ConnectionException, CertificateExpiredException, CertificateNotYetValidException,
			OCSPIssuerRequiredException, OCSPUnknownUrlException {

		try {
			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_DOCUMENT_EXTENSION, FileUtil.getExtension(pdfParameter.getPath()));
			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_MIMETYPE, pdfParameter.getMimeType());

			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_DOCUMENT_SIZE, new File(pdfParameter.getPath()).length() + "");

			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_TSA, PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_TS_ENABLE)
					+ "");

			// firma
			if (pdfParameter.getMimeType() != null && pdfParameter.getMimeType().equals(FileUtil.MIMETYPE_PDF)) {
				signPDF(pdfParameter, ksSignaturePreferences);
			} else if (pdfParameter.getMimeType() != null && pdfParameter.getMimeType().equals(FileUtil.MIMETYPE_XML)) {
				// TODO firma de xml enveloped
				signDetached(pdfParameter, ksSignaturePreferences);
			} else if (pdfParameter.getMimeType() != null && pdfParameter.getMimeType().equals(FileUtil.MIMETYPE_SAR)) {
				signDetached(pdfParameter, ksSignaturePreferences);
			} else {
				signDetached(pdfParameter, ksSignaturePreferences);
			}
		} catch (OverwritingException e) {

			File file = new File(pdfParameter.getPath());
			String fileDestino = PreferencesUtil.getOutputDir(file) + File.separatorChar + PreferencesUtil.getOutputName(file.getName());

			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("error.overwrite"), pdfParameter.getPath(), fileDestino);

			log.error(m, e);
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.ERROR, m));

		} catch (IOException e) {

			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("error.certificate.sign.unexpected"),
					pdfParameter.getPath(), e.toString());
			log.error(m, e);
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.ERROR, m));
		}
	}

	private static void signPDF(DocumentInfo pdfParameter, KsSignaturePreferences ksSignaturePreferences) throws OCSPCoreException,
			RevokedException, OverwritingException, ConnectionException, IOException, CertificateExpiredException,
			CertificateNotYetValidException, OCSPIssuerRequiredException, OCSPUnknownUrlException {

		try {

			PdfSignaturePreferences signaturePreferences = new PdfSignaturePreferences();
			signaturePreferences.setKsSignaturePreferences(ksSignaturePreferences);

			signaturePreferences.setKsCache(PreferencesUtil.getCacheKeystoreComplete());

			String reason = PreferencesUtil.getPreferences().getString(PreferencesUtil.PDF_REASON);
			signaturePreferences.setReason(reason);

			String location = PreferencesUtil.getPreferences().getString(PreferencesUtil.PDF_LOCATION);
			signaturePreferences.setLocation(location);

			boolean selloVisible = PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.PDF_VISIBLE);
			signaturePreferences.setVisible(selloVisible);

			signaturePreferences.setPage(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_PAGE));

			signaturePreferences.setStartX(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_STAMP_X));
			signaturePreferences.setStartY(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_STAMP_Y));
			signaturePreferences.setWidht(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_STAMP_WIDTH));
			signaturePreferences.setHeight(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_STAMP_HEIGHT));

			Image sello = null;
			if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.PDF_STAMP_ENABLE)) {
				try {
					sello = Image.getInstance(PreferencesUtil.getPreferences().getString(PreferencesUtil.PDF_STAMP_PATH));

				} catch (BadElementException e) {
					log.error("", e);

				} catch (MalformedURLException e) {
					log.error("", e);

				} catch (IOException e) {
					log.error("", e);
				}
			}
			signaturePreferences.setImage(sello);

			signaturePreferences.setCertified(PreferencesUtil.getPreferences().getInt(PreferencesUtil.PDF_CERTIFIED));

			String tsurl = null;
			if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_TS_ENABLE) == true) {
				tsurl = PreferencesUtil.getTimestampPreferences().get(
						PreferencesUtil.getPreferences().getString(PreferencesUtil.SIGN_TS_TSA));
			}
			signaturePreferences.setTimestampUrl(tsurl);
			signaturePreferences.setTimestampUser(null);
			signaturePreferences.setTimestampPassword(null);

			boolean addOCSP = PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_OCSP_ENABLE);
			signaturePreferences.setAddOCSP(addOCSP);

			StatisticsUtil.log(StatisticsUtil.KEY_SIGN_OCSP, addOCSP + "");

			File file = new File(pdfParameter.getPath());
			String outputPath = PreferencesUtil.getOutputDir(file) + File.separatorChar + PreferencesUtil.getOutputName(file.getName())
					+ "." + FileUtil.EXTENSION_PDF;
			File outputFile = new File(outputPath);
			if (outputFile.exists()) {
				throw new OverwritingException();
			}

			// firmar
			byte[] bytes = PdfService.sign(pdfParameter.getPath(), signaturePreferences);
			FileUtil.bytesToFile(bytes, outputFile);

			// TODO centralizar esto
			// actualizo la entrada de la tabla
			pdfParameter.setPath(outputPath);
			pdfParameter.setSignatures(null);
			String mimeType = FileUtil.getMimeType(outputPath);
			pdfParameter.setMimeType(mimeType);

			// validar
			if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.AUTO_VALIDATE)) {
				ValidateController.validate(pdfParameter);
			}

			// mensaje
			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("info.document.signed"), pdfParameter.getPath());
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.INFO, m));

		} catch (PdfSignatureException e) {

			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("error.certificate.sign.unexpected"),
					pdfParameter.getPath(), e.getMessage());
			log.error(m, e);
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.ERROR, m));
		}
	}

	private static void signDetached(DocumentInfo pdfParameter, KsSignaturePreferences ksSignaturePreferences) throws OverwritingException,
			IOException, OCSPUnknownUrlException, CertificateExpiredException, CertificateNotYetValidException, RevokedException,
			OCSPCoreException, ConnectionException, OCSPIssuerRequiredException {

		try {
			// firmar
			byte[] bytes = signXades(pdfParameter.getPath(), ksSignaturePreferences);

			File inputFile = new File(pdfParameter.getPath());
			String outputPath = null;
			if (pdfParameter.getMimeType() != null && pdfParameter.getMimeType().equals(FileUtil.MIMETYPE_SAR)) {

				outputPath = PreferencesUtil.getOutputDir(inputFile) + File.separatorChar
						+ PreferencesUtil.getOutputName(inputFile.getName()) + "." + FileUtil.EXTENSION_SAR;

			} else {
				if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.XADES_ARCHIVE)) {

					outputPath = PreferencesUtil.getOutputDir(inputFile) + File.separatorChar
							+ PreferencesUtil.getOutputName(inputFile.getName()) + "." + FileUtil.EXTENSION_SAR;
				} else {
					outputPath = PreferencesUtil.getOutputDir(inputFile) + File.separatorChar
							+ PreferencesUtil.getOutputName(inputFile.getName()) + "." + FileUtil.EXTENSION_XML;
				}
			}

			// copy
			File outputFile = new File(outputPath);
			if (outputFile.exists()) {
				throw new OverwritingException();
			}
			FileUtil.bytesToFile(bytes, outputPath);

			// TODO centralizar esto
			// actualizo la entrada de la tabla
			pdfParameter.setPath(outputPath);
			pdfParameter.setSignatures(null);
			String mimeType = FileUtil.getMimeType(outputPath);
			pdfParameter.setMimeType(mimeType);

			// validar
			if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.AUTO_VALIDATE)) {
				ValidateController.validate(pdfParameter);
			}

			// mensaje
			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("info.document.signed"), pdfParameter.getPath());
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.INFO, m));

		} catch (XadesSignatureException e) {

			String m = MessageFormat.format(LanguageUtil.getLanguage().getString("error.certificate.sign.unexpected"),
					pdfParameter.getPath(), e.getMessage());
			log.error(m, e);
			Display.getDefault().syncExec(new ProgressWriter(ProgressWriter.ERROR, m));
		}
	}

	private static byte[] signXades(String documentPath, KsSignaturePreferences ksSignaturePreferences) throws XadesSignatureException,
			OCSPUnknownUrlException, CertificateExpiredException, CertificateNotYetValidException, RevokedException, OCSPCoreException,
			ConnectionException, OCSPIssuerRequiredException {

		XadesSignaturePreferences signaturePreferences = new XadesSignaturePreferences();
		signaturePreferences.setKsSignaturePreferences(ksSignaturePreferences);
		signaturePreferences.setType(XadesSignaturePreferences.Type.Detached);
		signaturePreferences.setArchive(PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.XADES_ARCHIVE));
		signaturePreferences.setKsCache(PreferencesUtil.getCacheKeystoreComplete());

		String tsurl = null;
		if (PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_TS_ENABLE) == true) {
			tsurl = PreferencesUtil.getTimestampPreferences().get(PreferencesUtil.getPreferences().getString(PreferencesUtil.SIGN_TS_TSA));
		}

		signaturePreferences.setTimestampUrl(tsurl);
		signaturePreferences.setTimestampUser(null);
		signaturePreferences.setTimestampPassword(null);

		boolean addOCSP = PreferencesUtil.getPreferences().getBoolean(PreferencesUtil.SIGN_OCSP_ENABLE);
		signaturePreferences.setAddOCSP(addOCSP);

		StatisticsUtil.log(StatisticsUtil.KEY_SIGN_OCSP, addOCSP + "");

		// firmar
		byte[] bytes = XadesService.signArchiver(documentPath, signaturePreferences);

		return bytes;
	}

}