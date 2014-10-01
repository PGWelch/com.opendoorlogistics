/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.core.utils;

import java.util.Scanner;

final public class KeyboardInput {
	private KeyboardInput() {
	}

	public static boolean yesNoPrompt(String message, boolean appendYesNoToMessage) {
		Scanner input = new Scanner(System.in); // Decl. & init. a Scanner.
		try {
			if (appendYesNoToMessage) {
				message += " (y/n) ";
			}

			while (true) {
				System.out.print(message);
				String response = input.next();
				response = response.toLowerCase().trim();
				if (response.equals("y") || response.equals("yes")) {
					return true;
				}
				if (response.equals("n") || response.equals("no")) {
					return false;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			input.close();
		}

		return false;
	}
}
