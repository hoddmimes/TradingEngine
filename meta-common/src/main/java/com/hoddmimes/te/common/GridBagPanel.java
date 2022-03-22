/*
 * Copyright (c)  Hoddmimes Solution AB 2022.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hoddmimes.te.common;

import javax.swing.*;
import java.awt.*;

public class GridBagPanel extends JPanel
{
	private GridBagConstraints gc;

	public GridBagPanel() {
		this( GridBagConstraints.NORTHWEST);
	}

	public GridBagPanel(int pAnchor) {
		super( new GridBagLayout());
		gc = new GridBagConstraints();
		gc.gridx = gc.gridy = 0;
		gc.insets = new Insets(0,0,0,0);
		gc.anchor = pAnchor;
	}

	public GridBagPanel x( int pGridValue) {
		gc.gridx = pGridValue;
		return this;
	}

	public GridBagPanel y( int pGridValue) {
		gc.gridy = pGridValue;
		return this;
	}

	public GridBagPanel add( Component pComponent ) {
		super.add( pComponent, gc );
		return this;
	}

	public GridBagPanel insets( Insets pInsets) {
		gc.insets = pInsets;
		return this;
	}

	public GridBagPanel top( int pTop ) {
		gc.insets.top = pTop;
		return this;
	}

	public GridBagPanel bottom( int pBottom ) {
		gc.insets.bottom = pBottom;
		return this;
	}

	public GridBagPanel left( int pLeft ) {
		gc.insets.left = pLeft;
		return this;
	}

	public GridBagPanel right( int pRight ) {
		gc.insets.right = pRight;
		return this;
	}

	public GridBagPanel incx() {
		gc.gridx++;
		return this;
	}

	public GridBagPanel incy() {
		gc.gridx = 0; // ??????
		gc.gridy++;
		return this;
	}
}
