/*
 * Copyright (C) 2014 Magdalena Neumann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tum.imagesegmentation;

import java.io.Serializable;

import android.graphics.Color;
import android.graphics.Paint;

public class ColoredPaths implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private SerialPaint paint;
	private SinglePathCollection paths;

	public ColoredPaths() {
		initPaint();
		this.paths = new SinglePathCollection();
	}

	public ColoredPaths(int color) {
		initPaint();
		this.paint.setColor(color);
		this.paths = new SinglePathCollection();
	}

	public ColoredPaths(SinglePathCollection path) {
		initPaint();
		this.paths = path;
	}

	public ColoredPaths(SinglePathCollection path, int color) {
		initPaint();
		this.paint.setColor(color);
		this.paths = path;
	}

	private void initPaint() {
		this.paint = new SerialPaint();

		// Initialize Paint
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(Color.BLACK);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(6);
	}

	public SinglePathCollection getPath() {
		return paths;
	}

	public void setPath(SinglePathCollection path) {
		this.paths = path;
	}

	public Paint getPaint() {
		return paint;
	}

	public void setPaint(Paint paint) {
		this.paint = new SerialPaint(paint);
	}
}
