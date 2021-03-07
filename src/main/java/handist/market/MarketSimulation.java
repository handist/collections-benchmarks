/*******************************************************************************
 * Copyright (c) 2021 Handy Tools for Distributed Computing (HanDist) project.
 * 
 * This program and the accompanying materials are made available to you under 
 * the terms of the Eclipse Public License 1.0 which accompanies this
 * distribution, 
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 * 
 * SPDX-License-Identifier: EPL-1.0
 ******************************************************************************/
package handist.market;

/**
 * Launcher for a market simulation
 * @author Patrick Finnerty
 *
 */
public class MarketSimulation {

    /**
     * Main method
     * <p>
     * The current dummy implementation outputs the arguments given to this program line by line
     * @param args the strings for this program to display line by line on the standard output
     */
    public static void main(String[] args) {
        for (String s : args) {
            System.out.println(s);
        }
    }
}
