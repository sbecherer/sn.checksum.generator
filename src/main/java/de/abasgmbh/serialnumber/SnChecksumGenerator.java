package de.abasgmbh.serialnumber;

import java.math.BigInteger;

import org.apache.log4j.Logger;

import de.abas.eks.jfop.FOPException;
import de.abas.eks.jfop.remote.ContextRunnable;
import de.abas.eks.jfop.remote.FOPSessionContext;
import de.abas.jfop.base.buffer.BufferFactory;
import de.abas.jfop.base.buffer.UserTextBuffer;

public class SnChecksumGenerator implements ContextRunnable {
	Logger logger = Logger.getLogger(SnChecksumGenerator.class);

	// Berechnet die Pruefsumme zu der uebergebenen Seriennummer
	//
	//
	// Uebergabe Variabeln
	// U|xtsn - Seriennummer
	// U|xichecksumfactor - Teiler fuer Pruefsummenberechnung (Default: 97)
	//
	// Rueckgabe
	// U|xbok - Gab es Fehler
	// U|xtchecksum - Pruefziffer (2 Stellig)
	//

	@Override
	public int runFop(FOPSessionContext fopSessionContext, String[] arg0) throws FOPException {
		int checksum = 0;
		int factor = 97;

		BufferFactory bufferFactory = BufferFactory.newInstance(true);
		UserTextBuffer userTextBuffer = bufferFactory.getUserTextBuffer();

		if (userTextBuffer.isVarDefined("xichecksumfactor")) {
			factor = userTextBuffer.getIntegerValue("xichecksumfactor");
			logger.debug("Factor is set to: \"" + factor + "\"");
		}

		if (!userTextBuffer.isVarDefined("xtsn")) {
			logger.error("Var \"xtsn\" is not defined.");
			userTextBuffer.setValue("xbok", false);
			return 1;
		}

		String serial = userTextBuffer.getStringValue("xtsn").toUpperCase();

		if (serial.isEmpty()) {
			logger.error("Var \"xtsn\" is empty.");
			userTextBuffer.setValue("xbok", false);
			return 1;
		}

		// Seriennummer anhand von "-" aufteilen
		String[] snparts = serial.split("-");

		for (String part : snparts) {

			if (Character.isLetter(part.charAt(0))) {
				// Pruefsumme fuer Buchstaben-Zeichenketten
				String chartonumber = "";

				for (char ch : part.toCharArray()) {
					int chnum = (ch - 'A');
					if (chnum < 10) {
						chartonumber = chartonumber + "0" + chnum;
					} else {
						chartonumber = chartonumber + chnum;
					}
				}
				checksum += Integer.parseInt(chartonumber) % factor;

			} else if (part.startsWith("%") | part.startsWith("&")) {
				// Platzhalter nicht beruecksichtigen!
			} else {
				// Pruefsumme fuer Zahlen
				checksum += new BigInteger(part).mod(new BigInteger(Integer.toString(factor))).intValue();
			}
		}

		checksum = checksum % 100;

		if (!userTextBuffer.isVarDefined("xtchecksum")) {
			userTextBuffer.defineVar("I", "xtchecksum");
		}
		
		// Pruefsumme immer 2-stellig
		if (checksum < 10) {
			userTextBuffer.setValue("xtchecksum", "0" + checksum);
		} else {
			userTextBuffer.setValue("xtchecksum", checksum);
		}

		if (!userTextBuffer.isVarDefined("xbok")) {
			userTextBuffer.defineVar("B", "xbok");
		}
		userTextBuffer.setValue("xbok", true);
		
		return 0;
	}

}
