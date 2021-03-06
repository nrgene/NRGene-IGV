/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.broad.igv.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author eflakes
 */
abstract public class Filter {

    private String name;
    private LinkedHashSet<FilterElement> elements =
            new LinkedHashSet<FilterElement>();
    private boolean isEnabled = true;

    public Filter(String name, List<? extends FilterElement> elements) {
    	init(name, elements);
    }
    
    protected void init(String name,  List<? extends FilterElement> elements)
    {
    	this.name = name;

    	if (elements != null) {
            this.elements.addAll(elements);
        }
    }

    public void removeAll() {
        elements.clear();
    }

    public void setEnabled(boolean value) {
        isEnabled = value;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Iterator getFilterElements() {
        return elements.iterator();
    }

    public void add(FilterElement element) {
        elements.add(element);
    }

    public void remove(FilterElement element) {
        elements.remove(element);
    }

    /**
     * Evaluate the FilterElement set.
     *
     * @return
     */
    abstract protected void evaluate();

    public String getName() {
        return name;
    }
}
