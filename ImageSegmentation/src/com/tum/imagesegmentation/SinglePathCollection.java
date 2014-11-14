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

public class SinglePathCollection implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private LinkedList<SinglePath> paths;
	
	public SinglePathCollection() {
		this.setPaths(new LinkedList<SinglePath>());
	}
	
	public SinglePathCollection(LinkedList<SinglePath> paths) {
		this.setPaths(paths);
	}

	/**
	 * @return the paths
	 */
	public LinkedList<SinglePath> getPaths() {
		return paths;
	}

	/**
	 * @param paths the paths to set
	 */
	public void setPaths(LinkedList<SinglePath> paths) {
		this.paths = paths;
	}
	
	public void add(SinglePath path) {
		this.paths.add(path);
	}
}
