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
import java.util.LinkedList;

import android.graphics.PointF;

public class SinglePath implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private LinkedList<SerialPointF> path;

	public SinglePath() {
		this.setPath(new LinkedList<SerialPointF>());
	}

	public SinglePath(LinkedList<PointF> path) {
		this.path = new LinkedList<SerialPointF>();
		for(PointF element : path) {
			this.path.add(new SerialPointF(element));
		}
	}

	/**
	 * @return the path
	 */
	public LinkedList<SerialPointF> getPath() {
		return path;
	}
	
	public LinkedList<PointF> getPathPointF() {
		LinkedList<PointF> result = new LinkedList<PointF>();
		for(SerialPointF element : path) {
			result.add(new PointF(element.x, element.y));
		}
		return result;
	}

	/**
	 * @param path
	 *            the path to set
	 */
	public void setPath(LinkedList<SerialPointF> path) {
		this.path = path;
	}
}
